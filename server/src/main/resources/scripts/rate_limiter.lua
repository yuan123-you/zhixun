-- ============================================================
-- Redis 令牌桶限流 Lua 脚本
-- 原子操作保证分布式环境下的限流准确性
--
-- KEYS[1]: 令牌桶的 Redis Key（如 rate_limit:user:123）
-- ARGV[1]: 令牌桶最大容量（max_tokens）
-- ARGV[2]: 令牌生成速率（tokens_per_second）
-- ARGV[3]: 当前请求消耗的令牌数（requested_tokens）
-- ARGV[4]: 当前时间戳（毫秒）
--
-- 返回:
--   [1] 是否允许请求（1=允许, 0=拒绝）
--   [2] 当前桶中剩余令牌数
--   [3] 下次可获取令牌的等待时间（毫秒，0表示无需等待）
-- ============================================================

local key = KEYS[1]
local max_tokens = tonumber(ARGV[1])
local tokens_per_second = tonumber(ARGV[2])
local requested_tokens = tonumber(ARGV[3])
local now = tonumber(ARGV[4])

-- 获取当前桶状态
local bucket = redis.call('HMGET', key, 'tokens', 'last_refill')
local current_tokens = tonumber(bucket[1])
local last_refill = tonumber(bucket[2])

-- 初始化令牌桶
if current_tokens == nil then
    current_tokens = max_tokens
    last_refill = now
end

-- 计算自上次填充以来应生成的令牌数
local elapsed_ms = now - last_refill
local elapsed_seconds = elapsed_ms / 1000
local new_tokens = elapsed_seconds * tokens_per_second

-- 更新令牌数（不超过最大容量）
current_tokens = math.min(max_tokens, current_tokens + new_tokens)
last_refill = now

-- 判断是否有足够的令牌
local allowed = 0
local wait_time = 0

if current_tokens >= requested_tokens then
    -- 消耗令牌
    current_tokens = current_tokens - requested_tokens
    allowed = 1
else
    -- 计算需要等待的时间
    local deficit = requested_tokens - current_tokens
    wait_time = math.ceil((deficit / tokens_per_second) * 1000)
end

-- 保存桶状态（设置过期时间为填满桶所需时间的2倍，避免永久残留）
local ttl = math.ceil((max_tokens / tokens_per_second) * 2)
redis.call('HSET', key, 'tokens', current_tokens, 'last_refill', last_refill)
redis.call('EXPIRE', key, ttl)

return { allowed, current_tokens, wait_time }
