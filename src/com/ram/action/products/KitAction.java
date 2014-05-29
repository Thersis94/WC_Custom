/**
 * 
 */
package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.datafeed.data.RAMProductVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: KitAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that handles base information about the kits and
 * updating a product to support a kit.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since May 20, 2014
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class KitAction extends SBActionAdapter {

	public static final String KIT_ID = "kitId";
	public static final String ADD_ID = "ADD";
	public static final String ACTIVE_FLG = "activeFlg";
	public static final String CUSTOMER_ID = "customerId";
	public static final String PRODUCT_NM = "productNm";
	
	/**
	 * Default Constructor
	 */
	public KitAction() {
		super();
		
	}

	/**
	 * General Constructor with ActionInitVO Data
	 * @param actionInit
	 */
	public KitAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT set ACTIVE_FLG = 0 ");
		sb.append("where PRODUCT_ID = ?");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter(KIT_ID));
			ps.execute();
			((ModuleVO)attributes.get(Constants.MODULE_DATA)).setErrorMessage("Deactivation Successful");
		} catch(SQLException sqle) {
			log.error(sqle);
			((ModuleVO)attributes.get(Constants.MODULE_DATA)).setErrorMessage("Deactivation Failed");
		}
		
		//Redirect User
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		
		//If this is an add request, quick fail and return.
		if(StringUtil.checkVal(req.getParameter(KIT_ID)).equals(ADD_ID))
			return;
		
		List<RAMProductVO> products = new ArrayList<RAMProductVO>();
		
		//Boolean for kitId check
		boolean isKitLookup = req.hasParameter(KIT_ID);
		
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT where KIT_FLG = 1");
		
		//Check for kitId
		if(isKitLookup) {
			sb.append(" and PRODUCT_ID = ?");
		}
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			if(isKitLookup) {
				ps.setString(1, req.getParameter(KIT_ID));
			}
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				products.add(new RAMProductVO(rs));
			}
		} catch(SQLException sqle) {
			log.error(sqle);
		}
		
		this.putModuleData(products);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_PRODUCT set ACTIVE_FLG = ?, KIT_FLG = 1 ");
		sb.append("where PRODUCT_ID = ?");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter(ACTIVE_FLG));
			ps.setString(2, req.getParameter(KIT_ID));
			ps.execute();
			((ModuleVO)attributes.get(Constants.MODULE_DATA)).setErrorMessage("Update Successful");
		} catch(SQLException sqle) {
			log.error(sqle);
			((ModuleVO)attributes.get(Constants.MODULE_DATA)).setErrorMessage("Update Failed");
		}
		
		//Redirect User
		
	}
	
	
}
