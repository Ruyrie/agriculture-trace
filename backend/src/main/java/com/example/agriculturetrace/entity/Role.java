package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色实体，角色名称固定使用 ROLE_ADMIN、ROLE_FARMER、ROLE_INSPECTOR。
 */
@Getter
@Setter
@Entity
@Table(name = "role")
public class Role {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String name;

    @Column(length = 128)
    private String description;
}
