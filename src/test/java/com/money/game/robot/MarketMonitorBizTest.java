package com.money.game.robot;

import com.money.game.robot.biz.MarketMonitorBiz;
import com.money.game.robot.biz.SymbolTradeConfigBiz;
import com.money.game.robot.entity.SymbolTradeConfigEntity;
import com.money.game.robot.mail.MailQQ;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.schedule.MonitorSchedule;
import com.money.game.robot.vo.huobi.SymBolsDetailVo;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;
import java.util.List;

/**
 * conan
 * 2017/10/18 9:51
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class MarketMonitorBizTest {

    @Autowired
    private MarketMonitorBiz marketMonitorBiz;

    @Autowired
    private HuobiApi huobiApi;

    @Autowired
    private MonitorSchedule monitorSchedule;

    @Autowired
    private SymbolTradeConfigBiz symbolTradeConfigBiz;

    /**
     * 火币单种行情监控
     */
    @Test
    public void huoBiMonitorTest() throws Exception {
//        marketMonitorBiz.huoBiMonitor(DictEnum.MARKET_HUOBI_SYMBOL_BTC_USDT.getCode());
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        HttpGet httpGet = new HttpGet("https://api.huobipro.com/market/history/kline?period=1min&size=1&symbol=mtxbtc");
        CloseableHttpResponse response = httpclient.execute(httpGet);
        HttpEntity entity = response.getEntity();
        String jsonStr = EntityUtils.toString(entity, "utf-8");
        System.err.println(jsonStr);
    }

    /**
     * 火币全行情监控
     */
    @Test
    public void huoBiSymBolsTest() throws Exception {
        monitorSchedule.huoBiAllSymBolsMonitor();

    }


    /**
     * 火币交易对查询
     */
    @Test
    public void getSymbolsInfoTest() throws Exception {
        List<SymBolsDetailVo> list = huobiApi.getSymbolsInfo();
        log.info("list={}", list);

    }

    /**
     * send email
     */
    @Test
    public void sendEmailTest() throws Exception {
        MailQQ.sendEmail("aa", "bbb", "824968443@qq.com");

    }


    @Test
    @Rollback(false)
    public void checkTransTest() {
        SymbolTradeConfigEntity symbolTradeConfigEntity = symbolTradeConfigBiz.findById("1");
        //increase eosbtc
        marketMonitorBiz.checkToTrans("eoseth", new BigDecimal("0.1"), symbolTradeConfigEntity);

    }
}
