package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Objects;

/**
 * 用户角色关联表复合主键。
 */
@Getter
@Setter
@Embeddable
public class UserRoleId implements Serializable {

    @Column(name = "user_id", length = 32)
    private String userId;

    @Column(name = "role_id", length = 32)
    private String roleId;

    /**
     * JPA 反射创建复合主键对象时需要无参构造器。
     */
    public UserRoleId() {
    }

    /**
     * 便捷构造用户-角色复合主键。
     */
    public UserRoleId(String userId, String roleId) {
        this.userId = userId;
        this.roleId = roleId;
    }

    /**
     * 复合主键相等性：userId 和 roleId 同时相等才表示同一条关联。
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof UserRoleId that)) {
            return false;
        }
        return Objects.equals(userId, that.userId) && Objects.equals(roleId, that.roleId);
    }

    /**
     * 与 equals 保持一致的哈希码，用于 JPA 和集合结构正确识别复合主键。
     */
    @Override
    public int hashCode() {
        return Objects.hash(userId, roleId);
    }
}
