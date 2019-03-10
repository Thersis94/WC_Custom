package com.perfectstorm.action.admin;

// JDK 1.8.x
import java.util.List;

// PS Libs
import com.perfectstorm.data.AttributeVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: AttributeAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action to manage the attributes
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 7, 2019
 * @updates:
 ****************************************************************************/

public class AttributeAction extends SBActionAdapter {
	/**
	 * Key to access the widget through the controller
	 */
	public static final String AJAX_KEY = "attr";
	
	/**
	 * 
	 */
	public AttributeAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public AttributeAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getVenueAttributes());
	}
	
	/**
	 * Gets the attributes
	 * @return
	 */
	public List<AttributeVO> getVenueAttributes() {
		
		StringBuilder sql = new StringBuilder(64);
		sql.append("select * from ").append(getCustomSchema()).append("ps_attribute ");
		sql.append("order by attribute_nm");
		log.debug(sql.length() + "|" + sql);
		
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
			log.info("saving Attr: " + attr);
			if (req.getBooleanParameter("isInsert"))
				db.insert(attr);
			else
				db.update(attr);
			
			setModuleData(attr);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Unable to save attribute: " + attr, e);
			setModuleData(attr, 0, e.getLocalizedMessage());
		}
	}
}

