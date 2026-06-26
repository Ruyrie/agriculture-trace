package com.example.agriculturetrace.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * 用户账号实体，对应数据库 user 表。
 *
 * 密码字段存储 BCrypt 加盐哈希，由 PasswordConfig 提供的 BCryptPasswordEncoder 处理。
 * 角色通过独立的 user_role 表（UserRole 实体）关联，使用多对多映射，
 * 避免在用户表中硬编码角色，便于后续扩展为多角色。
 * enabled 字段控制账号是否可以登录，Spring Security 的 loadUserByUsername 读取此字段。
 *
 * 关联关系：
 *   - UserRole（一对多，非直接持有）：通过 UserRoleRepository.findByIdUserId() 查询。
 *   - CustomUserDetailsService 在登录时读取用户和关联角色，构建 UserDetails 交给 Spring Security。
 *   - UserService 负责所有 CRUD、密码哈希、角色绑定和头像管理。
 *   - UserController 暴露当前用户接口（/api/user/info）和管理员用户管理接口（/api/users/**）。
 */
@Getter
@Setter
@Entity
@Table(name = "user")
public class User {

    /**
     * 用户唯一主键，32 位去横线 UUID，由 Ids.uuid32() 生成。
     * 用于关联 user_role 表（UserRoleId.userId）。
     */
    @Id
    @Column(length = 32)
    private String id;

    /**
     * 登录用户名，全局唯一，最长 64 字符，不能为空。
     * 只允许英文字母开头、由字母/数字/下划线组成、3-32 位（UserService.USERNAME_PATTERN 校验）。
     * Spring Security 在 loadUserByUsername 中以此字段查找用户。
     * UserRepository.findByUsername() 供认证、当前用户资料和忘记密码使用。
     */
    @Column(nullable = false, unique = true, length = 64)
    private String username;

    /**
     * BCrypt 加盐密码哈希，最长 100 字符（BCrypt 输出固定 60 字符，留有余量）。
     * 明文密码从不持久化，UserService 使用 passwordEncoder.encode() 哈希后存储。
     * 忘记密码和管理员重置密码也统一通过 passwordEncoder.encode() 更新。
     * toUserInfo() 刻意不输出此字段，避免哈希值泄露到前端。
     */
    @Column(nullable = false, length = 100)
    private String password;

    /**
     * 用户昵称，显示名，最长 64 字符，可为空。
     * 登录后右上角展示 username，个人中心展示 nickname（优先）。
     * 管理员和用户本人均可修改。
     */
    @Column(length = 64)
    private String nickname;

    /**
     * 手机号，最长 32 字符，可为空，但若填写则全库唯一。
     * 忘记密码功能需要用户名 + 手机号双重身份验证，所以注册时建议填写。
     * UserRepository.findByPhone() 用于注册/编辑时的重复检验。
     */
    @Column(length = 32)
    private String phone;

    /**
     * 头像相对 URL，如 /uploads/xxx.png，最长 256 字符，可为空。
     * 上传后由 UserController.uploadAvatar() 保存文件并更新此字段。
     * Layout.vue 从 localStorage.userInfo.avatar 读取并用 resolveAssetUrl() 拼接完整 URL。
     */
    @Column(length = 256)
    private String avatar;

    /**
     * 账号启用状态，默认为 true（启用）。
     * CustomUserDetailsService 在 loadUserByUsername 中读取此字段，
     * 若为 false 则 Spring Security 拒绝登录并抛 DisabledException。
     * 管理员通过 /api/users/{id}/status 接口控制此字段。
     * UserController 阻止管理员禁用自己当前登录的账号。
     */
    private Boolean enabled = true;

    /**
     * 账号创建时间，格式 yyyy-MM-dd HH:mm:ss，由 TimeUtils.nowText() 生成。
     * UserService.create() 在创建时写入，后续不再更新。
     */
    @Column(name = "create_time", length = 19)
    private String createTime;
}
