package org.santavm.tms.controller;

import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.santavm.tms.dto.TaskReq;
import org.santavm.tms.dto.TaskResp;
import org.santavm.tms.model.Task;
import org.santavm.tms.service.TaskService;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.data.domain.PageRequest;
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
            description = "Add new Task from authenticated User with existing User as executor (optional)",
            summary = "Add new Task to TMS",
            responses = {
                    @ApiResponse(responseCode = "201", description = "Task created successfully",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "Task created with id: 1")) }),
                    @ApiResponse(responseCode = "400", description = "Wrong executorId",
                            content = { @Content(mediaType = "text/plain; charset=utf-8",
                                    schema = @Schema(example = "There is no User with executorId: 3")) })
            }
    )
    @PostMapping("/create")
    public ResponseEntity<?> createTask(@Valid @RequestBody TaskReq task, Authentication auth){
        Task savedTask = service.createTask(task, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body("Task created with id: " + savedTask.getId());
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Authentication auth){
        service.deleteTask( id, auth );

        return ResponseEntity.status(HttpStatus.OK).body("Task deleted successfully: " + id);
    }

    @Operation(
            description = "Author can update any field. Executor can update only \"status\" field.",
            summary = "Update existing Task",
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
    @PutMapping("/{id}/update")
    public ResponseEntity<?> updateTask(@PathVariable Long id, @Valid @RequestBody TaskReq task, Authentication auth){

        Long updatedTask = service.updateTask( id, task, auth );

        return ResponseEntity.status(HttpStatus.OK).body("Task updated successfully: " + updatedTask);
    }

    // GET /tasks/by-author/2?authorId=1&page=0&size=10&sort=createdAt,desc
    @GetMapping("/by-author/{authorId}")
    public ResponseEntity<?> finAllByAuthorId(@PathVariable("authorId") Long authorId,
                                              @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                                              Pageable pageable) {

        List<TaskResp> fromDb = service.findAllByAuthorId(authorId, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(fromDb);
    }

    @GetMapping("/by-executor/{executorId}")
    public ResponseEntity<?> finAllByExecutorId(@PathVariable("executorId") Long executorId,
                                                @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                                                Pageable pageable) {

        List<TaskResp> fromDb =service.findAllByExecutorId(executorId, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(fromDb);
    }

    @GetMapping("/by-status/{status}")
    public ResponseEntity<?> finAllByStatus(@PathVariable("status") Task.Status status,
                                            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                                            Pageable pageable) {

        List<TaskResp> fromDb =service.findAllByStatus(status, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(fromDb);
    }

    @GetMapping("/by-priority/{priority}")
    public ResponseEntity<?> finAllByPriority(@PathVariable("priority") Task.Priority priority,
                                              @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC)
                                              Pageable pageable) {

        List<TaskResp> fromDb = service.findAllByPriority(priority, pageable);

        return ResponseEntity.status(HttpStatus.OK).body(fromDb);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> findOne(@PathVariable Long id){
        return ResponseEntity.status(HttpStatus.OK).body(service.findOne(id));
    }

    @GetMapping("/criteria")
    public ResponseEntity<?> findByCriteria(@RequestParam(required = false) Long authorId,
                                     @RequestParam(required = false) Long executorId,
                                     @RequestParam(required = false) Task.Status status,
                                     @RequestParam(required = false) Task.Priority priority,
                                            Pageable pageable) {
        List<TaskResp> fromDb = service.findByCriteria(authorId, executorId, status, priority, pageable);
        return ResponseEntity.ok(fromDb);
    }

    @Operation(
            description = "Get all Tasks",
            summary = "Get all tasks \"lazily\""
    )
    @GetMapping("/lazy")
    public ResponseEntity<?> findAll(Pageable pageable){
        List<TaskResp> taskRespList = service.findAll(pageable);
        return ResponseEntity.ok(taskRespList);
    }

    @Operation(
            description = "Get all Tasks",
            summary = "Get all tasks \"eagerly\""
    )
    @GetMapping("/all")
    public ResponseEntity<?> findAllEagerly(@RequestParam(value = "page", defaultValue = "0") int page,
                                            @RequestParam(value = "size", defaultValue = "10") int size,
                                            @RequestParam(value = "sort", defaultValue = "id") String[] sort,
                                            @RequestParam(value = "direction", defaultValue = "ASC") Sort.Direction direction){
        PageRequest pageRequest = PageRequest.of(page, size, direction, sort);
        List<TaskResp> taskRespList = service.findFullTasks(pageRequest);
        return ResponseEntity.ok(taskRespList);
    }

}
