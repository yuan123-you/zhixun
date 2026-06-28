/**
 * 中国行政区划数据工具
 * 提供省/市/区三级联动、坐标匹配、IP定位等功能
 */

export interface RegionNode {
  label: string
  value: string
  children?: RegionNode[]
}

/** 省级行政区列表 */
export const provinces: RegionNode[] = [
  {
    label: '北京市', value: '110000', children: [
      { label: '东城区', value: '110101' }, { label: '西城区', value: '110102' },
      { label: '朝阳区', value: '110105' }, { label: '丰台区', value: '110106' },
      { label: '石景山区', value: '110107' }, { label: '海淀区', value: '110108' },
      { label: '门头沟区', value: '110109' }, { label: '房山区', value: '110111' },
      { label: '通州区', value: '110112' }, { label: '顺义区', value: '110113' },
      { label: '昌平区', value: '110114' }, { label: '大兴区', value: '110115' },
      { label: '怀柔区', value: '110116' }, { label: '平谷区', value: '110117' },
      { label: '密云区', value: '110118' }, { label: '延庆区', value: '110119' },
    ]
  },
  {
    label: '天津市', value: '120000', children: [
      { label: '和平区', value: '120101' }, { label: '河东区', value: '120102' },
      { label: '河西区', value: '120103' }, { label: '南开区', value: '120104' },
      { label: '河北区', value: '120105' }, { label: '红桥区', value: '120106' },
      { label: '东丽区', value: '120110' }, { label: '西青区', value: '120111' },
      { label: '津南区', value: '120112' }, { label: '北辰区', value: '120113' },
      { label: '武清区', value: '120114' }, { label: '宝坻区', value: '120115' },
      { label: '滨海新区', value: '120116' }, { label: '宁河区', value: '120117' },
      { label: '静海区', value: '120118' }, { label: '蓟州区', value: '120119' },
    ]
  },
  {
    label: '上海市', value: '310000', children: [
      { label: '黄浦区', value: '310101' }, { label: '徐汇区', value: '310104' },
      { label: '长宁区', value: '310105' }, { label: '静安区', value: '310106' },
      { label: '普陀区', value: '310107' }, { label: '虹口区', value: '310109' },
      { label: '杨浦区', value: '310110' }, { label: '闵行区', value: '310112' },
      { label: '宝山区', value: '310113' }, { label: '嘉定区', value: '310114' },
      { label: '浦东新区', value: '310115' }, { label: '金山区', value: '310116' },
      { label: '松江区', value: '310117' }, { label: '青浦区', value: '310118' },
      { label: '奉贤区', value: '310120' }, { label: '崇明区', value: '310151' },
    ]
  },
  {
    label: '重庆市', value: '500000', children: [
      { label: '万州区', value: '500101' }, { label: '涪陵区', value: '500102' },
      { label: '渝中区', value: '500103' }, { label: '大渡口区', value: '500104' },
      { label: '江北区', value: '500105' }, { label: '沙坪坝区', value: '500106' },
      { label: '九龙坡区', value: '500107' }, { label: '南岸区', value: '500108' },
      { label: '北碚区', value: '500109' }, { label: '渝北区', value: '500112' },
      { label: '巴南区', value: '500113' }, { label: '黔江区', value: '500114' },
    ]
  },
  {
    label: '河北省', value: '130000', children: [
      { label: '石家庄市', value: '130100' }, { label: '唐山市', value: '130200' },
      { label: '秦皇岛市', value: '130300' }, { label: '邯郸市', value: '130400' },
      { label: '邢台市', value: '130500' }, { label: '保定市', value: '130600' },
      { label: '张家口市', value: '130700' }, { label: '承德市', value: '130800' },
      { label: '沧州市', value: '130900' }, { label: '廊坊市', value: '131000' },
      { label: '衡水市', value: '131100' },
    ]
  },
  {
    label: '山西省', value: '140000', children: [
      { label: '太原市', value: '140100' }, { label: '大同市', value: '140200' },
      { label: '阳泉市', value: '140300' }, { label: '长治市', value: '140400' },
      { label: '晋城市', value: '140500' }, { label: '朔州市', value: '140600' },
      { label: '晋中市', value: '140700' }, { label: '运城市', value: '140800' },
      { label: '忻州市', value: '140900' }, { label: '临汾市', value: '141000' },
      { label: '吕梁市', value: '141100' },
    ]
  },
  {
    label: '内蒙古自治区', value: '150000', children: [
      { label: '呼和浩特市', value: '150100' }, { label: '包头市', value: '150200' },
      { label: '乌海市', value: '150300' }, { label: '赤峰市', value: '150400' },
      { label: '通辽市', value: '150500' }, { label: '鄂尔多斯市', value: '150600' },
      { label: '呼伦贝尔市', value: '150700' }, { label: '巴彦淖尔市', value: '150800' },
      { label: '乌兰察布市', value: '150900' },
    ]
  },
  {
    label: '辽宁省', value: '210000', children: [
      { label: '沈阳市', value: '210100' }, { label: '大连市', value: '210200' },
      { label: '鞍山市', value: '210300' }, { label: '抚顺市', value: '210400' },
      { label: '本溪市', value: '210500' }, { label: '丹东市', value: '210600' },
      { label: '锦州市', value: '210700' }, { label: '营口市', value: '210800' },
      { label: '阜新市', value: '210900' }, { label: '辽阳市', value: '211000' },
      { label: '盘锦市', value: '211100' }, { label: '铁岭市', value: '211200' },
      { label: '朝阳市', value: '211300' }, { label: '葫芦岛市', value: '211400' },
    ]
  },
  {
    label: '吉林省', value: '220000', children: [
      { label: '长春市', value: '220100' }, { label: '吉林市', value: '220200' },
      { label: '四平市', value: '220300' }, { label: '辽源市', value: '220400' },
      { label: '通化市', value: '220500' }, { label: '白山市', value: '220600' },
      { label: '松原市', value: '220700' }, { label: '白城市', value: '220800' },
    ]
  },
  {
    label: '黑龙江省', value: '230000', children: [
      { label: '哈尔滨市', value: '230100' }, { label: '齐齐哈尔市', value: '230200' },
      { label: '鸡西市', value: '230300' }, { label: '鹤岗市', value: '230400' },
      { label: '双鸭山市', value: '230500' }, { label: '大庆市', value: '230600' },
      { label: '伊春市', value: '230700' }, { label: '佳木斯市', value: '230800' },
      { label: '七台河市', value: '230900' }, { label: '牡丹江市', value: '231000' },
      { label: '黑河市', value: '231100' }, { label: '绥化市', value: '231200' },
    ]
  },
  {
    label: '江苏省', value: '320000', children: [
      { label: '南京市', value: '320100' }, { label: '无锡市', value: '320200' },
      { label: '徐州市', value: '320300' }, { label: '常州市', value: '320400' },
      { label: '苏州市', value: '320500' }, { label: '南通市', value: '320600' },
      { label: '连云港市', value: '320700' }, { label: '淮安市', value: '320800' },
      { label: '盐城市', value: '320900' }, { label: '扬州市', value: '321000' },
      { label: '镇江市', value: '321100' }, { label: '泰州市', value: '321200' },
      { label: '宿迁市', value: '321300' },
    ]
  },
  {
    label: '浙江省', value: '330000', children: [
      { label: '杭州市', value: '330100' }, { label: '宁波市', value: '330200' },
      { label: '温州市', value: '330300' }, { label: '嘉兴市', value: '330400' },
      { label: '湖州市', value: '330500' }, { label: '绍兴市', value: '330600' },
      { label: '金华市', value: '330700' }, { label: '衢州市', value: '330800' },
      { label: '舟山市', value: '330900' }, { label: '台州市', value: '331000' },
      { label: '丽水市', value: '331100' },
    ]
  },
  {
    label: '安徽省', value: '340000', children: [
      { label: '合肥市', value: '340100' }, { label: '芜湖市', value: '340200' },
      { label: '蚌埠市', value: '340300' }, { label: '淮南市', value: '340400' },
      { label: '马鞍山市', value: '340500' }, { label: '淮北市', value: '340600' },
      { label: '铜陵市', value: '340700' }, { label: '安庆市', value: '340800' },
      { label: '黄山市', value: '341000' }, { label: '滁州市', value: '341100' },
      { label: '阜阳市', value: '341200' }, { label: '宿州市', value: '341300' },
      { label: '六安市', value: '341500' }, { label: '亳州市', value: '341600' },
      { label: '池州市', value: '341700' }, { label: '宣城市', value: '341800' },
    ]
  },
  {
    label: '福建省', value: '350000', children: [
      { label: '福州市', value: '350100' }, { label: '厦门市', value: '350200' },
      { label: '莆田市', value: '350300' }, { label: '三明市', value: '350400' },
      { label: '泉州市', value: '350500' }, { label: '漳州市', value: '350600' },
      { label: '南平市', value: '350700' }, { label: '龙岩市', value: '350800' },
      { label: '宁德市', value: '350900' },
    ]
  },
  {
    label: '江西省', value: '360000', children: [
      { label: '南昌市', value: '360100' }, { label: '景德镇市', value: '360200' },
      { label: '萍乡市', value: '360300' }, { label: '九江市', value: '360400' },
      { label: '新余市', value: '360500' }, { label: '鹰潭市', value: '360600' },
      { label: '赣州市', value: '360700' }, { label: '吉安市', value: '360800' },
      { label: '宜春市', value: '360900' }, { label: '抚州市', value: '361000' },
      { label: '上饶市', value: '361100' },
    ]
  },
  {
    label: '山东省', value: '370000', children: [
      { label: '济南市', value: '370100' }, { label: '青岛市', value: '370200' },
      { label: '淄博市', value: '370300' }, { label: '枣庄市', value: '370400' },
      { label: '东营市', value: '370500' }, { label: '烟台市', value: '370600' },
      { label: '潍坊市', value: '370700' }, { label: '济宁市', value: '370800' },
      { label: '泰安市', value: '370900' }, { label: '威海市', value: '371000' },
      { label: '日照市', value: '371100' }, { label: '临沂市', value: '371300' },
      { label: '德州市', value: '371400' }, { label: '聊城市', value: '371500' },
      { label: '滨州市', value: '371600' }, { label: '菏泽市', value: '371700' },
    ]
  },
  {
    label: '河南省', value: '410000', children: [
      { label: '郑州市', value: '410100' }, { label: '开封市', value: '410200' },
      { label: '洛阳市', value: '410300' }, { label: '平顶山市', value: '410400' },
      { label: '安阳市', value: '410500' }, { label: '鹤壁市', value: '410600' },
      { label: '新乡市', value: '410700' }, { label: '焦作市', value: '410800' },
      { label: '濮阳市', value: '410900' }, { label: '许昌市', value: '411000' },
      { label: '漯河市', value: '411100' }, { label: '三门峡市', value: '411200' },
      { label: '南阳市', value: '411300' }, { label: '商丘市', value: '411400' },
      { label: '信阳市', value: '411500' }, { label: '周口市', value: '411600' },
      { label: '驻马店市', value: '411700' },
    ]
  },
  {
    label: '湖北省', value: '420000', children: [
      { label: '武汉市', value: '420100' }, { label: '黄石市', value: '420200' },
      { label: '十堰市', value: '420300' }, { label: '宜昌市', value: '420500' },
      { label: '襄阳市', value: '420600' }, { label: '鄂州市', value: '420700' },
      { label: '荆门市', value: '420800' }, { label: '孝感市', value: '420900' },
      { label: '荆州市', value: '421000' }, { label: '黄冈市', value: '421100' },
      { label: '咸宁市', value: '421200' }, { label: '随州市', value: '421300' },
    ]
  },
  {
    label: '湖南省', value: '430000', children: [
      { label: '长沙市', value: '430100' }, { label: '株洲市', value: '430200' },
      { label: '湘潭市', value: '430300' }, { label: '衡阳市', value: '430400' },
      { label: '邵阳市', value: '430500' }, { label: '岳阳市', value: '430600' },
      { label: '常德市', value: '430700' }, { label: '张家界市', value: '430800' },
      { label: '益阳市', value: '430900' }, { label: '郴州市', value: '431000' },
      { label: '永州市', value: '431100' }, { label: '怀化市', value: '431200' },
      { label: '娄底市', value: '431300' },
    ]
  },
  {
    label: '广东省', value: '440000', children: [
      { label: '广州市', value: '440100' }, { label: '韶关市', value: '440200' },
      { label: '深圳市', value: '440300' }, { label: '珠海市', value: '440400' },
      { label: '汕头市', value: '440500' }, { label: '佛山市', value: '440600' },
      { label: '江门市', value: '440700' }, { label: '湛江市', value: '440800' },
      { label: '茂名市', value: '440900' }, { label: '肇庆市', value: '441200' },
      { label: '惠州市', value: '441300' }, { label: '梅州市', value: '441400' },
      { label: '汕尾市', value: '441500' }, { label: '河源市', value: '441600' },
      { label: '阳江市', value: '441700' }, { label: '清远市', value: '441800' },
      { label: '东莞市', value: '441900' }, { label: '中山市', value: '442000' },
      { label: '潮州市', value: '445100' }, { label: '揭阳市', value: '445200' },
      { label: '云浮市', value: '445300' },
    ]
  },
  {
    label: '广西壮族自治区', value: '450000', children: [
      { label: '南宁市', value: '450100' }, { label: '柳州市', value: '450200' },
      { label: '桂林市', value: '450300' }, { label: '梧州市', value: '450400' },
      { label: '北海市', value: '450500' }, { label: '防城港市', value: '450600' },
      { label: '钦州市', value: '450700' }, { label: '贵港市', value: '450800' },
      { label: '玉林市', value: '450900' }, { label: '百色市', value: '451000' },
      { label: '贺州市', value: '451100' }, { label: '河池市', value: '451200' },
      { label: '来宾市', value: '451300' }, { label: '崇左市', value: '451400' },
    ]
  },
  {
    label: '海南省', value: '460000', children: [
      { label: '海口市', value: '460100' }, { label: '三亚市', value: '460200' },
      { label: '三沙市', value: '460300' }, { label: '儋州市', value: '460400' },
    ]
  },
  {
    label: '四川省', value: '510000', children: [
      { label: '成都市', value: '510100' }, { label: '自贡市', value: '510300' },
      { label: '攀枝花市', value: '510400' }, { label: '泸州市', value: '510500' },
      { label: '德阳市', value: '510600' }, { label: '绵阳市', value: '510700' },
      { label: '广元市', value: '510800' }, { label: '遂宁市', value: '510900' },
      { label: '内江市', value: '511000' }, { label: '乐山市', value: '511100' },
      { label: '南充市', value: '511300' }, { label: '眉山市', value: '511400' },
      { label: '宜宾市', value: '511500' }, { label: '广安市', value: '511600' },
      { label: '达州市', value: '511700' }, { label: '雅安市', value: '511800' },
      { label: '巴中市', value: '511900' }, { label: '资阳市', value: '512000' },
    ]
  },
  {
    label: '贵州省', value: '520000', children: [
      { label: '贵阳市', value: '520100' }, { label: '六盘水市', value: '520200' },
      { label: '遵义市', value: '520300' }, { label: '安顺市', value: '520400' },
      { label: '毕节市', value: '520500' }, { label: '铜仁市', value: '520600' },
    ]
  },
  {
    label: '云南省', value: '530000', children: [
      { label: '昆明市', value: '530100' }, { label: '曲靖市', value: '530300' },
      { label: '玉溪市', value: '530400' }, { label: '保山市', value: '530500' },
      { label: '昭通市', value: '530600' }, { label: '丽江市', value: '530700' },
      { label: '普洱市', value: '530800' }, { label: '临沧市', value: '530900' },
    ]
  },
  {
    label: '西藏自治区', value: '540000', children: [
      { label: '拉萨市', value: '540100' }, { label: '日喀则市', value: '540200' },
      { label: '昌都市', value: '540300' }, { label: '林芝市', value: '540400' },
      { label: '山南市', value: '540500' }, { label: '那曲市', value: '540600' },
    ]
  },
  {
    label: '陕西省', value: '610000', children: [
      { label: '西安市', value: '610100' }, { label: '铜川市', value: '610200' },
      { label: '宝鸡市', value: '610300' }, { label: '咸阳市', value: '610400' },
      { label: '渭南市', value: '610500' }, { label: '延安市', value: '610600' },
      { label: '汉中市', value: '610700' }, { label: '榆林市', value: '610800' },
      { label: '安康市', value: '610900' }, { label: '商洛市', value: '611000' },
    ]
  },
  {
    label: '甘肃省', value: '620000', children: [
      { label: '兰州市', value: '620100' }, { label: '嘉峪关市', value: '620200' },
      { label: '金昌市', value: '620300' }, { label: '白银市', value: '620400' },
      { label: '天水市', value: '620500' }, { label: '武威市', value: '620600' },
      { label: '张掖市', value: '620700' }, { label: '平凉市', value: '620800' },
      { label: '酒泉市', value: '620900' }, { label: '庆阳市', value: '621000' },
      { label: '定西市', value: '621100' }, { label: '陇南市', value: '621200' },
    ]
  },
  {
    label: '青海省', value: '630000', children: [
      { label: '西宁市', value: '630100' }, { label: '海东市', value: '630200' },
    ]
  },
  {
    label: '宁夏回族自治区', value: '640000', children: [
      { label: '银川市', value: '640100' }, { label: '石嘴山市', value: '640200' },
      { label: '吴忠市', value: '640300' }, { label: '固原市', value: '640400' },
      { label: '中卫市', value: '640500' },
    ]
  },
  {
    label: '新疆维吾尔自治区', value: '650000', children: [
      { label: '乌鲁木齐市', value: '650100' }, { label: '克拉玛依市', value: '650200' },
      { label: '吐鲁番市', value: '650400' }, { label: '哈密市', value: '650500' },
    ]
  },
  { label: '台湾省', value: '710000', children: [] },
  { label: '香港特别行政区', value: '810000', children: [] },
  { label: '澳门特别行政区', value: '820000', children: [] },
]

