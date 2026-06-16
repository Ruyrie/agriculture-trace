package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.UserRole;
import com.example.agriculturetrace.entity.UserRoleId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 用户角色关联仓库。
 */
public interface UserRoleRepository extends JpaRepository<UserRole, UserRoleId> {

    /**
     * 查询用户绑定的所有角色关系。
     */
    List<UserRole> findByIdUserId(String userId);

    /**
     * 删除用户的所有角色关系，常用于重新绑定角色或删除用户前清理。
     */
    void deleteByIdUserId(String userId);
}
