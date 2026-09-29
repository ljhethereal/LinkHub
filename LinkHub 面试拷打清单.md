# LinkHub 面试拷打清单

> 按模块整理, 每题后附答题要点. 打勾表示已能独立口述.

---

## 一、实体层 (JPA / 数据库设计)

- [ ] BaseEntity 用 @MappedSuperclass 的作用? 为什么不在每个实体重复写 id/createdAt/updatedAt?
  - 要点: 映射父类, 字段并入子类表, 自身不建表; 消除 id/时间戳重复
- [ ] @EnableJpaAuditing + @CreatedDate/@LastModifiedDate 的自动填充原理? 和 @PrePersist/@PreUpdate 的区别与取舍?
  - 要点: Auditing 自动填(暗坑: @CreatedBy 需 AuditorAware bean); 回调式每实体手动写一遍; Auditing 更省代码/一致/跨实体复用
- [ ] 一对一 User-UserProfile: 哪端维护外键 (@JoinColumn)? 为什么?
  - 要点: 写 @JoinColumn 的那端存外键; mappedBy 端只读不写
- [ ] 多对一 Post -> User/Category: LAZY 和 EAGER 区别? @ManyToOne 默认是什么?
  - 要点: LAZY 用时代理查 / EAGER 立即联查; 陷阱: @ManyToOne 规范默认是 EAGER, 项目里显式改成了 LAZY
- [ ] 多对多 Post <-> Tag: @JoinTable 中间表叫什么? 哪端是 owner? 非 owner 端为什么要 mappedBy?
  - 要点: t_tag_post; owner 端(有 @JoinTable)独揽中间表增删; 两端都有 @JoinTable = 两个独立 owner 各管各的, Hibernate 报错/数据错乱; 改双向时 @JoinTable 留在 owner, mappedBy 加到对端
- [ ] 自关联树 Category: parent (ManyToOne) + children (OneToMany mappedBy="parent"), 为什么两端注解不对称?
  - 要点: 不对称根源是"谁持有外键"——@ManyToOne 端用 @JoinColumn, @OneToMany 端用 mappedBy; 外键 parent_id 就在 t_category 自己表上; addChild 必须双向都设, 只加 children 集合不动 parent 字段 = 内存有父子、库里没落外键
- [ ] unique=true 的意义 (Category.name)? 不加会怎样? 重复数据怎么来的?
  - 要点: 建唯一索引; 不加则重复名可入库(旧的非幂等 DataInitializer 重复插 alice 就出过 NonUniqueResultException); 建表时由 ddl 自动加约束
- [ ] orphanRemoval=true vs cascade=ALL: 哪个是"父删子删", 哪个是"子失联删"? 项目里各自用在哪?
  - 要点: cascade=ALL 父删连带删子(Post 删→评论删); orphanRemoval 子从集合被摘掉即删(post.getComments().remove()); 两者叠加在 Post.comments 上, 分工不同
- [ ] ddl-auto 四种模式 (none/validate/update/create-drop), 生产环境用哪个? 为什么?
  - 要点: 生产用 validate 或 none + Flyway/Liquibase 迁移脚本; update 会偷偷改表结构, 不可控不能上生产
- [ ] @Builder 和 @SuperBuilder 的区别? @SuperBuilder 解决什么问题?
  - 要点: 普通 @Builder 不包含父类字段; @SuperBuilder 能 builder 到继承自 BaseEntity 的 id/createdAt

## 二、仓储层 (Spring Data JPA)

- [ ] JpaRepository 自带哪些方法? 为什么接口不需要写实现类? (动态代理 + 方法名解析)
  - 要点: JDK 动态代理在运行期生成实现; 无参方法(如 findById/save)走内置; 带名方法(findByXxx/existsByXxx)由方法名解析器拼查询
