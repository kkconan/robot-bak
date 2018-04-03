package com.money.game.robot.biz;

import com.money.game.core.constant.ResponseData;
import com.money.game.core.util.Digests;
import com.money.game.core.util.PwdUtil;
import com.money.game.core.util.StringUtil;
import com.money.game.robot.constant.DictEnum;
import com.money.game.robot.constant.ErrorEnum;
import com.money.game.robot.dto.client.ModifyUserInfoDto;
import com.money.game.robot.dto.client.UserRegisterDto;
import com.money.game.robot.entity.UserEntity;
import com.money.game.robot.exception.BizException;
import com.money.game.robot.service.UserService;
import com.money.game.robot.vo.LoginVo;
import com.money.game.robot.vo.UserVo;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @author conan
 *         2018/3/22 10:15
 **/
@Component
public class UserBiz {

    @Autowired
    private UserService userService;

    public List<UserEntity> findAllByNormal() {
        return userService.findAllByStatus(DictEnum.USER_STATUS_NORMAL.getCode());
    }

    public ResponseData getUserInfo(String userId) {
        UserEntity user = userService.findOne(userId);
        UserVo userVo = new UserVo();
        BeanUtils.copyProperties(user, userVo);
        return ResponseData.success(userVo);
    }

    public ResponseData modify(ModifyUserInfoDto dto, String userId) {
        UserEntity user = userService.findOne(userId);
        BeanUtils.copyProperties(dto, user);
        if (!StringUtil.isEmpty(dto.getPassword())) {
            user.setSalt(Digests.genSalt());
            user.setPassword(PwdUtil.encryptPassword(dto.getPassword(), user.getSalt()));
        }
        userService.save(user);
        return ResponseData.success();
    }

    public UserEntity findById(String userId) {
        return userService.findOne(userId);
    }

    public LoginVo login(String phone, String password) {
        UserEntity user = userService.findByPhone(phone);
        if (user == null) {
            throw new BizException(ErrorEnum.USER_NOT_FOUND);
        }
        boolean result = PwdUtil.checkPassword(password, user.getPassword(), user.getSalt());
        if (!result) {
            throw new BizException(ErrorEnum.USER_PWD_FAIL);
        }
        LoginVo vo = new LoginVo();
        vo.setUserId(user.getOid());
        vo.setPhone(user.getPhone());

        return vo;
    }

    public UserEntity register(UserRegisterDto dto) {
        UserEntity user = new UserEntity();
        user.setPhone(dto.getPhone());
        user.setSalt(Digests.genSalt());
        user.setPassword(PwdUtil.encryptPassword(dto.getUserPwd(), user.getSalt()));
        user.setStatus(DictEnum.USER_STATUS_NORMAL.getCode());
        return userService.save(user);
    }
}

