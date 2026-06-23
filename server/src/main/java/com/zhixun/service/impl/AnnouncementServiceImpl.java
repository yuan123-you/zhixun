package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.entity.Announcement;
import com.zhixun.mapper.AnnouncementMapper;
import com.zhixun.service.AnnouncementService;
import com.zhixun.vo.AnnouncementVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 公告服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AnnouncementServiceImpl implements AnnouncementService {

    private final AnnouncementMapper announcementMapper;

    @Override
    public Long create(String title, String content, Integer type, Integer isTop,
                       LocalDateTime startTime, LocalDateTime endTime, Integer status) {
        Announcement announcement = new Announcement();
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setType(type != null ? type : 1);
        announcement.setIsTop(isTop != null ? isTop : 0);
        announcement.setStartTime(startTime);
        announcement.setEndTime(endTime);
        announcement.setStatus(status != null ? status : 1);
        announcementMapper.insert(announcement);
        return announcement.getId();
    }

    @Override
    public void update(Long id, String title, String content, Integer type, Integer isTop,
                       LocalDateTime startTime, LocalDateTime endTime, Integer status) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        announcement.setTitle(title);
        announcement.setContent(content);
        announcement.setType(type);
        announcement.setIsTop(isTop);
        announcement.setStartTime(startTime);
        announcement.setEndTime(endTime);
        announcement.setStatus(status);
        announcementMapper.updateById(announcement);
    }

    @Override
    public void delete(Long id) {
        Announcement announcement = announcementMapper.selectById(id);
        if (announcement == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "公告不存在");
        }
        announcementMapper.deleteById(id);
    }

    @Override
    public List<AnnouncementVO> adminList() {
        List<Announcement> announcements = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .orderByDesc(Announcement::getIsTop)
                        .orderByDesc(Announcement::getCreatedAt));
        return announcements.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<AnnouncementVO> activeList() {
        LocalDateTime now = LocalDateTime.now();
        List<Announcement> announcements = announcementMapper.selectList(
                new LambdaQueryWrapper<Announcement>()
                        .eq(Announcement::getStatus, 1)
                        .le(Announcement::getStartTime, now)
                        .ge(Announcement::getEndTime, now)
                        .orderByDesc(Announcement::getIsTop)
                        .orderByDesc(Announcement::getCreatedAt));
        return announcements.stream().map(this::toVO).collect(Collectors.toList());
    }

    private AnnouncementVO toVO(Announcement announcement) {
        AnnouncementVO vo = new AnnouncementVO();
        vo.setId(announcement.getId());
        vo.setTitle(announcement.getTitle());
        vo.setContent(announcement.getContent());
        vo.setType(announcement.getType());
        vo.setIsTop(announcement.getIsTop());
        vo.setStartTime(announcement.getStartTime());
        vo.setEndTime(announcement.getEndTime());
        vo.setStatus(announcement.getStatus());
        vo.setCreatedAt(announcement.getCreatedAt());
        vo.setUpdatedAt(announcement.getUpdatedAt());
        return vo;
    }
}
