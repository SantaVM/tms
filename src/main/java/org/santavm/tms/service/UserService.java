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

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository repository;
    private final PasswordEncoder encoder;
    private final JwtService jwtService;

    private final AuthenticationManager authenticationManager;

//    @Transactional
    public UserDTO register(UserDTO userDTO){
        User user = this.mapDtoToUser(userDTO);

        Optional<User> userFromDb = repository.findByEmail(userDTO.getEmail());

        if (userFromDb.isPresent()) {
            //TODO need better response
            // return new AuthResponse("This email is already registered: " + userFromDb.get().getEmail());
            User badUser = User.builder().email(userFromDb.get().getEmail()).build();
            return this.mapUserToDto(badUser);
        }

        User savedUser = repository.saveAndFlush(user);
        return this.mapUserToDto(savedUser);  // TODO тут падают тесты
//        String token = jwtService.generateToken(user.getEmail());
//        return new AuthResponse(token);
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

    public boolean existsById(Long id){
        return repository.existsById(id);
    }

    public Optional<User> findById(Long id){
        return repository.findById(id);
    }

    public void update(User user){
        repository.save(user);
    }

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

    //TODO cascade deletion to Tasks and Comments
    public void deleteUser(Long userId) {
        repository.deleteById(userId);
    }
}
