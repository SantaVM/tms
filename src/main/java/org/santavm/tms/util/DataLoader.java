package org.santavm.tms.util;

import lombok.RequiredArgsConstructor;
import org.santavm.tms.dto.UserDTO;
import org.santavm.tms.model.Task;
import org.santavm.tms.model.User;
import org.santavm.tms.service.TaskService;
import org.santavm.tms.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(     // to disable running with tests
        prefix = "command.line.runner",
        value = "enabled",
        havingValue = "true",
        matchIfMissing = true)
public class DataLoader implements CommandLineRunner {
    private final UserService userService;
    private final TaskService taskService;

    @Override
    public void run(String... args) throws Exception {
        var admin = UserDTO.builder()
                .firstName("Admin")
                .lastName("Admin")
                .email("adm@site.com")
                .password("123")
                .role(User.Role.ADMIN)
                .build();
        UserDTO savedAdmin = userService.register(admin);
        System.out.println("Admin saved, id: " + savedAdmin.getId());

        var user = UserDTO.builder()
                .firstName("User")
                .lastName("User")
                .email("user@site.com")
                .password("123")
                .role(User.Role.USER)
                .build();
        UserDTO savedUser = userService.register(user);
        System.out.println("User saved, id: " + savedUser.getId());

        var task_1 = Task.builder()
                .authorId(savedAdmin.getId()) // .author(userService.mapDtoToUser(savedAdmin))
                .executorId(savedUser.getId())  // userService.mapDtoToUser(savedUser)
                .title("First Task")
                .description("The very first Task from CommandLineRunner")
                .status(Task.Status.IN_PROGRESS)
                .priority(Task.Priority.HIGH).build();
        Task task_1Saved = taskService.createTask(task_1);
        System.out.println("Task_1 saved, id: " + task_1Saved.getId());

        var task_2 = Task.builder()
                .authorId(savedAdmin.getId())
                .executorId(savedAdmin.getId())
                .title("Second Task")
                .description("Second Task with Admin as executor from CommandLineRunner")
                .status(Task.Status.IN_PROGRESS)
                .priority(Task.Priority.HIGH).build();
        Task task_2Saved = taskService.createTask(task_2);
        System.out.println("Task_2 saved, id: " + task_2Saved.getId());
    }
}
