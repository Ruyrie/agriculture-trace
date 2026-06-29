package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.Feedback;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 意见反馈仓库。
 *
 * 继承 JpaSpecificationExecutor 以支持管理员列表的多条件动态筛选（状态/类型/关键字）。
 */
public interface FeedbackRepository extends JpaRepository<Feedback, String>, JpaSpecificationExecutor<Feedback> {

    /**
     * 分页查询某个用户自己提交的反馈，按提交时间倒序由调用方的 Pageable 指定。
     */
    Page<Feedback> findByUserId(String userId, Pageable pageable);

    /**
     * 统计指定状态的反馈数量，供管理员汇总卡片展示"待处理"等计数。
     */
    long countByStatus(String status);

    /**
     * 统计指定类型的反馈数量，供管理员汇总各类反馈分布。
     */
    long countByType(String type);
}
