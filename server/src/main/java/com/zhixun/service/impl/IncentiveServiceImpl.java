package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.entity.*;
import com.zhixun.mapper.*;
import com.zhixun.service.IncentiveService;
import com.zhixun.vo.BadgeVO;
import com.zhixun.vo.CheckInVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IncentiveServiceImpl implements IncentiveService {
    private final UserCheckInMapper userCheckInMapper;
    private final UserExperienceMapper userExperienceMapper;
    private final BadgeMapper badgeMapper;
    private final UserBadgeMapper userBadgeMapper;

    private static final int BASE_CHECKIN_POINTS = 5;
    private static final int DAILY_CHECKIN_BONUS = 2;

    @Override @Transactional
    public CheckInVO checkIn(Long userId) {
        LocalDate today = LocalDate.now();
        UserCheckIn existing = userCheckInMapper.selectByUserIdAndDate(userId, today);
        if (existing != null) return getCheckInStatus(userId);

        UserCheckIn lastCheckIn = getLastCheckIn(userId);
        int consecutiveDays = 1;
        if (lastCheckIn != null && lastCheckIn.getCheckInDate().equals(today.minusDays(1))) {
            consecutiveDays = lastCheckIn.getConsecutiveDays() + 1;
        }
        int points = BASE_CHECKIN_POINTS + (consecutiveDays - 1) * DAILY_CHECKIN_BONUS;

        UserCheckIn ci = new UserCheckIn();
        ci.setUserId(userId); ci.setCheckInDate(today);
        ci.setConsecutiveDays(consecutiveDays); ci.setPoints(points);
        userCheckInMapper.insert(ci);
        addExp(userId, 10 + consecutiveDays * 2);
        checkAndAwardBadges(userId);

        CheckInVO vo = new CheckInVO();
        vo.setHasCheckedIn(true); vo.setConsecutiveDays(consecutiveDays);
        vo.setTodayPoints(points);
        UserExperience ue = getOrCreateExperience(userId);
        vo.setTotalExp(ue.getExp()); vo.setLevel(ue.getLevel()); vo.setLevelName(ue.getLevelName());
        return vo;
    }

    @Override
    public CheckInVO getCheckInStatus(Long userId) {
        LocalDate today = LocalDate.now();
        UserCheckIn existing = userCheckInMapper.selectByUserIdAndDate(userId, today);
        UserCheckIn lastCheckIn = getLastCheckIn(userId);
        int consecutiveDays = 0;
        if (existing != null) {
            consecutiveDays = existing.getConsecutiveDays();
        } else if (lastCheckIn != null && lastCheckIn.getCheckInDate().equals(today.minusDays(1))) {
            consecutiveDays = lastCheckIn.getConsecutiveDays();
        }
        CheckInVO vo = new CheckInVO();
        vo.setHasCheckedIn(existing != null); vo.setConsecutiveDays(consecutiveDays);
        vo.setTodayPoints(existing != null ? existing.getPoints() : 0);
        UserExperience ue = getOrCreateExperience(userId);
        vo.setTotalExp(ue.getExp()); vo.setLevel(ue.getLevel()); vo.setLevelName(ue.getLevelName());
        return vo;
    }

    @Override @Transactional
    public void addExperience(Long userId, String action) {
        int exp = getActionExp(action);
        addExp(userId, exp);
    }

    @Override
    public List<BadgeVO> getAllBadges(Long userId) {
        List<Badge> allBadges = badgeMapper.selectList(null);
        List<UserBadge> userBadges = userBadgeMapper.selectByUserId(userId);
        List<Long> ownedIds = userBadges.stream().map(ub -> ub.getBadgeId()).collect(Collectors.toList());
        return allBadges.stream().map(b -> toBadgeVO(b, ownedIds.contains(b.getId()))).collect(Collectors.toList());
    }

    @Override
    public List<BadgeVO> getUserBadges(Long userId) {
        List<UserBadge> userBadges = userBadgeMapper.selectByUserId(userId);
        if (userBadges.isEmpty()) return new ArrayList<>();
        return userBadges.stream().map(ub -> {
            BadgeVO vo = new BadgeVO();
            vo.setId(ub.getBadgeId()); vo.setName(ub.getBadgeName());
            vo.setDescription(ub.getBadgeDescription()); vo.setIcon(ub.getBadgeIcon());
            vo.setCategory(ub.getBadgeCategory()); vo.setIsOwned(true);
            vo.setEarnedAt(ub.getEarnedAt()); return vo;
        }).collect(Collectors.toList());
    }

    @Override @Transactional
    public void checkAndAwardBadges(Long userId) {
        UserExperience ue = getOrCreateExperience(userId);
        int consecutiveDays = getCheckInStatus(userId).getConsecutiveDays();
        LambdaQueryWrapper<UserBadge> uw = new LambdaQueryWrapper<>();
        uw.eq(UserBadge::getUserId, userId);
        List<UserBadge> owned = userBadgeMapper.selectList(uw);
        List<Long> ownedIds = owned.stream().map(ub -> ub.getBadgeId()).collect(Collectors.toList());

        if (!ownedIds.contains(1L)) awardBadge(userId, 1L); // 棣栨壒鐢ㄦ埛
        if (consecutiveDays >= 7 && !ownedIds.contains(2L)) awardBadge(userId, 2L); // 杩炵画绛惧埌7澶?
        if (ue.getLevel() >= 5 && !ownedIds.contains(6L)) awardBadge(userId, 6L); // 绛夌骇寰界珷
    }

    @Override public void onArticlePublished(Long userId) { addExperience(userId, "publish"); checkAndAwardBadges(userId); }
    @Override public void onArticleLiked(Long userId) { addExperience(userId, "like_received"); }
    @Override public void onCommentCreated(Long userId) { addExperience(userId, "comment"); checkAndAwardBadges(userId); }
    @Override public void onArticleCollected(Long userId) { addExperience(userId, "collect_received"); }

    private void addExp(Long userId, int exp) {
        UserExperience ue = getOrCreateExperience(userId);
        ue.setExp(ue.getExp() + exp);
        int newLevel = calculateLevel(ue.getExp());
        if (newLevel != ue.getLevel()) {
            ue.setLevel(newLevel);
            ue.setLevelName(getLevelName(newLevel));
            ue.setNextLevelExp(calculateNextLevelExp(newLevel));
        }
        userExperienceMapper.updateById(ue);
    }

    private int calculateLevel(long exp) {
        if (exp < 100) return 1;
        if (exp < 300) return 2;
        if (exp < 600) return 3;
        if (exp < 1000) return 4;
        if (exp < 2000) return 5;
        if (exp < 4000) return 6;
        if (exp < 7000) return 7;
        if (exp < 12000) return 8;
        if (exp < 20000) return 9;
        return 10;
    }

    private String getLevelName(int level) {
        String[] names = {"鍒濈骇鐢ㄦ埛","杩涢樁鐢ㄦ埛","涓骇鐢ㄦ埛","璧勬繁鐢ㄦ埛","楂樼骇鐢ㄦ埛","杈句汉","涓撳","澶у笀","瀹楀笀","浼犺"};
        return level >= 1 && level <= 10 ? names[level - 1] : "鍒濈骇鐢ㄦ埛";
    }

    private long calculateNextLevelExp(int level) {
        long[] exps = {100,300,600,1000,2000,4000,7000,12000,20000,50000};
        return level < 10 ? exps[level] : -1;
    }

    private int getActionExp(String action) {
        switch (action) {
            case "publish": return 30;
            case "comment": return 5;
            case "like_received": return 2;
            case "collect_received": return 5;
            case "share": return 3;
            case "checkin": return 10;
            default: return 1;
        }
    }

    private UserCheckIn getLastCheckIn(Long userId) {
        LambdaQueryWrapper<UserCheckIn> w = new LambdaQueryWrapper<>();
        w.eq(UserCheckIn::getUserId, userId).orderByDesc(UserCheckIn::getCheckInDate).last("LIMIT 1");
        return userCheckInMapper.selectOne(w);
    }

    private UserExperience getOrCreateExperience(Long userId) {
        LambdaQueryWrapper<UserExperience> w = new LambdaQueryWrapper<>();
        w.eq(UserExperience::getUserId, userId);
        UserExperience ue = userExperienceMapper.selectOne(w);
        if (ue == null) {
            ue = new UserExperience();
            ue.setUserId(userId); ue.setExp(0L); ue.setLevel(1);
            ue.setLevelName("鍒濈骇鐢ㄦ埛"); ue.setNextLevelExp(100L);
            userExperienceMapper.insert(ue);
        }
        return ue;
    }

    private void awardBadge(Long userId, Long badgeId) {
        UserBadge ub = new UserBadge();
        ub.setUserId(userId); ub.setBadgeId(badgeId);
        userBadgeMapper.insert(ub);
    }

    private BadgeVO toBadgeVO(Badge b, boolean owned) {
        BadgeVO vo = new BadgeVO();
        vo.setId(b.getId()); vo.setName(b.getName()); vo.setDescription(b.getDescription());
        vo.setIcon(b.getIcon()); vo.setCategory(b.getCategory()); vo.setIsOwned(owned);
        return vo;
    }
}