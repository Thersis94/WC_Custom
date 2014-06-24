package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.datafeed.data.KitLayerVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
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
		Map<String, String> result = new HashMap<String, String>();
		result.put("success", "true");
		result.put("msg", "Data Successfully Updated");
		
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
			sb.append("RAM_KIT_LAYER (IMAGE_PATH_URL, LAYOUT_DEPTH_NO, CREATE_DT, ");
			sb.append("ACTIVE_FLG, PRODUCT_ID) values (?,?,?,?,?)");
		}

		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("imagePathUrl"));
			ps.setString(2, req.getParameter("layoutDepthNo"));
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setInt(4, Convert.formatInteger(Convert.formatBoolean(req.getParameter("activeFlag"))));
			if(req.hasParameter(KIT_LAYER_ID)) {
				ps.setInt(5, Convert.formatInteger(req.getParameter(KIT_LAYER_ID)));
			} else {
				ps.setInt(5, Convert.formatInteger(req.getParameter(KitAction.KIT_ID)));
			}
			ps.execute();
		} catch(SQLException sqle) {
			log.error(sqle);
			result.put("success", "false");
			result.put("msg", "Problem Saving Record");
		} finally {
			try {
				ResultSet rs = ps.getGeneratedKeys();
				if(rs.next())
					result.put("kitLayerId", rs.getString(1));

			} catch(Exception e) {}
		}
		super.putModuleData(result);

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
		sb.append("where KIT_LAYER_ID = ? order by LAYER_DEPTH_NO");

		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
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
	public void retrieve(SMTServletRequest req) throws ActionException {
		
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
		
		sb.append(" order by LAYOUT_DEPTH_NO, KIT_LAYER_ID");
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			if(isKitLayerLookup) {
				ps.setString(1, req.getParameter(KIT_LAYER_ID));
			} else {
				ps.setString(1, req.getParameter("productId"));
			}
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				layers.add(new KitLayerVO(rs, false));
			}
		} catch(SQLException sqle) {
			log.error(sqle);
		}
		
		this.putModuleData(layers, layers.size(), false);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		
	}
	
}
