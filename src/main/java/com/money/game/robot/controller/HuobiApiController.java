//package com.money.game.robot.controller;
//
//import com.money.game.robot.biz.AccountBiz;
//import io.swagger.annotations.Api;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
///**
// * @author conan
// *         2018/1/8 17:27
// **/
//
//@RestController
//@Api(value = "huobi", description = "火币API")
//@RequestMapping(value = "/api/huobi", produces = "application/json;charset=UTF-8")
//public class HuobiApiController{
//
//    @Autowired
//    private AccountBiz paymentBiz;
//
//
//    /**
//     * 列表
//     */
////    @RequestMapping(value = "/list", method = RequestMethod.POST)
////    @ApiOperation(value = "列表", notes = "", httpMethod = "POST")
////    @ApiImplicitParams({@ApiImplicitParam(name = "dto", value = "列表参数", required = true, paramType = "body", dataType = "PaymentQueryDto")})
////    @ResponseBody
////    public ResponseData list(@Valid @RequestBody PaymentQueryDto dto) {
////        this.getLoginUser();
////        return paymentBiz.list(dto);
////    }
//
//
//
//}
