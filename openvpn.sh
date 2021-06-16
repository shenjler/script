

# openvpn 安装
# https://i4t.com/4481.html
# /etc/openvpn/server.conf


# 1、下载并解压easy-rsa软件包
mkdir /data/tools -p 
wget -P /data/tools http://down.i4t.com/easy-rsa.zip
unzip -d /usr/local /data/tools/easy-rsa.zip

# 2、在开始制作CA证书之前，我们还需要编辑vars文件，修改如下相关选项 
cd /usr/local/easy-rsa-old-master/easy-rsa/2.0/
 
# 可以不修改
vim vars
export KEY_COUNTRY="cn"
export KEY_PROVINCE="BJ"
export KEY_CITY="BJ"
export KEY_ORG="abcdocker"
export KEY_EMAIL="cyh@i4t.com"
export KEY_CN=abc
export KEY_NAME=abc
export KEY_OU=abc


# 3、然后使用使环境变量生效
#初始化环境边看
source vars
./clean-all
#注意：执行clean-all命令会在当前目录下创建一个名词为keys的目录


# 4、接下来开始正式制作CA证书，命令如下
./build-ca
 
# 生成根证书ca.crt和根密钥ca.key
#因为在vars中填写了证书的基本信息，所以这里一路回车即可


# 5、这时我们可以查看keys目录，已经帮我们生成ca.crt和ca.key两个文件，其中ca.crt就是我们的证书文件
ls keys
# ca.crt  ca.key  index.txt  serial


# 6、制作Server端证书
# 为服务端生成证书和密钥
#一直回车，2个Y
./build-key-server server
#这里的server就是我们server端的证书 


# 7、查看新生成的证书
ls keys

# 这里我们已经生成了server.crt、server.key、server.csr三个文件，其中server.crt和server.key两个文件是我们需要使用的



# 8、制作Client端证书
# 这里我们创建2个用户，分别为client1和client2

#每一个登陆的VPN客户端需要有一个证书，每个证书在同一时刻只能供一个客户端连接，下面建立2份
#为客户端生成证书和密钥（一路按回车，直到提示需要输入y/n时，输入y再按回车，一共两次）
./build-key client1
./build-key client2
# 每一个登陆的VPN客户端需要有一个证书，每个证书在同一时刻只可以一个客户端连接(可以修改配置文件)

# 现在为服务器生成加密交换时的Diffie-Hellman文件

./build-dh
# 创建迪菲·赫尔曼密钥，会生成dh2048.pem文件（生成过程比较慢，在此期间不要去中断它）


# 9、证书生成完毕
ll keys


# 10、yum 安装openVPN
yum install -y openvpn


# 11、配置OpenVPN服务端
# 我们需要创建openVPN文件目录和证书目录
# openVPN配置文件目录，yum安装默认存在
mkdir /etc/openvpn
 
#openvpn证书目录
mkdir /etc/openvpn/keys


# 12、生成tls-auth key并将其拷贝到证书目录中（防DDos攻击、UDP淹没等恶意攻击）
# 编译安装执行此句
# /usr/local/openvpn/sbin/openvpn --genkey --secret ta.key
 
# yum安装执行此句
openvpn --genkey --secret ta.key
 
#将本地的ta.key移动到openVPN证书目录
mv ./ta.key /etc/openvpn/keys/


# 13、将我们上面生成的CA证书和服务端证书拷贝到证书目录中
cp /usr/local/easy-rsa-old-master/easy-rsa/2.0/keys/{server.crt,server.key,ca.crt,dh2048.pem} /etc/openvpn/keys/

ll /etc/openvpn/keys/


# 14、拷贝OpenVPN配置文件
# 编译安装
# cp /data/tools/openvpn-2.4.7/sample/sample-config-files/server.conf /etc/openvpn/
 
# yum安装
cp /usr/share/doc/openvpn-2.4.11/sample/sample-config-files/server.conf /etc/openvpn/


# 15、接下来我们来配置服务端的配置文件
cat /etc/openvpn/server.conf 
port 1194       #openVPN端口
proto tcp       #tcp连接
dev tun         #生成tun0虚拟网卡
 
ca keys/ca.crt      #相关证书配置路径(可以修改为全路径/etc/openvpn/keys)
cert keys/server.crt
key keys/server.key  # This file should be kept secret
dh keys/dh2048.pem
 
server 192.168.2.0 255.255.255.0   #默认虚拟局域网网段，不要和实际的局域网冲突就可以
ifconfig-pool-persist ipp.txt     
 
push "route 192.168.2.0 255.255.255.0"    #可以通过iptables进行路由的转发
client-to-client                 #如果客户端都是用一个证书和密钥连接VPN，需要打开这个选项
duplicate-cn
keepalive 10 120
tls-auth keys/ta.key 0 # This file is secret
comp-lzo
 
persist-key
persist-tun
 
status openvpn-status.log   #状态日志路径
log-append  openvpn.log     #运行日志
verb 3                      #调试信息级别
# explicit-exit-notify 1    # 需禁用


# 16、开启内核路由转发功能
echo "net.ipv4.ip_forward = 1" >>/etc/sysctl.conf
sysctl -p

# 17、如果有iptables可以开启iptables策略
iptables -P FORWARD ACCEPT
iptables -I INPUT -p tcp --dport 1194 -m comment --comment "openvpn" -j ACCEPT
#iptables -t nat -A POSTROUTING -s 10.170.0.0/24 -j MASQUERADE
iptables -t nat -A POSTROUTING -s 192.168.2.0/24 -j MASQUERADE


# 18、启动openvpn服务
cd /etc/openvpn/
openvpn --daemon --config /etc/openvpn/server.conf

# 19、检查服务
netstat -lntup|grep 1194
# tcp        0      0 0.0.0.0:1194            0.0.0.0:*               LISTEN      48091/openvpn  


# 20、设置开机启动
echo "openvpn --daemon --config /etc/openvpn/server.conf > /dev




client
dev tun
proto tcp
remote 34.92.52.42 1194    #openvpn服务器的外网IP和端口(可以写多个做到高可用)
resolv-retry infinite
nobind
persist-key
persist-tun
ca ca.crt
cert client1.crt         #用户的证书
key client1.key
 
tls-auth ta.key 1
cipher AES-256-CBC
comp-lzo
verb 3



注意：
需配置路由转发 到互联网