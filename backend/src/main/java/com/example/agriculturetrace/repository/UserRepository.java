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
     * 数据库里可能查得到用户，也可能查不到用户，Optional<User>表示：这里可能有一个 User，也可能没有
     */
    Optional<User> findByUsername(String username);

    /**
     * 按手机号查询用户，用于新增/编辑用户时给出友好的重复手机号提示。
     */
    Optional<User> findByPhone(String phone);

    /**
     * 管理员用户列表的关键字查询，同时匹配用户名和手机号。
     * Page<User>：分页后的用户结果，包含用户列表 + 总条数 + 总页数等信息
     * Pageable：分页查询条件，比如页码、每页大小、排序
     * Pageable 是规则，PageRequest 是真正创建出来的分页对象
     */
    Page<User> findByUsernameContainingIgnoreCaseOrPhoneContaining(String username, String phone, Pageable pageable);
}
