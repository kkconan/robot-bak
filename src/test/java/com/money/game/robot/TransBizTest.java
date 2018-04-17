package com.money.game.robot;

import com.money.game.robot.biz.TransBiz;
import lombok.extern.slf4j.Slf4j;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author conan
 *         2018/3/16 16:03
 **/
@RunWith(SpringRunner.class)
@SpringBootTest
@Slf4j
public class TransBizTest {

    @Autowired
    TransBiz transBiz;

    @Test
    public void transTest() {
//        transBiz.trans("eoseth","eos",new BigDecimal(0.1),"1min",2,new BigDecimal(0.05));
    }

    @Test
    public void limitBuyOrderTest() {
        transBiz.hbTransModelLimitOrder();
    }


    @Test
    public void zbToSaleTest() {
        transBiz.zbToSale();
    }

    @Test
    public void zbCheckSaleFinishTest() {
        transBiz.zbCheckSaleFinish();
    }

    @Test
    public void zbTransModelLimitOrderTest() {
        transBiz.zbTransModelLimitOrder();
    }

    @Test
    @Rollback(false)
    public void hbCheckLimitBetaOrderTest() {
        transBiz.hbCheckLimitBetaOrder();
    }

    @Test
    @Rollback(false)
    public void zbCheckLimitBetaOrderTest() {
        transBiz.zbCheckLimitBetaOrder();
    }


}