/** 根据名称或编码查找省份 */
export function findProvince(nameOrCode: string): RegionNode | undefined {
  return provinces.find(p => p.label === nameOrCode || p.value === nameOrCode)
}

/** 在指定省份下根据名称或编码查找城市 */
export function findCity(province: RegionNode, nameOrCode: string): RegionNode | undefined {
  return province.children?.find(c => c.label === nameOrCode || c.value === nameOrCode)
}

/** 在指定城市下根据名称或编码查找区县 */
export function findDistrict(city: RegionNode, nameOrCode: string): RegionNode | undefined {
  return city.children?.find(d => d.label === nameOrCode || d.value === nameOrCode)
}

/**
 * 注意：腾讯地图 Key 严格保留在服务端。
 * - IP 定位 → 后端 /api/v1/users/ip-location-detail
 * - 逆地理编码 → 后端 /api/v1/users/reverse-geocode
 *
 * 前端不再直连 https://apis.map.qq.com，避免：
 * 1. Key 泄露到前端被刷配额 / 被 referer 限制 / CORS；
 * 2. 浏览器 referer 与腾讯控制台白名单不匹配导致的 status=121 等业务异常；
 * 3. 配额耗尽时所有用户受影响（现在由服务端缓存 + 限流统一治理）。
 */

