
# 搭建
# 配置一个 ngrok 的二级域名 ：ngrok.xxx.com （将 xxx.com 替换为你自己的域名，下同）
# 一个泛域名 : *.ngrok.xxx.com
# 都指向云服务器的 ip 地址

# 在服务器上安装 git
yum -y install git

# 在服务器上安装 golang
yum -y install golang


# clone ngrok 源码
#创建一个文件夹
mkdir ~/ngrok && cd ~/ngrok
#下载源码
git clone https://github.com/inconshreveable/ngrok.git

cd ngork

# 加了环境变量名GODEBUG值为x509ignoreCN=0然后就行了。
export GODEBUG=x509ignoreCN=0

# 生成自签名 ssl 证书
DOMAIN=ngrok.shenjl.club
openssl genrsa -out rootCA.key 2048

openssl req -x509 -new -nodes -key rootCA.key -subj "/CN=$DOMAIN" -days 5000 -out rootCA.pem
openssl genrsa -out device.key 2048

openssl req -new -key device.key -subj "/CN=$DOMAIN" -out device.csr
openssl x509 -req -in device.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out device.crt -days 5000

# 替换默认的证书
# 拷贝刚才生成的证书到指定的文件夹
/bin/cp -rf rootCA.pem assets/client/tls/ngrokroot.crt
/bin/cp -rf device.crt assets/server/tls/snakeoil.crt
/bin/cp -rf device.key assets/server/tls/snakeoil.key

# 编译服务端与客户端的 ngrok 文件

# 服务端（Linux）：
# make release-server release-client

make release-server

# 客户端：

# linux 客户端
make release-client
# mac 客户端
GOOS=darwin GOARCH=amd64 make release-client
# windows 客户端
GOOS=windows GOARCH=amd64 make release-client
GOOS=windows GOARCH=amd64 GODEBUG="x509ignoreCN=0" make release-client


# 复制到主机
scp -r ./bin/* root@8.129.232.64:/root/ngrok
ln -s ~/ngrok/ngrokd /usr/local/bin/ngrokd

#ln -s ~/ngrok/ngrok/bin/ngrokd /usr/local/bin/ngrokd

#启动服务端 ngrokd
# :8999 和 :4430为服务器的端口，因为我的服务器默认的 80和 443 端口被占用了，改用其他的端口
DOMAIN=ngrok.shenjl.club
nohup ngrokd -domain="$DOMAIN" -httpAddr=":8091" -httpsAddr=":8092" -tunnelAddr=":"  &

curl $DOMAIN:8091

# nohup ngrokd -domain="$DOMAIN" -httpAddr=":8091" -httpsAddr=":8092" -tunnelAddr=":4443" &

# ngrokd服务端 脚本启动
cat > start.sh << DDD
tunnelAddr="4443"
if [ \$pid ]; then
    echo "tunnelAddr = \$1"
    tunnelAddr=\$1
fi
pid=\`ps -ef | grep ngrokd | grep domain | awk '{print \$2}'\`
if [ \$pid ]; then
    echo "ngrok pid = \$pid, kill -9 \$pid "
    kill -9 \$pid
fi
DOMAIN=ngrok.shenjl.club
nohup ngrokd -domain="\$DOMAIN" -httpAddr=":8091" -httpsAddr=":8092" -tunnelAddr=":\$tunnelAddr"  &
DDD

chmod +x start



# 下载编译好的客户端文件到本地

# mac 客户端
scp root@xxx:~/ngrok/bin/darwin_amd64/ngork ./
# windows 客户端
scp root@xxx:~/ngrok/bin/windows_amd64/ngork.exe ./

# 配置客户端连接（以 mac 环境为例）
# 修改 ngrok.cfg 文件改为以下内容
DOMAIN=ngrok.shenjl.club
cat > ngrok.cfg <<AAA
server_addr: "$DOMAIN:4443"
trust_host_root_certs: false
tunnels:
  http:
    subdomain: www 
    remote_port: 8093
    proto:
      http: 8080 
  http2:
    subdomain: ccc 
    remote_port: 8094
    proto:
      https: 8081
  http3:
    subdomain: ddd 
    remote_port: 8095
    proto:
      http: 8081
  ssh:
    remote_port: 8022
    proto:
      tcp: 22
  tcp:
    remote_port: 9999
    proto:
      tcp: 8081
AAA

# ngrok -config=ngrok.cfg -log=log.txt  start http http2 http3 ssh
# ngrok -config=ngrok.cfg -log=log.txt  start-all

# 将 ngrok 文件设置为可执行文件
chmod +x ngrok

#在客户端启动一个项目，假设在我的客户端有一个运行的项目，端口为8000，启动客户端
#如果不加参数-subdomain=test，将会随机自动分配子域名。
ngrok -config=ngrok.cfg  -subdomain=www 8080

ngrok -config=ngrok.cfg  -proto="http" -log=log.txt 8080
ngrok -config=ngrok-google.cfg  -subdomain=google -log=google.txt  8080


cat > restart.sh << CCC
pid=\`ps -ef | grep ngrok | grep domain | awk '{print \$2}'\`
if [ \$pid ]; then
    echo "ngrok pid = \$pid, kill -9 \$pid "
    kill -9 \$pid
fi
nohup ./ngrok -config=ngrok.cfg -log=log.txt start-all &
# nohup ./ngrok -config=ngrok.cfg -subdomain=www -proto="http" -log=log.txt 80 &
# nohup ./ngrok -config=ngrok.cfg -log=log.txt start ssh http &
CCC
chmod +x  restart.sh
./restart.sh


#启动指定服务
# 启动http
#$ ./ngrok -config ngrok.cfg start tunnel-1
# 启动tcp
#$ ./ngrok -config ngrok.cfg start tunnel-2



#如果配置没有问题的话，会出现以下内容
# 访问 test.ngrok.xxx.com:8999 即可访问到运行在本地的项目



# 安装指定版本golang

wget https://storage.googleapis.com/golang/go1.12.5.linux-amd64.tar.gz

tar -C /root -xzf go1.12.5.linux-amd64.tar.gz
cd root;

cat >> ~/.bashrc << BBB
export GOPATH=/root/Go
export GOROOT=/root/go
export PATH=\$PATH:\$GOROOT/bin
BBB
 
source ~/.bashrc

go version


export PATH="/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin"




    upstream ngrok {
        server 127.0.0.1:4443; # 此处端口要跟 启动服务端ngrok 时指定的端口一致
        keepalive 64;
    }
    server {
        listen 80;
        server_name ngrok.shenjl.club;
        access_log /var/log/nginx/ngrok_access.log;
        error_log /var/log/nginx/ngrok_error.log;
        location / {
            proxy_set_header X-Real-IP $remote_addr;
            proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
            proxy_set_header Host $http_host:4443;  # 此处端口要跟 启动服务端ngrok 时指定的端口一致
            proxy_set_header X-Nginx-Proxy true;
            proxy_set_header Connection "";
            proxy_pass http://ngrok;
        }
    }