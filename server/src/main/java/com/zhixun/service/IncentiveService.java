package com.zhixun.service;

import com.zhixun.vo.BadgeVO;
import com.zhixun.vo.CheckInVO;

import java.util.List;

public interface IncentiveService {
    CheckInVO checkIn(Long userId);
    CheckInVO getCheckInStatus(Long userId);
    void addExperience(Long userId, String action);
    List<BadgeVO> getAllBadges(Long userId);
    List<BadgeVO> getUserBadges(Long userId);
    void checkAndAwardBadges(Long userId);
    void onArticlePublished(Long userId);
    void onArticleLiked(Long userId);
    void onCommentCreated(Long userId);
    void onArticleCollected(Long userId);
}