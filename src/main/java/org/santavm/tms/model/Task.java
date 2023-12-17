package org.santavm.tms.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

@Entity
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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

    @NotNull
    private Long authorId;

    private Long executorId;

    @Temporal(TemporalType.TIMESTAMP)
    private Date createdAt;

    @Temporal(TemporalType.TIMESTAMP)
    private Date updatedAt;

/*    public boolean onlyStatusChanged(Task task) {

        if (!this.title.equals(task.getTitle())) return false;
        if (!this.description.equals(task.getDescription())) return false;
        if (this.priority != task.getPriority()) return false;
        return this.executorId != null ? this.executorId.equals(task.getExecutorId()) : task.getExecutorId() == null;
    }*/

    public HashSet<String> fieldsChanged(Task task){
        HashSet<String> fieldsList = new HashSet<>();

        if (!this.title.equals(task.getTitle())) fieldsList.add("title");
        if (!this.description.equals(task.getDescription())) fieldsList.add("description");
        if (this.status != task.getStatus()) fieldsList.add("status");
        if (this.priority != task.getPriority()) fieldsList.add("priority");
        if ( this.executorId != null
                ? !this.executorId.equals(task.getExecutorId())
                : task.getExecutorId() != null ) fieldsList.add("executorId");

        return fieldsList;
    }

    public static enum Status{
        ON_HOLD,
        IN_PROGRESS,
        COMPLETED,
    }

    public static enum Priority{
        HIGH,
        REGULAR,
        LOW,
    }

    @PrePersist
    void createdAtInit() {
        this.createdAt = new Date();
    }
}
