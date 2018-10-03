package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Collections;
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
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.ProductWarrantyVO;

/****************************************************************************
 * <b>Title</b>: ProductSerialAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the product_serial table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 2, 2018
 * @updates:
 ****************************************************************************/

public class ProductSerialAction extends SBActionAdapter {

	public ProductSerialAction() {
		super();
	}

	public ProductSerialAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public ProductSerialAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		setAttributes(attrs);
		setDBConnection(conn);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String productId = req.getParameter("productId");
		
		if (!StringUtil.isEmpty(productId) && req.hasParameter("serialNo")) {
			//do serial# lookup for the ticket UI
			setModuleData(getProductSerial(productId, req.getParameter("serialNo")));
		} else {
			setModuleData(getSet(productId, new BSTableControlVO(req, ProductSerialNumberVO.class)));
		}
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductSerialNumberVO vo = new ProductSerialNumberVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save product serial", e);
		}
	}


	/**
	 * Return a list of products included in the requested set.
	 * @param setId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProductSerialNumberVO> getSet(String productId, BSTableControlVO bst) {
		if (StringUtil.isEmpty(productId)) return null;

		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select s.*, p.product_nm from ").append(schema).append("wsla_product_serial s ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master p on s.product_id=p.product_id ");
		sql.append("where 1=1 ");

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (p.product_nm like ? or p.cust_product_id like ? or p.sec_cust_product_id like ? or s.serial_no_txt like ?) ");
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
		}

		// always filter by productId
		sql.append("and s.product_id=? ");
		params.add(productId);

		sql.append(bst.getSQLOrderBy("s.serial_no_txt",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ProductSerialNumberVO(), bst.getLimit(), bst.getOffset());
	}


	/**
	 * Lookup a user-provided serial# to validate it.  Called from the UI (ajax).
	 * @param productId
	 * @param serialNo
	 * @return a list of VOs (rows in the table)
	 */
	public List<ProductWarrantyVO> getProductSerial(String productId, String serialNo) {
		if (StringUtil.isEmpty(productId) || StringUtil.isEmpty(serialNo))
			return Collections.emptyList();
		
		StringBuilder sql = new StringBuilder(256);
		sql.append("select * from wsla_product_serial a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("wsla_product_warranty b ");
		sql.append("on a.product_serial_id = b.product_serial_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("wsla_warranty c on ");
		sql.append("b.warranty_id = c.warranty_id ");
		sql.append("where lower(serial_no_txt) = ? and product_id = ? ");		
		
		List<Object> vals = new ArrayList<>();
		vals.add(serialNo.toLowerCase());
		vals.add(productId);
		log.info(sql.length() + "|" + sql);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), vals, new ProductWarrantyVO());
	}
}