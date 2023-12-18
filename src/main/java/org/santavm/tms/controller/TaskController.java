package org.santavm.tms.controller;

import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.santavm.tms.model.Task;
import org.santavm.tms.service.TaskService;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@SecurityRequirement(name = "JWT Bearer")
public class TaskController {

    private final TaskService service;

    @Operation(
            description = "Add new Task to TMS",
            summary = "Add new Task from authenticated User with existing User as executor (optional)",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Task created successfully",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "Task created with id: 1")) }),
                    @ApiResponse(responseCode = "400", description = "Wrong executorId",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "There is no User with executorId: 3")) }),
                    @ApiResponse(responseCode = "403", description = "Wrong authorId",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "You are not the author of this task: 1")) })
            }
    )
    @PostMapping("/create")
    public ResponseEntity<?> createTask(@Valid @RequestBody Task task, Authentication auth){
        Task savedTask = service.createTask(task, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body("Task created with id: " + savedTask.getId());
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Authentication auth){
        service.deleteTask( id, auth );

        return ResponseEntity.status(HttpStatus.OK).body("Task deleted successfully: " + id);
    }

    @Operation(
            description = "Update existing Task",
            summary = "Author can update any field EXCEPT: \"id\", \"authorId\", \"createdAt\" and \"updatedAt\"."
                    + " Executor can update only \"status\" field.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Task updated successfully",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "Task updated successfully: 1")) }),
                    @ApiResponse(responseCode = "400", description = "Wrong taskId OR authorId OR executorId",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "There is no Task with id: 1")) }),
                    @ApiResponse(responseCode = "403", description = "Wrong authorId",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "You have no permission to update this task: 1")) })
            }
    )
    @PutMapping("/update")
    public ResponseEntity<?> updateTask(@Valid @RequestBody Task task, Authentication auth){

        Task updatedTask = service.updateTask( task, auth );

        return ResponseEntity.status(HttpStatus.OK).body("Task updated successfully: " + updatedTask.getId());
    }

    // GET /tasks/by-author/2?authorId=1&page=0&size=10&sort=createdAt,desc
    @GetMapping("/by-author/{authorId}")
    public ResponseEntity<?> finAllByAuthorId(@PathVariable("authorId") Long authorId,
                                     @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        List<Task> fromDb = service.findAllByAuthorId(authorId, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(fromDb);
    }

    @GetMapping("/criteria")
    public List<Task> findByCriteria(@RequestParam(required = false) Long authorId,
                                     @RequestParam(required = false) Long executorId,
                                     @RequestParam(required = false) Task.Status status,
                                     @RequestParam(required = false) Task.Priority priority,
                                     Pageable pageable) {
        return service.findByCriteria(authorId, executorId, status, priority, pageable);
    }

    // message from orElseThrow
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<String> handleNoSuchElementException(NoSuchElementException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    @ExceptionHandler(CustomPermissionException.class)
    public ResponseEntity<String> handleCustomPermissionException(CustomPermissionException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ex.getMessage());
    }

    // TODO doesn't work
    @ExceptionHandler(JwtException.class)
    public ResponseEntity<String> handleJwtException(JwtException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

    //
    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<String> handleUsernameNotFoundException(UsernameNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
    }

}
