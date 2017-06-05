package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.solr.AccessControlQuery;
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
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 6, 2017
 ****************************************************************************/
public class SectionHierarchyAction extends AbstractTreeAction {

	public static final String CONTENT_HIERARCHY_CACHE_KEY = "BIOMED_CONTENT_HIERARCHY";

	/**
	 * @param init
	 */
	public SectionHierarchyAction(ActionInitVO init) {super(init);}
	public SectionHierarchyAction() {super();}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#copy(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void copy(ActionRequest req) throws ActionException {
		throw new ActionException("Method not supported.");
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		SectionVO s = new SectionVO(req);

		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));

		try {
			dbp.delete(s);

			this.clearCacheByKey(CONTENT_HIERARCHY_CACHE_KEY);

		} catch (InvalidDataException | DatabaseException e) {
			log.error(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		SectionVO s = new SectionVO(req);

		String actionPerform = req.getParameter("actionPerform");

		updateSectionVO(actionPerform, s);

		this.clearCacheByKey(CONTENT_HIERARCHY_CACHE_KEY);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String sectionId = req.getParameter("sectionId");

		Tree t;

		//Attempt to read ContentHierarchy Data from Cache.
		ModuleVO mod = readFromCache(CONTENT_HIERARCHY_CACHE_KEY);

		//If not found in cache Load data.
		if(mod == null) {
			t = loadTree(null);
			
			// Because the tree is cached, these only need to run once
			// These will maintain their values in the cache
			t.calculateTotalChildren(t.getRootNode());
			t.buildNodePaths();
			
			super.writeToCache(t, "SMARTTRAK", "SECTION");
		} else {
			//Get the Tree off the actionData
			t = (Tree) mod.getActionData();
		}

		//Place requested data on the request.
		if(!StringUtil.isEmpty(sectionId)) {
			//Put the requested Section Node on the request.
			Node n = t.findNode(sectionId);
			List<Node> sections = new ArrayList<>();
			sections.add(n);

			SmarttrakRoleVO role = (SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
			sections = checkPermissions(sections, role);
			
			this.putModuleData(sections);
		} else {
			List<Node> sections = t.preorderList();
			this.putModuleData(sections, sections.size(), false);
		}
	}

	
	/**
	 * Loop over the sections and make sure that the user has permission to access them
	 * @param sections
	 * @param role
	 */
	protected List<Node> checkPermissions(List<Node> sections, SmarttrakRoleVO role) {
		if (sections == null || sections.isEmpty()) return null;
		
		String[] roleAcl = role.getAuthorizedSections();
		List<Node> allowed = new ArrayList<>();
		for (Node n : sections) {
			SectionVO sec = (SectionVO) n.getUserObject();
			if (roleAcl == null || roleAcl.length == 0 || !AccessControlQuery.isAllowed("+g:" + sec.getSolrTokenTxt(), null, roleAcl)) {
				// Do nothing. This section cannot be seen by the current user
				// and there is nothing left for the loop to do
			} else {
				n.setChildren(checkPermissions(n.getChildren(), role));
				allowed.add(n);
			}
		}
		return allowed;
	}
	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		if(!StringUtil.isEmpty(req.getParameter(SBActionAdapter.SB_ACTION_ID))) {
			super.retrieve(req);
		} else {
			super.list(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
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
			log.error("Couldn't update the Section", e);
		}
	}
	
	/**
	 * For a given section in the hierarchy, updates the fd_pub_yr and fd_pub_qtr values only
	 * 
	 * @param sectionId
	 * @param year
	 * @param qtr
	 */
	public void updateFdPublish(String sectionId, int fdPubYr, int fdPubQtr) {
		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		
		// Setup the requirements for the db processor
		String sql = getFdPublishSql();
		List<String> fields = new ArrayList<>();
		fields.addAll(Arrays.asList("FD_PUB_YR", "FD_PUB_QTR", "SECTION_ID"));
		
		// Add the values to the vo
		SectionVO section = new SectionVO();
		section.setSectionId(sectionId);
		section.setFdPubQtr(fdPubQtr);
		section.setFdPubYr(fdPubYr);
		
		// Update the values
		try {
			dbp.executeSqlUpdate(sql, section, fields);
		} catch(Exception e) {
			log.error(e);
		}
		
		// Clear the cache so it gets refreshed with current values on the next request
		this.clearCacheByKey(getCacheKey());
	}
	
	/**
	 * Returns the sql for updating the fd publish values
	 * 
	 * @return
	 */
	private String getFdPublishSql() {
		String custom = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		
		sql.append("update ").append(custom).append("BIOMEDGPS_SECTION ");
		sql.append("set ").append("FD_PUB_YR = ?, FD_PUB_QTR = ? ");
		sql.append("where SECTION_ID = ? ");
		
		return sql.toString();
	}
	
	/**
	 * Gets the year and quarter of the most recently published section.
	 * 
	 * @return
	 */
	public SectionVO getLatestFdPublish() {
		SectionVO data = new SectionVO();
		String custom = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		StringBuilder sql = new StringBuilder(250);
		sql.append("select fd_pub_yr, max(fd_pub_qtr) as fd_pub_qtr ");
		sql.append("from ").append(custom).append("biomedgps_section ");
		sql.append("where fd_pub_yr = (select max(fd_pub_yr) from ").append(custom).append("biomedgps_section) ");
		sql.append("group by fd_pub_yr ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				data.setFdPubQtr(rs.getInt("fd_pub_qtr"));
				data.setFdPubYr(rs.getInt("fd_pub_yr"));
			}
		} catch(SQLException sqle) {
			log.error("Unable to get latest FD publish year & quarter", sqle);
		}
		
		return data;
	}

	/* (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.AbstractTreeAction#getCacheKey()
	 */
	@Override
	public String getCacheKey() {
		return CONTENT_HIERARCHY_CACHE_KEY;
	}
}