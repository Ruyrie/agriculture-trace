package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 角色实体，对应数据库 role 表。
 *
 * 系统预置三种角色，名称遵循 Spring Security 约定（ROLE_ 前缀）：
 *   - ROLE_ADMIN    管理员：可访问全部功能，包括用户管理
 *   - ROLE_FARMER   农户：可管理产品和批次，不可访问数据指纹/审计日志/统计分析
 *   - ROLE_INSPECTOR 监管员：可查看数据指纹、审计日志和统计分析，不可管理用户
 *
 * 角色权限在两个地方生效：
 *   1. SecurityConfig.authorizeHttpRequests() — 后端接口级 RBAC 拦截（权威来源）
 *   2. router/index.js 和 Layout.vue 的 meta.roles 过滤 — 前端菜单展示过滤（辅助体验）
 *
 * 关联关系：
 *   - UserRole（多对多桥接）：user_role 表存储用户与角色的关联关系。
 *   - CustomUserDetailsService 在登录时读取角色名，构建 SimpleGrantedAuthority 列表。
 *   - UserService.bindRole() 在创建/编辑用户时通过 RoleRepository.findByName() 查询角色并绑定。
 *   - RoleRepository 仅提供按名称查询方法。
 */
@Getter
@Setter
@Entity
@Table(name = "role")
public class Role {

    /**
     * 角色唯一主键，32 位去横线 UUID（由数据初始化脚本预置，运行期不创建新角色）。
     * UserRole 通过 UserRoleId.roleId 引用此字段。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 角色名称，全局唯一，不能为空，最长 64 字符。
     * 固定值为 ROLE_ADMIN / ROLE_FARMER / ROLE_INSPECTOR，与 Spring Security GrantedAuthority 名称一致。
     * UserService.normalizeRole() 将前端传入的简写（ADMIN/FARMER）统一补全为带 ROLE_ 前缀的形式。
     */
    @Column(nullable = false, unique = true, length = 64)
    private String name;

    /**
     * 角色说明文字，最长 128 字符，可为空。
     * 仅供管理员查看，当前版本前端未展示此字段。
     */
    @Column(length = 128)
    private String description;
}
