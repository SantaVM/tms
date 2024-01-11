package org.santavm.tms.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.hibernate.Hibernate;
import org.hibernate.Session;
import org.santavm.tms.dto.TaskReq;
import org.santavm.tms.dto.TaskResp;
import org.santavm.tms.model.Task;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.TaskRepository;
import org.santavm.tms.repository.UserRepository;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository repository;
    private final UserRepository userRepository;

    public List<TaskResp> findAllByAuthorId(Long authorId, Pageable pageable){
        if( !userRepository.existsById(authorId) ){
            throw new NoSuchElementException("There is no User with id: " + authorId);
        }
        List<Task> taskList = repository.findAllByAuthorId(authorId, pageable);
        return taskList.stream().map(this::toResponse).toList();
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "user_resp", condition = "#taskReq.executorId ne null", key = "#taskReq.executorId"),
            @CacheEvict(value = "user_resp", key = "#result.author.id")
    })
    public Task createTask(TaskReq taskReq, Authentication auth) {

        Long userId = this.extractUserId(auth);
        User authorUser = userRepository.getReferenceById(userId);

        Task newTask = this.toTask(taskReq);

        User executor;
        Long executorId = taskReq.getExecutorId();

        if(executorId != null){
            executor = userRepository.findById(executorId).orElseThrow(
                    () -> new NoSuchElementException("ERROR: There is no User with executorId: " + executorId));
            newTask.setExecutor(executor);
        }

        newTask.setAuthor(authorUser);
        newTask.setCreatedAt(new Date());

        return repository.save(newTask);
    }

    private Task toTask(TaskReq taskReq) {
        Task task = new Task();
        task.setTitle(taskReq.getTitle());
        task.setDescription(taskReq.getDescription());
        task.setStatus(taskReq.getStatus());
        task.setPriority(taskReq.getPriority());

        return task;
    }

    private TaskResp toResponse(Task task){
        TaskResp response = new TaskResp();
        response.setId(task.getId());
        response.setTitle(task.getTitle());
        response.setDescription(task.getDescription());
        response.setStatus(task.getStatus());
        response.setPriority(task.getPriority());
        response.setCreatedAt( this.toLocalDateTime( task.getCreatedAt() ) );
        response.setUpdatedAt( this.toLocalDateTime( task.getUpdatedAt() ) );
        if (Hibernate.isInitialized(task.getAuthor())){
            User author = task.getAuthor();
            response.setAuthor("id: " + author.getId()
                    + ", name: " + author.getFirstName()
                    + ", surname: " + author.getLastName());
        } else {
            response.setAuthor("id: " + task.getAuthorId());
        }
        if (Hibernate.isInitialized(task.getExecutor())){
            User executor = task.getExecutor();
            if (executor != null) {
                response.setExecutor("id: " + executor.getId()
                        + ", name: " + executor.getFirstName()
                        + ", surname: " + executor.getLastName());
            } else {
                response.setExecutor("no executor assigned");
            }
        } else {
            response.setExecutor("id: " + task.getExecutorId());
        }
        if (Hibernate.isInitialized(task.getComments())) {
            response.setComments(task.getComments().size() + " comment(s)");
        } else {
            response.setComments("undefined");
        }
        return response;
    }

    public List<TaskResp> findByCriteria(Long authorId, Long executorId, Task.Status status, Task.Priority priority, Pageable pageable) {
        List<Task> taskList = repository.findByCriteria(authorId, executorId, status, priority, pageable);
        return taskList.stream().map(this::toResponse).toList();
    }

    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#id"),
            @CacheEvict(value = "user_resp", condition = "#result.executorId ne null", key = "#result.executorId"),
            @CacheEvict(value = "user_resp", key = "#result.authorId")
    })
    public Task deleteTask(Long id, Authentication auth) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("There is no Task with id: "+id));

        Long authorId = task.getAuthorId();

        // Only User-Author can delete Task
        Long userId = this.extractUserId(auth);
        boolean isAuthor = userId.equals(authorId);
        if( !isAuthor ){
            throw new CustomPermissionException("You have no permission to delete this task: " + id);
        }

        // update Task DB
        repository.deleteById(id);

        return task;    // for caching purpose only
    }

    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "tasks", key = "#id"),
            @CacheEvict(value = "user_resp", allEntries = true)
    })
    public Long updateTask(Long id, TaskReq taskReq, Authentication auth) {

        Task fromDb = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("There is no Task with id: " + id) );
        Task newTask = this.toTask(taskReq); // no executor inside

        Long authorId = fromDb.getAuthorId();
        Long executorId = fromDb.getExecutorId();
        // check permissions for action
        Long userId = this.extractUserId(auth);
        boolean isAuthor = userId.equals(authorId);
        boolean isExecutor = userId.equals(executorId);

        HashSet<String> fieldsChanged = fromDb.fieldsChanged(newTask);

        // Author can update any field except "id", "authorId", "createdAt" and "updatedAt"
        // Executor can update only "status" field
        if( !isAuthor ){
            if( !isExecutor ) throw new CustomPermissionException("You have no permission to update this task: " + id);
            if( fieldsChanged.size() == 1 && fieldsChanged.contains("status") ){
                fromDb.setStatus(newTask.getStatus());
                fromDb.setUpdatedAt(new Date());

                return repository.save(fromDb).getId();
            } else {
                throw new CustomPermissionException("You have permission to update ONLY \"status\" for this task: " + id);
            }
        }

        Long newExecutorId = taskReq.getExecutorId();
        User newExecutor;

        if ( !Objects.equals(newExecutorId, executorId) ) {  // Executor changed

            if ( newExecutorId != null ) {
                newExecutor = userRepository.findById(newExecutorId).orElseThrow(
                        () -> new NoSuchElementException("There is no Executor User with id: " + newExecutorId) );

                fromDb.setExecutor(newExecutor);
            } else {  // Executor deleted
                fromDb.setExecutor(null);
            }
        }

        if(fieldsChanged.contains("title")) fromDb.setTitle(newTask.getTitle());
        if(fieldsChanged.contains("description")) fromDb.setDescription(newTask.getDescription());
        if(fieldsChanged.contains("priority")) fromDb.setPriority(newTask.getPriority());
        if(fieldsChanged.contains("status")) fromDb.setStatus(newTask.getStatus());

        fromDb.setUpdatedAt(new Date());

        return repository.save(fromDb).getId();
    }

    private Long extractUserId( Authentication auth){
        User user = null;
        if (auth instanceof UsernamePasswordAuthenticationToken token) {
            user = (User) token.getPrincipal();
        }
        assert user != null;
        return user.getId();
    }

    public List<TaskResp> findAllByExecutorId(Long executorId, Pageable pageable) {
        if( !userRepository.existsById(executorId) ){
            throw new NoSuchElementException("There is no User with id: " + executorId);
        }
        List<Task> taskList = repository.findAllByExecutorId(executorId, pageable);
        return taskList.stream().map(this::toResponse).toList();
    }

    public List<TaskResp> findAllByStatus(Task.Status status, Pageable pageable) {
        List<Task> taskList = repository.findAllByStatus(status, pageable);
        return taskList.stream().map(this::toResponse).toList();
    }

    public List<TaskResp> findAllByPriority(Task.Priority priority, Pageable pageable) {
        List<Task> taskList = repository.findAllByPriority(priority, pageable);
        return taskList.stream().map(this::toResponse).toList();
    }

    @Cacheable(value = "tasks", key = "#id")
    public TaskResp findOne(Long id) {
        Task task = repository.findFullTask(id).orElseThrow(
                () -> new NoSuchElementException("There is no Task with id: " + id) );
        return this.toResponse(task);
    }

    private String toLocalDateTime(Date date){
        if(date == null ) return "not updated";
        Instant instant = date.toInstant();
        LocalDateTime localDateTime = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return localDateTime.format(formatter);
    }

    public List<TaskResp> findAll(Pageable pageable) {
        List<Task> taskList = repository.findAll(pageable).getContent();
        return taskList.stream().map(this::toResponse).toList();
    }

    public List<TaskResp> findFullTasks(Pageable pageable) {
        List<Task> taskList = repository.findAllBy(pageable).getContent();
        return taskList.stream().map(this::toResponse).toList();
    }
}
