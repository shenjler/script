
@echo  update ip 172.16.253.95


set ipaddress=172.16.253.95

set mask=255.255.255.0

set gateway=172.16.253.254

set dns1=8.8.8.8


set name="Wi-Fi"


@pause
@echo off

::��������Ĳ����޸���������

set /p type=��ѡ�����÷�ʽ��1: ��̬��2: ��̬��
if /i %type%==1 goto :STATIC_IP
if /i %type%==2 goto :DHCP_IP

:STATIC_IP
echo ���ڽ��о�̬IP���ã����Ե�...

::netsh interface ip set address name=%name% source=static addr=%ipaddress% mask=%mask%

netsh interface ipv4 set address name=%name% static %ipaddress% %mask% %gateway%


echo. IP��ַ = %ipaddress% .�������
echo. ������ = %mask% .�������

::netsh interface ip set address name=%name% gateway=%gateway% gwmetric=1 >nul
echo. ���� = %gateway% .�������

::netsh interface ip set dns name=%name% source=static addr=%dns1% register=PRIMARY >nul

netsh interface ipv4 set dns name=%name% static %dns1%

echo. ��ѡDNS = %dns1% .�������


goto :SETEND

:DHCP_IP
echo ���ڽ��ж�̬IP���ã����Ե�...

netsh interface ipv4 set address name=%name% source=dhcp

netsh interface ip set address name=%name% source=dhcp
::netsh interface ip set dns name=%name% source=dhcp register=PRIMARY
::netsh interface ip set wins name=%name% source=dhcp

set gateway=172.16.23.254
netsh interface ip set address name=%name% gateway=%gateway% gwmetric=1 >nul

goto :SETEND

:SETEND
echo. ===============IP�������================
echo. =========================================
echo. == ���²��������Ƿ�����
echo. == ������������ʽ��
echo. == "Reply from %gateway%: bytes=32 time<1ms TTL=64"
echo. == �������������������粻������
echo. =========================================

:: �ȴ�2��
ping -n 5 127.0.0.1>nul
::ping %gateway% -n 1
::pause
ping %gateway% -n 3

::netsh interface ipv4 show config

pause