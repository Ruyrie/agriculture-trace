package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.User;
import com.example.agriculturetrace.service.UserService;
import com.example.agriculturetrace.util.Result;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.UUID;

/**
 * 用户认证、个人中心和管理员用户管理接口。
 */
@RestController
@RequestMapping("/api")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /**
     * 返回当前登录用户的公开资料和主角色。
     * authentication 来自 Spring Security Session，不信任前端传来的用户 ID。
     */
    @GetMapping("/user/info")
    public Result<?> currentUser(Authentication authentication) {
        User user = userService.getByUsername(authentication.getName());
        return Result.success(userService.toUserInfo(user));
    }

    /**
     * 修改当前登录用户的昵称、手机号和头像等个人资料。
     */
    @PutMapping("/user/profile")
    public Result<?> updateProfile(Authentication authentication, @RequestBody User input) {
        User current = userService.getByUsername(authentication.getName());
        User updated = userService.updateProfile(current.getId(), input);
        return Result.success(userService.toUserInfo(updated));
    }

    /**
     * 当前用户修改自己的密码。
     * Service 会先校验 oldPassword，再用 BCrypt 重新加盐哈希保存 newPassword。
     */
    @PutMapping("/user/password")
    public Result<?> changePassword(Authentication authentication, @RequestBody Map<String, String> body) {
        User current = userService.getByUsername(authentication.getName());
        userService.changePassword(current.getId(), body.get("oldPassword"), body.get("newPassword"));
        return Result.success(null);
    }

    /**
     * 上传当前用户头像到运行目录 uploads，并返回可公开访问的 /uploads/** URL。
     */
    @PostMapping("/user/avatar")
    public Result<?> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".png";
        String filename = UUID.randomUUID() + extension;
        Path uploadDir = Path.of(System.getProperty("user.dir"), "uploads");
        Files.createDirectories(uploadDir);
        Files.write(uploadDir.resolve(filename), file.getBytes());
        String avatarUrl = "/uploads/" + filename;
        User current = userService.getByUsername(authentication.getName());
        User updated = userService.updateAvatar(current.getId(), avatarUrl);
        return Result.success(Map.of(
                "url", avatarUrl,
                "user", userService.toUserInfo(updated)
        ));
    }

    /**
     * 管理员分页查询用户列表，支持按用户名或手机号关键字检索。
     */
    @GetMapping("/users")
    public Result<?> users(@RequestParam(defaultValue = "1") int page,
                           @RequestParam(defaultValue = "10") int pageSize,
                           @RequestParam(required = false) String keyword) {
        Page<User> users = userService.list(keyword, page, pageSize);
        return Result.success(Map.of(
                "records", users.getContent().stream().map(userService::toUserInfo).toList(),
                "total", users.getTotalElements(),
                "page", users.getNumber() + 1,
                "pageSize", users.getSize()
        ));
    }

    /**
     * 管理员创建用户，并同时绑定角色。
     * 未传密码时 Service 会使用默认密码 123456 并进行 BCrypt 哈希。
     */
    @PostMapping("/users")
    public Result<?> createUser(@RequestBody Map<String, Object> body) {
        User user = new User();
        user.setUsername((String) body.get("username"));
        user.setNickname((String) body.get("nickname"));
        user.setPhone((String) body.get("phone"));
        user.setAvatar((String) body.get("avatar"));
        user.setEnabled((Boolean) body.getOrDefault("enabled", true));
        User saved = userService.create(user, (String) body.get("password"), (String) body.get("role"));
        return Result.success(userService.toUserInfo(saved));
    }

    /**
     * 管理员更新用户资料和角色。
     * 额外拦截“禁用当前登录账号”，避免管理员把自己锁在系统外。
     */
    @PutMapping("/users/{id}")
    public Result<?> updateUser(Authentication authentication, @PathVariable String id, @RequestBody Map<String, Object> body) {
        Boolean enabled = (Boolean) body.getOrDefault("enabled", true);
        if (isCurrentUser(authentication, id) && Boolean.FALSE.equals(enabled)) {
            return Result.error(400, "不能禁用当前登录账号");
        }
        User user = new User();
        user.setNickname((String) body.get("nickname"));
        user.setPhone((String) body.get("phone"));
        user.setAvatar((String) body.get("avatar"));
        user.setEnabled(enabled);
        User saved = userService.updateByAdmin(id, user, (String) body.get("role"));
        return Result.success(userService.toUserInfo(saved));
    }

    /**
     * 管理员重置指定用户密码；请求体为空时恢复默认密码 123456。
     */
    @PutMapping("/users/{id}/password")
    public Result<?> resetPassword(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String password = body == null ? "123456" : body.getOrDefault("password", "123456");
        userService.resetPassword(id, password);
        return Result.success(null);
    }

    /**
     * 管理员启用或禁用用户账号。
     * 和 updateUser 一样禁止禁用当前登录账号。
     */
    @PutMapping("/users/{id}/status")
    public Result<?> updateStatus(Authentication authentication, @PathVariable String id, @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        if (isCurrentUser(authentication, id) && !enabled) {
            return Result.error(400, "不能禁用当前登录账号");
        }
        userService.updateStatus(id, enabled);
        return Result.success(null);
    }

    /**
     * 管理员删除用户，并阻止删除当前登录账号。
     */
    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(Authentication authentication, @PathVariable String id) {
        if (isCurrentUser(authentication, id)) {
            return Result.error(400, "不能删除当前登录账号");
        }
        userService.deleteByAdmin(id);
        return Result.success(null);
    }

    /**
     * 判断请求中的目标用户是否就是当前 Session 用户。
     * 这个保护用于禁用、删除等高风险管理操作。
     */
    private boolean isCurrentUser(Authentication authentication, String userId) {
        User current = userService.getByUsername(authentication.getName());
        return current.getId().equals(userId);
    }
}
