package com.ram.action.products;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import com.ram.action.provider.VisionAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: KitFacadeAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Facade Action for the RAM Kit DataTool.  This Action
 * looks at the stage variable on the requests and ensures that the proper 
 * action is called to process the data.
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
public class ProductFacadeAction extends SBActionAdapter {

	public static final String STEP_PARAM = "bType";
	public static enum KIT_STEP {product, productRecall, kitLayer, kitProduct, customer}
	/**
	 * Default Constructor
	 */
	public ProductFacadeAction() {
		super();
		
	}
	/**
	 * Generic Constructor with ActionInitVO data
	 * @param actionInit
	 */
	public ProductFacadeAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		String step = StringUtil.checkVal(req.getParameter(STEP_PARAM));
		getAction(step).build(req);

		//Ensure that any product/kit Edits that could affect a cached Kit get updated.
		if(req.hasParameter("productId") || req.hasParameter("kitLayerId")) {
			checkProductForKit(Convert.formatInteger(req.getParameter("productId")), Convert.formatInteger(req.getParameter("kitLayerId")));
		}
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String step = StringUtil.checkVal(req.getParameter(STEP_PARAM));
		getAction(step).retrieve(req);
	}
	
	/**
	 * Build the Action and set the necessary pieces on it before returning it.
	 * @param step
	 * @return
	 */
	public SMTActionInterface getAction(String step) {
		SMTActionInterface action = null;
		try {
			switch(KIT_STEP.valueOf(step)) {
				case kitLayer:
					action = new KitLayerAction(actionInit);
					break;
				case kitProduct:
					action = new KitLayerProductAction(actionInit);
					break;
				case productRecall:
					action = new ProductRecallAction(actionInit);
					break;
				case customer:
					action = new CustomerProductAction(actionInit);
					break;
				case product:
				default:
					action = new ProductAction(actionInit);
					break;
			}
		} catch(IllegalArgumentException iae) {
			action = new ProductAction(actionInit);
		}
		action.setDBConnection(dbConn);
		action.setAttributes(attributes);
		return action;
	}

	/**
	 * Helper method that ensures related kit cache groups are flushed
	 * successfully after a related product is updated.
	 * @param productId
	 */
	private void checkProductForKit(int productId, int kitLayerId) {
		log.debug("Checking Kit XRs for productId: " + productId);
		try (PreparedStatement ps = dbConn.prepareStatement(getProductKitCheckSQL())) {
			ps.setInt(1, productId);
			ps.setInt(2, productId);
			ps.setInt(3, kitLayerId);
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				log.debug("Flushing cache group: " + VisionAction.CACHE_PREFIX + rs.getInt("PRODUCT_ID"));
				super.clearCacheByKey(VisionAction.CACHE_PREFIX + rs.getInt("PRODUCT_ID"));
			}
		} catch (SQLException e) {
			log.error(e);
		}
	}

	/**
	 * Helper method that builds the Kit Lookup Query.
	 * @return
	 */
	private String getProductKitCheckSQL() {
		StringBuilder sql = new StringBuilder(550);

		sql.append("select distinct a.PRODUCT_ID from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("RAM_KIT_LAYER a ");
		sql.append("inner join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("RAM_PRODUCT_LAYER_XR b ");
		sql.append("on a.KIT_LAYER_ID = b.KIT_LAYER_ID ");
		sql.append("inner join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("RAM_PRODUCT c ");
		sql.append("on b.PRODUCT_ID = c.PRODUCT_ID ");
		sql.append("inner join ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("RAM_PRODUCT d ");
		sql.append("on a.PRODUCT_ID = d.PRODUCT_ID ");
		sql.append("where c.PRODUCT_ID = ? or d.PRODUCT_ID = ? or a.KIT_LAYER_ID = ?");

		log.debug(sql.toString());

		return sql.toString();
	}
}
