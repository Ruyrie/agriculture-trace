package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Role;
import com.example.agriculturetrace.entity.User;
import com.example.agriculturetrace.entity.UserRole;
import com.example.agriculturetrace.entity.UserRoleId;
import com.example.agriculturetrace.repository.RoleRepository;
import com.example.agriculturetrace.repository.UserRepository;
import com.example.agriculturetrace.repository.UserRoleRepository;
import com.example.agriculturetrace.util.Ids;
import com.example.agriculturetrace.util.TimeUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * 用户资料、角色和密码管理服务。
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 根据用户名获取用户实体，找不到时抛出异常。
     * 登录态接口和个人中心更新都通过用户名定位当前用户。
     */
    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }

    /**
     * 安全查询用户名：空值直接返回 Optional.empty，避免 Repository 收到无效条件。
     */
    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    /**
     * 查询用户的主角色名。
     * 当前系统一个用户主要绑定一个角色；没有绑定时默认农户角色，保证前端有稳定兜底。
     */
    public String getPrimaryRoleName(String userId) {
        return userRoleRepository.findByIdUserId(userId).stream()
                .findFirst()
                .flatMap(userRole -> roleRepository.findById(userRole.getId().getRoleId()))
                .map(Role::getName)
                .orElse("ROLE_FARMER");
    }

    /**
     * 将 User 实体转换成可返回前端的用户资料。
     * 注意这里故意不返回 password，避免 BCrypt 密文泄露到浏览器。
     */
    public Map<String, Object> toUserInfo(User user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("phone", user.getPhone());
        data.put("avatar", user.getAvatar());
        data.put("enabled", user.getEnabled());
        data.put("role", getPrimaryRoleName(user.getId()));
        return data;
    }

    /**
     * 管理员分页查询用户；keyword 会同时匹配用户名和手机号。
     */
    public Page<User> list(String keyword, int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), pageSize);
        if (keyword == null || keyword.isBlank()) {
            return userRepository.findAll(request);
        }
        return userRepository.findByUsernameContainingIgnoreCaseOrPhoneContaining(keyword, keyword, request);
    }

    /**
     * 创建用户：生成主键、用 BCrypt 保存密码哈希、设置启用状态和创建时间，然后绑定角色。
     */
    @Transactional
    public User create(User user, String rawPassword, String roleName) {
        user.setId(Ids.uuid32());
        user.setPassword(passwordEncoder.encode(rawPassword == null || rawPassword.isBlank() ? "123456" : rawPassword));
        user.setEnabled(user.getEnabled() == null || user.getEnabled());
        user.setCreateTime(TimeUtils.nowText());
        User saved = userRepository.save(user);
        bindRole(saved.getId(), roleName);
        return saved;
    }

    /**
     * 当前用户更新个人资料，只允许改昵称、手机号和非空头像。
     */
    @Transactional
    public User updateProfile(String id, User input) {
        User user = userRepository.findById(id).orElseThrow();
        user.setNickname(input.getNickname());
        user.setPhone(input.getPhone());
        if (input.getAvatar() != null) {
            user.setAvatar(input.getAvatar());
        }
        return userRepository.save(user);
    }

    /**
     * 更新用户头像 URL，通常由头像上传接口在文件保存成功后调用。
     */
    @Transactional
    public User updateAvatar(String id, String avatarUrl) {
        User user = userRepository.findById(id).orElseThrow();
        user.setAvatar(avatarUrl);
        return userRepository.save(user);
    }

    /**
     * 管理员更新用户资料和启用状态；当传入 roleName 时同步重新绑定角色。
     */
    @Transactional
    public User updateByAdmin(String id, User input, String roleName) {
        User user = userRepository.findById(id).orElseThrow();
        user.setNickname(input.getNickname());
        user.setPhone(input.getPhone());
        user.setAvatar(input.getAvatar());
        user.setEnabled(input.getEnabled());
        User saved = userRepository.save(user);
        if (roleName != null && !roleName.isBlank()) {
            bindRole(id, roleName);
        }
        return saved;
    }

    /**
     * 当前登录用户修改自己的密码。
     * 不再校验原密码（由 Controller 的图形验证码确认是本人操作），但仍限制长度，
     * 并通过 BCrypt 比对拦截“新密码与原密码相同”的无效修改。
     */
    @Transactional
    public void changeOwnPassword(String id, String newPassword) {
        User user = userRepository.findById(id).orElseThrow();
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new IllegalArgumentException("新密码长度需为 6-20 位");
        }
        // 新密码与原密码相同没有意义，且容易让用户误以为已更换，这里直接拦截。
        if (passwordEncoder.matches(newPassword, user.getPassword())) {
            throw new IllegalArgumentException("新密码不能与原密码相同");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 忘记密码场景：凭“用户名 + 注册手机号”校验身份后重置密码。
     * 不依赖登录态，由调用方（Controller）先完成图形验证码校验再进入这里，
     * 双重校验降低被脚本批量爆破的风险。身份不匹配时统一返回模糊提示，避免泄露账号是否存在。
     */
    @Transactional
    public void resetPasswordByIdentity(String username, String phone, String newPassword) {
        if (newPassword == null || newPassword.length() < 6 || newPassword.length() > 20) {
            throw new IllegalArgumentException("新密码长度需为 6-20 位");
        }
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("请填写注册手机号");
        }
        User user = findByUsername(username)
                .filter(candidate -> phone.equals(candidate.getPhone()))
                .orElseThrow(() -> new IllegalArgumentException("用户名与手机号不匹配"));
        if (Boolean.FALSE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    /**
     * 管理员重置密码；空密码回退到系统默认 123456，并同样以 BCrypt 哈希保存。
     */
    @Transactional
    public void resetPassword(String id, String rawPassword) {
        User user = userRepository.findById(id).orElseThrow();
        user.setPassword(passwordEncoder.encode(rawPassword == null || rawPassword.isBlank() ? "123456" : rawPassword));
        userRepository.save(user);
    }

    /**
     * 启用或禁用账号，Spring Security 加载用户时会读取 enabled 决定能否登录。
     */
    @Transactional
    public void updateStatus(String id, boolean enabled) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    /**
     * 管理员删除用户前先删除 user_role 关联，避免关联表残留孤儿关系。
     */
    @Transactional
    public void deleteByAdmin(String id) {
        User user = userRepository.findById(id).orElseThrow();
        userRoleRepository.deleteByIdUserId(id);
        userRepository.delete(user);
    }

    /**
     * 为用户绑定角色：先规范化角色名，再删除旧绑定并保存新绑定。
     */
    private void bindRole(String userId, String roleName) {
        String normalized = normalizeRole(roleName);
        Role role = roleRepository.findByName(normalized)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在：" + normalized));
        userRoleRepository.deleteByIdUserId(userId);
        UserRole relation = new UserRole();
        relation.setId(new UserRoleId(userId, role.getId()));
        userRoleRepository.save(relation);
    }

    /**
     * 统一角色名格式。前端可传 ADMIN/FARMER/INSPECTOR，也可传完整 ROLE_ADMIN。
     */
    private String normalizeRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "ROLE_FARMER";
        }
        return roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName.toUpperCase();
    }
}
