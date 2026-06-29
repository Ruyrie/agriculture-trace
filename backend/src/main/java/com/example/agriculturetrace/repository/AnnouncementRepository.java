package com.example.agriculturetrace.repository;

import com.example.agriculturetrace.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * 系统公告仓库。
 *
 * 继承 JpaSpecificationExecutor 以支持管理端按状态、关键字筛选。
 */
public interface AnnouncementRepository extends JpaRepository<Announcement, String>, JpaSpecificationExecutor<Announcement> {

    /**
     * 分页查询指定状态的公告（普通用户只查 PUBLISHED），排序由 Pageable 指定（置顶优先 + 时间倒序）。
     */
    Page<Announcement> findByStatus(String status, Pageable pageable);
}
