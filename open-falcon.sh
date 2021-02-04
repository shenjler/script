

wget https://golang.google.cn/dl/go1.15.7.linux-amd64.tar.gz

rm -rf /usr/local/go
tar -zxf go1.15.7.linux-amd64.tar.gz -C /usr/local


cat >> /etc/profile <<AAA

#golang env config
export GO111MODULE=on
export GOROOT=/usr/local/go
export GOPATH=/home/gopath
export PATH=\$PATH:\$GOROOT/bin:\$GOPATH/bin
AAA
cat /etc/profile

source /etc/profile

mkdir -p /home/gopath
go version

go env -w GOPROXY=https://goproxy.cn,direct




mkdir -p $GOPATH/src/github.com/open-falcon
cd $GOPATH/src/github.com/open-falcon
git clone https://github.com/open-falcon/falcon-plus.git


cd $GOPATH/src/github.com/open-falcon/falcon-plus/scripts/mysql/db_schema/
mysql -h 127.0.0.1 -u root -pPccw@123456 < 1_uic-db-schema.sql
mysql -h 127.0.0.1 -u root -pPccw@123456 < 2_portal-db-schema.sql
mysql -h 127.0.0.1 -u root -pPccw@123456 < 3_dashboard-db-schema.sql
mysql -h 127.0.0.1 -u root -pPccw@123456 < 4_graph-db-schema.sql
mysql -h 127.0.0.1 -u root -pPccw@123456 < 5_alarms-db-schema.sql



构建
cd $GOPATH/src/github.com/open-falcon/falcon-plus/
# make all modules
make all
# make specified module
#make agent
# pack all modules
make pack



# 创建工作目录
export FALCON_HOME=/home/work
export WORKSPACE=$FALCON_HOME/open-falcon
mkdir -p $WORKSPACE
# 解压二进制包
tar -xzvf $GOPATH/src/github.com/open-falcon/falcon-plus/open-falcon-v0.3.x.tar.gz -C $WORKSPACE


# 后端：
#1. 首先确认配置文件中数据库账号密码与实际相同，否则需要修改配置文件。
cd $WORKSPACE
grep -Ilr 3306  ./ | xargs -n1 -- sed -i 's/root:/root:Pccw@123456/g'
#2. 启动
cd $WORKSPACE
./open-falcon start


# 检查所有模块的启动状况
./open-falcon check

cd $WORKSPACE
./open-falcon start api



# 前端：
yum install -y python-virtualenv
yum install -y python-devel
yum install -y openldap-devel
yum install -y mysql-devel
yum groupinstall "Development tools"

cd $GOPATH/src/github.com/open-falcon/ && git clone https://github.com/open-falcon/dashboard.git
cd dashboard;

cat rrd/config.py

sed -i 's/PORTAL_DB_USER","falcon"/PORTAL_DB_USER","root"/g' rrd/config.py
sed -i 's/PORTAL_DB_PASS","falcon"/PORTAL_DB_PASS","Pccw@123456"/g' rrd/config.py

sed -i 's/ALARM_DB_PASS",""/ALARM_DB_PASS","Pccw@123456"/g' rrd/config.py

cat rrd/config.py
# grep -Ilr 3306  ./  | xargs -n1 -- sed -i 's/root:/root:Pccw@123456/g'


virtualenv ./env
./env/bin/pip install -r pip_requirements.txt


#以开发者模式启动
./env/bin/python wsgi.py
#open http://127.0.0.1:8081 in your browser.

#在生产环境启动
bash control start
#open http://127.0.0.1:8081 in your browser.
# bash control stop
#查看日志
bash control tail

curl http://127.0.0.1:8081









