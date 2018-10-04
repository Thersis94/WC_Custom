package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
// WSLA Libs
import com.wsla.data.product.ProductVO;

/****************************************************************************
 * <b>Title</b>: ProductMasterAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the product_master table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Sep 24, 2018
 * @updates:
 ****************************************************************************/

public class ProductMasterAction extends SBActionAdapter {
	
	public static final String REQ_PRODUCT_ID = "productId";

	public ProductMasterAction() {
		super();
	}
	
	/**
	 * 
	 * @param actionInit
	 */
	public ProductMasterAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Helper Method to initialize class
	 * @param attributes
	 * @param dbConn
	 */
	public ProductMasterAction(Map<String, Object> attributes, SMTDBConnection dbConn) {
		this();
		setAttributes(attributes);
		setDBConnection(dbConn);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String productId = req.getParameter(REQ_PRODUCT_ID);
		String providerId = req.getParameter("providerId");
		Integer setFlag = req.hasParameter("setFlag") ? Convert.formatInteger(req.getParameter("setFlag")) : null;
		setModuleData(getProducts(productId, providerId, setFlag, new BSTableControlVO(req, ProductVO.class)));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductVO product = new ProductVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			db.save(product);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save product", e);
		}
	}


	/**
	 * Gets a list of products - either by ID or for the given provider.
	 * @param productId
	 * @param providerId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProductVO> getProducts(String productId, String providerId, Integer setFlag, BSTableControlVO bst) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select pm.*, p.provider_nm from ").append(schema).append("wsla_product_master pm ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider p on pm.provider_id=p.provider_id ");
		sql.append("where 1=1 ");
		List<Object> params = new ArrayList<>();

		// Filter by provider id
		if (! StringUtil.checkVal(productId).isEmpty()) {
			sql.append("and pm.product_id=? ");
			params.add(productId);
		}

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (pm.product_nm like ? or pm.cust_product_id like ? or pm.sec_cust_product_id like ? or p.provider_nm like ?) ");
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
		}

		// Filter by provider type
		if (!StringUtil.isEmpty(providerId)) {
			sql.append("and pm.provider_id=? ");
			params.add(providerId);
		}

		if (setFlag != null) {
			sql.append("and pm.set_flg=? ");
			params.add(setFlag);
		}

		sql.append(bst.getSQLOrderBy("pm.product_nm",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ProductVO(), bst.getLimit(), bst.getOffset());
	}
}