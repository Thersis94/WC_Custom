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
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.product.ProductCategoryAssociationVO;

/****************************************************************************
 * <b>Title</b>: ProductCategoryXRAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the product_category_xr table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 5, 2018
 * @updates:
 ****************************************************************************/
public class ProductCategoryXRAction extends SBActionAdapter {

	public ProductCategoryXRAction() {
		super();
	}

	public ProductCategoryXRAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public ProductCategoryXRAction(Map<String, Object> attrs, SMTDBConnection conn) {
		this();
		this.setAttributes(attrs);
		this.setDBConnection(conn);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getCategories(req.getParameter("productId"), new BSTableControlVO(req, ProductCategoryAssociationVO.class)));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductCategoryAssociationVO vo = new ProductCategoryAssociationVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save product category XR", e);
		}
	}


	/**
	 * Return a list of categories bound to the given product.
	 * @param setId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProductCategoryAssociationVO> getCategories(String productId, BSTableControlVO bst) {
		if (StringUtil.isEmpty(productId)) return null;

		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select xr.*, c.category_cd from ").append(schema).append("wsla_product_category_xr xr ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_category c on xr.product_category_id=c.product_category_id ");
		sql.append("where 1=1 ");

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and lower(c.category_cd) like ?");
			params.add(bst.getLikeSearch().toLowerCase());
		}

		// always filter by productId
		sql.append("and xr.product_id=? ");
		params.add(productId);

		sql.append(bst.getSQLOrderBy("c.category_cd",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ProductCategoryAssociationVO(), bst.getLimit(), bst.getOffset());
	}
}