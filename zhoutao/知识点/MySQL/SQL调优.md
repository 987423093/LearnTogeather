### explain

有十列
分别是
 - id：选择标识符
 - select_type：查询的类型
 - table：输出结果集的表
 - partitions：匹配的分区
 - type：表连接的类型
 - possible_keys：查询时可能使用的索引
 - key：实际使用的索引
 - key_len：索引字段的长度
 - ref：列与索引的比较
 - rows：扫描出的行数（估算的行数）
 - filtered：按表条件过滤的百分比
 - Extra：执行情况的描述和说明
 
*** 

#### id
查询序列号，表示优先级

#### select type
1. simple/update/delete：简单的sql，不使用uinon/子查询
2. primary: 子查询中最外层查询
3. union: union中的第二个或者后面的select语句
4. dependent union：和union一样，但是该查询收到外部影响
5. union result : union的结果集
6. subquery: 子查询中的第一个select，除了from字句中包含的子查询外，其他都有可能是subquery
7. dependent subquery: 子查询中的第一个select，依赖于外层查询的结果 
8. derived: 在from字句中包含的子查询

#### table
显示查询表名称，如果用的别名，也显示别名，<>尖括号表示的是临时表

#### type
从好到差
system > const > eq_ref > ref > fulltext > ref_or_null > unique_subquery > index_subquery > range > index_merge > index > all
1. system: 单表查询，表中只有一行数据或者是空表，特殊的const
2. const: 单表查询+where 使用唯一索引/主键
3. eq_ref: 多表join，在连接时使用主键/唯一索引的全部字段
4. ref：索引+使用最左匹配原则 + 非范围查找 + 非全字段都可以从索引中获取
5. range：使用索引 + 范围查找（where 后面带上in,>,<,>=,<=,in,between）
6. index：全索引扫描，索要查询的数据在索引中就可以获取
7. all：全表扫描，剩下的没有命中的都是全表扫描

**最左匹配原则：最左优先，以最左边的为起点任何连续的索引都能匹配上。同时遇到范围查询(>、<、between、like)就会停止匹配。**

#### possible_keys
只是说明可能会用到，实际上是否用到取决于key字段

#### key
查询时真正会用到的索引

#### key len
计算方式：先根据最左匹配原则测算出哪几个字段被使用了，然后根据下列数据的长度估算
- 字符串
    - char(n):n个字节
    - varchar(n):1.utf-8(3n+2);2.utf8mb4(4n+2)
- 数值类型
    - tinyint:1个字节
    - smallint:2个字节
    - mediumint:3个字节
    - int:4个字节
    - bigint:8个字节
- 时间类型
    - date:3个字节
    - timestamp:4个字节
    - datetime:8个字节
- 字段属性
    - null:一个字节
    - not null:0个字节
    
#### rows
估算找到结果集要扫描的行数

#### filtered
查找出来的数据占所有行数据的多少

#### extra
 - Using firesort  
 当有using firesort时，表示mysql需要做额外的操作，不能通过索引顺序打到排序，建议优化
 - Using index  
 表示查询在索引数上就能查询到数据，性能不错
 - Using temporary  
 表示有临时表，一般出现在排序，分组和多表join情况，建议优化
 
 ***
### SQL优化
 1. 不要将可以为null的列作为索引 ？
 2. 不要对索引列在where条件中使用
    1. concat或者|| 字符串拼接
    2. !=运算，使用> or < 代替(使用union替换or更佳)
    3. not in,使用exists
    4. 函数 where score * 10 > 21,应该使用where score > 21 / 10
 3. 使用like通配符不要在最前面%
 4. order by 中不要用非索引列，用explain会提示using firesort
 5. 不要对索引列在where条件中使用!=运算，拆分成> or <
 6. select .. from .. where .. group by .. having .. order by .. limit .. 能在前面拦截就在前面拦截
 7. 能直接定位到>= 就不要用> 