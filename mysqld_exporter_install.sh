

# mysqld_exporter_install
# https://segmentfault.com/a/1190000007040144
# https://blog.csdn.net/kk185800961/article/details/84635360   mysql指标项
# 1、安装 mysqld_exporter

wget https://github.com/prometheus/mysqld_exporter/releases/download/v0.13.0/mysqld_exporter-0.13.0.linux-amd64.tar.gz
tar -xzvf mysqld_exporter-*.*.tar.gz
cd mysqld_exporter-*.*


# 2、创建数据账号密码，及授权
CREATE USER 'exporter'@'localhost' IDENTIFIED BY 'exporter' WITH MAX_USER_CONNECTIONS 3;
GRANT PROCESS, REPLICATION CLIENT, SELECT ON *.* TO 'exporter'@'localhost';



# 3、增加配置prometheus 的job
scrape_configs:
  - job_name: mysql
    static_configs:
      - targets: ['172.16.253.104:9104']
        labels:
          instance: db-104


# 4、创建.my.cnf文件并运行mysqld_exporter：
cat > mysqld_exporter.cnf << EOF 
[client]
user=exporter
password=exporter
EOF

# 5、启动
./mysqld_exporter --config.my-cnf="mysqld_exporter.cnf" 2>&1 &
nohup ./mysqld_exporter --config.my-cnf="mysqld_exporter.cnf" &

ps -ef | grep mysqld_exporter

# start.sh脚本
cat > start.sh << BBB
nohup ./mysqld_exporter --config.my-cnf="mysqld_exporter.cnf" &
BBB
chmod +x start.sh


nohup ./mysqld_exporter --collect.info_schema.processlist.processes_by_user		\
--collect.info_schema.processlist.processes_by_host		\
--collect.info_schema.tables.databases="*"		\
--collect.mysql.user.privileges		\
--collect.info_schema.processlist		\
--collect.mysql.user		\
--collect.info_schema.tables		\
--collect.info_schema.innodb_tablespaces		\
--collect.info_schema.innodb_metrics		\
--collect.global_status		\
--collect.global_variables		\
--collect.slave_status		\
--collect.perf_schema.indexiowaits		\
--collect.perf_schema.tablelocks		\
--collect.perf_schema.eventsstatements		\
--collect.perf_schema.eventsstatementssum		\
--collect.perf_schema.eventswaits		\
--collect.auto_increment.columns		\
--collect.binlog_size		\
--collect.perf_schema.tableiowaits		\
--collect.perf_schema.replication_group_members		\
--collect.perf_schema.replication_group_member_stats		\
--collect.perf_schema.replication_applier_status_by_worker		\
--collect.info_schema.userstats		\
--collect.info_schema.clientstats		\
--collect.perf_schema.file_events		\
--collect.perf_schema.file_instances		\
--collect.perf_schema.memory_events		\
--collect.info_schema.innodb_cmpmem		\
--collect.info_schema.query_response_time		\
--collect.engine_tokudb_status		\
--collect.engine_innodb_status		\
--collect.heartbeat		\
--collect.info_schema.tablestats		\
--collect.info_schema.schemastats		\
--collect.info_schema.innodb_cmp		\
--collect.slave_hosts		\
--collect.info_schema.replica_host	 --config.my-cnf="mysqld_exporter.cnf" &