
# Google Cloud 原本自带的SSH工具用着也挺不错，但是为了集中管理，还是用工具方便点。
# 首先使用Google Cloud SSH连接上去：
# 1.切换到 root
sudo -i

# 2.编辑ssh配置文件
vi /etc/ssh/sshd_config

# 3.修改以下内容即可   
PermitRootLogin yes         # //默认为no，需要开启root用户访问改为yes
PasswordAuthentication yes    #   //默认为no，改为yes开启密码登陆

# 4.给root用户设置新密码 xxx
passwd root


# 5.重启ssh
service sshd restart

