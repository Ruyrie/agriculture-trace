package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户表实体。
 *
 * 密码字段存储 BCrypt 密文；角色通过 user_role 表关联，避免在用户表中硬编码角色。
 */
@Getter
@Setter
@Entity
@Table(name = "user")
public class User {

    @Id
    @Column(length = 32)
    private String id;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, length = 100)
    private String password;

    @Column(length = 64)
    private String nickname;

    @Column(length = 32)
    private String phone;

    @Column(length = 256)
    private String avatar;

    private Boolean enabled = true;

    @Column(name = "create_time", length = 19)
    private String createTime;
}
