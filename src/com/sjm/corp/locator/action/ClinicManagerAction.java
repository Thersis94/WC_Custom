package com.sjm.corp.locator.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.dealer.DealerInfoAction;
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: ClinicUpdateAction.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Mar 28, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ClinicManagerAction extends DealerInfoAction {

	/**
	 * 
	 */
	public ClinicManagerAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public ClinicManagerAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		req.setValidateInput(false);
		String msg = (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		
		// If both of these values are present, the record is waiting for approval
		// and needs an update to the existing waiting value.
		String dealerLocationId = StringUtil.checkVal(req.getParameter("dealerLocationId"));
		String parentId = StringUtil.checkVal(req.getParameter("parentId"));
		
		// If the parent id is empty, this is the first update.  That means the 
		// parentId will get the value of the dealer location id and dlid
		// will be set to null.  This will cause an insert
		if (parentId.length() == 0) {
			req.setParameter("parentId", dealerLocationId);
			req.setParameter("dealerLocationId", null);
		}
		
		// Update the record
		try {
			updateDealerLocation(req, false);
			
			// Send the email to the country admin
			RegisterClinicAction rca = new RegisterClinicAction(this.actionInit);
			rca.setAttributes(attributes);
			rca.setDBConnection(dbConn);
			rca.sendEmail(req);
		} catch (MailException me) {
			msg = "Email notification not sent";
			log.error("Email notification not sent", me);
		} catch (Exception e) {
			msg = (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
			log.error("Unable to add/update clinic record", e);
		}
		
		// Redirect back to the 
		this.sendRedirect(((PageVO)req.getAttribute(Constants.PAGE_DATA)).getFullPath(), msg, req);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.dealer.DealerInfoAction#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String dlid = (String) req.getSession().getAttribute("dealerLocationId");
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);

		if (StringUtil.checkVal(req.getAttribute("dealerLocationId")).length() > 0)
			dlid = (String)req.getAttribute("dealerLocationId");
		else if (StringUtil.checkVal(req.getParameter("dealerLocationId")).length() > 0)
			dlid = req.getParameter("dealerLocationId");
		else if (StringUtil.checkVal(req.getSession().getAttribute("dealerLocationId")).length() > 0) 
			dlid = (String)req.getSession().getAttribute("dealerLocationId");
		else if (role.getRoleLevel() == 20 && StringUtil.checkVal(role.getAttribute(0)).length() > 2) {
			dlid = (String)role.getAttribute(0);
			req.getSession().setAttribute("dealerLocationId", dlid);
		}
		
		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();
		
		try {
			Map<String,DealerLocationVO> data = this.getDealerLocation(dlid, orgId);
			this.putModuleData(data, data.size(), false);
		} catch (DatabaseException e) {
			log.error("Unable to retrieve clinic record", e);
		}
	}
	
	
	/**
	 * Retrieves the dealer location information
	 * @param req
	 * @return
	 * @throws DatabaseException
	 */
	public Map<String,DealerLocationVO> getDealerLocation(String dlid, String orgId) 
	throws DatabaseException {
		StringBuilder s = new StringBuilder();
		s.append("select * from dealer a ");
		s.append("inner join dealer_location b on a.dealer_id = b.dealer_id ");
		s.append("inner join country c on b.country_cd = c.country_cd ");
		s.append("where organization_id  = ? ");
		s.append("and (parent_id = ? or dealer_location_id = ?)");
		log.debug("Dealer Loc SQL: " + s + "|" + dlid + "|" + orgId);
		
		PreparedStatement ps = null;
		Map<String,DealerLocationVO> data = new HashMap<String,DealerLocationVO>();
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, orgId);
			ps.setString(2, dlid);
			ps.setString(3, dlid);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String pId = StringUtil.checkVal(rs.getString("parent_id"));
				DealerLocationVO dlr = new DealerLocationVO(rs);
				dlr.setCassValidated(Convert.formatBoolean(rs.getInt("cass_validate_flg")));
				
				if (pId.length() > 0)
					data.put("pending", dlr);
				else 
					data.put("live", dlr);
			}
		} catch(Exception e) {
			log.error("Test", e);
			throw new DatabaseException("unable to perform a locator search", e);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		return data;
	}
}
