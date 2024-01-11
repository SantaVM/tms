package org.santavm.tms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.santavm.tms.model.Task;

@Getter
@Setter
@AllArgsConstructor
public class TaskReq {
    @NotBlank
    @Size(min = 3, max = 255)
    private String title;
    @NotBlank
    @Size(min = 3, max = 255)
    private String description;
    @NotNull
    private Task.Status status;
    @NotNull
    private Task.Priority priority;

    private Long executorId;
}
