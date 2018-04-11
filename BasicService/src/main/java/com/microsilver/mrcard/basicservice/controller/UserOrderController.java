/**
 * 
 */
package com.microsilver.mrcard.basicservice.controller;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.microsilver.mrcard.basicservice.dto.FreightDto;
import com.microsilver.mrcard.basicservice.dto.RespBaseDto;
import com.microsilver.mrcard.basicservice.model.FxSdCarriageOrder;
import com.microsilver.mrcard.basicservice.model.FxSdSysCarriageDispatch;
import com.microsilver.mrcard.basicservice.model.enums.EWarning;
import com.microsilver.mrcard.basicservice.service.OrderService;
import com.microsilver.mrcard.basicservice.service.UserCarriageDispatchService;
import com.microsilver.mrcard.basicservice.utils.MapDistance;
import com.microsilver.mrcard.basicservice.utils.TimeBetween;
import com.microsilver.mrcard.basicservice.utils.TwoDateBetween;

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
			@ApiImplicitParam(name = "courrtPage", value = "当前页"),
			@ApiImplicitParam(name = "pageSize", value = "每页需要显示的数量"),
			@ApiImplicitParam(name = "memberId", value = "用户id"),
			@ApiImplicitParam(name = "ordersn", value = "订单号"), 
			@ApiImplicitParam(name = "status", value = "订单状态;(1:待支付2:待抢单,3待取货,4.待收货,5.待评价.6.订单完成,7.等待退款8:退款完成,9:订单取消,-1:订单已锁定）为空则查全部数据") 
			})
	@RequestMapping(value="/getOrderInfo")
	@ResponseBody
	public RespBaseDto<List<FxSdCarriageOrder>> getOrderInfo(
			String courrtPage,
			String pageSize,
			String memberId,
			String ordersn,
			String status
			) {
		RespBaseDto<List<FxSdCarriageOrder>> result = new RespBaseDto<List<FxSdCarriageOrder>>();
		try {
			FxSdCarriageOrder fxSdCarriageOrder = new FxSdCarriageOrder();
			fxSdCarriageOrder.setMemberId(Long.parseLong(memberId));
			fxSdCarriageOrder.setOrdersn(ordersn);
			fxSdCarriageOrder.setStatus(Byte.valueOf(status));
			//通过页数查询多少条数据出来
			List<FxSdCarriageOrder> orderList = orderService.selectOrderByPage(fxSdCarriageOrder,Integer.valueOf(courrtPage),Integer.valueOf(pageSize));
			result.setData(orderList);
			result.setMessage("ok");
			result.setState(200);
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
			@ApiImplicitParam(name = "area_code", value ="订单所在区域" )
			})
	@RequestMapping(value="/getDispatchPrice")
	@ResponseBody
	public RespBaseDto<FreightDto> getDispatchPrice(
			@RequestParam(value="startLat",required = false) String startLat,
			@RequestParam(value="startLng",required = false) String startLng,
			String endLat,
			String endLng,
			String weight,
			String areaCode
			) {
		RespBaseDto<FreightDto> result = new RespBaseDto<FreightDto>();
		FreightDto freightDto = new FreightDto();
		try {
			//map价格分析
			Map<String,BigDecimal> map = new HashMap<String,BigDecimal>();
			
			//总价格
			Integer SumBigDecimal = 0;
			//先查询到代理商的规则数据
			FxSdSysCarriageDispatch dispatch = userCarriageDispatchService.selectDispatchByAreaId(areaCode);
			System.out.println(dispatch.toString());
				//传进来的重量
				BigDecimal weightBigDecimal = new BigDecimal(weight);
				//默认价格8元
				Integer defaultPrice = 8;
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
				//与当前时间进行对比
				String morning = AgentBeginTime+":00:00";
				String night = AgentendTime+":00:00";
				boolean inDate = TimeBetween.isInDate(new Date(), morning, night);
				
				if(inDate) {
					//正常价格(时间不加价)
					SumBigDecimal+=defaultPrice;
					map.put("basicPrice", new BigDecimal(defaultPrice));
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
						int intValue = weightPrice.intValue();
						SumBigDecimal+=intValue;
						map.put("weightPrice", weightPrice);
					}
					//距离作对比
					if(startLat==null||startLng==null||startLat.equals("")||startLng.equals("")) {
						//如果起点经纬度为空,则订单所在范围公里默认是3km内;
						//不需要加价
					}else {
						if(startLat!=null&&startLng!=null&&endLat!=null&&endLng!=null) {
							//起点经度,起点纬度,终点经度,终点纬度;(x,y)地图上获取的经纬度,x是纬度,y是经度;
							Double distance = Double.valueOf(MapDistance.getDistance(startLat, startLng,endLat, endLng));
							System.out.println("distance:"+distance);
							int compareTo2 = doubleValue.compareTo(distance);
							if(compareTo2 > 0) {
								//不需要加价
							}else {
								//距离加价
								long round = Math.round(distance-doubleValue);
								System.out.println("doubleValue:"+doubleValue);
								System.out.println("round:"+round);
								BigDecimal distancePrice = new BigDecimal(round).multiply(mileageMarkup);
								System.out.println("distancePrice:"+distancePrice);
								SumBigDecimal += distancePrice.intValue();
								map.put("distancePrice", distancePrice);
							}
						}
					}
				}else {
					//基础起步价
					SumBigDecimal+=defaultPrice;
					map.put("basicPrice", new BigDecimal(defaultPrice));
					//夜间加价
					BigDecimal Nightprice = dispatch.getNightMarkup();
					SumBigDecimal+= Nightprice.intValue();
					map.put("nightPrice", Nightprice);
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
						SumBigDecimal+= weightPrice.intValue();
						map.put("weightPrice", weightPrice);
					}
					//距离作对比
					if(startLat==null||startLng==null||startLat.equals("")||startLng.equals("")) {
						//如果起点经纬度为空,则订单所在范围公里默认是3km内;
						//不需要加价
					}else {
						if(startLat!=null&&startLng!=null&&endLat!=null&&endLng!=null) {
							//起点经度,起点纬度,终点经度,终点纬度;(x,y)地图上获取的经纬度,x是纬度,y是经度;
							Double distance = Double.valueOf(MapDistance.getDistance(startLat, startLng,endLat, endLng));
							System.out.println("distance:"+distance);
							int compareTo2 = doubleValue.compareTo(distance);
							if(compareTo2 > 0) {
								//不需要加价
							}else {
								//距离加价
								long round = Math.round(distance-doubleValue);
								System.out.println("doubleValue:"+doubleValue);
								System.out.println("round:"+round);
								BigDecimal distancePrice = new BigDecimal(round).multiply(mileageMarkup);
								System.out.println("distancePrice:"+distancePrice);
								SumBigDecimal += distancePrice.intValue();
								map.put("distancePrice", distancePrice);
							}
						}
					}
				}
				for ( String key : map.keySet()) {
					System.out.println("key:"+key+"  value:"+map.get(key));
				}
				freightDto.setSumBigDecimal(SumBigDecimal);
				freightDto.setMap(map);
				result.setData(freightDto);
				result.setMessage("ok");
				result.setState(200);
		}  catch (Exception e) {
			logger.error("getDispatchPrice error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}



	@ApiOperation(value = "获取用户未完成的订单", httpMethod = "POST")
	@ApiImplicitParams({
			@ApiImplicitParam(name = "member_id", value = "用户id"), 
			@ApiImplicitParam(name = "service_desc", value = "订单服务描述"), 
			@ApiImplicitParam(name = "dispatch_price", value = "运费"), 
			@ApiImplicitParam(name = "tip_price", value = "小费"), 
			@ApiImplicitParam(name = "start_name", value = "起点姓名"), 
			@ApiImplicitParam(name = "start_phone", value = "起点电话"), 
			@ApiImplicitParam(name = "start_address", value = "起点地址"), 
			@ApiImplicitParam(name = "start_lat", value = "起点经度"), 
			@ApiImplicitParam(name = "start_lng", value = "起点纬度"), 
			@ApiImplicitParam(name = "end_name", value = "终点姓名"), 
			@ApiImplicitParam(name = "end_phone", value = "终点电话"), 
			@ApiImplicitParam(name = "end_address", value = "终点地址"), 
			@ApiImplicitParam(name = "end_lat", value = "终点经度"), 
			@ApiImplicitParam(name = "end_lng", value = "终点纬度"), 
			@ApiImplicitParam(name = "weight", value = "重量"), 
			@ApiImplicitParam(name = "remark", value = "备注"), 
			@ApiImplicitParam(name = "area_code", value = "订单所在区域编号(区县级)"), 
			@ApiImplicitParam(name = "pay_type", value = "支付类型(支付类型 1为余额支付 21 微信支付 22 支付宝支付)"), 
			@ApiImplicitParam(name = "order_type", value = "订单类型(1.帮我买,2帮我取,3帮我送)"), 
			})
	@RequestMapping(value="/createOrder")
	@ResponseBody
	public RespBaseDto<Object> createOrder(
			String memberId,
			String serviceDesc,
			String dispatchPrice,
			String tipPrice,
			String startName,
			String startPhone,
			String startAddress,
			String startLat,
			String startLng,
			String endName,
			String endPhone,
			String endAddress,
			String endLat,
			String endLng,
			String weight,
			String remark,
			String areaCode,
			String payType,
			String orderType
			) {
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			RespBaseDto<FreightDto> price = getDispatchPrice(startLat,startLng,endLat,endLng,weight,areaCode);
			Integer sumBigDecimal = price.getData().getSumBigDecimal();
			//校验运费是否相等
			if(Integer.valueOf(dispatchPrice).equals(sumBigDecimal)) {
				FxSdCarriageOrder order = new FxSdCarriageOrder();
				order.setDispatchPrice(new BigDecimal(sumBigDecimal));
				//订单号
				order.setOrdersn(UUID.randomUUID()+String.valueOf(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(new Date().toLocaleString()).getTime()));
				order.setMemberId(Long.parseLong(memberId));
				order.setServiceDesc(serviceDesc);
				order.setTipPrice(new BigDecimal(sumBigDecimal));
				order.setStartName(startName);
				order.setStartPhone(startPhone);
				order.setStartAddress(startAddress);
				order.setStartLat(startLat);
				order.setStartLng(startLng);
				order.setEndName(endName);
				order.setEndPhone(endPhone);
				order.setEndAddress(endAddress);
				order.setEndLat(endLat);
				order.setEndLng(endLng);
				order.setWeight(Integer.valueOf(weight));
				order.setAreaCode(Integer.valueOf(areaCode));
				order.setPayType(Byte.valueOf(payType));
				order.setOrderType(Byte.valueOf(orderType));
			}
			
			
		}  catch (Exception e) {
			logger.error("createOrder error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	

	
}
