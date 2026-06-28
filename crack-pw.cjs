// 尝试用常见密码破解 bcrypt
const bcrypt = require('bcryptjs');
const h = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy';

// 优先尝试常见密码 + 一些 8-16 字符的组合
const candidates = [
  // 常见默认密码
  'password','12345678','123456789','1234567890','admin123',
  'admin1234','admin@123','root123','passw0rd','qwerty123',
  '11111111','00000000','88888888','66666666','aaaaaa',
  'abc12345','abcd1234','qwer1234','asdf1234','zxcv1234',
  'Password1','Password123','Password@1','Password@123',
  'Welcome1','Welcome123','Welcome@1','Welcome@123',
  'Changeme1','Changeme123','P@ssw0rd','Passw0rd1',
  'Admin@123','Admin123!','Admin@1234','Admin@12345',
  'test1234','Test1234','user1234','User1234',
  // 短密码
  '123','1234','12345','abc','abcd','admin','test',
  // 平台相关
  'zhixun','zhixun123','zhixun2026','Zhixun2026','Zhixun2026!',
  'zhixun321','Zhixun321','zhixun1234','Zhixun@123',
  // 加密常用
  'secret','secret123','Secret123','123qwe','Qwer1234',
  // 一些项目
  'app123','demo123','project','Project123','demo',
  // 8-12 字符含数字
  'a1234567','q1234567','test1234','test12345',
  // 弱密码
  'iloveyou','sunshine','princess','football','charlie',
  'shadow','master','michael','superman','batman',
];

(async () => {
  for (const p of candidates) {
    const ok = await bcrypt.compare(p, h);
    if (ok) {
      console.log('FOUND:', p);
      return;
    }
  }
  console.log('No match found among', candidates.length);
})();
