-- 在线刷题系统数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS ql_tiku2 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ql_tiku2;

-- 用户表
CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(100) NOT NULL COMMENT '密码',
    nickname VARCHAR(50) COMMENT '昵称',
    email VARCHAR(100) COMMENT '邮箱',
    phone VARCHAR(20) COMMENT '手机号',
    avatar VARCHAR(200) COMMENT '头像URL',
    user_type TINYINT DEFAULT 0 COMMENT '用户类型：0-学生，1-教师，2-管理员',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    last_login_time DATETIME COMMENT '最后登录时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志：0-未删除，1-已删除'
) COMMENT '用户表';

-- 科目表
CREATE TABLE subject (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '科目ID',
    name VARCHAR(50) NOT NULL COMMENT '科目名称',
    description TEXT COMMENT '科目描述',
    sort_order INT DEFAULT 0 COMMENT '排序',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志'
) COMMENT '科目表';

-- 题目表
CREATE TABLE question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    question_type TINYINT NOT NULL COMMENT '题目类型：1-单选，2-多选，3-判断，4-简答',
    title TEXT NOT NULL COMMENT '题目标题',
    content TEXT COMMENT '题目内容',
    options JSON COMMENT '选项（JSON格式）',
    correct_answer TEXT NOT NULL COMMENT '正确答案',
    analysis TEXT COMMENT '答案解析',
    difficulty TINYINT DEFAULT 1 COMMENT '难度：1-简单，2-中等，3-困难',
    knowledge_points VARCHAR(500) COMMENT '知识点标签',
    create_user_id BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志',
    INDEX idx_subject_id (subject_id),
    INDEX idx_question_type (question_type),
    INDEX idx_difficulty (difficulty)
) COMMENT '题目表';

-- 考试表
CREATE TABLE exam (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '考试ID',
    title VARCHAR(100) NOT NULL COMMENT '考试标题',
    description TEXT COMMENT '考试描述',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    duration INT NOT NULL COMMENT '考试时长（分钟）',
    total_score INT DEFAULT 100 COMMENT '总分',
    pass_score INT DEFAULT 60 COMMENT '及格分',
    question_count INT NOT NULL COMMENT '题目数量',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    status TINYINT DEFAULT 1 COMMENT '状态：0-禁用，1-正常',
    create_user_id BIGINT COMMENT '创建人ID',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted TINYINT DEFAULT 0 COMMENT '删除标志'
) COMMENT '考试表';

-- 考试题目关联表
CREATE TABLE exam_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    exam_id BIGINT NOT NULL COMMENT '考试ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    score INT DEFAULT 1 COMMENT '题目分值',
    sort_order INT DEFAULT 0 COMMENT '排序',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_exam_id (exam_id),
    INDEX idx_question_id (question_id)
) COMMENT '考试题目关联表';

-- 答题记录表
CREATE TABLE answer_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '记录ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    exam_id BIGINT COMMENT '考试ID（练习时为空）',
    user_answer TEXT COMMENT '用户答案',
    is_correct TINYINT COMMENT '是否正确：0-错误，1-正确',
    score INT DEFAULT 0 COMMENT '得分',
    answer_time INT COMMENT '答题用时（秒）',
    practice_type TINYINT DEFAULT 1 COMMENT '练习类型：1-日常练习，2-模拟考试，3-错题重做',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    INDEX idx_user_id (user_id),
    INDEX idx_question_id (question_id),
    INDEX idx_exam_id (exam_id),
    INDEX idx_is_correct (is_correct),
    INDEX idx_create_time (create_time)
) COMMENT '答题记录表';

-- 错题本表
CREATE TABLE wrong_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    question_id BIGINT NOT NULL COMMENT '题目ID',
    wrong_count INT DEFAULT 1 COMMENT '错误次数',
    last_wrong_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '最后错误时间',
    is_mastered TINYINT DEFAULT 0 COMMENT '是否已掌握：0-未掌握，1-已掌握',
    master_time DATETIME COMMENT '掌握时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_question (user_id, question_id),
    INDEX idx_user_id (user_id),
    INDEX idx_is_mastered (is_mastered)
) COMMENT '错题本表';

