package cc.riskswap.trader.collector.common.util;

import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.springframework.util.ObjectUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @Author: wuhaiming
 * @Date: 2024/6/18 18:39
 */
@Slf4j
public class HttpUtil {

    private static final OkHttpClient CLIENT = new OkHttpClient();


    public static final MediaType JSON = MediaType.get("application/json");

    /**
     * 发送get请求
     */
    public static String get(String url, Map<String, Object> params, Map<String, Object> headers) {
        if (ObjectUtils.isEmpty(url)) {
            return "";
        }
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (!ObjectUtils.isEmpty(params)) {
            for (Map.Entry<String, Object> param: params.entrySet()) {
                String key = param.getKey();
                if (param.getValue()==null) {
                    continue;
                }
                if (param.getValue() instanceof List) {
                    @SuppressWarnings("rawtypes")
                    List values = (List)param.getValue();
                    for (Object value : values) {
                        String valueStr = Objects.toString(value, "");
                        urlBuilder.addQueryParameter(key+"[]", valueStr);
                    }
                } else {
                    String value = Objects.toString(param.getValue(), "");
                    urlBuilder.addQueryParameter(key, value);
                }
            }
        }
        String endUrl = urlBuilder.build().toString();
        Request request = new Request.Builder()
                .url(endUrl)
                .get()
                .headers(buildHeaders(headers))
                .build();
        Response response = null;
        try {
            response = CLIENT.newCall(request).execute();
            ResponseBody body = response.body();
            if (body == null) {
                return "";
            }
            return body.string();
        } catch (IOException e) {
            log.error("List products exception");
        }
        return "";
    }

    /**
     * 发送post请求
     */
    public static String postJson(String url, Map<String, Object> params, Map<String, Object> headers) {
        RequestBody body = RequestBody.create(JSONUtil.toJsonStr(params), JSON);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .headers(buildHeaders(headers))
                .build();
        Call call = CLIENT.newCall(request);
        Response response = null;
        try {
            response = call.execute();
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return "";
            }
            return responseBody.string();
        } catch (IOException e) {
            log.error("Get zentao accessToken error", e);
        }
        return "";
    }

    public static String postJson(String url, Map<String,Object> params, Object data, Map<String, Object> headers) {
        if (ObjectUtils.isEmpty(url)) {
            return "";
        }
        HttpUrl.Builder urlBuilder = HttpUrl.parse(url).newBuilder();
        if (!ObjectUtils.isEmpty(params)) {
            for (Map.Entry<String, Object> param: params.entrySet()) {
                String key = param.getKey();
                if (param.getValue()==null) {
                    continue;
                }
                if (param.getValue() instanceof List) {
                    @SuppressWarnings("rawtypes")
                    List values = (List)param.getValue();
                    for (Object value : values) {
                        String valueStr = Objects.toString(value, "");
                        urlBuilder.addQueryParameter(key+"[]", valueStr);
                    }
                } else {
                    String value = Objects.toString(param.getValue(), "");
                    urlBuilder.addQueryParameter(key, value);
                }
            }
        }
        String endUrl = urlBuilder.build().toString();
        RequestBody body = RequestBody.create(JSONUtil.toJsonStr(data), JSON);
        Request request = new Request.Builder()
                .url(endUrl)
                .post(body)
                .headers(buildHeaders(headers))
                .build();
        Call call = CLIENT.newCall(request);
        Response response = null;
        try {
            response = call.execute();
            ResponseBody responseBody = response.body();
            if (responseBody == null) {
                return "";
            }
            return responseBody.string();
        } catch (IOException e) {
            log.error("Get zentao accessToken error", e);
        }
        return "";
    }

    /**
     * 发送post请求
     */
    public static String postJson(String url, Map<String, Object> params) {
        return postJson(url, params, null);
    }


    public static Headers buildHeaders(Map<String, Object> params) {
        Headers.Builder headersBuilder = new Headers.Builder();
        if (!ObjectUtils.isEmpty(params)) {
            for (Map.Entry<String, Object> header: params.entrySet()) {
                if (header.getValue() == null) {
                    continue;
                }
                headersBuilder.add(header.getKey(), Objects.toString(header.getValue()));
            }
        }
        return headersBuilder.build();
    }
}
