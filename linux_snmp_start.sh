
# linux中开启snmp协议
https://blog.csdn.net/konglongaa/article/details/78246609
https://blog.csdn.net/butterfly5211314/article/details/93325919


# 1.安装软件
yum -y install net-snmp*


# 2.修改配置文件
cp /etc/snmp/snmpd.conf /etc/snmp/snmpd.conf.bak


mycommunity=hodor
cat >> /etc/snmp/snmpd.conf << AAA
public $mycommunity
group   notConfigGroup v1           notConfigUser
group   notConfigGroup v2c           notConfigUser
access  notConfigGroup ""      any       noauth    exact all  none none
view all    included  .1                          80
syslocation Unknown
syscontact Root <root@localhost>
dontLogTCPWrappersConnects yes
AAA


# 3.重启snmpd服务:
service snmpd restart
#Stopping snmpd: [FAILED]
#Starting snmpd: [ OK ]

service snmpd status


# 4.设置snmpd每次开机时自动启动:
chkconfig snmpd on
#该命令执行完成后不会返回任何结果

# 5.检查snmpd服务是否已在运行:

netstat -nlup | grep ":161" 
#netstat -anp |grep snmpd
# udp 0 0 0.0.0.0:161 0.0.0.0:* 16986/snmpd
#该命令检查本地是否已在监听UDP端口161,如果返回类似以上结果,表明snmpd服务启动成功


# 测试验证：
snmpwalk -v 2c -c 共同体名 被监控主机ip
snmpwalk -v 2c -c hodor 172.16.253.103 system

snmpwalk -v 2c -c public 172.16.253.103 system

snmpwalk -v 2c -c public 172.16.253.105 system
snmpwalk -v 2c -c hodor 172.16.253.105 system
