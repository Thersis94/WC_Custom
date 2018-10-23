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
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.product.ProductCategoryVO;

/****************************************************************************
 * <b>Title</b>: ProductCategoryAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the product_category table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 5, 2018
 * @updates:
 ****************************************************************************/
public class ProductCategoryAction extends SBActionAdapter {

	public static final String PROD_CAT_ID = "productCategoryId";

	public ProductCategoryAction() {
		super();
	}

	public ProductCategoryAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public ProductCategoryAction(Map<String, Object> attrs, SMTDBConnection conn) {
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
		setModuleData(getCategories(req.getParameter("groupCode"),req.getParameter("parentId"), new BSTableControlVO(req, ProductCategoryVO.class)));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductCategoryVO vo = new ProductCategoryVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save product category", e);
		}
	}


	/**
	 * Return a list of categories bound to the given product.
	 * @param setId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProductCategoryVO> getCategories(String groupCode, String parentId, BSTableControlVO bst) {

		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select c.*, p.category_cd as parent_cd from ").append(schema).append("wsla_product_category c ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("wsla_product_category p on c.parent_id=p.product_category_id ");
		sql.append("where 1=1 ");

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and lower(c.category_cd) like ?");
			params.add(bst.getLikeSearch().toLowerCase());
		}
		
		if (!StringUtil.isEmpty(groupCode)) {
			sql.append("and c.group_cd=? ");
			params.add(groupCode);
		}

		if(!StringUtil.isEmpty(parentId)) {
			sql.append("and c.parent_id = ? ");
			params.add(parentId);
		}else {
			sql.append("and c.parent_id is null ");
			
		}

		sql.append(bst.getSQLOrderBy("c.parent_id desc, c.category_cd",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		GridDataVO<ProductCategoryVO> data = db.executeSQLWithCount(sql.toString(), params, new ProductCategoryVO(), bst.getLimit(), bst.getOffset());
	
		log.debug("data size " + data.getRowData().size());
		return data;
	}


	/**
	 * Return a <K,V> list of categories - used for selectpicker dropdowns.  Called from SelectLookupAction
	 * @param req
	 * @return
	 */
	public List<GenericVO> getCategoryList(ActionRequest req) {
		String productCategoryId = req.getStringParameter(PROD_CAT_ID, "");
		List<Object> params = new ArrayList<>();

		boolean allLevels = req.getBooleanParameter("allLevels");
		StringBuilder sql = new StringBuilder(196);
		sql.append("select product_category_id as key, category_cd as value from ");
		sql.append(getCustomSchema()).append("wsla_product_category ");
		sql.append("where 1=1 ");

		if (! productCategoryId.isEmpty()) {
			sql.append("and parent_id = ? ");
			params.add(productCategoryId);
		} else if (! allLevels) {
			sql.append("and parent_id is null ");
		}

		sql.append("order by category_cd ");

		// Execute and return
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), params, new GenericVO());
	}


	/**
	 * Return a <K,V> list of category group codes - used for selectpicker dropdowns.  Called from SelectLookupAction
	 * @param req
	 * @return
	 */
	public List<GenericVO> getGroupList() {
		StringBuilder sql = new StringBuilder(196);
		sql.append("select distinct group_cd as key, group_cd as value from ");
		sql.append(getCustomSchema()).append("wsla_product_category ");
		sql.append("where len(group_cd) > 0 order by group_cd");

		// Execute and return
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), null, new GenericVO());
	}
}