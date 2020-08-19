

rem "D:\\Program Files\\PuTTY\\plink.exe" 192.168.0.114 -batch -pw shenjl ls /root
rem "D:\\Program Files\\PuTTY\\plink.exe" 192.168.0.114 -batch -pw shenjl /root/start.sh
rem "D:\\Program Files\\PuTTY\\plink.exe" 192.168.0.114 -batch -pw shenjl "ps -ef | grep nginx

.\\plink root@192.168.0.114 -batch -pw shenjl "ls /root; ps -ef | grep nginx; echo 'hello world'" 
pause