/** IP 定位接口路径（后端代理） */
const BACKEND_IP_LOCATION_PATH = '/api/v1/users/ip-location-detail'

/** 逆地理编码接口路径（后端代理） */
const BACKEND_REVERSE_GEOCODE_PATH = '/api/v1/users/reverse-geocode'

/** 统一接口超时（毫秒） */
const REQUEST_TIMEOUT = 6000

interface BackendReverseGeocodeResult {
  province?: string
  city?: string
  district?: string
  address?: string
  lat?: number
  lng?: number
}

interface BackendIpLocationResult {
  nation?: string
  province?: string
  city?: string
  district?: string
  ip?: string
  lat?: number
  lng?: number
}

export type LocationResult = {
  province: string
  city: string
  district?: string
  nation?: string
  ip?: string
  lat?: number
  lng?: number
  raw?: BackendIpLocationResult | null
}

export type ReverseGeocodeResult = {
  province: string
  city: string
  district: string
  address?: string
  lat?: number
  lng?: number
  raw?: BackendReverseGeocodeResult | null
}

/**
 * 通用请求封装：通过后端代理调用腾讯地图（IP 定位 / 逆地理编码）。
 * - 后端持有 Key，前端无 CORS / referer / 配额问题；
 * - 后端走 Redis 缓存，重复请求不消耗腾讯配额；
 * - 失败时统一返回 null，调用方负责降级或提示。
 */
