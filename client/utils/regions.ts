/**
 * 中国行政区划数据（省-市-区三级联动）
 * 数据来源：国家统计局行政区划代码
 */
export interface RegionNode {
  value: string
  label: string
  children?: RegionNode[]
}

/** 省份数据 */
export const provinces: RegionNode[] = [
  {
    value: '110000', label: '北京市', children: [{
      value: '110100', label: '北京市', children: [
        { value: '110101', label: '东城区' }, { value: '110102', label: '西城区' },
        { value: '110105', label: '朝阳区' }, { value: '110106', label: '丰台区' },
        { value: '110107', label: '石景山区' }, { value: '110108', label: '海淀区' },
        { value: '110109', label: '门头沟区' }, { value: '110111', label: '房山区' },
        { value: '110112', label: '通州区' }, { value: '110113', label: '顺义区' },
        { value: '110114', label: '昌平区' }, { value: '110115', label: '大兴区' },
        { value: '110116', label: '怀柔区' }, { value: '110117', label: '平谷区' },
        { value: '110118', label: '密云区' }, { value: '110119', label: '延庆区' },
      ]
    }]
  },
  {
    value: '120000', label: '天津市', children: [{
      value: '120100', label: '天津市', children: [
        { value: '120101', label: '和平区' }, { value: '120102', label: '河东区' },
        { value: '120103', label: '河西区' }, { value: '120104', label: '南开区' },
        { value: '120105', label: '河北区' }, { value: '120106', label: '红桥区' },
        { value: '120110', label: '东丽区' }, { value: '120111', label: '西青区' },
        { value: '120112', label: '津南区' }, { value: '120113', label: '北辰区' },
        { value: '120114', label: '武清区' }, { value: '120115', label: '宝坻区' },
        { value: '120116', label: '滨海新区' }, { value: '120117', label: '宁河区' },
        { value: '120118', label: '静海区' }, { value: '120119', label: '蓟州区' },
      ]
    }]
  },
  {
    value: '130000', label: '河北省', children: [
      { value: '130100', label: '石家庄市', children: [
        { value: '130102', label: '长安区' }, { value: '130104', label: '桥西区' },
        { value: '130105', label: '新华区' }, { value: '130107', label: '井陉矿区' },
        { value: '130108', label: '裕华区' }, { value: '130109', label: '藁城区' },
        { value: '130110', label: '鹿泉区' }, { value: '130111', label: '栾城区' },
        { value: '130121', label: '井陉县' }, { value: '130123', label: '正定县' },
        { value: '130125', label: '行唐县' }, { value: '130126', label: '灵寿县' },
        { value: '130127', label: '高邑县' }, { value: '130128', label: '深泽县' },
        { value: '130129', label: '赞皇县' }, { value: '130130', label: '无极县' },
        { value: '130131', label: '平山县' }, { value: '130132', label: '元氏县' },
        { value: '130133', label: '赵县' }, { value: '130181', label: '辛集市' },
        { value: '130183', label: '晋州市' }, { value: '130184', label: '新乐市' },
      ]},
      { value: '130200', label: '唐山市', children: [
        { value: '130202', label: '路南区' }, { value: '130203', label: '路北区' },
        { value: '130204', label: '古冶区' }, { value: '130205', label: '开平区' },
        { value: '130207', label: '丰南区' }, { value: '130208', label: '丰润区' },
        { value: '130209', label: '曹妃甸区' }, { value: '130224', label: '滦南县' },
        { value: '130225', label: '乐亭县' }, { value: '130227', label: '迁西县' },
        { value: '130229', label: '玉田县' }, { value: '130281', label: '遵化市' },
        { value: '130283', label: '迁安市' }, { value: '130284', label: '滦州市' },
      ]},
      { value: '130300', label: '秦皇岛市', children: [
        { value: '130302', label: '海港区' }, { value: '130303', label: '山海关区' },
        { value: '130304', label: '北戴河区' }, { value: '130306', label: '抚宁区' },
        { value: '130321', label: '青龙满族自治县' }, { value: '130322', label: '昌黎县' },
        { value: '130324', label: '卢龙县' },
      ]},
      { value: '130400', label: '邯郸市', children: [
        { value: '130402', label: '邯山区' }, { value: '130403', label: '丛台区' },
        { value: '130404', label: '复兴区' }, { value: '130406', label: '峰峰矿区' },
        { value: '130407', label: '肥乡区' }, { value: '130408', label: '永年区' },
        { value: '130423', label: '临漳县' }, { value: '130424', label: '成安县' },
        { value: '130425', label: '大名县' }, { value: '130426', label: '涉县' },
        { value: '130427', label: '磁县' }, { value: '130430', label: '邱县' },
        { value: '130431', label: '鸡泽县' }, { value: '130432', label: '广平县' },
        { value: '130433', label: '馆陶县' }, { value: '130434', label: '魏县' },
        { value: '130435', label: '曲周县' }, { value: '130481', label: '武安市' },
      ]},
      { value: '130500', label: '邢台市', children: [] },
      { value: '130600', label: '保定市', children: [] },
      { value: '130700', label: '张家口市', children: [] },
      { value: '130800', label: '承德市', children: [] },
      { value: '130900', label: '沧州市', children: [] },
      { value: '131000', label: '廊坊市', children: [] },
      { value: '131100', label: '衡水市', children: [] },
    ]
  },
  {
    value: '140000', label: '山西省', children: [
      { value: '140100', label: '太原市', children: [] },
      { value: '140200', label: '大同市', children: [] },
      { value: '140300', label: '阳泉市', children: [] },
      { value: '140400', label: '长治市', children: [] },
      { value: '140500', label: '晋城市', children: [] },
      { value: '140600', label: '朔州市', children: [] },
      { value: '140700', label: '晋中市', children: [] },
      { value: '140800', label: '运城市', children: [] },
      { value: '140900', label: '忻州市', children: [] },
      { value: '141000', label: '临汾市', children: [] },
      { value: '141100', label: '吕梁市', children: [] },
    ]
  },
  {
    value: '150000', label: '内蒙古自治区', children: [
      { value: '150100', label: '呼和浩特市', children: [] },
      { value: '150200', label: '包头市', children: [] },
      { value: '150300', label: '乌海市', children: [] },
      { value: '150400', label: '赤峰市', children: [] },
      { value: '150500', label: '通辽市', children: [] },
      { value: '150600', label: '鄂尔多斯市', children: [] },
      { value: '150700', label: '呼伦贝尔市', children: [] },
      { value: '150800', label: '巴彦淖尔市', children: [] },
      { value: '150900', label: '乌兰察布市', children: [] },
    ]
  },
  {
    value: '210000', label: '辽宁省', children: [
      { value: '210100', label: '沈阳市', children: [] },
      { value: '210200', label: '大连市', children: [] },
      { value: '210300', label: '鞍山市', children: [] },
      { value: '210400', label: '抚顺市', children: [] },
      { value: '210500', label: '本溪市', children: [] },
      { value: '210600', label: '丹东市', children: [] },
      { value: '210700', label: '锦州市', children: [] },
      { value: '210800', label: '营口市', children: [] },
      { value: '210900', label: '阜新市', children: [] },
      { value: '211000', label: '辽阳市', children: [] },
      { value: '211100', label: '盘锦市', children: [] },
      { value: '211200', label: '铁岭市', children: [] },
      { value: '211300', label: '朝阳市', children: [] },
      { value: '211400', label: '葫芦岛市', children: [] },
    ]
  },
  {
    value: '220000', label: '吉林省', children: [
      { value: '220100', label: '长春市', children: [] },
      { value: '220200', label: '吉林市', children: [] },
      { value: '220300', label: '四平市', children: [] },
      { value: '220400', label: '辽源市', children: [] },
      { value: '220500', label: '通化市', children: [] },
      { value: '220600', label: '白山市', children: [] },
      { value: '220700', label: '松原市', children: [] },
      { value: '220800', label: '白城市', children: [] },
    ]
  },
  {
    value: '230000', label: '黑龙江省', children: [
      { value: '230100', label: '哈尔滨市', children: [] },
      { value: '230200', label: '齐齐哈尔市', children: [] },
      { value: '230300', label: '鸡西市', children: [] },
      { value: '230400', label: '鹤岗市', children: [] },
      { value: '230500', label: '双鸭山市', children: [] },
      { value: '230600', label: '大庆市', children: [] },
      { value: '230700', label: '伊春市', children: [] },
      { value: '230800', label: '佳木斯市', children: [] },
      { value: '230900', label: '七台河市', children: [] },
      { value: '231000', label: '牡丹江市', children: [] },
      { value: '231100', label: '黑河市', children: [] },
      { value: '231200', label: '绥化市', children: [] },
    ]
  },
  {
    value: '310000', label: '上海市', children: [{
      value: '310100', label: '上海市', children: [
        { value: '310101', label: '黄浦区' }, { value: '310104', label: '徐汇区' },
        { value: '310105', label: '长宁区' }, { value: '310106', label: '静安区' },
        { value: '310107', label: '普陀区' }, { value: '310109', label: '虹口区' },
        { value: '310110', label: '杨浦区' }, { value: '310112', label: '闵行区' },
        { value: '310113', label: '宝山区' }, { value: '310114', label: '嘉定区' },
        { value: '310115', label: '浦东新区' }, { value: '310116', label: '金山区' },
        { value: '310117', label: '松江区' }, { value: '310118', label: '青浦区' },
        { value: '310120', label: '奉贤区' }, { value: '310151', label: '崇明区' },
      ]
    }]
  },
  {
    value: '320000', label: '江苏省', children: [
      { value: '320100', label: '南京市', children: [] },
      { value: '320200', label: '无锡市', children: [] },
      { value: '320300', label: '徐州市', children: [] },
      { value: '320400', label: '常州市', children: [] },
      { value: '320500', label: '苏州市', children: [] },
      { value: '320600', label: '南通市', children: [] },
      { value: '320700', label: '连云港市', children: [] },
      { value: '320800', label: '淮安市', children: [] },
      { value: '320900', label: '盐城市', children: [] },
      { value: '321000', label: '扬州市', children: [] },
      { value: '321100', label: '镇江市', children: [] },
      { value: '321200', label: '泰州市', children: [] },
      { value: '321300', label: '宿迁市', children: [] },
    ]
  },
  {
    value: '330000', label: '浙江省', children: [
      { value: '330100', label: '杭州市', children: [] },
      { value: '330200', label: '宁波市', children: [] },
      { value: '330300', label: '温州市', children: [] },
      { value: '330400', label: '嘉兴市', children: [] },
      { value: '330500', label: '湖州市', children: [] },
      { value: '330600', label: '绍兴市', children: [] },
      { value: '330700', label: '金华市', children: [] },
      { value: '330800', label: '衢州市', children: [] },
      { value: '330900', label: '舟山市', children: [] },
      { value: '331000', label: '台州市', children: [] },
      { value: '331100', label: '丽水市', children: [] },
    ]
  },
  {
    value: '340000', label: '安徽省', children: [
      { value: '340100', label: '合肥市', children: [] },
      { value: '340200', label: '芜湖市', children: [] },
      { value: '340300', label: '蚌埠市', children: [] },
      { value: '340400', label: '淮南市', children: [] },
      { value: '340500', label: '马鞍山市', children: [] },
      { value: '340600', label: '淮北市', children: [] },
      { value: '340700', label: '铜陵市', children: [] },
      { value: '340800', label: '安庆市', children: [] },
      { value: '341000', label: '黄山市', children: [] },
      { value: '341100', label: '滁州市', children: [] },
      { value: '341200', label: '阜阳市', children: [] },
      { value: '341300', label: '宿州市', children: [] },
      { value: '341500', label: '六安市', children: [] },
      { value: '341600', label: '亳州市', children: [] },
      { value: '341700', label: '池州市', children: [] },
      { value: '341800', label: '宣城市', children: [] },
    ]
  },
  {
    value: '350000', label: '福建省', children: [
      { value: '350100', label: '福州市', children: [] },
      { value: '350200', label: '厦门市', children: [] },
      { value: '350300', label: '莆田市', children: [] },
      { value: '350400', label: '三明市', children: [] },
      { value: '350500', label: '泉州市', children: [] },
      { value: '350600', label: '漳州市', children: [] },
      { value: '350700', label: '南平市', children: [] },
      { value: '350800', label: '龙岩市', children: [] },
      { value: '350900', label: '宁德市', children: [] },
    ]
  },
  {
    value: '360000', label: '江西省', children: [
      { value: '360100', label: '南昌市', children: [] },
      { value: '360200', label: '景德镇市', children: [] },
      { value: '360300', label: '萍乡市', children: [] },
      { value: '360400', label: '九江市', children: [] },
      { value: '360500', label: '新余市', children: [] },
      { value: '360600', label: '鹰潭市', children: [] },
      { value: '360700', label: '赣州市', children: [] },
      { value: '360800', label: '吉安市', children: [] },
      { value: '360900', label: '宜春市', children: [] },
      { value: '361000', label: '抚州市', children: [] },
      { value: '361100', label: '上饶市', children: [] },
    ]
  },
  {
    value: '370000', label: '山东省', children: [
      { value: '370100', label: '济南市', children: [] },
      { value: '370200', label: '青岛市', children: [] },
      { value: '370300', label: '淄博市', children: [] },
      { value: '370400', label: '枣庄市', children: [] },
      { value: '370500', label: '东营市', children: [] },
      { value: '370600', label: '烟台市', children: [] },
      { value: '370700', label: '潍坊市', children: [] },
      { value: '370800', label: '济宁市', children: [] },
      { value: '370900', label: '泰安市', children: [] },
      { value: '371000', label: '威海市', children: [] },
      { value: '371100', label: '日照市', children: [] },
      { value: '371300', label: '临沂市', children: [] },
      { value: '371400', label: '德州市', children: [] },
      { value: '371500', label: '聊城市', children: [] },
      { value: '371600', label: '滨州市', children: [] },
      { value: '371700', label: '菏泽市', children: [] },
    ]
  },
  {
    value: '410000', label: '河南省', children: [
      { value: '410100', label: '郑州市', children: [] },
      { value: '410200', label: '开封市', children: [] },
      { value: '410300', label: '洛阳市', children: [] },
      { value: '410400', label: '平顶山市', children: [] },
      { value: '410500', label: '安阳市', children: [] },
      { value: '410600', label: '鹤壁市', children: [] },
      { value: '410700', label: '新乡市', children: [] },
      { value: '410800', label: '焦作市', children: [] },
      { value: '410900', label: '濮阳市', children: [] },
      { value: '411000', label: '许昌市', children: [] },
      { value: '411100', label: '漯河市', children: [] },
      { value: '411200', label: '三门峡市', children: [] },
      { value: '411300', label: '南阳市', children: [] },
      { value: '411400', label: '商丘市', children: [] },
      { value: '411500', label: '信阳市', children: [] },
      { value: '411600', label: '周口市', children: [] },
      { value: '411700', label: '驻马店市', children: [] },
    ]
  },
  {
    value: '420000', label: '湖北省', children: [
      { value: '420100', label: '武汉市', children: [
        { value: '420102', label: '江岸区' }, { value: '420103', label: '江汉区' },
        { value: '420104', label: '硚口区' }, { value: '420105', label: '汉阳区' },
        { value: '420106', label: '武昌区' }, { value: '420107', label: '青山区' },
        { value: '420111', label: '洪山区' }, { value: '420112', label: '东西湖区' },
        { value: '420113', label: '汉南区' }, { value: '420114', label: '蔡甸区' },
        { value: '420115', label: '江夏区' }, { value: '420116', label: '黄陂区' },
        { value: '420117', label: '新洲区' },
      ]},
      { value: '420200', label: '黄石市', children: [] },
      { value: '420300', label: '十堰市', children: [] },
      { value: '420500', label: '宜昌市', children: [] },
      { value: '420600', label: '襄阳市', children: [] },
      { value: '420700', label: '鄂州市', children: [] },
      { value: '420800', label: '荆门市', children: [] },
      { value: '420900', label: '孝感市', children: [] },
      { value: '421000', label: '荆州市', children: [] },
      { value: '421100', label: '黄冈市', children: [] },
      { value: '421200', label: '咸宁市', children: [] },
      { value: '421300', label: '随州市', children: [] },
      { value: '422800', label: '恩施土家族苗族自治州', children: [] },
    ]
  },
  {
    value: '430000', label: '湖南省', children: [
      { value: '430100', label: '长沙市', children: [] },
      { value: '430200', label: '株洲市', children: [] },
      { value: '430300', label: '湘潭市', children: [] },
      { value: '430400', label: '衡阳市', children: [] },
      { value: '430500', label: '邵阳市', children: [] },
      { value: '430600', label: '岳阳市', children: [] },
      { value: '430700', label: '常德市', children: [] },
      { value: '430800', label: '张家界市', children: [] },
      { value: '430900', label: '益阳市', children: [] },
      { value: '431000', label: '郴州市', children: [] },
      { value: '431100', label: '永州市', children: [] },
      { value: '431200', label: '怀化市', children: [] },
      { value: '431300', label: '娄底市', children: [] },
    ]
  },
  {
    value: '440000', label: '广东省', children: [
      { value: '440100', label: '广州市', children: [] },
      { value: '440200', label: '韶关市', children: [] },
      { value: '440300', label: '深圳市', children: [] },
      { value: '440400', label: '珠海市', children: [] },
      { value: '440500', label: '汕头市', children: [] },
      { value: '440600', label: '佛山市', children: [] },
      { value: '440700', label: '江门市', children: [] },
      { value: '440800', label: '湛江市', children: [] },
      { value: '440900', label: '茂名市', children: [] },
      { value: '441200', label: '肇庆市', children: [] },
      { value: '441300', label: '惠州市', children: [] },
      { value: '441400', label: '梅州市', children: [] },
      { value: '441500', label: '汕尾市', children: [] },
      { value: '441600', label: '河源市', children: [] },
      { value: '441700', label: '阳江市', children: [] },
      { value: '441800', label: '清远市', children: [] },
      { value: '441900', label: '东莞市', children: [] },
      { value: '442000', label: '中山市', children: [] },
      { value: '445100', label: '潮州市', children: [] },
      { value: '445200', label: '揭阳市', children: [] },
      { value: '445300', label: '云浮市', children: [] },
    ]
  },
  {
    value: '450000', label: '广西壮族自治区', children: [
      { value: '450100', label: '南宁市', children: [] },
      { value: '450200', label: '柳州市', children: [] },
      { value: '450300', label: '桂林市', children: [] },
      { value: '450400', label: '梧州市', children: [] },
      { value: '450500', label: '北海市', children: [] },
      { value: '450600', label: '防城港市', children: [] },
      { value: '450700', label: '钦州市', children: [] },
      { value: '450800', label: '贵港市', children: [] },
      { value: '450900', label: '玉林市', children: [] },
      { value: '451000', label: '百色市', children: [] },
      { value: '451100', label: '贺州市', children: [] },
      { value: '451200', label: '河池市', children: [] },
      { value: '451300', label: '来宾市', children: [] },
      { value: '451400', label: '崇左市', children: [] },
    ]
  },
  {
    value: '460000', label: '海南省', children: [
      { value: '460100', label: '海口市', children: [] },
      { value: '460200', label: '三亚市', children: [] },
      { value: '460300', label: '三沙市', children: [] },
      { value: '460400', label: '儋州市', children: [] },
    ]
  },
  {
    value: '500000', label: '重庆市', children: [{
      value: '500100', label: '重庆市', children: [
        { value: '500101', label: '万州区' }, { value: '500102', label: '涪陵区' },
        { value: '500103', label: '渝中区' }, { value: '500104', label: '大渡口区' },
        { value: '500105', label: '江北区' }, { value: '500106', label: '沙坪坝区' },
        { value: '500107', label: '九龙坡区' }, { value: '500108', label: '南岸区' },
        { value: '500109', label: '北碚区' }, { value: '500110', label: '綦江区' },
        { value: '500111', label: '大足区' }, { value: '500112', label: '渝北区' },
        { value: '500113', label: '巴南区' }, { value: '500114', label: '黔江区' },
        { value: '500115', label: '长寿区' }, { value: '500116', label: '江津区' },
        { value: '500117', label: '合川区' }, { value: '500118', label: '永川区' },
        { value: '500119', label: '南川区' }, { value: '500120', label: '璧山区' },
        { value: '500151', label: '铜梁区' }, { value: '500152', label: '潼南区' },
        { value: '500153', label: '荣昌区' }, { value: '500154', label: '开州区' },
        { value: '500155', label: '梁平区' }, { value: '500156', label: '武隆区' },
      ]}
    ]},
    { value: '500200', label: '郊县', children: [
        { value: '500229', label: '城口县' }, { value: '500230', label: '丰都县' },
        { value: '500231', label: '垫江县' }, { value: '500233', label: '忠县' },
        { value: '500235', label: '云阳县' }, { value: '500236', label: '奉节县' },
        { value: '500237', label: '巫山县' }, { value: '500238', label: '巫溪县' },
        { value: '500240', label: '石柱土家族自治县' }, { value: '500241', label: '秀山土家族苗族自治县' },
        { value: '500242', label: '酉阳土家族苗族自治县' }, { value: '500243', label: '彭水苗族土家族自治县' },
      ]}
    ]
  },
  {
    value: '510000', label: '四川省', children: [
      { value: '510100', label: '成都市', children: [] },
      { value: '510300', label: '自贡市', children: [] },
      { value: '510400', label: '攀枝花市', children: [] },
      { value: '510500', label: '泸州市', children: [] },
      { value: '510600', label: '德阳市', children: [] },
      { value: '510700', label: '绵阳市', children: [] },
      { value: '510800', label: '广元市', children: [] },
      { value: '510900', label: '遂宁市', children: [] },
      { value: '511000', label: '内江市', children: [] },
      { value: '511100', label: '乐山市', children: [] },
      { value: '511300', label: '南充市', children: [] },
      { value: '511400', label: '眉山市', children: [] },
      { value: '511500', label: '宜宾市', children: [] },
      { value: '511600', label: '广安市', children: [] },
      { value: '511700', label: '达州市', children: [] },
      { value: '511800', label: '雅安市', children: [] },
      { value: '511900', label: '巴中市', children: [] },
      { value: '512000', label: '资阳市', children: [] },
    ]
  },
  {
    value: '520000', label: '贵州省', children: [
      { value: '520100', label: '贵阳市', children: [] },
      { value: '520200', label: '六盘水市', children: [] },
      { value: '520300', label: '遵义市', children: [] },
      { value: '520400', label: '安顺市', children: [] },
      { value: '520500', label: '毕节市', children: [] },
      { value: '520600', label: '铜仁市', children: [] },
    ]
  },
  {
    value: '530000', label: '云南省', children: [
      { value: '530100', label: '昆明市', children: [] },
      { value: '530300', label: '曲靖市', children: [] },
      { value: '530400', label: '玉溪市', children: [] },
      { value: '530500', label: '保山市', children: [] },
      { value: '530600', label: '昭通市', children: [] },
      { value: '530700', label: '丽江市', children: [] },
      { value: '530800', label: '普洱市', children: [] },
      { value: '530900', label: '临沧市', children: [] },
    ]
  },
  {
    value: '540000', label: '西藏自治区', children: [
      { value: '540100', label: '拉萨市', children: [] },
      { value: '540200', label: '日喀则市', children: [] },
      { value: '540300', label: '昌都市', children: [] },
      { value: '540400', label: '林芝市', children: [] },
      { value: '540500', label: '山南市', children: [] },
      { value: '540600', label: '那曲市', children: [] },
    ]
  },
  {
    value: '610000', label: '陕西省', children: [
      { value: '610100', label: '西安市', children: [] },
      { value: '610200', label: '铜川市', children: [] },
      { value: '610300', label: '宝鸡市', children: [] },
      { value: '610400', label: '咸阳市', children: [] },
      { value: '610500', label: '渭南市', children: [] },
      { value: '610600', label: '延安市', children: [] },
      { value: '610700', label: '汉中市', children: [] },
      { value: '610800', label: '榆林市', children: [] },
      { value: '610900', label: '安康市', children: [] },
      { value: '611000', label: '商洛市', children: [] },
    ]
  },
  {
    value: '620000', label: '甘肃省', children: [
      { value: '620100', label: '兰州市', children: [] },
      { value: '620200', label: '嘉峪关市', children: [] },
      { value: '620300', label: '金昌市', children: [] },
      { value: '620400', label: '白银市', children: [] },
      { value: '620500', label: '天水市', children: [] },
      { value: '620600', label: '武威市', children: [] },
      { value: '620700', label: '张掖市', children: [] },
      { value: '620800', label: '平凉市', children: [] },
      { value: '620900', label: '酒泉市', children: [] },
      { value: '621000', label: '庆阳市', children: [] },
      { value: '621100', label: '定西市', children: [] },
      { value: '621200', label: '陇南市', children: [] },
    ]
  },
  {
    value: '630000', label: '青海省', children: [
      { value: '630100', label: '西宁市', children: [] },
      { value: '630200', label: '海东市', children: [] },
    ]
  },
  {
    value: '640000', label: '宁夏回族自治区', children: [
      { value: '640100', label: '银川市', children: [] },
      { value: '640200', label: '石嘴山市', children: [] },
      { value: '640300', label: '吴忠市', children: [] },
      { value: '640400', label: '固原市', children: [] },
      { value: '640500', label: '中卫市', children: [] },
    ]
  },
  {
    value: '650000', label: '新疆维吾尔自治区', children: [
      { value: '650100', label: '乌鲁木齐市', children: [] },
      { value: '650200', label: '克拉玛依市', children: [] },
      { value: '650400', label: '吐鲁番市', children: [] },
      { value: '650500', label: '哈密市', children: [] },
    ]
  },
  {
    value: '710000', label: '台湾省', children: [
      { value: '710100', label: '台北市', children: [] },
      { value: '710200', label: '高雄市', children: [] },
      { value: '710300', label: '台中市', children: [] },
      { value: '710400', label: '台南市', children: [] },
    ]
  },
  {
    value: '810000', label: '香港特别行政区', children: [
      { value: '810100', label: '香港岛', children: [
        { value: '810101', label: '中西区' }, { value: '810102', label: '东区' },
        { value: '810103', label: '南区' }, { value: '810104', label: '湾仔区' },
      ]},
      { value: '810200', label: '九龙', children: [
        { value: '810201', label: '油尖旺区' }, { value: '810202', label: '深水埗区' },
        { value: '810203', label: '九龙城区' }, { value: '810204', label: '黄大仙区' },
        { value: '810205', label: '观塘区' },
      ]},
      { value: '810300', label: '新界', children: [
        { value: '810301', label: '荃湾区' }, { value: '810302', label: '屯门区' },
        { value: '810303', label: '元朗区' }, { value: '810304', label: '北区' },
        { value: '810305', label: '大埔区' }, { value: '810306', label: '沙田区' },
        { value: '810307', label: '西贡区' }, { value: '810308', label: '葵青区' },
        { value: '810309', label: '离岛区' },
      ]},
    ]
  },
  {
    value: '820000', label: '澳门特别行政区', children: [
      { value: '820100', label: '澳门半岛', children: [
        { value: '820101', label: '花地玛堂区' }, { value: '820102', label: '圣安多尼堂区' },
        { value: '820103', label: '大堂区' }, { value: '820104', label: '望德堂区' },
        { value: '820105', label: '风顺堂区' },
      ]},
      { value: '820200', label: '离岛', children: [
        { value: '820201', label: '嘉模堂区(氹仔)' }, { value: '820202', label: '圣方济各堂区(路环)' },
      ]},
    ]
  },
]

