package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import com.biomed.smarttrak.action.AdminControllerAction.Section;
import com.biomed.smarttrak.action.SmarttrakSolrAction;
import com.biomed.smarttrak.security.SmartTRAKRoleModule;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.security.AuthorizationException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.solr.AccessControlQuery;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.DBRoleModule;

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
	public SectionHierarchyAction(ActionInitVO init) {
		super(init);
	}

	public SectionHierarchyAction() {
		super();
	}

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
		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		try {
			dbp.delete(new SectionVO(req));
			super.clearCacheByKey(CONTENT_HIERARCHY_CACHE_KEY);

		} catch (InvalidDataException | DatabaseException e) {
			log.error(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		updateSectionVO(req.getParameter("actionPerform"), new SectionVO(req));
		super.clearCacheByKey(CONTENT_HIERARCHY_CACHE_KEY);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		List<Node> sections = new ArrayList<>();
		String sectionId = req.getParameter("sectionId");
		Tree t = loadTree(null);
		Tree.calculateTotalChildren(t.getRootNode());
		t.buildNodePaths(SearchDocumentHandler.HIERARCHY_DELIMITER, Convert.formatBoolean(req.getParameter("useNames")));

		//Place requested data on the request.
		if (!StringUtil.isEmpty(sectionId)) {
			//Put the requested Section Node on the request.
			Node n = t.findNode(sectionId);
			sections.add(n);

			// Restrict the sections based on user id if not on the manage tool or if requested by the call
			if (req.hasParameter("amid") && (!"smarttrakAdmin".equals(req.getParameter("amid")) || req.hasParameter("overrideUser"))) {
				sections = loadSectionsFromId(req, sections);
				log.debug("secs=" + sections.size());
			}
		} else {
			sections = t.preorderList();
		}
		putModuleData(sections, sections.size(), false);
	}


	/**
	 * Load the viewed user's acls
	 * @param req
	 * @param sections
	 * @return
	 * @throws ActionException
	 */
	private List<Node> loadSectionsFromId(ActionRequest req, List<Node> sections) throws ActionException {
		SmarttrakRoleVO role;
		if (req.hasParameter("overrideUser")) {
			role = loadUser(req.getParameter("overrideUser"), req);
		} else {
			role = (SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		}

		// Attempt to limit sections by the user's permissions
		Section tool;
		if (req.hasParameter("overrideTool") && "smarttrakAdmin".equals(req.getParameter("amid"))) {
			tool = Section.valueOf(req.getParameter("overrideTool"));
		} else {
			tool = SmarttrakSolrAction.determineSection(req);
		}
		
		return checkPermissions(sections, role, tool);
	}

	/**
	 * Load the currently viewed user's information
	 * @param profileId
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private SmarttrakRoleVO loadUser(String profileId, ActionRequest req) throws ActionException {
		UserVO user = new UserVO(req);
		user.setProfileId(profileId);
		Calendar c = Calendar.getInstance(); 
		c.add(Calendar.DATE, 1);
		user.setExpirationDate( c.getTime());
		
		SmartTRAKRoleModule roleModule = new SmartTRAKRoleModule(attributes);
		roleModule.addAttribute(DBRoleModule.DB_CONN, dbConn);
		roleModule.addAttribute(DBRoleModule.HTTP_REQUEST, req);
		roleModule.addAttribute(Constants.USER_DATA, user);
		
		try {
			return (SmarttrakRoleVO) roleModule.getUserRole(profileId, ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteId());
		} catch (AuthorizationException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Loop over the sections and make sure that the user has permission to access them
	 * @param sections
	 * @param role
	 */
	protected List<Node> checkPermissions(List<Node> sections, SmarttrakRoleVO role, Section tool) {
		String[] roleAcl = role != null ? role.getAuthorizedSections(tool) : new String[0];
		if (sections == null || sections.isEmpty() || roleAcl == null || roleAcl.length == 0) 
			return Collections.emptyList();

		List<Node> allowed = new ArrayList<>(sections.size());
		for (Node n : sections) {
			SectionVO sec = (SectionVO) n.getUserObject();
			if (AccessControlQuery.isAllowed("+g:" + sec.getSolrTokenTxt(), null, roleAcl)) {
				n.setChildren(checkPermissions(n.getChildren(), role, tool));
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
		if (req.hasParameter(SBActionAdapter.SB_ACTION_ID)) {
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
		super.clearCacheByKey(CONTENT_HIERARCHY_CACHE_KEY);

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
			if (!StringUtil.isEmpty(actionPerform) && "delete".equals(actionPerform)) {
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
			super.clearCacheByKey(CONTENT_HIERARCHY_CACHE_KEY);
		} catch(Exception e) {
			log.error(e);
		}
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
	
	public SectionVO getLatestFdPublish() {
		return getLatestFdPublish(null);
	}

	/**
	 * Gets the year and quarter of the most recently published section.
	 * 
	 * @return
	 */
	public SectionVO getLatestFdPublish(String sectionId) {
		SectionVO data = new SectionVO();
		String custom = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(250);
		sql.append("select s.fd_pub_yr, greatest(s.fd_pub_qtr, s2.fd_pub_qtr, s3.fd_pub_qtr, s4.fd_pub_qtr) as fd_pub_qtr ");
		sql.append("from ").append(custom).append("biomedgps_section s ");
		sql.append("left join ").append(custom).append("biomedgps_section s2 ");
		sql.append("on s2.parent_id = s.section_id ");
		sql.append("left join ").append(custom).append("biomedgps_section s3 ");
		sql.append("on s3.parent_id = s2.section_id ");
		sql.append("left join ").append(custom).append("biomedgps_section s4 ");
		sql.append("on s4.parent_id = s3.section_id ");
		sql.append("where s.fd_pub_yr = (select max(fd_pub_yr) from ").append(custom).append("biomedgps_section) ");
		if (sectionId != null) sql.append("and s.section_id = ? ");
		sql.append("group by s.fd_pub_yr, s.fd_pub_qtr, s2.fd_pub_qtr, s3.fd_pub_qtr, s4.fd_pub_qtr ");
		sql.append("order by fd_pub_qtr desc ");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			if (sectionId != null) ps.setString(1, sectionId);
			
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