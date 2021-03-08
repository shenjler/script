 #/usr/lib/systemd/system/${module}.service
 
module=spider
cat > /usr/lib/systemd/system/${module}.service << AAA
[Unit]
Description=${module}

[Service]
ExecStart=${JAVA_HOME}/java  -Duser.timezone=UTC+8 -jar /data/spider/pccw-spider-1.0-SNAPSHOT.jar >/dev/null
ExecStop=/usr/bin/kill -9 `cat /run/java-${module}.pid`
Restart=always
PIDFile=/run/java-${module}.pid

[Install]
WantedBy=multi-user.target

AAA



systemctl start ${module}.service


systemctl status ${module}.service
systemctl daemon-reload
systemctl restart ${module}.service

