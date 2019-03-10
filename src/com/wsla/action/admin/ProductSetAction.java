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
import com.wsla.data.product.ProductSetVO;

/****************************************************************************
 * <b>Title</b>: ProductSetAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the product_set table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 1, 2018
 * @updates:
 ****************************************************************************/

public class ProductSetAction extends SBActionAdapter {

	public ProductSetAction() {
		super();
	}

	public ProductSetAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public ProductSetAction(Map<String, Object> attrs, SMTDBConnection conn) {
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
		setModuleData(getSet(req.getParameter("setId"), new BSTableControlVO(req, ProductSetVO.class)));
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductSetVO vo = new ProductSetVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save product set", e);
		}
	}


	/**
	 * Return a list of products included in the requested set.
	 * @param setId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProductSetVO> getSet(String setId, BSTableControlVO bst) {
		log.debug(" start get set set id: " + setId);
		if (StringUtil.isEmpty(setId)) return null;
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select s.*, p.product_nm, p.cust_product_id from ").append(schema).append("wsla_product_set s ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_product_master p on s.product_id=p.product_id ");
		sql.append("where 1=1 ");

		// Filter by search criteria
		if (bst.hasSearch()) {
			sql.append("and (p.product_nm like ? or p.cust_product_id like ? or p.sec_cust_product_id like ?) ");
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
			params.add(bst.getLikeSearch());
		}

		// always filter by setId
		sql.append("and s.set_id=? ");
		params.add(setId);

		sql.append(bst.getSQLOrderBy("p.product_nm",  "asc"));
		log.debug("sql "+sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ProductSetVO(), bst.getLimit(), bst.getOffset());
	}
}