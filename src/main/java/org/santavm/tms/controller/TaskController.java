package org.santavm.tms.controller;

import io.jsonwebtoken.JwtException;
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
public class TaskController {

    private final TaskService service;

    @PostMapping("/create")
    public ResponseEntity<?> createTask(@Valid @RequestBody Task task, Authentication auth){
        Task savedTask = service.createTask(task, auth);
        return ResponseEntity.status(HttpStatus.CREATED).body("Task created successfully: " + savedTask.getId());
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<?> deleteTask(@PathVariable Long id, Authentication auth){
        service.deleteTask( id, auth );

        return ResponseEntity.status(HttpStatus.OK).body("Task deleted successfully: " + id);
    }

    @PutMapping("/update")
    public ResponseEntity<?> updateTask(@Valid @RequestBody Task task, Authentication auth){
        Task updatedTask = service.updateTask( task, auth );
        if(updatedTask.getId() == null){
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("You have no enough permissions to update this task: " + task.getId());
        }

        return ResponseEntity.status(HttpStatus.OK).body("Task updated successfully: " + updatedTask.getId());
    }

    // GET /tasks/by-author/2?authorId=1&page=0&size=10&sort=createdAt,desc
    @GetMapping("/by-author/{authorId}")
    public ResponseEntity<?> finAllByAuthorId(@PathVariable("authorId") Long authorId,
                                     @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        List<Task> fromDb = service.findAllByAuthorId(authorId, pageable);
        if( fromDb.get(0).getAuthorId() == null){
            ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There is no User with ID:" + authorId);
        }
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
