package com.zhixun.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhixun.config.RedisConfig;
import com.zhixun.service.TencentMapService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * 腾讯地图服务实现
 * <ul>
 *   <li>主路径：腾讯地图 WebService API（Key 在服务端持有）</li>
 *   <li>兜底：ip-api.com（免费，每分钟 45 次）</li>
 *   <li>Redis 缓存：IP → 定位结果、坐标 → 逆地理编码结果（默认 24h）</li>
 * </ul>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TencentMapServiceImpl implements TencentMapService {

    private static final String TENCENT_HOST = "https://apis.map.qq.com";
    private static final String IP_LOCATION_PATH = "/ws/location/v1/ip";
    private static final String GEOCODER_PATH = "/ws/geocoder/v1/";
    private static final int CONNECT_TIMEOUT_MS = 3000;
    private static final int READ_TIMEOUT_MS = 3000;

    /** Redis Key 前缀：IP 定位（结构化） */
    private static final String REDIS_IP_KEY_PREFIX = "map:ip:";
    /** Redis Key 前缀：逆地理编码 */
    private static final String REDIS_GEOCODER_KEY_PREFIX = "map:geocoder:";
    /** Redis Key 前缀：负缓存（IP 已知无法解析时短路） */
    private static final String REDIS_IP_NEG_KEY_PREFIX = "map:ip:null:";

    /** 成功缓存 TTL：2h（带抖动）。IP 定位精度有限，缩短 TTL 以适应用户位置变化） */
    private static final long IP_CACHE_TTL_HOURS = 2L;
    /** 负缓存 TTL：30min（内网 IP、解析失败等） */
    private static final long IP_NEG_CACHE_TTL_MINUTES = 30L;
    /** 逆地理编码缓存 TTL：7 天（坐标基本不变） */
    private static final long GEOCODER_CACHE_TTL_HOURS = 24L * 7L;

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${zhixun.map.tencent.key:}")
    private String tencentMapKey;

    @Value("${zhixun.map.tencent.enabled:true}")
    private boolean tencentMapEnabled;

    // =================== Public API ===================

    @Override
    public String resolveIpLocation(String ip) {
        IpLocationDetail detail = resolveIpLocationDetail(ip);
        if (detail == null) return null;
        String province = detail.getProvince();
        String city = detail.getCity();
        if (province == null || province.isEmpty()) return null;
        if (city == null || city.isEmpty() || city.equals(province)) {
            return province;
        }
        return province + "·" + city;
    }

    @Override
    public IpLocationDetail resolveIpLocationDetail(String ip) {
        if (ip == null || ip.isEmpty()) {
            return null;
        }
        String trimmed = ip.trim();
        // 内网/回环 IP 不走腾讯；优先看负缓存
        if (isLoopbackOrPrivate(trimmed)) {
            IpLocationDetail fallback = resolveByIpApi(trimmed);
            return fallback; // 失败返回 null
        }

        // 1) Redis 缓存命中
        String cacheKey = REDIS_IP_KEY_PREFIX + trimmed;
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, IpLocationDetail.class);
            }
            // 负缓存：30min 内已确认无法解析
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(REDIS_IP_NEG_KEY_PREFIX + trimmed))) {
                return null;
            }
        } catch (Exception e) {
            log.warn("[TencentMap] 读 Redis 缓存异常，继续走腾讯: {}", e.getMessage());
        }

        // 2) 主路径：腾讯地图 IP 定位
        IpLocationDetail detail = resolveByTencentIp(trimmed);
        // 3) 兜底：ip-api
        if (detail == null) {
            detail = resolveByIpApi(trimmed);
        }

        // 4) 写缓存
        try {
            if (detail != null) {
                String json = objectMapper.writeValueAsString(detail);
                long ttl = RedisConfig.jitteredTTLFromHours(IP_CACHE_TTL_HOURS);
                stringRedisTemplate.opsForValue().set(cacheKey, json, ttl, TimeUnit.SECONDS);
            } else {
                long negTtl = RedisConfig.jitteredTTLFromMinutes(IP_NEG_CACHE_TTL_MINUTES);
                stringRedisTemplate.opsForValue().set(REDIS_IP_NEG_KEY_PREFIX + trimmed, "1", negTtl, TimeUnit.SECONDS);
            }
        } catch (Exception e) {
            log.warn("[TencentMap] 写 Redis 缓存异常，不影响本次结果: {}", e.getMessage());
        }

        return detail;
    }

    @Override
    public ReverseGeocodeResult reverseGeocode(double lat, double lng) {
        if (!Double.isFinite(lat) || !Double.isFinite(lng)) {
            return null;
        }
        // 坐标按 5 位小数（约 1.1m 精度）作为缓存 key，避免浮点漂移
        String coordKey = String.format("%.5f,%.5f", lat, lng);
        String cacheKey = REDIS_GEOCODER_KEY_PREFIX + coordKey;

        // 1) 缓存命中
        try {
            String cached = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return objectMapper.readValue(cached, ReverseGeocodeResult.class);
            }
        } catch (Exception e) {
            log.warn("[TencentMap] 读逆地理编码缓存异常，继续走腾讯: {}", e.getMessage());
        }

        ReverseGeocodeResult result = doReverseGeocode(lat, lng);

        // 2) 写缓存
        if (result != null) {
            try {
                String json = objectMapper.writeValueAsString(result);
                long ttl = RedisConfig.jitteredTTLFromHours(GEOCODER_CACHE_TTL_HOURS);
                stringRedisTemplate.opsForValue().set(cacheKey, json, ttl, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.warn("[TencentMap] 写逆地理编码缓存异常，不影响本次结果: {}", e.getMessage());
            }
        }
        return result;
    }

    // =================== Private: 腾讯 IP 定位 ===================

    private IpLocationDetail resolveByTencentIp(String ip) {
        if (!tencentMapEnabled || tencentMapKey == null || tencentMapKey.isEmpty()) {
            return null;
        }
        try {
            String urlStr = TENCENT_HOST + IP_LOCATION_PATH
                    + "?key=" + encode(tencentMapKey)
                    + "&ip=" + encode(ip)
                    + "&output=json";
            HttpURLConnection conn = openConnection(urlStr, "GET");
            if (conn.getResponseCode() == 200) {
                String json = readBody(conn);
                Integer status = extractJsonInt(json, "status");
                if (status == null || status != 0) {
                    String message = extractJsonString(json, "message");
                    log.warn("[TencentMap] IP 定位返回业务错误: status={}, message={}", status, message);
                    return null;
                }
                String province = firstNonBlank(
                        extractNestedJsonString(json, "ad_info", "province"));
                String city = firstNonBlank(
                        extractNestedJsonString(json, "ad_info", "city"));
                String district = firstNonBlank(
                        extractNestedJsonString(json, "ad_info", "district"));
                String nation = firstNonBlank(
                        extractNestedJsonString(json, "ad_info", "nation"));
                if ((province == null || province.isEmpty()) && (city == null || city.isEmpty())) {
                    return null;
                }
                // 腾讯地图对"省直辖县级行政区"可能 city 为空、district 有值，把 district 提升为 city
                String normCity = (city != null && !city.isEmpty()) ? city
                        : ((district != null && !district.isEmpty()) ? district : "");

                IpLocationDetail result = new IpLocationDetail();
                result.setNation(nation);
                result.setProvince(province != null ? province : "");
                result.setCity(normCity);
                result.setDistrict(city != null && !city.isEmpty() ? district : "");
                result.setIp(extractJsonString(json, "ip"));
                // 腾讯在 result.location 下给 lat/lng
                Double lat = extractNestedJsonDouble(json, "location", "lat");
                Double lng = extractNestedJsonDouble(json, "location", "lng");
                result.setLat(lat);
                result.setLng(lng);
                return result;
            }
        } catch (Exception e) {
            log.warn("[TencentMap] IP 定位请求失败: {}", e.getMessage());
        }
        return null;
    }

    /**
     * ip-api.com 兜底（免费但有频率限制，每分钟 45 次）
     */
    private IpLocationDetail resolveByIpApi(String ip) {
        try {
            String urlStr = "http://ip-api.com/json/" + encode(ip) + "?lang=zh-CN&fields=status,country,regionName,city,query,lat,lon";
            HttpURLConnection conn = openConnection(urlStr, "GET");
            if (conn.getResponseCode() == 200) {
                String json = readBody(conn);
                Integer status = extractJsonInt(json, "status");
                if (status == null || status != 1) {
                    return null;
                }
                String province = extractJsonString(json, "regionName");
                String city = extractJsonString(json, "city");
                String country = extractJsonString(json, "country");
                if ((province == null || province.isEmpty()) && (city == null || city.isEmpty())) {
                    return null;
                }
                IpLocationDetail result = new IpLocationDetail();
                result.setNation(country);
                result.setProvince(province != null ? province : "");
                result.setCity(city != null ? city : "");
                result.setDistrict("");
                result.setIp(extractJsonString(json, "query"));
                Double lat = extractJsonDouble(json, "lat");
                Double lng = extractJsonDouble(json, "lon");
                result.setLat(lat);
                result.setLng(lng);
                return result;
            }
        } catch (Exception e) {
            log.warn("[TencentMap] ip-api 兜底失败: {}", e.getMessage());
        }
        return null;
    }

    // =================== Private: 腾讯逆地理编码 ===================

    private ReverseGeocodeResult doReverseGeocode(double lat, double lng) {
        if (!tencentMapEnabled || tencentMapKey == null || tencentMapKey.isEmpty()) {
            return null;
        }
        try {
            String urlStr = TENCENT_HOST + GEOCODER_PATH
                    + "?key=" + encode(tencentMapKey)
                    + "&location=" + encode(String.format("%.6f,%.6f", lat, lng))
                    + "&get_poi=0&output=json";
            HttpURLConnection conn = openConnection(urlStr, "GET");
            if (conn.getResponseCode() == 200) {
                String json = readBody(conn);
                Integer status = extractJsonInt(json, "status");
                if (status == null || status != 0) {
                    log.warn("[TencentMap] 逆地理编码返回业务错误: status={}, message={}",
                            status, extractJsonString(json, "message"));
                    return null;
                }
                String province = firstNonBlank(
                        extractNestedJsonString(json, "address_component", "province"),
                        extractNestedJsonString(json, "ad_info", "province"));
                String city = firstNonBlank(
                        extractNestedJsonString(json, "address_component", "city"),
                        extractNestedJsonString(json, "ad_info", "city"));
                String district = firstNonBlank(
                        extractNestedJsonString(json, "address_component", "district"),
                        extractNestedJsonString(json, "ad_info", "district"));
                if ((province == null || province.isEmpty())
                        && (city == null || city.isEmpty())
                        && (district == null || district.isEmpty())) {
                    return null;
                }
                ReverseGeocodeResult result = new ReverseGeocodeResult();
                result.setLat(lat);
                result.setLng(lng);
                result.setProvince(province);
                result.setCity(city);
                result.setDistrict(district);
                result.setAddress(extractNestedJsonString(json, "formatted_addresses", "recommend"));
                return result;
            }
        } catch (Exception e) {
            log.warn("[TencentMap] 逆地理编码失败: {}", e.getMessage());
        }
        return null;
    }

    // =================== 网络 / IP 判断 ===================

    private boolean isLoopbackOrPrivate(String ip) {
        if (ip == null) return true;
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) return true;
        if (ip.startsWith("10.") || ip.startsWith("192.168.")) return true;
        if (ip.startsWith("172.")) {
            try {
                int second = Integer.parseInt(ip.split("\\.")[1]);
                return second >= 16 && second <= 31;
            } catch (Exception ignored) { }
        }
        if (ip.startsWith("169.254.")) return true; // link-local
        return false;
    }

    private HttpURLConnection openConnection(String urlStr, String method) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Accept", "application/json");
        return conn;
    }

    private String readBody(HttpURLConnection conn) throws Exception {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    private String encode(String s) {
        try {
            return URLEncoder.encode(s, "UTF-8");
        } catch (Exception e) {
            return s;
        }
    }

    // =================== JSON 工具（避免引入额外依赖） ===================

    private String extractJsonString(String json, String key) {
        if (json == null) return null;
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
        if (colonIndex == -1) return null;
        int valueStart = json.indexOf("\"", colonIndex);
        if (valueStart == -1) return null;
        int valueEnd = json.indexOf("\"", valueStart + 1);
        if (valueEnd == -1) return null;
        return json.substring(valueStart + 1, valueEnd);
    }

    private Integer extractJsonInt(String json, String key) {
        if (json == null) return null;
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
        if (colonIndex == -1) return null;
        int start = colonIndex + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) {
            start++;
        }
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) {
            end++;
        }
        if (end == start) return null;
        try {
            return Integer.parseInt(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double extractJsonDouble(String json, String key) {
        if (json == null) return null;
        String searchKey = "\"" + key + "\"";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return null;
        int colonIndex = json.indexOf(":", keyIndex + searchKey.length());
        if (colonIndex == -1) return null;
        int start = colonIndex + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) {
            start++;
        }
        int end = start;
        while (end < json.length()
                && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-'
                || json.charAt(end) == '.' || json.charAt(end) == 'e' || json.charAt(end) == 'E')) {
            end++;
        }
        if (end == start) return null;
        try {
            return Double.parseDouble(json.substring(start, end).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String extractNestedJsonString(String json, String parent, String child) {
        if (json == null) return null;
        String parentKey = "\"" + parent + "\"";
        int pIdx = json.indexOf(parentKey);
        if (pIdx == -1) {
            return extractJsonString(json, child);
        }
        int braceStart = json.indexOf("{", pIdx);
        if (braceStart == -1) {
            return extractJsonString(json, child);
        }
        int depth = 0;
        int braceEnd = -1;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    braceEnd = i;
                    break;
                }
            }
        }
        if (braceEnd == -1) {
            return extractJsonString(json, child);
        }
        String sub = json.substring(braceStart, braceEnd + 1);
        return extractJsonString(sub, child);
    }

    private Double extractNestedJsonDouble(String json, String parent, String child) {
        if (json == null) return null;
        String parentKey = "\"" + parent + "\"";
        int pIdx = json.indexOf(parentKey);
        if (pIdx == -1) {
            return extractJsonDouble(json, child);
        }
        int braceStart = json.indexOf("{", pIdx);
        if (braceStart == -1) {
            return extractJsonDouble(json, child);
        }
        int depth = 0;
        int braceEnd = -1;
        for (int i = braceStart; i < json.length(); i++) {
            char c = json.charAt(i);
            if (c == '{') depth++;
            else if (c == '}') {
                depth--;
                if (depth == 0) {
                    braceEnd = i;
                    break;
                }
            }
        }
        if (braceEnd == -1) {
            return extractJsonDouble(json, child);
        }
        String sub = json.substring(braceStart, braceEnd + 1);
        return extractJsonDouble(sub, child);
    }

    private String firstNonBlank(String... values) {
        for (String v : values) {
            if (v != null && !v.isEmpty()) return v;
        }
        return null;
    }
}
