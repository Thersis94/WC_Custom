package com.restpeer.action.admin;

import java.util.List;

import com.restpeer.data.CategoryVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: CategoryWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the categories of attributes for products
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 13, 2019
 * @updates:
 ****************************************************************************/

public class CategoryWidget extends SBActionAdapter {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "category";
	
	/**
	 * 
	 */
	public CategoryWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public CategoryWidget(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getCategories());
	}
	
	/**
	 * Get a list of categories
	 * @return
	 */
	public List<CategoryVO> getCategories() {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select * from ").append(getCustomSchema()).append("rp_category ");
		sql.append("order by category_nm");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new CategoryVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		CategoryVO cat = new CategoryVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		
		try {
			if (req.getBooleanParameter("isInsert")) db.insert(cat);
			else db.update(cat);
			
			setModuleData(cat);
		} catch (Exception e) {
			log.error("Unable to add cat:" + cat, e);
			putModuleData(cat, 0, false, e.getLocalizedMessage(), true);
		}
	}
}
