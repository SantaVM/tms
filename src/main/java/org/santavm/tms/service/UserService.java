package org.santavm.tms.service;

import lombok.RequiredArgsConstructor;
import org.santavm.tms.dto.AuthRequest;
import org.santavm.tms.dto.AuthResponse;
import org.santavm.tms.dto.UserDTO;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.UserRepository;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final CommentService commentService;
    private final TaskService taskService;

    private final AuthenticationManager authenticationManager;

    public UserDTO register(UserDTO userDTO){
        User user = this.mapDtoToUser(userDTO);

        Optional<User> userFromDb = repository.findByEmail(userDTO.getEmail());

        if (userFromDb.isPresent()) {
            throw new NoSuchElementException("ERROR: Email already registered: " + userDTO.getEmail());
        }

        User savedUser = repository.saveAndFlush(user);
        return this.mapUserToDto(savedUser);
    }

    public AuthResponse login(AuthRequest authRequest){
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(),
                        authRequest.getPassword()
                ));
        User user = repository.findByEmail(authRequest.getEmail())  // correct token, but user deleted from DB
                .orElseThrow(() -> new NoSuchElementException("There is no User with email: "+authRequest.getEmail()));
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(user.getId(), token);
    }

    public List<UserDTO> getAll() {
        List<User> userList = repository.findAll();
        return userList.stream().map(this::mapUserToDto).toList();
    }

    //TODO delete this
    public AuthResponse testProtected() {
        String userList = "Tested!";
        return new AuthResponse(100500L, userList);
    }

    public UserDTO mapUserToDto(User user){
        return UserDTO.builder()
                .id(user.getId())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .email(user.getEmail())
                .password("********")
                .role(user.getRole())
                .tasksAsAuthor(user.getTasksAsAuthor())
                .tasksAsExecutor(user.getTasksAsExecutor())
                .build();
    }

    public User mapDtoToUser(UserDTO userDTO){
        return User.builder()
                .firstName(userDTO.getFirstName())
                .lastName(userDTO.getLastName())
                .email(userDTO.getEmail())
                .password( encoder.encode( userDTO.getPassword() ) )
                .role(userDTO.getRole())  // or set constant role like "USER"
                .build();
    }

    // Only ADMIN  user can do this
    public void deleteUser(Long userId) {
        Optional<User> userOptional = repository.findById(userId);
        if( userOptional.isEmpty()){
            throw new NoSuchElementException("There is no User with id: " + userId);
        }
        User user = userOptional.get();
        Set<Long> tasksAsAuthor = user.getTasksAsAuthor();
        Set<Long> tasksAsExecutor = user.getTasksAsExecutor();

        // delete all comments
        commentService.deleteAllByAuthor(userId);

        // delete all tasks by user
        taskService.deleteAllByAuthor(userId, tasksAsAuthor, tasksAsExecutor);

        // update User DB
        repository.deleteById(userId);
    }
}
