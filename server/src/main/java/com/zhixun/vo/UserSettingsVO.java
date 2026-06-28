package com.zhixun.vo;

import lombok.Data;

import java.util.List;

/**
 * 用户设置视图
 */
@Data
public class UserSettingsVO {

    private Recommend recommend;

    private Notification notification;

    private Privacy privacy;

    private Display display;

    @Data
    public static class Recommend {
        /** 感兴趣的分类ID列表 */
        private List<Long> interestedCategories;

        /** 感兴趣的标签ID列表 */
        private List<Long> interestedTags;

        /** 屏蔽的分类ID列表 */
        private List<Long> blockedCategories;

        /** 屏蔽的标签ID列表 */
        private List<Long> blockedTags;
    }

    @Data
    public static class Notification {
        /** 系统通知：0-关闭，1-开启 */
        private Integer notifySystem;

        /** 互动通知：0-关闭，1-开启 */
        private Integer notifyInteract;

        /** 私信通知：0-关闭，1-开启 */
        private Integer notifyMessage;

        /** 关注通知：0-关闭，1-开启 */
        private Integer notifyFollow;
    }

    @Data
    public static class Privacy {
        /** 显示在线状态：0-关闭，1-开启 */
        private Integer showOnlineStatus;

        /** 私信权限：0-所有人，1-仅关注的人 */
        private Integer messagePermission;

        /** 保存浏览历史：0-关闭，1-开启 */
        private Integer saveViewHistory;

        /** 内容推荐：0-关闭，1-开启 */
        private Integer contentRecommend;

        /** 自动播放视频：0-关闭，1-仅WiFi，2-始终 */
        private Integer autoPlayVideo;

        /** 免打扰开关：0-关闭，1-开启 */
        private Integer quietHoursEnabled;

        /** 免打扰开始时间 */
        private String quietHoursStart;

        /** 免打扰结束时间 */
        private String quietHoursEnd;

        /** 显示阅读量：0-关闭，1-开启 */
        private Integer showViewCount;

        /** 允许被搜索：0-关闭，1-开启 */
        private Integer allowSearch;
    }

    @Data
    public static class Display {
        /** 字体大小：0-小，1-标准，2-大 */
        private Integer fontSize;

        /** 主题：light/dark/auto */
        private String theme;

        /** 语言 */
        private String language;
    }
}
