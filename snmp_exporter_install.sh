

# https://www.cnblogs.com/charlieroro/p/11118671.html
# https://cloud.tencent.com/developer/article/1602923

# scrape_configs 配置说明
# https://blog.csdn.net/wtan825/article/details/93651164

# snmp 监控
wget https://github.com/prometheus/snmp_exporter/releases/download/v0.20.0/snmp_exporter-0.20.0.linux-amd64.tar.gz
tar -zxvf snmp_exporter-*.tar.gz
cd snmp_exporter-*.*


nohup ./snmp_exporter > snmp_exporter.out 2>&1 &
ps -ef | grep snmp_exporter

curl http://pccw104:9116/
curl http://pccw104:9116/snmp?target=172.16.253.95
curl http://pccw104:9116/snmp?target=172.16.253.95&module=apcups
curl http://pccw104:9116/snmp?target=172.16.253.95&module=if_mib


# 安装 net-snmp-utils 
yum install net-snmp-utils -y 

# 本机测试snmp数据

snmpwalk -v 2c -c public 172.16.253.95 system
snmpwalk -v3 -u username -l auth -a MD5 -A password 172.16.253.95

# prometheus 配置
```
scrape_configs:
  - job_name: 'snmp'
    static_configs:
      - targets:
        - 172.16.253.95  ## SNMP 被监控设备
    metrics_path: /snmp
    params:
      module: [if_mib]
    relabel_configs:
      - source_labels: [__address__]
        target_label: __param_target
      - source_labels: [__param_target]
        target_label: instance
      - target_label: __address__
        replacement: pccw104:9116  # SNMP exporter's 真正地址，格式为 hostname:port。

```
# 统计指标
# 指标列表  http://pccw104:9116/config
```
grafana配置
snmp给出了snmp服务所在的节点(一般为交换机)的接口信息，如接口状态，名称，In/Out报文数目，丢弃报文数和错误报文数等。下面给出简单的配置：

计算接收的报文总数
sum(ifInBroadcastPkts+ifInMulticastPkts+ifInUcastPkts)by(ifDescr)

计算2分钟内接收到的报文总数的平均数
sum(rate(ifInBroadcastPkts[2m])+rate(ifInMulticastPkts[2m])+rate(ifInUcastPkts[2m]))by(ifDescr)

计算发送的报文总数
sum(ifOutBroadcastPkts+ifOutMulticastPkts+ifOutUcastPkts)by(ifDescr)

计算2分钟内发送的报文总数的平均数
sum(rate(ifOutBroadcastPkts[2m])+rate(ifOutMulticastPkts[2m])+rate(ifOutUcastPkts[2m]))by(ifDescr)

计算未上送的报文总数
sum(ifInDiscards+ifInErrors+ifInUnknownProtos)by(ifDescr)

计算2分钟内未上送的报文总数的平均数
sum(rate(ifInDiscards[2m])+rate(ifInErrors[2m])+rate(ifInUnknownProtos[2m]))by(ifDescr)

计算丢弃的报文总数
sum(ifOutDiscards+ifOutErrors)by(ifDescr)

计算2分钟内丢弃的报文总数的平均数
sum(rate(ifOutDiscards[2m])+rate(ifOutErrors[2m]))by(ifDescr)
```




# udp 检测
netcat -u 172.16.253.95 161
nc -u 172.16.253.95 161


#   blackbox_exporter检测
wget https://github.com/prometheus/blackbox_exporter/releases/download/v0.18.0/blackbox_exporter-0.18.0.linux-amd64.tar.gz
tar -zxvf blackbox_exporter-*.tar.gz
cd blackbox_exporter-*.*

nohup ./blackbox_exporter > blackbox_exporter.out 2>&1 &
ps -ef | grep blackbox_exporter

# module默认是http_2xx
curl http://pccw104:9115

curl http://pccw104:9115/probe?target=172.16.253.101:3000

curl http://pccw104:9115/probe?target=172.16.253.101&module=icmp

curl http://pccw104:9115/probe?target=172.16.253.101&module=tcp_connect



# blackbox_exporter的modules。 blackbox.yml
# 配置  http://pccw104:9115/config
```
modules:
  http_2xx:
    prober: http
  http_post_2xx:
    prober: http
    http:
      method: POST
  tcp_connect:
    prober: tcp
  pop3s_banner:
    prober: tcp
    tcp:
      query_response:
      - expect: "^+OK"
      tls: true
      tls_config:
        insecure_skip_verify: false
  ssh_banner:
    prober: tcp
    tcp:
      query_response:
      - expect: "^SSH-2.0-"
  irc_banner:
    prober: tcp
    tcp:
      query_response:
      - send: "NICK prober"
      - send: "USER prober prober prober :prober"
      - expect: "PING :([^ ]+)"
        send: "PONG ${1}"
      - expect: "^:[^ ]+ 001"
  icmp:
    prober: icmp
```