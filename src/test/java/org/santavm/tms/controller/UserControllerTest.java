package org.santavm.tms.controller;

import org.hamcrest.CoreMatchers;
import org.hamcrest.core.StringContains;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "command.line.runner.enabled=false"})  // exclude DataLoader
//@WebMvcTest
class UserControllerTest {

    @MockBean
    private UserRepository userRepository;

    @Autowired
    UserController userController;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext context; // for security

    @BeforeEach
    public void setup(){
        mockMvc = MockMvcBuilders  // for security
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        User bob = User.builder()
                .id(1000L).firstName("user").lastName("Doe").email("bob@domain.com")
                .password("password").role(User.Role.USER)
                .build();

        Mockito.when(userRepository.save( ArgumentMatchers.any() )).thenReturn(bob);
        Mockito.doNothing().when(userRepository).deleteById( ArgumentMatchers.any() ); // mocking void method
        //TODO мешает тесту на регистрацию
//        Mockito.when(userRepository.findByEmail(ArgumentMatchers.any())).thenReturn(Optional.of(bob));
    }

    @Test
    public void whenUserControllerInjected_thenNotNull() throws Exception {
        assertNotNull(userController);
    }

    @Test
    void whenPostRequestToUsersAndValidUser_thenCorrectResponse() throws Exception {
        MediaType textPlainUtf8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
        String user =
                "{\"firstName\": \"bob\",\"lastName\": \"Doe\",\"email\": \"bob@domain.com\",\"password\": \"111\",\"role\": \"USER\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/users/register")
                        .content(user)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isCreated())
                .andExpect(MockMvcResultMatchers.content().contentType( textPlainUtf8 ));
    }

    @Test
    public void whenPostRequestToUsersAndInValidUser_thenResponse400() throws Exception {
        String user =
                "{\"firstName\": \"bob\",\"lastName\": \"Doe\",\"email\": \"bobdomain.com\",\"password\": \"111\",\"role\": \"USER\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/users/register")
                        .content(user)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.jsonPath("$.email", CoreMatchers.is("must be a well-formed email address")))
//                .andExpect(MockMvcResultMatchers.content().string(StringContains.containsString("must be a well-formed email address")))
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithMockUser
    void login() throws Exception { //TODO login test
/*        mockMvc.perform(MockMvcRequestBuilders.post("/users/login"))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));*/
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
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/100/delete"))
                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(MockMvcResultMatchers.content().string(StringContains.containsString("User deleted successfully: 100")));
    }

    @Test
    @WithMockUser
    void whenDeleteAsUser_thenForbidden() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/100/delete"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}