package org.santavm.tms.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.santavm.tms.dto.AuthRequest;
import org.santavm.tms.dto.UserReq;
import org.santavm.tms.dto.AuthResponse;
import org.santavm.tms.dto.UserResp;
import org.santavm.tms.service.UserService;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "first controller for Users")
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
    public ResponseEntity<?> register(@Valid @RequestBody UserReq userReq){
        Long regUser = service.register(userReq);
        return ResponseEntity.status(HttpStatus.CREATED).body("User created successfully: " + regUser);
    }

    @PutMapping("/update")
    @SecurityRequirement(name = "JWT Bearer")
    public ResponseEntity<?> updateUser(@Valid @RequestBody UserReq userReq, Authentication auth){
        UserResp updated = service.updateUser( userReq, auth);
        return ResponseEntity.status(HttpStatus.OK).body("User updated successfully: " + updated.getId());
    }

    @Operation(
            description = "Get user ID and JWT token",
            summary = "Login with email and password",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login",
                            content = { @Content(
                                    mediaType = "application/json",
                                    schema = @Schema(implementation = AuthResponse.class) ) }),
                    @ApiResponse(responseCode = "403", description = "Wrong email or password",
                            content = { @Content(
                                    mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema( example = "Bad credentials") ) })
            }
    )
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest authRequest){
        AuthResponse authResponse = service.login(authRequest);
        return ResponseEntity.ok(authResponse);
    }

    @Operation(
            description = "Without information about user's tasks and comments",
            summary = "Getting list of all users \"lazily\"",
            responses = {
                    @ApiResponse(responseCode = "200", description = "All users list",
                            content = { @Content(mediaType = "application/json") })
            }
    )
    @GetMapping()
    public ResponseEntity<?> getAllUsers(){
        List<UserResp> userReqList = service.getAll();
        return ResponseEntity.ok(userReqList);
    }

    @Operation(description = "!!! Only user with 'ADMIN' role can delete other users -!!!Delete User from DB",
            summary = "Delete User from DB")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "User deleted successfully",
                    content = { @Content(
                            mediaType = "text/plain; charset=utf-8",
                            schema = @Schema( example = "User deleted successfully: 1") ) }),
            @ApiResponse(responseCode = "403", description = "Access denied",
                    content = { @Content(
                            mediaType = "text/plain; charset=utf-8",
                            schema = @Schema( example = "Bad credentials") ) })
        }
    )
    @SecurityRequirement(name = "JWT Bearer")
    @DeleteMapping("/{id}/delete")
    @PreAuthorize(value = "hasRole('ADMIN')")
    public ResponseEntity<?> deleteUser(@PathVariable Long id){
        service.deleteUser( id );
        return ResponseEntity.status(HttpStatus.OK).body("User deleted successfully: " + id);
    }

    @Operation(
            description = "All information loaded eagerly",
            summary = "Retrieve all information about User",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Login",
                            content = { @Content(mediaType = "application/json",
                                    schema = @Schema(implementation = UserResp.class)) }),
                    @ApiResponse(responseCode = "400", description = "No user found with given id",
                            content = { @Content(mediaType = "text/plain; charset=utf-8") })
            }
    )
    @SecurityRequirement(name = "JWT Bearer")
//    @Hidden
    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id){
        UserResp response = service.getById(id);
        return ResponseEntity.ok(response);
    }
}