/**
 * 根据标签匹配省份节点
 * @param label 省份名称
 * @returns 匹配的省份节点，找不到则返回 null
 */
export function findProvince(label: string): RegionNode | null {
  const clean = (s: string) => s.replace(/省|市|自治区|壮族|回族|维吾尔|特别行政区/g, '').trim()
  const needle = clean(label)
  for (const p of provinces) {
    if (clean(p.label) === needle || p.label.includes(needle) || needle.includes(clean(p.label))) {
      return p
    }
  }
  return null
}

/**
 * 在省份节点下根据标签匹配城市节点
 */
export function findCity(province: RegionNode, label: string): RegionNode | null {
  if (!province.children) return null
  const clean = (s: string) => s.replace(/市|自治州|地区|盟/g, '').trim()
  const needle = clean(label)
  for (const c of province.children) {
    if (clean(c.label) === needle || c.label.includes(needle) || needle.includes(clean(c.label))) {
      return c
    }
  }
  return null
}

/**
 * 在城市节点下根据标签匹配区县节点
 */
export function findDistrict(city: RegionNode, label: string): RegionNode | null {
  if (!city.children) return null
  const clean = (s: string) => s.replace(/区|县|自治县|市/g, '').trim()
  const needle = clean(label)
  for (const d of city.children) {
    if (clean(d.label) === needle || d.label.includes(needle) || needle.includes(clean(d.label))) {
      return d
    }
  }
  return null
}

