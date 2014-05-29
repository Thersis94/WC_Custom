package com.ram.action.customer;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// RAMDataFeed
import com.ram.datafeed.data.CustomerVO;

// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>CustomerAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since May 20, 2014<p/>
 *<b>Changes: </b>
 * May 20, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class CustomerAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public CustomerAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public CustomerAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("CustomerAction retrieve...");
		
		List<CustomerVO> data = new ArrayList<>();
		if (! Convert.formatBoolean(req.getParameter("addCustomer"))) {
			String schema = (String)getAttribute("customDbSchema");
			StringBuilder sql = new StringBuilder();
			sql.append("select a.* from ").append(schema);
			sql.append("RAM_CUSTOMER a ");
			int customerId = Convert.formatInteger(req.getParameter("customerId"), 0);
			if (customerId > 0) {
				sql.append("where CUSTOMER_ID = ? ");
			}
			sql.append("order by CUSTOMER_NM");
			log.debug("Customer retrieve SQL: " + sql.toString() + "|" + customerId);
			
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql.toString());
				if (customerId > 0) ps.setInt(1, customerId);
				ResultSet rs = ps.executeQuery();
				while (rs.next()) {
					data.add(new CustomerVO(rs, false));
				}
			} catch (SQLException e) {
				log.error("Error retrieving RAM customer data, ", e);
			} finally {
				if (ps != null) {
					try { 	ps.close(); }
					catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
				}
			}
		} else {
			data.add(new CustomerVO());
		}
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);
        modVo.setDataSize(data.size());
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("CustomerAction build...");
		CustomerVO vo = new CustomerVO(req);
		boolean isUpdate = (vo.getCustomerId() > 0);
		boolean reactivate = (Convert.formatBoolean(req.getParameter("activate")));
		boolean deactivate = (Convert.formatBoolean(req.getParameter("deactivate")));
		String msgAction;
		String schema = (String)getAttribute("customDbSchema");
		StringBuilder sql = new StringBuilder();
		if (reactivate || deactivate) {
			// is a re-activation or deactivation
			sql.append("update ").append(schema).append("RAM_CUSTOMER ");
			sql.append("set ACTIVE_FLG = ?, UPDATE_DT = ? where CUSTOMER_ID = ?");
			if (reactivate) {
				msgAction = "activated";
			} else {
				msgAction = "deactivated";
			}
			
		} else if (isUpdate) {
			// is an update
			sql.append("update ").append(schema).append("RAM_CUSTOMER ");
			sql.append("set ORGANIZATION_ID = ?, CUSTOMER_TYPE_ID = ?, ");
			sql.append("CUSTOMER_NM = ?, GTIN_NUMBER_TXT = ?, HIBC_LIC_ID = ?, ACTIVE_FLG = ?, ");
			sql.append("UPDATE_DT = ? WHERE CUSTOMER_ID = ?");
			msgAction = "updated";
			
		} else {
			// is an insert
			sql.append("insert into ").append(schema).append("RAM_CUSTOMER ");
			sql.append("(ORGANIZATION_ID, CUSTOMER_TYPE_ID, CUSTOMER_NM, GTIN_NUMBER_TXT, ");
			sql.append("HIBC_LIC_ID, ACTIVE_FLG, CREATE_DT) values (?,?,?,?,?,?,?");
			msgAction = "inserted";
			
		}
		
		log.debug("Customer build SQL: " + sql.toString());
		
		Object msg = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			int index = 1;
			if (reactivate || deactivate) {
				ps.setInt(index++, Convert.formatInteger(req.getParameter("activeFlag")));
				ps.setTimestamp(index++, Convert.getCurrentTimestamp());
				ps.setString(index++, req.getParameter("customerId"));
			} else {
				// handles insert/update
				ps.setString(index++, vo.getOrganizationId());
				ps.setString(index++, vo.getCustomerTypeId());
				ps.setString(index++, vo.getCustomerName());
				ps.setString(index++, vo.getGtinNumber());
				ps.setString(index++,  vo.getHibcLicCode());
				ps.setInt(index++, vo.getActiveFlag());
				ps.setTimestamp(index++, Convert.getCurrentTimestamp());
				if (isUpdate) ps.setInt(index++, vo.getCustomerId());
			}
			
			ps.executeUpdate();
			msg = "You have successfully " + msgAction + " the customer.";
		} catch (SQLException sqle) {
			log.error("Error managing RAM customer record, ", sqle);
			msg = "An errror occurred: The customer record was not " + msgAction;
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) {log.error("Error closing PrepraredStatement, ", e);}
			}
		}
				
        // Build the redirect and messages
		// Setup the redirect.
		StringBuilder url = new StringBuilder();
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		url.append(page.getRequestURI());
		if (msg != null) url.append("?msg=").append(msg);
		
		log.debug("CustomerAction redir: " + url);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, url.toString());
	}
	
}
