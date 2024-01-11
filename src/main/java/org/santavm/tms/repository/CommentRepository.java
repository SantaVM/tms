package org.santavm.tms.repository;

import org.santavm.tms.model.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListPagingAndSortingRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {
    @EntityGraph(attributePaths = {"author", "task"})
    List<Comment> findAllByTaskId(Long taskId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "task"})
    List<Comment> findAllByAuthorId(Long authorId, Pageable pageable);
}
