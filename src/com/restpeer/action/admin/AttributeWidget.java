package com.restpeer.action.admin;

// JDK 1.8.x
import java.util.List;
import java.util.Map;

// RP Libs
import com.restpeer.data.AttributeVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: AttributeWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the categories of attributes for locations
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
	
	/**
	 * @param actionInit
	 */
	public AttributeWidget(SMTDBConnection dbConn, Map<String, Object> attributes) {
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
		setModuleData(getAttributeData());
	}
	
	/**
	 * Get a list of categories
	 * @return
	 */
	public List<AttributeVO> getAttributeData() {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema()).append("rp_attribute a ");
		sql.append("order by attribute_nm");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new AttributeVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		AttributeVO attr = new AttributeVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			if (req.getBooleanParameter("isInsert")) db.insert(attr);
			else db.update(attr);
			
			setModuleData(attr);
		} catch (Exception e) {
			log.error("Unable to add attribute:" + attr, e);
			putModuleData(attr, 0, false, e.getLocalizedMessage(), true);
		}
	}
}
