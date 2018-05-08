package com.rezdox.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;


/****************************************************************************
 * <b>Title</b>: BusinessCategoryList.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> list that looks up business categories and sub categories
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 6, 2018
 * @updates:
 ****************************************************************************/
public class BusinessCategoryList extends SimpleActionAdapter {

	public BusinessCategoryList() {
		super();
	}

	public BusinessCategoryList(ActionInitVO arg0) {
		super(arg0);
	}
	
	/**
	 * @param dbConnection
	 * @param attributes
	 */
	public BusinessCategoryList(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<GenericVO> data = retrieveCategories(req.getParameter("businessCat"));
		putModuleData(data, data.size(),false);
	}
	
	/**
	 * Reusable retrieve to get the list outside of a Request
	 * 
	 * @param parentCode
	 * @return
	 */
	public List<GenericVO> retrieveCategories(String parentCode) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select business_category_cd as key, category_nm as value from ");
		sql.append(schema).append("rezdox_business_category ");
		
		if(!StringUtil.isEmpty(parentCode)) {
			sql.append("where parent_cd = ? ");
			sql.append("order by category_nm ");
			params.add(parentCode);
		} else {
			sql.append("where parent_cd is null ");
			sql.append("order by order_no ");
		}
		
		
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		log.debug("sql " + sql.toString() + " params " + params + " size " + data.size());
		return data;
	}
}
