package com.restpeer.action.admin;

import com.restpeer.data.RPUserVO;
import com.siliconmtn.action.ActionException;
// SMT Base Libs
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.user.UserBaseWidget;

/****************************************************************************
 * <b>Title</b>: UserWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the assignment of Users to a location
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 19, 2019
 * @updates:
 ****************************************************************************/

public class UserWidget extends UserBaseWidget {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "user";
	
	/**
	 * 
	 */
	public UserWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public UserWidget(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.user.UserBaseWidget#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		// Call the base class and process the user. Assign to the RP User
		super.build(req);
		RPUserVO user = (RPUserVO)this.extUser;
		user.setDriverLicense(req.getParameter("driverLicense"));
		user.setDriverLicensePath(req.getParameter("driverLicensePath"));
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (StringUtil.isEmpty(req.getParameter("userId"))) {
				db.insert(user);
			} else {
				db.update(user);
			}
			
			setModuleData(user);
		} catch (Exception e) {
			setModuleData(user, 0, e.getLocalizedMessage());
		}
	}
}

