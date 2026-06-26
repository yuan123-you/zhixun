package com.zhixun.dto.profile;

import lombok.Data;

@Data
public class ProfileThemeRequest {
    private String themeColor;
    private String backgroundImage;
    private String backgroundStyle;
    private String fontFamily;
    private String bioBgColor;
    private String cardStyle;
}