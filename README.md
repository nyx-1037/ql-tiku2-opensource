# 在线刷题系统 (QL-Tiku2)

## 项目概述

QL-Tiku2 是一个基于AI技术的在线刷题平台，采用前后端分离架构。系统集成阿里云百炼大模型平台，支持智能出题、AI批改、智能问答等功能。后端基于Spring Boot + SpringAI Alibaba Dashscope构建，前端包含两个独立项目：管理端和客户端，均基于Vue3生态开发。

### 技术架构
- **后端**：Spring Boot 3.x + SpringAI Alibaba Dashscope + MyBatis-Plus + Redis + MySQL
- **管理端前端**：Vue3 + Webpack + Pinia + TDesign组件库
- **客户端前端**：Vue3 + Webpack + Pinia + Element Plus组件库
- **AI能力**：集成阿里云百炼大模型平台，支持通义千问系列模型

### 主要功能
- **用户端**：注册登录、智能刷题、AI问答、错题本、考试模拟、学习分析
- **管理端**：系统配置、用户管理、题库管理、AI模型管理、运营数据统计
- **AI功能**：智能出题、AI批改、智能答疑、学习建议、知识图谱分析
- **后端服务**：RESTful API、JWT认证、Redis缓存、文件上传、数据同步
- **发题策略**：基于Redis的异步发题策略，防止重复出题和并发问题

## 目录结构

```
ql-tiku2/
├── LICENSE                         # 开源许可证
├── README.md                       # 项目说明文档
├── ql-tiku-admin-ui/               # 前端管理端项目 (Vue3 + Webpack + Pinia + TDesign)
│   ├── .env.development            # 开发环境变量
│   ├── .env.production             # 生产环境变量
│   ├── .env.test                   # 测试环境变量
│   ├── .gitignore                  # Git 忽略文件
│   ├── babel.config.js             # Babel 配置
│   ├── jsconfig.json               # JavaScript 配置
│   ├── package.json                # 依赖配置
│   ├── package-lock.json           # 依赖锁定文件
│   ├── postcss.config.js           # PostCSS 配置
│   ├── vue.config.js               # Vue CLI 配置
│   ├── public/                     # 公共资源
│   │   ├── favicon.ico
│   │   └── index.html
│   ├── src/                        # 源代码
│   │   ├── api/                    # API 接口
│   │   ├── assets/                 # 静态资源
│   │   ├── components/             # 公共组件
│   │   ├── main.js                 # 入口文件
│   │   ├── router/                 # 路由配置
│   │   ├── store/                  # 状态管理 (Pinia)
│   │   └── views/                  # 页面组件
│   └── 快速删除node_module脚本.bat # Windows清理脚本
├── ql-tiku-client-ui/              # 前端客户端项目 (Vue3 + Webpack + Pinia + Element Plus)
│   ├── .env.development            # 开发环境变量
│   ├── .env.production             # 生产环境变量
│   ├── .env.test                   # 测试环境变量
│   ├── .gitignore                  # Git 忽略文件
│   ├── babel.config.js             # Babel 配置
│   ├── jsconfig.json               # JavaScript 配置
│   ├── package.json                # 依赖配置
│   ├── package-lock.json           # 依赖锁定文件
│   ├── postcss.config.js           # PostCSS 配置
│   ├── vue.config.js               # Vue CLI 配置
│   ├── public/                     # 公共资源
│   │   ├── compatibility-check.js  # 兼容性检查
│   │   ├── favicon.ico
│   │   └── index.html
│   ├── src/                        # 源代码
│   │   ├── api/                    # API 接口
│   │   ├── assets/                 # 静态资源
│   │   ├── components/             # 公共组件
│   │   ├── composables/            # 组合式函数
│   │   ├── config/                 # 配置文件
│   │   ├── main.js                 # 入口文件
│   │   ├── plugins/                # 插件
│   │   ├── router/                 # 路由配置
│   │   ├── store/                  # 状态管理 (Pinia)
│   │   ├── styles/                 # 样式文件
│   │   ├── utils/                  # 工具函数
│   │   └── views/                  # 页面组件
│   └── 快速删除node_module脚本.bat # Windows清理脚本
├── ql-tiku-system/                 # 后端项目 (Spring Boot 3.x + SpringAI Alibaba Dashscope)
│   ├── .gitignore                  # Git 忽略文件
│   ├── .idea/                      # IntelliJ IDEA 项目配置
│   ├── pom.xml                     # Maven 配置
│   ├── src/                        # 源代码
│   │   ├── main/
│   │   │   ├── java/               # Java 源码
│   │   │   └── resources/          # 资源文件
│   │   └── test/                   # 测试代码
│   └── target/                     # 编译输出目录
├── ql_tiku2.sql                    # 数据库初始化脚本
└── sys_user.sql                    # 系统用户初始化脚本
```

