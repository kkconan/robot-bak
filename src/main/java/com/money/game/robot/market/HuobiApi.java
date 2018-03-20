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


    /**
     * 行情
     */
    @Value("${huobi.pro.market.api.url:https://api.huobipro.com/market/history/kline}")
    private String proMarketApi;

    /**
     * 交易对
     */
    @Value("${huobi.pro.api.symbol.url:https://api.huobipro.com/v1/common/symbols}")
    private String symbolsApi;

    /**
     * 获取所有的交易对
     */
    public List<SymBolsDetailVo> getSymbolsInfo() {
        String jsonStr = null;
        List<SymBolsDetailVo> detailVoList = new ArrayList<>();
        try {
            CloseableHttpClient httpclient = HttpClientBuilder.create().build();
            HttpGet httpGet = new HttpGet(symbolsApi);
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
            String url = proMarketApi + "?period=" + period + "&size=" + size + "&symbol=" + symbol;
            HttpGet httpGet = new HttpGet(url);
            CloseableHttpResponse response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            jsonStr = EntityUtils.toString(entity, "utf-8");
            httpGet.releaseConnection();
            JSONObject json = JSON.parseObject(jsonStr);
            if (!"ok".equals(json.getString("status"))) {
                log.info("market info not found. result={}", json);
                return null;
            }
            MarketInfoVo marketInfoVo = new ObjectMapper().readValue(jsonStr, MarketInfoVo.class);
            if (marketInfoVo.getData() == null || marketInfoVo.getData().isEmpty()) {
                throw new BizException(ErrorEnum.MARKEY_INFO_FAIL);
            }
//            log.info("marketInfoVo={}", marketInfoVo);
            return marketInfoVo;
        } catch (Exception e) {
            e.printStackTrace();
            log.error("get market fail,e={},result={},period={},size={},symbol={}", e, jsonStr, period, size, symbol);
            return null;
        }
    }
}


