/**
 * 
 */
package com.microsilver.mrcard.basicservice.controller;

import java.math.BigDecimal;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.microsilver.mrcard.basicservice.dto.RespBaseDto;
import com.microsilver.mrcard.basicservice.model.FxSdCarriageOrder;
import com.microsilver.mrcard.basicservice.model.FxSdSysCarriageDispatch;
import com.microsilver.mrcard.basicservice.model.enums.EWarning;
import com.microsilver.mrcard.basicservice.service.OrderService;
import com.microsilver.mrcard.basicservice.service.UserCarriageDispatchService;
import com.microsilver.mrcard.basicservice.utils.TwoDateBetween;
import com.microsilver.mrcard.basicservice.utils.TwoPointToDistanceUtils;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

/**
 * 
 * @Name com.microsilver.mrcard.basicservice.controller.UserOrderController
 * @Description 
 * 
 * @Author huwei
 * @Version 2018年4月9日 下午2:33:22
 * @Copyright  Micro Silver-SuperDelivery
 *
 */
@Api(value="/api/userOrder", description= "用户订单相关接口")
@Controller
@RequestMapping(value = "/api/userOrder")
public class UserOrderController extends BaseController {
			
	@Autowired
	private OrderService orderService;
	
	@Autowired
	private UserCarriageDispatchService userCarriageDispatchService;
	
	@Autowired
	private TwoDateBetween twoDateBetween;
	
	
	@ApiOperation(value = "获取用户未完成的订单", httpMethod = "POST")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "member_id", value = "用户id") 
			})
	@RequestMapping(value="/getOutStandingOrderNumber")
	@ResponseBody
	public RespBaseDto<Object> getOutStandingOrderNumber(
			String memberId
			) {
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			if(memberId!=null) {
				FxSdCarriageOrder orderByMemberId = orderService.selectOrderByMemberId(memberId);
				if(orderByMemberId.getStatus()<6) {
					result.setData(orderByMemberId.getOrdersn());
					result.setMessage("ok");
					result.setState(200);
				}
			}
		}  catch (Exception e) {
			logger.error("GetDeliversPosition error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	

	@ApiOperation(value = "获取用户跑腿订单数据", httpMethod = "POST")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "pageNum", value = "页数"),
			@ApiImplicitParam(name = "memberId", value = "用户id"),
			@ApiImplicitParam(name = "ordersn", value = "订单号"), 
			@ApiImplicitParam(name = "status", value = "订单状态;(1:待支付2:待抢单,3待取货,4.待收货,5.待评价.6.订单完成,7.等待退款8:退款完成,9:订单取消,-1:订单已锁定）为空则查全部数据") 
			})
	@RequestMapping(value="/getOrderInfo")
	@ResponseBody
	public RespBaseDto<Object> getOrderInfo(
			String PageNum,
			String memberId,
			String ordersn,
			String status
			) {
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			
			//通过页数查询多少条数据出来
			FxSdCarriageOrder order = orderService.selectOrderByOrdersnAndMemberId(memberId,ordersn);
			
			//
			
			
			
		}  catch (Exception e) {
			logger.error("getOrderInfo error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	
	@ApiOperation(value = "获取用户订单中的运费", httpMethod = "POST")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "start_lat", value = "起点经度",defaultValue =""),
			@ApiImplicitParam(name = "start_lng", value = "起点纬度"),
			@ApiImplicitParam(name = "end_lat", value = "终点经度"), 
			@ApiImplicitParam(name = "end_lng", value = "终点纬度") ,
			@ApiImplicitParam(name = "weight", value = "重量") ,
			@ApiImplicitParam(name = "area_code", value ="订单所在区域" ),
			@ApiImplicitParam(name = "ordersn", value ="下订单时间" )
			})
	@RequestMapping(value="/getDispatchPrice")
	@ResponseBody
	public RespBaseDto<Object> getDispatchPrice(
			String startLat,
			String startLng,
			String endLat,
			String endLng,
			String weight,
			String areaCode,
			String ordersn
			) {
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			//先查询到代理商的规则数据
			FxSdSysCarriageDispatch dispatch = userCarriageDispatchService.selectDispatchByAreaId(areaCode);
			//传进来的重量
			BigDecimal weightBigDecimal = new BigDecimal(weight);
			//默认重量8kg
			BigDecimal defaultWeight = new BigDecimal(8.00);
			//默认公里数
			Double doubleValue = dispatch.getBaseMileage().doubleValue();
			//超过默认公里,每公里需要添加的费用
			BigDecimal mileageMarkup = dispatch.getMileageMarkup();
			//时间作对比
			Short AgentBeginTime = dispatch.getBeginTime();
			Short AgentendTime = dispatch.getEndTime();
				//早上8:00-晚上22:00
				//获取订单下单时间
				FxSdCarriageOrder order = orderService.selectOrderByOrdersn(ordersn);
				Date OrdercreateTime = order.getCreateTime();
				String morning = "0"+AgentBeginTime+":00:00";
				String night = AgentendTime+":00:00";
				boolean inDate = TwoDateBetween.isInDate(OrdercreateTime, morning, night);
				if(inDate) {
					//正常价格(时间不加价)
					
					//重量做对比
					  //超过8kg每公斤需要增加的价格
					BigDecimal weightMarkup = dispatch.getWeightMarkup();
					int compareTo = defaultWeight.compareTo(weightBigDecimal);
					if( compareTo > 0) {
						//重量不加价
					}else if( compareTo < 0) {
						BigDecimal subtract = weightBigDecimal.subtract(defaultWeight);
						//重量加价的价格
						BigDecimal weightPrice = subtract.multiply(weightMarkup);
					}
					
					//距离作对比
					Double distance = TwoPointToDistanceUtils.getDistance(Double.parseDouble(startLat), Double.parseDouble(startLng), 
							Double.parseDouble(endLat), Double.parseDouble(endLng));
					int compareTo2 = doubleValue.compareTo(distance);
					if(compareTo2 > 0) {
						//不需要加价
					}else {
						//距离加价
						 long round = Math.round(distance-doubleValue);
						 BigDecimal distancePrice = new BigDecimal(round).subtract(mileageMarkup);
					}
					
				}else {
					//夜间加价
					BigDecimal Nightprice = dispatch.getNightMarkup();
					//重量做对比
					  //超过8kg每公斤需要增加的价格
					BigDecimal weightMarkup = dispatch.getWeightMarkup();
					int compareTo = defaultWeight.compareTo(weightBigDecimal);
					if( compareTo > 0) {
					  //重量不加价
					}else if( compareTo < 0) {
						BigDecimal subtract = weightBigDecimal.subtract(defaultWeight);
					  //重量加价的价格
						BigDecimal weightPrice = subtract.multiply(weightMarkup);
					}
				}
			
			
			
			
			
		}  catch (Exception e) {
			logger.error("getOrderInfo error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}





	
}
