package com.mts.subscriber.action;

// JDK 1.8.x
import java.util.Arrays;
import java.util.List;

// MTS Libs
import com.mts.subscriber.data.MTSUserVO;
import com.mts.subscriber.data.SubscriptionUserVO;

//. SMT Base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: TrialSubscriptionValidator.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Validates that the user is able to join a trial subscription
 * <b>Copyright:</b> Copyright (c) 2020
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 29, 2020
 * @updates:
 ****************************************************************************/
public class TrialSubscriptionValidator extends SBActionAdapter {

	/**
	 * Ajax Controller key for this action
	 */
	public static final String AJAX_KEY = "trial-validate";
	
	
	/**
	 * 
	 */
	public TrialSubscriptionValidator() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public TrialSubscriptionValidator(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * 
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String email = req.getParameter("email");
		String publication = StringUtil.checkVal(req.getParameter("publication"));
		
		MTSUserVO user = getUserData(email);
		log.info(user);
		int count = 0;
		for (int i = 0; user != null && i < user.getSubscriptions().size(); i++) {
			SubscriptionUserVO vo = user.getSubscriptions().get(i);
			if (publication.equalsIgnoreCase(vo.getPublicationId()) && vo.getExpirationDate() != null) {
				count++; 
			}
		}
		
		
		this.setModuleData("VALIDATION_SUBMITTED", 0, count == 0 ? null : "USER_NOT_AUTHORIZED");
	}
	
	/**
	 * 
	 * @param email
	 * @return
	 */
	public MTSUserVO getUserData(String email) {
		
		StringBuilder sql = new StringBuilder(128);
		sql.append(DBUtil.SELECT_FROM_STAR).append(getCustomSchema()).append("mts_user a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append(" mts_subscription_publication_xr b ");
		sql.append("on a.user_id = b.user_id and lower(email_address_txt) = lower(?)");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<MTSUserVO> user = db.executeSelect(sql.toString(), Arrays.asList(email), new MTSUserVO());
		
		return user.isEmpty() ? null : user.get(0);
	}

}