async function requestBackendMap<T>(
  path: string,
  params: Record<string, string | number | undefined>,
): Promise<T | null> {
  const queryString = Object.entries(params)
    .filter(([, v]) => v !== undefined && v !== null && `${v}` !== '')
    .map(([k, v]) => `${encodeURIComponent(k)}=${encodeURIComponent(String(v))}`)
    .join('&')
  const url = queryString ? `${path}?${queryString}` : path

  const controller = new AbortController()
  const timer = setTimeout(() => controller.abort(), REQUEST_TIMEOUT)

  try {
    // 直接 fetch 走相对路径，开发环境由 Vite /api 代理、生产环境由 nginx /api 代理。
    // 不复用 useApi 的 axios 是因为：regions.ts 是普通工具模块，不在 Vue setup 中。
    const res = await fetch(url, {
      method: 'GET',
      signal: controller.signal,
      headers: { Accept: 'application/json' },
      // 不携带 cookie
      credentials: 'omit',
      // 减少 CDN 缓存
      cache: 'no-store',
    })
    if (!res.ok) {
      console.warn(`[regions] 后端定位接口 HTTP 异常: ${res.status}`)
      return null
    }
    const payload = (await res.json()) as { code: number; message: string; data: T | null }
    if (payload.code !== 0 && payload.code !== 200) {
      console.warn(`[regions] 后端定位接口业务异常: code=${payload.code}, message=${payload.message}`)
      return null
    }
    return (payload.data ?? null) as T | null
  } catch (err: any) {
    if (err?.name === 'AbortError') {
      console.warn('[regions] 后端定位接口请求超时')
    } else {
      console.warn('[regions] 后端定位接口请求失败:', err?.message || err)
    }
    return null
  } finally {
    clearTimeout(timer)
  }
}

