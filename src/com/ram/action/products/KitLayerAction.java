package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.datafeed.data.KitLayerVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: KitLayerAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that handles all calls to creating and editing
 * Kit Layers for a product. The images for the kits are bound here.
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
 *************************************************************************** 
 */
public class KitLayerAction extends SBActionAdapter {

	public static final String KIT_LAYER_ID = "kitLayerId";
	
	/**
	 * Default Constructor
	 */
	public KitLayerAction() {
		super();
	}

	/**
	 * General Constructor with ActionInitVO Data
	 * @param actionInit
	 */
	public KitLayerAction(ActionInitVO actionInit) {
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
		sb.append("RAM_KIT_LAYER set ACTIVE_FLG = 0 ");
		sb.append("where KIT_LAYER_ID = ?");
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter(KIT_LAYER_ID));
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
		if(StringUtil.checkVal(req.getParameter(KIT_LAYER_ID)).equals(KitAction.ADD_ID))
			return;
		
		List<KitLayerVO> layers = new ArrayList<KitLayerVO>();
		
		//Boolean for kitId check
		boolean isKitLayerLookup = req.hasParameter(KIT_LAYER_ID);
				
		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_KIT_LAYER where ");

		if(isKitLayerLookup) {
			sb.append("KIT_LAYER_ID = ?");
		} else {
			sb.append("PRODUCT_ID = ?");
		}
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			if(isKitLayerLookup) {
				ps.setString(1, req.getParameter(KIT_LAYER_ID));
			} else {
				ps.setString(1, req.getParameter(KitAction.KIT_ID));
			}
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				layers.add(new KitLayerVO(rs, false));
			}
		} catch(SQLException sqle) {
			log.error(sqle);
		}
		
		this.putModuleData(layers);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		//Build Query
		StringBuilder sb = new StringBuilder();
		if(req.hasParameter(KIT_LAYER_ID)) {
			sb.append("update ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("RAM_KIT_LAYER set IMAGE_PATH_URL = ?, LAYOUT_DEPTH_NO = ?, ");
			sb.append("UPDATE_DT = ?, ACTIVE_FLG = ? ");
			sb.append("where KIT_LAYER_ID = ?");
		}
		else {
			sb.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
			sb.append("RAM_KIT_LAYER (IMAGE_PATH_URL LAYOUT_DEPTH_NO, CREATE_DT, ");
			sb.append("ACTIVE_FLG, PRODUCT_ID) values (?,?,?,?,?)");
		}
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("imagePathUrl"));
			ps.setString(2, req.getParameter("layoutDepthNo"));
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, req.getParameter(KitAction.ACTIVE_FLG));
			if(req.hasParameter(KIT_LAYER_ID)) {
				ps.setString(5, req.getParameter(KIT_LAYER_ID));
			} else {
				ps.setString(5, KitAction.KIT_ID);
			}
			ps.execute();
			((ModuleVO)attributes.get(Constants.MODULE_DATA)).setErrorMessage("Update Successful");
		} catch(SQLException sqle) {
			log.error(sqle);
			((ModuleVO)attributes.get(Constants.MODULE_DATA)).setErrorMessage("Update Failed");
		}
		
		//Redirect User
	}
	
}
