"""
测试真实登录场景 - 验证受限页面的访问和操作
"""
import subprocess
import os
os.environ['MYSQL_PWD'] = '123456'

# 1. 查询测试用户
r = subprocess.run(
    ['C:\\Program Files\\MySQL\\MySQL Server 8.0\\bin\\mysql.exe', '-uroot', '-e',
     'SELECT id, username, nickname, role FROM zhixun.sys_user ORDER BY id LIMIT 8'],
    capture_output=True, text=True
)
print("=== 测试用户 ===")
print(r.stdout)
