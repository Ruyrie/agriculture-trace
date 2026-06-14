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

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow();
    }

    public Optional<User> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return userRepository.findByUsername(username);
    }

    public String getPrimaryRoleName(String userId) {
        return userRoleRepository.findByIdUserId(userId).stream()
                .findFirst()
                .flatMap(userRole -> roleRepository.findById(userRole.getId().getRoleId()))
                .map(Role::getName)
                .orElse("ROLE_FARMER");
    }

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

    public Page<User> list(String keyword, int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), pageSize);
        if (keyword == null || keyword.isBlank()) {
            return userRepository.findAll(request);
        }
        return userRepository.findByUsernameContainingIgnoreCaseOrPhoneContaining(keyword, keyword, request);
    }

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

    @Transactional
    public User updateAvatar(String id, String avatarUrl) {
        User user = userRepository.findById(id).orElseThrow();
        user.setAvatar(avatarUrl);
        return userRepository.save(user);
    }

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

    @Transactional
    public void changePassword(String id, String oldPassword, String newPassword) {
        User user = userRepository.findById(id).orElseThrow();
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new IllegalArgumentException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public void resetPassword(String id, String rawPassword) {
        User user = userRepository.findById(id).orElseThrow();
        user.setPassword(passwordEncoder.encode(rawPassword == null || rawPassword.isBlank() ? "123456" : rawPassword));
        userRepository.save(user);
    }

    @Transactional
    public void updateStatus(String id, boolean enabled) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(enabled);
        userRepository.save(user);
    }

    @Transactional
    public void deleteByAdmin(String id) {
        User user = userRepository.findById(id).orElseThrow();
        userRoleRepository.deleteByIdUserId(id);
        userRepository.delete(user);
    }

    private void bindRole(String userId, String roleName) {
        String normalized = normalizeRole(roleName);
        Role role = roleRepository.findByName(normalized)
                .orElseThrow(() -> new IllegalArgumentException("角色不存在：" + normalized));
        userRoleRepository.deleteByIdUserId(userId);
        UserRole relation = new UserRole();
        relation.setId(new UserRoleId(userId, role.getId()));
        userRoleRepository.save(relation);
    }

    private String normalizeRole(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            return "ROLE_FARMER";
        }
        return roleName.startsWith("ROLE_") ? roleName : "ROLE_" + roleName.toUpperCase();
    }
}
