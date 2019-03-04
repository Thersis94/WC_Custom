package com.biomed.smarttrak.action;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

//Solr core
import org.apache.solr.common.SolrDocument;

// WC_Custom
import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.security.SecurityController;
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.util.BiomedLinkCheckerUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.vo.MarketAttributeVO;
import com.biomed.smarttrak.vo.MarketVO;
import com.biomed.smarttrak.vo.SectionVO;

// Base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

//WC Core
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SecureSolrDocumentVO.Permission;

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
public class MarketAction extends SimpleActionAdapter {

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
		//call to Solr for a list of markets.  Solr will enforce permissions for us using ACLs
		retrieveFromSolr(req);
		
		if (req.hasParameter(SolrAction.REQ_PARAM_1)) {
			//if the user is not logged in then cannot see market detail pages.
			SmarttrakRoleVO role = (SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
			if (role == null)
				SecurityController.throwAndRedirect(req);

			MarketVO vo = retrieveFromDB(req.getParameter(SolrAction.REQ_PARAM_1), req, true);

			if (StringUtil.isEmpty(vo.getMarketName())){
				PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
				sbUtil.manualRedirect(req,page.getFullPath());
			} else {
				//verify user has access to this market
				SecurityController.getInstance(req).isUserAuthorized(vo, req);
				PageVO page = (PageVO)req.getAttribute(Constants.PAGE_DATA);
				SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
				page.setTitleName(vo.getMarketName() + " | " + site.getSiteName());
				putModuleData(vo);
			}
			putModuleData(vo);

		} else {
			loadPermissionsHierarchy(req);
		}
	}


