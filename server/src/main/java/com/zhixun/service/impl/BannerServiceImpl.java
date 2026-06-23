package com.zhixun.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.entity.Banner;
import com.zhixun.mapper.BannerMapper;
import com.zhixun.service.BannerService;
import com.zhixun.vo.BannerVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 轮播图服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BannerServiceImpl implements BannerService {

    private final BannerMapper bannerMapper;

    @Override
    public Long create(String title, String imageUrl, String linkUrl, Integer linkType,
                       Integer sortOrder, LocalDateTime startTime, LocalDateTime endTime, Integer status) {
        Banner banner = new Banner();
        banner.setTitle(title);
        banner.setImageUrl(imageUrl);
        banner.setLinkUrl(linkUrl);
        banner.setLinkType(linkType != null ? linkType : 1);
        banner.setSortOrder(sortOrder != null ? sortOrder : 0);
        banner.setStartTime(startTime);
        banner.setEndTime(endTime);
        banner.setStatus(status != null ? status : 1);
        bannerMapper.insert(banner);
        return banner.getId();
    }

    @Override
    public void update(Long id, String title, String imageUrl, String linkUrl, Integer linkType,
                       Integer sortOrder, LocalDateTime startTime, LocalDateTime endTime, Integer status) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "轮播图不存在");
        }
        banner.setTitle(title);
        banner.setImageUrl(imageUrl);
        banner.setLinkUrl(linkUrl);
        banner.setLinkType(linkType);
        banner.setSortOrder(sortOrder);
        banner.setStartTime(startTime);
        banner.setEndTime(endTime);
        banner.setStatus(status);
        bannerMapper.updateById(banner);
    }

    @Override
    public void delete(Long id) {
        Banner banner = bannerMapper.selectById(id);
        if (banner == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "轮播图不存在");
        }
        bannerMapper.deleteById(id);
    }

    @Override
    public List<BannerVO> adminList() {
        List<Banner> banners = bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .orderByAsc(Banner::getSortOrder)
                        .orderByDesc(Banner::getCreatedAt));
        return banners.stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public List<BannerVO> activeList() {
        LocalDateTime now = LocalDateTime.now();
        List<Banner> banners = bannerMapper.selectList(
                new LambdaQueryWrapper<Banner>()
                        .eq(Banner::getStatus, 1)
                        .le(Banner::getStartTime, now)
                        .ge(Banner::getEndTime, now)
                        .orderByAsc(Banner::getSortOrder));
        return banners.stream().map(this::toVO).collect(Collectors.toList());
    }

    private BannerVO toVO(Banner banner) {
        BannerVO vo = new BannerVO();
        vo.setId(banner.getId());
        vo.setTitle(banner.getTitle());
        vo.setImageUrl(banner.getImageUrl());
        vo.setLinkUrl(banner.getLinkUrl());
        vo.setLinkType(banner.getLinkType());
        vo.setSortOrder(banner.getSortOrder());
        vo.setStartTime(banner.getStartTime());
        vo.setEndTime(banner.getEndTime());
        vo.setStatus(banner.getStatus());
        vo.setCreatedAt(banner.getCreatedAt());
        vo.setUpdatedAt(banner.getUpdatedAt());
        return vo;
    }
}
