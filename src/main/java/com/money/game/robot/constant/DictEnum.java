package com.money.game.robot.constant;

import java.util.ArrayList;
import java.util.List;

/**
 * conan
 * 2017/10/10 16:33
 **/
public enum DictEnum {


    USER_TYPE_01("01", "普通用户"),
    USER_TYPE_02("02", "系统用户"),

    USER_STATUS_NORMAL("01", "正常"),
    USER_STATUS_FORBIDDER("02", "冻结"),

    ACCOUNT_TYPE_01("01", "基本账户"),
    ACCOUNT_TYPE_05("05", "平台账户"),

    TRADE_TYPE_01("01", "注册赠送"),
    TRADE_TYPE_02("02", "充值"),
    TRADE_TYPE_03("03", "提现"),
    TRADE_TYPE_04("04", "投注"),
    TRADE_TYPE_05("05", "盈利"),

    PAY_STATUS_01("01", "已创建"),
    PAY_STATUS_02("02", "待审核"),
    PAY_STATUS_03("03", "已完成"),

    PAY_AUDIT_STATUS_01("01", "未审核"),
    PAY_AUDIT_STATUS_02("02", "审核通过"),
    PAY_AUDIT_STATUS_03("03", "审核拒绝"),

    PAY_DIRECTION_01("01", "加"),
    PAY_DIRECTION_02("02", "减"),

    MARKEY_PERIOD_1MIN("1min", "一分钟"),
    MARKEY_PERIOD_5MIN("5min", "五分钟"),
    MARKEY_PERIOD_30MIN("30min", "三十分钟"),
    MARKEY_PERIOD_60MIN("60min", "六十分钟"),
    MARKEY_PERIOD_1DAY("1day", "一天"),
    MARKEY_PERIOD_1WEEK("1week", "一周"),
    MARKEY_PERIOD_1MON("1mon", "一月"),
    MARKEY_PERIOD_1YEAR("1year", "一年"),

