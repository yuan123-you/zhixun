Start-Process -FilePath "D:\Java\jdk\bin\java.exe" -ArgumentList @(
    "-jar",
    "-Xms512m",
    "-Xmx1024m",
    "d:\zhixun\server\target\zhixun-server-1.0.0.jar",
    "--spring.profiles.active=local"
) -WorkingDirectory "d:\zhixun\server" -RedirectStandardOutput "d:\zhixun\my-server.log" -RedirectStandardError "d:\zhixun\my-server-err.log" -NoNewWindow
"started"
