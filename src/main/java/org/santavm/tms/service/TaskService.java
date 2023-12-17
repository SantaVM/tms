package org.santavm.tms.service;

import lombok.RequiredArgsConstructor;
import org.santavm.tms.model.Task;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.TaskRepository;
import org.santavm.tms.repository.UserRepository;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional  // LOB field needs this
public class TaskService {
    private final TaskRepository repository;
    private final UserRepository userRepository;
    private final CommentService commentService;

    public List<Task> findAllByAuthorId(Long authorId, Pageable pageable){
        if( !userRepository.existsById(authorId) ){
            throw new NoSuchElementException("There is no User with id: " + authorId);
        }
        return repository.findAllByAuthorId(authorId, pageable);
    }

    public Task createTask(Task task, Authentication auth) {
        Optional<User> authorUser = userRepository.findById(task.getAuthorId());
        Optional<User> executorUser = Optional.empty();

        // Only User-Author can delete Task
        Long userId = this.extractUserId(auth);
        boolean isAuthor = userId.equals(task.getAuthorId());
        if( !isAuthor ){
            throw new CustomPermissionException("You are not the author of this task: " + task.getAuthorId());
        }

        if (authorUser.isEmpty()){
            return this.badTask();
        }
        if(task.getExecutorId() != null){
            executorUser = userRepository.findById(task.getExecutorId());
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
            userRepository.save(executor);
        }
        userRepository.save(author);

        return saved;
    }

    public List<Task> findByCriteria(Long authorId, Long executorId, Task.Status status, Task.Priority priority, Pageable pageable) {
        return repository.findByCriteria(authorId, executorId, status, priority, pageable);
    }

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
        User author = userRepository.findById(authorId).orElseThrow(); // should exist
        author.removeTaskAsAuthor(id);
        if ( authorId.equals(executorId) ) author.removeTaskAsExecutor(id);
        userRepository.save(author);

        if ( !authorId.equals(executorId)) {
            if(executorId != null){
                User executor = userRepository.findById(executorId).orElseThrow(); // should exist
                executor.removeTaskAsExecutor(id);
                userRepository.save(executor);
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
                User newExecutorUser = userRepository.findById(task.getExecutorId())
                        .orElseThrow(() -> new NoSuchElementException("There is no User with id: "+task.getExecutorId()) );
                newExecutorUser.addTaskAsExecutor(task.getId());
                userRepository.save(newExecutorUser);
                // update this Task
                fromDb.setExecutorId(task.getExecutorId());
            } else if (task.getExecutorId() == null) {  // just delete current executor

                // updating User DB
                User oldExecutorUser = userRepository.findById(fromDb.getExecutorId()).orElseThrow();
                oldExecutorUser.removeTaskAsExecutor(task.getId());
                userRepository.save(oldExecutorUser);
                // update this Task
                fromDb.setExecutorId(task.getExecutorId());
            } else { // change current executor
                // update User DB
                User newExecutorUser = userRepository.findById(task.getExecutorId()).orElseThrow();
                newExecutorUser.addTaskAsExecutor(task.getId());
                userRepository.save(newExecutorUser);

                User oldExecutorUser = userRepository.findById(fromDb.getExecutorId()).orElseThrow();
                oldExecutorUser.removeTaskAsExecutor(task.getId());
                userRepository.save(oldExecutorUser);
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

    //when Author user deleted
    public void deleteAllByAuthor(Long authorId, Set<Long> asAuthor, Set<Long> asExecutor){

        // update every task with THIS User as executor
        List<Long> tasksToClearAsExecutor = new ArrayList<>(asExecutor);
        repository.clearExecutors(tasksToClearAsExecutor);

        // then update every NotNull executor User in every deleted task
        List<Long> tasksToDelete = new ArrayList<>(asAuthor);
        for(Long taskId : tasksToDelete){
            Task task = repository.findById(taskId).orElseThrow();
            Long exId = task.getExecutorId();
            if( exId != null){
                User executor = userRepository.findById(exId).orElseThrow();
                executor.removeTaskAsExecutor(taskId);
                userRepository.save(executor);
            }
        }

        // then delete comments for the Tasks
        for(Long taskId : tasksToDelete){
            commentService.deleteAllByTask(taskId);
        }

        // then delete all tasks
        repository.deleteAllByIdInBatch(tasksToDelete);
    }

    public boolean existsById(Long taskId) {
        return repository.existsById(taskId);
    }
}
