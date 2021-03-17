file=alertmanager-0.21.0.linux-amd64.tar.gz
wget https://github.com/prometheus/alertmanager/releases/download/v0.21.0/$file

tar -xzvf alertmanager-*.*.tar.gz
cd alertmanager-*.*

cat alertmanager.yml

nohup ./alertmanager > alertmanager.out 2>&1 &
ps -ef | grep alertmanager 

# ./alertmanager --config.file=alertmanager.yml


curl http://pccw104:9093/

#prometheus关联alertmanager
#prometheus.yml中的alerting标签下配置上alertmanager的地址即可，配置如下：

# Alertmanager configuration
alerting:
  alertmanagers:
  - static_configs:
    - targets: ['192.168.199.23:9093']
