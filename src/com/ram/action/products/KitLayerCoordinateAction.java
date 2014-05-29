package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.ram.datafeed.data.LayerCoordinateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: KitLayerCoordinateAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Action that handles saving coordinate data for Kit
 * Products.
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
public class KitLayerCoordinateAction extends SBActionAdapter {

	public static final String PRODUCT_KIT_ID = "productKitId";
	public static final String PRODUCT_KIT_ID_LIST = "productKitIdList";

	/**
	 * Default Constructor
	 */
	public KitLayerCoordinateAction() {
		super();

	}

	/**
	 * General Constructor with ActionInitVO Data
	 * 
	 * @param actionInit
	 */
	public KitLayerCoordinateAction(ActionInitVO actionInit) {
		super(actionInit);

	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http
	 * .SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		
		StringBuilder sb = new StringBuilder();
		sb.append("delete from RAM_LAYER_COORDINATE where PRODUCT_KIT_ID = ?");
		int productKitId = Convert.formatInteger(req.getParameter(PRODUCT_KIT_ID));
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setInt(1, productKitId);
			ps.execute();
		} catch (SQLException sqle) {
			log.error("Failed to delete product Coordinates with Id: " + productKitId, sqle);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.
	 * SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		
		//Get ProductKitId off request
		int productKitId = Convert.formatInteger(req.getParameter(PRODUCT_KIT_ID));
		
		//Create List to hold LayerCoordinateVOs
		List<LayerCoordinateVO> vos = new ArrayList<LayerCoordinateVO>();
		
		//Build SQL Statement
		StringBuilder sb = new StringBuilder();
		sb.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sb.append("RAM_LAYER_COORDINATE ");
		sb.append(buildWhereClause(productKitId));
		
		//Query Db for Coordinates with given ProductKitId and save
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				vos.add(new LayerCoordinateVO(rs, true));
			}
		} catch (SQLException sqle) {
			log.error(sqle);
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http
	 * .SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {

	}
	
	/**
	 * Return the where clause to be used on the list and delete queries.
	 * The method checks for the presence of a list of ProductKitId's and
	 * if present builds out an inclusive where clause, otherwise we return
	 * a singular where clause with the given productKitId.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	public String buildWhereClause(int productKitId) {
		StringBuilder sb = new StringBuilder();
		sb.append("where ");
		if(attributes.containsKey(PRODUCT_KIT_ID_LIST)) {
			List<String> ids = (List<String>)attributes.get(PRODUCT_KIT_ID_LIST);
			sb.append("PRODUCT_KIT_ID in (");
			for(int i = 0; i < ids.size(); i++) {
				if(i > 0)
					sb.append(", ");
				sb.append(ids.get(i));
			}
			sb.append(")");
		} else {
			sb.append("PRODUCT_KIT_ID = ").append(productKitId);
		}
		return sb.toString();

	}

}
