1. redo log——修改之后存储在changeBuffer,先用redo log记录防止数据丢失
2. undo log——存储了旧址，失败了进行回滚

3. redo log 和 undo log
 - undo log 是记录了历史值，方便回滚时使用
 - redo log（重做） 是在进行修改的时候记录是在changeBuffer中，后面记录写入redo log中，将redo log写入磁盘，并写入binlog, 然后再进行redo log的事务提交
 
```
 A.事务开始.
B.记录A=1到undo log.
C.修改A=3.
D.记录A=3到redo log.
E.记录B=2到undo log.
F.修改B=4.
G.记录B=4到redo log.
H.将redo log写入磁盘。
I.事务提交
```
buffer pool 缓存区——磁盘很慢，需要一个缓存，磁盘数据页与缓冲页对应  
curd是在缓冲区作处理，undo是数据的旧址，redo是缓冲区进行curd之后落盘的日志，