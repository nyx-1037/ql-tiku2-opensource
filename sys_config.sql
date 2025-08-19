/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80036
 Source Host           : localhost:3306
 Source Schema         : ql_tiku2

 Target Server Type    : MySQL
 Target Server Version : 80036
 File Encoding         : 65001

 Date: 19/08/2025 21:59:50
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_config
-- ----------------------------
DROP TABLE IF EXISTS `sys_config`;
CREATE TABLE `sys_config`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '配置ID',
  `config_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置名称',
  `config_key` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '配置键名',
  `config_value` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '配置键值',
  `config_type` char(1) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'N' COMMENT '系统内置（Y是 N否）',
  `create_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '创建者',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_by` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '更新者',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `remark` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_config_key`(`config_key` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 71 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '系统配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_config
-- ----------------------------
INSERT INTO `sys_config` VALUES (1, '系统名称', 'sys.site.name', '七洛题库2', 'Y', 'admin', '2025-07-23 14:36:13', '', '2025-08-14 23:42:35', '系统名称');
INSERT INTO `sys_config` VALUES (2, '系统版本', 'sys.site.version', '2.3.0813beta', 'Y', 'admin', '2025-07-23 14:36:13', '', '2025-08-14 23:42:35', '版本号');
INSERT INTO `sys_config` VALUES (3, '系统描述', 'sys.site.description', '七洛题库', 'Y', 'admin', '2025-07-23 14:36:13', '', '2025-08-14 23:42:36', '描述');
INSERT INTO `sys_config` VALUES (4, '邮件服务器', 'sys.email.host', 'smtp.qq.com', 'Y', 'admin', '2025-07-23 14:36:13', '', '2025-07-23 14:36:13', '服务器');
INSERT INTO `sys_config` VALUES (5, '邮件端口', 'sys.email.port', '587', 'Y', 'admin', '2025-07-23 14:36:13', '', '2025-07-23 14:36:13', '端口');
INSERT INTO `sys_config` VALUES (6, '存储类型', 'sys.storage.type', 'local', 'Y', 'admin', '2025-07-23 14:36:13', '', '2025-07-23 14:36:13', '存储类型');
INSERT INTO `sys_config` VALUES (7, '上传路径', 'sys.storage.path', '/uploads', 'Y', 'admin', '2025-07-23 14:36:13', '', '2025-07-23 14:36:13', '路径');
INSERT INTO `sys_config` VALUES (8, '管理员邮箱', 'sys.admin.email', 'admin@nie1037.cn', 'Y', '', '2025-07-23 18:26:22', '', '2025-08-14 23:42:35', '系统管理员邮箱地址');
INSERT INTO `sys_config` VALUES (9, '允许注册', 'sys.allow.register', 'true', 'Y', '', '2025-07-23 18:26:22', '', '2025-08-14 23:42:36', '是否允许用户注册');
INSERT INTO `sys_config` VALUES (10, '维护模式', 'sys.maintenance.mode', 'false', 'Y', '', '2025-07-23 18:26:22', '', '2025-08-14 23:42:36', '系统维护模式开关');
INSERT INTO `sys_config` VALUES (11, '维护提示', 'sys.maintenance.message', '维护中', 'Y', '', '2025-07-23 18:26:22', '', '2025-08-14 23:42:35', '维护模式提示信息');
INSERT INTO `sys_config` VALUES (12, '邮件发送方', 'sys.email.from', 'noreply@example.com', 'Y', '', '2025-07-23 18:26:22', '', '2025-07-23 18:26:22', '邮件发送方地址');
INSERT INTO `sys_config` VALUES (13, '邮件发送方名称', 'sys.email.name', '七洛题库系统', 'Y', '', '2025-07-23 18:26:22', '', '2025-07-23 18:26:22', '邮件发送方名称');
INSERT INTO `sys_config` VALUES (14, '邮件密码', 'sys.email.password', '', 'Y', '', '2025-07-23 18:26:22', '', '2025-07-23 18:26:22', '邮件服务器密码');
INSERT INTO `sys_config` VALUES (15, '启用SSL', 'sys.email.ssl', 'false', 'Y', '', '2025-07-23 18:26:22', '', '2025-07-23 18:26:22', '邮件服务器SSL设置');
INSERT INTO `sys_config` VALUES (16, '启用TLS', 'sys.email.tls', 'true', 'Y', '', '2025-07-23 18:26:22', '', '2025-07-23 18:26:22', '邮件服务器TLS设置');
INSERT INTO `sys_config` VALUES (17, '存储域名', 'sys.storage.domain', 'http://localhost:8080', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '本地存储访问域名');
INSERT INTO `sys_config` VALUES (18, 'OSS AccessKey', 'sys.oss.accesskey', '', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '阿里云OSS AccessKey');
INSERT INTO `sys_config` VALUES (19, 'OSS SecretKey', 'sys.oss.secretkey', '', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '阿里云OSS SecretKey');
INSERT INTO `sys_config` VALUES (20, 'OSS Bucket', 'sys.oss.bucket', '', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '阿里云OSS存储桶');
INSERT INTO `sys_config` VALUES (21, 'OSS 地域', 'sys.oss.region', 'oss-cn-hangzhou', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '阿里云OSS地域');
INSERT INTO `sys_config` VALUES (22, '七牛 AccessKey', 'sys.qiniu.accesskey', '', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '七牛云 AccessKey');
INSERT INTO `sys_config` VALUES (23, '七牛 SecretKey', 'sys.qiniu.secretkey', '', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '七牛云 SecretKey');
INSERT INTO `sys_config` VALUES (24, '七牛 Bucket', 'sys.qiniu.bucket', '', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '七牛云存储桶');
INSERT INTO `sys_config` VALUES (25, '七牛域名', 'sys.qiniu.domain', '', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '七牛云访问域名');
INSERT INTO `sys_config` VALUES (26, '最大文件大小', 'sys.storage.maxsize', '10', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '最大文件大小(MB)');
INSERT INTO `sys_config` VALUES (27, '允许文件类型', 'sys.storage.types', 'jpg,jpeg,png,gif,pdf,doc,docx,xls,xlsx', 'Y', '', '2025-07-23 18:26:31', '', '2025-07-23 18:26:31', '允许上传的文件类型');
INSERT INTO `sys_config` VALUES (28, '最小密码长度', 'sys.password.minlength', '8', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '用户密码最小长度');
INSERT INTO `sys_config` VALUES (29, '密码需要大写字母', 'sys.password.uppercase', 'false', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '密码是否需要大写字母');
INSERT INTO `sys_config` VALUES (30, '密码需要小写字母', 'sys.password.lowercase', 'true', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '密码是否需要小写字母');
INSERT INTO `sys_config` VALUES (31, '密码需要数字', 'sys.password.numbers', 'true', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '密码是否需要数字');
INSERT INTO `sys_config` VALUES (32, '密码需要特殊字符', 'sys.password.symbols', 'false', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '密码是否需要特殊字符');
INSERT INTO `sys_config` VALUES (33, '最大登录尝试次数', 'sys.login.maxattempts', '5', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '最大登录尝试次数');
INSERT INTO `sys_config` VALUES (34, '登录锁定时长', 'sys.login.lockout', '15', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '登录失败锁定时长(分钟)');
INSERT INTO `sys_config` VALUES (35, '会话超时时间', 'sys.session.timeout', '120', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '用户会话超时时间(分钟)');
INSERT INTO `sys_config` VALUES (36, '启用验证码', 'sys.captcha.enable', 'true', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '是否启用登录验证码');
INSERT INTO `sys_config` VALUES (37, '启用双因子认证', 'sys.twofactor.enable', 'false', 'Y', '', '2025-07-23 18:26:41', '', '2025-07-27 10:51:30', '是否启用双因子认证');
INSERT INTO `sys_config` VALUES (38, '启用自动备份', 'sys.backup.enabled', 'false', 'Y', '', '2025-07-23 18:26:48', '', '2025-07-23 18:26:48', '是否启用自动备份');
INSERT INTO `sys_config` VALUES (39, '备份频率', 'sys.backup.frequency', 'daily', 'Y', '', '2025-07-23 18:26:48', '', '2025-07-23 18:26:48', '自动备份频率');
INSERT INTO `sys_config` VALUES (40, '备份时间', 'sys.backup.time', '02:00', 'Y', '', '2025-07-23 18:26:48', '', '2025-07-23 18:26:48', '自动备份时间');
INSERT INTO `sys_config` VALUES (41, '备份保留数量', 'sys.backup.retention', '7', 'Y', '', '2025-07-23 18:26:48', '', '2025-07-23 18:26:48', '备份文件保留数量');
INSERT INTO `sys_config` VALUES (43, '系统Logo', 'sys.site.logo', 'https://nie1037-oss.oss-cn-hangzhou.aliyuncs.com/logo/1753513582879_35c04e1b.png', 'Y', 'admin', '2025-07-24 15:32:06', '', '2025-08-14 23:42:35', '系统Logo图片URL');
INSERT INTO `sys_config` VALUES (44, '网站备案号', 'sys.site.icp', '赣ICP备2025065901号', 'Y', 'admin', '2025-07-24 15:32:06', '', '2025-08-14 23:42:35', '网站ICP备案号');
INSERT INTO `sys_config` VALUES (45, '公安备案号', 'sys.site.police', '赣公网安备36042402000108号', 'Y', 'admin', '2025-07-24 15:32:06', '', '2025-08-14 23:42:35', '公安备案号');
INSERT INTO `sys_config` VALUES (46, '公安备案链接', 'sys.site.police.url', 'https://beian.mps.gov.cn/#/query/webSearch?code=36042402000108', 'Y', 'admin', '2025-07-24 15:32:06', '', '2025-08-14 23:42:35', '公安备案查询链接');
INSERT INTO `sys_config` VALUES (47, '备案图标', 'sys.site.police.icon', 'https://nie1037-oss.oss-cn-hangzhou.aliyuncs.com/%E5%A4%87%E6%A1%88%E5%9B%BE%E6%A0%87.png', 'Y', 'admin', '2025-07-24 15:32:06', '', '2025-08-14 23:42:35', '公安备案图标URL');
INSERT INTO `sys_config` VALUES (48, '版权信息', 'sys.site.copyright', '© 2025 Mr.Nie 保留所有权利', 'Y', 'admin', '2025-07-24 15:32:06', '', '2025-08-14 23:42:36', '版权信息');
INSERT INTO `sys_config` VALUES (49, 'ICP备案链接', 'sys.site.icp.url', 'http://beian.miit.gov.cn/', 'Y', 'system', '2025-07-24 17:22:13', '', '2025-08-14 23:42:35', 'ICP备案链接地址');
INSERT INTO `sys_config` VALUES (50, 'Qwen AI API密钥', 'ai.qwen.api.key', 'xxxx', 'Y', '', '2025-07-25 12:46:07', '', '2025-07-30 22:29:41', 'Qwen AI API密钥配置');
INSERT INTO `sys_config` VALUES (51, 'Qwen AI模型名称', 'ai.qwen.model.name', 'qwen-turbo', 'Y', '', '2025-07-25 12:46:07', '', '2025-07-30 22:29:41', 'Qwen AI模型名称配置');
INSERT INTO `sys_config` VALUES (52, 'Qwen AI最大令牌数', 'ai.qwen.max.tokens', '2000', 'Y', '', '2025-07-25 12:46:07', '', '2025-07-30 22:29:41', 'Qwen AI最大令牌数配置');
INSERT INTO `sys_config` VALUES (53, 'Qwen AI温度参数', 'ai.qwen.temperature', '0.7', 'Y', '', '2025-07-25 12:46:07', '', '2025-07-30 22:29:41', 'Qwen AI温度参数配置');
INSERT INTO `sys_config` VALUES (54, 'AI功能启用', 'ai.enable', 'true', 'Y', '', '2025-07-25 14:04:12', '', '2025-07-30 22:29:41', '是否启用AI功能');
INSERT INTO `sys_config` VALUES (55, 'AI问题分析前缀', 'ai.analysis.prefix', '请分析以下题目并做出精简的回答：', 'Y', '', '2025-07-25 14:04:12', '', '2025-07-30 22:29:41', 'AI问题分析前缀提示语');
INSERT INTO `sys_config` VALUES (64, 'Qwen AI服务地址', 'ai.qwen.api.url', 'api.nie1037.cn', 'Y', '', '2025-07-25 22:30:07', '', '2025-07-30 22:29:41', 'Qwen AI服务的API接口地址');
INSERT INTO `sys_config` VALUES (65, 'Qwen AI超时时间', 'ai.qwen.timeout', '30', 'Y', '', '2025-07-25 22:30:07', '', '2025-07-30 22:29:41', 'Qwen AI请求的超时时间（秒）');
INSERT INTO `sys_config` VALUES (66, 'Qwen AI重试次数', 'ai.qwen.retry.count', '3', 'Y', '', '2025-07-25 22:30:07', '', '2025-07-30 22:29:41', 'Qwen AI请求失败时的重试次数');
INSERT INTO `sys_config` VALUES (67, 'AI判题前缀模板', 'ai.grading.prefix', '请对以下简答题进行判题评分：\n题目：{question}\n用户答案：{userAnswer}\n参考答案：{correctAnswer}\n请给出评分和详细的评价。', 'Y', '', '2025-07-27 11:25:31', '', '2025-07-30 22:29:41', 'AI判题功能的前缀模板，支持{question}、{userAnswer}、{correctAnswer}占位符');
INSERT INTO `sys_config` VALUES (68, 'AI判题正确性判断前缀', 'ai.grading.correctness.prefix', '请判断以下简答题答案是否正确：\n题目：{question}\n用户答案：{userAnswer}\n参考答案：{correctAnswer}\n请只回答：正确 或 错误', 'A', '', '2025-07-27 12:01:00', '', '2025-07-30 22:29:41', 'AI判题时用于判断答案正确性的提示语模板，支持占位符：{question}、{userAnswer}、{correctAnswer}');
INSERT INTO `sys_config` VALUES (70, '每日一语API地址', 'sys.daily.quote.api', 'https://api.tjit.net/api/randtext/get?key=hG4Uq1XPWhq3b14eOGjukNZwr3', 'Y', 'system', '2025-08-03 14:25:06', '', '2025-08-14 23:42:36', '每日一语API接口地址');
INSERT INTO `sys_config` VALUES (71, '注册码校验开关', 'sys.registration.code.required', 'true', 'Y', '', '2025-08-04 22:24:49', '', '2025-08-14 23:42:35', '控制注册时是否需要注册码验证');

SET FOREIGN_KEY_CHECKS = 1;