/**
 * 将经纬度坐标匹配到最近的中国城市
 * 使用省会及主要城市坐标进行粗略匹配
 */
interface CityCoordinate {
  code: string
  name: string
  lat: number
  lng: number
  provinceCode: string
  provinceName: string
}

const cityCoordinates: CityCoordinate[] = [
  { code: '110100', name: '北京市', lat: 39.9042, lng: 116.4074, provinceCode: '110000', provinceName: '北京市' },
  { code: '120100', name: '天津市', lat: 39.1252, lng: 117.1907, provinceCode: '120000', provinceName: '天津市' },
  { code: '130100', name: '石家庄市', lat: 38.0423, lng: 114.5149, provinceCode: '130000', provinceName: '河北省' },
  { code: '130200', name: '唐山市', lat: 39.6305, lng: 118.1802, provinceCode: '130000', provinceName: '河北省' },
  { code: '130300', name: '秦皇岛市', lat: 39.9355, lng: 119.5996, provinceCode: '130000', provinceName: '河北省' },
  { code: '130400', name: '邯郸市', lat: 36.6255, lng: 114.5391, provinceCode: '130000', provinceName: '河北省' },
  { code: '140100', name: '太原市', lat: 37.8706, lng: 112.5489, provinceCode: '140000', provinceName: '山西省' },
  { code: '150100', name: '呼和浩特市', lat: 40.8415, lng: 111.7512, provinceCode: '150000', provinceName: '内蒙古自治区' },
  { code: '210100', name: '沈阳市', lat: 41.8057, lng: 123.4315, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '210200', name: '大连市', lat: 38.9140, lng: 121.6147, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '220100', name: '长春市', lat: 43.8965, lng: 125.3258, provinceCode: '220000', provinceName: '吉林省' },
  { code: '230100', name: '哈尔滨市', lat: 45.8038, lng: 126.5350, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '310100', name: '上海市', lat: 31.2304, lng: 121.4737, provinceCode: '310000', provinceName: '上海市' },
  { code: '320100', name: '南京市', lat: 32.0603, lng: 118.7969, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320200', name: '无锡市', lat: 31.4910, lng: 120.3119, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320400', name: '常州市', lat: 31.8110, lng: 119.9741, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320500', name: '苏州市', lat: 31.2990, lng: 120.5853, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320600', name: '南通市', lat: 31.9796, lng: 120.8937, provinceCode: '320000', provinceName: '江苏省' },
  { code: '330100', name: '杭州市', lat: 30.2741, lng: 120.1551, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330200', name: '宁波市', lat: 29.8735, lng: 121.5435, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330300', name: '温州市', lat: 27.9939, lng: 120.6994, provinceCode: '330000', provinceName: '浙江省' },
  { code: '340100', name: '合肥市', lat: 31.8206, lng: 117.2272, provinceCode: '340000', provinceName: '安徽省' },
  { code: '350100', name: '福州市', lat: 26.0745, lng: 119.2965, provinceCode: '350000', provinceName: '福建省' },
  { code: '350200', name: '厦门市', lat: 24.4798, lng: 118.0894, provinceCode: '350000', provinceName: '福建省' },
  { code: '360100', name: '南昌市', lat: 28.6820, lng: 115.8579, provinceCode: '360000', provinceName: '江西省' },
  { code: '370100', name: '济南市', lat: 36.6512, lng: 116.9972, provinceCode: '370000', provinceName: '山东省' },
  { code: '370200', name: '青岛市', lat: 36.0671, lng: 120.3826, provinceCode: '370000', provinceName: '山东省' },
  { code: '410100', name: '郑州市', lat: 34.7466, lng: 113.6253, provinceCode: '410000', provinceName: '河南省' },
  { code: '420100', name: '武汉市', lat: 30.5928, lng: 114.3052, provinceCode: '420000', provinceName: '湖北省' },
  { code: '430100', name: '长沙市', lat: 28.2282, lng: 112.9388, provinceCode: '430000', provinceName: '湖南省' },
  { code: '440100', name: '广州市', lat: 23.1292, lng: 113.2644, provinceCode: '440000', provinceName: '广东省' },
  { code: '440300', name: '深圳市', lat: 22.5429, lng: 114.0596, provinceCode: '440000', provinceName: '广东省' },
  { code: '440400', name: '珠海市', lat: 22.2707, lng: 113.5767, provinceCode: '440000', provinceName: '广东省' },
  { code: '440600', name: '佛山市', lat: 23.0219, lng: 113.1219, provinceCode: '440000', provinceName: '广东省' },
  { code: '441900', name: '东莞市', lat: 23.0208, lng: 113.7518, provinceCode: '440000', provinceName: '广东省' },
  { code: '450100', name: '南宁市', lat: 22.8170, lng: 108.3666, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '460100', name: '海口市', lat: 20.0440, lng: 110.1999, provinceCode: '460000', provinceName: '海南省' },
  { code: '460200', name: '三亚市', lat: 18.2528, lng: 109.5119, provinceCode: '460000', provinceName: '海南省' },
  { code: '500100', name: '重庆市', lat: 29.5647, lng: 106.5505, provinceCode: '500000', provinceName: '重庆市' },
  { code: '510100', name: '成都市', lat: 30.5728, lng: 104.0668, provinceCode: '510000', provinceName: '四川省' },
  { code: '520100', name: '贵阳市', lat: 26.6470, lng: 106.6302, provinceCode: '520000', provinceName: '贵州省' },
  { code: '530100', name: '昆明市', lat: 24.8801, lng: 102.8329, provinceCode: '530000', provinceName: '云南省' },
  { code: '540100', name: '拉萨市', lat: 29.6507, lng: 91.1145, provinceCode: '540000', provinceName: '西藏自治区' },
  { code: '610100', name: '西安市', lat: 34.3416, lng: 108.9402, provinceCode: '610000', provinceName: '陕西省' },
  { code: '620100', name: '兰州市', lat: 36.0614, lng: 103.8343, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '630100', name: '西宁市', lat: 36.6171, lng: 101.7785, provinceCode: '630000', provinceName: '青海省' },
  { code: '640100', name: '银川市', lat: 38.4872, lng: 106.2309, provinceCode: '640000', provinceName: '宁夏回族自治区' },
  { code: '650100', name: '乌鲁木齐市', lat: 43.8256, lng: 87.6168, provinceCode: '650000', provinceName: '新疆维吾尔自治区' },
  { code: '810100', name: '香港', lat: 22.2793, lng: 114.1628, provinceCode: '810000', provinceName: '香港特别行政区' },
  { code: '820100', name: '澳门', lat: 22.1987, lng: 113.5439, provinceCode: '820000', provinceName: '澳门特别行政区' },
]