- [ ] existsByCategoryId 返回类型为什么必须是 Boolean 不能是 Optional<Post>? 方法名查询解析规则?
  - 要点: existsBy 底层生成 count/exists 标量, 返回契约只允许 boolean/Boolean/long; 类型不符是启动期抛 InvalidJpaQueryMethodException (不是运行时) —— 派生查询的返回类型不匹配=应用直接起不来
- [ ] @Query + JPQL 为什么用实体名 (大写 Post) 而不是表名 (小写 post)? JPQL 和原生 SQL 的根本区别?
  - 要点: JPQL 操作面向对象(实体/属性), 不是表/列; 大写 Post 是实体类名; 原生 SQL(需 nativeQuery=true)才写表名 post; 混用会报 unexpected token
- [ ] 动态条件 (:keyword IS NULL OR p.title LIKE ...) 的原理? 为什么一个查询能表达"有值就过滤、无值就跳过"?
  - 要点: 传 null 时 :keyword IS NULL 为 true, OR 短路整段跳过; 传值时 IS NULL 为 false, 走 LIKE 过滤; 一条语句覆盖 2^n 种条件组合
- [ ] 这种 IS NULL 写法的性能隐患? 为什么可能导致全表扫描? 怎么优化? (查询计划器 / Criteria API 动态拼接)
  - 要点: 根因①OR+NULL 谓词非 sargable, 优化器走 Seq Scan; ②一条语句承载所有组合, 无法按组合选最优计划; ③PG prepared statement 前几次 custom plan 后切 generic plan, 对"传没传参数"反应迟钝; ④LIKE '%xx%' 前置通配符本就无法用 B-tree 索引(trigram/GIN 才行); 优化=Criteria/Specification 动态拼 WHERE, 只给真实传参生成谓词
- [ ] @Modifying 必须搭配什么才生效? 不配抛什么异常? (Transactional / TransactionRequiredException)
  - 要点: @Modifying 只是"bulk 写"标记, 不自己开事务; 必须配 @Transactional(JPA 硬规定), 否则抛 jakarta.persistence.TransactionRequiredException; JPQL UPDATE/DELETE 必须有活动事务
- [ ] @Modifying(clearAutomatically=true) 的作用? 什么时候需要?
  - 要点: bulk UPDATE/DELETE 绕过一级缓存(持久化上下文), 之后同事务再 findById 可能读到脏旧值; clearAutomatically 执行后清空 session, 下次查走 DB; 紧跟着要读同一实体时才加
- [ ] 分页原理: PageRequest.of(page, size, Sort) 底层做了什么? 为什么返回 Page 而不是 List?
  - 要点: Spring Data 实际发两条 SQL——①带 WHERE 的 count(*) 求总数 ②LIMIT :size OFFSET :page*size 取当页; Page 同时包 content(当页) + totalElements/totalPages/hasNext, List 只能给当页拿不到总数
- [ ] JPQL 里的 ORDER BY 和 Pageable 里的 Sort 有什么区别? 会不会冲突?
  - 要点: Pageable 的 Sort 会被追加到 JPQL 的 ORDER BY 之后(JPQL 已写的在前, Sort 追加在后作 tie-break); 重复字段=先 JPQL 再 Sort 的多级排序, 不冲突

## 三、Service 层 (事务 / 业务逻辑)

- [ ] @Transactional 的底层原理? (Spring AOP 动态代理, 环绕通知)
- [ ] @Transactional 失效的四种场景: 同类自调用 / 非 public / 异常被吞 / 传播行为配置错误, 各自为什么失效?
- [ ] @Transactional(readOnly=true) 的作用? (不 flush / DB 只读优化)
- [ ] 为什么 toResponse 访问 post.getUser().getUserName() 必须待在事务方法内? 去掉抛什么异常? (LazyInitializationException)
- [ ] 为什么推荐"事务内转 DTO"而不是"返回实体让 Jackson 序列化"? (递归引用 + 敏感字段 + N+1)
- [ ] 智能标签 resolveTags: orElseGet 里嵌套 save 在同一事务里安全吗? 为什么不用 INSERT ON CONFLICT?
- [ ] 分类安全删除的两道防线: 为什么"先查帖子再查子分类"顺序不能反?
- [ ] 浏览量初始值同步的并发竞态 (100 线程 hasKey=false -> set -> INCR): 丢计数 + 击穿, 怎么解决?

