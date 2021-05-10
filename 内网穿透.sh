
# https://www.xncoding.com/2017/12/29/web/ngrok.html
# 搭建ngrok服务
# 安装go语言环境
# ngrok是基于go语言开发的，所以需要先安装go语言开发环境，CentOS可以使用yum安装：

yum install golang
# 安装git
# 默认的git版本太低了，需要升级到git2.5，具体步骤如下：

sudo yum remove git
sudo yum install epel-release
sudo yum install https://centos7.iuscommunity.org/ius-release.rpm
sudo yum install git2u
git --version # ，返回 git version 2.5.0，安装成功。

# 下载ngrok源码
# 新建一个目录，并clone一份源码：

mkdir ~/go/src/github.com/inconshreveable
cd ~/go/src/github.com/inconshreveable
git clone https://github.com/inconshreveable/ngrok.git
export GOPATH=~/go/src/github.com/inconshreveable/ngrok


# 生成自签名证书

$ cd ngrok
$ openssl genrsa -out rootCA.key 2048
$ openssl req -x509 -new -nodes -key rootCA.key -subj "/CN=ngrok.xncoding.com" -days 5000 -out rootCA.pem
$ openssl genrsa -out device.key 2048
$ openssl req -new -key device.key -subj "/CN=ngrok.xncoding.com" -out device.csr
$ openssl x509 -req -in device.csr -CA rootCA.pem -CAkey rootCA.key -CAcreateserial -out device.crt -days 5000

# 替换证书
$ cp rootCA.pem assets/client/tls/ngrokroot.crt
$ cp device.crt assets/server/tls/snakeoil.crt
$ cp device.key assets/server/tls/snakeoil.key


# 编译ngrokd和ngrok

$ make release-server


$ GOOS=linux GOARCH=amd64 make release-client
$ GOOS=windows GOARCH=amd64 make release-client
$ GOOS=linux GOARCH=arm make release-client