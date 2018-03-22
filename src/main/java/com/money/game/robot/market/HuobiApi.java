package com.money.game.robot.market;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.money.game.core.util.StringUtil;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.constant.ErrorEnum;
import com.money.game.robot.exception.BizException;
import com.money.game.robot.vo.huobi.MarketInfoVo;
import com.money.game.robot.vo.huobi.SymBolsDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * @author conan
 *         2018/3/8 17:53
 **/
@Component
@Slf4j
public class HuobiApi {


    @Value("${huobi.pro.api.host}")
    private String apiHost;
    /**
     * 行情
     */
    @Value("${huobi.pro.market.api.url:/market/history/kline}")
    private String marketApiUrl;

    /**
     * 交易对
     */
    @Value("${huobi.pro.api.symbol.url:/v1/common/symbols}")
    private String symbolsApiUrl;

    /**
     * 获取所有的交易对
     */
    public List<SymBolsDetailVo> getSymbolsInfo() {
        String jsonStr = null;
        List<SymBolsDetailVo> detailVoList = new ArrayList<>();
        try {
            CloseableHttpClient httpclient = HttpClientBuilder.create().build();
            HttpGet httpGet = new HttpGet(apiHost + symbolsApiUrl);
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000).build();//设置请求和传输超时时间
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            jsonStr = EntityUtils.toString(entity, "utf-8");
            httpGet.releaseConnection();
            JSONObject jsonObject = JSON.parseObject(jsonStr);
            JSONArray detailArray = jsonObject.getJSONArray("data");
            for (int i = 0; i < detailArray.size(); i++) {
                SymBolsDetailVo detailVo = new SymBolsDetailVo();
                //去除无效的交易对
                if (!"bt1".equals(detailArray.getJSONObject(i).getString("base-currency")) && !"bt2".equals(detailArray.getJSONObject(i).getString("base-currency"))) {
                    detailVo.setSymbols(detailArray.getJSONObject(i).getString("base-currency") + detailArray.getJSONObject(i).getString("quote-currency"));
                    detailVoList.add(detailVo);
                }
            }
            return detailVoList;
        } catch (Exception e) {
            log.warn("get all symbols fail,use default info,e={},result={}", e.getMessage(), e, jsonStr);
            List<DictEnum> dictEnumList = DictEnum.huobiSymbol;
            for (DictEnum dictEnum : dictEnumList) {
                SymBolsDetailVo detailVo = new SymBolsDetailVo();
                detailVo.setSymbols(dictEnum.getCode());
                detailVoList.add(detailVo);
            }
        }
        return detailVoList;
    }

    /**
     * @param period K线类型 1min, 5min, 15min, 30min, 60min, 1day, 1mon, 1week, 1year
     * @param size   获取数量 [1,2000]
     * @param symbol 交易对 btcusdt, bccbtc, rcneth
     */
    public MarketInfoVo getMarketInfo(String period, Integer size, String symbol) {
        String jsonStr = null;
        String url = null;
        try {
            CloseableHttpClient httpclient = HttpClientBuilder.create().build();

            //超过2000分钟，默认查询100条
            if (size > 2000) {
                size = 2000;
            }
            if (StringUtil.isEmpty(symbol)) {
                log.error("symbol is not null");
                return null;
            }
            url = apiHost + marketApiUrl + "?period=" + period + "&size=" + size + "&symbol=" + symbol;
            HttpGet httpGet = new HttpGet(url);
            RequestConfig requestConfig = RequestConfig.custom().setSocketTimeout(60000).setConnectTimeout(60000).build();//设置请求和传输超时时间
            httpGet.setConfig(requestConfig);
            CloseableHttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            jsonStr = EntityUtils.toString(entity, "utf-8");
            httpGet.releaseConnection();
            JSONObject json = JSON.parseObject(jsonStr);
            if (!"ok".equals(json.getString("status"))) {
                log.info(symbol + " market info not found. result={}", json);
                return null;
            }
            MarketInfoVo marketInfoVo = new ObjectMapper().readValue(jsonStr, MarketInfoVo.class);
            if (marketInfoVo.getData() == null || marketInfoVo.getData().isEmpty()) {
                throw new BizException(ErrorEnum.MARKEY_INFO_FAIL);
            }
            return marketInfoVo;
        } catch (Exception e) {
            log.error("get market fail,url={},result={},period={},size={},symbol={},e={},", url, jsonStr, period, size, symbol, e.getMessage());
            return null;
        }
    }
}


