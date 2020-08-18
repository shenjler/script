echo "hello world" > /root/hello.txt

yum install zabbix-agent

sed -i 's/Server=127.0.0.1/Server=19.52.68.169/g' /etc/zabbix/zabbix_agentd.conf
sed -i 's/ServerActive=127.0.0.1/ServerActive=19.52.68.169/g' /etc/zabbix/zabbix_agentd.conf


grep "Server" /etc/zabbix/zabbix_agentd.conf | grep -v '^#'


service zabbix-agent restart
