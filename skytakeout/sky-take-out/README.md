# 苍穹外卖 Sky Takeout

> 一个完整的外卖订餐系统，包含管理端（Vue.js）和用户端（微信小程序），后端基于 Spring Boot 构建。

## 目录

- [环境配置](#环境配置)
- [项目结构](#项目结构)
- [功能模块](#功能模块)
  - [1. 员工管理](#1-员工管理)
  - [2. 分类管理](#2-分类管理)
  - [3. 菜品管理](#3-菜品管理)
  - [4. 套餐管理](#4-套餐管理)
  - [5. 店铺管理](#5-店铺管理)
  - [6. 微信登录](#6-微信登录)
  - [7. 地址簿管理](#7-地址簿管理)
  - [8. 购物车](#8-购物车)
  - [9. 用户下单](#9-用户下单)
  - [10. 模拟支付](#10-模拟支付)
  - [11. 订单管理](#11-订单管理)
  - [12. 数据统计](#12-数据统计)
  - [13. 工作台](#13-工作台)
  - [14. WebSocket通知](#14-websocket通知)
  - [15. 定时任务](#15-定时任务)
  - [16. 文件上传](#16-文件上传)
  - [17. Redis缓存](#17-redis缓存)
- [常见问题](#常见问题)

---

## 环境配置

### 基础环境

| 组件 | 版本 |
|------|------|
| JDK | 1.8+ |
| Maven | 3.6+ |
| MySQL | 5.7+ |
| Redis | 6.0+ |
| MinIO | 最新版 |
| nginx | 1.20+ |

### 配置文件

**application-dev.yml** 主要配置：

```yaml
sky:
  datasource:
    host: localhost
    port: 3306
    database: sky_take_out
    username: xxxx
    password: xxxx

  redis:
    host: localhost
    port: 6379
    database: 0

  wechat:
    appid: 你的appid
    secret: 你的secret

  minio:
    endpoint: http://localhost:9000
    access-key: minioadmin
    secret-key: minioadmin
    bucket-name: sky
```

### nginx 配置

```nginx
# 管理端前端
location / {
    root html/sky;
    index index.html;
}

# 管理端API代理
location /api/ {
    proxy_pass http://localhost:8080/admin/;
}

# 用户端API代理
location /user/ {
    proxy_pass http://localhost:8080/user/;
}

# WebSocket代理
location /ws/ {
    proxy_pass http://localhost:8080/ws/;
    proxy_http_version 1.1;
    proxy_set_header Upgrade $http_upgrade;
    proxy_set_header Connection "upgrade";
}
```

### 启动顺序

1. 启动 MySQL、Redis、MinIO
2. 启动后端 `SkyApplication.java`
3. 启动 nginx
4. 打开管理端 `http://localhost/sky/`
5. 微信开发者工具打开小程序

---

## 项目结构

```
sky-take-out/
├── sky-common/          # 公共模块（工具类、常量、异常）
├── sky-pojo/            # 实体模块（Entity、DTO、VO）
└── sky-server/          # 服务模块（Controller、Service、Mapper）
    └── src/main/java/com/sky/
        ├── controller/
        │   ├── admin/   # 管理端接口
        │   └── user/    # 用户端接口
        ├── service/
        ├── mapper/
        ├── config/      # 配置类
        ├── interceptor/ # 拦截器
        ├── aspect/      # AOP切面
        ├── task/        # 定时任务
        └── websocket/   # WebSocket
```

---

## 功能模块

### 1. 员工管理

#### 功能说明
管理端员工的登录、登出、增删改查、启停用。

#### 实现流程

**登录流程：**
1. 前端提交 `POST /admin/employee/login`，参数 `{username, password}`
2. `EmployeeController` 接收请求，调用 `EmployeeService.login()`
3. Service 根据 username 查询数据库
4. 比对密码（MD5加密）
5. 生成 JWT Token 返回给前端
6. 前端将 Token 存入 localStorage，后续请求携带 Token

**增删改查：**
- 新增：密码 MD5 加密后存储
- 分页查询：使用 PageHelper 插件
- 启停用：修改 status 字段（1启用，0停用）

#### 关键代码

```java
// 登录
@PostMapping("/login")
public Result<EmployeeLoginVO> login(@RequestBody EmployeeLoginDTO dto) {
    Employee employee = employeeService.login(dto);
    // 生成JWT
    Map<String, Object> claims = new HashMap<>();
    claims.put("EmpId", employee.getId());
    String token = JwtUtil.createJWT(jwtProperties.getSecretKey(), jwtProperties.getTtl(), claims);
    return Result.success(EmployeeLoginVO.builder().token(token).build());
}
```

#### 遇到的问题

**问题：** Bean 名称冲突，管理端和用户端都有 `EmployeeController`
**解决：** 使用 `@RestController("adminEmployeeController")` 指定不同 Bean 名称

#### 快速回顾

1. 密码使用 MD5 加密存储，登录时比对加密后的值
2. 登录成功后生成 JWT Token，前端存入 localStorage
3. 后续请求通过拦截器 `JwtTokenAdminInterceptor` 验证 Token
4. 分页查询使用 PageHelper，只需 `PageHelper.startPage(pageNum, pageSize)`
5. Bean 名称冲突用 `@RestController("beanName")` 解决

---

### 2. 分类管理

#### 功能说明
管理菜品和套餐的分类，支持增删改查和启停用。

#### 实现流程

1. `CategoryController` 提供 CRUD 接口
2. `CategoryService` 处理业务逻辑
3. `CategoryMapper` 使用 MyBatis 操作数据库
4. 删除分类时需检查是否有关联的菜品或套餐

#### 关键代码

```java
// 分页查询
@GetMapping("/page")
public Result<PageResult> page(CategoryPageQueryDTO dto) {
    PageResult pageResult = categoryService.pageQuery(dto);
    return Result.success(pageResult);
}

// 删除（需检查关联）
@DeleteMapping
public Result delete(Long id) {
    // 检查分类下是否有菜品
    if (dishService.countByCategoryId(id) > 0) {
        throw new DeletionNotAllowedException(MessageConstant.CATEGORY_BE_RELATED_BY_DISH);
    }
    categoryService.delete(id);
    return Result.success();
}
```

#### 快速回顾

1. 分类类型：1菜品分类，2套餐分类
2. 删除前必须检查是否有关联的菜品/套餐
3. 分页查询统一使用 PageHelper + PageResult
4. 启停用修改 status 字段即可

---

### 3. 菜品管理

#### 功能说明
管理菜品的增删改查、启停用，支持口味配置和图片上传。

#### 实现流程

**新增菜品流程：**
1. 前端提交菜品信息（名称、价格、分类、口味、图片）
2. `DishController` 接收 `DishDTO`（包含 `DishFlavor` 列表）
3. Service 事务操作：
   - 插入 `dish` 表
   - 批量插入 `dish_flavor` 表
4. 启停用时清除 Redis 缓存

**查询菜品流程：**
1. 先查 Redis 缓存（key: `dish_{categoryId}`）
2. 缓存命中直接返回
3. 缓存未命中查数据库，结果存入 Redis

#### 关键代码

```java
// 新增菜品（事务）
@Transactional
public void saveWithFlavor(DishDTO dishDTO) {
    Dish dish = new Dish();
    BeanUtils.copyProperties(dishDTO, dish);
    dishMapper.insert(dish); // 返回自增id
    Long dishId = dish.getId();
    List<DishFlavor> flavors = dishDTO.getFlavors();
    if (flavors != null && !flavors.isEmpty()) {
        flavors.forEach(f -> f.setDishId(dishId));
        dishFlavorMapper.insertBatch(flavors);
    }
}

// 启停用时清除缓存
@PostMapping("/status/{status}")
public Result startOrStop(@PathVariable Integer status, Long id) {
    dishService.startOrStop(status, id);
    cleanCache("dish_*"); // 清除所有菜品缓存
    return Result.success();
}
```

#### 遇到的问题

**问题：** 修改菜品启停用状态后，小程序端不更新
**原因：** Redis 缓存未清除
**解决：** 在 Controller 层（不是 Service 层）清除缓存

#### 快速回顾

1. 新增菜品需要同时操作 `dish` 和 `dish_flavor` 两张表（事务）
2. 菜品缓存 key 为 `dish_{categoryId}`，使用 RedisTemplate 直接操作
3. 启停用、修改、删除时必须清除缓存
4. 缓存清除在 Controller 层执行，不在 Service 层
5. 口味数据存储在 `dish_flavor` 表，通过 `dish_id` 关联

---

### 4. 套餐管理

#### 功能说明
管理套餐的增删改查、启停用，套餐包含多个菜品。

#### 实现流程

**新增套餐流程：**
1. 前端提交套餐信息（名称、价格、分类、包含的菜品列表）
2. `SetmealController` 接收 `SetmealDTO`（包含 `SetmealDish` 列表）
3. Service 事务操作：
   - 插入 `setmeal` 表
   - 批量插入 `setmeal_dish` 表
4. 使用 Spring Cache 注解管理缓存

#### 关键代码

```java
// 新增套餐（带缓存清除）
@PostMapping
@CacheEvict(cacheNames = "setmealCache", key = "#setmealDTO.categoryId")
public Result save(@RequestBody SetmealDTO setmealDTO) {
    setmealService.saveWithDish(setmealDTO);
    return Result.success();
}

// 用户端查询套餐（带缓存）
@GetMapping("/list")
@Cacheable(cacheNames = "setmealCache", key = "#categoryId")
public Result<List<Setmeal>> list(Long categoryId) {
    return Result.success(setmealService.list(categoryId));
}
```

#### 快速回顾

1. 套餐和菜品是多对多关系，通过 `setmeal_dish` 中间表关联
2. 使用 Spring Cache 管理缓存（@Cacheable、@CacheEvict）
3. `@CacheEvict` 的 key 要和 `@Cacheable` 的 key 对应
4. 删除套餐时需要同时删除 `setmeal_dish` 关联数据
5. 启停用套餐时清除所有套餐缓存（allEntries=true）

---

### 5. 店铺管理

#### 功能说明
管理端设置营业状态，用户端查询营业状态。状态存储在 Redis 中。

#### 实现流程

1. 管理端 `PUT /admin/shop/{status}` 设置营业状态
2. 将状态存入 Redis（key: `SHOP_STATUS`）
3. 用户端 `GET /user/shop/status` 查询营业状态
4. 从 Redis 读取状态返回

#### 关键代码

```java
// 管理端设置状态
@PutMapping("/{status}")
public Result setStatus(@PathVariable Integer status) {
    redisTemplate.opsForValue().set("SHOP_STATUS", status);
    return Result.success();
}

// 用户端查询状态
@GetMapping("/status")
public Result<Integer> getStatus() {
    Integer status = (Integer) redisTemplate.opsForValue().get("SHOP_STATUS");
    return Result.success(status);
}
```

#### 遇到的问题

**问题：** 管理端和用户端都有 `ShopController`，Bean 名称冲突
**解决：** 用户端使用 `@RestController("userShopController")`

#### 快速回顾

1. 营业状态存储在 Redis，key 为 `SHOP_STATUS`
2. 管理端可读写，用户端只读
3. 使用 `redisTemplate.opsForValue()` 操作字符串类型
4. 两个 Controller 用不同 Bean 名称区分

---

### 6. 微信登录

#### 功能说明
用户通过微信小程序扫码登录，获取 openid 建立用户会话。

#### 实现流程

1. 小程序调用 `wx.login()` 获取临时登录凭证 `code`
2. 小程序将 `code` 发送到后端 `POST /user/user/login`
3. 后端用 `code` 调用微信接口 `jscode2session` 获取 `openid`
4. 根据 `openid` 查询数据库：
   - 用户存在：直接登录
   - 用户不存在：自动注册
5. 生成 JWT Token 返回给小程序
6. 小程序存储 Token，后续请求携带

#### 关键代码

```java
// 用户登录
@PostMapping("/login")
public Result<UserLoginVO> login(@RequestBody UserLoginDTO dto) {
    // 调用微信接口获取openid
    String openid = wxService.getOpenid(dto.getCode());
    // 查询用户
    User user = userMapper.getByOpenid(openid);
    if (user == null) {
        // 自动注册
        user = User.builder().openid(openid).build();
        userMapper.insert(user);
    }
    // 生成JWT
    Map<String, Object> claims = new HashMap<>();
    claims.put("userId", user.getId());
    String token = JwtUtil.createJWT(...);
    return Result.success(UserLoginVO.builder().id(user.getId()).token(token).build());
}
```

#### 遇到的问题

**问题：** `JwtTokenUserInterceptor` 导入错误的 `HandlerMethod` 类
**原因：** 用了 `org.springframework.messaging.handler.HandlerMethod`（消息模块）
**解决：** 改为 `org.springframework.web.method.HandlerMethod`

**问题：** 小程序没有登录弹窗
**原因：** `wx.login()` 是静默登录，不会弹窗。`wx.getUserProfile()` 已废弃
**解决：** 使用 `<button open-type="getUserInfo">` 组件获取用户信息

#### 快速回顾

1. 微信登录核心是获取 `openid`，通过 `code` 调用微信接口
2. `openid` 是用户唯一标识，用于判断是否已注册
3. 未注册用户自动创建（自动注册）
4. 后续认证使用 JWT Token，不是 openid
5. 用户端和管理端使用不同的拦截器（`JwtTokenUserInterceptor` / `JwtTokenAdminInterceptor`）

---

### 7. 地址簿管理

#### 功能说明
用户管理收货地址，支持增删改查和设置默认地址。

#### 实现流程

1. `AddressBookController` 提供完整 CRUD 接口
2. 每个用户有多个地址，通过 `user_id` 关联
3. 设置默认地址时：
   - 先将该用户所有地址设为非默认
   - 再将指定地址设为默认

#### 关键代码

```java
// 设置默认地址
@PutMapping("/default")
public Result setDefault(@RequestBody AddressBook addressBook) {
    // 先全部设为非默认
    addressBook.setIsDefault(0);
    addressBookMapper.updateIsDefaultByUserId(addressBook);
    // 再设指定地址为默认
    addressBook.setIsDefault(1);
    addressBookMapper.update(addressBook);
    return Result.success();
}
```

#### 快速回顾

1. 地址和用户是一对多关系，通过 `user_id` 关联
2. 默认地址字段 `is_default`（0否，1是）
3. 设置默认地址需要两步操作：先清除，再设置
4. 下单时通过 `addressBookId` 关联地址信息

---

### 8. 购物车

#### 功能说明
用户添加、删除、清空购物车，支持菜品和套餐。

#### 实现流程

**添加购物车流程：**
1. 前端提交 `POST /user/shoppingCart/add`，参数 `{dishId, setmealId, dishFlavor}`
2. 查询购物车中是否已存在相同商品（同用户、同菜品/套餐、同口味）
3. 存在：数量 +1
4. 不存在：查询菜品/套餐信息，插入新记录

**减少购物车流程：**
1. 前端提交 `POST /user/shoppingCart/sub`
2. 查询购物车中的商品
3. 数量 > 1：数量 -1
4. 数量 = 1：删除记录

#### 关键代码

```java
// 添加购物车
public void addShoppingCart(ShoppingCartDTO dto) {
    ShoppingCart cart = new ShoppingCart();
    BeanUtils.copyProperties(dto, cart);
    cart.setUserId(BaseContext.getCurrentId());
    // 查询是否已存在
    List<ShoppingCart> list = shoppingCartMapper.list(cart);
    if (list != null && list.size() > 0) {
        // 已存在，数量+1
        ShoppingCart existing = list.get(0);
        existing.setNumber(existing.getNumber() + 1);
        shoppingCartMapper.updateNumberById(existing);
    } else {
        // 不存在，查询商品信息并插入
        if (dto.getDishId() != null) {
            Dish dish = dishMapper.getById(dto.getDishId());
            cart.setName(dish.getName());
            cart.setImage(dish.getImage());
            cart.setAmount(dish.getPrice());
        } else {
            Setmeal setmeal = setmealMapper.getById(dto.getSetmealId());
            cart.setName(setmeal.getName());
            cart.setImage(setmeal.getImage());
            cart.setAmount(setmeal.getPrice());
        }
        cart.setNumber(1);
        cart.setCreateTime(LocalDateTime.now());
        shoppingCartMapper.insert(cart);
    }
}
```

#### 遇到的问题

**问题：** 减少购物车接口报 405 Method Not Allowed
**原因：** 前端发送 POST，后端用了 @DeleteMapping
**解决：** 改为 @PostMapping

#### 快速回顾

1. 购物车通过 `user_id` 关联用户，每个用户独立
2. 添加时先判断是否已存在（同用户+同商品+同口味）
3. 已存在数量+1，不存在插入新记录
4. 减少时数量>1则-1，数量=1则删除
5. 下单后清空购物车

---

### 9. 用户下单

#### 功能说明
用户提交订单，生成订单记录和订单明细，清空购物车。

#### 实现流程

1. 前端提交 `POST /user/order/submit`，参数 `{addressBookId, payMethod, remark, ...}`
2. 业务校验：
   - 地址簿是否存在
   - 购物车是否为空
3. 生成订单号（时间戳）
4. 插入 `orders` 表（状态：待付款）
5. 批量插入 `order_detail` 表
6. 清空购物车
7. 返回订单信息（id、订单号、金额、时间）

#### 关键代码

```java
@Transactional
public OrderSubmitVO submitOrder(OrdersSubmitDTO dto) {
    // 1. 校验地址
    AddressBook addressBook = addressBookMapper.getById(dto.getAddressBookId());
    if (addressBook == null) {
        throw new AddressBookBusinessException(MessageConstant.ADDRESS_BOOK_IS_NULL);
    }
    // 2. 校验购物车
    List<ShoppingCart> cartList = shoppingCartMapper.list(...);
    if (cartList == null || cartList.size() == 0) {
        throw new ShoppingCartBusinessException(MessageConstant.SHOPPING_CART_IS_NULL);
    }
    // 3. 生成订单
    Orders orders = new Orders();
    BeanUtils.copyProperties(dto, orders);
    orders.setOrderTime(LocalDateTime.now());
    orders.setPayStatus(Orders.UN_PAID);
    orders.setStatus(Orders.PENDING_PAYMENT);
    orders.setNumber(String.valueOf(System.currentTimeMillis()));
    orders.setPhone(addressBook.getPhone());
    orders.setConsignee(addressBook.getConsignee());
    orders.setAddress(addressBook.getProvinceName() + addressBook.getCityName()
                     + addressBook.getDistrictName() + addressBook.getDetail());
    orders.setUserId(BaseContext.getCurrentId());
    orderMapper.insert(orders);
    // 4. 插入订单明细
    List<OrderDetail> detailList = new ArrayList<>();
    for (ShoppingCart cart : cartList) {
        OrderDetail detail = new OrderDetail();
        BeanUtils.copyProperties(cart, detail);
        detail.setOrderId(orders.getId());
        detailList.add(detail);
    }
    orderDetailMapper.insertBatch(detailList);
    // 5. 清空购物车
    shoppingCartMapper.deleteByUserId(BaseContext.getCurrentId());
    // 6. 返回VO
    return OrderSubmitVO.builder()
            .id(orders.getId())
            .orderNumber(orders.getNumber())
            .orderTime(orders.getOrderTime())
            .orderAmount(orders.getAmount())
            .build();
}
```

#### 遇到的问题

**问题：** 购物车为空时错误信息显示"地址簿为空"
**原因：** 错误常量写错了，用了 `ADDRESS_BOOK_IS_NULL`
**解决：** 改为 `SHOPPING_CART_IS_NULL`

**问题：** 订单缺少地址信息
**原因：** 没有设置 `address` 字段
**解决：** 拼接省+市+区+详细地址

**问题：** 支付时报 400 Bad Request
**原因：** 下单返回的 VO 没有 `orderNumber`，前端传空值
**解决：** 在 VO 中添加 `orderNumber` 字段

#### 快速回顾

1. 下单是事务操作：插入订单、插入明细、清空购物车
2. 订单号使用时间戳生成
3. 地址信息需要拼接（省+市+区+详细地址）
4. 订单状态初始为 `待付款(1)`
5. 返回的 VO 必须包含 `orderNumber`，否则支付时会报错

---

### 10. 模拟支付

#### 功能说明
模拟微信支付，更新订单状态为"待接单"。

#### 实现流程

1. 前端提交 `PUT /user/order/payment`，参数 `{orderNumber, payMethod}`
2. 根据订单号查询订单
3. 校验订单状态是否为"待付款"
4. 更新订单：
   - 状态：待付款(1) → 待接单(2)
   - 支付状态：未支付(0) → 已支付(1)
   - 结账时间：当前时间
   - 支付方式：前端传入
5. 通过 WebSocket 推送来单提醒

#### 关键代码

```java
public void paySuccess(String orderNumber, Integer payMethod) {
    Orders orders = orderMapper.getByNumber(orderNumber);
    if (orders == null || !orders.getStatus().equals(Orders.PENDING_PAYMENT)) {
        return;
    }
    orders.setStatus(Orders.TO_BE_CONFIRMED);
    orders.setPayStatus(Orders.PAID);
    orders.setPayMethod(payMethod);
    orders.setCheckoutTime(LocalDateTime.now());
    orderMapper.updateStatus(orders);
    // WebSocket推送来单提醒
    Map map = new HashMap();
    map.put("type", 1);
    map.put("orderId", orders.getId());
    map.put("content", "订单号：" + orders.getNumber());
    webSocketServer.sendToAllClient(JSON.toJSONString(map));
}
```

#### 遇到的问题

**问题：** 前端传参报 400
**原因：** 前端用 `data` 发送 JSON，后端用 `@RequestParam` 接收
**解决：** 改为 `@RequestBody OrdersPaymentDTO`

#### 快速回顾

1. 模拟支付核心就是更新订单状态
2. 状态流转：待付款(1) → 待接单(2)
3. 前端用 `data` 发送 JSON，后端必须用 `@RequestBody` 接收
4. 支付成功后推送 WebSocket 来单提醒
5. 模拟支付不影响后续功能，状态流转一致

---

### 11. 订单管理

#### 功能说明
用户端：订单列表、详情、取消、再来一单、催单
管理端：订单搜索、统计、接单、拒单、取消、派送、完成

#### 状态流转

```
待付款(1) → 待接单(2) → 已接单(3) → 派送中(4) → 已完成(5)
                ↓                        ↓
             已取消(6)                 已取消(6)
```

#### 用户端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /user/order/historyOrders | GET | 历史订单（分页） |
| /user/order/orderDetail/{id} | GET | 订单详情 |
| /user/order/cancel/{id} | PUT | 取消订单 |
| /user/order/repetition/{id} | POST | 再来一单 |
| /user/order/reminder/{id} | GET | 催单 |

#### 管理端接口

| 接口 | 方法 | 说明 |
|------|------|------|
| /admin/order/conditionSearch | GET | 订单搜索 |
| /admin/order/statistics | GET | 订单统计 |
| /admin/order/details/{id} | GET | 订单详情 |
| /admin/order/confirm | PUT | 接单 |
| /admin/order/rejection | PUT | 拒单 |
| /admin/order/cancel | PUT | 取消 |
| /admin/order/delivery/{id} | PUT | 派送 |
| /admin/order/complete/{id} | PUT | 完成 |

#### 关键代码

```java
// 取消订单
public void userCancelById(Long id) {
    Orders ordersDB = orderMapper.getById(id);
    if (ordersDB == null) {
        throw new OrderBusinessException(MessageConstant.ORDER_NOT_FOUND);
    }
    // 只有待付款和待接单可以取消
    if (ordersDB.getStatus() > 2) {
        throw new OrderBusinessException(MessageConstant.ORDER_STATUS_ERROR);
    }
    Orders orders = new Orders();
    orders.setId(ordersDB.getId());
    // 待接单取消需要退款
    if (ordersDB.getStatus().equals(Orders.TO_BE_CONFIRMED)) {
        orders.setPayStatus(Orders.REFUND);
    }
    orders.setStatus(Orders.CANCELLED);
    orders.setCancelReason("用户取消");
    orders.setCancelTime(LocalDateTime.now());
    orderMapper.update(orders);
}

// 再来一单
public void repetition(Long id) {
    Long userId = BaseContext.getCurrentId();
    List<OrderDetail> detailList = orderDetailMapper.getByOrderId(id);
    List<ShoppingCart> cartList = detailList.stream().map(x -> {
        ShoppingCart cart = new ShoppingCart();
        BeanUtils.copyProperties(x, cart, "id");
        cart.setUserId(userId);
        cart.setCreateTime(LocalDateTime.now());
        return cart;
    }).collect(Collectors.toList());
    shoppingCartMapper.insertBatch(cartList);
}
```

#### 遇到的问题

**问题：** OrderMapper 缺少 `update` 方法
**原因：** 取消订单需要更新多个字段（状态、取消原因、取消时间），`updateStatus` 不够用
**解决：** 新增 `update` 方法和对应的 XML SQL

#### 快速回顾

1. 订单状态流转是核心，每个状态变更都需要校验前置状态
2. 取消订单：待付款直接取消，待接单取消需要退款
3. 再来一单：查询订单明细 → 转为购物车 → 批量插入
4. 催单通过 WebSocket 推送 type=2 消息
5. 管理端和用户端共用同一个 Service，通过不同 Controller 暴露接口

---

### 12. 数据统计

#### 功能说明
统计营业额、用户数、订单数、销量Top10，支持导出 Excel。

#### 接口列表

| 接口 | 说明 |
|------|------|
| /admin/report/turnoverStatistics | 营业额统计 |
| /admin/report/userStatistics | 用户统计 |
| /admin/report/ordersStatistics | 订单统计 |
| /admin/report/top10 | 销量Top10 |
| /admin/report/export | 导出Excel |

#### 实现流程

**营业额统计：**
1. 生成日期列表（begin 到 end）
2. 遍历每天，查询已完成订单的金额合计
3. 返回日期列表和营业额列表（逗号分隔）

**导出 Excel：**
1. 读取模板文件 `template/运营数据报表模板.xlsx`
2. 查询最近30天的营业数据
3. 使用 POI 填充数据到 Excel
4. 设置响应头，输出到浏览器下载

#### 关键代码

```java
// 营业额统计
public TurnoverReportVO getTurnoverStatistics(LocalDate begin, LocalDate end) {
    List<LocalDate> dateList = new ArrayList<>();
    dateList.add(begin);
    while (!begin.equals(end)) {
        begin = begin.plusDays(1);
        dateList.add(begin);
    }
    List<Double> turnoverList = new ArrayList<>();
    for (LocalDate date : dateList) {
        LocalDateTime beginTime = LocalDateTime.of(date, LocalTime.MIN);
        LocalDateTime endTime = LocalDateTime.of(date, LocalTime.MAX);
        Map map = new HashMap();
        map.put("begin", beginTime);
        map.put("end", endTime);
        map.put("status", Orders.COMPLETED);
        Double turnover = orderMapper.sumByMap(map);
        turnover = turnover == null ? 0.0 : turnover;
        turnoverList.add(turnover);
    }
    return TurnoverReportVO.builder()
            .dateList(StringUtils.join(dateList, ","))
            .turnoverList(StringUtils.join(turnoverList, ","))
            .build();
}
```

#### 遇到的问题

**问题：** UserMapper.xml 有两个 `countByMap`（重复）
**解决：** 删除多余的，只保留 user 表的查询

**问题：** `getSalesTop10` SQL 写在了 UserMapper.xml
**解决：** 移到 OrderMapper.xml

**问题：** 导出 Excel 每天数据都一样
**原因：** 循环中查询结果没有赋值给变量
**解决：** `businessDatavo = workspaceService.getBusinessData(...)`

#### 快速回顾

1. 统计数据都是按天遍历，逐日查询数据库
2. VO 中的列表用逗号分隔的字符串（前端 ECharts 需要）
3. 导出 Excel 使用 POI + 模板文件
4. 查询条件用 Map 传递，SQL 用动态 `<if>` 判断
5. 营业额只统计已完成(status=5)的订单

---

### 13. 工作台

#### 功能说明
管理端首页展示今日营业数据和各模块总览。

#### 接口列表

| 接口 | 说明 |
|------|------|
| /admin/workspace/businessData | 今日营业数据 |
| /admin/workspace/overviewOrders | 订单总览 |
| /admin/workspace/overviewDishes | 菜品总览 |
| /admin/workspace/overviewSetmeals | 套餐总览 |

#### 实现流程

**今日营业数据：**
1. 查询今日的总订单数、有效订单数、营业额
2. 计算订单完成率 = 有效订单数 / 总订单数
3. 计算平均客单价 = 营业额 / 有效订单数
4. 查询今日新增用户数

#### 关键代码

```java
public BusinessDataVO getBusinessData(LocalDateTime begin, LocalDateTime end) {
    Map map = new HashMap();
    map.put("begin", begin);
    map.put("end", end);
    // 总订单数
    Integer totalOrderCount = orderMapper.countByMap(map);
    map.put("status", Orders.COMPLETED);
    // 营业额
    Double turnover = orderMapper.sumByMap(map);
    turnover = turnover == null ? 0.0 : turnover;
    // 有效订单数
    Integer validOrderCount = orderMapper.countByMap(map);
    // 计算完成率和客单价
    Double orderCompletionRate = 0.0;
    Double unitPrice = 0.0;
    if (totalOrderCount != 0 && validOrderCount != 0) {
        orderCompletionRate = validOrderCount.doubleValue() / totalOrderCount;
        unitPrice = turnover / validOrderCount;
    }
    // 新增用户
    Integer newUsers = userMapper.countByMap(map);
    return BusinessDataVO.builder()
            .turnover(turnover)
            .validOrderCount(validOrderCount)
            .orderCompletionRate(orderCompletionRate)
            .unitPrice(unitPrice)
            .newUsers(newUsers)
            .build();
}
```

#### 快速回顾

1. 工作台数据都是实时查询，不走缓存
2. 复用 `countByMap` 和 `sumByMap` 方法，通过 Map 传条件
3. 除法运算需要判断除数不为0
4. 营业额只统计已完成订单
5. 订单总览按状态分组统计

---

### 14. WebSocket通知

#### 功能说明
实时推送来单提醒和催单提醒到管理端。

#### 实现流程

1. `WebSocketConfiguration` 注册 `ServerEndpointExporter`
2. `WebSocketServer` 使用 `@ServerEndpoint("/ws/{sid}")` 暴露端点
3. 管理端前端连接 WebSocket
4. 支付成功/催单时，服务端调用 `sendToAllClient()` 群发消息

#### 消息格式

```json
{
    "type": 1,        // 1=来单提醒, 2=催单提醒
    "orderId": 123,
    "content": "订单号：1749876543210"
}
```

#### 关键代码

```java
@Component
@ServerEndpoint("/ws/{sid}")
public class WebSocketServer {
    private static Map<String, Session> sessionMap = new HashMap();

    @OnOpen
    public void onOpen(Session session, @PathParam("sid") String sid) {
        sessionMap.put(sid, session);
    }

    @OnClose
    public void onClose(@PathParam("sid") String sid) {
        sessionMap.remove(sid);
    }

    public void sendToAllClient(String message) {
        for (Session session : sessionMap.values()) {
            try {
                session.getBasicRemote().sendText(message);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
```

#### 遇到的问题

**问题：** 浏览器报 `NotAllowedError: play() failed`
**原因：** 浏览器安全策略，未交互的页面不能自动播放音频
**解决：** 先点击页面任意位置，后续音频就能播放

#### 快速回顾

1. WebSocket 端点 `/ws/{sid}`，sid 用于标识客户端
2. `sessionMap` 存储所有连接的会话
3. `sendToAllClient` 群发消息给所有连接的客户端
4. nginx 需要配置 WebSocket 代理（Upgrade 头）
5. 浏览器需要先交互才能播放音频

---

### 15. 定时任务

#### 功能说明
自动处理超时订单和长时间派送中的订单。

#### 任务列表

| 任务 | Cron | 说明 |
|------|------|------|
| 超时订单取消 | 每分钟 | 待付款超过15分钟自动取消 |
| 派送自动完成 | 每天凌晨1点 | 派送中超过60分钟自动完成 |

#### 关键代码

```java
@Component
public class OrderTask {
    @Scheduled(cron = "0 * * * * ?")
    public void processTimeoutOrder() {
        LocalDateTime time = LocalDateTime.now().plusMinutes(-15);
        List<Orders> list = orderMapper.getByStatusAndOrderTimeLT(Orders.PENDING_PAYMENT, time);
        if (list != null && list.size() > 0) {
            for (Orders orders : list) {
                orders.setStatus(Orders.CANCELLED);
                orders.setCancelReason("订单超时，自动取消");
                orders.setCancelTime(LocalDateTime.now());
                orderMapper.update(orders);
            }
        }
    }
}
```

#### 快速回顾

1. 需要在启动类加 `@EnableScheduling`
2. Cron 表达式 `0 * * * * ?` 表示每分钟执行
3. 查询条件：状态 + 时间小于（当前时间 - 超时时长）
4. 批量更新需要遍历逐条更新
5. Druid 报错 `discard long time connection` 是正常的，可忽略

---

### 16. 文件上传

#### 功能说明
上传图片到 MinIO 对象存储，返回可访问的 URL。

#### 实现流程

1. 前端提交文件到 `POST /admin/common/upload`
2. `CommonController` 接收 `MultipartFile`
3. 调用 `MinIOUtil.upload()` 上传到 MinIO
4. 返回文件的可访问 URL

#### 关键代码

```java
@PostMapping("/upload")
public Result<String> upload(MultipartFile file) {
    String url = minIOUtil.upload(file);
    return Result.success(url);
}
```

#### 快速回顾

1. MinIO 是对象存储，类似阿里云 OSS
2. 上传返回的是可直接访问的 URL
3. 需要配置 MinIO 的 endpoint、access-key、secret-key
4. `MultipartFile` 是 Spring 的文件上传封装

---

### 17. Redis缓存

#### 功能说明
缓存菜品和套餐数据，提高查询性能。

#### 缓存策略

| 数据 | 缓存方式 | Key |
|------|----------|-----|
| 菜品 | RedisTemplate | `dish_{categoryId}` |
| 套餐 | Spring Cache | `setmealCache::{categoryId}` |

#### 关键代码

```java
// 菜品缓存（直接操作RedisTemplate）
@GetMapping("/list")
public Result<List<DishVO>> list(Long categoryId) {
    String key = "dish_" + categoryId;
    List<DishVO> list = (List<DishVO>) redisTemplate.opsForValue().get(key);
    if (list != null && list.size() > 0) {
        return Result.success(list);
    }
    list = dishService.listWithFlavor(categoryId);
    redisTemplate.opsForValue().set(key, list);
    return Result.success(list);
}

// 清除缓存
private void cleanCache(String pattern) {
    Set keys = redisTemplate.keys(pattern);
    redisTemplate.delete(keys);
}
```

#### 快速回顾

1. 菜品用 RedisTemplate 手动管理缓存
2. 套餐用 Spring Cache 注解管理（@Cacheable、@CacheEvict）
3. 修改数据时必须清除缓存，否则数据不一致
4. 缓存清除在 Controller 层，不在 Service 层
5. 使用 `keys("dish_*")` 模式匹配清除所有菜品缓存

---

## 常见问题

### 1. Bean 名称冲突
**现象：** 启动报 `BeanNameConflict`
**原因：** 管理端和用户端有同名 Controller
**解决：** 使用 `@RestController("beanName")` 指定不同名称

### 2. 拦截器放行路径
**现象：** 某些接口被拦截，返回 401
**解决：** 在 `WebMvcConfiguration` 中配置 `excludePathPatterns`

### 3. 缓存不更新
**现象：** 修改数据后，查询结果还是旧的
**原因：** Redis 缓存未清除
**解决：** 在增删改操作后清除缓存

### 4. WebSocket 连接不上
**现象：** 管理端收不到通知
**检查：**
- nginx 是否配置了 WebSocket 代理
- 浏览器 Console 是否有连接错误
- 刷新页面重新连接

### 5. 前端传参 400 错误
**现象：** 接口报 400 Bad Request
**原因：** 前端用 `data` 发 JSON，后端用 `@RequestParam` 接收
**解决：** 后端改为 `@RequestBody`

### 6. 导出 Excel 数据重复
**现象：** Excel 中每天数据都一样
**原因：** 循环中查询结果没有赋值给变量
**解决：** `variable = service.query(...)`

---

## 技术栈总结

| 类别 | 技术 |
|------|------|
| 后端框架 | Spring Boot 2.7.3 |
| ORM | MyBatis + PageHelper |
| 缓存 | Redis + Spring Cache |
| 认证 | JWT |
| 文件存储 | MinIO |
| 实时通信 | WebSocket |
| API文档 | Knife4j (Swagger) |
| Excel | Apache POI |
| 前端管理端 | Vue.js + Element UI |
| 前端用户端 | 微信小程序 (uni-app) |
| 反向代理 | nginx |
