package org.santavm.tms.service;

import lombok.RequiredArgsConstructor;
import org.santavm.tms.model.Comment;
import org.santavm.tms.model.User;
import org.santavm.tms.repository.CommentRepository;
import org.santavm.tms.repository.TaskRepository;
import org.santavm.tms.util.CustomPermissionException;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.NoSuchElementException;

@Service
@RequiredArgsConstructor
@Transactional
public class CommentService {
    private final CommentRepository repository;

    private final TaskRepository taskRepository;

    // when Task deleted
    public void deleteAllByTask(Long taskId){
        repository.deleteCommentsByTaskId(taskId);
        //update Users (too expensive)
    }

    // when Author deleted
    public void deleteAllByAuthor(Long authorId){
        repository.deleteCommentsByAuthorId(authorId);
    }

    public void deleteOne(Long id, Authentication auth){
        Comment comment = repository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("There is no Comment with id: "+id));
        Long authorId = comment.getAuthorId();

        // Only User-Author can delete Comment
        Long userId = this.extractUserId(auth);
        boolean isAuthor = userId.equals(authorId);
        if( !isAuthor ){
            throw new CustomPermissionException("You have no permission to delete this comment: " + id);
        }

        repository.deleteById(id);
    }

    public void update(Comment newComment, Authentication auth){
        Comment oldComment = repository.findById(newComment.getId())
                .orElseThrow(() -> new NoSuchElementException("There is no Comment with id: "+newComment.getId()));
        Long authorId = oldComment.getAuthorId();

        // Only User-Author can update Comment
        Long userId = this.extractUserId(auth);
        boolean isAuthor = userId.equals(authorId);
        if( !isAuthor ){
            throw new CustomPermissionException("You have no permission to update this comment: " + newComment.getId());
        }

        oldComment.setContent(newComment.getContent());
        oldComment.setUpdatedAt(new Date());

        repository.save(oldComment);
    }

    public Comment create(Comment comment, Authentication auth){
        // Only Logged User can create a Comment
        Long userId = this.extractUserId(auth);
        boolean isAuthor = userId.equals(comment.getAuthorId());
        if( !isAuthor ){
            throw new CustomPermissionException("You can not create comment with authorId: " + comment.getAuthorId());
        }

        // Check Task presence
        if( !taskRepository.existsById(comment.getTaskId())){
            throw new NoSuchElementException("There is no Task with taskId: " + comment.getTaskId());
        }

        return repository.saveAndFlush(comment);
    }

    private Long extractUserId(Authentication auth) {
        User user = null;
        if (auth instanceof UsernamePasswordAuthenticationToken token) {
            user = (User) token.getPrincipal();
        }
        assert user != null;
        return user.getId();
    }

    public List<Comment> findAllByTaskId(Long taskId, Pageable pageable) {
        return repository.findAllByTaskId(taskId, pageable).getContent();
    }

    public List<Comment> findAllByAuthorId(Long authorId, Pageable pageable) {
        return repository.findAllByAuthorId(authorId, pageable).getContent();
    }

    public List<Comment> findAllByTaskAndAuthor(Long taskId, Long authorId, Pageable pageable) {
        return repository.findAllByAuthorIdAndTaskId(authorId, taskId, pageable).getContent();
    }
}
