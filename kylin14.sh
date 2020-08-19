#!/bin/bash


smail(){
        smtp="idcmail.meizu.com 25" # 邮件服务器地址+25端口
        smtp_domain="meizu.com" # 发送邮件的域名，即@后面的
        FROM="dw@meizu.com" # 发送邮件地址
        RCPTTO=$1 # 收件人地址
        username_base64="ZHdAbWVpenUuY29t" # 用户名base64编码
        password_base64="I09wd2QjMTIzNDU2IQ==" # 密码base64编码
        ( for i in "ehlo $smtp_domain" "AUTH LOGIN" "$username_base64" "$password_base64" "MAIL FROM:<$FROM>" "RCPT TO:<$RCPTTO>" "DATA";do
                echo $i
                sleep 4
        done
        echo "Subject:kylin server can not access"
        echo "From:<$FROM>"
        echo "To:<$RCPTTO>"
        echo ""
        echo "server http://10.129.252.14:7070/kylin/login has error "
        echo "."
        sleep 2
        echo "quit" )|telnet $smtp
}
STATUS=$(curl -m 10 -s -o /dev/null -w '%{http_code}' http://10.129.252.14:7070/kylin/api/user/authentication)
if [ $STATUS -eq 200 ]; then
    
    t=`date '+%Y-%m-%d %H:%M:%S'`
    echo $t"  Got 200! All done!"
	 exit 0
else
    echo "Got $STATUS :( Not done yet..."
    echo "email notify"
    smail "shenjinlong@meizu.com" # 这里参数为收信地址
	 exit 1
fi




