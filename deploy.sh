module=$1
prefix="http://19.52.68.168/"
if [[ $module == "wechat" ]]; then 
    wechatFile="wechat.zip"
    if [ -f $wechatFile ]; then
        cp -R $wechatFile "$wechatFile-`date +%Y%m%d-%H%M`.bak"
    fi
    curl -o $wechatFile $prefix$wechatFile
    unzip -o $wechatFile -d ./ > /dev/null 2>&1
    # rm -rf `find . -name '*.bak' -mtime +5`
    exit
fi

killModule(){
    allowModules='gateway,common,workflow,notify,job,cmdb-service,log-service,automation,report,wechat,devops,probe,gis'
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
}

if [[ $module == "gis" ]]; then
    gisFile="gis-1.0-SNAPSHOT.jar"
    if [ -f $gisFile ]; then
        cp -R $gisFile "$gisFile-`date +%Y%m%d-%H%M`.bak"
    fi
    curl -o $gisFile --connect-timeout 30 $prefix$gisFile
    killModule gis
    exit
fi


files=("automation-service" "cmdb-service" "common-service" "devops-service" "gateway" "job-service" "log-service" "notify-service" "probe-service" "report" "stock-service" "workflow-service")

subfix="-0.0.1-SNAPSHOT.jar"


for file in ${files[@]} ; do 
    if [[ $file =~ $module ]]; then
        fileName=$file$subfix
        if [ -f $fileName ]; then
            cp -R $fileName "$fileName-`date +%Y%m%d-%H%M`.bak"
        fi
        echo $fileName
    	curl -o $fileName --connect-timeout 30 $prefix$fileName
	killModule $module
        exit
    else
        echo "Not found module: "$module
    fi
done
