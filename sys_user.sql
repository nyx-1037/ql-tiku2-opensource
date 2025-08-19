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

 Date: 19/08/2025 21:23:21
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for sys_user
-- ----------------------------
DROP TABLE IF EXISTS `sys_user`;
CREATE TABLE `sys_user`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户名',
  `password` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '密码',
  `nickname` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '昵称',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '邮箱',
  `phone` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '手机号',
  `avatar` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '头像URL',
  `user_type` tinyint NULL DEFAULT 0 COMMENT '用户类型：0-学生，1-教师，2-管理员',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
  `last_login_time` datetime NULL DEFAULT NULL COMMENT '最后登录时间',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint NULL DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除',
  `membership_level` int NULL DEFAULT 0 COMMENT '用户会员等级代码，关联membership表的level_code',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `username`(`username` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Records of sys_user
-- ----------------------------
INSERT INTO `sys_user` VALUES (1, 'admin', '$2a$10$/wLG8FfQCdgvrYkyedfMCOR556HvgeoSfcm.52oXNdAubVK37RvRu', '系统管理员', 'admin@nie1037.cn', NULL,NULL, 2, 1, '2025-08-15 23:21:35', '2025-07-22 21:43:40', '2025-08-15 23:17:55', 0, 4);
INSERT INTO `sys_user` VALUES (2, 'student1', '.YfNYa6kMBAB.39bZTWBa', '学生一号', 'student1@test.com', NULL, NULL, 0, 0, NULL, '2025-07-22 23:07:31', '2025-08-10 12:53:32', 0, 0);
INSERT INTO `sys_user` VALUES (3, 'student2', '.YfNYa6kMBAB.39bZTWBa', '学生二号', 'student2@test.com', NULL, NULL, 0, 0, NULL, '2025-07-22 23:07:31', '2025-08-10 12:53:34', 0, 0);
INSERT INTO `sys_user` VALUES (4, 'teacher1', '.YfNYa6kMBAB.39bZTWBa', '教师一号', 'teacher1@test.com', NULL, NULL, 1, 0, NULL, '2025-07-22 23:07:31', '2025-08-13 21:06:21', 0, 0);
INSERT INTO `sys_user` VALUES (5, 'test_student', '.YfNYa6kMBAB.39bZTWBa', '测试学生', NULL, NULL, NULL, 0, 0, NULL, '2025-07-22 23:23:30', '2025-08-10 12:53:32', 0, 0);
INSERT INTO `sys_user` VALUES (6, 'test_teacher', '.YfNYa6kMBAB.39bZTWBa', '测试教师', NULL, NULL, NULL, 1, 0, NULL, '2025-07-22 23:23:30', '2025-07-23 14:31:32', 0, 0);
INSERT INTO `sys_user` VALUES (7, 'test', '$2a$10$BfPa1hsHgsBik7eJ2s7s9eMpTbN3bgdkLe2HsZulP.kuvAwK7sZvm', 'test', 'test@test.com', NULL,NULL, 0, 1, '2025-07-24 17:30:46', '2025-07-24 17:18:00', '2025-07-25 10:51:22', 0, 0);

SET FOREIGN_KEY_CHECKS = 1;
