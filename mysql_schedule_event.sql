
select * from dhq_common.common_sys_base_setting  where `code` = 'screenAlarmDataRange'

update dhq_common.common_sys_base_setting set data = 215 + DATEDIFF(now(), '2021-03-12')  where `code` = 'screenAlarmDataRange'


SELECT 215 + DATEDIFF(now(), '2021-03-12');

-- https://blog.csdn.net/chenshun123/article/details/79677193
SHOW VARIABLES LIKE 'event_scheduler';
SET GLOBAL event_scheduler = ON;
SHOW PROCESSLIST;
 
--  查看定时任务
 SELECT * FROM information_schema.events; 
 
-- 关闭定时任务

DROP event event_update_config;
 
 
-- 每天凌晨1点执行
CREATE EVENT event_update_config
    ON SCHEDULE EVERY 1 DAY STARTS DATE_ADD(DATE_ADD(CURDATE(), INTERVAL 1 DAY), INTERVAL 1 HOUR)   
    ON COMPLETION PRESERVE ENABLE
    DO update dhq_common.common_sys_base_setting set data = 215 + DATEDIFF(now(), '2021-03-12') 
		where `code` = 'screenAlarmDataRange';





