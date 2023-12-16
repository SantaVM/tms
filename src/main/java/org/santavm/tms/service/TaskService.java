package org.santavm.tms.service;

import lombok.RequiredArgsConstructor;
import org.santavm.tms.model.Comment;
import org.santavm.tms.model.Task;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.TaskRepository;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional  // LOB field needs this
public class TaskService {
    private final TaskRepository repository;
    private final UserService userService;
    private final CommentService commentService;

    public List<Task> findAllByAuthorId(Long authorId, Pageable pageable){
        Optional<User> author = userService.findOne(authorId);
        if(author.isEmpty()){
            return Collections.singletonList(this.badTask());
        }
        return repository.findAllByAuthorId(authorId, pageable);
    }

    public Task createTask(Task task) {
        //TODO set author as logged in user? - Dataloader refactor see CommServ
        Optional<User> authorUser = userService.findOne(task.getAuthorId());
        Optional<User> executorUser = Optional.empty();
        if (authorUser.isEmpty()){
            return this.badTask();
        }
        if(task.getExecutorId() != null){
            executorUser = userService.findOne(task.getExecutorId());
            if(executorUser.isEmpty()){
                return this.badTask();
            }
        }
        Task saved = repository.saveAndFlush(task);

        User author = authorUser.get();
        author.addTaskAsAuthor(saved.getId());
        if(executorUser.isPresent()){
            User executor = executorUser.get();
            executor.addTaskAsExecutor(saved.getId());
            userService.update(executor);
        }
        userService.update(author);

        return saved;
    }

    public List<Task> findByCriteria(Long authorId, Long executorId, Task.Status status, Task.Priority priority, Pageable pageable) {
        return repository.findByCriteria(authorId, executorId, status, priority, pageable);
    }

    //TODO cascade delete comments
    public void deleteTask(Long id, Authentication auth) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("There is no Task with id: "+id)); //OK

        Long authorId = task.getAuthorId();
        Long executorId = task.getExecutorId();

        // Only User-Author can delete Task
        Long userId = this.extractUserId(auth);
        boolean isAuthor = userId.equals(authorId);
        if( !isAuthor ){
            throw new CustomPermissionException("You have no permission to delete this task: " + id);
        }

        // update Task DB
        repository.deleteById(id);
        //update Comment DB
        commentService.deleteAllByTask(id);

        // update User DB
        User author = userService.findOne(authorId).orElseThrow(); // should exist
        author.removeTaskAsAuthor(id);
        if ( authorId.equals(executorId) ) author.removeTaskAsExecutor(id);
        userService.update(author);

        if ( !authorId.equals(executorId)) {
            if(executorId != null){
                User executor = userService.findOne(executorId).orElseThrow(); // should exist
                executor.removeTaskAsExecutor(id);
                userService.update(executor);
            }
        }
    }

    public Task updateTask(Task task, Authentication auth) {

        Task fromDb = repository.findById(task.getId())
                .orElseThrow(() -> new NoSuchElementException("There is no Task with id: "+task.getId()) );

        Long authorId = fromDb.getAuthorId();
        Long executorId = fromDb.getExecutorId();
        // check permissions for action
        Long userId = this.extractUserId(auth);
        boolean isAuthor = userId.equals(authorId);
        boolean isExecutor = userId.equals(executorId);

        HashSet<String> fieldsChanged = fromDb.fieldsChanged(task);

        // Author can update any field except "id", "authorId", "createdAt" and "updatedAt"
        // Executor can update only "status" field
        if( !isAuthor ){
            if( !isExecutor ) throw new CustomPermissionException("You have no permission to update this task: " + task.getId());
            if( fieldsChanged.size() == 1 && fieldsChanged.contains("status") ){
                fromDb.setStatus(task.getStatus());
                fromDb.setUpdatedAt(new Date());

                return repository.save(fromDb);
            } else {
                throw new CustomPermissionException("You have no enough permissions to update this task: " + task.getId());
            }
        }

        if(fieldsChanged.contains("title")) fromDb.setTitle(task.getTitle());
        if(fieldsChanged.contains("description")) fromDb.setDescription(task.getDescription());
        if(fieldsChanged.contains("priority")) fromDb.setPriority(task.getPriority());
        if(fieldsChanged.contains("status")) fromDb.setStatus(task.getStatus());
        if(fieldsChanged.contains("executorId")){
            if(fromDb.getExecutorId() == null){ // was null, setting new executor

                // update User DB
                User newExecutorUser = userService.findOne(task.getExecutorId())
                        .orElseThrow(() -> new NoSuchElementException("There is no User with id: "+task.getExecutorId()) );
                newExecutorUser.addTaskAsExecutor(task.getId());
                userService.update(newExecutorUser);
                // update this Task
                fromDb.setExecutorId(task.getExecutorId());
            } else if (task.getExecutorId() == null) {  // just delete current executor

                // updating User DB
                User oldExecutorUser = userService.findOne(fromDb.getExecutorId()).orElseThrow();
                oldExecutorUser.removeTaskAsExecutor(task.getId());
                userService.update(oldExecutorUser);
                // update this Task
                fromDb.setExecutorId(task.getExecutorId());
            } else { // change current executor
                // update User DB
                User newExecutorUser = userService.findOne(task.getExecutorId()).orElseThrow();
                newExecutorUser.addTaskAsExecutor(task.getId());
                userService.update(newExecutorUser);

                User oldExecutorUser = userService.findOne(fromDb.getExecutorId()).orElseThrow();
                oldExecutorUser.removeTaskAsExecutor(task.getId());
                userService.update(oldExecutorUser);
                // update this Task
                fromDb.setExecutorId(task.getExecutorId());
            }
        }

        fromDb.setUpdatedAt(new Date());

        return repository.save(fromDb);
    }

    //TODO refactor this
    private Task badTask(){
        return Task.builder().build();
    }
    private Long extractUserId( Authentication auth){
        User user = null;
        if (auth instanceof UsernamePasswordAuthenticationToken token) {
            user = (User) token.getPrincipal();
        }
        assert user != null;
        return user.getId();
    }

    public boolean existsById(Long taskId) {
        return repository.existsById(taskId);
    }
}
