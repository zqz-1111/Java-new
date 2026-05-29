# Tlias 智能学习辅助系统

基于 Spring Boot 的企业级后台管理系统，用于管理培训机构的部门、员工、班级和学员信息。

## 技术栈

| 技术 | 版本 | 说明 |
|------|------|------|
| Spring Boot | 4.0.6 | 应用框架 |
| MyBatis | 4.0.1 | ORM 框架 |
| MySQL | - | 数据库 |
| PageHelper | 1.4.7 | 分页插件 |
| JWT (jjwt) | 0.12.6 | 身份认证 |
| Lombok | - | 代码简化 |

## 项目结构

```
src/main/java/com/itheima/
├── TliasWebManagementApplication.java  # 启动类
├── config/
│   └── WebConfig.java                  # Web 配置（拦截器注册）
├── controller/
│   ├── LoginController.java            # 登录接口
│   ├── DeptController.java             # 部门管理
│   ├── EmpController.java              # 员工管理
│   ├── ClazzController.java            # 班级管理
│   ├── StudentController.java          # 学员管理
│   └── ReportController.java           # 数据统计
├── service/
│   ├── impl/
│   │   ├── EmpServiceImpl.java         # 员工业务实现（含登录逻辑）
│   │   ├── DeptServiceImpl.java        # 部门业务实现
│   │   ├── ClazzServiceImpl.java       # 班级业务实现
│   │   ├── StudentServiceImpl.java     # 学员业务实现
│   │   ├── ReportServiceImpl.java      # 统计业务实现
│   │   └── EmpLogServiceImpl.java      # 日志业务实现
│   └── ...
├── mapper/
│   ├── EmpMapper.java                  # 员工数据访问
│   ├── DeptMapper.java                 # 部门数据访问
│   ├── ClazzMapper.java                # 班级数据访问
│   ├── StudentMapper.java              # 学员数据访问
│   └── ...
├── pojo/                               # 实体类
│   ├── Emp.java                        # 员工
│   ├── Dept.java                       # 部门
│   ├── Clazz.java                      # 班级
│   ├── Student.java                    # 学员
│   ├── LoginInfo.java                  # 登录信息
│   ├── Result.java                     # 统一响应
│   └── ...
├── interceptor/
│   └── TokenInterceptor.java           # Token 校验拦截器
├── filter/
│   ├── TokenFilter.java                # Token 校验过滤器（备用）
│   ├── DemoFilter.java                 # 示例过滤器
│   └── XyzFilter.java                  # 示例过滤器
├── exception/
│   ├── GlobalExceptionHandler.java     # 全局异常处理
│   └── BusinessException.java          # 业务异常
└── utils/
    └── JwtUtils.java                   # JWT 工具类
```

## 功能模块

### 1. 登录认证
- 用户名密码登录
- JWT 令牌生成与验证
- 拦截器自动校验 Token
- 未登录返回 401 状态码

### 2. 部门管理
- 部门列表查询
- 部门增删改查
- 部门详情查看

### 3. 员工管理
- 员工分页条件查询
- 员工增删改查
- 批量删除员工
- 工作经历管理
- 操作日志记录

### 4. 班级管理
- 班级分页条件查询
- 班级增删改查
- 班主任关联

### 5. 学员管理
- 学员分页条件查询
- 学员增删改查
- 违纪处理

### 6. 数据统计
- 员工职位人数统计
- 员工性别统计
- 学员学历统计
- 班级人数统计

## 快速开始

### 环境要求
- JDK 17+
- Maven 3.6+
- MySQL 8.0+

### 数据库配置

1. 创建数据库：
```sql
CREATE DATABASE tlias DEFAULT CHARACTER SET utf8mb4;
```

2. 修改配置文件 `src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/tlias
    username: root
    password: your_password
```

### 运行项目

```bash
# 克隆项目
git clone https://github.com/zqz-1111/Java-new.git

# 进入项目目录
cd Tlias/tlias-web-management

# 运行
mvn spring-boot:run
```

访问：http://localhost:8080

## API 接口

### 登录接口
```
POST /login
Content-Type: application/json

{
  "username": "songjiang",
  "password": "123456"
}
```

### 业务接口（需要 Token）
```
GET /depts              # 部门列表
GET /emps               # 员工分页查询
GET /clazzs             # 班级分页查询
GET /students           # 学员分页查询
GET /report/empJobData  # 员工职位统计
...
```

请求头携带 Token：
```
token: eyJhbGciOiJIUzI1NiJ9...
```

## 登录校验流程

```
请求 → TokenInterceptor
  ├─ /login 请求 → 放行
  ├─ 无 Token → 返回 401
  ├─ Token 无效/过期 → 返回 401
  └─ Token 有效 → 放行 → Controller 处理
```

## 统一响应格式

```json
{
  "code": 1,
  "msg": "success",
  "data": {}
}
```

| 字段 | 说明 |
|------|------|
| code | 1 成功，0 失败 |
| msg | 提示信息 |
| data | 返回数据 |

## 开发说明

### 项目采用分层架构
- **Controller 层**：接收请求，参数校验，返回结果
- **Service 层**：业务逻辑处理，事务管理
- **Mapper 层**：数据访问，SQL 操作

### 异常处理
- 全局异常捕获 `GlobalExceptionHandler`
- 自定义业务异常 `BusinessException`
- 数据库唯一键冲突处理

## 许可证

MIT License
