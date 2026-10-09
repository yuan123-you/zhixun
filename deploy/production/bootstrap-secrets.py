from pathlib import Path
import json, os, secrets, sys

root=Path('/srv/zhixun')
assert root.resolve()==root
private=root/'secrets'
private.mkdir(mode=0o700,exist_ok=True)
os.chmod(private,0o700)
path=private/'production.env'
if path.exists():
    raise SystemExit('Production credentials already exist; refusing to rotate a running deployment.')
optional=json.load(sys.stdin)
allowed=['MAIL_HOST','MAIL_PORT','MAIL_USERNAME','MAIL_PASSWORD','ZHIPU_API_KEY','ZHIPU_BASE_URL','ZHIPU_MODEL','ZHIPU_FALLBACK_MODEL','TENCENT_MAP_KEY','TENCENT_MAP_ENABLED','WECHAT_APP_ID','WECHAT_APP_SECRET','QQ_APP_ID','QQ_APP_KEY']
values={key:str(optional.get(key,'')) for key in allowed}
values.update({
 'MYSQL_ROOT_PASSWORD':secrets.token_hex(32),'MYSQL_USER':'zhixun_app',
 'MYSQL_PASSWORD':secrets.token_hex(32),'MYSQL_DATABASE':'zhixun',
 'REDIS_PASSWORD':secrets.token_hex(32),'REDIS_DATABASE':'0',
 'RABBITMQ_USER':'zhixun_app','RABBITMQ_PASSWORD':secrets.token_hex(32),'RABBITMQ_VHOST':'/',
 'MINIO_ACCESS_KEY':'zhixun_storage','MINIO_SECRET_KEY':secrets.token_hex(32),'MINIO_BUCKET':'zhixun',
 'OPENSEARCH_HTTP_PORT':'9200','OPENSEARCH_HOSTS':'http://opensearch:9200','OPENSEARCH_USERNAME':'','OPENSEARCH_PASSWORD':'',
 'JWT_ACCESS_SECRET':secrets.token_hex(64),'JWT_REFRESH_SECRET':secrets.token_hex(64),
 'JWT_ACCESS_EXPIRATION':'3600','JWT_REFRESH_EXPIRATION':'604800',
 'AES_SECRET_KEY':secrets.token_hex(32),'SSR_SECRET':secrets.token_hex(64),
 'ADMIN_USERNAME':'yuan_admin','ADMIN_PASSWORD':'aA9!'+secrets.token_urlsafe(12),
 'CORS_ALLOWED_ORIGINS':'https://glint.novo.ccwu.cc',
 'APP_DATA_VERSION':'20250625','OFFICIAL_ACCOUNT_ID':'1',
 'SENTINEL_ENABLED':'false','SENTINEL_TRANSPORT_PORT':'8719','SENTINEL_DASHBOARD':'',
 'TZ':'Asia/Shanghai','RELEASE_TAG':'d9dbaac',
})
if not values['MAIL_HOST']: values['MAIL_HOST']='smtp.qq.com'
if not values['MAIL_PORT']: values['MAIL_PORT']='465'
if not values['ZHIPU_BASE_URL']: values['ZHIPU_BASE_URL']='https://open.bigmodel.cn/api/paas/v4'
if not values['ZHIPU_MODEL']: values['ZHIPU_MODEL']='glm-4-flash'
if not values['TENCENT_MAP_ENABLED']: values['TENCENT_MAP_ENABLED']='false'
def quote(value):
    assert '\n' not in value and '\r' not in value
    return "'"+value.replace("'", "\\'")+"'"
with path.open('x') as f:
    os.chmod(path,0o600)
    f.write(''.join(k+'='+quote(v)+'\n' for k,v in values.items()))
credentials=private/'administrator.txt'
with credentials.open('x') as f:
    os.chmod(credentials,0o600)
    f.write('URL: https://glint.novo.ccwu.cc/admin/\nUsername: '+values['ADMIN_USERNAME']+'\nPassword: '+values['ADMIN_PASSWORD']+'\n')
print('Fresh private production credentials created; no values printed.')
print('Optional integrations configured:', ', '.join(k for k in allowed if values[k]))
