package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.common.util.JwtUtil;
import com.zhixun.entity.User;
import com.zhixun.enums.RoleEnum;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.OAuthService;
import com.zhixun.vo.LoginUserVO;
import com.zhixun.vo.TokenResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * OAuth 第三方登录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OAuthServiceImpl implements OAuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final StringRedisTemplate stringRedisTemplate;
    private final RestTemplate restTemplate = new RestTemplate();

    /** 刷新令牌 Redis Key 前缀 */
    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    /** 访问令牌过期时间（秒） */
    @Value("${jwt.access-expiration:604800}")
    private Long accessExpiration;

    // ========== 微信配置 ==========
    @Value("${oauth.wechat.app-id:}")
    private String wechatAppId;

    @Value("${oauth.wechat.app-secret:}")
    private String wechatAppSecret;

    @Value("${oauth.wechat.authorize-url:}")
    private String wechatAuthorizeUrl;

    @Value("${oauth.wechat.access-token-url:}")
    private String wechatAccessTokenUrl;

    // ========== QQ 配置 ==========
    @Value("${oauth.qq.app-id:}")
    private String qqAppId;

    @Value("${oauth.qq.app-key:}")
    private String qqAppKey;

    @Value("${oauth.qq.authorize-url:}")
    private String qqAuthorizeUrl;

    @Value("${oauth.qq.access-token-url:}")
    private String qqAccessTokenUrl;

    @Value("${oauth.qq.open-id-url:}")
    private String qqOpenIdUrl;

    @Override
    public String getAuthorizationUrl(String provider, String redirectUri) {
        String encodedRedirectUri = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8);

        if ("wechat".equals(provider)) {
            return wechatAuthorizeUrl
                    + "?appid=" + wechatAppId
                    + "&redirect_uri=" + encodedRedirectUri
                    + "&response_type=code"
                    + "&scope=snsapi_login"
                    + "&state=wechat#wechat_redirect";
        }

        if ("qq".equals(provider)) {
            return qqAuthorizeUrl
                    + "?client_id=" + qqAppId
                    + "&redirect_uri=" + encodedRedirectUri
                    + "&response_type=code"
                    + "&scope=get_user_info"
                    + "&state=qq";
        }

        throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的授权类型: " + provider);
    }

    @Override
    public TokenResponse oauthLogin(String provider, String code) {
        // 1. 用 code 换取 openid
        String openid = getOpenId(provider, code);
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "获取第三方用户标识失败");
        }

        // 2. 根据 openid 查找关联用户
        User user = findUserByOpenId(provider, openid);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND, "该第三方账号未绑定本平台账号，请先绑定");
        }

        // 3. 检查账号状态
        if (user.getStatus() != null && user.getStatus() == User.STATUS_DISABLED) {
            throw new BusinessException(ErrorCode.AUTH_ACCOUNT_DISABLED);
        }

        // 4. 更新最后登录时间
        User updateUser = new User();
        updateUser.setId(user.getId());
        updateUser.setLastLoginAt(java.time.LocalDateTime.now());
        userMapper.updateById(updateUser);

        // 5. 生成双 Token
        return buildTokenResponse(user);
    }

    @Override
    public void bindOAuth(Long userId, String provider, String code) {
        // 1. 用 code 换取 openid
        String openid = getOpenId(provider, code);
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "获取第三方用户标识失败");
        }

        // 2. 检查该 openid 是否已被其他用户绑定
        User existUser = findUserByOpenId(provider, openid);
        if (existUser != null && !existUser.getId().equals(userId)) {
            throw new BusinessException(ErrorCode.CONFLICT, "该第三方账号已被其他用户绑定");
        }

        // 3. 更新用户表的 openid 字段
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);
        if ("wechat".equals(provider)) {
            updateWrapper.set(User::getWechatOpenid, openid);
        } else if ("qq".equals(provider)) {
            updateWrapper.set(User::getQqOpenid, openid);
        }
        userMapper.update(null, updateWrapper);
    }

    @Override
    public void unbindOAuth(Long userId, String provider) {
        // 检查当前用户是否已绑定该第三方账号
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if ("wechat".equals(provider)) {
            if (!StringUtils.hasText(user.getWechatOpenid())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "未绑定微信账号");
            }
        } else if ("qq".equals(provider)) {
            if (!StringUtils.hasText(user.getQqOpenid())) {
                throw new BusinessException(ErrorCode.BAD_REQUEST, "未绑定QQ账号");
            }
        }

        // 清除用户表的 openid 字段
        LambdaUpdateWrapper<User> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.eq(User::getId, userId);
        if ("wechat".equals(provider)) {
            updateWrapper.set(User::getWechatOpenid, null);
        } else if ("qq".equals(provider)) {
            updateWrapper.set(User::getQqOpenid, null);
        }
        userMapper.update(null, updateWrapper);
    }

    /**
     * 用授权码换取 openid
     */
    private String getOpenId(String provider, String code) {
        if ("wechat".equals(provider)) {
            return getWechatOpenId(code);
        }
        if ("qq".equals(provider)) {
            return getQqOpenId(code);
        }
        throw new BusinessException(ErrorCode.BAD_REQUEST, "不支持的授权类型: " + provider);
    }

    /**
     * 微信：用 code 换取 access_token 和 openid
     */
    @SuppressWarnings("unchecked")
    private String getWechatOpenId(String code) {
        String url = wechatAccessTokenUrl
                + "?appid=" + wechatAppId
                + "&secret=" + wechatAppSecret
                + "&code=" + code
                + "&grant_type=authorization_code";

        try {
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null || response.containsKey("errcode")) {
                log.error("微信获取access_token失败: {}", response);
                return null;
            }
            return (String) response.get("openid");
        } catch (Exception e) {
            log.error("微信OAuth请求异常", e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "微信授权登录失败");
        }
    }

    /**
     * QQ：用 code 换取 access_token，再用 access_token 获取 openid
     */
    private String getQqOpenId(String code) {
        // 第一步：用 code 换取 access_token
        String tokenUrl = qqAccessTokenUrl
                + "?grant_type=authorization_code"
                + "&client_id=" + qqAppId
                + "&client_secret=" + qqAppKey
                + "&code=" + code
                + "&redirect_uri="; // QQ需要传入回调地址，但此处code已获取，redirect_uri可为空

        String accessToken;
        try {
            String tokenResponse = restTemplate.getForObject(tokenUrl, String.class);
            if (!StringUtils.hasText(tokenResponse)) {
                log.error("QQ获取access_token失败: 响应为空");
                return null;
            }
            // QQ返回格式：access_token=xxx&expires_in=xxx&refresh_token=xxx
            accessToken = extractParam(tokenResponse, "access_token");
            if (!StringUtils.hasText(accessToken)) {
                log.error("QQ获取access_token失败: {}", tokenResponse);
                return null;
            }
        } catch (Exception e) {
            log.error("QQ获取access_token异常", e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "QQ授权登录失败");
        }

        // 第二步：用 access_token 获取 openid
        try {
            String openIdUrl = qqOpenIdUrl + "?access_token=" + accessToken + "&unionid=1";
            String openIdResponse = restTemplate.getForObject(openIdUrl, String.class);
            if (!StringUtils.hasText(openIdResponse)) {
                log.error("QQ获取openid失败: 响应为空");
                return null;
            }
            // QQ返回格式：callback( {"client_id":"xxx","openid":"xxx"} );
            return extractQqOpenId(openIdResponse);
        } catch (Exception e) {
            log.error("QQ获取openid异常", e);
            throw new BusinessException(ErrorCode.BAD_REQUEST, "QQ授权登录失败");
        }
    }

    /**
     * 从QQ获取access_token的响应中提取参数值
     */
    private String extractParam(String response, String paramName) {
        String[] params = response.split("&");
        for (String param : params) {
            String[] kv = param.split("=", 2);
            if (kv.length == 2 && kv[0].equals(paramName)) {
                return kv[1];
            }
        }
        return null;
    }

    /**
     * 从QQ获取openid的响应中提取openid
     * 响应格式：callback( {"client_id":"xxx","openid":"xxx"} );
     */
    private String extractQqOpenId(String response) {
        Pattern pattern = Pattern.compile("\"openid\"\\s*:\\s*\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(response);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * 根据 openid 查找关联用户
     */
    private User findUserByOpenId(String provider, String openid) {
        LambdaQueryWrapper<User> queryWrapper = new LambdaQueryWrapper<>();
        if ("wechat".equals(provider)) {
            queryWrapper.eq(User::getWechatOpenid, openid);
        } else if ("qq".equals(provider)) {
            queryWrapper.eq(User::getQqOpenid, openid);
        }
        return userMapper.selectOne(queryWrapper);
    }

    /**
     * 构建 Token 响应
     */
    private TokenResponse buildTokenResponse(User user) {
        String accessToken = jwtUtil.generateAccessToken(user.getId(), user.getUsername(),
                user.getRole() != null ? user.getRole().getValue() : User.ROLE_USER);
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getUsername(),
                user.getRole() != null ? user.getRole().getValue() : User.ROLE_USER);

        // 将 RefreshToken 存入 Redis（用于轮转机制）
        stringRedisTemplate.opsForValue().set(
                REFRESH_TOKEN_PREFIX + refreshToken,
                String.valueOf(user.getId()),
                accessExpiration * 7,
                TimeUnit.SECONDS);

        // 构建用户信息
        LoginUserVO userInfo = new LoginUserVO();
        userInfo.setId(user.getId());
        userInfo.setUsername(user.getUsername());
        userInfo.setNickname(user.getNickname());
        userInfo.setAvatar(user.getAvatar());
        userInfo.setRole(user.getRole() != null ? user.getRole().getValue() : User.ROLE_USER);

        // 构建 Token 响应
        TokenResponse response = new TokenResponse();
        response.setAccessToken(accessToken);
        response.setRefreshToken(refreshToken);
        response.setTokenType("Bearer");
        response.setExpiresIn(accessExpiration);
        response.setUserInfo(userInfo);

        return response;
    }
}
