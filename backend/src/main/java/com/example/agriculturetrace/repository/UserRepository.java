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

    /**
     * 按用户名查询用户，登录认证和当前用户资料读取都依赖它。
     */
    Optional<User> findByUsername(String username);

    /**
     * 管理员用户列表的关键字查询，同时匹配用户名和手机号。
     */
    Page<User> findByUsernameContainingIgnoreCaseOrPhoneContaining(String username, String phone, Pageable pageable);
}
