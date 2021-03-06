package com.rezdox.data;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title:</b> ProjectCategoryList.java<br/>
 * <b>Description:</b> Returns a list of project categories from the RezDox custom table.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 26, 2018
 ****************************************************************************/
public class ProjectCategoryList extends SimpleActionAdapter {

	public ProjectCategoryList() {
		super();
	}

	public ProjectCategoryList(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(200);
		sql.append("select project_category_cd as key, category_nm as value from ");
		sql.append(schema).append("REZDOX_PROJECT_CATEGORY order by category_nm ");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		putModuleData(db.executeSelect(sql.toString(), null, new GenericVO()));
	}
}