#!/bin/sh
module=$1
params=''

if [ $1 == 'eureka01' ]; then
  params='-Dspring.profiles.active=eureka01'
fi
if [ $1 == 'eureka02' ]; then
  params="-Dspring.profiles.active=eureka02" 
fi
echo $params

allowModules='eureka01,eureka02,discovery,gateway,common,workflow,notify,job,cmdb-service,log-service,automation,report,stock,devops,probe'
if [ $module == '' ];then
   echo "no args"
else
    pid=`ps -ef | grep java | grep eoms | grep "$module" | awk '{print $2}'`

    echo 'module pid = ' $pid
    if [ -z $pid ]; then
        echo "pid = '', module [$module] is not running!"
    fi
    if [ $pid ]; then
        if [[ $allowModules =~ $module ]]; then
            echo "module[$module], pid = $pid, kill -9 $pid "
            kill -9 $pid
        fi
    fi
    if [[ $allowModules =~ $module ]]; then
        # echo "start module [$module]"
	
	if [[ "eureka01,eureka02" =~ $1 ]]; then
            module='discovery'
	    echo "params=$params"
	else
	    echo ""
	    params="-Dspring.profiles.active=pro"
	fi
        echo "start module [$module]"
        nohup nice java $params -Duser.timezone=UTC+8 -jar /data/eoms/$module-*.jar 2>> /data/eoms/startlog/${module}.log >>/data/eoms/startlog/${module}out.log &
    else
        echo "module is not exist! please check! "
    fi
fi

