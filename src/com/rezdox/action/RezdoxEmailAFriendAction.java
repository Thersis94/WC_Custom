package com.rezdox.action;

import java.util.HashMap;
import java.util.Map;

import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.session.SMTSession;
import com.smt.sitebuilder.action.tools.EmailFriendAction;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: RezdoxEmailAFriend.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Extends core email a friend action to
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 19, 2018
 * @updates:
 ****************************************************************************/
public class RezdoxEmailAFriendAction extends EmailFriendAction{
	

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.tools.EmailFriendAction#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override 
	public void build(ActionRequest req)  throws ActionException {
		log.debug("reqbuild in action called");
		
		SMTSession session = req.getSession();
		MemberVO member = (MemberVO) session.getAttribute(Constants.USER_DATA);
		
		BusinessVO bvo = new BusinessVO(req);
		String schema = getCustomSchema();
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		
		try {
			db.getByPrimaryKey(bvo);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("could not load busines data ",e);
		}
		
		log.debug("business: " + bvo);
		
		Map<String, Object> emailData = new HashMap <>();
		
		//default email a friend only gets the name from the form 
		emailData.put("memberName", member.getFullName());
		emailData.put("businessName", bvo.getBusinessName());
		emailData.put("businessId", bvo.getBusinessId());
		
		attributes.put(EmailFriendAction.MESSAGE_DATA_MAP, emailData);
		
		super.build(req);
	}
}
