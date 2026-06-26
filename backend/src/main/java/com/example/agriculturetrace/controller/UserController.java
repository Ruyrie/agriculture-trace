package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.User;
import com.example.agriculturetrace.service.UserService;
import com.example.agriculturetrace.util.Result;
import com.example.agriculturetrace.util.UploadPaths;
import jakarta.servlet.http.HttpSession;
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
     * 无需输入原密码，改由图形验证码确认为本人操作；Service 仍校验长度并拦截“新旧密码相同”。
     */
    @PutMapping("/user/password")
    public Result<?> changePassword(Authentication authentication, @RequestBody Map<String, String> body, HttpSession session) {
        // 图形验证码确认是本人操作，防止他人趁登录态未锁屏时直接改密。
        validateCaptcha(session, body.get("captcha"));
        User current = userService.getByUsername(authentication.getName());
        userService.changeOwnPassword(current.getId(), body.get("newPassword"));
        return Result.success(null);
    }

    /**
     * 忘记密码：无需登录，凭“图形验证码 + 用户名 + 注册手机号”重置密码。
     * 先校验 Session 中的验证码（一次性、5 分钟有效），再交给 Service 校验身份并改密。
     */
    @PostMapping("/user/forgot-password")
    public Result<?> forgotPassword(@RequestBody Map<String, String> body, HttpSession session) {
        validateCaptcha(session, body.get("captcha"));
        userService.resetPasswordByIdentity(body.get("username"), body.get("phone"), body.get("newPassword"));
        return Result.success(null);
    }

    /**
     * 校验 Session 中的图形验证码：检查存在性、5 分钟有效期与大小写无关的内容匹配。
     * 验证码一次性使用，校验后立即作废；任何不通过都抛 IllegalArgumentException，由全局异常处理返回 400。
     */
    private void validateCaptcha(HttpSession session, String input) {
        Object expected = session.getAttribute(CaptchaController.CAPTCHA_CODE);
        Object issuedAt = session.getAttribute(CaptchaController.CAPTCHA_TIME);
        if (expected == null || issuedAt == null
                || System.currentTimeMillis() - (Long) issuedAt > CaptchaController.CAPTCHA_TTL_MILLIS) {
            throw new IllegalArgumentException("验证码已过期，请刷新后重试");
        }
        if (input == null || !((String) expected).equalsIgnoreCase(input.trim())) {
            throw new IllegalArgumentException("验证码错误");
        }
        session.removeAttribute(CaptchaController.CAPTCHA_CODE);
        session.removeAttribute(CaptchaController.CAPTCHA_TIME);
    }

    /**
     * 上传当前用户头像到运行目录 uploads，并返回可公开访问的 /uploads/** URL。
     */
    @PostMapping("/user/avatar")
    public Result<?> uploadAvatar(Authentication authentication, @RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) {
            return Result.error(400, "上传文件不能为空");
        }
        // 从浏览器上传的原始文件名提取扩展名，保留格式信息；无扩展名时默认 .png。
        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = originalName.contains(".") ? originalName.substring(originalName.lastIndexOf(".")) : ".png";
        // UUID 随机文件名防止同名文件互相覆盖，同时避免路径遍历漏洞（../）。
        String filename = UUID.randomUUID() + extension;
        // 头像直接存 uploads/ 根目录（与溯源图片的 uploads/trace-images/ 区分）。
        Path uploadDir = UploadPaths.rootUploadDir();
        // createDirectories 首次运行时自动创建目录，目录已存在时不抛异常。
        Files.createDirectories(uploadDir);
        Files.write(uploadDir.resolve(filename), file.getBytes());
        // 生成可访问的 URL 路径，由 WebMvcConfig 静态资源映射提供服务。
        String avatarUrl = "/uploads/" + filename;
        // 将新 URL 持久化到数据库，下次登录获取 userInfo 时就会读到新头像。
        User current = userService.getByUsername(authentication.getName());
        User updated = userService.updateAvatar(current.getId(), avatarUrl);
        return Result.success(Map.of(
                "url", avatarUrl,               // 前端用于立即预览的图片 URL
                "user", userService.toUserInfo(updated) // 更新后的完整用户信息，前端可直接写入 store
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
        // 用 Map 接收而非直接绑定 User 实体，是为了同时获取 password 和 role 这两个 User 实体里没有的字段。
        User user = new User();
        user.setUsername((String) body.get("username"));
        user.setNickname((String) body.get("nickname"));
        user.setPhone((String) body.get("phone"));
        user.setAvatar((String) body.get("avatar"));
        // getOrDefault 保证前端未传 enabled 时默认启用，避免创建出来就是禁用状态。
        user.setEnabled((Boolean) body.getOrDefault("enabled", true));
        // Service 负责 BCrypt 哈希密码和绑定角色；password 为空时 Service 使用 "123456" 作为默认密码。
        User saved = userService.create(user, (String) body.get("password"), (String) body.get("role"));
        return Result.success(userService.toUserInfo(saved));
    }

    /**
     * 管理员更新用户资料和角色。
     * 额外拦截“禁用当前登录账号”，避免管理员把自己锁在系统外。
     */
    @PutMapping("/users/{id}")
    public Result<?> updateUser(Authentication authentication, @PathVariable String id, @RequestBody Map<String, Object> body) {
        // 先取 enabled 值再做保护检查，避免先改库再发现不该改的问题。
        Boolean enabled = (Boolean) body.getOrDefault("enabled", true);
        // 禁止管理员把"自己"标记为禁用：如果允许，管理员会立刻失去访问权限，无法再解禁。
        if (isCurrentUser(authentication, id) && Boolean.FALSE.equals(enabled)) {
            return Result.error(400, "不能禁用当前登录账号");
        }
        User user = new User();
        user.setNickname((String) body.get("nickname"));
        user.setPhone((String) body.get("phone"));
        user.setAvatar((String) body.get("avatar"));
        user.setEnabled(enabled);
        // role 字段由 Service 单独处理角色绑定，Controller 只负责传递角色名称字符串。
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
