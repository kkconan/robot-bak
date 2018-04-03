package com.money.game.robot.controller;

import com.money.game.basic.component.ext.web.BaseController;
import com.money.game.core.constant.ResponseData;
import com.money.game.robot.biz.OrderBiz;
import com.money.game.robot.dto.client.OrderDto;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @author conan
 *         2018/3/28 10:40
 **/
@RestController
@Api(value = "order", description = "订单API")
@RequestMapping(value = "/api/order", produces = "application/json;charset=UTF-8")
public class OrderController extends BaseController {

    @Autowired
    private OrderBiz orderBiz;

    @RequestMapping(value = "/realOrderList", method = RequestMethod.POST)
    @ApiOperation(value = "实时订单列表", notes = "", httpMethod = "POST")
    @ApiImplicitParams({@ApiImplicitParam(name = "dto", value = "实时订单查询参数", required = true, paramType = "body", dataType = "OrderDto")})
    @ResponseBody
    public ResponseData realOrderList(@RequestBody OrderDto dto) {
        String userId = this.getLoginUser();
        return orderBiz.findRealOrderList(dto, userId);
    }


    @RequestMapping(value = "/limitOrderList", method = RequestMethod.POST)
    @ApiOperation(value = "限价单列表", notes = "", httpMethod = "POST")
    @ApiImplicitParams({@ApiImplicitParam(name = "dto", value = "限价单查询参数", required = true, paramType = "body", dataType = "OrderDto")})
    @ResponseBody
    public ResponseData limitOrderList(@RequestBody OrderDto dto) {
        String userId = this.getLoginUser();
        return orderBiz.findLimitOrderList(dto, userId);
    }

    @RequestMapping(value = "/cancelOrder", method = RequestMethod.GET)
    @ApiOperation(value = "撤销订单", notes = "", httpMethod = "GET")
    @ApiImplicitParams({@ApiImplicitParam(name = "oid", value = "oid", required = true, paramType = "query", dataType = "String")})
    @ResponseBody
    public ResponseData cancelOrder(String oid) {
        this.getLoginUser();
        return orderBiz.cancelOrder(oid);
    }
}