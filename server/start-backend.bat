@echo off
setlocal
for /f "usebackq tokens=1,2 delims==" %%a in ("d:\zhixun\.env") do (
    set "%%a=%%b"
)
cd /d d:\zhixun\server
java -jar target\zhixun-server-1.0.0.jar --spring.profiles.active=dev