## 环境要求

### 后端环境
- **Java**: JDK 17+ (推荐 JDK 17)
- **Maven**: 3.6+
- **MySQL**: 5.7+ 或 8.0+
- **Redis**: 5.0+

### 前端环境
- **Node.js**: 14.x LTS 或 16.x LTS
- **npm**: 6.x+ 或 7.x+

### AI服务配置
- **阿里云百炼平台**: 需要开通阿里云百炼大模型服务
- **API Key**: 需要在阿里云控制台获取Dashscope API Key
- **网络要求**: 需要能够访问阿里云Dashscope服务

## 安装步骤

### 1. 克隆项目
```bash
git clone https://github.com/nyx-1037/ql-tiku2-opensource.git
cd ql-tiku2
```

### 2. 配置数据库
- 创建 MySQL 数据库：`ql_tiku2`

- 导入数据库表结构：`mysql -u root -p ql_tiku2 < ql_tiku2.sql`

- 导入用户初始化数据：`mysql -u root -p ql_tiku2 < sys_user.sql`

- 初始账号密码为 

   admin

  123456

### 3. 后端配置与安装
- 进入后端目录：`cd ql-tiku-system`
- 编辑配置文件：`src/main/resources/application.yml`
- 替换所有 `YOUR_*` 占位符为你的实际配置（详见[配置](#配置)部分）
- 设置 JAVA_HOME：`export JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.8.7-hotspot"` (Windows 使用 set)
- 安装依赖：`mvn clean install`
- 运行：`mvn spring-boot:run`

### 4. 前端配置与安装
#### 客户端 (ql-tiku-client-ui)
- 进入目录：`cd ql-tiku-client-ui`
- 复制环境变量模板：创建 `.env.development` 文件（可参考 `.env.development.example`）
- 编辑环境变量文件，配置API地址
- 安装依赖：`npm install`
- 运行开发服务器：`npm run serve` (访问 http://localhost:8089/)

#### 管理端 (ql-tiku-admin-ui)
- 进入目录：`cd ql-tiku-admin-ui`
- 复制环境变量模板：创建 `.env.development` 文件（可参考 `.env.development.example`）
- 编辑环境变量文件，配置API地址
- 安装依赖：`npm install`
- 运行开发服务器：`npm run serve`

## 配置

### 后端配置
- **主配置文件**：`ql-tiku-system/src/main/resources/application.yml` (数据库、Redis、AI服务等)
- **配置填写说明**：
  1. 将 `YOUR_DB_USERNAME` 和 `YOUR_DB_PASSWORD` 替换为你的MySQL数据库用户名和密码
  2. 将 `YOUR_REDIS_PASSWORD` 替换为你的Redis密码（如无密码可留空）
  3. 将 `YOUR_DASHSCOPE_API_KEY` 替换为阿里云百炼平台的API密钥
  4. 将 `YOUR_JWT_SECRET_KEY` 替换为至少256位的JWT安全密钥
  5. 将 `YOUR_FTP_HOST`、`YOUR_FTP_USERNAME`、`YOUR_FTP_PASSWORD` 替换为你的FTP配置
  6. 将 `YOUR_ALIYUN_ACCESS_KEY_ID`、`YOUR_ALIYUN_ACCESS_KEY_SECRET`、`YOUR_BUCKET_NAME`、`YOUR_REGION` 替换为你的阿里云OSS配置

- **AI服务配置示例**：
  ```yaml
  spring:
    ai:
      dashscope:
        api-key: YOUR_DASHSCOPE_API_KEY
        base-url: https://dashscope.aliyuncs.com
  ```

### 前端配置
- **环境变量**：各前端项目中的 `.env.development`、`.env.production`、`.env.test`
- **API配置**：在环境变量中配置后端API地址
  ```
  VUE_APP_API_BASE_URL=http://localhost:8888/api
  VUE_APP_CDN_PREFIX=your-cdn-prefix
  ```
- **配置填写说明**：
  1. 将 `VUE_APP_API_BASE_URL` 替换为你的后端API地址（如 `http://localhost:8888/api`）
  2. 将 `VUE_APP_CDN_PREFIX` 替换为你的CDN前缀（如使用阿里云OSS）

### 系统配置
- **维护模式**：通过后端 `sys_config` 表配置 `sys.maintenance.mode` 和 `sys.maintenance.message`
- **AI模型配置**：可在管理端动态配置AI模型参数和调用限制

## 开发注意事项

### 前端开发规范
- **管理端**：使用 TDesign 组件库，遵循 TDesign 设计规范
- **客户端**：使用 Element Plus 组件库，遵循 Element Plus 设计原则
- **状态管理**：统一使用 Pinia 进行状态管理
- **构建工具**：使用 Webpack 作为构建工具，支持热更新和代码分割
- **响应式设计**：所有页面必须支持响应式布局，适配Web和移动端
- **CDN资源**：所有CDN资源必须使用中国区可用地址，确保访问稳定性

### 后端开发规范
- **认证授权**：使用 Spring Security + JWT 实现无状态认证
- **数据库操作**：使用 MyBatis-Plus 简化数据库操作，支持代码生成
- **AI集成**：通过 SpringAI Alibaba Dashscope 集成阿里云百炼大模型
- **接口规范**：所有API遵循RESTful设计原则，统一返回格式
- **异常处理**：全局异常处理，统一错误码和错误信息
- **并发处理**：使用Redis的异步发题策略，防止重复出题和并发问题

### 项目构建要求
- **代码规范**：遵循阿里巴巴Java开发手册和Vue.js风格指南
- **编译要求**：项目必须编译无报错后才能启动，确保代码质量
- **环境隔离**：开发、测试、生产环境配置分离，通过环境变量控制

### 安全配置注意事项
- **敏感信息**：所有敏感配置（API密钥、密码等）已替换为 `YOUR_*` 占位符
- **生产环境**：建议使用环境变量或配置中心管理敏感信息
- **Git忽略**：确保 `.gitignore` 文件已正确配置，避免提交敏感文件

## 贡献

欢迎提交 Pull Request 或 Issue。

## 许可证

MIT License

## 项目图片展示

### 客户端图片展示


![屏幕截图 2025-08-19 143138](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20143138.png?raw=true)

![屏幕截图 2025-08-19 143759](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20143525.png?raw=true)

![屏幕截图 2025-08-19 143525](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20143525.png?raw=true)

![屏幕截图 2025-08-19 144209](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144209.png?raw=true)

![屏幕截图 2025-08-19 144136](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144136.png?raw=true)

![屏幕截图 2025-08-19 144104](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144104.png?raw=true)

![屏幕截图 2025-08-19 144052](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144052.png?raw=true)

![屏幕截图 2025-08-19 144022](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144022.png?raw=true)

![屏幕截图 2025-08-19 144008](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144008.png?raw=true)

![屏幕截图 2025-08-19 143936](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20143936.png?raw=true)

![屏幕截图 2025-08-19 144439](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144439.png?raw=true)

### 管理端图片展示



![屏幕截图 2025-08-19 144533](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144533.png?raw=true)

![屏幕截图 2025-08-19 144422](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144422.png?raw=true)

![屏幕截图 2025-08-19 144354](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144354.png?raw=true)

![屏幕截图 2025-08-19 144320](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144320.png?raw=true)

![屏幕截图 2025-08-19 144240](https://github.com/nyx-1037/ql-tiku2-opensource/blob/master/assert/%E5%B1%8F%E5%B9%95%E6%88%AA%E5%9B%BE%202025-08-19%20144240.png?raw=true)

欢迎体验！有意咨询可联系 nyx1037678078@Gmail.com