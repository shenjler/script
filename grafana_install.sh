# wget https://dl.grafana.com/oss/release/grafana-7.4.3.linux-amd64.tar.gz
wget https://mirrors.huaweicloud.com/grafana/7.4.3/grafana-7.4.3.linux-amd64.tar.gz
tar -zxvf grafana-*.tar.gz
cd grafana-*.*


# ./bin/grafana-cli plugins install xxxxxPlugin

nohup ./bin/grafana-server start > grafana.out 2>&1 &

ps -ef | grep grafana