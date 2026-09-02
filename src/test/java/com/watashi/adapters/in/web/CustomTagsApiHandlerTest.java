package com.watashi.adapters.in.web;

import com.watashi.core.domain.tag.CustomTag;
import com.watashi.core.ports.in.ManageCustomTagsUseCase;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.List;
import junit.framework.TestCase;

public class CustomTagsApiHandlerTest extends TestCase {

    public void testGetAllTags() throws Exception {
        CustomTag tag1 = new CustomTag("tag-1", "Go", "LANGUAGE", "#00add8", true);
        List<CustomTag> list = List.of(tag1);

        ManageCustomTagsUseCase mockTagsUseCase = new ManageCustomTagsUseCase() {
            @Override
            public List<CustomTag> getAllTags() {
                return list;
            }

            @Override
            public CustomTag addCustomTag(String name, String category, String colorHex) {
                return null;
            }

            @Override
            public void removeCustomTag(String tagId) {}
        };

        DashboardHttpServer server = new DashboardHttpServer(18102, null, mockTagsUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18102/api/tags"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertTrue(response.body().contains("Go"));
            assertTrue(response.body().contains("LANGUAGE"));
        } finally {
            server.stop();
        }
    }

    public void testAddCustomTag() throws Exception {
        List<CustomTag> tagsList = new ArrayList<>();
        ManageCustomTagsUseCase mockTagsUseCase = new ManageCustomTagsUseCase() {
            @Override
            public List<CustomTag> getAllTags() {
                return tagsList;
            }

            @Override
            public CustomTag addCustomTag(String name, String category, String colorHex) {
                CustomTag t = new CustomTag("id-" + name, name, category, colorHex, true);
                tagsList.add(t);
                return t;
            }

            @Override
            public void removeCustomTag(String tagId) {}
        };

        DashboardHttpServer server = new DashboardHttpServer(18103, null, mockTagsUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            String jsonPayload = "{\"name\":\"Docker\",\"category\":\"DEVOPS\",\"colorHex\":\"#2496ed\"}";
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18103/api/tags"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(1, tagsList.size());
            assertEquals("Docker", tagsList.get(0).name());
            assertEquals("DEVOPS", tagsList.get(0).category());
        } finally {
            server.stop();
        }
    }

    public void testRemoveCustomTag() throws Exception {
        List<String> removedIds = new ArrayList<>();
        ManageCustomTagsUseCase mockTagsUseCase = new ManageCustomTagsUseCase() {
            @Override
            public List<CustomTag> getAllTags() {
                return List.of();
            }

            @Override
            public CustomTag addCustomTag(String name, String category, String colorHex) {
                return null;
            }

            @Override
            public void removeCustomTag(String tagId) {
                removedIds.add(tagId);
            }
        };

        DashboardHttpServer server = new DashboardHttpServer(18104, null, mockTagsUseCase);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18104/api/tags?id=tag-xyz"))
                    .DELETE()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(200, response.statusCode());
            assertEquals(1, removedIds.size());
            assertEquals("tag-xyz", removedIds.get(0));
        } finally {
            server.stop();
        }
    }

    public void testNullTagsUseCaseReturns501() throws Exception {
        DashboardHttpServer server = new DashboardHttpServer(18105, null, (ManageCustomTagsUseCase) null);
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("http://localhost:18105/api/tags"))
                    .GET()
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            assertEquals(501, response.statusCode());
        } finally {
            server.stop();
        }
    }
}
