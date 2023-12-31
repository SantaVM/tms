package org.santavm.tms.repository;

import org.santavm.tms.model.Task;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long>, ListPagingAndSortingRepository<Task, Long> { // JpaSpecificationExecutor<Task>
    // !!! List as return type
    List<Task> findAllByAuthorId(Long authorId, Pageable pageable);

    List<Task> findAllByExecutorId(Long executorId, Pageable pageable);

    List<Task> findAllByStatus(Task.Status status, Pageable pageable);

    List<Task> findAllByPriority(Task.Priority priority, Pageable pageable);

    @Query("SELECT t FROM Task t WHERE " +
            "(:authorId IS NULL OR t.authorId = :authorId) AND " +
            "(:executorId IS NULL OR t.executorId = :executorId) AND " +
            "(:status IS NULL OR t.status = :status) AND " +
            "(:priority IS NULL OR t.priority = :priority)")
    List<Task> findByCriteria(@Param("authorId") Long authorId,
                              @Param("executorId") Long executorId,
                              @Param("status") Task.Status status,
                              @Param("priority") Task.Priority priority,
                              Pageable pageable);

    @Modifying
    @Transactional
    @Query("UPDATE Task t SET t.executorId = NULL WHERE t.id IN ?1")
    void clearExecutors(List<Long> idList);
}
