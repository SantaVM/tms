package org.santavm.tms.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.santavm.tms.model.User;

import java.util.Set;
import java.util.TreeSet;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserDTO {
    private Long id;
    @NotBlank
    @Size(min=3, max=20)
    private String firstName;
    @NotBlank
    @Size(min=3, max=20)
    private String lastName;
    @NotEmpty
    @Email
    private String email;
    @NotBlank
    @Size(min=3, max=20)
    private String password;
    private User.Role role;
    private Set<Long> tasksAsAuthor;
    private Set<Long> tasksAsExecutor;
}
