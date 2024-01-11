package org.santavm.tms.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentReq {
    @NotNull
    private Long taskId;
    @NotBlank
    @Size(min = 3, max = 255)
    private String content;
}
