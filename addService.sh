#/bin/sh
if [ $# -ne 2]; then
    echo "参数个数有误,请输入2个参数: module, command "
fi
module=$1
command=$2

if [ ! -f "/run/${module}.pid"]; then 
    touch  "/run/${module}.pid"
fi

cat > /usr/lib/systemd/system/${module}.service << AAA
[Unit]
Description=${module}

[Service]
ExecStart=${command}
ExecStop=/usr/bin/kill -9 $MAINPID
Restart=always
RestartSec=10s
PIDFile=/run/${module}.pid

[Install]
WantedBy=multi-user.target

AAA


# curl http://file.shenjl.club/addService.sh | sh -s -- prometheus "/root/prometheus-2.24.1.linux-amd64/prometheus --config.file=/root/prometheus-2.24.1.linux-amd64/prometheus.yml --web.enable-lifecycle"
# curl http://file.shenjl.club/addService.sh | sh -s -- grafana "/root/grafana-5.4.0/start.sh"
#  /root/grafana-5.4.0/bin/grafana-server --homepath=/root/grafana-5.4.0 --config=/root/grafana-5.4.0/conf/defaults.ini start