cat > /lib/systemd/system/nacos.service << AAA
[Unit]
Description=nacos
After=network.target

[Service]
Type=forking
ExecStart=/data/eoms/nacos/bin/startup.sh
ExecReload=/data/eoms/nacos/bin/shutdown.sh
ExecStop=/data/eoms/nacos/bin/shutdown.sh
PrivateTmp=true

[Install]
WantedBy=multi-user.target
AAA


systemctl daemon-reload
systemctl enable nacos.service
systemctl start nacos.service
systemctl status nacos.service


echo $JAVA_HOME     # /opt/jdk 为java_home目录
sed -i "s/cygwin=false/JAVA_HOME=\/opt\/jdk\ncygwin=false/"  /data/eoms/nacos/bin/startup.sh



vim /data/eoms/nacos/bin/startup.sh
