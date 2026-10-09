import json, re, subprocess, sys

HOST='glint.novo.ccwu.cc'

def request(path, origin=True):
    command=['curl','-fsS','--connect-timeout','6','--max-time','20']
    if origin:
        command+=['--resolve',HOST+':443:127.0.0.1']
    return subprocess.check_output(command+['https://'+HOST+path],text=True)

for origin in [True,False]:
    source='origin' if origin else 'public-domain'
    html=request('/',origin)
    title=re.search(r'<title>(.*?)</title>',html).group(1)
    print(source,'title='+title)
    assert '知讯' in title, 'Domain does not serve the deployed Zhixun application'
    assets=re.findall(r'(?:src|href)="(/assets/[^" ]+)"',html)
    assert assets
    for asset in assets:
        assert len(request(asset,origin))>0
    for path in ['/login','/admin/']:
        assert '<div id="app"' in request(path,origin)
    feed=json.loads(request('/api/v1/articles?page=1&size=5',origin))
    assert feed.get('code') in [0,200], 'Public article API failed'
    categories=json.loads(request('/api/v1/categories',origin))
    assert categories.get('code') in [0,200], 'Public category API failed'
    print(source,'PASS: pages,',len(assets),'assets, article/category APIs')

for path in ['/api/actuator/health','/api/v3/api-docs','/.env']:
    status=subprocess.check_output(['curl','-sS','--max-time','15','--resolve',HOST+':443:127.0.0.1','-o','/dev/null','-w','%{http_code}','https://'+HOST+path],text=True)
    assert status=='404', 'Restricted endpoint exposed: '+path+' '+status
print('PASS: diagnostic and secret-file routes are not public')
