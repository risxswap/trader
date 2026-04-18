package cc.riskswap.trader.collector.repository.tushare;

import com.alibaba.fastjson2.JSON;

import cc.riskswap.trader.collector.common.util.DateUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Nocobase API 通用操作管理器
 * 封装Nocobase API的常用操作，提供统一的数据访问接口
 */
@Slf4j
@Component
public class TushareManager {

    private final OkHttpClient client;
    
    @Value("${trader.tushare.url:}")
    private String baseUrl;
    
    @Value("${trader.tushare.token:}")
    private String token;
    
    // 限流相关配置
    private static final int MAX_REQUESTS_PER_MINUTE = 80;
    private static final long WINDOW_SIZE_MS = 60 * 1000; // 1分钟
    
    // 使用滑动窗口算法实现限流
    private final ConcurrentLinkedQueue<Long> requestTimestamps = new ConcurrentLinkedQueue<>();
    private final AtomicInteger currentRequests = new AtomicInteger(0);
    private final Object rateLimitLock = new Object();
    
    /**
     * 构造函数，初始化OkHttpClient
     */
    public TushareManager() {
        // 配置OkHttpClient，设置连接超时、读取超时和写入超时
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }

    /**
     * 检查是否可以发起请求（限流检查）
     * @return true 如果可以发起请求，false 如果需要等待
     */
    private boolean canMakeRequest() {
        synchronized (rateLimitLock) {
            long currentTime = System.currentTimeMillis();
            
            // 清理过期的时间戳（超过1分钟的请求记录）
            while (!requestTimestamps.isEmpty() && 
                   currentTime - requestTimestamps.peek() > WINDOW_SIZE_MS) {
                requestTimestamps.poll();
                currentRequests.decrementAndGet();
            }
            
            // 检查当前请求数是否超过限制
            if (currentRequests.get() >= MAX_REQUESTS_PER_MINUTE) {
                return false;
            }
            
            // 记录当前请求
            requestTimestamps.offer(currentTime);
            currentRequests.incrementAndGet();
            return true;
        }
    }
    
    /**
     * 等待直到可以发起请求
     */
    private void waitForRateLimit() {
        while (!canMakeRequest()) {
            try {
                // 计算需要等待的时间
                Long oldestRequest = requestTimestamps.peek();
                if (oldestRequest != null) {
                    long waitTime = WINDOW_SIZE_MS - (System.currentTimeMillis() - oldestRequest);
                    if (waitTime > 0) {
                        log.debug("达到限流限制，等待 {} ms", waitTime);
                        Thread.sleep(Math.min(waitTime, 1000)); // 最多等待1秒后重试
                    }
                } else {
                    Thread.sleep(100); // 如果队列为空，短暂等待
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("等待限流时被中断", e);
            }
        }
    }

    public String post(String apiName, String fields, Object params) {
        // 限流检查
        waitForRateLimit();
        
        try {
            // 构建请求体
            Map<String,Object> jsonData = new HashMap<>();
            jsonData.put("token", token);
            jsonData.put("api_name", apiName);
            if (!ObjectUtils.isEmpty(fields)) {
                jsonData.put("fields", fields);
            }
            if (params!=null) {
                jsonData.put("params", params);
            }
            RequestBody body = RequestBody.create(
                    JSON.toJSONString(jsonData),
                    MediaType.parse("application/json; charset=utf-8")
            );
            HttpUrl.Builder urlBuilder = HttpUrl.parse(baseUrl).newBuilder();
            String requestUrl = urlBuilder.build().toString();
            // 构建请求
            Request request = new Request.Builder()
                    .url(requestUrl)
                    .post(body)
                    .build();
            
            // 执行请求
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    log.error("POST请求失败: {}, 状态码: {}", apiName, response.code());
                    throw new RuntimeException("请求失败，状态码: " + response.code());
                }
                
                ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    return "";
                }
                
                String responseString = responseBody.string();
                log.debug("POST请求成功: {}, 响应: {}", apiName, responseString);
                return responseString;
            }
        } catch (IOException e) {
            log.error("执行POST请求异常: {}", apiName, e);
            throw new RuntimeException("请求异常", e);
        }
    }

    public OffsetDateTime parseDate(String dateTime) {
        if (ObjectUtils.isEmpty(dateTime)) {
            return null;
        }
        // 使用LocalDate解析，然后转换为OffsetDateTime，避免ZonedDateTime解析错误
        return DateUtil.parseLocalDateToOffsetDateTime(dateTime, DateUtil.D_FORMAT_INT_FORMATTER);
    }
}