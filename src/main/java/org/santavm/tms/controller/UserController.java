package org.santavm.tms.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.santavm.tms.dto.AuthRequest;
import org.santavm.tms.dto.UserDTO;
import org.santavm.tms.dto.AuthResponse;
import org.santavm.tms.model.User;
import org.santavm.tms.service.UserService;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {
    private final UserService service;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO userDTO){
        UserDTO regUser = service.register(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("User created successfully: " + regUser.getId());
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest){
        AuthResponse authResponse = service.login(authRequest);
        return ResponseEntity.ok(authResponse);
    }

    @GetMapping()
    public ResponseEntity<List<UserDTO>> getAllUsers(){
        List<UserDTO> userDTOList = service.getAll();
        return ResponseEntity.ok(userDTOList);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteUser(@PathVariable Long id){
        service.deleteUser( id );
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("User deleted successfully: " + id);
    }

    @GetMapping("/protected")  //TODO remove this
    public ResponseEntity<?> testProtected(Authentication auth){
        AuthResponse authResponse = service.testProtected();
        User user = null;
        if (auth instanceof UsernamePasswordAuthenticationToken token) {
            user = (User) token.getPrincipal();
        }

        return ResponseEntity.ok(user);
    }

    // message from orElseThrow
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(CustomPermissionException.class)
    public ResponseEntity<String> handleCustomPermissionException(CustomPermissionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }
}
