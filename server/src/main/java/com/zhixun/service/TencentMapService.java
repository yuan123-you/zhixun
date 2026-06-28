package com.zhixun.service;

import lombok.Data;

/**
 * 腾讯地图服务：IP 定位 / 逆地理编码
 *
 * <p>设计目标：
 * <ul>
 *   <li>Key 仅在服务端持有，避免暴露给前端；</li>
 *   <li>结构化返回 province / city / district / lat / lng，方便前端 RegionSelector 联动；</li>
 *   <li>Redis 缓存 IP → 定位结果、坐标 → 逆地理编码结果，减少对腾讯地图的调用次数；</li>
 *   <li>失败兜底：ip-api.com（公共免费接口）。</li>
 * </ul>
 *
 * <p>配置：
 * <ul>
 *   <li>zhixun.map.tencent.key = TENCENT_MAP_KEY</li>
 *   <li>zhixun.map.tencent.enabled = true</li>
 * </ul>
 */
public interface TencentMapService {

    /**
     * 解析 IP 对应的属地，返回 "省·市" 格式（如 "广东·深圳"）；
     * 解析失败或为内网/回环 IP 返回 null。
     *
     * <p>保留旧语义：仅返回省·市字符串，主要用于"展示 IP 属地"场景。
     */
    String resolveIpLocation(String ip);

    /**
     * 结构化 IP 定位：返回省 / 市 / 区 / 经纬度 / 原始 IP 字符串。
     *
     * <p>结果会按 IP 在 Redis 缓存（默认 24h，TTL 抖动），相同 IP 重复请求不消耗腾讯配额。
     */
    IpLocationDetail resolveIpLocationDetail(String ip);

    /**
     * 逆地理编码：根据经纬度获取详细地址。
     */
    ReverseGeocodeResult reverseGeocode(double lat, double lng);

    /**
     * IP 定位结构化结果
     */
    @Data
    class IpLocationDetail {
        private String nation;
        private String province;
        private String city;
        private String district;
        private String ip;
        private Double lat;
        private Double lng;
        /** 原始 ad_info，调试用 */
        private String raw;
    }

    /**
     * 逆地理编码结果
     */
    @Data
    class ReverseGeocodeResult {
        private String province;
        private String city;
        private String district;
        private String address;
        private Double lat;
        private Double lng;
    }
}
