package com.microsilver.mrcard.basicservice.controller;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;

import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import com.alibaba.fastjson.JSON;
import com.microsilver.mrcard.basicservice.common.Consts;
import com.microsilver.mrcard.basicservice.dto.FxAreaDto;
import com.microsilver.mrcard.basicservice.dto.RespBaseDto;
import com.microsilver.mrcard.basicservice.model.FxSdSysArea;
import com.microsilver.mrcard.basicservice.model.FxSdSysAreaopen;
import com.microsilver.mrcard.basicservice.model.FxSdSysVersion;
import com.microsilver.mrcard.basicservice.model.enums.EWarning;
import com.microsilver.mrcard.basicservice.mq.MQSendMsgHelper;
import com.microsilver.mrcard.basicservice.service.FxSdSysVersionServices;
import com.microsilver.mrcard.basicservice.service.IFinanceRemoteSmpl;
import com.microsilver.mrcard.basicservice.service.SysAreasServices;
import com.microsilver.mrcard.basicservice.utils.GetPictureMd5;
import com.microsilver.mrcard.basicservice.utils.UUIDTool;
import com.microsilver.mrcard.mq.PushMsg;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiImplicitParams;
import io.swagger.annotations.ApiOperation;

@Api(value = "/api/Common", description = "通用方法API")
@Controller
@RequestMapping("/api/Common")
public class CommonController extends BaseController {
	@Autowired
	private MQSendMsgHelper mqsendMsgHelper;
	@Autowired
	private SysAreasServices sysAreasServices;
	@Autowired
	private FxSdSysVersionServices fxSdSysVersionServices;
	/**
	 * 获取短信验证码
	 */
	@Autowired
	private StringRedisTemplate stringRedisTemplate;

	@ApiOperation(value = "获取短信验证码", httpMethod = "POST")
	@RequestMapping(value = { "/getCheckCode" }, method = RequestMethod.POST)
	@ResponseBody
	public RespBaseDto<Object> getCheckCode(String mobile) {
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		if (mobile != null && !mobile.equals("")) {
			String checkCode = stringRedisTemplate.opsForValue().get(mobile);
			if (checkCode != null) {
				result.setData(checkCode);
				result.setMessage("短信验证码");
				result.setState(1);
				return result;
			}
		}
		result.setMessage("验证码不能为空");
		result.setState(2);
		return result;
	}

	/**
	 * 发送验证码
	 */
	@Autowired
	IFinanceRemoteSmpl financeRemoteSmpl;

	@Autowired
	private MQSendMsgHelper mqSendMsgHelper;

	@ApiOperation(value = "发送短信验证码并保存", httpMethod = "POST")
	@RequestMapping(value = { "/sendAndSaveCheckCode" }, method = RequestMethod.POST)
	@ResponseBody
	public RespBaseDto<Object> sendAndSaveCheckCode(String mobile) {
		RespBaseDto<Object> result = new RespBaseDto<Object>();
		if (mobile != null && !mobile.equals("")) {
			mobile = mobile.trim();
			// 生成验证码
			int radomInt = new Random().nextInt(999999);
			String checkCode = String.valueOf(radomInt);
			// 存入redis中
			stringRedisTemplate.opsForValue().set(mobile, checkCode);
			//发送消息给MQ
			mqSendMsgHelper.sendJSONMsg(mobile+"-"+checkCode+"-"+"您的验证码是:"+checkCode+",如非本人操作,请忽略本短信");
			result.setData(null);
			result.setMessage("发送成功");
			result.setState(1);
			return result;
		}

		return result;
	}
	@ApiOperation(value = "MQ接收处理，短信消息，推送消息", httpMethod = "POST")
	@RequestMapping(value = "/pushMessageToMQ", method = RequestMethod.POST)
	@ResponseBody
	public RespBaseDto<Object> pushMessageToMQ(PushMsg msg){
		logger.info("PushMsg :"+JSON.toJSONString(msg));

		RespBaseDto<Object> res = new RespBaseDto<Object>();
		try{
			mqsendMsgHelper.sendJSONMsg(msg);
			res.setMessage("OK");
		}catch (Exception ex){
			res.setState(EWarning.Unknown.getValue());
			logger.error(ex.getMessage());
			ex.printStackTrace();
		}
		return res;
	}
	
