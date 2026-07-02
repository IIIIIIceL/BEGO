package com.bego.backend.todo;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TodoIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void createDetailListAndIdempotentCreateWithTags() throws Exception {
        AuthTokens tokens = register("phase4-create-" + UUID.randomUUID() + "@example.com");
        long tagId = createTag(tokens.accessToken(), "Work");
        String clientTempId = "offline-" + UUID.randomUUID();

        long todoId = createTodo(
                tokens.accessToken(),
                """
                        {
                          "title": "Write backend",
                          "description": "finish todo module",
                          "priority": "HIGH",
                          "dueAt": "2026-07-05T10:00:00Z",
                          "reminderAt": "2026-07-05T09:00:00Z",
                          "clientTempId": "%s",
                          "tagIds": [%d]
                        }
                        """.formatted(clientTempId, tagId)
        );

        mockMvc.perform(get("/api/v1/todos/{id}", todoId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Write backend"))
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.priority").value("HIGH"))
                .andExpect(jsonPath("$.syncStatus").value("SYNCED"))
                .andExpect(jsonPath("$.tags[0].id").value(tagId));

        long idempotentTodoId = createTodo(
                tokens.accessToken(),
                """
                        {
                          "title": "Ignored retry title",
                          "clientTempId": "%s",
                          "tagIds": [%d]
                        }
                        """.formatted(clientTempId, tagId)
        );

        mockMvc.perform(get("/api/v1/todos?size=10")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].id").value(todoId));

        org.junit.jupiter.api.Assertions.assertEquals(todoId, idempotentTodoId);
    }

    @Test
    void updateStatusAndDeleteTodo() throws Exception {
        AuthTokens tokens = register("phase4-update-" + UUID.randomUUID() + "@example.com");
        long firstTagId = createTag(tokens.accessToken(), "First");
        long secondTagId = createTag(tokens.accessToken(), "Second");
        long todoId = createTodo(
                tokens.accessToken(),
                """
                        {
                          "title": "Original",
                          "priority": "LOW",
                          "tagIds": [%d]
                        }
                        """.formatted(firstTagId)
        );

        mockMvc.perform(put("/api/v1/todos/{id}", todoId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Updated",
                                  "description": "changed",
                                  "priority": "MEDIUM",
                                  "tagIds": [%d]
                                }
                                """.formatted(secondTagId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Updated"))
                .andExpect(jsonPath("$.tags[0].id").value(secondTagId));

        mockMvc.perform(patch("/api/v1/todos/{id}/status", todoId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DONE"))
                .andExpect(jsonPath("$.completedAt").value(notNullValue()));

        mockMvc.perform(patch("/api/v1/todos/{id}/status", todoId)
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "TODO"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TODO"))
                .andExpect(jsonPath("$.completedAt").value(nullValue()));

        mockMvc.perform(delete("/api/v1/todos/{id}", todoId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/todos/{id}", todoId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/v1/todos")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void filtersAndCrossUserTagValidation() throws Exception {
        AuthTokens owner = register("phase4-filter-" + UUID.randomUUID() + "@example.com");
        AuthTokens other = register("phase4-other-" + UUID.randomUUID() + "@example.com");
        long ownerTagId = createTag(owner.accessToken(), "Focus");
        long otherTagId = createTag(other.accessToken(), "Private");

        mockMvc.perform(post("/api/v1/todos")
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "Bad relation",
                                  "tagIds": [%d]
                                }
                                """.formatted(otherTagId)))
                .andExpect(status().isNotFound());

        long firstTodoId = createTodo(
                owner.accessToken(),
                """
                        {
                          "title": "Buy milk",
                          "description": "organic milk",
                          "priority": "LOW",
                          "dueAt": "2026-07-02T10:00:00Z",
                          "tagIds": [%d]
                        }
                        """.formatted(ownerTagId)
        );
        createTodo(
                owner.accessToken(),
                """
                        {
                          "title": "Finish report",
                          "description": "quarterly work",
                          "priority": "HIGH",
                          "dueAt": "2026-07-09T10:00:00Z",
                          "tagIds": [%d]
                        }
                        """.formatted(ownerTagId)
        );

        mockMvc.perform(patch("/api/v1/todos/{id}/status", firstTodoId)
                        .header("Authorization", "Bearer " + owner.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "DONE"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/todos?status=TODO&priority=HIGH&tagId={tagId}&keyword=report&dueFrom=2026-07-01T00:00:00Z&dueTo=2026-07-10T00:00:00Z&sort=DUE_ASC", ownerTagId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("Finish report"));

        mockMvc.perform(get("/api/v1/todos?tagId={tagId}", otherTagId)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void blankTitleIsRejected() throws Exception {
        AuthTokens tokens = register("phase4-validation-" + UUID.randomUUID() + "@example.com");

        mockMvc.perform(post("/api/v1/todos")
                        .header("Authorization", "Bearer " + tokens.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    void searchIsUserScopedAndEscapesLikeWildcards() throws Exception {
        AuthTokens owner = register("phase5-search-" + UUID.randomUUID() + "@example.com");
        AuthTokens other = register("phase5-other-" + UUID.randomUUID() + "@example.com");
        String needle = "needle-" + UUID.randomUUID();

        createTodo(
                other.accessToken(),
                """
                        {
                          "title": "%s private"
                        }
                        """.formatted(needle)
        );
        createTodo(
                owner.accessToken(),
                """
                        {
                          "title": "100 percent"
                        }
                        """
        );
        createTodo(
                owner.accessToken(),
                """
                        {
                          "title": "100% literal"
                        }
                        """
        );

        mockMvc.perform(get("/api/v1/todos?keyword={keyword}", needle)
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(0))
                .andExpect(jsonPath("$.items", hasSize(0)));

        mockMvc.perform(get("/api/v1/todos")
                        .param("keyword", "100%")
                        .header("Authorization", "Bearer " + owner.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.items[0].title").value("100% literal"));
    }

    @Test
    void combinedFiltersWorkWithPaginationAndSizeCap() throws Exception {
        AuthTokens tokens = register("phase5-page-" + UUID.randomUUID() + "@example.com");
        long tagId = createTag(tokens.accessToken(), "Paged");

        for (int i = 0; i < 3; i++) {
            createTodo(
                    tokens.accessToken(),
                    """
                            {
                              "title": "Paged report %d",
                              "description": "phase five",
                              "priority": "HIGH",
                              "dueAt": "2026-07-0%dT10:00:00Z",
                              "tagIds": [%d]
                            }
                            """.formatted(i, i + 3, tagId)
            );
        }

        mockMvc.perform(get("/api/v1/todos?status=TODO&priority=HIGH&tagId={tagId}&keyword=report&page=1&size=2&sort=DUE_ASC", tagId)
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(3))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.items[0].title").value("Paged report 2"));

        mockMvc.perform(get("/api/v1/todos?size=150")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(100));
    }

    private AuthTokens register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "password123",
                                  "displayName": "Phase Four"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new AuthTokens(JsonPath.read(response, "$.accessToken"));
    }

    private long createTag(String accessToken, String name) throws Exception {
        String response = mockMvc.perform(post("/api/v1/tags")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s",
                                  "sortOrder": 0
                                }
                                """.formatted(name)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private long createTodo(String accessToken, String body) throws Exception {
        String response = mockMvc.perform(post("/api/v1/todos")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private record AuthTokens(String accessToken) {
    }
}
