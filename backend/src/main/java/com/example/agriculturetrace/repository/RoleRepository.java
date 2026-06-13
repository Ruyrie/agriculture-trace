package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * 角色 JPA 仓库。
 */
public interface RoleRepository extends JpaRepository<Role, String> {

    Optional<Role> findByName(String name);
}
