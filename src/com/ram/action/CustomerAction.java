package com.ram.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

import com.ram.action.data.CustomerVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: CustomerAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Manages the customer data for the ram group.  Each customer will 
 * be able to update their customer info.  Admins will be adble to add a 
 * customer or delete one
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Feb 10, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class CustomerAction extends SBActionAdapter {

	/**
	 * 
	 */
	public CustomerAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public CustomerAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
		if (user == null) return;
		String customerId = StringUtil.checkVal(user.getUserExtendedInfo());
		String rCustomerId = StringUtil.checkVal(req.getParameter("customerId"));
		
		// Make sure that if a customerId is assigned to the user, they can only 
		// request data for that ID
		if  (customerId.length() > 0) rCustomerId = customerId;
		
		String dbs = (String)this.getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder s = new StringBuilder();
		s.append("select * from ").append(dbs).append("ram_customer a ");
		if (rCustomerId.length() > 0) s.append("where customer_id = ? ");
		s.append("order by customer_nm ");
		log.debug("Customer lookup SQL: " + s);
		
		List<CustomerVO> data = new ArrayList<CustomerVO>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			if (rCustomerId.length() > 0) ps.setString(1, rCustomerId);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new CustomerVO(rs));
			}
			
			// Get the profile data if there is only 1 customer returned.  If only
			// 1 is returned, this means the entry is being edited, not listed
			if (data.size() == 1 && data.get(0).hasContactAssigned()) {
				CustomerVO c = data.get(0);
				ProfileManager pm = ProfileManagerFactory.getInstance(attributes);
				c.setContact(pm.getProfile(c.getProfileId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, null));
			}
			
			this.putModuleData(data, data.size(), false);
		} catch (Exception e) {
			log.error("Unable to retrieve customers", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
	}

}
