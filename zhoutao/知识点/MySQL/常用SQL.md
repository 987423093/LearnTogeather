1. 去重：根据某些列作为唯一值，去除多于唯一信息的数据
 - 表信息：user_id,username,password,nickname,age；username,password,nickname,age作为唯一索引
```
delete from user where
user_id not in 
(select user_id from 
(select min(user_id) as user_id from user group by username, password, nickname, age having count(*) > 1) t)
and user_id not in 
(select user_id from
(select min(user_id) as user_id from user group by username, password, nickname, age having count(*) = 1) t)
```