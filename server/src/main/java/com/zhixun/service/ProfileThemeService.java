package com.zhixun.service;

import com.zhixun.dto.profile.ProfileThemeRequest;
import com.zhixun.vo.UserProfileThemeVO;

public interface ProfileThemeService {
    UserProfileThemeVO getTheme(Long userId);
    void saveTheme(Long userId, ProfileThemeRequest request);
}