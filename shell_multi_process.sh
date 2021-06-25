#!/bin/bash
# https://justcode.ikeepstudying.com/linux-shell%E5%AE%9E%E7%8E%B0%E5%A4%9A%E7%BA%BF%E7%A8%8B-forking-multi-threaded-processes-bash/
# https://blog.csdn.net/m0/article/details?share_token=9a9e4-4a5d-c-0d6bad
# https://www.cnblogs.com/dwdxdy/archive6.html

# curl -s -k  https://file.shenjl.club/ping.txt | sh
# curl -s -k https://file.shenjl.club/ping.txt  | sh -s -- 3 # 参数为线程数

curl -s -k -o ips.txt https://file.shenjl.club/ips.txt

function pingAction(){
    ip=$1
    res=`ping -c 3 $ip | egrep 'statistics|rtt' | sed -r 's/(--- | ping.*| min.*= | ms)//g' | sed ':a;N;$!ba;s/\nrtt/=/g'`
    out=`curl -s -L "https://www.shenjl.club/club/test/?name=hodor&${res}"`
    echo $res
 }


thread_num=8
if [[ $# -eq 1 && $(grep '^[[:digit:]]*$' <<< "$1") ]]; then
   thread_num=$1
   echo "set thread_num=$1"
else
   echo "$1 is not 1 param || not digit"
fi

# 使用文件描述符控制并发数
a=$(date +%H:%M:%S)
tempfifo="/tmp/$.fifo"
mkfifo ${tempfifo}
exec 6<>${tempfifo}
rm -f ${tempfifo}

for ((i=1;i<=${thread_num};i++))
do
{
    echo
}
done >&6

for line in `cat ips.txt`;do
    read -u6
    {
        pingAction  $line && { # 此处可以用来判断子进程的逻辑
            #echo "pingAction is finished"
            echo 
        }
        echo "" >&6
    } &
done

wait
exec 6>&-
 
b=$(date +%H:%M:%S)
rm -f ips.txt
echo -e "startTime:\t$a"
echo -e "endTime:\t$b"
