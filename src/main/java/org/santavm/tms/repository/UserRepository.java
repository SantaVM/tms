package org.santavm.tms.repository;

import org.santavm.tms.model.User;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.lang.NonNull;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Cacheable(value = "users", key = "#email")
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.executedTasks WHERE u.id = ?1")
    Optional<User> findByIdWithTasks(Long userId);

    @EntityGraph(attributePaths = {"comments"})
    Optional<User> findOneById(Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.authoredTasks WHERE u.id = ?1")
    Optional<User> findOneByIdWithTasks(Long id);

    Optional<User> findUserByEmail(String email);  // for caching purpose only
}