	@ApiOperation(value = "获取全部区域", httpMethod = "POST")
	@RequestMapping(value = "/getAllArea")
	@ResponseBody
	public RespBaseDto<List<FxSdSysArea>> getAllArea() throws Exception{
		RespBaseDto<List<FxSdSysArea>> dto = new RespBaseDto<>();
		List<FxSdSysArea> allArea = sysAreasServices.getAllSysAreas();
		dto.setData(allArea);
		return dto;
	}
	
	
	@ApiOperation(value = "获取已开通区域", httpMethod = "POST")
	@RequestMapping(value = "/getOpenArea")
	@ResponseBody
	public RespBaseDto<List<FxAreaDto>> getOpenArea() throws Exception{
		RespBaseDto<List<FxAreaDto>> info = new RespBaseDto<>();
		List<FxSdSysAreaopen> openSysAreasList = sysAreasServices.getOpenSysAreas();
		List<FxAreaDto> list = new ArrayList<FxAreaDto>();
		if(openSysAreasList!=null) {
			for (FxSdSysAreaopen fxSdSysAreaopen : openSysAreasList) {
				FxAreaDto fxAreaDto = new FxAreaDto();
				fxAreaDto.setProvince(sysAreasServices.getOpenAreaAttribute((long)fxSdSysAreaopen.getProvince()));
				fxAreaDto.setCity(sysAreasServices.getOpenAreaAttribute((long)fxSdSysAreaopen.getCity()));
				fxAreaDto.setCounty(fxSdSysAreaopen.getName());
				fxAreaDto.setId(fxSdSysAreaopen.getId().intValue());
				list.add(fxAreaDto);
			}
			for (FxAreaDto fxAreaDto2 : list) {
				System.out.println(fxAreaDto2.toString());
			}
			info.setData(list);
			info.setMessage("Ok");
			return info;
		}
		return null;
	}
	@ApiOperation(value = "APP版本检查更新", httpMethod = "POST")
	@RequestMapping(value = "/getAppVersion")
	@ApiImplicitParams({@ApiImplicitParam(name="appType",value="APP类型1:超级跑腿,2:超级飞人",dataType="int"),
		@ApiImplicitParam(name="clientType",value="终端类型(ture:android,false:ios)",dataType="int")})
	@ResponseBody
	public RespBaseDto<FxSdSysVersion> getAppVersion(Integer appType,Boolean clientType){
		RespBaseDto<FxSdSysVersion> result = new RespBaseDto<FxSdSysVersion>();
		FxSdSysVersion version = fxSdSysVersionServices.CheackVersion(appType, clientType);
		if(version == null){
			version = new FxSdSysVersion();
			result.setMessage("未初始化版本");
			result.setState(0);
		}else{
			result.setMessage("SUCCESS");
		}
		result.setData(version);		
		return result;
	}
	
	@ApiOperation(value = "上传图片到服务器", httpMethod = "POST")
	@RequestMapping(value = "/uploadPictureToServer")
	@ApiImplicitParams({
		@ApiImplicitParam(name="oldfileurl",value="图片访问路径",dataType="body")
		})
	@ResponseBody
	public RespBaseDto<Set<Object>> uploadPictureToServer(
			//String oldfileurl,
			HttpServletRequest request){
		RespBaseDto<Set<Object>> result = new RespBaseDto<Set<Object>>();
		
		//存图片到服务器上
		//如果当前文件是该月的文件夹,直接选择存入
		//如果当前文件不是该月的文件夹,就创建一个文件夹,并存入图片
		//每次上传的图片我都找到这个图片对应的md5值,存在集合中,每次上传图片都对比集合中的md5值,如果相同就返回不能上传的信息
		
		
		List<MultipartFile> files = ((MultipartHttpServletRequest) request).getFiles("file");
		MultipartFile file = null;
		BufferedOutputStream stream = null;
		
		
		
		//存放图片的根目录
		String filePath =Consts.PICTURES_ADDRESS;
		//存储md5的值
		Set<Object> set  = new HashSet<Object>();
		//创建日期文件夹
		String path="D:/aim/"+new SimpleDateFormat("yyyy/MM/").format(new Date());  
		//如果不存在,创建文件夹  
	    File f = new File(path);  
	    if(!f.exists()){  
	       f.mkdirs();   
	    }  
		 
		for (int i = 0; i < files.size(); ++i) {
			file = files.get(i);
	        String substring = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".") + 1);  
			//String finallyFile = filePath + UUID.randomUUID()+"."+substring;
	        String realPath = f.getPath()+"\\"+UUIDTool.getUUID()+"."+substring;
	        System.out.println(realPath+"-----------");
	        
	        System.out.println(file.getOriginalFilename());
			//写入到磁盘中
			if (!file.isEmpty()) {
				try {
						//存储该图片的md5值到set中
						byte[] bytes = file.getBytes();
						stream = new BufferedOutputStream(
								new FileOutputStream(new File(realPath)));// 设置文件路径及名字
						stream.write(bytes);// 写入
						stream.close();
						if(set.add(GetPictureMd5.getPictureMd5(realPath))) {
							
						}else {
							new File(realPath).delete();
							result.setMessage("你上传图片重复了.......");
							return result;
							
						}
				
					
				} catch (Exception e) {
					
					  result.setMessage("You failed to upload " + i + " => " + e.getMessage());
					  return result;
				}
			} else {
				result.setMessage("You failed to upload " + i + " because the file was empty.");
				return result;
			}
		}
		result.setData(set);
		return result;
	}
	 
}
