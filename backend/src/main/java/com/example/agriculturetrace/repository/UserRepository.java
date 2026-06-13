package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 用户 JPA 仓库。
 */
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByUsername(String username);

    Page<User> findByUsernameContainingIgnoreCaseOrPhoneContaining(String username, String phone, Pageable pageable);
}
