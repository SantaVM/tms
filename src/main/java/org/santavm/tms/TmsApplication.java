package org.santavm.tms;

import org.santavm.tms.dto.UserDTO;
import org.santavm.tms.model.Task;
import org.santavm.tms.model.User;
import org.santavm.tms.service.TaskService;
import org.santavm.tms.service.UserService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TmsApplication {

    public static void main(String[] args) {
        SpringApplication.run(TmsApplication.class, args);
    }

}
