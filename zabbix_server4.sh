
### a. Install Zabbix repository
rpm -Uvh https://repo.zabbix.com/zabbix/4.0/rhel/7/x86_64/zabbix-release-4.0-2.el7.noarch.rpm
yum clean all

### b. 安装Zabbix server，Web前端，agent
yum install zabbix-server-mysql zabbix-web-mysql zabbix-agent

### c. 创建初始数据库
### Make sure you have database server up and running.

## 在数据库主机上运行以下代码。

# mysql -uroot -p
## password
## mysql> create database zabbix character set utf8 collate utf8_bin;
## mysql> create user zabbix@localhost identified by 'password';
## mysql> grant all privileges on zabbix.* to zabbix@localhost;
## mysql> quit;
## 导入初始架构和数据，系统将提示您输入新创建的密码。

# zcat /usr/share/doc/zabbix-server-mysql*/create.sql.gz | mysql -uzabbix -p zabbix


mysql -h 172.16.253.104 -u root --password=Pccw@123456 -e "create database zabbix character set utf8 collate utf8_bin;"
zcat /usr/share/doc/zabbix-server-mysql*/create.sql.gz | mysql -h 172.16.253.104 -uroot -p Pccw@123456



### d. 为Zabbix server配置数据库
### 编辑配置文件 /etc/zabbix/zabbix_server.conf
### DBPassword=password

cat /etc/zabbix/zabbix_server.conf 

sed -i "s/DBHost=localhost/DBHost=172.16.253.104/" /etc/zabbix/zabbix_server.conf
sed -i "s/DBUser=zabbix/DBUser=root/" /etc/zabbix/zabbix_server.conf
sed -i "s/DBPassword=password/DBPassword=Pccw@123456/" /etc/zabbix/zabbix_server.conf



### e. 为Zabbix前端配置PHP
### 编辑配置文件 /etc/httpd/conf.d/zabbix.conf, uncomment and set the right timezone for you.
# php_value date.timezone Europe/Riga

sed -i "s/php_value date.timezone Europe\/Riga/php_value date.timezone Asia\/Shanghai/" /etc/httpd/conf.d/zabbix.conf


### f. 启动Zabbix server和agent进程
### 启动Zabbix server和agent进程，并为它们设置开机自启：

systemctl restart zabbix-server zabbix-agent httpd
systemctl enable zabbix-server zabbix-agent httpd

### g. 配置Zabbix前端
### 连接到新安装的Zabbix前端： http://server_ip_or_name/zabbix
## 根据Zabbix文件里步骤操作： Installing frontend

curl -i http://172.16.253.105/zabbix


### 开始使用Zabbix
# https://www.zabbix.com/documentation/4.0/manual/installation/install#installing_frontend
