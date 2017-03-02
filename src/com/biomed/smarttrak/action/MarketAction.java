package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.solr.common.SolrDocument;

import com.biomed.smarttrak.vo.MarketAttributeVO;
import com.biomed.smarttrak.vo.MarketVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: MarketAction.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Return either a list of markets by search terms
 * or details on a particular market.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Feb 15, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class MarketAction extends SBActionAdapter {
	private static final String DEFAULT_GROUP = "Other";

	public MarketAction() {
		super();
	}

	public MarketAction(ActionInitVO init) {
		super(init);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (req.hasParameter("reqParam_1")) {
			retrieveMarket(req.getParameter("reqParam_1"), req);
		} else {
			retrieveMarkets(req);
		}
	}


	/**
	 * Get the indicated market
	 * @param marketId
	 * @throws ActionException
	 */
	protected void retrieveMarket(String marketId, ActionRequest req) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		MarketVO market = new MarketVO();
		market.setMarketId(marketId);
		try {
			db.getByPrimaryKey(market);
			addAttributes(market);
			addSections(market);
		} catch (Exception e) {
			throw new ActionException(e);
		}

		//TODO turn this on once complete and ready for testing
		//verify user has access to this market
		//SecurityController.getInstance(req).isUserAuthorized(market, req);

		putModuleData(market);
	}


	/**
	 * Build the time since last updated message
	 * @param doc
	 */
	//TODO replace this method with javascript running at the browser; talk to Billy (libraries are already loaded and running at page-load)
	private void buildUpdateMsg(SolrDocument market) {
		// Unpublished markets can be skipped
		if (!"P".equals(market.get(SearchDocumentHandler.CONTENT_TYPE))) {
			return;
		}
		Date d  = (Date)market.getFieldValue(SearchDocumentHandler.UPDATE_DATE);
		
		long diff = Convert.getCurrentTimestamp().getTime() - d.getTime();
		long diffDays = diff / (1000 * 60 * 60 * 24);
		long diffHours = diff / (1000 * 60 * 60);
		if (diffDays > 365) {
			int years = (int) (diffDays/365);
			market.setField("updateMsg", years + " year(s) ago");
		} else if (diffDays > 30) {
			int months = (int) (diffDays/30);
			market.setField("updateMsg", months + " month(s) ago");
		} else if (diffDays > 7) {
			int weeks = (int) (diffDays/7);
			market.setField("updateMsg", weeks + " week(s) ago");
		} else if (diffDays > 0) {
			market.setField("updateMsg", diffDays + " day(s) ago");
		} else if (diffHours > 0) {
			market.setField("updateMsg", diffHours + " hour(s) ago");
		} else {
			market.setField("updateMsg", "less than an hour ago");
		}
	}

	/**
	 * Get all attributes for the supplied market
	 * @param market
	 */
	protected void addAttributes(MarketVO market) {
		StringBuilder sql = new StringBuilder(150);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE MARKET_ID = ? ");
		sql.append("ORDER BY xr.ORDER_NO");
		log.debug(sql+"|"+market.getMarketId());

		List<Object> params = new ArrayList<>();
		params.add(market.getMarketId());
		DBProcessor db = new DBProcessor(dbConn);

		List<Object> results = db.executeSelect(sql.toString(), params, new MarketAttributeVO());
		for (Object o : results)
			market.addMarketAttribute((MarketAttributeVO)o);
	}


	/**
	 * Get all sections for supplied market
	 * @param market
	 * @throws ActionException
	 */
	protected void addSections(MarketVO market) throws ActionException {
		StringBuilder sql = new StringBuilder(350);
		String customDb = (String) attributes.get(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_SECTION ms ");
		sql.append("ON m.MARKET_ID = ms.MARKET_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ");
		sql.append("ON s.SECTION_ID = ms.SECTION_ID ");
		sql.append("WHERE m.MARKET_ID = ? ");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, market.getMarketId());
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				market.addSection(new GenericVO(rs.getString("MARKET_SECTION_XR_ID"), rs.getString("SECTION_NM")));

		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * Get all markets from the database
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveMarkets(ActionRequest req) throws ActionException {
		// Pass along the proper information for a search to be done.
	    	ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
	    	req.setParameter("pmid", mod.getPageModuleId());
		
	    	// Build the solr action
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);
		

	    	mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
	    	SolrResponseVO res = (SolrResponseVO) mod.getActionData();
	    	orderMarkets(res);
	}


	/**
	 * Sort all returned markets into groups based on thier hierarchies.
	 * @param res
	 */
	protected void orderMarkets(SolrResponseVO res) {
		Map<String, List<SolrDocument>> groups = new TreeMap<>();
		for (SolrDocument doc : res.getResultDocuments()) {
			log.debug(doc.getFieldValue("hierarchy"));
			buildUpdateMsg(doc);
			String[] hierarchy = StringUtil.checkVal(doc.get(SearchDocumentHandler.HIERARCHY)).split(SearchDocumentHandler.HIERARCHY_DELIMITER);
			if (hierarchy.length < 3) {
				addMarket(doc, groups, DEFAULT_GROUP);
				continue;
			}
			
			addMarket(doc, groups, hierarchy[2]);
		}
		super.putModuleData(groups);
	}

	
	/**
	 * Add the current market to the supplied group.
	 * @param doc
	 * @param groups
	 * @param groupName
	 */
	protected void addMarket(SolrDocument doc,
			Map<String, List<SolrDocument>> groups, String groupName) {
		if (!groups.containsKey(groupName)) {
			groups.put(groupName, new ArrayList<SolrDocument>());
		}
		
		groups.get(groupName).add(doc);
	}
}