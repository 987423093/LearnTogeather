#### centos7安装RabbitMQ

地址：***http://122.51.211.176:15672/***  
用户名/密码：***xinyuzang/xinyuzang***
1. 安装erlang
    ```
    rpm -Uvh http://www.rabbitmq.com/releases/erlang/erlang-18.1-1.el7.centos.x86_64.rpm
    ``` 
2. 安装mq
    ```
    rpm -Uvh http://www.rabbitmq.com/releases/rabbitmq-server/v3.5.6/rabbitmq-server-3.5.6-1.noarch.rpm
    ``` 
3. 常用命令
    ```
    service rabbitmq-server start
    service rabbitmq-server restart
    service rabbitmq-server stop
    rabbitmqctl status  # 查看状态
    ```
4. 安装页面插件
    ```
    rabbitmq-plugins enable rabbitmq_management
    ```
5. 开启远程访问
    ```
    1. /usr/share/doc/rabbitmq-server-3.5.6/rabbitmq.conf.example
    2. {loopback_users, []}
    ```
6. 添加用户
    ```
    添加用户mq,密码mq123:rabbitmqctl add_user mq mq123
    添加权限:rabbitmqctl set_permissions -p / mq ".*" ".*" ".*"
    修改用户角色:rabbitmqctl set_user_tags mq administrator
    ```
7. 其他操作
    ```
    删除一个用户:rabbitmqctl delete_user Username
    修改用户密码:rabbitmqctl change_password Username Newpassword
    查看当前用户列表:rabbitmqctl list_users
    ```
