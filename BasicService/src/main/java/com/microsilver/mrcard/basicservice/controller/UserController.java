package com.microsilver.mrcard.basicservice.controller;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.codec.digest.DigestUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.microsilver.mrcard.basicservice.dto.RespBaseDto;
import com.microsilver.mrcard.basicservice.model.FxSdFinanceCustomer;
import com.microsilver.mrcard.basicservice.model.FxSdSysCarriageDispatch;
import com.microsilver.mrcard.basicservice.model.FxSdUserAddress;
import com.microsilver.mrcard.basicservice.model.FxSdUserDeliverAdditional;
import com.microsilver.mrcard.basicservice.model.FxSdUserMember;
import com.microsilver.mrcard.basicservice.model.FxSdUserPreReg;
import com.microsilver.mrcard.basicservice.model.enums.EWarning;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

import com.microsilver.mrcard.basicservice.service.OrderService;
import com.microsilver.mrcard.basicservice.service.SupserDeliveryInfoService;
import com.microsilver.mrcard.basicservice.service.UserCarriageDispatchService;
import com.microsilver.mrcard.basicservice.service.UserService;
import com.microsilver.mrcard.basicservice.utils.ArrayRandom;
import com.microsilver.mrcard.basicservice.utils.MapDistance;

/**
 * 
 * @Name com.microsilver.mrcard.basicservice.controller.UserController
 * @Description 
 * 
 * @Author huwei
 * @Version 2018年3月28日 上午10:16:02
 * @Copyright  Micro Silver-SuperDelivery
 *
 */
@Api(value = "/api/User", description= "用户基础API")
@Controller
@RequestMapping(value = "/api/User")
public class UserController extends BaseController {
	@Autowired
	private UserService userService;
	
	@Autowired
	private StringRedisTemplate stringRedisTemplate;
	
	@Autowired
	private SupserDeliveryInfoService supserDeliveryInfoService;
	
	@Autowired
	private UserCarriageDispatchService userCarriageDispatchService;
	
	@Autowired
	private OrderService orderService;
	
	
	
	
	
