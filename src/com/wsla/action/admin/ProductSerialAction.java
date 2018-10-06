package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.wsla.data.product.ProductSerialNumberVO;
import com.wsla.data.product.ProductWarrantyVO;
import com.wsla.data.product.WarrantyVO;

import static com.wsla.action.admin.ProductMasterAction.REQ_PRODUCT_ID;

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

public class ProductSerialAction extends BatchImport {

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
		String productId = req.getParameter(REQ_PRODUCT_ID);

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
		sql.append("select s.*, w.desc_txt as warranty_nm from ").append(schema).append("wsla_product_serial s ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master p on s.product_id=p.product_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_warranty pw on s.product_serial_id=pw.product_serial_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_warranty w on pw.warranty_id=w.warranty_id ");
		sql.append("where 1=1 ");

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (lower(p.product_nm) like ? or lower(p.cust_product_id) like ? or lower(p.sec_cust_product_id) like ? or lower(s.serial_no_txt) like ?) ");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
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

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), vals, new ProductWarrantyVO());
	}


	/* (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#getBatchImportableClass()
	 */
	@Override
	protected Class<?> getBatchImportableClass() {
		return ProductSerialNumberVO.class;
	}


	/**
	 * Remove any entries that are already in the system (compare by serial#)
	 * (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#validateBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void validateBatchImport(ActionRequest req,
			ArrayList<? extends Object> entries) throws ActionException {
		String sql = StringUtil.join("select lower(serial_no_txt) as key from ", getCustomSchema(), 
				"wsla_product_serial where product_id=?");

		// load this product's SKUs from the DB and store them as a Set for quick reference.
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<GenericVO> serialNos = db.executeSelect(sql, Arrays.asList(req.getParameter(REQ_PRODUCT_ID)), new GenericVO());
		Set<String> skus = new HashSet<>(serialNos.size());
		for (GenericVO vo : serialNos)
			skus.add(StringUtil.checkVal(vo.getKey()));

		//remove items from the batch import which are already in the database
		Iterator<? extends Object> iter = entries.iterator();
		while (iter.hasNext()) {
			ProductSerialNumberVO vo = (ProductSerialNumberVO) iter.next();
			if (StringUtil.isEmpty(vo.getSerialNumber()) || skus.contains(vo.getSerialNumber().toLowerCase())) {
				iter.remove();
				log.debug("omitting pre-existing or empty SKU: " + vo.getSerialNumber());
			}
		}
	}


	/**
	 * give some additional default values to the records about to be inserted
	 * (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#transposeBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void transposeBatchImport(ActionRequest req, ArrayList<? extends Object> entries)
			throws ActionException {
		//set the productId for all beans to the one passed on the request
		String productId = req.getParameter(REQ_PRODUCT_ID);
		Date dt = Calendar.getInstance().getTime();
		for (Object obj : entries) {
			ProductSerialNumberVO vo = (ProductSerialNumberVO) obj;
			vo.setProductId(productId);
			vo.setValidatedFlag(1); //vendor-provided file (not customer via ticket request), mark these all as validated
			vo.setRetailerDate(dt); //default to today for retailer issued date.
		}
	}


	/**
	 * Insert the new serial# records, then create warranty records for each
	 * (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#saveBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void saveBatchImport(ActionRequest req, ArrayList<? extends Object> entries)
			throws ActionException {
		//save the product_serial table
		super.saveBatchImport(req, entries);

		//save the product_warranty table
		addProductWarranties(req, entries);
	}


	/**
	 * Creates a product_warranty record for each of the serials#s saved
	 * @param req
	 * @param entries
	 * @throws ActionException 
	 */
	private void addProductWarranties(ActionRequest req, ArrayList<? extends Object> entries)
			throws ActionException {
		String warrantyId = req.getParameter("warrantyId");
		if (StringUtil.isEmpty(warrantyId)) return;

		//get the warranty, then calculate expiration date for the product based on the warrantyDays, from today.
		Calendar today = Calendar.getInstance();
		WarrantyVO warranty = new WarrantyAction(getAttributes(), getDBConnection()).getWarranty(warrantyId);
		today.add(Calendar.DATE, warranty.getWarrantyLength()); //e.g. add 90 days to today.
		Date expDate = today.getTime();

		ArrayList<ProductWarrantyVO> data = new ArrayList<>(entries.size());
		for (Object obj : entries) {
			ProductSerialNumberVO vo = (ProductSerialNumberVO) obj;
			data.add(new ProductWarrantyVO(vo.getProductSerialId(), warrantyId, expDate));
		}
		//push these through the same batch-insert logic
		super.saveBatchImport(req, data);
	}
}