## 四、Controller 层 / REST 设计

- [ ] @RestController = @Controller + @ResponseBody, 为什么不需要 @ResponseBody 也能返回 JSON?
- [ ] @PathVariable Long postId 和 @GetMapping("/{postId}") 路径变量名不匹配会怎样?
- [ ] RESTful 语义: 为什么"创建"用 POST 不用 PUT? DELETE 为什么返回 204 不返回 200?
- [ ] @Valid @RequestBody 校验失败抛什么异常? 在哪捕获? (MethodArgumentNotValidException)
- [ ] @RequestParam(defaultValue="0") int page: 为什么用 primitive int 不用 Integer?

## 五、DTO 设计

- [ ] 为什么需要 DTO? 三个理由: 递归序列化 / 敏感字段 / 懒加载异常
- [ ] Request 和 Response 字段取舍的逻辑? (Request 只要用户传的, Response 只给前端看的)
- [ ] @NotBlank vs @NotNull vs @NotEmpty 的区别? String / 集合 / Integer 各用哪个?
- [ ] 校验注解写在 DTO 上 vs Service 手动 if, 为什么推荐前者?

## 六、异常处理

- [ ] @RestControllerAdvice 的作用原理? (AOP 全局异常拦截)
- [ ] BusinessException extends RuntimeException: 为什么是 Runtime 不是 Checked? (事务默认只回滚 Runtime)
- [ ] 多个 @ExceptionHandler 同时匹配时的优先级规则? (子类 > 父类 > Exception)
- [ ] catch-all 返回 500 而不是 400 的语义区别? 生产环境该返回什么?
- [ ] 为什么用 log.error 而不是 printStackTrace?
- [ ] DataIntegrityViolationException 不单独处理会走哪条路? (catch-all -> 500) 要不要单独 catch -> 400?

## 七、Redis 缓存设计

### 7.1 浏览量 INCR 计数器

- [ ] 为什么用 Redis INCR 而不是 UPDATE SET views=views+1? (内存 vs 磁盘 10万倍 / 原子指令 / 避免行锁)
- [ ] 为什么浏览量不进详情缓存? (每次访问都在变, 进缓存就是脏数据)
- [ ] @Scheduled 的 fixedDelay 和 fixedRate 区别? @EnableScheduling 不写会怎样?
- [ ] 覆盖式落库 (SET views=:views) vs 增量式落库 (views=views+delta): 各自竞态窗口在哪?
- [ ] 增量式下 Redis 存总量还是增量? GETDEL (Redis 6.2+) 或 Lua 脚本解决什么问题?
- [ ] GETDEL 取走增量后 DB 写失败怎么办? (INCRBY 把 delta 写回 Redis, 下周期自动重试, 天然幂等)
- [ ] TTL 7 天过期后 + 100 并发点开: set 覆盖已 INCR 的值 -> 极端只计 1 次; 100 次 findById -> 击穿. 怎么防?

### 7.2 帖子详情缓存 (Cache-Aside)

- [ ] 缓存旁路的读写流程? (读: 先查缓存, 未命中查 DB 写回; 写: 更新 DB 后删缓存)
- [ ] 为什么"删缓存"而不是"更新缓存"? (并发写精确同步难 / 自愈 / 避免无效计算)
- [ ] 先更新 DB 再删 vs 先删再更新: 各自脏数据场景? (读线程在中间回写旧值)
- [ ] 延迟双删: 删缓存 -> sleep(100ms) -> 再删. 解决什么? 局限是什么? (sleep 经验值 / 不解决写写竞态 / 生产用 MQ+版本号)
- [ ] readValue 失败 catch 后置 response=null 走 DB 的意义? (缓存损坏 -> 降级自愈, 否则 NPE)
- [ ] 浏览量用 Redis INCR 实时覆盖 setViews(views) 的设计意图? (缓存里 views 是旧值, 返回前动态填, 两头都实时)

