package com.ram.action.customer;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.datafeed.data.CustomerLocationVO;
// RAMDataFeed
import com.ram.datafeed.data.CustomerVO;
// SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
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
		int customerId = Convert.formatInteger(req.getParameter("customerId"), 0);
		String customerTypeId = StringUtil.checkVal(req.getParameter("customerTypeId"));
		String excludeTypeId = StringUtil.checkVal(req.getParameter("excludeTypeId"));
		int start = Convert.formatInteger(req.getParameter("start"), 0);
		int limit = Convert.formatInteger(req.getParameter("limit"), 25) + start;
		log.debug("excludeTypeId: " + excludeTypeId);
		
		List<CustomerVO> data = new ArrayList<>();
		if (! Convert.formatBoolean(req.getParameter("addCustomer"))) {
			String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
			StringBuilder sql = new StringBuilder(260);
			sql.append("select top ").append(limit).append(" a.* from ").append(schema);
			sql.append("ram_customer a ");
			if(customerId > 0) {
				sql.append("inner join ").append(schema).append("ram_customer_location b ");
				sql.append("on a.customer_id = b.customer_id ");
			}
			sql.append("where 1 = 1 ");

			if (customerId > 0) sql.append("and a.customer_id = ? ");
			if (customerTypeId.length() > 0) {
				sql.append("and customer_type_id = ? ");
			} else if (excludeTypeId.length() > 0) {
				sql.append("and customer_type_id != ? ");
			}
			
			sql.append("order by CUSTOMER_NM");
			
			log.debug("Customer retrieve SQL: " + sql.toString() + "|" + customerId);
			int index = 1;
			PreparedStatement ps = null;
			try {
				ps = dbConn.prepareStatement(sql.toString());
				if (customerId > 0) ps.setInt(index++, customerId);
				if (customerTypeId.length() > 0) {
					ps.setString(index++, customerTypeId);
				} else if (excludeTypeId.length() > 0) {
					ps.setString(index++, excludeTypeId);
				}
				ResultSet rs = ps.executeQuery();
				if(customerId > 0) {
					CustomerVO c = null;
					List<CustomerLocationVO> locs = new ArrayList<CustomerLocationVO>();
					while(rs.next()) {
						c = new CustomerVO(rs, false);
						locs.add(new CustomerLocationVO(rs, false));
					}
					c.setCustomerLocations(locs);
					data.add(c);
				} else {
					for(int i=0; rs.next(); i++) {
						if (i >= start && i < limit)
							data.add(new CustomerVO(rs, false));
					}
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

		//Only go after record count if we are doing a list.
        modVo.setDataSize((customerId > 0 ? 1 : getRecordCount()));
        modVo.setActionData(data);
        this.setAttribute(Constants.MODULE_DATA, modVo);
	}
	
	/**
	 * Gets the count of the records
	 * @param customerId
	 * @param term
	 * @return
	 * @throws SQLException
	 */
	protected int getRecordCount() {
		log.debug("Retrieving Total Counts");
		StringBuilder sb = new StringBuilder(80);
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sb.append("select count(customer_id) from ").append(schema).append("RAM_CUSTOMER");
		int cnt = 0;
		PreparedStatement ps = null;
		try {
		ps = dbConn.prepareStatement(sb.toString());

		//Get the count off the first row.
		ResultSet rs = ps.executeQuery();
		if(rs.next())
			cnt = rs.getInt(1);
		} catch(SQLException sqle) {
			log.error("Error retrieving customer Count", sqle);
		} finally {
			DBUtil.close(ps);
		}
		log.debug("Count: " + cnt);
		return cnt;		
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
		StringBuilder sql = new StringBuilder(220);
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
			sql.append("CUSTOMER_NM = ?, ACTIVE_FLG = ?, ");
			sql.append("UPDATE_DT = ? WHERE CUSTOMER_ID = ?");
			msgAction = "updated";
			
		} else {
			// is an insert
			sql.append("insert into ").append(schema).append("RAM_CUSTOMER ");
			sql.append("(ORGANIZATION_ID, CUSTOMER_TYPE_ID, CUSTOMER_NM, ");
			sql.append("ACTIVE_FLG, CREATE_DT) values (?,?,?,?,?)");
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
			DBUtil.close(ps);
		}
		
		boolean isJson = Convert.formatBoolean(StringUtil.checkVal(req.getParameter("amid")).length() > 0);
		if (isJson) {
			Map<String, Object> res = new HashMap<>(); 
			res.put("success", true);
			putModuleData(res);
		} else {
	        // Build the redirect and messages
			// Setup the redirect.
			StringBuilder url = new StringBuilder(50);
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			url.append(page.getRequestURI());
			url.append("?msg=").append(msg);
			
			log.debug("CustomerAction redir: " + url);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, url.toString());
		}
	}
	
}
