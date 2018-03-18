package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

import com.rezdox.data.BusinessCategoryList;
import com.rezdox.vo.BusinessVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> BusinessAdminDataTool.java<br/>
 * <b>Description:</b> WC Data Tool for managing business approvals and business
 * review moderation.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author Tim Johnson
 * @version 1.0
 * @since March 14, 2018
 ****************************************************************************/
public class BusinessAdminDataTool extends SimpleActionAdapter {
	public static final String REQ_APPROVE_BUSINESS = "approveBusiness";
	
	public BusinessAdminDataTool() {
		super();
	}

	public BusinessAdminDataTool(ActionInitVO arg0) {
		super(arg0);
	}


	/**
	 * Return a list of all the businesses requiring approval or business reviews
	 * requiring moderation.
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		BusinessCategoryList bcl = new BusinessCategoryList(dbConn, attributes);
		bcl.retrieve(req);
		req.setAttribute("categoryList", ((ModuleVO) bcl.getAttribute(Constants.MODULE_DATA)).getActionData());

		// Only load data for ajax calls (bootstrap tables)
		if (!req.hasParameter("loadData")) return;

		if (req.hasParameter("businessApproval")) {
			BusinessAction ba = new BusinessAction(getDBConnection(), getAttributes());
			List<BusinessVO> businesses = ba.retrievePendingBusinesses();
			putModuleData(businesses);
		} else {
			// TODO: Retrieve business reviews for moderation
		}
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		String msg = null;

		if (req.hasParameter(REQ_APPROVE_BUSINESS)) {
			// Set values required for approval or denial of business
			BusinessVO business = new BusinessVO();
			business.setBusinessId(req.getParameter(BusinessAction.REQ_BUSINESS_ID));
			business.setStatusCode(Convert.formatInteger(req.getParameter(REQ_APPROVE_BUSINESS)));
			msg = setBusinessStatus(business);
		} else {
			// TODO: Handle moderation of business reviews
		}

		adminRedirect(req, msg, (String) getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SimpleActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		// TODO: Delete a business review after admin moderation
	}


	/**
	 * Approve or deny a business.  
	 *
	 * @param mrv
	 */
	private String setBusinessStatus(BusinessVO business) {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("rezdox_business_member_xr ");
		sql.append("set status_flg = ? ");
		sql.append("where business_id = ? ");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, business.getStatusCode());
			ps.setString(2, business.getBusinessId());
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("Could not approve or deny business ", sqle);
			return (String) getAttribute(AdminConstants.KEY_ERROR_MESSAGE); 
		}
		
		return (String) getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
	}
}