/**
 * 通过后端代理获取当前公网 IP 对应的省市区。
 * 后端会自动取请求方 IP（即浏览器公网 IP，无需前端传入），并按 IP 缓存定位结果。
 * - 内网/回环 IP 由后端直接短路为 null。
 * - 失败时返回 null，由调用方降级到手动选择。
 */
export async function getRegionByIP(ip?: string): Promise<LocationResult | null> {
  const result = await requestBackendMap<BackendIpLocationResult>(BACKEND_IP_LOCATION_PATH, {
    // 显式传 ip 是为了开发环境（localhost 走默认会取到 127.0.0.1），
    // 后端会优先使用请求头 X-Forwarded-For / X-Real-IP，这里传参作为兜底。
    ip,
  })
  if (!result) return null
  const province = (result.province || '').trim()
  const city = (result.city || '').trim()
  const district = (result.district || '').trim()
  if (!province && !city) return null
  // 腾讯地图对“省直辖县级行政区”会将 city 留空并把区县放在 district，
  // 此时把 district 提升为 city 以匹配 RegionSelector 期望的省市两级
  const normalizedCity = city || (district && province ? district : '')
  return {
    province,
    city: normalizedCity,
    district: city ? district : '',
    nation: result.nation,
    ip: result.ip,
    lat: result.lat,
    lng: result.lng,
    raw: result,
  }
}

