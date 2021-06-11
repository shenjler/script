
-- https://www.jianshu.com/p/cb0152efac32
-- https://blog.csdn.net/JesseYoung/article/details/35257527
-- https://blog.csdn.net/xuSir_1/article/details/85233823

   -- 第一种 while 循环 
   -- 求 1-n 的和
   /*  while循环语法：
   while 条件 DO
               循环体;
   end while;
   */
    create procedure sum1(a int) 
    begin
        declare sum int default 0;  -- default 是指定该变量的默认值
        declare i int default 1;
    while i<=a DO -- 循环开始
        set sum=sum+i;
        set i=i+1;
    end while; -- 循环结束
    select sum;  -- 输出结果
    end;
    -- 执行存储过程
    call sum1(100);
    -- 删除存储过程
    drop procedure if exists sum1



-- ---------定时网络延时及连接状态 表数据

-- 定时更新数据
    create procedure updateData() 
    begin
        declare sum int default 0;  -- default 是指定该变量的默认值
        declare i int default 1;
        declare size int default 10;
        select count(1) into size from info;
        while i<=size DO -- 循环开始
            set sum=sum+i;
            update info set status= if(FLOOR((RAND() * (10-1+1)))>0,1,0), delay =FLOOR((RAND() * (200-20+1))+20), create_date = now() where id = i
            set i=i+1;
        end while; -- 循环结束
        select i;  -- 输出结果
    end;
    -- 执行存储过程
    call updateData();
    -- 删除存储过程
    drop procedure if exists updateData

-- 查看是否开启事件调度器
show variables like '%event_scheduler%';
-- +-----------------+-------+
-- | Variable_name   | Value |
-- +-----------------+-------+
-- | event_scheduler | OFF   |

-- 开启事件调度器
SET GLOBAL event_scheduler = ON;


-- 查看当前所在库的事件
show events;

-- 查看所有事件
select * from mysql.event;


-- 事件创建语法如下
CREATE
    [DEFINER = { user | CURRENT_USER }]
    EVENT
    [IF NOT EXISTS]
    event_name
    ON SCHEDULE schedule
    [ON COMPLETION [NOT] PRESERVE]
    [ENABLE | DISABLE | DISABLE ON SLAVE]
    [COMMENT 'comment']
    DO event_body;
 
schedule:
    AT timestamp [+ INTERVAL interval] ...
  | EVERY interval
    [STARTS timestamp [+ INTERVAL interval] ...]
    [ENDS timestamp [+ INTERVAL interval] ...]
 
interval:
    quantity {YEAR | QUARTER | MONTH | DAY | HOUR | MINUTE |
              WEEK | SECOND | YEAR_MONTH | DAY_HOUR | DAY_MINUTE |
              DAY_SECOND | HOUR_MINUTE | HOUR_SECOND | MINUTE_SECOND}

-- 创建事件并执行， 每2分钟执行一次
CREATE EVENT IF NOT EXISTS updateNetworkEvent on SCHEDULE
EVERY 2 MINUTE
ON COMPLETION PRESERVE
ENABLE
COMMENT '定时更新网络状态及网络延迟'
DO call updateData();