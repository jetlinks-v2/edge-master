#!/bin/bash
# 配置frps

# 创建服务配置
cat << EOF > $1/frps.ini
[common]
bind_port = $2
token = $3

# frp日志配置
log_file = ./frps.log
log_level = info
log_max_days = 3
EOF

$1/$4 -c $1/frps.ini

