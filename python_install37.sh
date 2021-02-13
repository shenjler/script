
wget https://cdn.npm.taobao.org/dist/python/3.7.3/Python-3.7.3.tar.xz

tar -xvf Python-3.7.3.tar.xz


# 3.编译安装
mkdir /usr/local/python3 #创建编译安装目录
cd Python-3.7.0
./configure --prefix=/usr/local/python3
make && make install
# 4.创建软连接
ln -s /usr/local/python3/bin/python3 /usr/local/bin/python3
ln -s /usr/local/python3/bin/pip3 /usr/local/bin/pip3
# 5.验证是否成功
python3 -V
pip3 -V


#5、调整yum配置和软件安装配置文件
#修改yum文件，因为升级了版本以后，yum就会报错

vi /usr/bin/yum

#!/usr/bin/python 修改为 #!/usr/bin/python2.7

#在安装软件的时候又会报错误

vi /usr/libexec/urlgrabber-ext-down

#!/usr/bin/python 修改为#!/usr/bin/python2.7
