package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.Feedback;
import com.example.agriculturetrace.entity.User;
import com.example.agriculturetrace.service.FeedbackService;
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

import java.util.Map;

/**
 * 意见反馈接口。
 *
 * 权限划分（见 SecurityConfig）：
 *   - /api/feedback（POST 提交）、/api/feedback/mine（查看自己的反馈）：任意已登录用户。
 *   - /api/feedback/admin/**（列表、汇总、回复、关闭、删除）：仅管理员。
 *
 * 当前用户身份统一从 Spring Security Authentication 解析，不信任前端传入的用户标识。
 */
@RestController
@RequestMapping("/api/feedback")
public class FeedbackController {

    private final FeedbackService feedbackService;
    private final UserService userService;

    public FeedbackController(FeedbackService feedbackService, UserService userService) {
        this.feedbackService = feedbackService;
        this.userService = userService;
    }

    /**
     * 普通用户提交一条反馈。
     * 提交人身份从登录态解析，请求体只携带 type/title/content。
     */
    @PostMapping
    public Result<?> submit(Authentication authentication, @RequestBody Map<String, String> body) {
        User current = userService.getByUsername(authentication.getName());
        Feedback saved = feedbackService.create(
                current.getId(),
                current.getUsername(),
                body.get("type"),
                body.get("title"),
                body.get("content"));
        return Result.success(feedbackService.toRow(saved));
    }

    /**
     * 当前用户查看自己提交过的反馈（含管理员回复），分页倒序。
     */
    @GetMapping("/mine")
    public Result<?> mine(Authentication authentication,
                          @RequestParam(defaultValue = "1") int page,
                          @RequestParam(defaultValue = "10") int pageSize) {
        User current = userService.getByUsername(authentication.getName());
        Page<Feedback> feedbacks = feedbackService.listMine(current.getId(), page, pageSize);
        return Result.success(toPageResult(feedbacks));
    }

    /**
     * 管理员分页查询全部反馈，支持按状态、类型、关键字筛选。
     */
    @GetMapping("/admin/list")
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String type,
                               @RequestParam(required = false) String keyword) {
        Page<Feedback> feedbacks = feedbackService.listAll(status, type, keyword, page, pageSize);
        return Result.success(toPageResult(feedbacks));
    }

    /**
     * 管理员获取反馈汇总统计（总量、各状态计数、各类型计数）。
     * 反馈量大时先看汇总卡片，再点进列表查看明细。
     */
    @GetMapping("/admin/summary")
    public Result<?> adminSummary() {
        return Result.success(feedbackService.summary());
    }

    /**
     * 管理员回复指定反馈。
     */
    @PutMapping("/admin/{id}/reply")
    public Result<?> reply(Authentication authentication,
                           @PathVariable String id,
                           @RequestBody Map<String, String> body) {
        Feedback saved = feedbackService.reply(id, authentication.getName(), body.get("reply"));
        return Result.success(feedbackService.toRow(saved));
    }

    /**
     * 管理员关闭指定反馈（保留记录，仅置为已关闭）。
     */
    @PutMapping("/admin/{id}/close")
    public Result<?> close(@PathVariable String id) {
        Feedback saved = feedbackService.close(id);
        return Result.success(feedbackService.toRow(saved));
    }

    /**
     * 管理员删除指定反馈。
     */
    @DeleteMapping("/admin/{id}")
    public Result<?> delete(@PathVariable String id) {
        feedbackService.delete(id);
        return Result.success(null);
    }

    /**
     * 统一分页响应结构，与项目其他列表接口保持一致（records/total/page/pageSize）。
     */
    private Map<String, Object> toPageResult(Page<Feedback> feedbacks) {
        return Map.of(
                "records", feedbacks.getContent().stream().map(feedbackService::toRow).toList(),
                "total", feedbacks.getTotalElements(),
                "page", feedbacks.getNumber() + 1,
                "pageSize", feedbacks.getSize());
    }
}
