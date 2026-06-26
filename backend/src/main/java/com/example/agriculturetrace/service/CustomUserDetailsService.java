package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Role;
import com.example.agriculturetrace.entity.User;
import com.example.agriculturetrace.repository.RoleRepository;
import com.example.agriculturetrace.repository.UserRepository;
import com.example.agriculturetrace.repository.UserRoleRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Spring Security 用户加载服务。
 *
 * 登录时根据用户名加载用户、启用状态和角色权限，最终写入 Session。
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public CustomUserDetailsService(UserRepository userRepository,
                                    RoleRepository roleRepository,
                                    UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    /**
     * Spring Security 登录时调用的用户加载函数。
     * 根据用户名读出用户、BCrypt 密码哈希、启用状态和角色权限，交给框架完成密码比对和授权。
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // 根据用户名查找用户实体；找不到时抛 UsernameNotFoundException，
        // Spring Security 将其转为 HTTP 401 并触发登录失败处理器。
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));

        // 查出该用户在 user_role 关联表中的所有绑定行，再逐条获取角色名称：
        //   findByIdUserId → 返回该用户的所有 UserRole 记录；
        //   findById(roleId) → 根据角色 ID 查角色实体（Optional）；
        //   flatMap(Optional::stream) → 跳过角色 ID 找不到的异常记录；
        //   Role::getName → 取 "ROLE_ADMIN" / "ROLE_FARMER" / "ROLE_INSPECTOR" 等字符串；
        //   SimpleGrantedAuthority::new → 包装成 Spring Security 授权对象。
        List<SimpleGrantedAuthority> authorities = userRoleRepository.findByIdUserId(user.getId()).stream()
                .map(userRole -> roleRepository.findById(userRole.getId().getRoleId()))
                .flatMap(java.util.Optional::stream)
                .map(Role::getName)
                .map(SimpleGrantedAuthority::new)
                .toList();

        // 用 Spring Security 内置 builder 组装 UserDetails；框架用它完成 BCrypt 密码比对和权限绑定。
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())                       // 已 BCrypt 哈希的密码，框架自动比对
                .authorities(authorities)                           // 角色列表，决定接口访问权限
                .disabled(Boolean.FALSE.equals(user.getEnabled()))  // enabled=false → disabled=true
                .build();
    }
}
