import json
import unittest
from pathlib import Path

ROOT = Path(__file__).resolve().parent

class ProductionConfigTests(unittest.TestCase):
    def config(self):
        path = ROOT / 'compose.production.json'
        self.assertTrue(path.is_file(), 'Missing isolated production Compose configuration')
        return json.loads(path.read_text(encoding='utf-8-sig'))
    def test_private_infrastructure_and_loopback_application_ports(self):
        config = self.config()
        services = config['services']
        for name in ['mysql', 'redis', 'rabbitmq', 'opensearch']:
            self.assertFalse(services[name].get('ports'), name + ' must not publish infrastructure ports')
        for name in ['server', 'client', 'admin']:
            for port in services[name]['ports']:
                self.assertTrue(port.startswith('127.0.0.1:'), name + ' must bind loopback only')
        self.assertNotIn('nginx', services, 'Do not compete with existing host Nginx')
        self.assertNotIn('sonarqube', services)
    def test_credentials_and_persistence(self):
        config = self.config()
        for service in config['services'].values():
            for key, value in service.get('environment', {}).items():
                if any(word in key for word in ['PASSWORD', 'SECRET', 'API_KEY']):
                    self.assertTrue(str(value).startswith('${'), key + ' must come from private environment')
        self.assertEqual('https://glint.novo.ccwu.cc', config['services']['server']['environment']['CORS_ALLOWED_ORIGINS'])
        self.assertTrue(config['services']['mysql']['volumes'])
        self.assertTrue(config['services']['server']['volumes'])
    def test_builtin_file_storage_is_persistent_without_unavailable_image(self):
        services = self.config()['services']
        self.assertEqual('false', services['server']['environment'].get('MINIO_ENABLED'))
        self.assertTrue('uploads_data:/app/uploads' in services['server']['volumes'])
        self.assertNotIn('minio', services)

    def test_forwarded_client_identity_is_not_spoofable(self):
        environment = self.config()['services']['server']['environment']
        self.assertEqual('framework', environment.get('SERVER_FORWARD_HEADERS_STRATEGY'))
        text = (ROOT / 'glint.novo.ccwu.cc.conf').read_text(encoding='utf-8-sig')
        self.assertIn('proxy_set_header X-Forwarded-For $remote_addr;', text)
        self.assertNotIn('$proxy_add_x_forwarded_for', text)
        self.assertIn('proxy_set_header Forwarded "";', text)
        self.assertIn('proxy_set_header X-Forwarded-Host $host;', text)

    def test_site_is_domain_scoped_with_tls_and_websocket_support(self):
        path = ROOT / 'glint.novo.ccwu.cc.conf'
        self.assertTrue(path.is_file(), 'Missing glint-only Nginx site')
        text = path.read_text(encoding='utf-8-sig')
        self.assertIn('server_name glint.novo.ccwu.cc;', text)
        self.assertIn('listen 443 ssl;', text)
        self.assertNotIn('default_server', text)
        self.assertIn('proxy_set_header Upgrade $http_upgrade;', text)
        self.assertIn('location ^~ /api/actuator/', text)
        self.assertIn('return 404;', text)

if __name__ == '__main__':
    unittest.main()
