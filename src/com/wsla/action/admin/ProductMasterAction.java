package com.wsla.action.admin;

import java.sql.PreparedStatement;
import java.sql.SQLException;
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
import com.smt.sitebuilder.action.BatchImport;

// WSLA Libs
import com.wsla.data.product.ProductCategoryAssociationVO;
import static com.wsla.action.admin.ProviderAction.REQ_PROVIDER_ID;
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
		String providerId = req.getParameter(REQ_PROVIDER_ID);
		Integer validatedFlag = req.getIntegerParameter("validatedFlag");
		Integer setFlag = req.hasParameter("setFlag") ? Convert.formatInteger(req.getParameter("setFlag")) : null;
		BSTableControlVO bst = new BSTableControlVO(req, ProductVO.class);
		setModuleData(getProducts(productId, providerId, setFlag, validatedFlag, bst));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductVO product = new ProductVO(req);
		try {
			if (req.getBooleanParameter("validate")) {
				validateProduct(new ProductVO(req), req.getParameter("newProductId"));
			} else {
				
				DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
				db.save(product);
				putModuleData(product);
			}
		} catch (InvalidDataException | DatabaseException | SQLException e) {
			log.error("Unable to save product", e);
			putModuleData(product, 0, false, e.getLocalizedMessage(), true);
		}
	}

	/**
	 * 
	 * @param product
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void validateProduct(ProductVO product, String newProductId) 
			throws InvalidDataException, DatabaseException, SQLException {
		
		// Update the validated flag to 1
		if (product.getValidatedFlag() == 1) {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.update(product, Arrays.asList("validated_flg", "product_id"));
		} else {
			
			// Switch any products in the product serial table to the new product
			StringBuilder sql = new StringBuilder(128);
			sql.append("update ").append(getCustomSchema());
			sql.append("wsla_product_serial set product_id = ? where product_id = ?");
			
			try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
				ps.setString(1, newProductId);
				ps.setString(2, product.getProductId());
				
				ps.executeUpdate();
			}
			
			// delete the product
			DBProcessor db  = new DBProcessor(getDBConnection(), getCustomSchema());
			db.delete(product);
		}
	}

	/**
	 * Gets a list of products - either by ID or for the given provider.
	 * @param productId
	 * @param providerId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProductVO> getProducts(String productId, String providerId, 
			Integer setFlag, Integer validatedFlag, BSTableControlVO bst) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select pm.*, p.provider_nm from ").append(schema).append("wsla_product_master pm ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_provider p on pm.provider_id=p.provider_id ");
		sql.append("where 1=1 ");
		List<Object> params = new ArrayList<>();

		// Filter by search criteria
		String term = bst.getLikeSearch().toLowerCase();
		if (!StringUtil.isEmpty(term)) {
			sql.append("and (lower(pm.product_nm) like ? or lower(pm.cust_product_id) like ? ");
			sql.append("or lower(pm.sec_cust_product_id) like ? or lower(p.provider_nm) like ?) ");
			params.add(term);
			params.add(term);
			params.add(term);
			params.add(term);
		}

		if (setFlag != null) {
			sql.append("and pm.set_flg=? ");
			params.add(setFlag);
		}

		if (validatedFlag != null) {
			sql.append("and pm.validated_flg=? ");
			params.add(validatedFlag);
		}

		// Filter by providerId
		if (!StringUtil.isEmpty(providerId)) {
			sql.append("and pm.provider_id=? ");
			params.add(providerId);
		}

		// Filter by productId
		if (!StringUtil.isEmpty(productId)) {
			sql.append("and pm.product_id=? ");
			params.add(productId);
		}

		sql.append(bst.getSQLOrderBy("pm.product_nm",  "asc"));
		log.info(sql + "|" + params);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ProductVO(), bst.getLimit(), bst.getOffset());
	}


	/**
	 * Generate a list of Parts (<ProductId, ProductNm> pairs) bound to the given provider (likely an OEM)
	 * @param providerId
	 * @return List<GenericVO> used to populate a selectpicker dropdown in the UI.  See SelectLookupAction reference.
	 */
	public List<GenericVO> listProducts(String providerId, Integer activeFlg, Integer setFlg) {
		List<Object> params = new ArrayList<>();
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select product_id as key, product_nm as value from ").append(schema);
		sql.append("wsla_product_master where 1=1 ");

		if (activeFlg != null) {
			sql.append("and active_flg=? ");
			params.add(activeFlg);
		}

		if (setFlg != null) {
			sql.append("and set_flg=? ");
			params.add(setFlg);
		}

		if (!StringUtil.isEmpty(providerId)) {
			sql.append("and provider_id=? ");
			params.add(providerId);
		}

		sql.append(" order by product_nm");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new GenericVO());
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
	 * @see com.smt.sitebuilder.action.BatchImport#validateBatchImport(com.siliconmtn.action.ActionRequest, java.util.ArrayList)
	 */
	@Override
	protected void validateBatchImport(ActionRequest req,
			ArrayList<? extends Object> entries) throws ActionException {
		String sql = StringUtil.join("select lower(cust_product_id) as key from ", getCustomSchema(), 
				"wsla_product_master where provider_id=?");

		// load this product's SKUs from the DB and store them as a Set for quick reference.
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<GenericVO> skus = db.executeSelect(sql, Arrays.asList(req.getParameter(REQ_PROVIDER_ID)), new GenericVO());
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
		String providerId = req.getParameter(REQ_PROVIDER_ID);
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