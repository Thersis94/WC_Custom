package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.datafeed.data.KitLayerProductVO;
import com.ram.datafeed.data.LayerCoordinateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: KitLayerProductAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that handles binding Products to a Kit Layer.
 * 
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
public class KitLayerProductAction extends SBActionAdapter {

	/**
	 * Default Constructor
	 */
	public KitLayerProductAction() {
		super();
		
	}

	/**
	 * General Constructor with ActionInitVO Data
	 * @param actionInit
	 */
	public KitLayerProductAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		
		List<KitLayerProductVO> layers = new ArrayList<KitLayerProductVO>();
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		KitLayerProductVO prodVO = null;

		//Build Query
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(customDb).append("RAM_PRODUCT_LAYER_XR a ");
		sb.append("inner join ").append(customDb).append("RAM_LAYER_COORDINATE b ");
		sb.append("on a.PRODUCT_KIT_ID = b.PRODUCT_KIT_ID ");
		sb.append("inner join ").append(customDb).append("RAM_PRODUCT c ");
		sb.append("on a.product_id = c.product_id ");
		sb.append("where a.KIT_LAYER_ID = ?");
		
		//Log sql Statement for verification
		log.debug("sql: " + sb.toString());
		
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, req.getParameter("kitLayerId"));
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				if(prodVO == null) {
					prodVO = new KitLayerProductVO(rs, false);
				} else if(!prodVO.getProductKitId().equals(rs.getString("product_kit_id"))) {
					layers.add(prodVO);
					prodVO = new KitLayerProductVO(rs, false);
				}
				prodVO.addCoordinate(new LayerCoordinateVO(rs, false));
			}
		} catch(SQLException sqle) {
			log.error(sqle);
		}
		
		this.putModuleData(layers, layers.size(), false);
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
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
	}

	
}
