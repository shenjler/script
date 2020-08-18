module=$1
prefix="http://19.52.68.168/"
files=("automation" "cmdb" "common" "devops" "gateway" "job" "log" "notify" "probe" "report" "stock" "workflow")

for file in ${files[@]} ; do
    if [[ $file =~ $module ]]; then
        oldFile=`ls *.jar | grep $module`
	echo $oldFile
	if [ -f $oldFile ]; then
	    cp -R $oldFile "$oldFile-`date +%Y%m%d-%H%M`.bak"
	    rm -rf $oldFile
	fi
	moduleFile=`curl "$prefix/devops/version/file?service=$module"`
	echo "module file: "$moduleFile
	url=`curl "$prefix/devops/version/module?service=$module"`
	echo "url: "$url
	curl -o $moduleFile --connect-timeout 30 $url
	sed -i "s/\/data\/eoms\/.*\.jar/\/data\/eoms\/$moduleFile/" /usr/lib/systemd/system/$module.service
	systemctl daemon-reload
	pid=`jps | grep "$module" | awk '{print $1}'`
	if [ -z $pid ]; then
	    echo "pid = '', module [$module] is not running!"
	    systemctl start $module
	else
            echo "systemctl restart"
	    systemctl restart $module
	fi
	exit
    else
        echo "Not found module: "$module
    fi

done

#for file in ${files[@]} ; do 
#    if [[ $file =~ $module ]]; then
#        oldFile=`ls *.jar | grep $module`
#        echo $oldFile
#        if [ -f $oldFile ]; then
#            cp -R $oldFile "$oldFile-`date +%Y%m%d-%H%M`.bak"
#        fi
#        moduleFile=`curl "$prefix/devops/version/file?service=$module"`
#        url=`curl "$prefix/devops/version/module?service=$module"`
#        curl -o $moduleFile --connect-timeout 30 $url
#        sed "s/\/data\/eoms\/.*\.jar/\/data\/eoms\/$file/" /usr/lib/systemd/system/$module.service
#        systemctl daemon-reload
#
#        pid=`jps | grep "$module" | awk '{print $2}'`
#        if [ -z $pid ]; then
#            echo "pid = '', module [$module] is not running!"
#            systemctl start $module.service
#        else
#            echo "systemctl restart"
#            systemctl restart $module.service
#        fi
#        exit
#    else
#        echo "Not found module: "$module
#    fi
#done

