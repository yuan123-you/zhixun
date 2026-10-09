package com.zhixun.service.ai;

import com.zhixun.common.exception.BusinessException;
import com.zhixun.common.result.ErrorCode;
import com.zhixun.vo.AIResponseVO;
import java.util.Locale;

/** Shared success contract: a provider failure or missing final text is not normal business content. */
public final class AiResults {
    private AiResults() {}

    public static AIResponseVO requireFinal(AIResponseVO response) {
        String usage = response == null || response.getUsage() == null ? "" : response.getUsage().trim();
        if (response == null || usage.toLowerCase(Locale.ROOT).startsWith("error:")
                || response.getContent() == null || response.getContent().isBlank()) {
            throw new BusinessException(ErrorCode.SERVICE_UNAVAILABLE, "AI 服务暂时不可用，请稍后再试");
        }
        return response;
    }
}
