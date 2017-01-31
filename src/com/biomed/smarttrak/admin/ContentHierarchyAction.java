package com.biomed.smarttrak.admin;

import java.util.ArrayList;
import java.util.List;

import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ContentHierarchyAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action that manages Content Hierarchies.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 6, 2017
 ****************************************************************************/
public class ContentHierarchyAction extends AbstractTreeAction {

	public static final String CONTENT_HIERARCHY_CACHE_KEY = "BIOMED_CONTENT_HIERARCHY";

	/**
	 * @param init
	 */
	public ContentHierarchyAction(ActionInitVO init) {super(init);}
	public ContentHierarchyAction() {super();}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#copy(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void copy(SMTServletRequest req) throws ActionException {
		throw new ActionException("Method not supported.");
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(SMTServletRequest req) throws ActionException {
		SectionVO s = new SectionVO(req);

		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));

		try {
			dbp.delete(s);
		} catch (InvalidDataException | DatabaseException e) {
			log.error(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		SectionVO s = new SectionVO(req);

		String actionPerform = req.getParameter("actionPerform");

		updateSectionVO(actionPerform, s);

		this.clearCacheByKey(CONTENT_HIERARCHY_CACHE_KEY);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		String sectionId = req.getParameter("sectionId");

		Tree t;

		//Attempt to read ContentHierarchy Data from Cache.
		ModuleVO mod = super.readFromCache(getCacheKey());

		//If not found in cache Load data.
		if(mod == null) {
			t = loadTree(null);
		} else {
			//Get the Tree off the actionData
			t = (Tree) mod.getActionData();
		}

		t.calculateTotalChildren(t.getRootNode());

		//Place requested data on the request.
		if(!StringUtil.isEmpty(sectionId)) {
			//Put the requested Section Node on the request.
			Node n = t.findNode(sectionId);
			List<Node> sections = new ArrayList<>();
			sections.add(n);
			this.putModuleData(sections);
		} else {
			List<Node> sections = t.preorderList();
			this.putModuleData(sections, sections.size(), false);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		if(!StringUtil.isEmpty(req.getParameter(SBActionAdapter.SB_ACTION_ID))) {
			super.retrieve(req);
		} else {
			super.list(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);

		 // Redirect after the update
        sbUtil.adminRedirect(req, attributes.get(Constants.ACTION_SUCCESS_KEY), (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}

	/**
	 * Helper method that inserts/updates a SectionVO.
	 * @param s
	 */
	private void updateSectionVO(String actionPerform, SectionVO s) {
		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));

		try {
			if(!StringUtil.isEmpty(actionPerform) && "delete".equals(actionPerform)) {
				dbp.delete(s);
			} else {
				dbp.save(s);
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.AbstractTreeAction#getCacheKey()
	 */
	@Override
	public String getCacheKey() {
		return CONTENT_HIERARCHY_CACHE_KEY;
	}
}