    //主流
    MARKEY_HUOBI_SYMBOL_BTC_USDT("btcusdt", "btc/usdt行情"),
    MARKEY_HUOBI_SYMBOL_BTC_ETH("ethbtc", "btc/eth行情"),
    MARKEY_HUOBI_SYMBOL_BTC_LTC("ltcbtc", "btc/ltc行情"),
    MARKEY_HUOBI_SYMBOL_BTC_ETC("etcbtc", "btc/etc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_EOS("eosbtc", "btc/eos市场"),
    MARKEY_HUOBI_SYMBOL_BTC_OMG("omgbtc", "btc/omg市场"),
    MARKEY_HUOBI_SYMBOL_BTC_DASH("dashbtc", "btc/dash市场"),
    MARKEY_HUOBI_SYMBOL_BTC_XRP("xrpbtc", "btc/xrp市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ZEC("zecbtc", "btc/zec市场"),
    //创新
    MARKEY_HUOBI_SYMBOL_BTC_BLZ("blzbtc", "btc/blz市场"),
    MARKEY_HUOBI_SYMBOL_BTC_EDU("edubtc", "btc/edu市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ENG("engbtc", "btc/eng市场"),
    MARKEY_HUOBI_SYMBOL_BTC_WPR("wprbtc", "btc/wpr市场"),
    MARKEY_HUOBI_SYMBOL_BTC_MTX("mtxbtc", "btc/mtx市场"),
    MARKEY_HUOBI_SYMBOL_BTC_MTN("mtnbtc", "btc/mtn市场"),
    MARKEY_HUOBI_SYMBOL_BTC_SNC("sncbtc", "btc/snc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_LSK("lskbtc", "btc/lsk市场"),
    MARKEY_HUOBI_SYMBOL_BTC_STK("stkbtc", "btc/stk市场"),
    MARKEY_HUOBI_SYMBOL_BTC_HT("htbtc", "btc/ht市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ELA("elabtc", "btc/ela市场"),
    MARKEY_HUOBI_SYMBOL_BTC_SRN("srnbtc", "btc/srn市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ZLA("zlabtc", "btc/zla市场"),
    MARKEY_HUOBI_SYMBOL_BTC_TRX("trxbtc", "btc/trx市场"),
    MARKEY_HUOBI_SYMBOL_BTC_OCN("ocnbtc", "btc/ocn市场"),
    MARKEY_HUOBI_SYMBOL_BTC_LUN("lunbtc", "btc/lun市场"),
    MARKEY_HUOBI_SYMBOL_BTC_IOST("iostbtc", "btc/iost市场"),
    MARKEY_HUOBI_SYMBOL_BTC_HSR("hsrbtc", "btc/hsr市场"),
    MARKEY_HUOBI_SYMBOL_BTC_SMT("smtbtc", "btc/smt市场"),
    MARKEY_HUOBI_SYMBOL_BTC_LET("letbtc", "btc/let市场"),
    MARKEY_HUOBI_SYMBOL_BTC_SWFTC("swftcbtc", "btc/swftc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_WAX("waxbtc", "btc/wax市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ELF("elfbtc", "btc/elf市场"),
    MARKEY_HUOBI_SYMBOL_BTC_MDS("mdsbtc", "btc/mds市场"),
    MARKEY_HUOBI_SYMBOL_BTC_TNB("tnbbtc", "btc/tnb市场"),
    MARKEY_HUOBI_SYMBOL_BTC_NAS("nasbtc", "btc/nas市场"),
    MARKEY_HUOBI_SYMBOL_BTC_BTM("btmbtc", "btc/btm市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ITC("itcbtc", "btc/itc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_THETA("thetabtc", "btc/theta市场"),
    MARKEY_HUOBI_SYMBOL_BTC_WICC("wiccbtc", "btc/wicc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_GNX("gnxbtc", "btc/gnx市场"),
    MARKEY_HUOBI_SYMBOL_BTC_VEN("venbtc", "btc/ven市场"),
    MARKEY_HUOBI_SYMBOL_BTC_DTA("dtabtc", "btc/dta市场"),
    MARKEY_HUOBI_SYMBOL_BTC_MANA("manabtc", "btc/mana市场"),
    MARKEY_HUOBI_SYMBOL_BTC_QASH("qashbtc", "btc/qash市场"),
    MARKEY_HUOBI_SYMBOL_BTC_PROPY("propybtc", "btc/propy市场"),
    MARKEY_HUOBI_SYMBOL_BTC_SNT("sntbtc", "btc/snt市场"),
    MARKEY_HUOBI_SYMBOL_BTC_QUN("qunbtc", "btc/qun市场"),
    MARKEY_HUOBI_SYMBOL_BTC_QTUM("qtumbtc", "btc/qtum市场"),
    MARKEY_HUOBI_SYMBOL_BTC_DAT("datbtc", "btc/snt市场"),
    MARKEY_HUOBI_SYMBOL_BTC_TNT("tntbtc", "btc/tnt市场"),
    MARKEY_HUOBI_SYMBOL_BTC_CMT("cmtbtc", "btc/cmt市场"),
    MARKEY_HUOBI_SYMBOL_BTC_YEE("yeebtc", "btc/yee市场"),
    MARKEY_HUOBI_SYMBOL_BTC_GAS("cmtbtc", "btc/gas市场"),
    MARKEY_HUOBI_SYMBOL_BTC_AIDOC("aidocbtc", "btc/aidoc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_STORJ("storjbtc", "btc/storj市场"),
    MARKEY_HUOBI_SYMBOL_BTC_XEM("xembtc", "btc/xem市场"),
    MARKEY_HUOBI_SYMBOL_BTC_PAY("paybtc", "btc/pay市场"),
    MARKEY_HUOBI_SYMBOL_BTC_NEO("neobtc", "btc/neo市场"),
    MARKEY_HUOBI_SYMBOL_BTC_CVC("cvcbtc", "btc/cvc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_QSP("qspbtc", "btc/qsp市场"),
    MARKEY_HUOBI_SYMBOL_BTC_TOPC("topcbtc", "btc/topc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_RCN("rcnbtc", "btc/rcn市场"),
    MARKEY_HUOBI_SYMBOL_BTC_AST("astbtc", "btc/ast市场"),
    MARKEY_HUOBI_SYMBOL_BTC_BAT("batbtc", "btc/bat市场"),
    MARKEY_HUOBI_SYMBOL_BTC_DBC("dbcbtc", "btc/dbc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_RPX("rpxbtc", "btc/rpx市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ACT("actbtc", "btc/act市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ICX("rcnbtc", "btc/rcn市场"),
    MARKEY_HUOBI_SYMBOL_BTC_KNC("kncbtc", "btc/knc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_MCO("mcobtc", "btc/mco市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ZRX("zrxbtc", "btc/zrx市场"),
    MARKEY_HUOBI_SYMBOL_BTC_MTL("mtlbtc", "btc/mtl市场"),
    MARKEY_HUOBI_SYMBOL_BTC_GNT("gntbtc", "btc/gnt市场"),
    MARKEY_HUOBI_SYMBOL_BTC_REQ("reqbtc", "btc/req市场"),
    MARKEY_HUOBI_SYMBOL_BTC_RDN("rdnbtc", "btc/rdn市场"),
    MARKEY_HUOBI_SYMBOL_BTC_SALT("saltbtc", "btc/salt市场"),
    MARKEY_HUOBI_SYMBOL_BTC_MEE("meebtc", "btc/mee市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ZIL("zilbtc", "btc/zil市场"),
    MARKEY_HUOBI_SYMBOL_BTC_CHAT("chatbtc", "btc/chat市场"),
    MARKEY_HUOBI_SYMBOL_BTC_POWR("powrbtc", "btc/powr市场"),
    MARKEY_HUOBI_SYMBOL_BTC_DGD("dgdbtc", "btc/dgd市场"),
    MARKEY_HUOBI_SYMBOL_BTC_APPC("appcbtc", "btc/appc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_OST("ostbtc", "btc/ost市场"),
    MARKEY_HUOBI_SYMBOL_BTC_SOC("socbtc", "btc/soc市场"),
    MARKEY_HUOBI_SYMBOL_BTC_EKO("ekobtc", "btc/eko市场"),
    MARKEY_HUOBI_SYMBOL_BTC_LINK("linkbtc", "btc/link市场"),
    MARKEY_HUOBI_SYMBOL_BTC_UTK("utkbtc", "btc/utk市场"),
    MARKEY_HUOBI_SYMBOL_BTC_EVX("evxbtc", "btc/evx市场"),
    MARKEY_HUOBI_SYMBOL_BTC_ADX("adxbtc", "btc/adx市场"),
    MARKEY_HUOBI_SYMBOL_BTC_BCD("bcdbtc", "btc/bcd市场"),
    MARKEY_HUOBI_SYMBOL_BTC_BCX("bcxbtc", "btc/bcx市场"),

    DIGITAL_CASH_NAME("大盘猜涨跌", "数字货币名称"),

    GAME_TYPE_01("01", "球赛"),
    GAME_TYPE_02("02", "猜涨跌"),
    GAME_STATUS_01("01", "未开始"),
    GAME_STATUS_02("02", "进行中"),
    GAME_STATUS_03("03", "已结束"),
    GAME_STATUS_04("04", "状态异常"),

    GAME_PROPERTIES_WIN("win", "胜"),
    GAME_PROPERTIES_LOSE("lose", "负"),
    GAME_PROPERTIES_DRAW("draw", "平"),
    GAME_PROPERTIES_EXEC("exec", "异常"),


    ORDER_STATUS_01("01", "已投注"),
    ORDER_STATUS_02("02", "已完成"),


    SMS_SWITCH_ON("on", "短信开关开"),
    SMS_SWITCH_OFF("off", "短信开关关"),

    PAY_NO_PREFIX("1", "交易流水前缀"),
    ORDER_NO_PREFIX("2", "订单流水前缀"),
    ACCOUNT_NO_PREFIX("AC", "资金账号前缀");

    public static List<DictEnum> huobiSymbol = new ArrayList<>();

    static {
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_USDT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ETH);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_LTC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ETC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_EOS);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_OMG);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_DASH);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_XRP);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ZEC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_BLZ);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ENG);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_EDU);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_WPR);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_MTX);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_MTN);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_SNC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_LSK);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_STK);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_HT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ELA);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_SRN);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ZLA);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_TRX);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_OCN);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_LUN);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_IOST);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_HSR);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_SMT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_LET);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_SWFTC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_WAX);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ELF);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_MDS);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_TNB);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_NAS);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_BTM);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ITC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_THETA);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_WICC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_GNX);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_VEN);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_DTA);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_MANA);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_QASH);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_PROPY);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_SNT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_QUN);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_QTUM);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_DAT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_TNT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_CMT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_YEE);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_GAS);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_AIDOC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_STORJ);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_XEM);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_PAY);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_NEO);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_CVC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_QSP);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_TOPC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_RCN);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_AST);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_BAT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_DBC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_RPX);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ACT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ICX);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_KNC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_MCO);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ZRX);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_MTL);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_GNT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_REQ);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_RDN);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_SALT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_MEE);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ZIL);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_CHAT);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_POWR);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_DGD);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_APPC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_OST);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_SOC);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_EKO);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_LINK);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_UTK);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_EVX);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_ADX);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_BCD);
        huobiSymbol.add(MARKEY_HUOBI_SYMBOL_BTC_BCX);
    }

    private String code;

    private String value;

    DictEnum(String code, String value) {
        this.code = code;
        this.value = value;
    }


    public String getCode() {
        return code;
    }

    public String getValue() {
        return value;
    }
}