	@ApiOperation(value = "用户注册(手机号密码注册)", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "mobile", value = "手机号", required = true, paramType = "body"),
		@ApiImplicitParam(name = "password", value = "密码", required = true, paramType = "body"),
		@ApiImplicitParam(name = "checkCode", value = "验证码", required = true, paramType = "body")})
	@RequestMapping(value = "/UserRegister")
	@ResponseBody
	public RespBaseDto<Object> UserRegister(
			String mobile,
			String checkCode,
			String password){
		 RespBaseDto<Object> result = new RespBaseDto<Object>();
		 if(mobile.trim().equals("")||mobile==null) {
			 	result.setMessage("手机号为空,请重新输入");
				result.setState(1);
				return result;
		 }
		 if(password==null||password.trim().trim().equals("")) {
			 	result.setMessage("密码不能为空,请重新输入");
				result.setState(2);
				return result;
		 }
		 FxSdUserMember selectUserByMobile = userService.selectUserByMobile(mobile);
		 if(selectUserByMobile!=null) {
			 result.setMessage("您的手机号已注册,请重新选择");
			 result.setState(3);
			 return result;
		 }
		 if(checkCode==null) {
			 result.setMessage("您的验证码为空,请重新输入");
			 result.setState(3);
			 return result;
		 }
		 //对比验证码
		 String redisCheckCode = stringRedisTemplate.opsForValue().get(mobile);
			if (redisCheckCode == null || redisCheckCode.trim().equals("")) {
				result.setMessage("验证码未获取,请联系管理员");
				result.setState(4);
				return result;
			}
			int valueOf = Integer.valueOf(redisCheckCode);
			int valueOf2 = Integer.valueOf(checkCode);
			if (valueOf != valueOf2) {
				result.setMessage("验证码有误,请联系管理员");
				result.setState(5);
				return result;
			}
		 //创建一个用户对象
		 FxSdUserMember fxSdUserMember = new FxSdUserMember();
		 fxSdUserMember.setMobile(mobile);
		 fxSdUserMember.setNickname(mobile.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
		 fxSdUserMember.setPwd(DigestUtils.md5Hex(password));
		 //默认信用等级分80
		 fxSdUserMember.setServicescor(80);
		 fxSdUserMember.setLevelscore(1);
		 fxSdUserMember.setCreatetime((int) new Date().getTime());
		 userService.insertUser(fxSdUserMember);
		 //查询该手机号在邀请表中是否存在,存在就属于被邀请对象,不存在则正常注册无推荐奖金.
		 //如果是,那么则修改status状态0变出1;0表示被邀请人邀请的用户(预注册),1是表示被邀请人已经注册完成,但是未发放奖金,2是表示被邀请人已经注册完成,已发送完奖金;
		 FxSdUserPreReg checkIsInvitation = userService.checkIsInvitation(Long.parseLong(mobile));
		 if(checkIsInvitation!=null) {
			 if(checkIsInvitation.getStatus()==0) {
				 checkIsInvitation.setStatus((byte)1);
				 userService.updateStatus(checkIsInvitation.getRefereeId(),fxSdUserMember);
			 }
		 }
		 //添加到数据库
		 result.setData(fxSdUserMember);
		 result.setMessage("用户添加成功");
		 result.setState(200);
	
		 return result;
			
	}
	
	@ApiOperation(value = "用户修改密码(新密码旧密码)和忘记密码(手机验证码)", httpMethod = "POST")
	@ApiImplicitParams({ 
			@ApiImplicitParam(name = "mobile", value = "手机号", required = true, paramType = "form"),
			@ApiImplicitParam(name = "password", value = "用户新密码", required = true, paramType = "form"),
			@ApiImplicitParam(name = "code", value = "旧密码或者验证码", required = true, paramType = "form"),
			@ApiImplicitParam(name = "type", value = "类型,1修改;2忘记", required = true, paramType = "form") })
	@RequestMapping(value = "/UpDatePwd")
	@ResponseBody
	public RespBaseDto<Object> UpDatePwd(
			String mobile, 
			String password, 
			String code, 
			String type
			) {
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			if(mobile!=null) {
				FxSdUserMember selectUserByMobile = userService.selectUserByMobile(mobile);
				if(selectUserByMobile!=null) {
					//type类型是1则是修改密码,此刻的coke是旧密码
					if(type!=null&&type.equals("1")&&password!=null) {
						if(DigestUtils.md5Hex(code).equals(selectUserByMobile.getPwd())) {
							selectUserByMobile.setPwd(DigestUtils.md5Hex(password));
							userService.updatePwd(selectUserByMobile,mobile);
							System.out.println(selectUserByMobile.toString());
							result.setData(selectUserByMobile);
							result.setMessage("Ok");
							result.setState(200);
						}
					}
					//type类型是1则是修改密码,此刻的coke是验证码
					String redisCheckCode = stringRedisTemplate.opsForValue().get(mobile);
					if(type!=null&&type.equals("2")&&password!=null) {
						if(Integer.parseInt(code)==Integer.parseInt(redisCheckCode)) {
							selectUserByMobile.setPwd(DigestUtils.md5Hex(password));
							userService.updatePwd(selectUserByMobile,mobile);
							result.setData(selectUserByMobile);
							result.setMessage("Ok");
							result.setState(200);
						}
					}
				}
			}
			
		}catch(Exception e) {
			logger.error("UpDatePwd error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}

		return result;

	}
	
	@ApiOperation(value = "用户登陆(手机号,验证码登陆)", httpMethod = "POST")
	@ApiImplicitParams({ 
			@ApiImplicitParam(name = "mobile", value = "手机号", required = true, paramType = "form"),
			@ApiImplicitParam(name = "checkCode", value = "短信验证码", required = true, paramType = "form") })
	@RequestMapping(value = "/UserQuickLogin")
	@ResponseBody
	public RespBaseDto<Object> UserQuickLogin(String mobile, String checkCode) {
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		if (mobile == null || mobile.trim().equals("")) {
			result.setState(1);
			result.setMessage("手机号不能为空,请重新输入");
			return result;
		}
		if (checkCode == null || checkCode.trim().equals("")) {
			result.setState(5);
			result.setMessage("验证码不能为空,请重新输入");
			return result;
		}
		FxSdUserMember selectUserByMobile = userService.selectUserByMobile(mobile);
		if (selectUserByMobile == null) {
			result.setState(2);
			result.setMessage("手机号未注册,请注册后登陆");
			return result;
		}
		String redisCheckCode = stringRedisTemplate.opsForValue().get(mobile);
		if (!redisCheckCode.equals(checkCode)) {
			result.setState(3);
			result.setMessage("验证码有误,请重新输入");
			return result;
		}
		result.setState(200);
		result.setMessage("恭喜你,登陆成功");
		return result;
	}
	
	@ApiOperation(value = "用户登陆(手机号,密码登陆)", httpMethod = "POST")
	@ApiImplicitParams({ 
			@ApiImplicitParam(name = "mobile", value = "手机号", required = true, paramType = "body"),
			@ApiImplicitParam(name = "password", value = "密码", required = true, paramType = "body") })
	@RequestMapping(value = "/UserLogin")
	@ResponseBody
	public RespBaseDto<Object> UserLogin(
			String mobile, 
			String password
			) {
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			if(mobile!=null&&password!=null) {
				FxSdUserMember selectUserByMobile = userService.selectUserByMobile(mobile);
				if(selectUserByMobile!=null) {
					if(selectUserByMobile.getPwd().equals(DigestUtils.md5Hex(password))) {
						result.setData(selectUserByMobile);
						result.setMessage("ok");
						result.setState(200);
					}
				}
			}
		}catch(Exception e) {
			logger.error("UploadSuperDeliveryHeadImg error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		
		return result;
	}
	
	@ApiOperation(value = "用户id返回用户信息", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "userId", value = "用户id", required = true, paramType = "body") })
	@RequestMapping(value = "/GetUserInfo")
	@ResponseBody
	public RespBaseDto<Object> GetUserInfo(String userId){
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			if(userId!=null) {
				FxSdUserMember selectUserByMobile = userService.selectUserByUserId(userId);
				if(selectUserByMobile!=null) {
					result.setData(selectUserByMobile);
					result.setMessage("Ok");
					result.setState(200);
				}
			}
		}catch(Exception e) {
			logger.error("GetUserInfo error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	
	@ApiOperation(value = "修改用户手机", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "oldPhoneNumber", value = "用户旧手机", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "oldCheckCode", value = "用户旧手机验证码", required = true, paramType = "body"),
		@ApiImplicitParam(name = "newPhoneNumber", value = "用户新手机", required = true, paramType = "body"),
		@ApiImplicitParam(name = "newCheckCode", value = "用户新手机验证码", required = true, paramType = "body")})
	@RequestMapping(value = "/UpdateUserPhone")
	@ResponseBody
	public RespBaseDto<Object> UpdateUserPhone(
			String oldPhoneNumber,
			String oldCheckCode,
			String newPhoneNumber,
			String newCheckCode ){
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			if(oldPhoneNumber!=null) {
				FxSdUserMember selectUserByMobile = userService.selectUserByMobile(oldPhoneNumber);
				if(selectUserByMobile!=null) {
					String redisCheckCode = stringRedisTemplate.opsForValue().get(oldPhoneNumber);
					if(redisCheckCode!=null) {
						if(Integer.parseInt(oldCheckCode)==Integer.parseInt(redisCheckCode)) {
							FxSdUserMember selectUserByMobile2 = userService.selectUserByMobile(newPhoneNumber);
							if(selectUserByMobile2==null) {
								String redisNewPhoneCheckCode = stringRedisTemplate.opsForValue().get(newPhoneNumber);
								if(Integer.parseInt(redisNewPhoneCheckCode)==Integer.parseInt(newCheckCode)) {
									//通过旧手机查询到记录并修改为修手机
									selectUserByMobile.setMobile(newPhoneNumber);
									selectUserByMobile.setNickname(newPhoneNumber.replaceAll("(\\d{3})\\d{4}(\\d{4})", "$1****$2"));
									userService.updatePhone(selectUserByMobile,oldPhoneNumber);
									result.setData(selectUserByMobile);
									result.setMessage("Ok");
									result.setState(200);
								}
							}
						}
					}
				}
			}
			
		}catch(Exception e) {
			logger.error("UpdateUserPhone error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	
	@ApiOperation(value = "新增用户常用地址", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "mobile", value = "用户手机号", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "realname", value = "姓名", required = true, paramType = "body"),
		@ApiImplicitParam(name = "province", value = "省", required = true, paramType = "body"),
		@ApiImplicitParam(name = "city", value = "市", required = true, paramType = "body"),
		@ApiImplicitParam(name = "area", value = "县/区", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "street", value = "街道", required = true, paramType = "body"),
		@ApiImplicitParam(name = "address", value = "详细地址", required = true, paramType = "body"),
		@ApiImplicitParam(name = "lat", value = "纬度", required = true, paramType = "body"),
		@ApiImplicitParam(name = "lng", value = "经度", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "isdefault", value = "是否默认地址(0 非默认地址 1 默认地址)", required = true, paramType = "body"),
		@ApiImplicitParam(name = "memeberId", value = "用户Id", required = true, paramType = "body")})
	@RequestMapping(value = "/AddUserAdress")
	@ResponseBody
	public RespBaseDto<Object> AddUserAdress(
			String mobile,
			String realname,
			String province,
			String city,
			String area,
			String street,
			String address,
			String lat,
			String lng,
			String isdefault,
			String memeberId){
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			FxSdUserAddress fxSdUserAddress = new FxSdUserAddress();
			if(mobile!=null&&realname!=null&&province!=null&&city!=null&&area!=null&&street!=null
					&&address!=null&&lat!=null&&lng!=null&&memeberId!=null) {
				FxSdUserMember selectUserByMobile = userService.selectUserByUserId(memeberId);
				System.out.println(selectUserByMobile.toString());
				if(selectUserByMobile!=null) {
					//判断该用户是否只有一条常用地址,如果是,则设置这条地址为默认地址;
					fxSdUserAddress.setAddress(address);
					fxSdUserAddress.setProvince(province);
					fxSdUserAddress.setArea(area);
					fxSdUserAddress.setCity(city);
					fxSdUserAddress.setLat(lat);
					fxSdUserAddress.setLng(lng);
					fxSdUserAddress.setMemberId(Long.parseLong(memeberId));
					fxSdUserAddress.setMobile(mobile);
					fxSdUserAddress.setRealname(realname);
					fxSdUserAddress.setStreet(street);
					System.out.println(selectUserByMobile.toString());
					//判断是否是默认地址
					if(isdefault.equals("1")) {
						//查询所有订单
						List<FxSdUserAddress> listAddress = userService.selectUserAllAddress(memeberId);
						System.out.println(listAddress.toString());
						if(listAddress!=null) {
							for (FxSdUserAddress addressDefault : listAddress) {
								Byte isdefault2 = addressDefault.getIsdefault();
								//找到默认值为1的地址修改为0
								if(isdefault2==1) {
									addressDefault.setIsdefault((byte)0);
									userService.updateAddress(addressDefault, addressDefault.getId().toString());
								}
							}
						}
							System.out.println("11111111");
							//写入数据库
							fxSdUserAddress.setIsdefault((byte)1);
							userService.insertAddress(fxSdUserAddress);
						
					}else {
						//查询数据库中是否有默认地址
						//查询所有订单
						List<FxSdUserAddress> listAddress = userService.selectUserAllAddress(memeberId);
						
						//System.out.println(listAddress.toString());
						if(listAddress!=null) {
							for (FxSdUserAddress addressDefault : listAddress) {
								Byte isdefault2 = addressDefault.getIsdefault();
								//找到默认值为1的地址修改为0
								if(isdefault2==1) {
									//写入数据库
									fxSdUserAddress.setIsdefault((byte)0);
									userService.insertAddress(fxSdUserAddress);
								}else {
									//没有默认地址则设置第一条数据为默认地址
									FxSdUserAddress fxSdUserAddress2 = listAddress.get(0);
									fxSdUserAddress2.setIsdefault((byte)1);
									userService.updateAddress(fxSdUserAddress2, fxSdUserAddress2.getId().toString());
								}
							}
						}else {
							
							fxSdUserAddress.setIsdefault((byte)1);
							userService.insertAddress(fxSdUserAddress);
						}
						
					}
					result.setData(fxSdUserAddress);
					result.setMessage("Ok");
					result.setState(200);
				}
			}
		}catch(Exception e) {
			logger.error("AddUserAdress error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}

	@ApiOperation(value = "修改用户常用地址", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "mobile", value = "用户手机号", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "realname", value = "姓名", required = true, paramType = "body"),
		@ApiImplicitParam(name = "province", value = "省", required = true, paramType = "body"),
		@ApiImplicitParam(name = "city", value = "市", required = true, paramType = "body"),
		@ApiImplicitParam(name = "area", value = "县/区", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "street", value = "街道", required = true, paramType = "body"),
		@ApiImplicitParam(name = "address", value = "详细地址", required = true, paramType = "body"),
		@ApiImplicitParam(name = "lat", value = "经度", required = true, paramType = "body"),
		@ApiImplicitParam(name = "lng", value = "纬度", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "addressId", value = "用户地址id", required = true, paramType = "body"),
		@ApiImplicitParam(name = "memeberId", value = "用户Id", required = true, paramType = "body"),
		@ApiImplicitParam(name = "isdefault", value = "是否默认地址", required = true, paramType = "body")})
	@RequestMapping(value = "/UpdateUserAdress")
	@ResponseBody
	public RespBaseDto<Object> UpdateUserAdress(
			String mobile,
			String realname,
			String province,
			String city,
			String area,
			String street,
			String address,
			String lat,
			String lng,
			String addressId,
			String isdefault,
			String memeberId){
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			FxSdUserAddress fxSdUserAddress = new FxSdUserAddress();
			if(mobile!=null&&realname!=null&&province!=null&&city!=null&&area!=null&&street!=null
					&&address!=null&&lat!=null&&lng!=null&&memeberId!=null&&addressId!=null) {
				fxSdUserAddress.setMobile(mobile);
				fxSdUserAddress.setRealname(realname);
				fxSdUserAddress.setProvince(province);
				fxSdUserAddress.setCity(city);
				fxSdUserAddress.setArea(area);
				fxSdUserAddress.setAddress(address);
				fxSdUserAddress.setLat(lat);
				fxSdUserAddress.setLng(lng);
				fxSdUserAddress.setMemberId(Long.parseLong(memeberId));
				fxSdUserAddress.setStreet(street);
				fxSdUserAddress.setId(Integer.valueOf(addressId));
				//判断是否用户地址是默认地址
				if(isdefault.equals("1")) {
					//查询所有订单
					List<FxSdUserAddress> listAddress = userService.selectUserAllAddress(memeberId);
					if(listAddress!=null) {
						for (FxSdUserAddress addressDefault : listAddress) {
							Byte isdefault2 = addressDefault.getIsdefault();
							//找到默认值为1的地址修改为0
							if(isdefault2==1) {
								addressDefault.setIsdefault((byte)0);
								userService.updateAddress(addressDefault, addressDefault.getId().toString());
							}
						}
					}
					//写入数据库
					fxSdUserAddress.setIsdefault((byte)1);
					userService.updateAddress(fxSdUserAddress, addressId);
				}else {
					//查询数据库中是否有默认地址
					//查询所有订单
					List<FxSdUserAddress> listAddress = userService.selectUserAllAddress(memeberId);
					
					System.out.println(listAddress.toString());
					if(listAddress!=null) {
						for (FxSdUserAddress addressDefault : listAddress) {
							Byte isdefault2 = addressDefault.getIsdefault();
							//找到默认值为1的地址修改为0
							if(isdefault2==1) {
								//写入数据库
								fxSdUserAddress.setIsdefault((byte)0);
								userService.updateAddress(fxSdUserAddress, addressId);
							}else {
								//没有默认地址则设置第一条数据为默认地址
								FxSdUserAddress fxSdUserAddress2 = listAddress.get(0);
								fxSdUserAddress2.setIsdefault((byte)1);
								userService.updateAddress(fxSdUserAddress2, fxSdUserAddress2.getId().toString());
							}
						}
					}
				}
				
				result.setData(fxSdUserAddress);
				result.setMessage("Ok");
				result.setState(200);
				
			}
		}catch(Exception e) {
			logger.error("UpdateUserAdress error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	
	@ApiOperation(value = "删除用户常用地址", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "memberId", value = "用户Id", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "addressId", value = "用户地址Id", required = true, paramType = "body"),
		})
	@RequestMapping(value = "/deleteUserAdress")
	@ResponseBody
	public RespBaseDto<Object> deleteUserAdress(
			String memberId,
			String addressId
			){
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			
			//删除之后,当用户的默认地址没有,则当第一条地址设置为默认地址
			userService.deleteAddress(addressId);
			Map<Integer, Byte> map = new HashMap<Integer,Byte>();
			List<FxSdUserAddress> list = userService.selectUserAllAddress(memberId);
			for (FxSdUserAddress fxSdUserAddress : list) {
				map.put(fxSdUserAddress.getId(), fxSdUserAddress.getIsdefault());
				System.out.println(fxSdUserAddress.toString());
			}
			
			if(map.containsValue((byte)1)) {
				result.setMessage("OK");
				result.setState(200);
			}else{
				FxSdUserAddress fxSdUserAddress = list.get(0);
				fxSdUserAddress.setIsdefault((byte)1);
				userService.updateAddress(fxSdUserAddress, String.valueOf(fxSdUserAddress.getId()));
				result.setMessage("OK");
				result.setState(200);
			}
		}catch(Exception e) {
			logger.error("deleteUserAdress error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	
	
	@ApiOperation(value = "设置用户常用默认地址", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "memberId", value = "用户Id", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "addressId", value = "用户地址Id", required = true, paramType = "body"),
		})
	@RequestMapping(value = "/SetUpDefaultAddress")
	@ResponseBody
	public RespBaseDto<Object> SetUpDefaultAddress(
			String memberId,
			String addressId
			){
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			if(memberId!=null&&addressId!=null) {
				List<FxSdUserAddress> list = userService.selectUserAllAddress(memberId);
				for (FxSdUserAddress address : list) {
					if(address.getIsdefault()==1) {
						address.setIsdefault((byte) 0);
						userService.updateAddress(address, address.getId().toString());
					}
				}
				FxSdUserAddress fxSdUserAddress = new FxSdUserAddress();
				fxSdUserAddress.setIsdefault((byte) 1);
				userService.updateAddress(fxSdUserAddress, addressId);
				result.setMessage("ok");
				result.setState(200);
			}
		}catch(Exception e) {
			logger.error("SetUpDefaultAddress error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	
	
	@ApiOperation(value = "查询常用默认地址集合", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "memberId", value = "用户Id", required = true, paramType = "body") 
		})
	@RequestMapping(value = "/SerachUserAddressList")
	@ResponseBody
	public RespBaseDto<List<FxSdUserAddress>> SerachUserAddressList(
			String memberId
			){
		RespBaseDto<List<FxSdUserAddress>> result = new RespBaseDto<List<FxSdUserAddress>>();
		try {
			if(memberId!=null) {
				List<FxSdUserAddress> list = userService.selectUserAllAddress(memberId);
				result.setData(list);
				result.setMessage("ok");
				result.setState(200);
			}
		}catch(Exception e) {
			logger.error("SerachUserAddressList error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	
	@ApiOperation(value = "用户上传头像", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "userId", value = "用户Id", required = true, paramType = "body") ,
		@ApiImplicitParam(name = "avatar", value = "用户头像URL", required = true, paramType = "body") 
		})
	@RequestMapping(value = "/UploadUserHeadImg")
	@ResponseBody
	public RespBaseDto<Object> UploadUserHeadImg(
			String userId,
			String avatar 
			){
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			if(userId!=null) {
				FxSdUserMember user = userService.selectUserByUserId(userId);
				if(avatar!=null&&user!=null) {
					user.setAvatar(avatar);
					userService.updateAvatar(user);
					result.setData(user);
					result.setMessage("ok");
					result.setState(200);
				}
			}
		}catch(Exception e) {
			logger.error("UploadUserHeadImg error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	
	@ApiOperation(value = "用户昵称修改", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "userId", value = "用户Id", required = true, paramType = "body"), 
		@ApiImplicitParam(name = "nickName", value = "用户昵称", required = true, paramType = "body") 
		})
	@RequestMapping(value = "/UpdateMemberNickname")
	@ResponseBody
	public RespBaseDto<Object> UpdateMemberNickname(
			String userId,
			String nickName 
			){
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		try {
			if(userId!=null) {
				FxSdUserMember user = userService.selectUserByUserId(userId);
				if(nickName!=null&&user!=null) {
					user.setAvatar(nickName);
					userService.updateAvatar(user);
					result.setData(user);
					result.setMessage("ok");
					result.setState(200);
				}
			}
		}catch(Exception e) {
			logger.error("UploadUserHeadImg error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
	}
	
	@ApiOperation(value = "用户首页获取周围骑手信息", httpMethod = "POST")
	@ApiImplicitParams({ 
		@ApiImplicitParam(name = "lat", value = "经度", required = true, paramType = "body"), 
		@ApiImplicitParam(name = "lng", value = "纬度", required = true, paramType = "body"),
		@ApiImplicitParam(name = "areaCode", value = "区域编号", required = true, paramType = "body") 
		})
	@RequestMapping(value = "/GetDeliversPosition")
	@ResponseBody
	public RespBaseDto<Map<String, String>> GetDeliversPosition(
			String lat,
			String lng,
			String areaCode
			){
		RespBaseDto<Map<String, String>> result = new RespBaseDto<Map<String, String>>();
		try {
			//存放骑手的map
			Map<String, String> map = new HashMap<String,String>();
			//查询出fx_sd_user_deliver_additional所有的经度纬度
			List<FxSdUserDeliverAdditional> listSuperDelivery = supserDeliveryInfoService.selectSuperDelivery();
			if(listSuperDelivery!=null) {
				for (FxSdUserDeliverAdditional fxSdUserDeliverAdditional : listSuperDelivery) {
					System.out.println(fxSdUserDeliverAdditional.toString());
					//骑手于用户之间的距离
					
					double distance = Double.valueOf(MapDistance.getDistance(lat,lng, 
							fxSdUserDeliverAdditional.getLat(), fxSdUserDeliverAdditional.getLng()))*1000;
					//通过区域编号查询到该区域编号的默认公里数;
					 FxSdSysCarriageDispatch selectDispatchByAreaId = userCarriageDispatchService.selectDispatchByAreaId(areaCode);
					 Byte baseMileage =selectDispatchByAreaId.getBaseMileage();
					double doubleValue = baseMileage.doubleValue()*1000;
					
					System.out.println("distance: "+distance);
					System.out.println("doubleValue:"+doubleValue);
					
					
					Double doubleDistance = new Double(distance);
					Double doubleDoubleValue = new Double(doubleValue);
					int compareTo = doubleDistance.compareTo(doubleDoubleValue);
					if(compareTo > 0) {
						
					}else if(compareTo < 0) {
						System.out.println("------------");
						//骑手工作时
						System.out.println("fxSdUserDeliverAdditional.getIsWork():"+fxSdUserDeliverAdditional.getIsWork());
						if(!fxSdUserDeliverAdditional.getIsWork()) {
							System.out.println("111111111111111");
							//骑手未接单
							//通过用骑手的id去查询订单表中订单的状态为<6;
							Byte orderStatus = orderService.selectOrderByDeliveryId(fxSdUserDeliverAdditional.getDeliverId());
							System.out.println("orderStatus"+orderStatus);
							if(orderStatus<6) {
								map.put(fxSdUserDeliverAdditional.getLat(), fxSdUserDeliverAdditional.getLng());
								result.setData(map);
								result.setMessage("骑手数据经纬度已返回");
								result.setState(200);
							}
						}
					}else {
						result.setMessage("起点终点是一致的");
					}
				}
				//骑手少于十个,则假数据十个;
				double lngDouble = Double.parseDouble(lng);
				double latDouble = Double.parseDouble(lat);
				if(map.size()<10) {
					double[] random = ArrayRandom.random(10, 20);
					for (double i : random) {
						map.put(String.valueOf(latDouble+i), String.valueOf(lngDouble+i));
						result.setData(map);
						result.setMessage("骑手数据经纬度已返回");
						result.setState(200);
					}
				}
			}
			for (String key : map.keySet()) {
				System.out.println("key:"+key+"  value:"+map.get(key));
			}
		}catch(Exception e) {
			logger.error("GetDeliversPosition error:{}",e.getMessage());
			result.setMessage(EWarning.Unknown.getName()+e.getMessage());
			result.setState(EWarning.Unknown.getValue());
		}
		return result;
		
		
	}
	
	
}
