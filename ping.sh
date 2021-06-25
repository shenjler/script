
# https://xin96jie.gitee.io/womennaxiaozhan/2019/12/18/shell%E5%AF%B9ping%E5%91%BD%E4%BB%A4%E7%BB%93%E6%9E%9C%E8%BF%9B%E8%A1%8C%E5%88%86%E8%A7%A3%E8%8E%B7%E5%8F%96ip%E5%9C%B0%E5%9D%80%E5%92%8C%E5%BB%B6%E8%BF%9F%E6%95%B0%E5%8F%8A%E7%8A%B6%E6%80%81/

ping for shell 


#!/bin/bash
for ips in `cat target_ip.txt`
do
	time_now=$(date "+%Y-%m-%d %H:%M:%S")
	echo "#####" $time_now "#####" >> /script/ping.txt
        IP=`grep 'PING' /script/result.txt |awk -F ' ' '{print $2}'`
        ping -c 3 ${ips} > /script/result.txt
        echo  "IP: $IP" >> /script/ping.txt
        status=` grep 'packet' /script/result.txt | awk -F" " '{print $6}'| awk -F"%" '{print $1}'`
	if [ $status -eq 0 ];then
		echo   "状态：连接成功" >> /script/ping.txt
        	delay=`grep "icmp_seq=" /script/result.txt |awk '{print $7}'|awk -F'=' '{print $2}'|sort -nr|head -1`	
       		echo  "延迟数(ms): $delay" >> /script/ping.txt
	else
		echo   "状态：连接失败" >> /script/ping.txt
	fi	
done

