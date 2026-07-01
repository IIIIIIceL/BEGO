package com.bego.backend.tag;

import com.bego.backend.todo.entity.TodoEntity;
import com.bego.backend.todo.entity.TodoTagEntity;
import com.bego.backend.todo.mapper.TodoMapper;
import com.bego.backend.todo.mapper.TodoTagMapper;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class TagIntegrationTests {
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TodoMapper todoMapper;

    @Autowired
    private TodoTagMapper todoTagMapper;

    @Test
    void createListUpdateReorderDeleteAndReuseName() throws Exception {
        String accessToken = register("phase3-tags-" + UUID.randomUUID() + "@example.com").accessToken();

        long workId = createTag(accessToken, "Work", "#2f80ed", 20);
        long lifeId = createTag(accessToken, "Life", null, 10);

        mockMvc.perform(get("/api/v1/tags")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(lifeId))
                .andExpect(jsonPath("$[0].color").value("#2F80ED"))
                .andExpect(jsonPath("$[1].id").value(workId));

        String updateBody = """
                {
                  "name": "Project",
                  "color": "#27ae60",
                  "sortOrder": 30
                }
                """;

        mockMvc.perform(put("/api/v1/tags/{id}", workId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Project"))
                .andExpect(jsonPath("$.color").value("#27AE60"));

        String reorderBody = """
                {
                  "items": [
                    { "id": %d, "sortOrder": 1 },
                    { "id": %d, "sortOrder": 2 }
                  ]
                }
                """.formatted(workId, lifeId);

        mockMvc.perform(put("/api/v1/tags/reorder")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(reorderBody))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/tags")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(workId))
                .andExpect(jsonPath("$[1].id").value(lifeId));

        mockMvc.perform(delete("/api/v1/tags/{id}", workId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        createTag(accessToken, "Project", "#2F80ED", 3);

        mockMvc.perform(get("/api/v1/tags")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void duplicateNameAndCrossUserMutationAreRejected() throws Exception {
        AuthTokens first = register("phase3-owner-" + UUID.randomUUID() + "@example.com");
        AuthTokens second = register("phase3-other-" + UUID.randomUUID() + "@example.com");
        long tagId = createTag(first.accessToken(), "Focus", "#2F80ED", 0);

        String duplicateBody = """
                {
                  "name": " focus ",
                  "color": "#2F80ED",
                  "sortOrder": 1
                }
                """;

        mockMvc.perform(post("/api/v1/tags")
                        .header("Authorization", "Bearer " + first.accessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(duplicateBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("TAG_NAME_ALREADY_EXISTS"));

        mockMvc.perform(delete("/api/v1/tags/{id}", tagId)
                        .header("Authorization", "Bearer " + second.accessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    void listIncludesOpenTodoCountOnlyForTodoStatus() throws Exception {
        AuthTokens tokens = register("phase3-count-" + UUID.randomUUID() + "@example.com");
        long tagId = createTag(tokens.accessToken(), "Count", "#2F80ED", 0);

        TodoEntity openTodo = new TodoEntity();
        openTodo.setUserId(tokens.userId());
        openTodo.setTitle("open");
        openTodo.setStatus("TODO");
        openTodo.setPriority("MEDIUM");
        openTodo.setSyncStatus("SYNCED");
        openTodo.setSortOrder(0L);
        todoMapper.insert(openTodo);

        TodoEntity doneTodo = new TodoEntity();
        doneTodo.setUserId(tokens.userId());
        doneTodo.setTitle("done");
        doneTodo.setStatus("DONE");
        doneTodo.setPriority("MEDIUM");
        doneTodo.setSyncStatus("SYNCED");
        doneTodo.setSortOrder(0L);
        todoMapper.insert(doneTodo);

        TodoTagEntity openRelation = new TodoTagEntity();
        openRelation.setUserId(tokens.userId());
        openRelation.setTodoId(openTodo.getId());
        openRelation.setTagId(tagId);
        todoTagMapper.insert(openRelation);

        TodoTagEntity doneRelation = new TodoTagEntity();
        doneRelation.setUserId(tokens.userId());
        doneRelation.setTodoId(doneTodo.getId());
        doneRelation.setTagId(tagId);
        todoTagMapper.insert(doneRelation);

        mockMvc.perform(get("/api/v1/tags")
                        .header("Authorization", "Bearer " + tokens.accessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].openTodoCount").value(1));
    }

    private AuthTokens register(String email) throws Exception {
        String registerBody = """
                {
                  "email": "%s",
                  "password": "password123",
                  "displayName": "Phase Three"
                }
                """.formatted(email);

        String response = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return new AuthTokens(
                ((Number) JsonPath.read(response, "$.user.id")).longValue(),
                JsonPath.read(response, "$.accessToken")
        );
    }

    private long createTag(String accessToken, String name, String color, int sortOrder) throws Exception {
        String colorValue = color == null ? "null" : "\"" + color + "\"";
        String body = """
                {
                  "name": "%s",
                  "color": %s,
                  "sortOrder": %d
                }
                """.formatted(name, colorValue, sortOrder);

        String response = mockMvc.perform(post("/api/v1/tags")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return ((Number) JsonPath.read(response, "$.id")).longValue();
    }

    private record AuthTokens(Long userId, String accessToken) {
    }
}
