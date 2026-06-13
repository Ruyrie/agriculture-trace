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

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("用户不存在"));
        List<SimpleGrantedAuthority> authorities = userRoleRepository.findByIdUserId(user.getId()).stream()
                .map(userRole -> roleRepository.findById(userRole.getId().getRoleId()))
                .flatMap(java.util.Optional::stream)
                .map(Role::getName)
                .map(SimpleGrantedAuthority::new)
                .toList();

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities(authorities)
                .disabled(Boolean.FALSE.equals(user.getEnabled()))
                .build();
    }
}