/**
 * 根据经纬度找到最近的城市
 * 使用简单的欧几里得距离（对于中国范围内精度足够做粗略匹配）
 */
export function findNearestCity(lat: number, lng: number): CityCoordinate | null {
  let nearest: CityCoordinate | null = null
  let minDist = Infinity
  for (const city of cityCoordinates) {
    const dLat = city.lat - lat
    const dLng = city.lng - lng
    const dist = dLat * dLat + dLng * dLng
    if (dist < minDist) {
      minDist = dist
      nearest = city
    }
  }
  return nearest
}

/**
 * 匹配坐标到省/市文本
 */
export function matchRegionByCoord(lat: number, lng: number): { province: string; city: string } | null {
  const nearest = findNearestCity(lat, lng)
  if (!nearest) return null
  return { province: nearest.provinceName, city: nearest.name }
}

/**
 * 通过 IP 地址匹配到省/市信息
 * 将调用免费的 IP 定位 API
 */
export async function getRegionByIP(): Promise<{ province: string; city: string } | null> {
  const endpoints = [
    // 国内优先：腾讯地图 IP 定位（无需 Key 的公开接口）
    'https://apis.map.qq.com/ws/location/v1/ip?output=json',
    // 国外备选
    'https://ipapi.co/json/',
  ]

  for (const url of endpoints) {
    try {
      const controller = new AbortController()
      const timeout = setTimeout(() => controller.abort(), 5000)
      const res = await fetch(url, {
        signal: controller.signal,
        headers: { 'Accept': 'application/json' },
      })
      clearTimeout(timeout)
      if (!res.ok) continue
      const data = await res.json()

      if (url.includes('map.qq.com')) {
        // 腾讯地图 API 响应格式
        if (data.status === 0 && data.result?.ad_info) {
          const ad = data.result.ad_info
          return { province: ad.province || '', city: ad.city || '' }
        }
      } else if (url.includes('ipapi.co')) {
        // ipapi.co 响应格式
        if (data && data.city) {
          return { province: data.region || '', city: data.city || '' }
        }
      }
    } catch {
      // 当前接口失败，尝试下一个
      continue
    }
  }

  return null
}
