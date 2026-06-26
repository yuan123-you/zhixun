package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.dto.profile.ProfileThemeRequest;
import com.zhixun.entity.UserProfileTheme;
import com.zhixun.mapper.UserProfileThemeMapper;
import com.zhixun.service.ProfileThemeService;
import com.zhixun.vo.UserProfileThemeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileThemeServiceImpl implements ProfileThemeService {
    private final UserProfileThemeMapper userProfileThemeMapper;

    @Override
    public UserProfileThemeVO getTheme(Long userId) {
        LambdaQueryWrapper<UserProfileTheme> w = new LambdaQueryWrapper<>();
        w.eq(UserProfileTheme::getUserId, userId);
        UserProfileTheme theme = userProfileThemeMapper.selectOne(w);
        if (theme == null) return defaultTheme();
        UserProfileThemeVO vo = new UserProfileThemeVO();
        vo.setThemeColor(theme.getThemeColor()); vo.setBackgroundImage(theme.getBackgroundImage());
        vo.setBackgroundStyle(theme.getBackgroundStyle()); vo.setFontFamily(theme.getFontFamily());
        vo.setBioBgColor(theme.getBioBgColor()); vo.setCardStyle(theme.getCardStyle());
        return vo;
    }

    @Override @Transactional
    public void saveTheme(Long userId, ProfileThemeRequest request) {
        LambdaQueryWrapper<UserProfileTheme> w = new LambdaQueryWrapper<>();
        w.eq(UserProfileTheme::getUserId, userId);
        UserProfileTheme theme = userProfileThemeMapper.selectOne(w);
        if (theme == null) { theme = new UserProfileTheme(); theme.setUserId(userId); }
        if (request.getThemeColor() != null) theme.setThemeColor(request.getThemeColor());
        if (request.getBackgroundImage() != null) theme.setBackgroundImage(request.getBackgroundImage());
        if (request.getBackgroundStyle() != null) theme.setBackgroundStyle(request.getBackgroundStyle());
        if (request.getFontFamily() != null) theme.setFontFamily(request.getFontFamily());
        if (request.getBioBgColor() != null) theme.setBioBgColor(request.getBioBgColor());
        if (request.getCardStyle() != null) theme.setCardStyle(request.getCardStyle());
        if (theme.getId() == null) {
            userProfileThemeMapper.insert(theme);
        } else {
            userProfileThemeMapper.updateById(theme);
        }
    }

    private UserProfileThemeVO defaultTheme() {
        UserProfileThemeVO vo = new UserProfileThemeVO();
        vo.setThemeColor("#3b82f6"); vo.setBackgroundImage(null);
        vo.setBackgroundStyle("cover"); vo.setFontFamily(null);
        vo.setBioBgColor(null); vo.setCardStyle("default");
        return vo;
    }
}