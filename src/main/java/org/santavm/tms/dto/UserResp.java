package org.santavm.tms.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.santavm.tms.model.User;

import java.util.List;

@Getter
@Setter
@ToString
public class UserResp {
    private Long id;
    private String firstName;
    private String lastName;
    private String email;
    private User.Role role;
    private List<String> asAuthor;
    private List<String> asExecutor;
    private List<String> comments;
}
