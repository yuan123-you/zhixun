package com.zhixun.security;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.util.SecurityUtil;
import jakarta.servlet.http.HttpServletResponse;

/** Never advertise user-dependent content as safe for a shared HTTP/CDN cache. */
public final class PersonalizedCacheControl {
    private PersonalizedCacheControl() {}

    public static void apply(HttpServletResponse response, SecurityUtil security, int publicMaxAge) {
        Long viewer = null;
        try { viewer = security.getCurrentUserId(); }
        catch (BusinessException ignored) { /* Anonymous requests have no authenticated user. */ }
        response.setHeader("Cache-Control", viewer == null ? "public, max-age=" + publicMaxAge : "private, no-store");
        response.addHeader("Vary", "Authorization");
        response.addHeader("Vary", "Cookie");
    }
}
