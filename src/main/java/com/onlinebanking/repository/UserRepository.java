package com.onlinebanking.repository;

import com.onlinebanking.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsername(@org.springframework.data.repository.query.Param("username") String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByPasswordResetToken(String token);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
