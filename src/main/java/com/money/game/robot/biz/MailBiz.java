package com.money.game.robot.biz;

import com.money.game.core.util.StrRedisUtil;
import com.money.game.core.util.StringUtil;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.entity.OrderEntity;
import com.money.game.robot.entity.UserEntity;
import com.money.game.robot.mail.MailQQ;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * @author conan
 *         2018/4/12 13:34
 **/
@Component
@Slf4j
public class MailBiz {

    @Autowired
    private RedisTemplate<String, String> redis;

    private static final String BLANCE_NOTIFY_KEY = "blance_notify_key_";

    /**
     * 搬砖单交易成功邮件通知
     */
    public void transToEmailNotify(OrderEntity orderEntity, UserEntity userEntity) {
        if (userEntity == null || StringUtil.isEmpty(userEntity.getNotifyEmail())) {
            log.info("email address is empty...");
            return;
        }
        String subject = orderEntity.getMarketType() + " " + orderEntity.getSymbol() + " " + orderEntity.getType() + " success notify";

        String content = orderEntity.getMarketType() + " " + orderEntity.getModel() + " orderId=" + orderEntity.getOrderId() + " " + orderEntity.getSymbol() + " " +
                orderEntity.getType() + " success. price is " + orderEntity.getPrice().setScale(8, BigDecimal.ROUND_DOWN) + ",amount is " +
                orderEntity.getAmount().setScale(8, BigDecimal.ROUND_DOWN) + " and totalToUsdt is " + orderEntity.getTotalToUsdt().setScale(8, BigDecimal.ROUND_DOWN);
        if (orderEntity.getType().equals(DictEnum.ORDER_TYPE_SELL_LIMIT.getCode())) {
            content = content + " and relating buyorderId=" + orderEntity.getBuyOrderId();
        }
        MailQQ.sendEmail(subject, content, userEntity.getNotifyEmail());
    }

    /**
     * 余额不足提醒
     */
    public void balanceToEmailNotify(UserEntity userEntity, String baseQuote, String marketType) {
        if (userEntity == null || StringUtil.isEmpty(userEntity.getNotifyEmail())) {
            log.info("email address is empty...");
            return;
        }
        if (StrRedisUtil.get(redis, BLANCE_NOTIFY_KEY + marketType + baseQuote) == null) {
            log.info("发送余额不足提醒,userId={},baseQuote={},marketType={}", userEntity.getOid(), baseQuote, marketType);
            String subject = marketType + " " + baseQuote + " balance not enough";
            String content = marketType + " " + baseQuote + " balance not enough,please deposit.";
            MailQQ.sendEmail(subject, content, userEntity.getNotifyEmail());
            StrRedisUtil.setEx(redis, BLANCE_NOTIFY_KEY + marketType + baseQuote, 7200, baseQuote);
        }
    }
}
