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
    value: '500000', label: '重庆市', children: [
      { value: '500100', label: '重庆市', children: [
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
  // ========== 北京市 (11) ==========
  { code: '110100', name: '北京市', lat: 39.9042, lng: 116.4074, provinceCode: '110000', provinceName: '北京市' },
  // ========== 天津市 (12) ==========
  { code: '120100', name: '天津市', lat: 39.1252, lng: 117.1907, provinceCode: '120000', provinceName: '天津市' },
  // ========== 河北省 (13) ==========
  { code: '130100', name: '石家庄市', lat: 38.0423, lng: 114.5149, provinceCode: '130000', provinceName: '河北省' },
  { code: '130200', name: '唐山市', lat: 39.6305, lng: 118.1802, provinceCode: '130000', provinceName: '河北省' },
  { code: '130300', name: '秦皇岛市', lat: 39.9355, lng: 119.5996, provinceCode: '130000', provinceName: '河北省' },
  { code: '130400', name: '邯郸市', lat: 36.6255, lng: 114.5391, provinceCode: '130000', provinceName: '河北省' },
  { code: '130500', name: '邢台市', lat: 37.0706, lng: 114.5048, provinceCode: '130000', provinceName: '河北省' },
  { code: '130600', name: '保定市', lat: 38.8739, lng: 115.4646, provinceCode: '130000', provinceName: '河北省' },
  { code: '130700', name: '张家口市', lat: 40.8244, lng: 114.8875, provinceCode: '130000', provinceName: '河北省' },
  { code: '130800', name: '承德市', lat: 40.9518, lng: 117.9634, provinceCode: '130000', provinceName: '河北省' },
  { code: '130900', name: '沧州市', lat: 38.3045, lng: 116.8388, provinceCode: '130000', provinceName: '河北省' },
  { code: '131000', name: '廊坊市', lat: 39.5378, lng: 116.6838, provinceCode: '130000', provinceName: '河北省' },
  { code: '131100', name: '衡水市', lat: 37.7389, lng: 115.6702, provinceCode: '130000', provinceName: '河北省' },
  // ========== 山西省 (14) ==========
  { code: '140100', name: '太原市', lat: 37.8706, lng: 112.5489, provinceCode: '140000', provinceName: '山西省' },
  { code: '140200', name: '大同市', lat: 40.0768, lng: 113.3000, provinceCode: '140000', provinceName: '山西省' },
  { code: '140300', name: '阳泉市', lat: 37.8567, lng: 113.5805, provinceCode: '140000', provinceName: '山西省' },
  { code: '140400', name: '长治市', lat: 36.1954, lng: 113.1165, provinceCode: '140000', provinceName: '山西省' },
  { code: '140500', name: '晋城市', lat: 35.4907, lng: 112.8511, provinceCode: '140000', provinceName: '山西省' },
  { code: '140600', name: '朔州市', lat: 39.3316, lng: 112.4329, provinceCode: '140000', provinceName: '山西省' },
  { code: '140700', name: '晋中市', lat: 37.6870, lng: 112.7525, provinceCode: '140000', provinceName: '山西省' },
  { code: '140800', name: '运城市', lat: 35.0264, lng: 111.0075, provinceCode: '140000', provinceName: '山西省' },
  { code: '140900', name: '忻州市', lat: 38.4167, lng: 112.7342, provinceCode: '140000', provinceName: '山西省' },
  { code: '141000', name: '临汾市', lat: 36.0880, lng: 111.5190, provinceCode: '140000', provinceName: '山西省' },
  { code: '141100', name: '吕梁市', lat: 37.5193, lng: 111.1447, provinceCode: '140000', provinceName: '山西省' },
  // ========== 内蒙古自治区 (15) ==========
  { code: '150100', name: '呼和浩特市', lat: 40.8415, lng: 111.7512, provinceCode: '150000', provinceName: '内蒙古自治区' },
  { code: '150200', name: '包头市', lat: 40.6578, lng: 109.8404, provinceCode: '150000', provinceName: '内蒙古自治区' },
  { code: '150300', name: '乌海市', lat: 39.6538, lng: 106.7955, provinceCode: '150000', provinceName: '内蒙古自治区' },
  { code: '150400', name: '赤峰市', lat: 42.2586, lng: 118.8889, provinceCode: '150000', provinceName: '内蒙古自治区' },
  { code: '150500', name: '通辽市', lat: 43.6529, lng: 122.2447, provinceCode: '150000', provinceName: '内蒙古自治区' },
  { code: '150600', name: '鄂尔多斯市', lat: 39.6085, lng: 109.7813, provinceCode: '150000', provinceName: '内蒙古自治区' },
  { code: '150700', name: '呼伦贝尔市', lat: 49.2116, lng: 119.7658, provinceCode: '150000', provinceName: '内蒙古自治区' },
  { code: '150800', name: '巴彦淖尔市', lat: 40.7432, lng: 107.3877, provinceCode: '150000', provinceName: '内蒙古自治区' },
  { code: '150900', name: '乌兰察布市', lat: 40.9944, lng: 113.1326, provinceCode: '150000', provinceName: '内蒙古自治区' },
  // ========== 辽宁省 (21) ==========
  { code: '210100', name: '沈阳市', lat: 41.8057, lng: 123.4315, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '210200', name: '大连市', lat: 38.9140, lng: 121.6147, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '210300', name: '鞍山市', lat: 41.1078, lng: 122.9946, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '210400', name: '抚顺市', lat: 41.8809, lng: 123.9572, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '210500', name: '本溪市', lat: 41.2941, lng: 123.7665, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '210600', name: '丹东市', lat: 40.0005, lng: 124.3547, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '210700', name: '锦州市', lat: 41.0951, lng: 121.1270, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '210800', name: '营口市', lat: 40.6668, lng: 122.2352, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '210900', name: '阜新市', lat: 42.0217, lng: 121.6701, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '211000', name: '辽阳市', lat: 41.2681, lng: 123.2370, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '211100', name: '盘锦市', lat: 41.1200, lng: 122.0708, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '211200', name: '铁岭市', lat: 42.2862, lng: 123.8424, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '211300', name: '朝阳市', lat: 41.5735, lng: 120.4508, provinceCode: '210000', provinceName: '辽宁省' },
  { code: '211400', name: '葫芦岛市', lat: 40.7110, lng: 120.8370, provinceCode: '210000', provinceName: '辽宁省' },
  // ========== 吉林省 (22) ==========
  { code: '220100', name: '长春市', lat: 43.8965, lng: 125.3258, provinceCode: '220000', provinceName: '吉林省' },
  { code: '220200', name: '吉林市', lat: 43.8378, lng: 126.5494, provinceCode: '220000', provinceName: '吉林省' },
  { code: '220300', name: '四平市', lat: 43.1665, lng: 124.3504, provinceCode: '220000', provinceName: '吉林省' },
  { code: '220400', name: '辽源市', lat: 42.8881, lng: 125.1437, provinceCode: '220000', provinceName: '吉林省' },
  { code: '220500', name: '通化市', lat: 41.7283, lng: 125.9399, provinceCode: '220000', provinceName: '吉林省' },
  { code: '220600', name: '白山市', lat: 41.9408, lng: 126.4144, provinceCode: '220000', provinceName: '吉林省' },
  { code: '220700', name: '松原市', lat: 45.1411, lng: 124.8251, provinceCode: '220000', provinceName: '吉林省' },
  { code: '220800', name: '白城市', lat: 45.6196, lng: 122.8387, provinceCode: '220000', provinceName: '吉林省' },
  // ========== 黑龙江省 (23) ==========
  { code: '230100', name: '哈尔滨市', lat: 45.8038, lng: 126.5350, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '230200', name: '齐齐哈尔市', lat: 47.3543, lng: 123.9182, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '230300', name: '鸡西市', lat: 45.2951, lng: 130.9693, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '230400', name: '鹤岗市', lat: 47.3499, lng: 130.2980, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '230500', name: '双鸭山市', lat: 46.6466, lng: 131.1591, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '230600', name: '大庆市', lat: 46.5876, lng: 125.1031, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '230700', name: '伊春市', lat: 47.7275, lng: 128.8405, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '230800', name: '佳木斯市', lat: 46.7998, lng: 130.3189, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '230900', name: '七台河市', lat: 45.7713, lng: 131.0031, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '231000', name: '牡丹江市', lat: 44.5517, lng: 129.6324, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '231100', name: '黑河市', lat: 50.2452, lng: 127.5285, provinceCode: '230000', provinceName: '黑龙江省' },
  { code: '231200', name: '绥化市', lat: 46.6538, lng: 126.9693, provinceCode: '230000', provinceName: '黑龙江省' },
  // ========== 上海市 (31) ==========
  { code: '310100', name: '上海市', lat: 31.2304, lng: 121.4737, provinceCode: '310000', provinceName: '上海市' },
  // ========== 江苏省 (32) ==========
  { code: '320100', name: '南京市', lat: 32.0603, lng: 118.7969, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320200', name: '无锡市', lat: 31.4910, lng: 120.3119, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320300', name: '徐州市', lat: 34.2044, lng: 117.2858, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320400', name: '常州市', lat: 31.8110, lng: 119.9741, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320500', name: '苏州市', lat: 31.2990, lng: 120.5853, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320600', name: '南通市', lat: 31.9796, lng: 120.8937, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320700', name: '连云港市', lat: 34.5967, lng: 119.2229, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320800', name: '淮安市', lat: 33.6102, lng: 119.0153, provinceCode: '320000', provinceName: '江苏省' },
  { code: '320900', name: '盐城市', lat: 33.3495, lng: 120.1616, provinceCode: '320000', provinceName: '江苏省' },
  { code: '321000', name: '扬州市', lat: 32.3942, lng: 119.4129, provinceCode: '320000', provinceName: '江苏省' },
  { code: '321100', name: '镇江市', lat: 32.1896, lng: 119.4250, provinceCode: '320000', provinceName: '江苏省' },
  { code: '321200', name: '泰州市', lat: 32.4555, lng: 119.9255, provinceCode: '320000', provinceName: '江苏省' },
  { code: '321300', name: '宿迁市', lat: 33.9619, lng: 118.2755, provinceCode: '320000', provinceName: '江苏省' },
  // ========== 浙江省 (33) ==========
  { code: '330100', name: '杭州市', lat: 30.2741, lng: 120.1551, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330200', name: '宁波市', lat: 29.8735, lng: 121.5435, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330300', name: '温州市', lat: 27.9939, lng: 120.6994, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330400', name: '嘉兴市', lat: 30.7460, lng: 120.7556, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330500', name: '湖州市', lat: 30.8930, lng: 120.0880, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330600', name: '绍兴市', lat: 30.0303, lng: 120.5802, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330700', name: '金华市', lat: 29.0781, lng: 119.6476, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330800', name: '衢州市', lat: 28.9359, lng: 118.8595, provinceCode: '330000', provinceName: '浙江省' },
  { code: '330900', name: '舟山市', lat: 29.9853, lng: 122.2078, provinceCode: '330000', provinceName: '浙江省' },
  { code: '331000', name: '台州市', lat: 28.6557, lng: 121.4208, provinceCode: '330000', provinceName: '浙江省' },
  { code: '331100', name: '丽水市', lat: 28.4676, lng: 119.9229, provinceCode: '330000', provinceName: '浙江省' },
  // ========== 安徽省 (34) ==========
  { code: '340100', name: '合肥市', lat: 31.8206, lng: 117.2272, provinceCode: '340000', provinceName: '安徽省' },
  { code: '340200', name: '芜湖市', lat: 31.3527, lng: 118.4331, provinceCode: '340000', provinceName: '安徽省' },
  { code: '340300', name: '蚌埠市', lat: 32.9163, lng: 117.3893, provinceCode: '340000', provinceName: '安徽省' },
  { code: '340400', name: '淮南市', lat: 32.6255, lng: 116.9998, provinceCode: '340000', provinceName: '安徽省' },
  { code: '340500', name: '马鞍山市', lat: 31.6705, lng: 118.5061, provinceCode: '340000', provinceName: '安徽省' },
  { code: '340600', name: '淮北市', lat: 33.9548, lng: 116.7983, provinceCode: '340000', provinceName: '安徽省' },
  { code: '340700', name: '铜陵市', lat: 30.9447, lng: 117.8115, provinceCode: '340000', provinceName: '安徽省' },
  { code: '340800', name: '安庆市', lat: 30.5429, lng: 117.0636, provinceCode: '340000', provinceName: '安徽省' },
  { code: '341000', name: '黄山市', lat: 29.7152, lng: 118.3387, provinceCode: '340000', provinceName: '安徽省' },
  { code: '341100', name: '滁州市', lat: 32.3018, lng: 118.3168, provinceCode: '340000', provinceName: '安徽省' },
  { code: '341200', name: '阜阳市', lat: 32.8896, lng: 115.8145, provinceCode: '340000', provinceName: '安徽省' },
  { code: '341300', name: '宿州市', lat: 33.6477, lng: 116.9642, provinceCode: '340000', provinceName: '安徽省' },
  { code: '341500', name: '六安市', lat: 31.7349, lng: 116.5219, provinceCode: '340000', provinceName: '安徽省' },
  { code: '341600', name: '亳州市', lat: 33.8446, lng: 115.7790, provinceCode: '340000', provinceName: '安徽省' },
  { code: '341700', name: '池州市', lat: 30.6647, lng: 117.4914, provinceCode: '340000', provinceName: '安徽省' },
  { code: '341800', name: '宣城市', lat: 30.9407, lng: 118.7588, provinceCode: '340000', provinceName: '安徽省' },
  // ========== 福建省 (35) ==========
  { code: '350100', name: '福州市', lat: 26.0745, lng: 119.2965, provinceCode: '350000', provinceName: '福建省' },
  { code: '350200', name: '厦门市', lat: 24.4798, lng: 118.0894, provinceCode: '350000', provinceName: '福建省' },
  { code: '350300', name: '莆田市', lat: 25.4540, lng: 119.0077, provinceCode: '350000', provinceName: '福建省' },
  { code: '350400', name: '三明市', lat: 26.2634, lng: 117.6392, provinceCode: '350000', provinceName: '福建省' },
  { code: '350500', name: '泉州市', lat: 24.8741, lng: 118.6759, provinceCode: '350000', provinceName: '福建省' },
  { code: '350600', name: '漳州市', lat: 24.5135, lng: 117.6473, provinceCode: '350000', provinceName: '福建省' },
  { code: '350700', name: '南平市', lat: 26.6415, lng: 118.1777, provinceCode: '350000', provinceName: '福建省' },
  { code: '350800', name: '龙岩市', lat: 25.0751, lng: 117.0172, provinceCode: '350000', provinceName: '福建省' },
  { code: '350900', name: '宁德市', lat: 26.6656, lng: 119.5479, provinceCode: '350000', provinceName: '福建省' },
  // ========== 江西省 (36) ==========
  { code: '360100', name: '南昌市', lat: 28.6820, lng: 115.8579, provinceCode: '360000', provinceName: '江西省' },
  { code: '360200', name: '景德镇市', lat: 29.2688, lng: 117.1784, provinceCode: '360000', provinceName: '江西省' },
  { code: '360300', name: '萍乡市', lat: 27.6228, lng: 113.8546, provinceCode: '360000', provinceName: '江西省' },
  { code: '360400', name: '九江市', lat: 29.7051, lng: 116.0015, provinceCode: '360000', provinceName: '江西省' },
  { code: '360500', name: '新余市', lat: 27.8178, lng: 114.9171, provinceCode: '360000', provinceName: '江西省' },
  { code: '360600', name: '鹰潭市', lat: 28.2602, lng: 117.0692, provinceCode: '360000', provinceName: '江西省' },
  { code: '360700', name: '赣州市', lat: 25.8310, lng: 114.9346, provinceCode: '360000', provinceName: '江西省' },
  { code: '360800', name: '吉安市', lat: 27.1138, lng: 114.9929, provinceCode: '360000', provinceName: '江西省' },
  { code: '360900', name: '宜春市', lat: 27.8144, lng: 114.4168, provinceCode: '360000', provinceName: '江西省' },
  { code: '361000', name: '抚州市', lat: 27.9490, lng: 116.3581, provinceCode: '360000', provinceName: '江西省' },
  { code: '361100', name: '上饶市', lat: 28.4549, lng: 117.9436, provinceCode: '360000', provinceName: '江西省' },
  // ========== 山东省 (37) ==========
  { code: '370100', name: '济南市', lat: 36.6512, lng: 116.9972, provinceCode: '370000', provinceName: '山东省' },
  { code: '370200', name: '青岛市', lat: 36.0671, lng: 120.3826, provinceCode: '370000', provinceName: '山东省' },
  { code: '370300', name: '淄博市', lat: 36.8131, lng: 118.0548, provinceCode: '370000', provinceName: '山东省' },
  { code: '370400', name: '枣庄市', lat: 34.8109, lng: 117.3220, provinceCode: '370000', provinceName: '山东省' },
  { code: '370500', name: '东营市', lat: 37.4337, lng: 118.6747, provinceCode: '370000', provinceName: '山东省' },
  { code: '370600', name: '烟台市', lat: 37.4635, lng: 121.4479, provinceCode: '370000', provinceName: '山东省' },
  { code: '370700', name: '潍坊市', lat: 36.7069, lng: 119.1618, provinceCode: '370000', provinceName: '山东省' },
  { code: '370800', name: '济宁市', lat: 35.4146, lng: 116.5872, provinceCode: '370000', provinceName: '山东省' },
  { code: '370900', name: '泰安市', lat: 36.2000, lng: 117.0876, provinceCode: '370000', provinceName: '山东省' },
  { code: '371000', name: '威海市', lat: 37.5135, lng: 122.1204, provinceCode: '370000', provinceName: '山东省' },
  { code: '371100', name: '日照市', lat: 35.4164, lng: 119.5272, provinceCode: '370000', provinceName: '山东省' },
  { code: '371300', name: '临沂市', lat: 35.1047, lng: 118.3564, provinceCode: '370000', provinceName: '山东省' },
  { code: '371400', name: '德州市', lat: 37.4356, lng: 116.3593, provinceCode: '370000', provinceName: '山东省' },
  { code: '371500', name: '聊城市', lat: 36.4570, lng: 115.9855, provinceCode: '370000', provinceName: '山东省' },
  { code: '371600', name: '滨州市', lat: 37.3820, lng: 117.9728, provinceCode: '370000', provinceName: '山东省' },
  { code: '371700', name: '菏泽市', lat: 35.2336, lng: 115.4806, provinceCode: '370000', provinceName: '山东省' },
  // ========== 河南省 (41) ==========
  { code: '410100', name: '郑州市', lat: 34.7466, lng: 113.6253, provinceCode: '410000', provinceName: '河南省' },
  { code: '410200', name: '开封市', lat: 34.7973, lng: 114.3073, provinceCode: '410000', provinceName: '河南省' },
  { code: '410300', name: '洛阳市', lat: 34.6181, lng: 112.4539, provinceCode: '410000', provinceName: '河南省' },
  { code: '410400', name: '平顶山市', lat: 33.7662, lng: 113.1924, provinceCode: '410000', provinceName: '河南省' },
  { code: '410500', name: '安阳市', lat: 36.0977, lng: 114.3931, provinceCode: '410000', provinceName: '河南省' },
  { code: '410600', name: '鹤壁市', lat: 35.7470, lng: 114.2974, provinceCode: '410000', provinceName: '河南省' },
  { code: '410700', name: '新乡市', lat: 35.3030, lng: 113.9268, provinceCode: '410000', provinceName: '河南省' },
  { code: '410800', name: '焦作市', lat: 35.2156, lng: 113.2418, provinceCode: '410000', provinceName: '河南省' },
  { code: '410900', name: '濮阳市', lat: 35.7618, lng: 115.0293, provinceCode: '410000', provinceName: '河南省' },
  { code: '411000', name: '许昌市', lat: 34.0355, lng: 113.8524, provinceCode: '410000', provinceName: '河南省' },
  { code: '411100', name: '漯河市', lat: 33.5814, lng: 114.0165, provinceCode: '410000', provinceName: '河南省' },
  { code: '411200', name: '三门峡市', lat: 34.7726, lng: 111.2003, provinceCode: '410000', provinceName: '河南省' },
  { code: '411300', name: '南阳市', lat: 32.9908, lng: 112.5285, provinceCode: '410000', provinceName: '河南省' },
  { code: '411400', name: '商丘市', lat: 34.4143, lng: 115.6563, provinceCode: '410000', provinceName: '河南省' },
  { code: '411500', name: '信阳市', lat: 32.1473, lng: 114.0910, provinceCode: '410000', provinceName: '河南省' },
  { code: '411600', name: '周口市', lat: 33.6258, lng: 114.6968, provinceCode: '410000', provinceName: '河南省' },
  { code: '411700', name: '驻马店市', lat: 33.0114, lng: 114.0223, provinceCode: '410000', provinceName: '河南省' },
  // ========== 湖北省 (42) ==========
  { code: '420100', name: '武汉市', lat: 30.5928, lng: 114.3052, provinceCode: '420000', provinceName: '湖北省' },
  { code: '420200', name: '黄石市', lat: 30.1995, lng: 115.0389, provinceCode: '420000', provinceName: '湖北省' },
  { code: '420300', name: '十堰市', lat: 32.6475, lng: 110.7980, provinceCode: '420000', provinceName: '湖北省' },
  { code: '420500', name: '宜昌市', lat: 30.6919, lng: 111.2865, provinceCode: '420000', provinceName: '湖北省' },
  { code: '420600', name: '襄阳市', lat: 32.0090, lng: 112.1224, provinceCode: '420000', provinceName: '湖北省' },
  { code: '420700', name: '鄂州市', lat: 30.3908, lng: 114.8906, provinceCode: '420000', provinceName: '湖北省' },
  { code: '420800', name: '荆门市', lat: 31.0354, lng: 112.1993, provinceCode: '420000', provinceName: '湖北省' },
  { code: '420900', name: '孝感市', lat: 30.9248, lng: 113.9165, provinceCode: '420000', provinceName: '湖北省' },
  { code: '421000', name: '荆州市', lat: 30.3352, lng: 112.2407, provinceCode: '420000', provinceName: '湖北省' },
  { code: '421100', name: '黄冈市', lat: 30.4536, lng: 114.8722, provinceCode: '420000', provinceName: '湖北省' },
  { code: '421200', name: '咸宁市', lat: 29.8412, lng: 114.3225, provinceCode: '420000', provinceName: '湖北省' },
  { code: '421300', name: '随州市', lat: 31.6902, lng: 113.3826, provinceCode: '420000', provinceName: '湖北省' },
  { code: '422800', name: '恩施土家族苗族自治州', lat: 30.2831, lng: 109.4867, provinceCode: '420000', provinceName: '湖北省' },
  // ========== 湖南省 (43) ==========
  { code: '430100', name: '长沙市', lat: 28.2282, lng: 112.9388, provinceCode: '430000', provinceName: '湖南省' },
  { code: '430200', name: '株洲市', lat: 27.8274, lng: 113.1340, provinceCode: '430000', provinceName: '湖南省' },
  { code: '430300', name: '湘潭市', lat: 27.8297, lng: 112.9441, provinceCode: '430000', provinceName: '湖南省' },
  { code: '430400', name: '衡阳市', lat: 26.8932, lng: 112.5720, provinceCode: '430000', provinceName: '湖南省' },
  { code: '430500', name: '邵阳市', lat: 27.2389, lng: 111.4677, provinceCode: '430000', provinceName: '湖南省' },
  { code: '430600', name: '岳阳市', lat: 29.3573, lng: 113.1292, provinceCode: '430000', provinceName: '湖南省' },
  { code: '430700', name: '常德市', lat: 29.0316, lng: 111.6985, provinceCode: '430000', provinceName: '湖南省' },
  { code: '430800', name: '张家界市', lat: 29.1170, lng: 110.4784, provinceCode: '430000', provinceName: '湖南省' },
  { code: '430900', name: '益阳市', lat: 28.5539, lng: 112.3552, provinceCode: '430000', provinceName: '湖南省' },
  { code: '431000', name: '郴州市', lat: 25.7706, lng: 113.0148, provinceCode: '430000', provinceName: '湖南省' },
  { code: '431100', name: '永州市', lat: 26.4203, lng: 111.6134, provinceCode: '430000', provinceName: '湖南省' },
  { code: '431200', name: '怀化市', lat: 27.5695, lng: 110.0016, provinceCode: '430000', provinceName: '湖南省' },
  { code: '431300', name: '娄底市', lat: 27.7001, lng: 111.9946, provinceCode: '430000', provinceName: '湖南省' },
  // ========== 广东省 (44) ==========
  { code: '440100', name: '广州市', lat: 23.1292, lng: 113.2644, provinceCode: '440000', provinceName: '广东省' },
  { code: '440200', name: '韶关市', lat: 24.8104, lng: 113.5972, provinceCode: '440000', provinceName: '广东省' },
  { code: '440300', name: '深圳市', lat: 22.5429, lng: 114.0596, provinceCode: '440000', provinceName: '广东省' },
  { code: '440400', name: '珠海市', lat: 22.2707, lng: 113.5767, provinceCode: '440000', provinceName: '广东省' },
  { code: '440500', name: '汕头市', lat: 23.3535, lng: 116.6820, provinceCode: '440000', provinceName: '广东省' },
  { code: '440600', name: '佛山市', lat: 23.0219, lng: 113.1219, provinceCode: '440000', provinceName: '广东省' },
  { code: '440700', name: '江门市', lat: 22.5787, lng: 113.0816, provinceCode: '440000', provinceName: '广东省' },
  { code: '440800', name: '湛江市', lat: 21.2713, lng: 110.3589, provinceCode: '440000', provinceName: '广东省' },
  { code: '440900', name: '茂名市', lat: 21.6630, lng: 110.9254, provinceCode: '440000', provinceName: '广东省' },
  { code: '441200', name: '肇庆市', lat: 23.0469, lng: 112.4651, provinceCode: '440000', provinceName: '广东省' },
  { code: '441300', name: '惠州市', lat: 23.1107, lng: 114.4168, provinceCode: '440000', provinceName: '广东省' },
  { code: '441400', name: '梅州市', lat: 24.2886, lng: 116.1222, provinceCode: '440000', provinceName: '广东省' },
  { code: '441500', name: '汕尾市', lat: 22.7853, lng: 115.3754, provinceCode: '440000', provinceName: '广东省' },
  { code: '441600', name: '河源市', lat: 23.7437, lng: 114.7007, provinceCode: '440000', provinceName: '广东省' },
  { code: '441700', name: '阳江市', lat: 21.8583, lng: 111.9826, provinceCode: '440000', provinceName: '广东省' },
  { code: '441800', name: '清远市', lat: 23.6820, lng: 113.0561, provinceCode: '440000', provinceName: '广东省' },
  { code: '441900', name: '东莞市', lat: 23.0208, lng: 113.7518, provinceCode: '440000', provinceName: '广东省' },
  { code: '442000', name: '中山市', lat: 22.5160, lng: 113.3926, provinceCode: '440000', provinceName: '广东省' },
  { code: '445100', name: '潮州市', lat: 23.6567, lng: 116.6226, provinceCode: '440000', provinceName: '广东省' },
  { code: '445200', name: '揭阳市', lat: 23.5497, lng: 116.3727, provinceCode: '440000', provinceName: '广东省' },
  { code: '445300', name: '云浮市', lat: 22.9152, lng: 112.0445, provinceCode: '440000', provinceName: '广东省' },
  // ========== 广西壮族自治区 (45) ==========
  { code: '450100', name: '南宁市', lat: 22.8170, lng: 108.3666, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '450200', name: '柳州市', lat: 24.3264, lng: 109.4286, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '450300', name: '桂林市', lat: 25.2736, lng: 110.2900, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '450400', name: '梧州市', lat: 23.4769, lng: 111.2792, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '450500', name: '北海市', lat: 21.4733, lng: 109.1202, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '450600', name: '防城港市', lat: 21.6871, lng: 108.3547, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '450700', name: '钦州市', lat: 21.9797, lng: 108.6543, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '450800', name: '贵港市', lat: 23.1131, lng: 109.5976, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '450900', name: '玉林市', lat: 22.6364, lng: 110.1810, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '451000', name: '百色市', lat: 23.9022, lng: 106.6184, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '451100', name: '贺州市', lat: 24.4038, lng: 111.5669, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '451200', name: '河池市', lat: 24.6931, lng: 108.0854, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '451300', name: '来宾市', lat: 23.7502, lng: 109.2214, provinceCode: '450000', provinceName: '广西壮族自治区' },
  { code: '451400', name: '崇左市', lat: 22.3789, lng: 107.3649, provinceCode: '450000', provinceName: '广西壮族自治区' },
  // ========== 海南省 (46) ==========
  { code: '460100', name: '海口市', lat: 20.0440, lng: 110.1999, provinceCode: '460000', provinceName: '海南省' },
  { code: '460200', name: '三亚市', lat: 18.2528, lng: 109.5119, provinceCode: '460000', provinceName: '海南省' },
  { code: '460300', name: '三沙市', lat: 16.8310, lng: 112.3387, provinceCode: '460000', provinceName: '海南省' },
  { code: '460400', name: '儋州市', lat: 19.5209, lng: 109.5807, provinceCode: '460000', provinceName: '海南省' },
  // ========== 重庆市 (50) ==========
  { code: '500100', name: '重庆市', lat: 29.5647, lng: 106.5505, provinceCode: '500000', provinceName: '重庆市' },
  // ========== 四川省 (51) ==========
  { code: '510100', name: '成都市', lat: 30.5728, lng: 104.0668, provinceCode: '510000', provinceName: '四川省' },
  { code: '510300', name: '自贡市', lat: 29.3390, lng: 104.7784, provinceCode: '510000', provinceName: '四川省' },
  { code: '510400', name: '攀枝花市', lat: 26.5823, lng: 101.7186, provinceCode: '510000', provinceName: '四川省' },
  { code: '510500', name: '泸州市', lat: 28.8717, lng: 105.4423, provinceCode: '510000', provinceName: '四川省' },
  { code: '510600', name: '德阳市', lat: 31.1268, lng: 104.3980, provinceCode: '510000', provinceName: '四川省' },
  { code: '510700', name: '绵阳市', lat: 31.4675, lng: 104.6786, provinceCode: '510000', provinceName: '四川省' },
  { code: '510800', name: '广元市', lat: 32.4355, lng: 105.8434, provinceCode: '510000', provinceName: '四川省' },
  { code: '510900', name: '遂宁市', lat: 30.5326, lng: 105.5927, provinceCode: '510000', provinceName: '四川省' },
  { code: '511000', name: '内江市', lat: 29.5802, lng: 105.0584, provinceCode: '510000', provinceName: '四川省' },
  { code: '511100', name: '乐山市', lat: 29.5523, lng: 103.7656, provinceCode: '510000', provinceName: '四川省' },
  { code: '511300', name: '南充市', lat: 30.8378, lng: 106.1107, provinceCode: '510000', provinceName: '四川省' },
  { code: '511400', name: '眉山市', lat: 30.0773, lng: 103.8485, provinceCode: '510000', provinceName: '四川省' },
  { code: '511500', name: '宜宾市', lat: 28.7512, lng: 104.6433, provinceCode: '510000', provinceName: '四川省' },
  { code: '511600', name: '广安市', lat: 30.4560, lng: 106.6331, provinceCode: '510000', provinceName: '四川省' },
  { code: '511700', name: '达州市', lat: 31.2086, lng: 107.4678, provinceCode: '510000', provinceName: '四川省' },
  { code: '511800', name: '雅安市', lat: 29.9805, lng: 103.0133, provinceCode: '510000', provinceName: '四川省' },
  { code: '511900', name: '巴中市', lat: 31.8672, lng: 106.7475, provinceCode: '510000', provinceName: '四川省' },
  { code: '512000', name: '资阳市', lat: 30.1289, lng: 104.6270, provinceCode: '510000', provinceName: '四川省' },
  // ========== 贵州省 (52) ==========
  { code: '520100', name: '贵阳市', lat: 26.6470, lng: 106.6302, provinceCode: '520000', provinceName: '贵州省' },
  { code: '520200', name: '六盘水市', lat: 26.5924, lng: 104.8302, provinceCode: '520000', provinceName: '贵州省' },
  { code: '520300', name: '遵义市', lat: 27.7257, lng: 106.9274, provinceCode: '520000', provinceName: '贵州省' },
  { code: '520400', name: '安顺市', lat: 26.2531, lng: 105.9462, provinceCode: '520000', provinceName: '贵州省' },
  { code: '520500', name: '毕节市', lat: 27.2839, lng: 105.3050, provinceCode: '520000', provinceName: '贵州省' },
  { code: '520600', name: '铜仁市', lat: 27.7315, lng: 109.1896, provinceCode: '520000', provinceName: '贵州省' },
  // ========== 云南省 (53) ==========
  { code: '530100', name: '昆明市', lat: 24.8801, lng: 102.8329, provinceCode: '530000', provinceName: '云南省' },
  { code: '530300', name: '曲靖市', lat: 25.4900, lng: 103.7962, provinceCode: '530000', provinceName: '云南省' },
  { code: '530400', name: '玉溪市', lat: 24.3518, lng: 102.5466, provinceCode: '530000', provinceName: '云南省' },
  { code: '530500', name: '保山市', lat: 25.1120, lng: 99.1618, provinceCode: '530000', provinceName: '云南省' },
  { code: '530600', name: '昭通市', lat: 27.3382, lng: 103.7168, provinceCode: '530000', provinceName: '云南省' },
  { code: '530700', name: '丽江市', lat: 26.8567, lng: 100.2271, provinceCode: '530000', provinceName: '云南省' },
  { code: '530800', name: '普洱市', lat: 22.8252, lng: 100.9665, provinceCode: '530000', provinceName: '云南省' },
  { code: '530900', name: '临沧市', lat: 23.8843, lng: 100.0888, provinceCode: '530000', provinceName: '云南省' },
  // ========== 西藏自治区 (54) ==========
  { code: '540100', name: '拉萨市', lat: 29.6507, lng: 91.1145, provinceCode: '540000', provinceName: '西藏自治区' },
  { code: '540200', name: '日喀则市', lat: 29.2670, lng: 88.8812, provinceCode: '540000', provinceName: '西藏自治区' },
  { code: '540300', name: '昌都市', lat: 31.1407, lng: 97.1720, provinceCode: '540000', provinceName: '西藏自治区' },
  { code: '540400', name: '林芝市', lat: 29.6490, lng: 94.3615, provinceCode: '540000', provinceName: '西藏自治区' },
  { code: '540500', name: '山南市', lat: 29.2371, lng: 91.7731, provinceCode: '540000', provinceName: '西藏自治区' },
  { code: '540600', name: '那曲市', lat: 31.4762, lng: 92.0514, provinceCode: '540000', provinceName: '西藏自治区' },
  // ========== 陕西省 (61) ==========
  { code: '610100', name: '西安市', lat: 34.3416, lng: 108.9402, provinceCode: '610000', provinceName: '陕西省' },
  { code: '610200', name: '铜川市', lat: 34.8967, lng: 108.9451, provinceCode: '610000', provinceName: '陕西省' },
  { code: '610300', name: '宝鸡市', lat: 34.3632, lng: 107.2377, provinceCode: '610000', provinceName: '陕西省' },
  { code: '610400', name: '咸阳市', lat: 34.3293, lng: 108.7093, provinceCode: '610000', provinceName: '陕西省' },
  { code: '610500', name: '渭南市', lat: 34.4996, lng: 109.5102, provinceCode: '610000', provinceName: '陕西省' },
  { code: '610600', name: '延安市', lat: 36.5855, lng: 109.4898, provinceCode: '610000', provinceName: '陕西省' },
  { code: '610700', name: '汉中市', lat: 33.0676, lng: 107.0238, provinceCode: '610000', provinceName: '陕西省' },
  { code: '610800', name: '榆林市', lat: 38.2852, lng: 109.7346, provinceCode: '610000', provinceName: '陕西省' },
  { code: '610900', name: '安康市', lat: 32.6847, lng: 109.0291, provinceCode: '610000', provinceName: '陕西省' },
  { code: '611000', name: '商洛市', lat: 33.8727, lng: 109.9186, provinceCode: '610000', provinceName: '陕西省' },
  // ========== 甘肃省 (62) ==========
  { code: '620100', name: '兰州市', lat: 36.0614, lng: 103.8343, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '620200', name: '嘉峪关市', lat: 39.7720, lng: 98.2901, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '620300', name: '金昌市', lat: 38.5201, lng: 102.1876, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '620400', name: '白银市', lat: 36.5448, lng: 104.1377, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '620500', name: '天水市', lat: 34.5808, lng: 105.7249, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '620600', name: '武威市', lat: 37.9282, lng: 102.6380, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '620700', name: '张掖市', lat: 38.9255, lng: 100.4499, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '620800', name: '平凉市', lat: 35.5430, lng: 106.6653, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '620900', name: '酒泉市', lat: 39.7324, lng: 98.4940, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '621000', name: '庆阳市', lat: 35.7098, lng: 107.6429, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '621100', name: '定西市', lat: 35.5806, lng: 104.5922, provinceCode: '620000', provinceName: '甘肃省' },
  { code: '621200', name: '陇南市', lat: 33.4007, lng: 104.9218, provinceCode: '620000', provinceName: '甘肃省' },
  // ========== 青海省 (63) ==========
  { code: '630100', name: '西宁市', lat: 36.6171, lng: 101.7785, provinceCode: '630000', provinceName: '青海省' },
  { code: '630200', name: '海东市', lat: 36.5029, lng: 102.4017, provinceCode: '630000', provinceName: '青海省' },
  // ========== 宁夏回族自治区 (64) ==========
  { code: '640100', name: '银川市', lat: 38.4872, lng: 106.2309, provinceCode: '640000', provinceName: '宁夏回族自治区' },
  { code: '640200', name: '石嘴山市', lat: 38.9841, lng: 106.3832, provinceCode: '640000', provinceName: '宁夏回族自治区' },
  { code: '640300', name: '吴忠市', lat: 37.9976, lng: 106.1983, provinceCode: '640000', provinceName: '宁夏回族自治区' },
  { code: '640400', name: '固原市', lat: 36.0158, lng: 106.2426, provinceCode: '640000', provinceName: '宁夏回族自治区' },
  { code: '640500', name: '中卫市', lat: 37.4999, lng: 105.1969, provinceCode: '640000', provinceName: '宁夏回族自治区' },
  // ========== 新疆维吾尔自治区 (65) ==========
  { code: '650100', name: '乌鲁木齐市', lat: 43.8256, lng: 87.6168, provinceCode: '650000', provinceName: '新疆维吾尔自治区' },
  { code: '650200', name: '克拉玛依市', lat: 45.5799, lng: 84.8893, provinceCode: '650000', provinceName: '新疆维吾尔自治区' },
  { code: '650400', name: '吐鲁番市', lat: 42.9477, lng: 89.1897, provinceCode: '650000', provinceName: '新疆维吾尔自治区' },
  { code: '650500', name: '哈密市', lat: 42.8184, lng: 93.5150, provinceCode: '650000', provinceName: '新疆维吾尔自治区' },
  // ========== 台湾省 (71) ==========
  { code: '710100', name: '台北市', lat: 25.0330, lng: 121.5654, provinceCode: '710000', provinceName: '台湾省' },
  { code: '710200', name: '高雄市', lat: 22.6273, lng: 120.3014, provinceCode: '710000', provinceName: '台湾省' },
  { code: '710300', name: '台中市', lat: 24.1477, lng: 120.6736, provinceCode: '710000', provinceName: '台湾省' },
  { code: '710400', name: '台南市', lat: 22.9999, lng: 120.2269, provinceCode: '710000', provinceName: '台湾省' },
  // ========== 香港特别行政区 (81) ==========
  { code: '810100', name: '香港', lat: 22.2793, lng: 114.1628, provinceCode: '810000', provinceName: '香港特别行政区' },
  // ========== 澳门特别行政区 (82) ==========
  { code: '820100', name: '澳门', lat: 22.1987, lng: 113.5439, provinceCode: '820000', provinceName: '澳门特别行政区' },
]

/**
 * 使用 Haversine 公式计算两点间球面距离（单位：公里）
 * 适用于中国范围内的精确城市匹配
 */
function haversineDistance(lat1: number, lng1: number, lat2: number, lng2: number): number {
  const R = 6371 // 地球半径（公里）
  const dLat = toRad(lat2 - lat1)
  const dLng = toRad(lng2 - lng1)
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos(toRad(lat1)) * Math.cos(toRad(lat2)) *
    Math.sin(dLng / 2) * Math.sin(dLng / 2)
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
  return R * c
}

function toRad(deg: number): number {
  return (deg * Math.PI) / 180
}

/**
 * 根据经纬度找到最近的城市
 * 使用 Haversine 公式计算球面距离，精度优于简单欧几里得距离
 */
export function findNearestCity(lat: number, lng: number): CityCoordinate | null {
  let nearest: CityCoordinate | null = null
  let minDist = Infinity
  for (const city of cityCoordinates) {
    const dist = haversineDistance(lat, lng, city.lat, city.lng)
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
 * 通过 GPS 坐标反向地理编码获取省/市/区（优先使用免费 Nominatim API）
 */
export async function reverseGeocode(lat: number, lng: number): Promise<{ province: string; city: string; district: string } | null> {
  // 先尝试 Nominatim 反向地理编码（OpenStreetMap，免费）
  try {
    const controller = new AbortController()
    const timeout = setTimeout(() => controller.abort(), 5000)
    const url = `https://nominatim.openstreetmap.org/reverse?lat=${lat}&lon=${lng}&format=json&accept-language=zh&zoom=14`
    const res = await fetch(url, {
      signal: controller.signal,
      headers: { 'User-Agent': 'ZhixunApp/1.0', 'Accept': 'application/json' },
    })
    clearTimeout(timeout)
    if (res.ok) {
      const data = await res.json()
      if (data && data.address) {
        const addr = data.address
        // Nominatim 对中国行政区划的字段映射
        const province = addr.state || addr.province || ''
        const city = addr.city || addr.town || addr.county || addr.state_district || ''
        const district = addr.county || addr.district || addr.suburb || addr.village || ''
        if (province || city) {
          return { province, city, district }
        }
      }
    }
  } catch {
    // Nominatim 失败，继续其他尝试
  }

  // 退回到 IP 定位 + 坐标最近城市匹配
  try {
    const ipRegion = await getRegionByIP()
    if (ipRegion) {
      const coordMatch = matchRegionByCoord(lat, lng)
      return {
        province: ipRegion.province || coordMatch?.province || '',
        city: ipRegion.city || coordMatch?.city || '',
        district: ipRegion.district || '',
      }
    }
  } catch {
    // 忽略
  }

  // 最终退回本地城市坐标匹配
  const coordMatch = matchRegionByCoord(lat, lng)
  return coordMatch ? { ...coordMatch, district: '' } : null
}

/**
 * 通过 IP 地址匹配到省/市/区信息
 * 将调用免费的 IP 定位 API
 */
export async function getRegionByIP(): Promise<{ province: string; city: string; district: string } | null> {
  const endpoints = [
    // 腾讯地图 IP 定位不支持浏览器端 CORS，已移除，改用后端代理方式
    // 备选1: 免费 IP 定位
    'https://ipapi.co/json/',
    // 备选2: 另一个免费服务
    'https://ipinfo.io/json',
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

      if (url.includes('ipapi.co')) {
        // ipapi.co 响应格式
        if (data && data.city) {
          return {
            province: data.region || '',
            city: data.city || '',
            district: '',
          }
        }
      } else if (url.includes('ipinfo.io')) {
        // ipinfo.io 响应格式
        if (data && data.city) {
          return {
            province: data.region || '',
            city: data.city || '',
            district: '',
          }
        }
      }
    } catch {
      // 当前接口失败，尝试下一个
      continue
    }
  }

  return null
}
