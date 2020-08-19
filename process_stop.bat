
@echo off
title stop-nginx
color 1b

tasklist|find /i "nginx.exe" && taskkill /F /IM nginx.exe > nul 
echo nginx  has been closed.
echo Continue to shut down node, or please close the current window.

tasklist|find /i "node.exe" && taskkill /F /IM node.exe > nul
echo node has been closed.

tasklist|find /i "cmd.exe" && taskkill /F /IM cmd.exe > nul

exit



echo "重启webpack"
start /D "E:\fed-node\project\sgr" stop.bat
start /D "E:\fed-node\project\sgr" start.bat