/**
 * 通过后端代理做逆地理编码：根据经纬度获取详细地址与省市区。
 * 后端会按坐标缓存结果（坐标基本不变，缓存 7 天）。
 * 失败返回 null，由调用方决定降级策略（如使用 IP 定位）。
 */
export async function reverseGeocode(lat: number, lng: number): Promise<ReverseGeocodeResult | null> {
  if (!Number.isFinite(lat) || !Number.isFinite(lng)) return null
  const result = await requestBackendMap<BackendReverseGeocodeResult>(BACKEND_REVERSE_GEOCODE_PATH, {
    lat,
    lng,
  })
  if (!result) return null
  const province = (result.province || '').trim()
  const city = (result.city || '').trim()
  const district = (result.district || '').trim()
  if (!province && !city && !district) return null
  // 港澳台/直辖市等场景下腾讯地图 province/city 会带”后缀“，做轻量规范化
  const normProvince = province.replace(/(省|市|自治区|特别行政区|壮族自治区|回族自治区|维吾尔自治区)$/, '')
  const normCity = city.replace(/(市|地区|盟|自治州)$/, '') || normProvince
  return {
    province: normProvince || province,
    city: normCity,
    district,
    address: result.address,
    lat,
    lng,
    raw: result,
  }
}

/**
 * 复合定位：先尝试浏览器 GPS + 逆地理编码（更精确），失败再降级到 IP 定位。
 * - 返回结构兼容 { province, city, district }；
 * - 任何阶段失败都不抛异常，由调用方决定是否提示用户手动选择。
 */
