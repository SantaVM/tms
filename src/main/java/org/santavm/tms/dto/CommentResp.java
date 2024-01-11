package org.santavm.tms.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@NoArgsConstructor
@ToString
public class CommentResp {
    private Long id;
    private String content;
    private String author;
    private String task;
    private String createdAt;
    private String updatedAt;
}
