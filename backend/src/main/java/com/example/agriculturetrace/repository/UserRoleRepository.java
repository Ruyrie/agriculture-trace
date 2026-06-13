package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.UserRole;
import com.example.agriculturetrace.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用户角色关联仓库。
 */
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    List<UserRole> findByIdUserId(String userId);

    void deleteByIdUserId(String userId);
}
