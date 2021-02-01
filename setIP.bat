
@echo  update ip 172.16.253.95


set ipaddress=172.16.253.95

set mask=255.255.255.0

set gateway=172.16.253.254

set dns1=8.8.8.8


set name="Wi-Fi"


@pause
@echo off

::请根据您的参数修改以上数据

set /p type=请选择设置方式，1: 静态，2: 动态：
if /i %type%==1 goto :STATIC_IP
if /i %type%==2 goto :DHCP_IP

:STATIC_IP
echo 正在进行静态IP设置，请稍等...

::netsh interface ip set address name=%name% source=static addr=%ipaddress% mask=%mask%

netsh interface ipv4 set address name=%name% static %ipaddress% %mask% %gateway%


echo. IP地址 = %ipaddress% .完成设置
echo. 子掩码 = %mask% .完成设置

::netsh interface ip set address name=%name% gateway=%gateway% gwmetric=1 >nul
echo. 网关 = %gateway% .完成设置

::netsh interface ip set dns name=%name% source=static addr=%dns1% register=PRIMARY >nul

netsh interface ipv4 set dns name=%name% static %dns1%

echo. 首选DNS = %dns1% .完成设置


goto :SETEND

:DHCP_IP
echo 正在进行动态IP设置，请稍等...

netsh interface ipv4 set address name=%name% source=dhcp

netsh interface ip set address name=%name% source=dhcp
::netsh interface ip set dns name=%name% source=dhcp register=PRIMARY
::netsh interface ip set wins name=%name% source=dhcp

set gateway=172.16.23.254
netsh interface ip set address name=%name% gateway=%gateway% gwmetric=1 >nul

goto :SETEND

:SETEND
echo. ===============IP设置完成================
echo. =========================================
echo. == 以下测试网络是否正常
echo. == 若出现如下形式：
echo. == "Reply from %gateway%: bytes=32 time<1ms TTL=64"
echo. == 则网络正常，否则网络不正常！
echo. =========================================

:: 等待2秒
ping -n 5 127.0.0.1>nul
::ping %gateway% -n 1
::pause
ping %gateway% -n 3

::netsh interface ipv4 show config

pause