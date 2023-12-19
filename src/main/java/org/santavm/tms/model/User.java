package org.santavm.tms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "user_table")
public class User implements UserDetails {
    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    @Size(min = 3, max=40)
    private String firstName;
    @NotBlank
    @Size(min = 3, max=60)
    private String lastName;
    @NotEmpty
    @Email
    private String email;
    @NotBlank
    @Size(min=3)
    private String password;

    @Enumerated(EnumType.STRING)
    private Role role;

    @ElementCollection  // объявляем как тип интерфейса - иначе Хибернейт не может сопоставить типы!
    private Set<Long> tasksAsAuthor = new TreeSet<>();

    public void addTaskAsAuthor(Long taskId){
        this.tasksAsAuthor.add(taskId);
    }
    public void removeTaskAsAuthor(Long taskId){
        this.tasksAsAuthor.remove(taskId);
    }

    @ElementCollection
    private Set<Long> tasksAsExecutor = new TreeSet<>();

    public void addTaskAsExecutor(Long taskId){
        this.tasksAsExecutor.add(taskId);
    }
    public void removeTaskAsExecutor(Long taskId){
        this.tasksAsExecutor.remove(taskId);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority( "ROLE_" + role.name() ) );
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }


    public static enum Role{
        USER,
        ADMIN,  // Only Admin can delete Users
    }

}
