package org.santavm.tms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import org.hibernate.Hibernate;

import java.util.*;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
//@Table(indexes = {
//        @Index(name = "task_author_idx", columnList = "author")
//})
public class Task {
    @Id
    @GeneratedValue
    private Long id;

    @NotBlank
    @Size(max = 255)
    private String title;

    @NotBlank
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id")
    @NotNull
    private User author;

    @Column(name = "author_id", insertable = false, updatable = false)
    private Long authorId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "executor_id")
    private User executor;

    @Column(name = "executor_id", insertable = false, updatable = false)
    private Long executorId;

    @OneToMany(mappedBy = "task",
            cascade = CascadeType.ALL,
            orphanRemoval = true,
            fetch = FetchType.LAZY)
    @Builder.Default
    private List<Comment> comments = new ArrayList<>();

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

    public HashSet<String> fieldsChanged(Task task){
        HashSet<String> fieldsList = new HashSet<>();

        if (!this.title.equals(task.getTitle())) fieldsList.add("title");
        if (!this.description.equals(task.getDescription())) fieldsList.add("description");
        if (this.status != task.getStatus()) fieldsList.add("status");
        if (this.priority != task.getPriority()) fieldsList.add("priority");

        return fieldsList;
    }

    public enum Status{
        ON_HOLD,
        IN_PROGRESS,
        COMPLETED,
    }

    public enum Priority{
        HIGH,
        REGULAR,
        LOW,
    }

    public void setAuthor(User author){
        this.author = author;
        author.getAuthoredTasks().add(this);
    }

    public void setExecutor(User executor){
        if(executor == null){  // removing executor
            if(this.executor == null) return;
            this.executor.getExecutedTasks().remove(this);
            this.executor = null;
            return;
        }
        executor.getExecutedTasks().add(this);
        this.executor = executor;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o))
            return false;
        Task task = (Task) o;
        return Objects.equals(this.author.getId(), task.author.getId()) &&
                Objects.equals(this.createdAt, task.getCreatedAt());
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.authorId, createdAt);  // this.author.getId()
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "(" +
                "id = " + id + ", " +
                "title = " + title + ", " +
                "description = " + description + ", " +
                "status = " + status + ", " +
                "priority = " + priority + ", " +
                "author = " + authorId + ", " +  //  author.getId()
                "executor = " + executorId + ", " + // (executor != null ? executor.getId() : null)
                "createdAt = " + createdAt + ", " +
                "updatedAt = " + updatedAt + ")";
    }
}
