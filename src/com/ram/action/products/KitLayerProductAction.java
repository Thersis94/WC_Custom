package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ram.datafeed.data.KitLayerProductVO;
import com.ram.datafeed.data.LayerCoordinateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
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
		sb.append("left outer join ").append(customDb).append("RAM_LAYER_COORDINATE b ");
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
				} else if(!prodVO.getProductKitId().equals(rs.getInt("product_kit_id"))) {
					layers.add(prodVO);
					prodVO = new KitLayerProductVO(rs, false);
				}
				prodVO.addCoordinate(new LayerCoordinateVO(rs, false));
			}
			
			//Add final productVO
			layers.add(prodVO);
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
		Map<String, String> result = new HashMap<String, String>();
		result.put("success", "true");
		result.put("msg", "Data Successfully Updated");
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		String [] values = null;
		List<KitLayerProductVO> inserts = new ArrayList<KitLayerProductVO>();
		List<KitLayerProductVO> updates = new ArrayList<KitLayerProductVO>();
		KitLayerProductVO vo = null;
		StringBuilder insert = new StringBuilder();
		insert.append("insert into ").append(customDb).append("RAM_PRODUCT_LAYER_XR (PRODUCT_ID, ");
		insert.append("KIT_LAYER_ID, COORDINATE_TYPE_CD, CREATE_DT, ACTIVE_FLG) ");
		insert.append("values (?,?,?,?,?)");
		
		StringBuilder update = new StringBuilder();
		update.append("update ").append(customDb).append("RAM_PRODUCT_LAYER_XR set PRODUCT_ID = ?, ");
		update.append("COORDINATE_TYPE_CD = ?, UPDATE_DT = ?, ");
		update.append("ACTIVE_FLG = ? where PRODUCT_KIT_ID = ?");
		
		for(String s : req.getParameterMap().keySet())
			if(s.startsWith("kitProduct_")) {
				values = req.getParameter(s).split("\\|");
				vo = new KitLayerProductVO();
				vo.setProductKitId(Convert.formatInteger(values[0]));
				vo.setProductId(Convert.formatInteger(values[1]));
				vo.setKitLayerId(Convert.formatInteger(values[2]));
				vo.setCoordinateType(values[3]);
				vo.setActiveFlag(Convert.formatInteger(values[4]));
				
				if(!(vo.getProductId() > 0)) {
					//Ignore these ones.
				}
				else if(vo.getProductKitId() > 0)
					updates.add(vo);
				else
					inserts.add(vo);	
			}
		PreparedStatement ps = null;
		try {
			//Insert new Records
			log.debug(insert);
			ps = dbConn.prepareStatement(insert.toString());
			for(KitLayerProductVO v : inserts) {
				ps.setInt(1, v.getProductId());
				ps.setInt(2, v.getKitLayerId());
				ps.setString(3, v.getCoordinateType().name());
				ps.setTimestamp(4, Convert.getCurrentTimestamp());
				ps.setInt(5, v.getActiveFlag());
				ps.addBatch();
			}
			ps.executeBatch();
			ps.close();
			
			//Update Existing Records
			log.debug(update.toString());
			ps = dbConn.prepareStatement(update.toString());
			for(KitLayerProductVO v : updates) {
				ps.setInt(1, v.getProductId());
				ps.setString(2, v.getCoordinateType().name());
				ps.setTimestamp(3, Convert.getCurrentTimestamp());
				ps.setInt(4, v.getActiveFlag());
				ps.setInt(5, v.getProductKitId());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch(SQLException sqle) {
			log.error("Problem inserting/updating Kit Layer Products.", sqle);
			result.put("success", "false");
			result.put("msg", "Problem Saving Record");
		} finally {
			try {
				ps.close();
			} catch(Exception e){}
		}
		//super.putModuleData(result);
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
