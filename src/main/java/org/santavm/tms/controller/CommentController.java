package org.santavm.tms.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.santavm.tms.dto.AuthResponse;
import org.santavm.tms.dto.CommentReq;
import org.santavm.tms.dto.CommentResp;
import org.santavm.tms.model.Comment;
import org.santavm.tms.service.CommentService;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/comments")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT Bearer")
public class CommentController {
    private final CommentService service;

    @Operation(
            description = "Add new Comment to TMS",
            summary = "Add new Comment to particular Task from authenticated User",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Comment created successfully",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "Comment created with id: 1")) }),
                    @ApiResponse(responseCode = "400", description = "Wrong taskId",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "There is no Task with taskId: 1")) }),
                    @ApiResponse(responseCode = "403", description = "Wrong authorId",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "You can not create comment with authorId: 1")) })
            }
    )
    @PostMapping("/create")
    public ResponseEntity<?> createComment(@RequestBody @Valid CommentReq comment, Authentication auth){
        Comment created = service.create(comment, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body("Comment created with id: " + created.getId());
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateComment(@PathVariable Long id, @RequestBody @Valid CommentReq comment, Authentication auth){
        Long updated = service.update(id, comment, auth);
        return ResponseEntity.status(HttpStatus.OK).body("Comment updated with id: " + updated);
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteComment(@PathVariable Long id, Authentication auth){
        service.deleteOne( id, auth);
        return ResponseEntity.status(HttpStatus.OK).body("Comment deleted with id: " + id);
    }

    @GetMapping("/by-task/{taskId}")
    public ResponseEntity<?> findAllByTaskId(@PathVariable Long taskId, Pageable pageable){
        List<CommentResp> commentList = service.findAllByTaskId(taskId, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(commentList);
    }

    @GetMapping("/by-author/{authorId}")
    public ResponseEntity<?> findAllByAuthorId(@PathVariable Long authorId, Pageable pageable){
        List<CommentResp> commentList = service.findAllByAuthorId(authorId, pageable);
        return ResponseEntity.status(HttpStatus.OK).body(commentList);
    }

    @GetMapping("/all")
    public ResponseEntity<?> findAll(Pageable pageable){
        List<CommentResp> commentList = service.findAllBy( pageable);
        return ResponseEntity.status(HttpStatus.OK).body(commentList);
    }

}
