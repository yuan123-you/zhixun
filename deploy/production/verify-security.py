from pathlib import Path
import base64, http.client, json, socket, ssl

HOST='glint.novo.ccwu.cc'
class OriginHTTPS(http.client.HTTPSConnection):
    def connect(self):
        self.sock=self._context.wrap_socket(socket.create_connection(('127.0.0.1',443),timeout=20),server_hostname=HOST)

def request(method,path,data=None,headers=None):
    conn=OriginHTTPS(HOST,timeout=20,context=ssl.create_default_context())
    conn.request(method,path,body=data,headers=headers or {})
    response=conn.getresponse()
    body=response.read()
    result=(response.status,dict(response.getheaders()),body)
    conn.close()
    return result
status,headers,raw=request('GET','/api/v1/categories',headers={'Forwarded':'for=127.0.0.1;proto=http;host=attacker.invalid','X-Forwarded-Host':'attacker.invalid'})
assert status==200
assert 'secure' in headers.get('Set-Cookie','').lower(), 'Untrusted forwarding headers can downgrade the secure cookie'
print('PASS: spoofed proxy headers cannot downgrade HTTPS cookies')
values={}
for line in Path('/srv/zhixun/secrets/production.env').read_text().splitlines():
    if '=' in line:
        key,value=line.split('=',1)
        values[key]=value.strip("'")
body=json.dumps({'username':values['ADMIN_USERNAME'],'password':values['ADMIN_PASSWORD']}).encode()
status,headers,raw=request('POST','/api/v1/auth/login',body,{'Content-Type':'application/json','X-Client-Type':'admin'})
login=json.loads(raw)
print('Administrator login HTTP status:',status,'application code:',login.get('code'))
assert status==200 and login.get('code') in [0,200], 'Configured administrator cannot log in'
token=login['data']['accessToken']
assert token
print('PASS: strong administrator credentials authenticate; token not printed')
status,headers,raw=request('GET','/api/v1/admin/users')
assert status in [401,403], 'Anonymous visitor can access administration API'
print('PASS: administration API rejects anonymous requests')
status,headers,raw=request('GET','/api/v1/categories',headers={'Authorization':'Bearer '+token})
assert json.loads(raw).get('code') in [0,200]
# Synthetic one-pixel PNG; no user media or personal file is uploaded.
png=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aN9kAAAAASUVORK5CYII=')
boundary='zhixun-deployment-check'
body=('--'+boundary+'\r\nContent-Disposition: form-data; name="file"; filename="deployment-check.png"\r\nContent-Type: image/png\r\n\r\n').encode()+png+('\r\n--'+boundary+'--\r\n').encode()
status,headers,raw=request('POST','/api/v1/files/upload/image',body,{'Content-Type':'multipart/form-data; boundary='+boundary,'Authorization':'Bearer '+token})
upload=json.loads(raw)
assert status==200 and upload.get('code') in [0,200], 'Authenticated image upload failed'
path=upload['data']
assert path.startswith(('/api/v1/files/view/', '/api/uploads/'))
print('Upload returned supported storage path prefix:', path.rsplit('/',1)[0])
status,headers,raw=request('GET',path)
assert status==200 and raw==png, 'Uploaded image cannot be read back byte-for-byte'
print('PASS: authenticated upload and anonymous image read-back')
# Read the XSRF cookie for a later externally-originated check.
print('PASS: deployed authentication and file-storage smoke tests')
