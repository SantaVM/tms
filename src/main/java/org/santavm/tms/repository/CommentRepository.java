package org.santavm.tms.repository;

import org.santavm.tms.model.Comment;
import org.santavm.tms.model.Task;
import org.santavm.tms.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long>, ListPagingAndSortingRepository<Comment, Long> {
    Page<Comment> findAllByTaskId(Long taskId, Pageable pageable);

    Page<Comment> findAllByAuthorId(Long authorId, Pageable pageable);

    Page<Comment> findAllByAuthorIdAndTaskId(Long authorId, Long taskId, Pageable pageable);
}
