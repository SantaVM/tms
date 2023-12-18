package org.santavm.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
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

    @Operation(summary = "Add new User to TMS")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User added successfully",
                    content = { @Content(mediaType = "text/plain; charset=utf-8",
                            schema = @Schema(
                                    example = "User created successfully: 1"
                            )) }),
            @ApiResponse(responseCode = "400", description = "Email already taken",
                    content = { @Content(mediaType = "text/plain; charset=utf-8",
                            schema = @Schema(
                                    example = "ERROR: Email already registered: some@site.com"
                            )) })
            }
    )
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody UserDTO userDTO){
        UserDTO regUser = service.register(userDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body("User created successfully: " + regUser.getId());
    }

    @Operation(
            description = "Login with email and password",
            summary = "Get user ID and JWT token",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login",
                            content = { @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponse.class)) }),
                    @ApiResponse(responseCode = "401", description = "Wrong email or password",
                            content = { @Content(mediaType = "text/plain; charset=utf-8") })
            }
    )
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

    @Operation(description = "Delete User from DB",
            summary = "!!! Only user with 'ADMIN' role can delete other users -!!!")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "User deleted successfully",
                    content = { @Content(mediaType = "text/plain; charset=utf-8",
                            schema = @Schema(
                                    example = "User deleted successfully: 1"
                            )) }),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = { @Content(mediaType = "text/plain; charset=utf-8") })
        }
    )
    @SecurityRequirement(name = "JWT Bearer")
    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteUser(@PathVariable Long id){
        service.deleteUser( id );
        return ResponseEntity.status(HttpStatus.NO_CONTENT).body("User deleted successfully: " + id);
    }

    @SecurityRequirement(name = "JWT Bearer")
    @GetMapping("/protected")  //TODO remove this
    public ResponseEntity<?> testProtected(Authentication auth){
        AuthResponse authResponse = service.testProtected();
        return ResponseEntity.ok(authResponse);
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
