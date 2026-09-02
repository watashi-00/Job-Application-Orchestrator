package com.watashi.adapters.in.email;

import com.watashi.core.domain.email.RecruiterEmailMessage;
import com.watashi.core.ports.in.SyncRecruiterInboxUseCase;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

public class MiniMxServerTest extends TestCase {

    private static class StubSyncRecruiterInboxUseCase implements SyncRecruiterInboxUseCase {
        private final List<RecruiterEmailMessage> received = new ArrayList<>();

        @Override
        public RecruiterEmailMessage receiveIncomingEmail(
                String sender, String recipient, String subject, String bodyText) {
            RecruiterEmailMessage message = new RecruiterEmailMessage(sender, recipient, subject, bodyText);
            received.add(message);
            return message;
        }

        @Override
        public List<RecruiterEmailMessage> getAllReceivedEmails() {
            return new ArrayList<>(received);
        }
    }

    public void testSmtpProtocolAndEmailReception() throws Exception {
        StubSyncRecruiterInboxUseCase syncUseCase = new StubSyncRecruiterInboxUseCase();
        int testPort = 2526;
        MiniMxServer server = new MiniMxServer(testPort, syncUseCase);

        try {
            server.start();
            assertTrue(server.isRunning());

            try (Socket socket = new Socket("localhost", testPort);
                    BufferedReader reader =
                            new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                    PrintWriter writer = new PrintWriter(
                            new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

                String greeting = reader.readLine();
                assertNotNull(greeting);
                assertTrue(greeting.startsWith("220"));

                writer.print("HELO localhost\r\n");
                writer.flush();
                String heloResp = reader.readLine();
                assertNotNull(heloResp);
                assertTrue(heloResp.startsWith("250"));

                writer.print("MAIL FROM:<recruiter@acme.com>\r\n");
                writer.flush();
                String mailResp = reader.readLine();
                assertNotNull(mailResp);
                assertTrue(mailResp.startsWith("250"));

                writer.print("RCPT TO:<candidate@localhost>\r\n");
                writer.flush();
                String rcptResp = reader.readLine();
                assertNotNull(rcptResp);
                assertTrue(rcptResp.startsWith("250"));

                writer.print("DATA\r\n");
                writer.flush();
                String dataResp = reader.readLine();
                assertNotNull(dataResp);
                assertTrue(dataResp.startsWith("354"));

                writer.print("Subject: Interview Request - Senior Backend Engineer\r\n");
                writer.print("\r\n");
                writer.print("Hello, Acme Corp would love to invite you for an interview!\r\n");
                writer.print(".\r\n");
                writer.flush();
                String endDataResp = reader.readLine();
                assertNotNull(endDataResp);
                assertTrue(endDataResp.startsWith("250"));

                writer.print("QUIT\r\n");
                writer.flush();
                String quitResp = reader.readLine();
                assertNotNull(quitResp);
                assertTrue(quitResp.startsWith("221"));
            }

            List<RecruiterEmailMessage> receivedList = syncUseCase.getAllReceivedEmails();
            assertEquals(1, receivedList.size());
            RecruiterEmailMessage msg = receivedList.get(0);
            assertEquals("recruiter@acme.com", msg.sender());
            assertEquals("candidate@localhost", msg.recipient());
            assertEquals("Interview Request - Senior Backend Engineer", msg.subject());
            assertTrue(msg.bodyText().contains("Acme Corp would love to invite you"));
        } finally {
            server.stop();
            assertFalse(server.isRunning());
        }
    }
}
