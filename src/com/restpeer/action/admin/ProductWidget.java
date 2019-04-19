package com.restpeer.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// RP Libs
import com.restpeer.data.ProductVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: ProductWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the products being sold
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 13, 2019
 * @updates:
 ****************************************************************************/

public class ProductWidget extends SBActionAdapter {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "product";
	
	/**
	 * 
	 */
	public ProductWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public ProductWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * 
	 * @param dbConn
	 * @param attributes
	 */
	public ProductWidget(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		putModuleData(getProducts(req.getParameter("parentCode"), false));
	}
	
	/**
	 * 
	 * @param parentCode
	 * @return
	 */
	public List<ProductVO> getProducts(String parentCode,boolean all) {
		StringBuilder sql = new StringBuilder(164);
		sql.append("select * from ").append(getCustomSchema()).append("rp_product a ");
		sql.append("left outer join ").append(getCustomSchema()).append("rp_category b ");
		sql.append("on a.category_cd = b.category_cd ");
		sql.append("where 1=1 ");
		
		List<Object> vals = new ArrayList<>();
		if (!all && !StringUtil.isEmpty(parentCode)) {
			sql.append("and parent_cd = ? ");
			vals.add(parentCode);
		} else if (! all) {
			sql.append("and parent_cd is null ");
		}
		
		if (all) sql.append("order by group_cd, parent_cd desc, product_nm ");
		else sql.append("order by order_no, hours_week_no, price_no, product_nm, a.category_cd");
		log.debug(sql.length() + "|" + sql);
		
		// Execute and return the data
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new ProductVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductVO product = new ProductVO(req);
		if (StringUtil.checkVal(req.getParameter("parentCode")).isEmpty())
			product.setParentCode(null);
		
		try {
			if (req.getBooleanParameter("delete")) deleteProduct(product);
			else saveProduct(product, req.getBooleanParameter("isInsert"));
			
			setModuleData(product);
		} catch (Exception e) {
			log.error("Unable to save product: " + product, e);
			setModuleData(product, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Removes an product item
	 * @param product
	 * @throws DatabaseException
	 */
	public void deleteProduct(ProductVO product) throws DatabaseException {
		
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.delete(product);
		} catch (Exception e) {
			throw new DatabaseException("inable to delete product: " + product, e);
		}
	}
	
	/**
	 * Saves the product data
	 * @param product
	 * @param isInsert
	 * @throws DatabaseException
	 */
	public void saveProduct(ProductVO product, boolean isInsert) throws DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (isInsert) db.insert(product);
			else db.update(product);
		
		} catch (Exception e) {
			throw new DatabaseException("Unable to save product: " + product, e);
		}
		
	}
}

