package org.santavm.tms.service;

import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.santavm.tms.dto.AuthRequest;
import org.santavm.tms.dto.AuthResponse;
import org.santavm.tms.dto.UserReq;
import org.santavm.tms.dto.UserResp;
import org.santavm.tms.model.Comment;
import org.santavm.tms.model.Task;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.TaskRepository;
import org.santavm.tms.repository.UserRepository;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;
    private final TaskRepository taskRepository;

    private final AuthenticationManager authenticationManager;

    public Long register(UserReq userReq){

        User user = this.mapDtoToUser(userReq);

        Optional<User> userFromDb = repository.findUserByEmail(userReq.getEmail());

        if (userFromDb.isPresent()) {
            throw new NoSuchElementException("ERROR: Email already registered: " + userReq.getEmail());
        }

        User savedUser = repository.saveAndFlush(user);
        return savedUser.getId();
    }

    public AuthResponse login(AuthRequest authRequest){
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        authRequest.getEmail(),
                        authRequest.getPassword()
                ));
        User user = (User) auth.getPrincipal();
        String token = jwtService.generateToken(user.getEmail());
        return new AuthResponse(user.getId(), token);
    }

    public List<UserResp> getAll() {
        List<User> userList = repository.findAll();
        return userList.stream().map(this::toResponse).toList();
    }

    // we need 3 calls here to avoid MultipleBagFetchException and Cartesian product
    @Cacheable(value = "user_resp", key = "#id")
    @Transactional(readOnly = true)
    public UserResp getById(Long id){
        // all 3 users merged in one persistence context
        User user = repository.findByIdWithTasks(id).orElseThrow(
                () -> new NoSuchElementException("There is no User with id: " + id)
        );
        User user1 = repository.findOneById(id).orElseThrow();
        User user2 = repository.findOneByIdWithTasks(id).orElseThrow();
        return this.toResponse(user2);
    }

    public UserResp toResponse(User user){
        UserResp resp = new UserResp();
        resp.setId(user.getId());
        resp.setFirstName(user.getFirstName());
        resp.setLastName(user.getLastName());
        resp.setEmail(user.getEmail());
        resp.setRole(user.getRole());
        if (Hibernate.isInitialized( user.getAuthoredTasks() )) {
            List<Task> tasks = user.getAuthoredTasks();
            List<String> taskList = new ArrayList<>();
            for (Task t : tasks) {
                taskList.add("id: " + t.getId() + ", title: " + t.getTitle());
            }
            resp.setAsAuthor(taskList);
        } else {
            resp.setAsAuthor(List.of("undefined: loaded lazily"));
        }
        if (Hibernate.isInitialized( user.getExecutedTasks() )) {
            List<Task> tasks = user.getExecutedTasks();
            List<String> taskList = new ArrayList<>();
            for (Task t : tasks) {
                taskList.add("id: " + t.getId() + ", title: " + t.getTitle());
            }
            resp.setAsExecutor(taskList);
        } else {
            resp.setAsExecutor(List.of("undefined: loaded lazily"));
        }
        if (Hibernate.isInitialized( user.getComments() )) {
            List<Comment> comments = user.getComments();
            List<String> commentList = new ArrayList<>();
            for (Comment c : comments) {
                commentList.add("id: " + c.getId() + ", to task: " + c.getTaskId());
            }
            resp.setComments(commentList);
        } else {
            resp.setComments(List.of("undefined: loaded lazily"));
        }


        return resp;
    }

    public User mapDtoToUser(UserReq userReq){
        return User.builder()
                .firstName(userReq.getFirstName())
                .lastName(userReq.getLastName())
                .email(userReq.getEmail())
                .password( encoder.encode( userReq.getPassword() ) )
                .role(userReq.getRole())  // or set constant role like "USER"
                .build();
    }

    // Only ADMIN  user can do this
    @Caching(evict = {
            @CacheEvict(value = "users", key = "#result.email"),
            @CacheEvict(value = "user_resp", key = "#userId"),
            @CacheEvict(value = "tasks", allEntries = true)
    })
    public User deleteUser(Long userId) {

        User user = repository.findByIdWithTasks(userId).orElseThrow(
                () -> new NoSuchElementException("There is no User with id: " + userId));

        // There is no orphan removal here!!!
        List<Long> executedTaskList = user.getExecutedTasks()
                .stream()
                .map(Task::getId)
                .toList();

        // clear executor field in affected tasks
        taskRepository.clearExecutors(executedTaskList);

        // update User DB
        repository.delete(user);

        return user;    // for caching purpose only
    }

    @Caching(evict = {
            @CacheEvict(value = "users", allEntries = true),
            @CacheEvict(value = "user_resp", key = "#result.id")
    })
    public UserResp updateUser(UserReq newUser, Authentication auth) {
        User current = (User) auth.getPrincipal();
        User fromDb = repository.findById( current.getId()).orElseThrow();
        fromDb.setFirstName(newUser.getFirstName());
        fromDb.setLastName(newUser.getLastName());
        fromDb.setEmail(newUser.getEmail());
        fromDb.setPassword( encoder.encode( newUser.getPassword() ));
        fromDb.setRole(newUser.getRole());
        User saved = repository.save(fromDb);
        return this.toResponse(saved);
    }
}
