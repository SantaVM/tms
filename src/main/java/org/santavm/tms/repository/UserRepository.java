package org.santavm.tms.repository;

import org.santavm.tms.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.lang.NonNull;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Override
    @Transactional(timeout = 2) // для тестов - не помогло
    @NonNull
    <S extends User> S save(@NonNull S entity);

    Optional<User> findByEmail(String email);
}
