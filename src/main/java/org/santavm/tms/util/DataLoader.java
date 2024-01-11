package org.santavm.tms.util;

import lombok.RequiredArgsConstructor;
import org.santavm.tms.model.Comment;
import org.santavm.tms.model.Task;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.CommentRepository;
import org.santavm.tms.repository.TaskRepository;
import org.santavm.tms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.TreeSet;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(     // to disable running with tests
        prefix = "command.line.runner",
        value = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DataLoader implements CommandLineRunner {
    private final UserRepository userRepository;
    private final TaskRepository taskRepository;
    private final PasswordEncoder passwordEncoder;
    private final CommentRepository commentRepository;

    @Override
    public void run(String... args) throws Exception {
        var admin = User.builder()
                .firstName("Admin")
                .lastName("Admin")
                .email("adm@site.com")
                .password(passwordEncoder.encode("123"))
                .role(User.Role.ADMIN)
                .build();
        User savedAdmin = userRepository.save(admin);
        System.out.println("Admin saved, id: " + savedAdmin.getId());

        var user = User.builder()
                .firstName("User")
                .lastName("User")
                .email("user@site.com")
                .password(passwordEncoder.encode("123"))
                .role(User.Role.USER)
                .build();
        User savedUser = userRepository.save(user);
        System.out.println("User saved, id: " + savedUser.getId());

        var task_1 = Task.builder()
                .author(savedAdmin)
                .executor(savedUser)
                .title("First Task")
                .description("The first Task from CommandLineRunner")
                .status(Task.Status.IN_PROGRESS)
                .priority(Task.Priority.HIGH)
                .createdAt(new Date())
                .build();
        Task task_1Saved = taskRepository.save(task_1);
        System.out.println("Task_1 saved, id: " + task_1Saved.getId());

        var task_2 = Task.builder()
                .author(savedAdmin)
                .executor(savedAdmin)
                .title("Second Task")
                .description("Second Task with Admin as executor from CommandLineRunner")
                .status(Task.Status.IN_PROGRESS)
                .priority(Task.Priority.HIGH)
                .createdAt(new Date())
                .build();
        Task task_2Saved = taskRepository.save(task_2);
        System.out.println("Task_2 saved, id: " + task_2Saved.getId());

        var comment_1 = Comment.builder()
                .task(task_1Saved)
                .author(savedAdmin)
                .content("First comment from 1 to the task 1")
                .createdAt(new Date())
                .build();
        var comment_2 = Comment.builder()
                .task(task_2Saved)
                .author(savedUser)
                .content("Second comment from 2 to the task 2")
                .createdAt(new Date())
                .build();
        Comment comment_1saved = commentRepository.save(comment_1);
        Comment comment_2saved = commentRepository.save(comment_2);
        System.out.println("Comment_1 saved: " + comment_1saved); // null for authorId and taskId here, but DB is ok
        System.out.println("Comment_2 saved: " + comment_2saved);

//        userRepository.save(savedAdmin);
//        userRepository.save(savedUser);
    }
}
