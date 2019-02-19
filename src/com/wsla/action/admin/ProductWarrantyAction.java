package com.wsla.action.admin;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

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
import com.wsla.data.product.ProductWarrantyVO;

/****************************************************************************
 * <b>Title</b>: ProductWarrantyAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the administration of the product_warranty table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James McKain
 * @version 1.0
 * @since Oct 5, 2018
 * @updates:
 ****************************************************************************/
public class ProductWarrantyAction extends SBActionAdapter {

	public ProductWarrantyAction() {
		super();
	}

	public ProductWarrantyAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/**
	 * Overloaded constructor used for calling between actions.
	 * @param attrs
	 * @param conn
	 */
	public ProductWarrantyAction(Map<String, Object> attrs, SMTDBConnection conn) {
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
		setModuleData(getData(req.getParameter("productSerialId"), new BSTableControlVO(req, ProductWarrantyVO.class)));
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ProductWarrantyVO vo = new ProductWarrantyVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save product warranty", e);
		}
	}


	/**
	 * Return a list of products included in the requested set.
	 * @param setId
	 * @param bst
	 * @return
	 */
	public GridDataVO<ProductWarrantyVO> getData(String productSerialId, BSTableControlVO bst) {
		if (StringUtil.isEmpty(productSerialId)) return null;

		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("wsla_product_warranty pw ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_warranty w on pw.warranty_id=w.warranty_id ");
		sql.append("where pw.product_serial_id=? ");
		params.add(productSerialId);

		sql.append(bst.getSQLOrderBy("w.desc_txt",  "asc"));
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSQLWithCount(sql.toString(), params, new ProductWarrantyVO(), bst.getLimit(), bst.getOffset());
	}
	
	/**
	 * Checks to see if the serial number, warranty combo exists
	 * @param productSerialId
	 * @param warrantyId
	 * @return
	 * @throws SQLException 
	 */
	public boolean hasProductWarranty(String productSerialId, String warrantyId) throws SQLException {
		StringBuilder sql = new StringBuilder(80);
		sql.append("select product_serial_id as key, warranty_id as value from ");
		sql.append(getCustomSchema()).append("wsla_product_warranty ");
		sql.append("where product_serial_id = ? and warranty_id = ?");
		
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, productSerialId);
			ps.setString(2, warrantyId);
			
			try(ResultSet rs = ps.executeQuery()) {
				if (rs.next()) return true;
			}
		}
		
		return false;
	}
}