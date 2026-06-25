package com.zhixun.service;

import com.zhixun.common.result.PageResult;
import com.zhixun.dto.user.ProfileUpdateRequest;
import com.zhixun.dto.user.SettingsUpdateRequest;
import com.zhixun.vo.ArticleVO;
import com.zhixun.vo.UserSettingsVO;
import com.zhixun.vo.UserVO;

import java.util.List;
import java.util.Map;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 获取个人资料（脱敏）
     *
     * @param userId 用户ID
     * @return 用户信息视图
     */
    UserVO getProfile(Long userId);

    /**
     * 更新个人资料
     *
     * @param userId  用户ID
     * @param request 资料更新请求
     */
    void updateProfile(Long userId, ProfileUpdateRequest request);

    /**
     * 获取我发布的文章
     *
     * @param userId   用户ID
     * @param status   文章状态（可选）
     * @param page     页码
     * @param pageSize 每页大小
     * @return 文章分页列表
     */
    PageResult<ArticleVO> getUserArticles(Long userId, Integer status, Integer page, Integer pageSize);

    /**
     * 获取我的点赞
     *
     * @param userId   用户ID
     * @param page     页码
     * @param pageSize 每页大小
     * @return 文章分页列表
     */
    PageResult<ArticleVO> getUserLikes(Long userId, Integer page, Integer pageSize);

    /**
     * 获取浏览历史
     *
     * @param userId    用户ID
     * @param startDate 开始日期
     * @param endDate   结束日期
     * @param page      页码
     * @param pageSize  每页大小
     * @return 文章分页列表
     */
    PageResult<ArticleVO> getViewHistory(Long userId, String startDate, String endDate, Integer page, Integer pageSize);

    /**
     * 批量同步浏览历史
     *
     * @param userId  用户ID
     * @param records 浏览记录列表（articleId + viewDuration）
     */
    void batchSyncViewHistory(Long userId, List<Map<String, Object>> records);

    /**
     * 获取全局设置
     *
     * @param userId 用户ID
     * @return 用户设置视图
     */
    UserSettingsVO getSettings(Long userId);

    /**
     * 更新全局设置
     *
     * @param userId  用户ID
     * @param request 设置更新请求
     */
    void updateSettings(Long userId, SettingsUpdateRequest request);
}
