package com.example.agriculturetrace.entity;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户与角色的多对多关联实体，对应数据库 user_role 表。
 *
 * 采用独立桥接表而非 @ManyToMany 注解，以保留未来扩展关联字段（如生效时间）的灵活性。
 * 当前一个用户只绑定一个角色，UserService.getPrimaryRoleName() 取第一条关联记录的角色。
 *
 * 复合主键由 UserRoleId（userId + roleId）构成，通过 @EmbeddedId 嵌入。
 *
 * 关联关系：
 *   - UserService.bindRole()：先删除用户旧角色关联，再保存新关联（deleteByIdUserId + save）。
 *   - UserService.deleteByAdmin()：删除用户前先删除 user_role 关联，避免孤儿记录。
 *   - CustomUserDetailsService：查询用户所有角色关联后逐一转换为 SimpleGrantedAuthority。
 *   - UserRoleRepository 提供按 userId 查询和批量删除方法。
 */
@Getter
@Setter
@Entity
@Table(name = "user_role")
public class UserRole {

    /**
     * 复合主键，包含 userId 和 roleId 两个字段，由 @Embeddable 的 UserRoleId 承载。
     * JPA 要求嵌入主键类实现 Serializable 并正确重写 equals/hashCode（UserRoleId 已实现）。
     */
    @EmbeddedId
    private UserRoleId id;
}
