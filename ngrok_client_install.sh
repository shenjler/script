
mkdir ~/ngrok && cd ~/ngrok

wget http://file.shenjl.club/ngrok

wget http://file.shenjl.club/ngrok.cfg

wget http://file.shenjl.club/restart.sh

chmod +x ngrok
chmod +x restart.sh

sshPort=8022
if [ $1 ]; then
    echo " sshPort = $1 "
    sshPort=$1
fi

sed -i "s/remote_port: 8022/remote_port: $sshPort/" ngrok.cfg
#sed -i "s/remote_port: 9999/remote_port: 8023/" ngrok.cfg

./restart.sh 

tail -300 log.txt


# curl http://file.shenjl.club/ngrok_client_install.sh | bash -s -- 8222