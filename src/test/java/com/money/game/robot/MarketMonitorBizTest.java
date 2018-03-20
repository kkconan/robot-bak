package com.money.game.robot;

import com.money.game.robot.biz.MarketMonitorBiz;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.mail.MailQQ;
import com.money.game.robot.market.HuobiApi;
import com.money.game.robot.schedule.MonitorSchedule;
import com.money.game.robot.vo.huobi.SymBolsDetailVo;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 火币单种行情监控
     */
    @Test
    public void huoBiMonitorTest() throws Exception {
        marketMonitorBiz.huoBiMonitor(DictEnum.MARKET_HUOBI_SYMBOL_BTC_USDT.getCode());
//        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
//        HttpGet httpGet = new HttpGet("https://api.huobipro.com");
//        CloseableHttpResponse response = httpclient.execute(httpGet);
//        HttpEntity entity = response.getEntity();
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
        //increase eosbtc
        marketMonitorBiz.checkToTrans("eoseth", new BigDecimal("0.1"), new BigDecimal("0.05"));

    }
}
