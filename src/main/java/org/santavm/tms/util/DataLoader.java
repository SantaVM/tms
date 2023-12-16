package org.santavm.tms.util;

import lombok.RequiredArgsConstructor;
import org.santavm.tms.model.Task;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.TaskRepository;
import org.santavm.tms.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

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

    @Override
    public void run(String... args) throws Exception {
        var admin = User.builder()
                .firstName("Admin")
                .lastName("Admin")
                .email("adm@site.com")
                .password("123")
                .role(User.Role.ADMIN)
                .tasksAsAuthor(new TreeSet<>())
                .tasksAsExecutor(new TreeSet<>())
                .build();
        User savedAdmin = userRepository.saveAndFlush(admin);
        System.out.println("Admin saved, id: " + savedAdmin.getId());

        var user = User.builder()
                .firstName("User")
                .lastName("User")
                .email("user@site.com")
                .password("123")
                .role(User.Role.USER)
                .tasksAsAuthor(new TreeSet<>())
                .tasksAsExecutor(new TreeSet<>())
                .build();
        User savedUser = userRepository.saveAndFlush(user);
        System.out.println("User saved, id: " + savedUser.getId());

        var task_1 = Task.builder()
                .authorId(savedAdmin.getId())
                .executorId(savedUser.getId())
                .title("First Task")
                .description("The very first Task from CommandLineRunner")
                .status(Task.Status.IN_PROGRESS)
                .priority(Task.Priority.HIGH).build();
        Task task_1Saved = taskRepository.saveAndFlush(task_1);
        System.out.println("Task_1 saved, id: " + task_1Saved.getId());

        var task_2 = Task.builder()
                .authorId(savedAdmin.getId())
                .executorId(savedAdmin.getId())
                .title("Second Task")
                .description("Second Task with Admin as executor from CommandLineRunner")
                .status(Task.Status.IN_PROGRESS)
                .priority(Task.Priority.HIGH).build();
        Task task_2Saved = taskRepository.saveAndFlush(task_2);
        System.out.println("Task_2 saved, id: " + task_2Saved.getId());

        // update users
        savedAdmin.addTaskAsAuthor(task_1Saved.getId());
        savedAdmin.addTaskAsAuthor(task_2Saved.getId());
        savedAdmin.addTaskAsExecutor(task_2Saved.getId());

        savedUser.addTaskAsExecutor(task_1Saved.getId());

        userRepository.save(savedAdmin);
        userRepository.save(savedUser);
    }
}
