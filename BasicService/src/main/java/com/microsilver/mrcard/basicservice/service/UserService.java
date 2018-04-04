/**
 * 
 */
package com.microsilver.mrcard.basicservice.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.microsilver.mrcard.basicservice.dao.FxSdUserAddressMapper;
import com.microsilver.mrcard.basicservice.dao.FxSdUserMemberMapper;
import com.microsilver.mrcard.basicservice.dao.FxSdUserPreRegMapper;
import com.microsilver.mrcard.basicservice.model.FxSdUserAddress;
import com.microsilver.mrcard.basicservice.model.FxSdUserAddressExample;
import com.microsilver.mrcard.basicservice.model.FxSdUserMember;
import com.microsilver.mrcard.basicservice.model.FxSdUserMemberExample;
import com.microsilver.mrcard.basicservice.model.FxSdUserMemberExample.Criteria;
import com.microsilver.mrcard.basicservice.model.FxSdUserPreReg;
import com.microsilver.mrcard.basicservice.model.FxSdUserPreRegExample;
import com.microsilver.mrcard.basicservice.service.UserService;

@Service
@Transactional
public class UserService {

	@Autowired
	private FxSdUserMemberMapper fxSdUserMemberMapper;
	@Autowired
	private FxSdUserPreRegMapper fxSdUserPreRegMapper;
	@Autowired
	private FxSdUserAddressMapper fxSdUserAddressMapper;

	public FxSdUserMember selectUserByMobile(String mobile) {
		FxSdUserMemberExample fxSdUserMemberExample = new FxSdUserMemberExample();
		Criteria createCriteria = fxSdUserMemberExample.createCriteria();
		createCriteria.andMobileLike(mobile);
		List<FxSdUserMember> selectByExample = fxSdUserMemberMapper.selectByExample(fxSdUserMemberExample);
		if (selectByExample.size() > 0) {
			return selectByExample.get(0);
		}
		return null;
	}

	public void insertUser(FxSdUserMember info) {
		fxSdUserMemberMapper.insert(info);
	}

	public void updatePwd(FxSdUserMember selectUserByMobile,String mobile) {
		FxSdUserMemberExample example = new FxSdUserMemberExample();
		example.createCriteria().andMobileEqualTo(mobile);
		fxSdUserMemberMapper.updateByExampleSelective(selectUserByMobile, example);
	}

	public FxSdUserPreReg checkIsInvitation(Long mobile) {
		FxSdUserPreRegExample example = new FxSdUserPreRegExample();
		example.createCriteria().andMobileEqualTo(mobile);
		List<FxSdUserPreReg> list = fxSdUserPreRegMapper.selectByExample(example);
		if (list.size() > 0) {
			return list.get(0);
		}
		return null;
	}

	public void updateStatus(String refereeId, FxSdUserMember fxSdUserMember) {
		FxSdUserMemberExample example = new FxSdUserMemberExample();
		example.createCriteria().andMobileEqualTo(fxSdUserMember.getMobile());
		fxSdUserMember.setReferee(Long.parseLong(refereeId));
		fxSdUserMemberMapper.updateByExampleSelective(fxSdUserMember, example);
	}

	public FxSdUserMember selectUserByUserId(String userId) {
		return fxSdUserMemberMapper.selectByPrimaryKey(Long.parseLong(userId));
	}

	
	public void updatePhone(FxSdUserMember selectUserByMobile, String oldPhoneNumber) {
		FxSdUserMemberExample example = new FxSdUserMemberExample();
		example.createCriteria().andMobileEqualTo(oldPhoneNumber);
		fxSdUserMemberMapper.updateByExampleSelective(selectUserByMobile, example);
	}

	
	public int selectUserAddressByUserId(Long id) {
		FxSdUserAddressExample example = new FxSdUserAddressExample();
		example.createCriteria().andMemberIdEqualTo(id);
		return  fxSdUserAddressMapper.countByExample(example);
	}

	
	public void insertAddress(FxSdUserAddress fxSdUserAddress) {
		fxSdUserAddressMapper.insert(fxSdUserAddress);
		
	}
	public void updateAddress(FxSdUserAddress fxSdUserAddress, String addressId) {
		FxSdUserAddressExample example = new FxSdUserAddressExample();
		example.createCriteria().andIdEqualTo(Integer.valueOf(addressId));
		fxSdUserAddressMapper.updateByExampleSelective(fxSdUserAddress, example);
	}

	
	public void deleteAddress(String addressId) {
		fxSdUserAddressMapper.deleteByPrimaryKey(Integer.valueOf(addressId));
	}


	public List<FxSdUserAddress> selectUserAllAddress(String memberId) {
		FxSdUserAddressExample example = new FxSdUserAddressExample();
		example.createCriteria().andMemberIdEqualTo(Long.parseLong(memberId));
		List<FxSdUserAddress> list = fxSdUserAddressMapper.selectByExample(example);
		if(list.size()>0) {
			return list;
		}
		return null;
	}

	
	public void updateAvatar(FxSdUserMember user) {
		fxSdUserMemberMapper.updateByPrimaryKey(user);
	}
}
