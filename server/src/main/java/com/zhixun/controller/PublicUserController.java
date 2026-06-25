package com.zhixun.controller;

import com.zhixun.common.result.R;
import com.zhixun.entity.User;
import com.zhixun.mapper.UserMapper;
import com.zhixun.service.RankService;
import com.zhixun.vo.UserVO;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 公开用户接口（C端）
 */
@RestController
@RequestMapping("/v1/users")
@RequiredArgsConstructor
public class PublicUserController {

    private final RankService rankService;
    private final UserMapper userMapper;

    /**
     * 推荐用户（公开）
     */
    @GetMapping("/recommend")
    public R<List<UserVO>> recommend(@RequestParam(defaultValue = "5") Integer limit) {
        return R.ok(rankService.getHotUsers(limit));
    }

    /**
     * 通过UID查找用户（公开）
     */
    @GetMapping("/by-uid")
    public R<UserVO> findByUid(@RequestParam String uid) {
        User user = userMapper.selectByUid(uid);
        if (user == null) {
            return R.fail(404, "用户不存在");
        }
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUid(user.getUid());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setAvatar(user.getAvatar());
        vo.setBio(user.getBio());
        vo.setProvince(user.getProvince());
        vo.setIpLocation(user.getIpLocation());
        vo.setRole(user.getRole() != null ? user.getRole().name() : null);
        vo.setFollowCount(user.getFollowCount());
        vo.setFollowerCount(user.getFollowerCount());
        vo.setArticleCount(user.getArticleCount());
        return R.ok(vo);
    }
}
