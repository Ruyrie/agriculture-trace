package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.LoginLog;
import com.example.agriculturetrace.repository.LoginLogRepository;
import com.example.agriculturetrace.util.Ids;
import com.example.agriculturetrace.util.TimeUtils;
import jakarta.persistence.criteria.Predicate;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 登录日志业务服务。
 *
 * 负责在登录成功/失败时记录一条日志，并向管理员提供分页查询与汇总统计。
 * record(...) 在 Spring Security 的认证处理器中调用，任何异常都被吞掉，绝不影响登录主流程。
 */
@Service
public class LoginLogService {

    private final LoginLogRepository loginLogRepository;

    public LoginLogService(LoginLogRepository loginLogRepository) {
        this.loginLogRepository = loginLogRepository;
    }

    /**
     * 记录一次登录尝试。
     * 该方法在认证成功/失败处理器中被调用，刻意 try/catch 兜底：
     * 写日志失败（如数据库瞬时不可用）不能阻断用户登录或登录失败响应。
     */
    public void record(HttpServletRequest request, String username, String status, String message) {
        try {
            LoginLog log = new LoginLog();
            log.setId(Ids.uuid32());
            log.setUsername(truncate(username, 64));
            log.setIp(resolveClientIp(request));
            log.setStatus(status);
            log.setMessage(truncate(message, 128));
            log.setUserAgent(truncate(request.getHeader("User-Agent"), 256));
            log.setLoginTime(TimeUtils.nowText());
            loginLogRepository.save(log);
        } catch (Exception ignored) {
            // 登录日志属于旁路功能，记录失败不影响登录主流程。
        }
    }

    /**
     * 解析客户端真实 IP：优先取反向代理透传的 X-Forwarded-For 首个地址，回退到 RemoteAddr。
     */
    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            // X-Forwarded-For 可能是 "client, proxy1, proxy2"，第一个才是真实客户端。
            return truncate(forwarded.split(",")[0].trim(), 64);
        }
        return truncate(request.getRemoteAddr(), 64);
    }

    /**
     * 管理员分页查询登录日志，支持按结果、用户名、时间范围筛选，按登录时间倒序。
     */
    public Page<LoginLog> list(String status, String keyword, String startTime, String endTime, int page, int pageSize) {
        PageRequest request = PageRequest.of(
                Math.max(page - 1, 0),
                Math.max(pageSize, 1),
                Sort.by(Sort.Direction.DESC, "loginTime"));
        return loginLogRepository.findAll(buildSpecification(status, keyword, startTime, endTime), request);
    }

    /**
     * 构造筛选条件：状态精确匹配，用户名/IP 模糊匹配，时间范围按字典序比较。
     */
    private Specification<LoginLog> buildSpecification(String status, String keyword, String startTime, String endTime) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(status)) {
                predicates.add(builder.equal(root.get("status"), status.trim()));
            }
            if (hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(builder.or(
                        builder.like(root.get("username"), like),
                        builder.like(root.get("ip"), like)));
            }
            // loginTime 为 yyyy-MM-dd HH:mm:ss 文本，按字典序比较等价于按时间比较。
            if (hasText(startTime)) {
                predicates.add(builder.greaterThanOrEqualTo(root.get("loginTime"), startTime.trim()));
            }
            if (hasText(endTime)) {
                predicates.add(builder.lessThanOrEqualTo(root.get("loginTime"), endTime.trim()));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 汇总统计：总次数、成功次数、失败次数。
     */
    public Map<String, Object> summary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", loginLogRepository.count());
        summary.put("success", loginLogRepository.countByStatus("SUCCESS"));
        summary.put("failure", loginLogRepository.countByStatus("FAILURE"));
        return summary;
    }

    /**
     * 将实体转换为前端展示行，显式字段映射避免实体结构变化影响接口契约。
     */
    public Map<String, Object> toRow(LoginLog log) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", log.getId());
        row.put("username", log.getUsername());
        row.put("ip", log.getIp());
        row.put("status", log.getStatus());
        row.put("message", log.getMessage());
        row.put("userAgent", log.getUserAgent());
        row.put("loginTime", log.getLoginTime());
        return row;
    }

    private String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() > max ? value.substring(0, max) : value;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