-- 用户学习统计表
CREATE TABLE user_study_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT 'ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    subject_id BIGINT NOT NULL COMMENT '科目ID',
    total_questions INT DEFAULT 0 COMMENT '总练习题数',
    correct_questions INT DEFAULT 0 COMMENT '正确题数',
    wrong_questions INT DEFAULT 0 COMMENT '错误题数',
    accuracy_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT '正确率',
    study_time INT DEFAULT 0 COMMENT '学习时长（分钟）',
    last_study_time DATETIME COMMENT '最后学习时间',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    UNIQUE KEY uk_user_subject (user_id, subject_id),
    INDEX idx_user_id (user_id)
) COMMENT '用户学习统计表';

-- 系统配置表
CREATE TABLE sys_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '配置ID',
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    config_type VARCHAR(20) DEFAULT 'string' COMMENT '配置类型：string、number、boolean、json',
    description VARCHAR(200) COMMENT '配置描述',
    is_system TINYINT DEFAULT 0 COMMENT '是否系统配置：0-否，1-是',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    INDEX idx_config_key (config_key)
) COMMENT '系统配置表';

-- 插入初始数据

-- 插入管理员用户
INSERT INTO sys_user (username, password, nickname, email, user_type, status) VALUES 
('admin', '$2a$10$7JB720yubVSOfvVWbazBOeUlsNqHuqHd.YfNYa6kMBAB.39bZTWBa', '系统管理员', 'admin@qltiku2.com', 2, 1);

-- 插入测试科目
INSERT INTO subject (name, description, sort_order) VALUES 
('计算机基础', '计算机基础知识', 1),
('数据结构', '数据结构与算法', 2),
('操作系统', '操作系统原理', 3),
('数据库', '数据库系统原理', 4),
('网络技术', '计算机网络技术', 5);

-- 插入系统配置
INSERT INTO sys_config (config_key, config_value, config_type, description, is_system) VALUES 
('site.name', '题库管理系统', 'string', '网站名称', 1),
('site.url', 'http://localhost:8080', 'string', '网站地址', 1),
('site.logo', '/logo.png', 'string', '网站Logo', 1),
('site.description', '在线刷题学习平台', 'string', '网站描述', 1),
('admin.email', 'admin@example.com', 'string', '管理员邮箱', 1),
('system.version', '1.0.0', 'string', '系统版本', 1),
('exam.default_duration', '60', 'number', '默认考试时长（分钟）', 1),
('exam.default_pass_score', '60', 'number', '默认及格分数', 1),
('user.default_avatar', '/default-avatar.png', 'string', '用户默认头像', 1),
('system.maintenance', 'false', 'boolean', '系统维护模式', 1);

-- 插入测试题目
INSERT INTO question (subject_id, question_type, title, content, options, correct_answer, analysis, difficulty, knowledge_points, create_user_id) VALUES 
(1, 1, '计算机的基本组成包括哪些部分？', '计算机系统主要由哪几个基本部分组成？', '["运算器、控制器、存储器、输入设备、输出设备", "CPU、内存、硬盘", "主板、显卡、电源", "键盘、鼠标、显示器"]', 'A', '计算机系统由运算器、控制器、存储器、输入设备和输出设备五个基本部分组成。', 1, '计算机组成原理', 1),
(1, 2, '以下哪些是计算机的输入设备？', '请选择所有正确的输入设备', '["键盘", "鼠标", "显示器", "扫描仪", "打印机", "麦克风"]', 'A,B,D,F', '键盘、鼠标、扫描仪和麦克风都是输入设备，显示器和打印机是输出设备。', 1, '计算机硬件', 1),
(1, 3, 'CPU是计算机的核心部件。', 'CPU（中央处理器）是计算机系统的核心部件，负责执行程序指令。', '[]', '正确', 'CPU确实是计算机的核心部件，负责程序的执行和数据的处理。', 1, 'CPU原理', 1);