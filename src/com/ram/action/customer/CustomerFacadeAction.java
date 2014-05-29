package com.ram.action.customer;

// JDK 7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

// RAMDataFeed
import com.ram.datafeed.data.CustomerVO;

//SMTBaseLibs 2.0
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 2.0
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title: </b>CustomerFacadeAction.java <p/>
 * <b>Project: </b>WC_Custom <p/>
 * <b>Description: </b>
 * </p>
 * <b>Copyright: </b>Copyright (c) 2014<p/>
 * <b>Company: </b>Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0<p/>
 * @since May 19, 2014<p/>
 *<b>Changes: </b>
 * May 19, 2014: David Bargerhuff: Created class.
 ****************************************************************************/
public class CustomerFacadeAction extends SBActionAdapter {
	
	/**
	 * 
	 */
	public CustomerFacadeAction() {
		super(new ActionInitVO());
	}

	/**
	 * @param actionInit
	 */
	public CustomerFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("CustomerFacadeAction retrieve...");
		boolean searchSubmitted = Convert.formatBoolean(req.getParameter("searchSubmitted"));
		if (searchSubmitted) {
			// perform search
			performSearch(req);
		} else {
			SMTActionInterface sai = null;
			boolean ft = Convert.formatBoolean(req.getParameter("facadeType"));
			if (ft) {
				// CustomerLocationAction
				sai = new CustomerLocationAction(actionInit);
				sai.setAttributes(attributes);
				sai.setDBConnection(dbConn);
				sai.retrieve(req);
				
			} else {
				// CustomerAction
				sai = new CustomerAction(actionInit);
				sai.setAttributes(attributes);
				sai.setDBConnection(dbConn);
				sai.retrieve(req);
			}
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("CustomerFacadeAction build...");
		boolean searchSubmitted = Convert.formatBoolean(req.getParameter("searchSubmitted"));
		if (searchSubmitted) {
			// perform search
			performSearch(req);
		} else {
			SMTActionInterface sai = null;
			boolean ft = Convert.formatBoolean(req.getParameter("facadeType"));
			if (ft) {
				// CustomerLocationAction update
				sai = new CustomerLocationAction(actionInit);
				sai.setAttributes(attributes);
				sai.setDBConnection(dbConn);
				sai.build(req);
			} else {
				// CustomerAction update
				sai = new CustomerAction(actionInit);
				sai.setAttributes(attributes);
				sai.setDBConnection(dbConn);
				sai.build(req);
			}
		}
	}
	
	/**
	 * Performs search against customer table(s)
	 * @param req
	 */
	private void performSearch(SMTServletRequest req) {
		log.debug("CustomerFacadeAction performSearch...");
		String schema = (String)getAttribute("customDbSchema");
		int customerId = Convert.formatInteger(StringUtil.checkVal(req.getParameter("srchCustomerId")));
		String customerName = StringUtil.checkVal(req.getParameter("srchCustomerName"));
		String srchCity = StringUtil.checkVal(req.getParameter("srchCity"));
		String srchState = StringUtil.checkVal(req.getParameter("srchState"));
		// ensure we search only for 'active' customers, otherwise use flag.
		String activeFlag = StringUtil.checkVal(req.getParameter("activeFlag"), "1");

		StringBuilder sql = new StringBuilder();
		sql.append("select a.* ");
		
		if (srchCity.length() > 0 || srchState.length() > 0) {
			sql.append(", b.customer_location_id, b.region_id, b.location_nm, ");
			sql.append("b.address_txt, b.address2_txt, b.city_nm, b.state_cd, b.country_cd, ");
			sql.append("b.latitude_no, b.longitude_no, b.match_cd, b.stocking_location_txt, ");
			sql.append("b.active_flg ");
			sql.append("from ").append(schema).append("RAM_CUSTOMER a ");
			sql.append("inner join ").append(schema).append("RAM_CUSTOMER_LOCATION b ");
			sql.append("on a.CUSTOMER_ID = b.CUSTOMER_ID ");
		} else {
			sql.append("from ").append(schema).append("RAM_CUSTOMER a ");
		}
				
		sql.append("where 1 = 1 and active_flg = ? ");
		StringBuilder orderBy = new StringBuilder();
		if (customerId > 0) {
			sql.append("and a.CUSTOMER_ID = ? ");
			orderBy.append("a.CUSTOMER_ID");
			log.debug("customerId: " + customerId);
		}
		if (customerName.length() > 0) {
			sql.append("and a.CUSTOMER_NM like ? ");
			if (orderBy.length() > 0) orderBy.append(",");
			log.debug("customerName: " + customerName);
		}
		if (srchCity.length() > 0) {
			sql.append("and b.CITY_NM = ? ");
			log.debug("city name: " + srchCity);
		}
		if (srchState.length() > 0) {
			sql.append("and b.STATE_CD = ? ");
			log.debug("state cd: " + srchState);
		}
		
		if (orderBy.length() > 0) orderBy.append(",");
		orderBy.append("a.CUSTOMER_NM");
		
		sql.append("order by ").append(orderBy);
		
		log.debug("Customer search SQL: " + sql.toString());
		
		PreparedStatement ps = null;
		int index = 1;
		List<CustomerVO> data = new ArrayList<>();
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setInt(index++,  Convert.formatInteger(activeFlag));
			if (customerId > 0) {
				ps.setInt(index++,  customerId);
			}
			if (customerName.length() > 0) {
				ps.setString(index++,  "%" + customerName + "%");
			}
			if (srchCity.length() > 0) {
				ps.setString(index++, srchCity);
			}
			if (srchState.length() > 0) {
				ps.setString(index++, srchState);
			}
			
			ResultSet rs = ps.executeQuery();
			String prevId = null;
			String currId = null;
			while (rs.next()) {
				log.debug("examing customerId: " + rs.getString("CUSTOMER_ID"));
				currId = rs.getString("CUSTOMER_ID");
				if (currId.equals(prevId)) {
					continue;
				} else {
					CustomerVO cvo = new CustomerVO(rs, false);
					data.add(cvo);
				}
			}
			
		} catch (SQLException sqle) {
			log.error("Error performing customer search, ", sqle);
			
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch (Exception e) { log.error("Error closing PreparedStatement, ", e);}
			}
		}
		
		log.debug("data size: " + data.size());
		
		ModuleVO modVo = (ModuleVO) attributes.get(Constants.MODULE_DATA);
        modVo.setDataSize(data.size());
        modVo.setActionData(data);
        req.setAttribute(Constants.MODULE_DATA, modVo);
		
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
        super.retrieve(req);
	}

}
