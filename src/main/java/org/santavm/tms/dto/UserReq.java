package org.santavm.tms.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.santavm.tms.model.User;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UserReq {

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
}