	/**
	 * Load the full hierarchy so that the full list of second and third
	 * level market sections can be displayed and then mark which sections
	 * the user has permission to view.
	 * @param req
	 */
	private void loadPermissionsHierarchy(ActionRequest req) {
		// load the section hierarchy Tree from the hierarchy action
		SectionHierarchyAction sha = new SectionHierarchyAction();
		sha.setAttributes(getAttributes());
		sha.setDBConnection(getDBConnection());
		Tree hierarchy = sha.loadDefaultTree();
		SmarttrakRoleVO role = (SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		String acls = role.getAccessControlList(SmarttrakSolrAction.BROWSE_SECTION);
		if (acls == null) return;
		for (Node n : hierarchy.preorderList()) {
			SectionVO sec = (SectionVO) n.getUserObject();
			if (acls.contains(sec.getSolrTokenTxt())) {
				sec.setSelected(true);
			} else {
				sec.setSelected(false);
			}
		}
		putModuleData(hierarchy);
	}

	/**
	 * Get the indicated market
	 * @param marketId
	 * @throws ActionException
	 */
	public MarketVO retrieveFromDB(String marketId, ActionRequest req, boolean loadGraphs) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		if (marketId.startsWith("MARKET_"))
			marketId = marketId.replace("MARKET_", "");
		MarketVO market = new MarketVO();
		market.setMarketId(marketId);
		try {
			SmarttrakRoleVO role = (SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
			db.getByPrimaryKey(market);
			addAttributes(market, role.getRoleLevel());
			addSections(market);
			if (loadGraphs) addGraphs(market, req);
		} catch (Exception e) {
			throw new ActionException(e);
		}
		return market;
	}


	/**
	 * Add associated graphs to the supplied market.
	 */
	private void addGraphs(MarketVO market, ActionRequest req) {
		String[] excludeMarkets = req.getParameterValues("excludeGraphs");
		List<Object> params = new ArrayList<>();
		params.add(market.getMarketId());
		
		StringBuilder sql = new StringBuilder(150);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT xr.*, case when xr.value_3_txt is not null then xr.value_3_txt else g.TITLE_NM end as ATTRIBUTE_NM, g.SHORT_TITLE_NM as GROUP_NM FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ");
		sql.append("ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_GRID g ");
		sql.append("ON g.GRID_ID = xr.VALUE_1_TXT ");
		sql.append("WHERE MARKET_ID = ? and a.TYPE_CD = 'GRID' ");
		sql.append("and status_no is null ");
		if (excludeMarkets != null) {
			sql.append("and market_attribute_id not in (");
			DBUtil.preparedStatmentQuestion(excludeMarkets.length, sql);
			sql.append(")");
			for (String param : excludeMarkets) params.add(param);
		}

		sql.append("ORDER BY xr.ORDER_NO");
		log.debug(sql+"|"+market.getMarketId());
		DBProcessor db = new DBProcessor(dbConn);

		List<Object> results = db.executeSelect(sql.toString(), params, new MarketAttributeVO());
		for (Object o : results)
			market.addGraph((MarketAttributeVO)o);
	}


	/**
	 * Get all non grid attributes for the supplied market
	 * @param market
	 */
	protected void addAttributes(MarketVO market, int roleLevel) {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(150);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE_XR xr ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_ATTRIBUTE a ON a.ATTRIBUTE_ID = xr.ATTRIBUTE_ID ");
		sql.append("WHERE MARKET_ID = ? and a.TYPE_CD != 'GRID'  and STATUS_NO in (");
		if (AdminControllerAction.STAFF_ROLE_LEVEL == roleLevel) {
			sql.append("'").append(AdminControllerAction.Status.E).append("', "); 
		}
		sql.append("'").append(AdminControllerAction.Status.P).append("') "); 
		sql.append("ORDER BY xr.ORDER_NO");
		log.debug(sql + "|" + market.getMarketId());

		List<Object> params = new ArrayList<>();
		params.add(market.getMarketId());
		DBProcessor db = new DBProcessor(dbConn);

		List<Object> results = db.executeSelect(sql.toString(), params, new MarketAttributeVO());
		Map<String, List<MarketAttributeVO>> attrMap = new HashMap<>();
		boolean isPreview = Convert.formatBoolean(getAttribute(Constants.PAGE_PREVIEW));
		for (Object o : results) {
			MarketAttributeVO attr = (MarketAttributeVO)o;

			//adjust the links to manage links if in preview mode from manage tool
			if(isPreview) adjustContentLinks(attr); 
			
			if ("LINK".equals(attr.getAttributeTypeCd()) ||
					"ATTACH".equals(attr.getAttributeTypeCd())) {
				// Links need to be specailly sorted in order to display properly
				addLink(attrMap, attr);
			} else {
				market.addMarketAttribute(attr);
			}
		}

		for (Entry<String, List<MarketAttributeVO>> entry : attrMap.entrySet()) {
			for (MarketAttributeVO m : entry.getValue())
				market.addMarketAttribute(m);
		}
	}

	/**
	 * Modifies public links to their corresponding manage tool link
	 * @param attribute
	 */
	protected void adjustContentLinks(MarketAttributeVO attribute){
		SiteVO siteData = (SiteVO)getAttribute(Constants.SITE_DATA);
		BiomedLinkCheckerUtil linkUtil = new BiomedLinkCheckerUtil(dbConn, siteData);
		
		String attrType = attribute.getAttributeTypeCd();
		if("LINK".equals(attrType)) {
			attribute.setValueText(linkUtil.modifyPlainURL(attribute.getValueText()));
		}else if("HTML".equals(attrType)) {
			attribute.setValueText(linkUtil.modifySiteLinks(attribute.getValueText()));
		}
	}

	/**
	 * Add the link to the proper list, including specialized lists for attatchments
	 * @param attrMap
	 * @param attr
	 */
	private void addLink(Map<String, List<MarketAttributeVO>> attrMap, MarketAttributeVO attr) {
		//make sure the list we're about to append to exists on the map first
		if (attrMap.get(attr.getAttributeId()) == null) 
			attrMap.put(attr.getAttributeId(), new ArrayList<MarketAttributeVO>());
		
		attrMap.get(attr.getAttributeId()).add(attr);
	}


	/**
	 * Get all sections for supplied market
	 * @param market
	 * @throws ActionException
	 */
	protected void addSections(MarketVO market) throws ActionException {
		StringBuilder sql = new StringBuilder(350);
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(customDb).append("BIOMEDGPS_MARKET m ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_MARKET_SECTION ms ON m.MARKET_ID = ms.MARKET_ID ");
		sql.append("LEFT JOIN ").append(customDb).append("BIOMEDGPS_SECTION s ON s.SECTION_ID = ms.SECTION_ID ");
		sql.append("WHERE m.MARKET_ID = ? ");
		log.debug(sql);
		SmarttrakTree t = loadDefaultTree();
		t.buildNodePaths();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, market.getMarketId());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				market.addSection(new SectionVO(rs));
				Node n = null;

				if (!StringUtil.isEmpty(rs.getString("SECTION_ID"))) 
					n = t.findNode(rs.getString("SECTION_ID"));

				if (n != null) {
					SectionVO sec = (SectionVO) n.getUserObject();
					market.addACLGroup(Permission.GRANT, sec.getSolrTokenTxt());
				}
			}

		} catch (Exception e) {
			throw new ActionException(e);
		}
	}


	/**
	 * returns the default hierarchy built by the hierarchy action
	 * @return
	 */
	private SmarttrakTree loadDefaultTree() {
		// load the section hierarchy Tree from the hierarchy action
		SectionHierarchyAction sha = new SectionHierarchyAction();
		sha.setAttributes(getAttributes());
		sha.setDBConnection(getDBConnection());
		return sha.loadDefaultTree();
	}

	/**
	 * Get all markets from the database
	 * @param req
	 * @throws ActionException
	 */
	protected void retrieveFromSolr(ActionRequest req) throws ActionException {
		// Pass along the proper information for a search to be done.
		ModuleVO mod = (ModuleVO)getAttribute(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		// This should never use an id. Remove it from now.
		String tempId = req.getParameter(SolrAction.REQ_PARAM_1);
		req.setParameter(SolrAction.REQ_PARAM_1, null);

		// Build the solr action
		SolrAction sa = new SmarttrakSolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);
		
		req.setParameter(SolrAction.REQ_PARAM_1, tempId);

		mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SolrResponseVO res = (SolrResponseVO) mod.getActionData();
		req.setAttribute("marketList", orderMarkets(res));
	}


	/**
	 * Create a tree based on the markets in order to ensure proper parent child relationships
	 * and place the list of root children into the module data
	 * @param res
	 * @return 
	 */
	protected List<Node> orderMarkets(SolrResponseVO res) {
		List<Node> markets = new ArrayList<>();
		for (SolrDocument doc : res.getResultDocuments()) {
			Node n = new Node((String) doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID), (String) doc.getFieldValue("parentId_s"));
			n.setUserObject(doc);
			markets.add(n);
		}

		Tree t = new Tree(markets);

		return t.getRootNode().getChildren();
	}
}