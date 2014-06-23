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
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>CustomerSearchAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since Jun 17, 2014<p/>
 *<b>Changes: </b>
 * Jun 17, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class CustomerSearchAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public CustomerSearchAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public CustomerSearchAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("CustomerSearchAction retrieve...");
		
		int srchCustomerId = Convert.formatInteger(req.getParameter("srchCustomerId"), 0);
		String srchCity = StringUtil.checkVal(req.getParameter("srchCity"));
		String srchState = StringUtil.checkVal(req.getParameter("srchState"));
		int srchActiveFlag = Convert.formatInteger(req.getParameter("srchActiveFlag"), -1);
		
		List<CustomerVO> data = new ArrayList<>();
		String schema = (String)getAttribute("customDbSchema");
		StringBuilder sql = new StringBuilder();
		
		// if city or state specified, filter by location
		if (srchCity.length() > 0 || srchState.length() > 0) {
			sql.append("select a.*, b.CITY_NM, b.STATE_CD ");
			sql.append("from ").append(schema).append("RAM_CUSTOMER a ");
			sql.append("inner join ").append(schema).append("RAM_CUSTOMER_LOCATION b ");
			sql.append("on a.CUSTOMER_ID = b.CUSTOMER_ID ");	
		} else {
			sql.append("select a.* from ").append(schema).append("RAM_CUSTOMER a ");
		}
		
		sql.append("where CUSTOMER_TYPE_ID in ('OEM', 'PROVIDER') ");

		if (srchCustomerId > 0) sql.append("and CUSTOMER_ID = ? ");
		if (srchCity.length() > 0) sql.append("and CITY_NM like ? ");
		if (srchState.length() > 0) sql.append("and STATE_CD = ? ");
		if (srchActiveFlag > -1) sql.append("and ACTIVE_FLG = ? ");
		sql.append("order by CUSTOMER_NM");
		
		log.debug("CustomerSearchAction retrieve SQL: " + sql.toString() + "|" + srchCustomerId);
		int index = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			if (srchCustomerId > 0) ps.setInt(index++, srchCustomerId);
			if (srchCity.length() > 0) ps.setString(index++, srchCity);
			if (srchState.length() > 0) ps.setString(index++, srchState);
			if (srchActiveFlag > -1) ps.setInt(index++, srchActiveFlag);
			
			ResultSet rs = ps.executeQuery();
			int prevId = -1;
			int currId = -1;
			while (rs.next()) {
				currId = rs.getInt("CUSTOMER_ID");
				if (currId != prevId) {
					data.add(new CustomerVO(rs, false));
				}
				prevId = currId;
			}
		} catch (SQLException e) {
			log.error("Error retrieving RAM customer data, ", e);
		} finally {
			if (ps != null) {
				try { 	ps.close(); }
				catch (Exception e) { log.error("Error closing PreparedStatement, ", e); }
			}
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
	public void build(SMTServletRequest req) throws ActionException {}
	
}
