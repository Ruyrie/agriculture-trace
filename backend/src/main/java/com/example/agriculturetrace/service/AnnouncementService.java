package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Announcement;
import com.example.agriculturetrace.repository.AnnouncementRepository;
import com.example.agriculturetrace.util.Ids;
import com.example.agriculturetrace.util.TimeUtils;
import jakarta.persistence.criteria.Predicate;
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
 * 系统公告业务服务。
 *
 * 负责管理员的公告 CRUD、发布/下线、置顶，以及普通用户的已发布公告浏览。
 */
@Service
public class AnnouncementService {

    // 置顶优先、再按创建时间倒序，保证重要公告与最新公告都靠前。
    private static final Sort DEFAULT_SORT =
            Sort.by(Sort.Order.desc("pinned"), Sort.Order.desc("createTime"));

    private final AnnouncementRepository announcementRepository;

    public AnnouncementService(AnnouncementRepository announcementRepository) {
        this.announcementRepository = announcementRepository;
    }

    /**
     * 普通用户分页查询"已发布"公告（置顶优先 + 时间倒序）。
     */
    public Page<Announcement> listPublished(int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1), DEFAULT_SORT);
        return announcementRepository.findByStatus("PUBLISHED", request);
    }

    /**
     * 管理员分页查询全部公告，支持按状态、关键字（标题）筛选。
     */
    public Page<Announcement> listAll(String status, String keyword, int page, int pageSize) {
        PageRequest request = PageRequest.of(Math.max(page - 1, 0), Math.max(pageSize, 1), DEFAULT_SORT);
        return announcementRepository.findAll(buildSpecification(status, keyword), request);
    }

    private Specification<Announcement> buildSpecification(String status, String keyword) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(status)) {
                predicates.add(builder.equal(root.get("status"), status.trim()));
            }
            if (hasText(keyword)) {
                predicates.add(builder.like(root.get("title"), "%" + keyword.trim() + "%"));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 管理员新建公告。
     */
    public Announcement create(String title, String content, String status, Boolean pinned, String creator) {
        validateContent(title, content);
        Announcement announcement = new Announcement();
        announcement.setId(Ids.uuid32());
        announcement.setTitle(title.trim());
        announcement.setContent(content.trim());
        announcement.setStatus(normalizeStatus(status));
        announcement.setPinned(Boolean.TRUE.equals(pinned));
        announcement.setCreator(creator);
        String now = TimeUtils.nowText();
        announcement.setCreateTime(now);
        announcement.setUpdateTime(now);
        return announcementRepository.save(announcement);
    }

    /**
     * 管理员编辑公告（标题/正文/状态/置顶），刷新更新时间。
     */
    public Announcement update(String id, String title, String content, String status, Boolean pinned) {
        validateContent(title, content);
        Announcement announcement = getOrThrow(id);
        announcement.setTitle(title.trim());
        announcement.setContent(content.trim());
        announcement.setStatus(normalizeStatus(status));
        announcement.setPinned(Boolean.TRUE.equals(pinned));
        announcement.setUpdateTime(TimeUtils.nowText());
        return announcementRepository.save(announcement);
    }

    /**
     * 切换公告状态：PUBLISHED ⇄ DRAFT（发布/下线）。
     */
    public Announcement changeStatus(String id, String status) {
        Announcement announcement = getOrThrow(id);
        announcement.setStatus(normalizeStatus(status));
        announcement.setUpdateTime(TimeUtils.nowText());
        return announcementRepository.save(announcement);
    }

    /**
     * 管理员删除公告。
     */
    public void delete(String id) {
        announcementRepository.deleteById(id);
    }

    private Announcement getOrThrow(String id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("公告不存在"));
    }

    private void validateContent(String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("公告标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("公告内容不能为空");
        }
    }

    /**
     * 规范化状态值，非法值一律按草稿处理，避免脏数据被当成已发布对全员可见。
     */
    private String normalizeStatus(String status) {
        return "PUBLISHED".equals(status) ? "PUBLISHED" : "DRAFT";
    }

    /**
     * 将实体转换为前端展示行。
     */
    public Map<String, Object> toRow(Announcement announcement) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", announcement.getId());
        row.put("title", announcement.getTitle());
        row.put("content", announcement.getContent());
        row.put("status", announcement.getStatus());
        row.put("pinned", announcement.getPinned());
        row.put("creator", announcement.getCreator());
        row.put("createTime", announcement.getCreateTime());
        row.put("updateTime", announcement.getUpdateTime());
        return row;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
