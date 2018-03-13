package com.rezdox.data;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
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

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select business_category_cd as key, category_nm as value from ");
		sql.append(schema).append("rezdox_business_category ");
		
		if(!StringUtil.isEmpty(req.getParameter("businessCat"))) {
			sql.append("where parent_cd = ? ");
			params.add(req.getParameter("businessCat"));
			log.debug("Business Cat: " + req.getParameter("businessCat"));
		}
		
		sql.append("order by business_category_cd ");
		
		
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = db.executeSelect(sql.toString(), params, new GenericVO());
		log.debug("sql " + sql.toString() + " params " + params + " size " + data.size());
		putModuleData(data, data.size(),false);
	}
}
