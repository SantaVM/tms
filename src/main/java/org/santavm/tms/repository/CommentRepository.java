package org.santavm.tms.repository;

import org.santavm.tms.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, ListPagingAndSortingRepository<Comment, Long> {
    Page<Comment> findAllByTaskId(Long taskId, Pageable pageable);

    Page<Comment> findAllByAuthorId(Long authorId, Pageable pageable);

    Page<Comment> findAllByAuthorIdAndTaskId(Long authorId, Long taskId, Pageable pageable);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.authorId = ?1")
    void deleteCommentsByAuthorId(Long authorId);

    @Modifying
    @Transactional
    @Query("DELETE FROM Comment c WHERE c.taskId = ?1")
    void deleteCommentsByTaskId(Long taskId);
}
