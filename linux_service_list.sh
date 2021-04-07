
# 查看开机启动项
systemctl list-unit-files | grep enable



# springcloud微服务 
systemctl list-unit-files | grep -nE '(gateway|^common|workflow|customer|automation|^cmdb|stock|knowledge|report|^log|devops|^notify|^job|gis|^probe)'

systemctl list-unit-files | grep enable | grep -nE '(gateway|^common|workflow|customer|automation|^cmdb|stock|knowledge|report|^log|devops|^notify|^job|gis|^probe)'

ls -l /usr/lib/systemd/system/ | grep -nE '(gateway|common|workflow|customer|automation|cmdb|stock|knowledge|report|log.service|devops|notify|job.service|gis|probe)'


ls -l /data/eoms/*.jar
