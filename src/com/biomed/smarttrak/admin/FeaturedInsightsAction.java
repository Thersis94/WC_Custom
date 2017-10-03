package com.biomed.smarttrak.admin;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import com.biomed.smarttrak.action.FeaturedInsightAction;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.util.BiomedInsightIndexer;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
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
	private void loadFromDb(ActionRequest req) throws ActionException {
		ActionInterface ai = new InsightAction();
		req.setParameter("featuredFlg", "1");
		ai.setActionInit(actionInit);
		ai.setAttributes(attributes);
		ai.setDBConnection(dbConn);
		ai.retrieve(req);
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
		
		if (userRoles.isEmpty()) {
			fia.simulatedFeaturedRequest(req, userRoles);
		} else {
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
			ps.setString(1, mod.getActionUrl());
			
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
		if (!req.hasParameter("sectionTxt")) return Collections.emptySet();
		
		Set<String> solrPermissions = new HashSet<>();
		for (String solrTxt : req.getParameterValues("sectionTxt")) {
			solrPermissions.add(solrTxt);
		}
		solrPermissions.add(SmarttrakRoleVO.PUBLIC_ACL);
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
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			String[] order = req.getParameterValues("orderNo");
			String[] ids = req.getParameterValues(INSIGHT_ID);
			for (int i=0; i < order.length || i < ids.length; i++) {
				ps.setInt(1, Convert.formatInteger(order[i]));
				ps.setString(2, ids[i]);
				ps.addBatch();
				log.debug("Setting " + ids[i] + " to " + order[i]);
			}

			ps.executeBatch();
		} catch (SQLException e) {
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
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, Convert.formatInteger(req.getParameter("featuredFlg")));
			ps.setInt(2, Convert.formatInteger(req.getParameter("sliderFlg")));
			ps.setInt(3, Convert.formatInteger(req.getParameter("sectionFlg")));
			ps.setString(4, req.getParameter(INSIGHT_ID));
			
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}

		updateSolr(req.getParameterValues(INSIGHT_ID));
	}
}
