package com.example.agriculturetrace.controller;

import com.example.agriculturetrace.entity.Announcement;
import com.example.agriculturetrace.service.AnnouncementService;
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
 * 系统公告接口。
 *
 * 权限划分（见 SecurityConfig）：
 *   - GET /api/announcements：浏览已发布公告，任意已登录用户。
 *   - /api/announcements/admin/**：发布/编辑/删除等管理操作，仅管理员。
 */
@RestController
@RequestMapping("/api/announcements")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    /**
     * 普通用户浏览已发布公告（分页，置顶优先 + 时间倒序）。
     */
    @GetMapping
    public Result<?> published(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(toPageResult(announcementService.listPublished(page, pageSize)));
    }

    /**
     * 管理员分页查询全部公告（含草稿），支持按状态、关键字筛选。
     */
    @GetMapping("/admin/list")
    public Result<?> adminList(@RequestParam(defaultValue = "1") int page,
                               @RequestParam(defaultValue = "10") int pageSize,
                               @RequestParam(required = false) String status,
                               @RequestParam(required = false) String keyword) {
        return Result.success(toPageResult(announcementService.listAll(status, keyword, page, pageSize)));
    }

    /**
     * 管理员新建公告。发布人取自登录态。
     */
    @PostMapping("/admin")
    public Result<?> create(Authentication authentication, @RequestBody Map<String, Object> body) {
        Announcement saved = announcementService.create(
                (String) body.get("title"),
                (String) body.get("content"),
                (String) body.get("status"),
                (Boolean) body.get("pinned"),
                authentication.getName());
        return Result.success(announcementService.toRow(saved));
    }

    /**
     * 管理员编辑公告。
     */
    @PutMapping("/admin/{id}")
    public Result<?> update(@PathVariable String id, @RequestBody Map<String, Object> body) {
        Announcement saved = announcementService.update(
                id,
                (String) body.get("title"),
                (String) body.get("content"),
                (String) body.get("status"),
                (Boolean) body.get("pinned"));
        return Result.success(announcementService.toRow(saved));
    }

    /**
     * 管理员发布/下线公告（切换 PUBLISHED ⇄ DRAFT）。
     */
    @PutMapping("/admin/{id}/status")
    public Result<?> changeStatus(@PathVariable String id, @RequestBody Map<String, String> body) {
        Announcement saved = announcementService.changeStatus(id, body.get("status"));
        return Result.success(announcementService.toRow(saved));
    }

    /**
     * 管理员删除公告。
     */
    @DeleteMapping("/admin/{id}")
    public Result<?> delete(@PathVariable String id) {
        announcementService.delete(id);
        return Result.success(null);
    }

    private Map<String, Object> toPageResult(Page<Announcement> announcements) {
        return Map.of(
                "records", announcements.getContent().stream().map(announcementService::toRow).toList(),
                "total", announcements.getTotalElements(),
                "page", announcements.getNumber() + 1,
                "pageSize", announcements.getSize());
    }
}
