package org.santavm.tms.controller;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;

import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.authenticated;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@AutoConfigureDataJpa
@ActiveProfiles(profiles = {"test"})
@Sql(scripts = "classpath:createTables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:dropTablesAndSeq.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@WithUserDetails(value = "adm@site.com", userDetailsServiceBeanName = "userDetailsService")
@SpringBootTest(properties = {
        "command.line.runner.enabled=false"})  // exclude DataLoader
class IT_TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @AfterEach
    public void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void createTask() throws Exception {
        MediaType textPlainUtf8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

        String task = """
                {
                "title": "New Task",
                "description": "Test new Task",
                "status": "ON_HOLD",
                "priority": "LOW",
                "executorId": "1"
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.post("/tasks/create")
                        .content(task)
                        .contentType(MediaType.APPLICATION_JSON)
                        )
                .andExpect(status().isCreated())
                .andExpect(content().contentType(textPlainUtf8))
                .andExpect(content().string(StringContains.containsString("Task created with id: ")));
    }

    @Test
    void deleteTask() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete("/tasks/{id}/delete", 1))
                .andExpect(authenticated().withUsername("adm@site.com").withRoles("ADMIN"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(StringContains.containsString("Task deleted successfully: 1"))
                );
    }

    @Test
    void updateTask() throws Exception {
        MediaType textPlainUtf8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);

        String task = """
                {
                "title": "New Task",
                "description": "Test new Task",
                "status": "ON_HOLD",
                "priority": "LOW",
                "executorId": "1"
                }
                """;

        mockMvc.perform(MockMvcRequestBuilders.put("/tasks/{id}/update", 1)
                        .content(task)
                        .contentType(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk())
                .andExpect(content().contentType(textPlainUtf8))
                .andExpect(content().string(StringContains.containsString("Task updated successfully: 1"))
                );
    }

    @Test
    void findOne() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/tasks/{id}", 1)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.author", CoreMatchers.is("id: 1, name: Admin, surname: Admin")));
    }

    @Test
    void findAllEagerly() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/tasks/all")
                        .param("page", "0")
                        .param("size", "1")
                        .param("sort", "id")
                        .param("direction", "DESC")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.[0].author", CoreMatchers.is("id: 1, name: Admin, surname: Admin")));
    }

    @Test
    @WithAnonymousUser
    void anonymousRejected() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.get("/tasks/lazy")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(unauthenticated());
    }
}