export async function locateUser(opts?: { ip?: string }): Promise<LocationResult | null> {
  // 1) 浏览器 GPS（需 HTTPS/localhost；用户未授权时会 reject）
  if (typeof navigator !== 'undefined' && 'geolocation' in navigator) {
    try {
      const pos = await new Promise<GeolocationPosition>((resolve, reject) => {
        navigator.geolocation.getCurrentPosition(resolve, reject, {
          enableHighAccuracy: true,
          timeout: 8000,
          maximumAge: 120000,
        })
      })
      const { latitude, longitude } = pos.coords
      const geo = await reverseGeocode(latitude, longitude)
      if (geo?.province) {
        return {
          province: geo.province,
          city: geo.city,
          district: geo.district,
          lat: latitude,
          lng: longitude,
        }
      }
    } catch {
      // GPS 不可用/被拒绝/超时，继续走 IP 定位
    }
  }
  // 2) 腾讯地图 IP 定位降级
  return await getRegionByIP(opts?.ip)
}

/**
 * 根据经纬度匹配地区（保留旧接口语义：直接走逆地理编码）。
 * 注意：返回结构与旧实现保持一致，避免破坏既有调用方。
 */
export async function matchRegionByCoord(lat: number, lng: number): Promise<{ province: string; city: string; district: string } | null> {
  try {
    const result = await reverseGeocode(lat, lng)
    if (!result) return null
    return {
      province: result.province,
      city: result.city,
      district: result.district,
    }
  } catch {
    return null
  }
}
