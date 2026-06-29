/**
 * 系统公告 API 模块。
 *
 * 封装对后端 AnnouncementController（/api/announcements/*）的请求，供 Announcement.vue 调用。
 *
 * 权限说明（与后端 SecurityConfig 一致）：
 *   - getPublishedAnnouncements：浏览已发布公告，任意已登录用户。
 *   - 其余 admin 接口：仅管理员（/api/announcements/admin/** 受 hasRole('ADMIN') 保护）。
 */
import request from '@/utils/request'

/**
 * 普通用户浏览已发布公告（分页，置顶优先 + 时间倒序）。
 *
 * @param {{ page: number, pageSize: number }} params
 * @returns {Promise<{code: 200, data: { records: AnnouncementRow[], total, page, pageSize }}>}
 */
export const getPublishedAnnouncements = (params) => {
  return request.get('/announcements', { params })
}

/**
 * 管理员分页查询全部公告（含草稿），支持按状态、关键字筛选。
 *
 * @param {{ page, pageSize, status?: string, keyword?: string }} params
 *   status 取值 DRAFT / PUBLISHED。
 * @returns {Promise<{code: 200, data: { records: AnnouncementRow[], total, page, pageSize }}>}
 */
export const getAnnouncementList = (params) => {
  return request.get('/announcements/admin/list', { params })
}

/**
 * 管理员新建公告。
 *
 * @param {{ title, content, status, pinned }} data
 * @returns {Promise<{code: 200, data: AnnouncementRow}>}
 */
export const createAnnouncement = (data) => {
  return request.post('/announcements/admin', data)
}

/**
 * 管理员编辑公告。
 *
 * @param {string} id
 * @param {{ title, content, status, pinned }} data
 * @returns {Promise<{code: 200, data: AnnouncementRow}>}
 */
export const updateAnnouncement = (id, data) => {
  return request.put(`/announcements/admin/${id}`, data)
}

/**
 * 管理员发布/下线公告（切换 PUBLISHED ⇄ DRAFT）。
 *
 * @param {string} id
 * @param {string} status - PUBLISHED / DRAFT。
 * @returns {Promise<{code: 200, data: AnnouncementRow}>}
 */
export const changeAnnouncementStatus = (id, status) => {
  return request.put(`/announcements/admin/${id}/status`, { status })
}

/**
 * 管理员删除公告。
 *
 * @param {string} id
 * @returns {Promise<{code: 200, data: null}>}
 */
export const deleteAnnouncement = (id) => {
  return request.delete(`/announcements/admin/${id}`)
}
