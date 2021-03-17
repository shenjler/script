file=alertmanager-0.21.0.linux-amd64.tar.gz
wget https://github.com/prometheus/alertmanager/releases/download/v0.21.0/$file

tar -xzvf alertmanager-*.*.tar.gz
cd alertmanager-*.*

cat alertmanager.yml

nohup ./alertmanager > alertmanager.out 2>&1 &
ps -ef | grep alertmanager 

# ./alertmanager --config.file=alertmanager.yml
