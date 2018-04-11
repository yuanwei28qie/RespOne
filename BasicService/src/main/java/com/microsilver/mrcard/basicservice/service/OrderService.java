/**
 * 
 */
package com.microsilver.mrcard.basicservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.github.pagehelper.PageHelper;
import com.microsilver.mrcard.basicservice.dao.FxSdCarriageOrderMapper;
import com.microsilver.mrcard.basicservice.dto.PageBeanDto;
import com.microsilver.mrcard.basicservice.model.FxSdCarriageOrder;
import com.microsilver.mrcard.basicservice.model.FxSdCarriageOrderExample;

/**
 * 
 * @Name com.microsilver.mrcard.basicservice.service.OrderService
 * @Description 
 * 
 * @Author huwei
 * @Version 2018年4月9日 上午9:05:29
 * @Copyright  Micro Silver-SuperDelivery
 *
 */
@Service
public class OrderService {
		
	@Autowired
	private FxSdCarriageOrderMapper fxSdCarriageOrderMapper;

	
	public  Byte selectOrderByDeliveryId(Long id) {
		FxSdCarriageOrderExample example = new FxSdCarriageOrderExample();
		example.createCriteria().andDeliverIdEqualTo(id);
		List<FxSdCarriageOrder> list = fxSdCarriageOrderMapper.selectByExample(example);
		if(list.size()>0) {
			return list.get(0).getStatus();
		}
		return null;
	}


	public FxSdCarriageOrder selectOrderByMemberId(String id) {
		FxSdCarriageOrderExample example = new FxSdCarriageOrderExample();
		example.createCriteria().andMemberIdEqualTo(Long.parseLong(id));
		List<FxSdCarriageOrder> list = fxSdCarriageOrderMapper.selectByExample(example);
		if(list.size()>0) {
			return list.get(0);
		}
		return null;	
	}


	public FxSdCarriageOrder selectOrderByOrdersnAndMemberId(String memberId, String ordersn) {
		FxSdCarriageOrderExample example = new FxSdCarriageOrderExample();
		example.createCriteria().andMemberIdEqualTo(Long.parseLong(memberId)).andOrdersnEqualTo(ordersn);
		List<FxSdCarriageOrder> list = fxSdCarriageOrderMapper.selectByExample(example);
		if(list.size()>0) {
			return list.get(0);
		}
		return null;
		
	}


	
	public FxSdCarriageOrder selectOrderByOrdersn(String ordersn) {
		FxSdCarriageOrderExample example = new FxSdCarriageOrderExample();
		example.createCriteria().andOrdersnEqualTo(ordersn);
		List<FxSdCarriageOrder> list = fxSdCarriageOrderMapper.selectByExample(example);
		if(list.size()>0) {
			return list.get(0);
		}
		return null;
		
		
	}

	public List<FxSdCarriageOrder> selectOrderByPage(FxSdCarriageOrder fxSdCarriageOrder,Integer currentPage, Integer pageSize) {
		PageHelper.startPage(currentPage, pageSize);
		FxSdCarriageOrderExample example = new	FxSdCarriageOrderExample();
		example.createCriteria().andMemberIdEqualTo(fxSdCarriageOrder.getMemberId()).
		andStatusEqualTo(fxSdCarriageOrder.getStatus()).andOrdersnEqualTo(fxSdCarriageOrder.getOrdersn());
		//所有记录
		List<FxSdCarriageOrder> orderAll = fxSdCarriageOrderMapper.selectByExample(example);
		//总记录数
		Integer count = fxSdCarriageOrderMapper.countByExample(example);
		PageBeanDto<FxSdCarriageOrder> pageData = new PageBeanDto<FxSdCarriageOrder>(currentPage, pageSize, count);
		pageData.setItems(orderAll);
		return pageData.getItems();
		
	}


	
	
}
