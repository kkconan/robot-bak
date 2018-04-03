package com.money.game.robot.zb.api;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.dto.zb.BaseZbDto;
import com.money.game.robot.dto.zb.ZbCancelOrderDto;
import com.money.game.robot.dto.zb.ZbCreateOrderDto;
import com.money.game.robot.dto.zb.ZbOrderDetailDto;
import com.money.game.robot.exception.BizException;
import com.money.game.robot.zb.EncryDigestUtil;
import com.money.game.robot.zb.HttpUtilManager;
import com.money.game.robot.zb.MapSort;
import com.money.game.robot.zb.vo.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

/**
 * @author conan
 *         2018/3/29 17:19
 **/
@Component
@Slf4j
public class ZbApi {

    @Value("${zb.trade.host:https://trade.zb.com/api/}")
    public String tradeHost;

    @Value("${zb.api.host:http://api.zb.com}")
    public String apiHost;

    public ZbKineDetailVo getOneKineInfo(String currency) {
        ZbKineVo zbKineVo = getKline(currency, DictEnum.TRADE_CONFIG_THRESHOLD_TYPE_ONE_MIN.getCode(), 1);
        if (zbKineVo != null && zbKineVo.getData() != null) {
            return zbKineVo.getData().get(0);
        }
        return null;
    }

    /**
     * 获取K线行情
     */
    public ZbKineVo getKline(String currency, String type, Integer size) {
        ZbKineVo kineVo = new ZbKineVo();
        List<ZbKineDetailVo> detailVos = new ArrayList<>();
        // 请求地址
        String url = apiHost + "/data/v1/kline?market=" + currency + "&type=" + type + "&size=" + size;
        String callback = get(url);
        JSONObject json = JSONObject.parseObject(callback);
        if (json == null || json.get("data") == null) {
            log.info("交易对不存在,url={},json={}", url, json);
            return kineVo;
        }
        Object[] objects = json.getJSONArray("data").toArray();
        for (Object object : objects) {
            JSONArray jsonArray = (JSONArray) object;
            ZbKineDetailVo vo = new ZbKineDetailVo();
            vo.setTimestamp(jsonArray.getLong(0));
            vo.setOpen(jsonArray.getBigDecimal(1));
            vo.setHigh(jsonArray.getBigDecimal(2));
            vo.setLow(jsonArray.getBigDecimal(3));
            vo.setClose(jsonArray.getBigDecimal(4));
            vo.setVol(jsonArray.getBigDecimal(5));
            detailVos.add(vo);
        }
        kineVo.setData(detailVos);
        kineVo.setMoneyType(json.getString("moneyType"));
        kineVo.setSymbol(json.getString("symbol"));
        return kineVo;
    }

    /**
     * 获取所有交易对
     */
    public List<ZbSymbolInfoVo> getSymbolInfo() {
        List<ZbSymbolInfoVo> zbSymbolInfoVoList = new ArrayList<>();
        // 请求地址
        String url = apiHost + "/data/v1/markets";
        String callback = get(url);
        JSONObject jsonObject = JSONObject.parseObject(callback);
        Set<Map.Entry<String, Object>> set = jsonObject.entrySet();
        for (Map.Entry<String, Object> aSet : set) {
            ZbSymbolInfoVo zbSymbolInfoVo = new ZbSymbolInfoVo();
            zbSymbolInfoVo.setCurrency(aSet.getKey());
            JSONObject json = (JSONObject) aSet.getValue();
            zbSymbolInfoVo.setAmountScale(json.getInteger("amountScale"));
            zbSymbolInfoVo.setPriceScale(json.getInteger("priceScale"));
            zbSymbolInfoVoList.add(zbSymbolInfoVo);
        }
        return zbSymbolInfoVoList;
    }

    /**
     * 委托下单
     */
    public ZbCreateOrderVo createOrder(ZbCreateOrderDto dto) {
        Map<String, String> params = new HashMap<>();
        params.put("method", "order");
        params.put("price", dto.getPrice());
        params.put("amount", dto.getAmount());
        params.put("tradeType", dto.getTradeType());
        params.put("currency", dto.getCurrency());
        return this.post(params, dto.getAccessKey(), dto.getSecretKey(), new TypeReference<ZbCreateOrderVo>() {
        });
    }

