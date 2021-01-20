1. redo log——修改之后存储在changeBuffer,先用redo log记录防止数据丢失
2. undo log——存储了旧址，失败了进行回滚