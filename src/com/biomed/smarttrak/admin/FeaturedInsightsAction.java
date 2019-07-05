package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.biomed.smarttrak.action.FeaturedInsightAction;
import com.biomed.smarttrak.util.BiomedInsightIndexer;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.InsightVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: FeaturedInsightsAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Creates an instance of the public featured insights action
 * from information set on the AdminControllerAction in order to return a preview
 * of what the homepage of the public side looks like.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @since Aug 9, 2017<p/>
 * @updates:
 ****************************************************************************/

public class FeaturedInsightsAction extends SBActionAdapter {

	private static final String INSIGHT_ID = "insightId";
	private static final String SECTION_TXT = "sectionTxt";
	private static final String BIOMED_FEATURED_WIDGET_ID = "c77490469d64f478c0a80237b68e7be3";

	public FeaturedInsightsAction() {
		super();
	}

	public FeaturedInsightsAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("loadData"))) {
			loadFromDb(req);
		} else {
			loadFromSolr(req);
		}
	}
	
	/**
	 * Load the documents form the database.
	 * @param req
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private void loadFromDb(ActionRequest req) throws ActionException {
		InsightAction ai = new InsightAction();
		req.setParameter("featuredFlg", "1");
		req.setParameter("sort", "Published");
		req.setParameter("order", "desc");
		req.setParameter("sectionBypass", "true");
		req.setParameter("retrieveAll", "true");
		ai.setActionInit(actionInit);
		ai.setAttributes(attributes);
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
		
		List<Object> list = (List<Object>) ((ModuleVO)attributes.get(Constants.MODULE_DATA)).getActionData();
		List<InsightVO> approvedDocs = new ArrayList<>(list.size());
		String[] userRoles = req.getParameter(SECTION_TXT).split(",");

		//Load the Section Tree and set all the Hierarchies.
		SmarttrakTree t = ai.loadDefaultTree();
		for(Object o : list) {
			InsightVO i = (InsightVO)o;
			i.configureSolrHierarchies(t);
			for (String perm : userRoles) {
				if (i.getACLPermissions().contains(perm)) {
					approvedDocs.add(i);
					break;
				}
			}
		}
		
		putModuleData(approvedDocs, approvedDocs.size(), false);
	}
	
	/**
	 * Load the documents from solr to simulare what the user will see.
	 * @param req
	 * @throws ActionException
	 */
	private void loadFromSolr(ActionRequest req) throws ActionException {
		Set<String> userRoles = buildSimulatedRole(req);
		
		// Pass along the id of the solr search widget used for featured insights
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		setFeaturedData(mod);
		
		FeaturedInsightAction fia = new FeaturedInsightAction();
		fia.setActionInit(actionInit);
		fia.setAttributes(attributes);
		fia.setDBConnection(dbConn);
		
		if (!userRoles.isEmpty()) {
			fia.simulatedFeaturedRequest(req, userRoles);
		} else {
			mod.setAttribute(ModuleVO.ATTRIBUTE_1, mod.getActionUrl());
			fia.retrieve(req);
		}
	}
	
	
	/**
	 * Get the action data for the featured insights
	 * @param mod
	 * @throws ActionException
	 */
	private void setFeaturedData(ModuleVO mod) throws ActionException {
		String sql = "select attrib1_txt, attrib2_txt from sb_action where action_id = ?";
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, BIOMED_FEATURED_WIDGET_ID);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				mod.setAttribute(ModuleVO.ATTRIBUTE_1, rs.getString("attrib1_txt"));
				mod.setAttribute(ModuleVO.ATTRIBUTE_2, rs.getString("attrib2_txt"));
			}
			
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Build a simulated solr acl set from the request object.
	 * @param req
	 * @return
	 */
	private Set<String> buildSimulatedRole(ActionRequest req) {
		if (!req.hasParameter(SECTION_TXT)) return Collections.emptySet();
		
		Set<String> solrPermissions = new HashSet<>();
		for (String solrTxt : req.getParameterValues(SECTION_TXT)) {
			solrPermissions.add(solrTxt);
		}
		return solrPermissions;
	}

	/**
	 * determine what needs to be updated.
	 */
	public void build(ActionRequest req) throws ActionException {
		if ("orderUpdate".equals(req.getParameter("buildAction"))) {
			updateOrder(req);
		} else {
			updateFlags(req);
		}
	}
	
	/**
	 * Update the order numbers of each item in the list.
	 * @param req
	 * @throws ActionException
	 */
	protected void updateOrder(ActionRequest req) throws ActionException {
		String customDbSchema = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append("UPDATE ").append(customDbSchema);
		sql.append("BIOMEDGPS_INSIGHT SET ORDER_NO = ? WHERE INSIGHT_ID = ? ");
		DBProcessor db = new DBProcessor(dbConn);
		Map<String, List<Object>> psValues = new HashMap<>();

		String[] ids = req.getParameterValues(INSIGHT_ID);
		for (int i=0; i < ids.length; i++) {
			List<Object> values = new ArrayList<>();
			values.add(i+1);
			values.add(ids[i]);
			psValues.put(ids[i], values);
		}
		
		try {
			db.executeBatch(sql.toString(), psValues);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		
		updateSolr(req.getParameterValues(INSIGHT_ID));
	}
	
	/**
	 * Reindex the supplied documents.
	 * @param ids
	 */
	private void updateSolr(String[] ids) {

		Properties props = new Properties();
		props.putAll(getAttributes());
		BiomedInsightIndexer indexer = new BiomedInsightIndexer(props);
		indexer.setDBConnection(dbConn);
		
		indexer.indexItems(ids);
	}

	/**
	 * Update the flags for this document
	 * @param req
	 * @throws ActionException
	 */
	protected void updateFlags(ActionRequest req) throws ActionException {
		StringBuilder sql = new StringBuilder(200);
		
		sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("BIOMEDGPS_INSIGHT ");
		sql.append("SET FEATURED_FLG = ?, SLIDER_FLG = ?, SECTION_FLG = ? WHERE INSIGHT_ID = ? ");

		DBProcessor db = new DBProcessor(dbConn);
		List<String> params = new ArrayList<>();
		params.add("featured_flg");
		params.add("slider_flg");
		params.add("section_flg");
		params.add("insight_id");
		
		try {
			db.executeSqlUpdate(sql.toString(), new InsightVO(req), params);
		} catch (Exception e) {
			throw new ActionException(e);
		}

		updateSolr(req.getParameterValues(INSIGHT_ID));
	}
}
