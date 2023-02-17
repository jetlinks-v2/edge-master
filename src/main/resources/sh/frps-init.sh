#!/bin/bash
# 初始化frps

# 赋权
chmod 777 $1/$2

# 服务配置
cat << EOF > /etc/systemd/system/frps.service
[Unit]
# 服务名称，可自定义
Description = frp server
After = network.target syslog.target
Wants = network.target

[Service]
Type = simple
# 启动frps的命令，需修改为您的frps的安装路径
ExecStart = $1/$2 -c $1/frps.ini

[Install]
WantedBy = multi-user.target
EOF