yum install -y tinyproxy
cat /etc/tinyproxy/tinyproxy.conf | grep -v '^#'| grep -v '^$'
sed -i 's/Allow 127.0.0.1/#Allow 127.0.0.1/' /etc/tinyproxy/tinyproxy.conf

systemctl restart tinyproxy

systemctl status tinyproxy
