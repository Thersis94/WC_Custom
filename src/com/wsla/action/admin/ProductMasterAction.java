package com.wsla.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.product.ProductCategoryAssociationVO;
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
public class ProductMasterAction extends BatchImport {

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
			sql.append("and (lower(pm.product_nm) like ? or lower(pm.cust_product_id) like ? ");
			sql.append("or lower(pm.sec_cust_product_id) like ? or lower(p.provider_nm) like ?) ");
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
			params.add(bst.getLikeSearch().toLowerCase());
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

	/* (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#getBatchImportableClass()
	 */
	@Override
	protected Class<?> getBatchImportableClass() {
		return ProductVO.class;
	}


	/**
	 * Remove any entries that are already in the system (compare to provider's other SKUs)
	 * (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#validateBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void validateBatchImport(ActionRequest req,
			ArrayList<? extends Object> entries) throws ActionException {
		String sql = StringUtil.join("select lower(cust_product_id) as key from ", getCustomSchema(), 
				"wsla_product_master where provider_id=?");

		// load this product's SKUs from the DB and store them as a Set for quick reference.
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<GenericVO> skus = db.executeSelect(sql, Arrays.asList(req.getParameter("providerId")), new GenericVO());
		Set<String> products = new HashSet<>(skus.size());
		for (GenericVO vo : skus)
			products.add(StringUtil.checkVal(vo.getKey()));

		//remove items from the batch import which are already in the database
		Iterator<? extends Object> iter = entries.iterator();
		while (iter.hasNext()) {
			ProductVO vo = (ProductVO) iter.next();
			if (!StringUtil.isEmpty(vo.getCustomerProductId()) && products.contains(vo.getCustomerProductId().toLowerCase())) {
				iter.remove();
				log.debug("omitting pre-existing product: " + vo.getCustomerProductId());
			}
		}
	}


	/*
	 * set additional values into the VOs from request params (oem, category, etc.)
	 * (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#transposeBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void transposeBatchImport(ActionRequest req,
			ArrayList<? extends Object> entries) throws ActionException {
		//set the providerId for all beans to the one passed on the request
		String providerId = req.getParameter("providerId");
		Date dt = Calendar.getInstance().getTime();
		for (Object obj : entries) {
			ProductVO vo = (ProductVO) obj;
			vo.setProviderId(providerId);
			vo.setActiveFlag(1);
			vo.setCreateDate(dt);
		}
	}


	/*
	 * save the products, then save their category_xr's
	 * (non-Javadoc)
	 * @see com.wsla.action.admin.BatchImport#saveBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void saveBatchImport(ActionRequest req,
			ArrayList<? extends Object> entries) throws ActionException {
		//save the product_master table
		super.saveBatchImport(req, entries);

		//save the product_category_xr table
		if (req.hasParameter("categoryId"))
			addProductCategories(req, entries, req.getParameter("categoryId"));

		//possibly save the 2nd category
		if (req.hasParameter("categoryId2"))
			addProductCategories(req, entries, req.getParameter("categoryId2"));
	}


	/**
	 * Creates a product_category record for each of the products saved
	 * @param req
	 * @param entries
	 * @throws ActionException 
	 */
	private void addProductCategories(ActionRequest req, ArrayList<? extends Object> entries, 
			String categoryId) throws ActionException {
		ArrayList<ProductCategoryAssociationVO> data = new ArrayList<>(entries.size());
		for (Object obj : entries) {
			ProductVO vo = (ProductVO) obj;
			data.add(new ProductCategoryAssociationVO(vo.getProductId(), categoryId));
		}
		//push these through the same batch-insert logic
		super.saveBatchImport(req, data);
	}
}