### 7.3 分类树缓存

- [ ] 整树缓存 key 只有一个 category:tree, 好处 (读 O(1)) 和坏处 (任何变动整体失效)?
- [ ] 为什么树用 1 小时 TTL、帖子详情用 10 分钟? (读多写少, 变化频率低)
- [ ] 写操作 delete(TREE_KEY) 放在事务内还是提交后? (事务回滚 + 缓存已删 = 下次重建, 无害; 事务成功 + 删失败 = 脏数据)
- [ ] TypeReference<List<CategoryResponse>> 的用途? (Jackson 泛型擦除)

### 7.4 Redis 通用概念

- [ ] Redis 为什么快? (单线程无锁竞争 + 内存 + IO 多路复用 epoll)
- [ ] 缓存三大问题: 穿透 (查不存在) / 击穿 (热点 key 过期) / 雪崩 (大量 key 同时过期) -- 定义 + 本项目应对?
  - 穿透: 空对象缓存 / 布隆过滤器
  - 击穿: 互斥锁 SET NX / 永不过期+逻辑过期
  - 雪崩: TTL 加随机扰动 / 多级缓存
- [ ] StringRedisTemplate vs RedisTemplate 区别? (序列化器: String vs JDK)
- [ ] 为什么本项目只用 StringRedisTemplate, 不需要自定义 RedisConfig? (只存字符串/JSON, 序列化由业务层 ObjectMapper 控制)
- [ ] redisTemplate.keys("post:views:*") 的生产风险? (O(N) 阻塞主线程, 用 SCAN 替代)
- [ ] Redis 持久化 RDB vs AOF? 你的项目丢了 Redis 数据怎么办? (从 DB 重建 = 缓存思维)
- [ ] hasKey + set + INCR 三步非原子的竞态怎么消除? (Lua 脚本 / SETNX+INCR)

## 八、Jackson / 序列化

- [ ] Jackson 3 和 2 的包名变化? (com.fasterxml.jackson -> tools.jackson)
- [ ] Jackson 3 里 readValue/writeValueAsString 抛的检查异常叫什么? (JacksonException, 不再是 JsonProcessingException)
- [ ] LocalDateTime 在 Jackson 3 里需要单独引 jsr310 模块吗? (不需要, 已内置 databind)

## 九、Spring / 通用

- [ ] 为什么推荐构造器注入而不是 @Autowired 字段注入? (final 不可变 / 空指针早暴露 / 测试友好)
- [ ] Spring Boot 自动配置原理? (@EnableAutoConfiguration + spring.factories/AutoConfiguration.imports + 条件注解)
- [ ] @Service/@Component/@Repository/@Controller 的 stereotype 注解有什么实际作用? (被组件扫描 + 语义分层)
- [ ] 启动失败的 NonUniqueResultException 是怎么回事? (findBy 返回单值却查出多条) 怎么防? (unique 约束 + 清理脏数据)
- [ ] @Scheduled 跑在哪个线程? 默认单线程还是线程池? 长任务会不会互相阻塞?

## 十、项目综合叙事题

- [ ] 介绍你项目的架构分层 (Controller -> Service -> Repository -> Entity), 每层的职责边界?
- [ ] 为什么浏览量用 Redis、详情用缓存、分类用树缓存 -- 三种不同的缓存策略对应什么数据特征?
- [ ] 你的项目如果并发量从 100 涨到 10000, 最先崩的瓶颈在哪? 怎么改?
- [ ] 如果让你把"当前用户 alice"写死换成正经登录, 你会怎么做? (Spring Security / JWT / HandlerInterceptor 取 token)
- [ ] 你项目里有哪些地方是"为了教学简化"的? 生产会怎么补? (日志 / 安全删除 / 缓存击穿防护 / 分页参数校验 / keys 命令换 SCAN)
