package com.watashi.adapters.in.email;

import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MiniMxServer {

    private static final int DEFAULT_PORT = 2525;

    private final int configuredPort;
    private final SyncRecruiterInboxUseCase syncUseCase;
    private ServerSocket serverSocket;
    private Thread serverThread;
    private volatile boolean running = false;

    public MiniMxServer(SyncRecruiterInboxUseCase syncUseCase) {
        this(DEFAULT_PORT, syncUseCase);
    }

    public MiniMxServer(int port, SyncRecruiterInboxUseCase syncUseCase) {
        this.configuredPort = port;
        this.syncUseCase = Objects.requireNonNull(syncUseCase, "syncUseCase cannot be null");
    }

    public synchronized void start() {
        if (running) {
            return;
        }
        try {
            serverSocket = new ServerSocket(configuredPort);
            running = true;
            serverThread = new Thread(this::listen, "MiniMxServer-Thread");
            serverThread.setDaemon(true);
            serverThread.start();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to start MiniMxServer on port " + configuredPort, e);
        }
    }

    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (serverSocket != null && !serverSocket.isClosed()) {
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
        }
        if (serverThread != null) {
            serverThread.interrupt();
        }
    }

    public boolean isRunning() {
        return running && serverSocket != null && !serverSocket.isClosed();
    }

    public int getPort() {
        if (serverSocket != null && !serverSocket.isClosed()) {
            return serverSocket.getLocalPort();
        }
        return configuredPort;
    }

    private void listen() {
        while (running && serverSocket != null && !serverSocket.isClosed()) {
            try {
                Socket clientSocket = serverSocket.accept();
                Thread.ofVirtual().start(() -> handleClient(clientSocket));
            } catch (IOException e) {
                if (!running || serverSocket.isClosed()) {
                    break;
                }
            }
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
                BufferedReader reader =
                        new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                PrintWriter writer = new PrintWriter(
                        new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            writer.print("220 localhost MiniMX SMTP Ready\r\n");
            writer.flush();

            String sender = "";
            String recipient = "";

            String line;
            while ((line = reader.readLine()) != null) {
                String trimmedLine = line.trim();
                String upper = trimmedLine.toUpperCase();

                if (upper.startsWith("HELO") || upper.startsWith("EHLO")) {
                    String domain = trimmedLine
                            .substring(Math.min(4, trimmedLine.length()))
                            .trim();
                    writer.print("250 Hello " + domain + "\r\n");
                    writer.flush();
                } else if (upper.startsWith("MAIL FROM:")) {
                    sender = parseEmailAddress(trimmedLine, "MAIL FROM:");
                    writer.print("250 2.1.0 Ok\r\n");
                    writer.flush();
                } else if (upper.startsWith("RCPT TO:")) {
                    recipient = parseEmailAddress(trimmedLine, "RCPT TO:");
                    writer.print("250 2.1.5 Ok\r\n");
                    writer.flush();
                } else if (upper.equals("DATA")) {
                    writer.print("354 End data with <CR><LF>.<CR><LF>\r\n");
                    writer.flush();

                    List<String> dataLines = new ArrayList<>();
                    String dataLine;
                    while ((dataLine = reader.readLine()) != null) {
                        if (dataLine.equals(".")) {
                            break;
                        }
                        dataLines.add(dataLine);
                    }

                    ParsedEmail parsed = parseEmailData(dataLines);
                    syncUseCase.receiveIncomingEmail(sender, recipient, parsed.subject, parsed.body);

                    writer.print("250 2.0.0 Ok: queued\r\n");
                    writer.flush();
                } else if (upper.equals("QUIT")) {
                    writer.print("221 2.0.0 Bye\r\n");
                    writer.flush();
                    break;
                } else if (upper.equals("RSET") || upper.equals("NOOP")) {
                    writer.print("250 Ok\r\n");
                    writer.flush();
                } else {
                    writer.print("500 Command unrecognized\r\n");
                    writer.flush();
                }
            }
        } catch (IOException ignored) {
        }
    }

    private String parseEmailAddress(String line, String prefix) {
        String rest = line.substring(prefix.length()).trim();
        if (rest.startsWith("<") && rest.contains(">")) {
            return rest.substring(1, rest.indexOf(">")).trim();
        }
        return rest;
    }

    private record ParsedEmail(String subject, String body) {}

    private ParsedEmail parseEmailData(List<String> lines) {
        String subject = "(No Subject)";
        StringBuilder bodyBuilder = new StringBuilder();
        boolean readingHeaders = true;

        for (String line : lines) {
            if (readingHeaders) {
                if (line.isEmpty()) {
                    readingHeaders = false;
                } else if (line.toLowerCase().startsWith("subject:")) {
                    subject = line.substring(8).trim();
                }
            } else {
                if (!bodyBuilder.isEmpty()) {
                    bodyBuilder.append("\n");
                }
                bodyBuilder.append(line);
            }
        }

        if (readingHeaders && bodyBuilder.isEmpty()) {
            bodyBuilder.append(String.join("\n", lines));
        }

        return new ParsedEmail(subject, bodyBuilder.toString());
    }
}
