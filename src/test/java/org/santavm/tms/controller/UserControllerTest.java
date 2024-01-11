package org.santavm.tms.controller;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.AutoConfigureDataJpa;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@AutoConfigureMockMvc
@AutoConfigureDataJpa
@ActiveProfiles(profiles = {"test"})
@Sql(scripts = "classpath:createTables.sql", executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "classpath:dropTablesAndSeq.sql", executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@SpringBootTest(properties = {
        "command.line.runner.enabled=false"})  // exclude DataLoader
class UserControllerTest {
    @Autowired
    UserController userController;

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void whenUserControllerInjected_thenNotNull() {
        assertNotNull(userController);
    }

    @Test
    void whenPostRequestToUsersAndValidUser_thenCorrectResponse() throws Exception {
        MediaType textPlainUtf8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
        String user = """
                {
                "firstName": "Name",
                "lastName": "Lastname",
                "email": "email@site.com",
                "password": "password",
                "role": "USER"
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.post("/users/register")
                        .content(user)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentType( textPlainUtf8 ));
    }

    @Test
    public void whenPostRequestToUsersAndInValidUser_thenResponse400() throws Exception {
        String user =
                """
                {
                "firstName": "Name",
                "lastName": "Lastname",
                "email": "NOT_EMAIL",
                "password": "password",
                "role": "USER"
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.post("/users/register")
                        .content(user)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email", CoreMatchers.is("must be a well-formed email address")))
//                .andExpect(MockMvcResultMatchers.content().string(StringContains.containsString("must be a well-formed email address")))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void whenLoginWithValidData_thenOk() throws Exception {
        String loginCorrect = """
                {
                "email": "adm@site.com",
                "password": "123"
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                .content(loginCorrect)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void whenLoginWithInvalidData_thenBadCredentials() throws Exception {
        String loginIncorrect = """
                {
                "email": "adm@site.com",
                "password": "123456"
                }
                """;
        mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                        .content(loginIncorrect)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isForbidden())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.TEXT_PLAIN))
                .andExpect(MockMvcResultMatchers.content().string(StringContains.containsString("Bad credentials")));
    }

    @Test
    void whenGetRequestToUsers_thenCorrectResponse() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.get("/users")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser(username="admin",roles={"ADMIN"})
    void whenDeleteAsAdmin_thenSuccess() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/{id}/delete", 1))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().string(StringContains.containsString("User deleted successfully: 1"))
                );
    }

    @Test
    @WithMockUser
    void whenDeleteAsUser_thenForbidden() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/{id}/delete", 1))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}