    /**
     * 取消下单
     */
    public ZbResponseVo cancelOrder(ZbCancelOrderDto dto) {
        Map<String, String> params = new HashMap<>();
        params.put("method", "cancelOrder");
        params.put("id", dto.getOrderId());
        params.put("currency", dto.getCurrency());
        return this.post(params, dto.getAccessKey(), dto.getSecretKey(), new TypeReference<ZbResponseVo>() {
        });
    }

    /**
     * 获取订单信息
     */
    public ZbOrderDetailVo orderDetail(ZbOrderDetailDto dto) {
        Map<String, String> params = new HashMap<>();
        params.put("method", "getOrder");
        params.put("id", dto.getOrderId());
        params.put("currency", dto.getCurrency());
        return this.post(params, dto.getAccessKey(), dto.getSecretKey(), new TypeReference<ZbOrderDetailVo>() {
        });
    }


    /**
     * 获取深度
     */
    public ZbOrderDepthVo orderDepth(String currency, Integer size) {
        String url = apiHost + "/data/v1/depth?market=" + currency;
        if (size != null) {
            url = url + "&size=" + size;
        }
        return get(url, new TypeReference<ZbOrderDepthVo>() {
        });
    }

    /**
     * 获取个人信息
     */
    public List<ZbAccountDetailVo> getAccountInfo(BaseZbDto dto) {
        // 需加密的请求参数
        Map<String, String> params = new HashMap<>();
        params.put("method", "getAccountInfo");
        ZbAccountResponseVo accountResponseVo = this.post(params, dto.getAccessKey(), dto.getSecretKey(), new TypeReference<ZbAccountResponseVo>() {
        });
        return accountResponseVo.getResult().getCoins();

    }

    /**
     * @return 返回指定类
     */
    private <T> T post(Map<String, String> params, String accessKey, String secretKey, TypeReference<T> ref) {
        try {
            String result = this.getJsonPost(params, accessKey, secretKey);
            return JsonUtil.readValue(result, ref);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }
    }

    /**
     * 获取json内容(统一加密)
     */
    private String getJsonPost(Map<String, String> params, String accessKey, String secretKey) {
        params.put("accesskey", accessKey);// 这个需要加入签名,放前面
        String digest = EncryDigestUtil.digest(secretKey);

        String sign = EncryDigestUtil.hmacSign(MapSort.toStringMap(params), digest); // 参数执行加密
        String method = params.get("method");

        // 加入验证
        params.put("sign", sign);
        params.put("reqTime", System.currentTimeMillis() + "");
        String json = "";
        try {
            json = HttpUtilManager.getInstance().requestHttpPost(tradeHost, method, params);
        } catch (HttpException | IOException e) {
            log.error("获取交易json异常", e);
        }
        return json;
    }

    /**
     * @param urlStr :请求接口
     * @return 返回指定类
     */
    private <T> T get(String urlStr, TypeReference<T> ref) {
        try {
            String result = this.get(urlStr);
            return JsonUtil.readValue(result, ref);
        } catch (Exception e) {
            throw new BizException(e.getMessage());
        }
    }

    private String get(String urlStr) {
        BufferedReader reader;
        String result;
        StringBuilder sbf = new StringBuilder();
        String userAgent = "Mozilla/5.0 (Windows NT 6.1) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/29.0.1547.66 Safari/537.36";// 模拟浏览器
        try {
            URL url = new URL(urlStr);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setReadTimeout(30000);
            connection.setConnectTimeout(30000);
            connection.setRequestProperty("User-agent", userAgent);
            connection.connect();
            InputStream is = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(is, "UTF-8"));
            String strRead;
            while ((strRead = reader.readLine()) != null) {
                sbf.append(strRead);
                sbf.append("\r\n");
            }
            reader.close();
            result = sbf.toString();
            return result;
        } catch (Exception e) {
            log.error("e={}", e);
            throw new BizException(e.getMessage());
        }
    }

}

class JsonUtil {

    public static String writeValue(Object obj) throws IOException {
        return OBJECT_MAPPER.writeValueAsString(obj);
    }

    static <T> T readValue(String s, TypeReference<T> ref) throws IOException {
        return OBJECT_MAPPER.readValue(s, ref);
    }

    private static final ObjectMapper OBJECT_MAPPER = createObjectMapper();

    private static ObjectMapper createObjectMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategy.KEBAB_CASE);
        mapper.setSerializationInclusion(JsonInclude.Include.ALWAYS);
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }
}
