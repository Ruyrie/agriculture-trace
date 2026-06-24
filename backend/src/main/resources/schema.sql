CREATE DATABASE IF NOT EXISTS `agriculture_trace`
DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;

USE `agriculture_trace`;

CREATE TABLE IF NOT EXISTS `user` (
  `id` varchar(32) NOT NULL COMMENT '用户ID (UUID)',
  `username` varchar(64) NOT NULL COMMENT '用户名',
  `password` varchar(100) NOT NULL COMMENT '密码（BCrypt加密）',
  `nickname` varchar(64) DEFAULT NULL COMMENT '昵称',
  `phone` varchar(32) DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(256) DEFAULT NULL COMMENT '头像URL',
  `enabled` tinyint(1) DEFAULT '1' COMMENT '是否启用',
  `create_time` varchar(19) DEFAULT NULL COMMENT '创建时间 yyyy-MM-dd HH:mm:ss',
  PRIMARY KEY (`id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `role` (
  `id` varchar(32) NOT NULL COMMENT '角色ID (UUID)',
  `name` varchar(64) NOT NULL COMMENT '角色名',
  `description` varchar(128) DEFAULT NULL COMMENT '角色描述',
  PRIMARY KEY (`id`),
  UNIQUE KEY `name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `user_role` (
  `user_id` varchar(32) NOT NULL,
  `role_id` varchar(32) NOT NULL,
  PRIMARY KEY (`user_id`,`role_id`),
  FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
  FOREIGN KEY (`role_id`) REFERENCES `role`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `product` (
  `id` varchar(32) NOT NULL COMMENT '产品ID (UUID)',
  `name` varchar(128) NOT NULL COMMENT '产品名称',
  `category` varchar(64) DEFAULT NULL COMMENT '产品类别',
  `origin` varchar(128) DEFAULT NULL COMMENT '产地',
  `price` decimal(10,2) DEFAULT NULL,
  `create_time` varchar(19) DEFAULT NULL COMMENT '创建时间',
  `data_hash` varchar(64) DEFAULT NULL COMMENT '产品数据哈希(SHA-256)',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `batch` (
  `id` varchar(32) NOT NULL COMMENT '批次ID (UUID)',
  `batch_no` varchar(64) NOT NULL COMMENT '批次号',
  `product_id` varchar(32) NOT NULL,
  `production_date` date DEFAULT NULL COMMENT '生产日期',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `create_time` varchar(19) DEFAULT NULL COMMENT '创建时间',
  `data_hash` varchar(64) DEFAULT NULL COMMENT '批次数据哈希(SHA-256)',
  PRIMARY KEY (`id`),
  UNIQUE KEY `batch_no` (`batch_no`),
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `blockchain_log` (
  `id` varchar(32) NOT NULL COMMENT '日志ID (时间前缀UUID)',
  `action_type` varchar(20) NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE',
  `target_type` varchar(20) NOT NULL COMMENT '目标类型: PRODUCT/BATCH',
  `target_id` varchar(32) NOT NULL COMMENT '目标ID',
  `operator` varchar(64) NOT NULL COMMENT '操作人用户名',
  `data_before` text COMMENT '操作前数据JSON',
  `data_after` text COMMENT '操作后数据JSON',
  `data_hash` varchar(64) NOT NULL COMMENT '本条日志哈希',
  `previous_hash` varchar(64) DEFAULT NULL COMMENT '上一条日志哈希',
  `timestamp` varchar(19) NOT NULL COMMENT '操作时间 yyyy-MM-dd HH:mm:ss',
  PRIMARY KEY (`id`),
  KEY `idx_blockchain_target` (`target_type`,`target_id`),
  KEY `idx_blockchain_timestamp` (`timestamp`,`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `production_record` (
  `id` varchar(32) NOT NULL COMMENT '生产记录ID',
  `batch_id` varchar(32) NOT NULL COMMENT '批次ID',
  `activity_name` varchar(128) NOT NULL COMMENT '生产活动',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作员',
  `activity_date` varchar(19) DEFAULT NULL COMMENT '活动时间',
  `remark` varchar(256) DEFAULT NULL COMMENT '备注',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_production_batch` (`batch_id`),
  FOREIGN KEY (`batch_id`) REFERENCES `batch`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `inspection_record` (
  `id` varchar(32) NOT NULL COMMENT '质检记录ID',
  `batch_id` varchar(32) NOT NULL COMMENT '批次ID',
  `inspection_item` varchar(128) NOT NULL COMMENT '检测项目',
  `result` varchar(128) DEFAULT NULL COMMENT '检测结果',
  `inspector` varchar(64) DEFAULT NULL COMMENT '检测员',
  `inspection_date` varchar(19) DEFAULT NULL COMMENT '检测时间',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_inspection_batch` (`batch_id`),
  FOREIGN KEY (`batch_id`) REFERENCES `batch`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `logistics_record` (
  `id` varchar(32) NOT NULL COMMENT '物流记录ID',
  `batch_id` varchar(32) NOT NULL COMMENT '批次ID',
  `node_name` varchar(128) NOT NULL COMMENT '物流节点',
  `location` varchar(128) DEFAULT NULL COMMENT '节点地点',
  `operator` varchar(64) DEFAULT NULL COMMENT '操作员',
  `update_time` varchar(19) DEFAULT NULL COMMENT '更新时间',
  `sort_order` int DEFAULT 0 COMMENT '排序',
  PRIMARY KEY (`id`),
  KEY `idx_logistics_batch` (`batch_id`),
  FOREIGN KEY (`batch_id`) REFERENCES `batch`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS `trace_record` (
  `id` varchar(32) NOT NULL COMMENT '记录ID (UUID)',
  `product_id` varchar(32) NOT NULL,
  `trace_time` varchar(19) DEFAULT NULL COMMENT '查询时间',
  `ip` varchar(64) DEFAULT NULL COMMENT '客户端IP',
  PRIMARY KEY (`id`),
  FOREIGN KEY (`product_id`) REFERENCES `product`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

INSERT IGNORE INTO `role` (`id`, `name`, `description`) VALUES
('role_admin', 'ROLE_ADMIN', '系统管理员，拥有所有权限'),
('role_farmer', 'ROLE_FARMER', '农户，可管理自己的产品和批次'),
('role_inspector', 'ROLE_INSPECTOR', '监管员，可查看所有数据和统计分析');

INSERT IGNORE INTO `user` (`id`, `username`, `password`, `nickname`, `phone`, `enabled`, `create_time`) VALUES
('user_admin', 'admin', '$2a$10$yH0q67MimV6tNlRNMsZGtetBkjfblZ8UPGM2cA7LUaZF/sss6eXmS', '系统管理员', '13800000000', 1, '2026-01-01 00:00:00'),
('user_farmer', 'farmer', '$2a$10$yH0q67MimV6tNlRNMsZGtetBkjfblZ8UPGM2cA7LUaZF/sss6eXmS', '张三', '13912345678', 1, '2026-01-01 00:00:00'),
('user_inspector', 'inspector', '$2a$10$yH0q67MimV6tNlRNMsZGtetBkjfblZ8UPGM2cA7LUaZF/sss6eXmS', '李监管', '13800138000', 1, '2026-01-01 00:00:00');

INSERT IGNORE INTO `user_role` (`user_id`, `role_id`) VALUES
('user_admin', 'role_admin'),
('user_farmer', 'role_farmer'),
('user_inspector', 'role_inspector');

INSERT IGNORE INTO `product` (`id`, `name`, `category`, `origin`, `price`, `create_time`) VALUES
('prod_1', '有机苹果', '水果', '山东烟台', 12.50, '2026-01-01 10:00:00'),
('prod_2', '五常大米', '粮食', '黑龙江五常', 8.80, '2026-01-01 10:00:00'),
('prod_3', '日照绿茶', '茶叶', '山东日照', 68.00, '2026-01-01 10:00:00'),
('prod_4', '有机西兰花', '蔬菜', '云南昆明', 6.50, '2026-01-01 10:00:00'),
('prod_5', '土鸡蛋', '禽蛋', '河北保定', 1.20, '2026-01-01 10:00:00');

-- 只为“尚未生成指纹”的产品回填 data_hash；绝不能无条件重算，
-- 否则 spring.sql.init.mode=always 每次重启都会用当前数据覆盖指纹，
-- 直接抹掉对数据库直改的篡改痕迹，使防篡改校验形同虚设。
UPDATE `product`
SET `data_hash` = SHA2(CONCAT(
  IFNULL(`id`, ''), '|',
  IFNULL(`name`, ''), '|',
  IFNULL(`category`, ''), '|',
  IFNULL(`origin`, ''), '|',
  CASE
    WHEN `price` IS NULL THEN ''
    WHEN `price` = 0 THEN '0'
    ELSE TRIM(TRAILING '.' FROM TRIM(TRAILING '0' FROM CAST(`price` AS CHAR)))
  END, '|',
  IFNULL(`create_time`, '')
), 256)
WHERE (`data_hash` IS NULL OR `data_hash` = '')
  AND NOT EXISTS (SELECT 1 FROM `blockchain_log`);

INSERT IGNORE INTO `batch` (`id`, `batch_no`, `product_id`, `production_date`, `remark`, `create_time`) VALUES
('batch_1', 'B202601001', 'prod_1', '2026-01-10', '一级果，糖度≥14%', '2026-01-11 09:00:00'),
('batch_2', 'B202602001', 'prod_2', '2026-02-15', '当季新米', '2026-02-16 09:00:00'),
('batch_3', 'B202603001', 'prod_3', '2026-03-05', '明前茶，特级', '2026-03-06 09:00:00'),
('batch_4', 'B202604001', 'prod_4', '2026-04-01', '有机认证，无农残', '2026-04-02 09:00:00'),
('batch_5', 'B202605001', 'prod_5', '2026-05-10', '散养土鸡蛋', '2026-05-11 09:00:00'),
('batch_6', 'B202601002', 'prod_1', '2026-01-18', '二级果，冷库入库', '2026-01-19 09:00:00'),
('batch_7', 'B202602002', 'prod_1', '2026-02-08', '节后补货批次', '2026-02-09 09:00:00'),
('batch_8', 'B202603002', 'prod_1', '2026-03-12', '精选礼盒装', '2026-03-13 09:00:00'),
('batch_9', 'B202604002', 'prod_1', '2026-04-16', '春季促销批次', '2026-04-17 09:00:00'),
('batch_10', 'B202605002', 'prod_1', '2026-05-22', '复检合格批次', '2026-05-23 09:00:00'),
('batch_11', 'B202603003', 'prod_2', '2026-03-20', '真空包装 5kg', '2026-03-21 09:00:00'),
('batch_12', 'B202604003', 'prod_2', '2026-04-18', '家庭装补货', '2026-04-19 09:00:00'),
('batch_13', 'B202605003', 'prod_2', '2026-05-25', '企业团购批次', '2026-05-26 09:00:00'),
('batch_14', 'B202606001', 'prod_2', '2026-06-05', '端午备货批次', '2026-06-06 09:00:00'),
('batch_15', 'B202604004', 'prod_3', '2026-04-08', '雨前茶，一级', '2026-04-09 09:00:00'),
('batch_16', 'B202605004', 'prod_3', '2026-05-06', '春茶礼盒', '2026-05-07 09:00:00'),
('batch_17', 'B202606002', 'prod_3', '2026-06-02', '冷藏保鲜批次', '2026-06-03 09:00:00'),
('batch_18', 'B202604005', 'prod_4', '2026-04-12', '基地直采批次', '2026-04-13 09:00:00'),
('batch_19', 'B202605005', 'prod_4', '2026-05-15', '无农残抽检合格', '2026-05-16 09:00:00'),
('batch_20', 'B202606003', 'prod_4', '2026-06-07', '社区团购批次', '2026-06-08 09:00:00'),
('batch_21', 'B202605006', 'prod_5', '2026-05-18', '30枚家庭装', '2026-05-19 09:00:00'),
('batch_22', 'B202606004', 'prod_5', '2026-06-01', '端午礼盒装', '2026-06-02 09:00:00'),
('batch_23', 'B202606005', 'prod_5', '2026-06-10', '当日鲜蛋批次', '2026-06-11 09:00:00');

UPDATE `batch`
SET `data_hash` = SHA2(CONCAT(
  IFNULL(`id`, ''), '|',
  IFNULL(`batch_no`, ''), '|',
  IFNULL(`product_id`, ''), '|',
  IFNULL(DATE_FORMAT(`production_date`, '%Y-%m-%d'), ''), '|',
  IFNULL(`remark`, ''), '|',
  IFNULL(`create_time`, '')
), 256)
WHERE (`data_hash` IS NULL OR `data_hash` = '')
  AND NOT EXISTS (SELECT 1 FROM `blockchain_log`);

INSERT IGNORE INTO `production_record` (`id`, `batch_id`, `activity_name`, `operator`, `activity_date`, `remark`, `sort_order`) VALUES
('prodrec_1', 'batch_1', '果园采摘', '张三', '2026-01-10 08:30:00', '晴天采摘，糖度抽检达标', 1),
('prodrec_2', 'batch_1', '冷库预冷', '王仓管', '2026-01-10 11:00:00', '2小时内入库预冷', 2),
('prodrec_3', 'batch_2', '稻谷脱壳', '李四', '2026-02-15 09:20:00', '低温碾米，保留胚芽', 1),
('prodrec_4', 'batch_2', '真空包装', '赵包装', '2026-02-15 15:10:00', '5kg规格抽检封装', 2),
('prodrec_5', 'batch_3', '鲜叶采摘', '陈茶农', '2026-03-05 07:40:00', '一芽一叶标准', 1),
('prodrec_6', 'batch_3', '杀青揉捻', '周师傅', '2026-03-05 13:20:00', '低温杀青保香', 2),
('prodrec_7', 'batch_4', '基地采收', '刘农户', '2026-04-01 08:15:00', '有机地块采收', 1),
('prodrec_8', 'batch_4', '分拣装箱', '孙分拣', '2026-04-01 14:30:00', '去除破损花球', 2),
('prodrec_9', 'batch_5', '鲜蛋收集', '杨养殖', '2026-05-10 06:50:00', '当日收集', 1),
('prodrec_10', 'batch_5', '清洁装托', '吴包装', '2026-05-10 10:25:00', '紫外线消杀后装托', 2);

INSERT IGNORE INTO `inspection_record` (`id`, `batch_id`, `inspection_item`, `result`, `inspector`, `inspection_date`, `sort_order`) VALUES
('insp_1', 'batch_1', '农药残留', '未检出', '李监管', '2026-01-11 09:00:00', 1),
('insp_2', 'batch_1', '糖度检测', '合格', '李监管', '2026-01-11 09:40:00', 2),
('insp_3', 'batch_2', '重金属检测', '合格', '王质检', '2026-02-16 10:15:00', 1),
('insp_4', 'batch_2', '水分含量', '合格', '王质检', '2026-02-16 10:55:00', 2),
('insp_5', 'batch_3', '农残快检', '未检出', '赵质检', '2026-03-06 09:30:00', 1),
('insp_6', 'batch_3', '感官评审', '合格', '赵质检', '2026-03-06 10:20:00', 2),
('insp_7', 'batch_4', '有机抽检', '合格', '李监管', '2026-04-02 09:20:00', 1),
('insp_8', 'batch_4', '农残快检', '未检出', '李监管', '2026-04-02 10:05:00', 2),
('insp_9', 'batch_5', '新鲜度检测', '合格', '王质检', '2026-05-11 08:40:00', 1),
('insp_10', 'batch_5', '沙门氏菌检测', '未检出', '王质检', '2026-05-11 09:15:00', 2);

INSERT IGNORE INTO `logistics_record` (`id`, `batch_id`, `node_name`, `location`, `operator`, `update_time`, `sort_order`) VALUES
('log_1', 'batch_1', '产地入库', '山东烟台仓', '王仓管', '2026-01-11 09:30:00', 1),
('log_2', 'batch_1', '冷链运输', '华北分拨中心', '冷链司机A', '2026-01-12 14:20:00', 2),
('log_3', 'batch_1', '门店签收', '济南历下门店', '门店店长', '2026-01-13 10:05:00', 3),
('log_4', 'batch_2', '产地入库', '黑龙江五常仓', '赵仓管', '2026-02-16 10:10:00', 1),
('log_5', 'batch_2', '干线运输', '东北分拨中心', '干线司机B', '2026-02-17 15:40:00', 2),
('log_6', 'batch_2', '门店签收', '北京朝阳门店', '门店店长', '2026-02-18 11:00:00', 3),
('log_7', 'batch_3', '茶厂入库', '山东日照茶厂', '周仓管', '2026-03-06 08:45:00', 1),
('log_8', 'batch_3', '恒温运输', '华东分拨中心', '物流员C', '2026-03-07 13:30:00', 2),
('log_9', 'batch_3', '门店签收', '上海徐汇门店', '门店店长', '2026-03-08 09:50:00', 3),
('log_10', 'batch_4', '产地入库', '云南昆明基地仓', '刘仓管', '2026-04-02 09:15:00', 1),
('log_11', 'batch_4', '冷链运输', '西南分拨中心', '冷链司机D', '2026-04-03 16:10:00', 2),
('log_12', 'batch_4', '门店签收', '成都高新门店', '门店店长', '2026-04-04 10:25:00', 3),
('log_13', 'batch_5', '养殖场入库', '河北保定养殖场', '杨仓管', '2026-05-11 07:30:00', 1),
('log_14', 'batch_5', '冷链运输', '京津冀分拨中心', '冷链司机E', '2026-05-11 18:20:00', 2),
('log_15', 'batch_5', '门店签收', '天津和平门店', '门店店长', '2026-05-12 09:15:00', 3);

-- 为所有演示批次补齐默认溯源数据，避免后续月份批次只有基础信息没有生产、质检、物流明细。
INSERT INTO `production_record` (`id`, `batch_id`, `activity_name`, `operator`, `activity_date`, `remark`, `sort_order`)
SELECT REPLACE(UUID(), '-', ''), b.id, '采收/加工', '生产员', DATE_FORMAT(b.production_date, '%Y-%m-%d 08:30:00'), CONCAT(p.name, '批次完成采收或加工'), 1
FROM `batch` b
JOIN `product` p ON p.id = b.product_id
WHERE NOT EXISTS (SELECT 1 FROM `production_record` pr WHERE pr.batch_id = b.id)
UNION ALL
SELECT REPLACE(UUID(), '-', ''), b.id, '分拣包装', '包装员', DATE_FORMAT(b.production_date, '%Y-%m-%d 14:30:00'), CONCAT(p.name, '完成分拣、称重和包装'), 2
FROM `batch` b
JOIN `product` p ON p.id = b.product_id
WHERE NOT EXISTS (SELECT 1 FROM `production_record` pr WHERE pr.batch_id = b.id);

-- 为所有演示批次补齐默认质检数据，保证批次溯源页能完整呈现质量报告。
INSERT INTO `inspection_record` (`id`, `batch_id`, `inspection_item`, `result`, `inspector`, `inspection_date`, `sort_order`)
SELECT REPLACE(UUID(), '-', ''), b.id, '农残快检', '合格', '质检员', DATE_FORMAT(DATE_ADD(b.production_date, INTERVAL 1 DAY), '%Y-%m-%d 09:30:00'), 1
FROM `batch` b
WHERE NOT EXISTS (SELECT 1 FROM `inspection_record` ir WHERE ir.batch_id = b.id)
UNION ALL
SELECT REPLACE(UUID(), '-', ''), b.id, '外观抽检', '合格', '质检员', DATE_FORMAT(DATE_ADD(b.production_date, INTERVAL 1 DAY), '%Y-%m-%d 10:30:00'), 2
FROM `batch` b
WHERE NOT EXISTS (SELECT 1 FROM `inspection_record` ir WHERE ir.batch_id = b.id);

-- 为所有演示批次补齐默认物流轨迹，产品溯源和批次溯源都直接读取这些数据库记录。
INSERT INTO `logistics_record` (`id`, `batch_id`, `node_name`, `location`, `operator`, `update_time`, `sort_order`)
SELECT REPLACE(UUID(), '-', ''), b.id, '产地入库', CONCAT(p.origin, '产地仓库'), '仓管员', DATE_FORMAT(DATE_ADD(b.production_date, INTERVAL 1 DAY), '%Y-%m-%d 09:00:00'), 1
FROM `batch` b
JOIN `product` p ON p.id = b.product_id
WHERE NOT EXISTS (SELECT 1 FROM `logistics_record` lr WHERE lr.batch_id = b.id)
UNION ALL
SELECT REPLACE(UUID(), '-', ''), b.id, '冷链运输', '区域分拨中心', '物流员', DATE_FORMAT(DATE_ADD(b.production_date, INTERVAL 1 DAY), '%Y-%m-%d 14:00:00'), 2
FROM `batch` b
JOIN `product` p ON p.id = b.product_id
WHERE NOT EXISTS (SELECT 1 FROM `logistics_record` lr WHERE lr.batch_id = b.id)
UNION ALL
SELECT REPLACE(UUID(), '-', ''), b.id, '终端签收', '销售终端门店', '门店人员', DATE_FORMAT(DATE_ADD(b.production_date, INTERVAL 2 DAY), '%Y-%m-%d 10:00:00'), 3
FROM `batch` b
JOIN `product` p ON p.id = b.product_id
WHERE NOT EXISTS (SELECT 1 FROM `logistics_record` lr WHERE lr.batch_id = b.id);

INSERT IGNORE INTO `trace_record` (`id`, `product_id`, `trace_time`, `ip`) VALUES
('trace_1', 'prod_1', '2026-06-06 09:20:00', '127.0.0.1'),
('trace_2', 'prod_1', '2026-06-07 10:10:00', '127.0.0.1'),
('trace_3', 'prod_2', '2026-06-08 11:35:00', '127.0.0.1'),
('trace_4', 'prod_3', '2026-06-09 14:12:00', '127.0.0.1'),
('trace_5', 'prod_1', '2026-06-10 16:30:00', '127.0.0.1'),
('trace_6', 'prod_4', '2026-06-11 17:45:00', '127.0.0.1'),
('trace_7', 'prod_5', '2026-06-12 08:50:00', '127.0.0.1'),
('trace_8', 'prod_2', '2026-06-12 09:05:00', '127.0.0.1');
