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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.TreeSet;

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
    PasswordEncoder encoder;

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

        String pass = encoder.encode("123");
        User bob = new User(1L,"user","user","user@user.com",pass, User.Role.ADMIN,new TreeSet<>(),new TreeSet<>());

        Mockito.when(userRepository.saveAndFlush( ArgumentMatchers.any() )).thenReturn(bob);
        // makes /register test: 400 ERROR: Email already registered: (intentionally)
        Mockito.when(userRepository.findByEmail( ArgumentMatchers.any() )).thenReturn(Optional.of(bob));
//        Mockito.doNothing().when(userRepository).deleteById( ArgumentMatchers.any() ); // mocking void method

    }

    @Test
    public void whenUserControllerInjected_thenNotNull() {
        assertNotNull(userController);
    }

    @Test
    void whenPostRequestToUsersAndValidUser_thenCorrectResponse() throws Exception {
        MediaType textPlainUtf8 = new MediaType(MediaType.TEXT_PLAIN, StandardCharsets.UTF_8);
        String user =
                "{\"firstName\": \"user\",\"lastName\": \"user\",\"email\": \"user@user.com\",\"password\": \"123\",\"role\": \"ADMIN\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/users/register")
                        .content(user)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isBadRequest())  // isCreated()
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
    void login() throws Exception {
        String loginBob = "{\"email\": \"user@user.com\",\"password\": \"123\"}";
        mockMvc.perform(MockMvcRequestBuilders.post("/users/login")
                .content(loginBob)
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.content().contentType(MediaType.APPLICATION_JSON));
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
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/1/delete"))
//                .andExpect(MockMvcResultMatchers.status().isNoContent())
                .andExpect(MockMvcResultMatchers.status().isBadRequest())
                .andExpect(MockMvcResultMatchers.content().string(StringContains.containsString("There is no User with id: 1"))
                );
    }

    @Test
    @WithMockUser
    void whenDeleteAsUser_thenForbidden() throws Exception{
        mockMvc.perform(MockMvcRequestBuilders.delete("/users/100/delete"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }
}