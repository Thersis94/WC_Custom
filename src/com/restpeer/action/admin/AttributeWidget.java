package com.restpeer.action.admin;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.List;

// RP Libs
import com.restpeer.data.AttributeVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: AttributeWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the attributes / products being sold
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 13, 2019
 * @updates:
 ****************************************************************************/

public class AttributeWidget extends SBActionAdapter {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "attribute";
	
	/**
	 * 
	 */
	public AttributeWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public AttributeWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		putModuleData(getAttributeValues(req.getParameter("parentCode")));
	}
	
	/**
	 * 
	 * @param parentCode
	 * @return
	 */
	public List<AttributeVO> getAttributeValues(String parentCode) {
		StringBuilder sql = new StringBuilder(164);
		sql.append("select * from ").append(getCustomSchema()).append("rp_attribute a ");
		sql.append("left outer join ").append(getCustomSchema()).append("rp_category b ");
		sql.append("on a.category_cd = b.category_cd ");
		sql.append("where 1=1 ");
		
		List<Object> vals = new ArrayList<>();
		if (!StringUtil.isEmpty(parentCode)) {
			sql.append("and parent_cd = ? ");
			vals.add(parentCode);
		} else {
			sql.append("and parent_cd is null ");
		}
		
		sql.append("order by order_no, a.category_cd, attribute_nm");
		log.debug(sql.length() + "|" + sql);
		
		// Execute and return the data
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new AttributeVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		AttributeVO attr = new AttributeVO(req);
		if (StringUtil.checkVal(req.getParameter("parentCode")).isEmpty())
			attr.setParentCode(null);
		
		try {
			if (req.getBooleanParameter("delete")) deleteAttribute(attr);
			else saveAttribute(attr, req.getBooleanParameter("isInsert"));
			
			setModuleData(attr);
		} catch (Exception e) {
			log.error("Unable to save attr: " + attr, e);
			setModuleData(attr, 0, e.getLocalizedMessage());
		}
	}
	
	/**
	 * Removes an attribute item
	 * @param attr
	 * @throws DatabaseException
	 */
	public void deleteAttribute(AttributeVO attr) throws DatabaseException {
		
		try {
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			db.delete(attr);
		} catch (Exception e) {
			throw new DatabaseException("inable to delete attribute: " + attr, e);
		}
	}
	
	/**
	 * Saves the attribute data
	 * @param attr
	 * @param isInsert
	 * @throws DatabaseException
	 */
	public void saveAttribute(AttributeVO attr, boolean isInsert) throws DatabaseException {
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (isInsert) db.insert(attr);
			else db.update(attr);
		
		} catch (Exception e) {
			throw new DatabaseException("Unable to save attribute: " + attr, e);
		}
		
	}
}

