package com.example.agriculturetrace.service;

import com.example.agriculturetrace.entity.Feedback;
import com.example.agriculturetrace.repository.FeedbackRepository;
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
import java.util.Set;

/**
 * 意见反馈业务服务。
 *
 * 负责普通用户提交/查看自己反馈，以及管理员分页查询、汇总统计、回复和关闭反馈。
 */
@Service
public class FeedbackService {

    /** 允许的反馈类型，提交时做白名单校验，非法值统一归为 OTHER。 */
    private static final Set<String> VALID_TYPES = Set.of("BUG", "SUGGESTION", "OTHER");

    private final FeedbackRepository feedbackRepository;

    public FeedbackService(FeedbackRepository feedbackRepository) {
        this.feedbackRepository = feedbackRepository;
    }

    /**
     * 普通用户提交一条反馈。
     * 校验标题/内容非空，类型不在白名单时归为 OTHER，初始状态为 PENDING。
     */
    public Feedback create(String userId, String username, String type, String title, String content) {
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("反馈标题不能为空");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("反馈内容不能为空");
        }
        Feedback feedback = new Feedback();
        feedback.setId(Ids.uuid32());
        feedback.setUserId(userId);
        feedback.setUsername(username);
        // 非法类型统一归一为 OTHER，避免脏数据破坏后续类型筛选与汇总。
        feedback.setType(VALID_TYPES.contains(type) ? type : "OTHER");
        feedback.setTitle(title.trim());
        feedback.setContent(content.trim());
        feedback.setStatus("PENDING");
        feedback.setCreateTime(TimeUtils.nowText());
        return feedbackRepository.save(feedback);
    }

    /**
     * 分页查询当前用户自己提交的反馈，按提交时间倒序（最新在前）。
     */
    public Page<Feedback> listMine(String userId, int page, int pageSize) {
        PageRequest request = PageRequest.of(
                Math.max(page - 1, 0),
                Math.max(pageSize, 1),
                Sort.by(Sort.Direction.DESC, "createTime"));
        return feedbackRepository.findByUserId(userId, request);
    }

    /**
     * 管理员分页查询全部反馈，支持按状态、类型、关键字（标题/用户名）动态筛选。
     */
    public Page<Feedback> listAll(String status, String type, String keyword, int page, int pageSize) {
        PageRequest request = PageRequest.of(
                Math.max(page - 1, 0),
                Math.max(pageSize, 1),
                Sort.by(Sort.Direction.DESC, "createTime"));
        return feedbackRepository.findAll(buildSpecification(status, type, keyword), request);
    }

    /**
     * 构造管理员列表筛选条件：状态、类型精确匹配，关键字模糊匹配标题或用户名。
     */
    private Specification<Feedback> buildSpecification(String status, String type, String keyword) {
        return (root, query, builder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (hasText(status)) {
                predicates.add(builder.equal(root.get("status"), status.trim()));
            }
            if (hasText(type)) {
                predicates.add(builder.equal(root.get("type"), type.trim()));
            }
            if (hasText(keyword)) {
                String like = "%" + keyword.trim() + "%";
                predicates.add(builder.or(
                        builder.like(root.get("title"), like),
                        builder.like(root.get("username"), like)));
            }
            return builder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 汇总统计：总量、各状态计数、各类型计数。
     * 当反馈过多时，管理员先看汇总卡片掌握全局，再点进列表查看明细。
     */
    public Map<String, Object> summary() {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("total", feedbackRepository.count());
        summary.put("pending", feedbackRepository.countByStatus("PENDING"));
        summary.put("replied", feedbackRepository.countByStatus("REPLIED"));
        summary.put("closed", feedbackRepository.countByStatus("CLOSED"));
        Map<String, Object> byType = new LinkedHashMap<>();
        byType.put("BUG", feedbackRepository.countByType("BUG"));
        byType.put("SUGGESTION", feedbackRepository.countByType("SUGGESTION"));
        byType.put("OTHER", feedbackRepository.countByType("OTHER"));
        summary.put("byType", byType);
        return summary;
    }

    /**
     * 管理员回复反馈：写入回复内容、回复人、回复时间，并将状态推进为 REPLIED。
     */
    public Feedback reply(String id, String replyBy, String replyContent) {
        if (replyContent == null || replyContent.isBlank()) {
            throw new IllegalArgumentException("回复内容不能为空");
        }
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
        feedback.setReply(replyContent.trim());
        feedback.setReplyBy(replyBy);
        feedback.setReplyTime(TimeUtils.nowText());
        feedback.setStatus("REPLIED");
        return feedbackRepository.save(feedback);
    }

    /**
     * 管理员关闭反馈，状态置为 CLOSED（不删除，保留记录可追溯）。
     */
    public Feedback close(String id) {
        Feedback feedback = feedbackRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("反馈不存在"));
        feedback.setStatus("CLOSED");
        return feedbackRepository.save(feedback);
    }

    /**
     * 管理员删除反馈。
     */
    public void delete(String id) {
        feedbackRepository.deleteById(id);
    }

    /**
     * 将实体转换为前端展示行，显式字段映射避免实体结构变化影响接口契约。
     */
    public Map<String, Object> toRow(Feedback feedback) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("id", feedback.getId());
        row.put("userId", feedback.getUserId());
        row.put("username", feedback.getUsername());
        row.put("type", feedback.getType());
        row.put("title", feedback.getTitle());
        row.put("content", feedback.getContent());
        row.put("status", feedback.getStatus());
        row.put("reply", feedback.getReply());
        row.put("replyBy", feedback.getReplyBy());
        row.put("replyTime", feedback.getReplyTime());
        row.put("createTime", feedback.getCreateTime());
        return row;
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }
}
