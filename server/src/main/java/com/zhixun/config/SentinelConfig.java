package com.zhixun.config;

import com.alibaba.csp.sentinel.annotation.aspectj.SentinelResourceAspect;
import com.alibaba.csp.sentinel.slots.block.RuleConstant;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.system.SystemRule;
import com.alibaba.csp.sentinel.slots.system.SystemRuleManager;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.List;

/**
 * Sentinel 限流熔断配置
 */
@Slf4j
@Configuration
public class SentinelConfig {

    /**
     * 注册 Sentinel 注解切面，使 @SentinelResource 注解生效
     */
    @Bean
    public SentinelResourceAspect sentinelResourceAspect() {
        return new SentinelResourceAspect();
    }

    /**
     * 初始化限流、熔断和系统保护规则
     */
    @PostConstruct
    public void initRules() {
        initFlowRules();
        initDegradeRules();
        initSystemRules();
        log.info("Sentinel 限流熔断规则初始化完成");
    }

    /**
     * 限流规则
     */
    private void initFlowRules() {
        List<FlowRule> rules = new ArrayList<>();

        // 登录接口限流：每秒最多5次，防止暴力破解
        FlowRule loginRule = new FlowRule();
        loginRule.setResource("auth-login");
        loginRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        loginRule.setCount(5);
        loginRule.setLimitApp("default");
        rules.add(loginRule);

        // 注册接口限流：每秒最多3次，防止恶意注册
        FlowRule registerRule = new FlowRule();
        registerRule.setResource("auth-register");
        registerRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        registerRule.setCount(3);
        registerRule.setLimitApp("default");
        rules.add(registerRule);

        // 搜索接口限流：每秒最多20次
        FlowRule searchRule = new FlowRule();
        searchRule.setResource("search");
        searchRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        searchRule.setCount(20);
        searchRule.setLimitApp("default");
        rules.add(searchRule);

        // 文章发布限流：每秒最多10次
        FlowRule articleCreateRule = new FlowRule();
        articleCreateRule.setResource("article-create");
        articleCreateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        articleCreateRule.setCount(10);
        articleCreateRule.setLimitApp("default");
        rules.add(articleCreateRule);

        // 评论发布限流：每秒最多10次，防止刷评论
        FlowRule commentCreateRule = new FlowRule();
        commentCreateRule.setResource("comment-create");
        commentCreateRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        commentCreateRule.setCount(10);
        commentCreateRule.setLimitApp("default");
        rules.add(commentCreateRule);

        // 文件上传限流：每秒最多5次
        FlowRule fileUploadRule = new FlowRule();
        fileUploadRule.setResource("file-upload");
        fileUploadRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        fileUploadRule.setCount(5);
        fileUploadRule.setLimitApp("default");
        rules.add(fileUploadRule);

        // 私信发送限流：每秒最多5次，防止骚扰
        FlowRule messageSendRule = new FlowRule();
        messageSendRule.setResource("message-send");
        messageSendRule.setGrade(RuleConstant.FLOW_GRADE_QPS);
        messageSendRule.setCount(5);
        messageSendRule.setLimitApp("default");
        rules.add(messageSendRule);

        FlowRuleManager.loadRules(rules);
        log.info("加载限流规则 {} 条", rules.size());
    }

    /**
     * 熔断降级规则
     */
    private void initDegradeRules() {
        List<DegradeRule> rules = new ArrayList<>();

        // 搜索服务熔断：慢调用比例 > 50%，熔断10秒
        DegradeRule searchDegradeRule = new DegradeRule();
        searchDegradeRule.setResource("search");
        searchDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_RT);
        searchDegradeRule.setCount(1000); // 慢调用RT阈值1秒
        searchDegradeRule.setSlowRatioThreshold(0.5);
        searchDegradeRule.setTimeWindow(10); // 熔断持续10秒
        searchDegradeRule.setMinRequestAmount(5); // 最小请求数
        searchDegradeRule.setStatIntervalMs(60000); // 统计时长60秒
        rules.add(searchDegradeRule);

        // 文章服务熔断：异常比例 > 50%，熔断10秒
        DegradeRule articleDegradeRule = new DegradeRule();
        articleDegradeRule.setResource("article-create");
        articleDegradeRule.setGrade(RuleConstant.DEGRADE_GRADE_EXCEPTION_RATIO);
        articleDegradeRule.setCount(0.5);
        articleDegradeRule.setTimeWindow(10);
        articleDegradeRule.setMinRequestAmount(5);
        articleDegradeRule.setStatIntervalMs(60000);
        rules.add(articleDegradeRule);

        DegradeRuleManager.loadRules(rules);
        log.info("加载熔断规则 {} 条", rules.size());
    }

    /**
     * 系统保护规则
     */
    private void initSystemRules() {
        List<SystemRule> rules = new ArrayList<>();

        // 系统总QPS不超过100
        SystemRule qpsRule = new SystemRule();
        qpsRule.setHighestSystemLoad(100.0);
        rules.add(qpsRule);

        // 系统平均RT不超过500ms
        SystemRule rtRule = new SystemRule();
        rtRule.setAvgRt(500);
        rules.add(rtRule);

        // 系统并发线程数不超过50
        SystemRule threadRule = new SystemRule();
        threadRule.setMaxThread(50);
        rules.add(threadRule);

        SystemRuleManager.loadRules(rules);
        log.info("加载系统保护规则 {} 条", rules.size());
    }
}
