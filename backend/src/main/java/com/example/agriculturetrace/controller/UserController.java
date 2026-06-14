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

    @GetMapping("/user/info")
    public Result<?> currentUser(Authentication authentication) {
        User user = userService.getByUsername(authentication.getName());
        return Result.success(userService.toUserInfo(user));
    }

    @PutMapping("/user/profile")
    public Result<?> updateProfile(Authentication authentication, @RequestBody User input) {
        User current = userService.getByUsername(authentication.getName());
        User updated = userService.updateProfile(current.getId(), input);
        return Result.success(userService.toUserInfo(updated));
    }

    @PutMapping("/user/password")
    public Result<?> changePassword(Authentication authentication, @RequestBody Map<String, String> body) {
        User current = userService.getByUsername(authentication.getName());
        userService.changePassword(current.getId(), body.get("oldPassword"), body.get("newPassword"));
        return Result.success(null);
    }

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

    @PutMapping("/users/{id}/password")
    public Result<?> resetPassword(@PathVariable String id, @RequestBody(required = false) Map<String, String> body) {
        String password = body == null ? "123456" : body.getOrDefault("password", "123456");
        userService.resetPassword(id, password);
        return Result.success(null);
    }

    @PutMapping("/users/{id}/status")
    public Result<?> updateStatus(Authentication authentication, @PathVariable String id, @RequestBody Map<String, Boolean> body) {
        boolean enabled = body.getOrDefault("enabled", true);
        if (isCurrentUser(authentication, id) && !enabled) {
            return Result.error(400, "不能禁用当前登录账号");
        }
        userService.updateStatus(id, enabled);
        return Result.success(null);
    }

    @DeleteMapping("/users/{id}")
    public Result<?> deleteUser(Authentication authentication, @PathVariable String id) {
        if (isCurrentUser(authentication, id)) {
            return Result.error(400, "不能删除当前登录账号");
        }
        userService.deleteByAdmin(id);
        return Result.success(null);
    }

    private boolean isCurrentUser(Authentication authentication, String userId) {
        User current = userService.getByUsername(authentication.getName());
        return current.getId().equals(userId);
    }
}
