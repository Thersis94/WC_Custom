package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.lang.ArrayUtils;

import com.biomed.smarttrak.admin.SectionHierarchyAction;
import com.biomed.smarttrak.admin.report.GapAnalysisReportVO;
import com.biomed.smarttrak.admin.vo.GapColumnVO;
import com.biomed.smarttrak.security.SecurityController;
import com.biomed.smarttrak.vo.GapCompanyVO;
import com.biomed.smarttrak.vo.GapProductVO;
import com.biomed.smarttrak.vo.GapTableVO;
import com.biomed.smarttrak.vo.GapTableVO.ColumnKey;
import com.biomed.smarttrak.vo.SaveStateVO;
import com.biomed.smarttrak.vo.SectionVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.OrderedTree;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

import net.sf.json.JSONObject;

/****************************************************************************
 * <b>Title</b>: GapAnalysisAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Public facing action for Processing GAP Analysis
 * Requests.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 1.0
 * @since Jan 13, 2017
 ****************************************************************************/
public class GapAnalysisAction extends SectionHierarchyAction {

	/*
	 * Gap Attribute Types.
	 */
	public enum AType {MARKET, CLASSIFICATION, INDICATION, TECHNOLOGY, METHOD}

	private static final String INNER_JOIN = "inner join ";
	private static final String FROM = "from ";
	public static final String GAP_ROOT_ID = "GAP_ANALYSIS_ROOT";
	public static final String GAP_CACHE_KEY = "GAP_ANALYSIS_TREE_CACHE_KEY";
	private String[] selNodes;
	public GapAnalysisAction() {
		super();
	}

	public GapAnalysisAction(ActionInitVO init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SecurityController.isGaAuth(req);

		if (req.hasParameter("selNodes")) {
			//Instantiate GapTableVO to Store Data.
			GapTableVO gtv = new GapTableVO();

			req.setParameter("sectionId", null);
			//Filter the List of Nodes to just the ones we want.
			selNodes = req.getParameterValues("selNodes");
			gtv.setHeaders(filterNodes(getColData(req)));

			//Get Table Body Data based on columns in the GTV.
			if (!gtv.getColumns().isEmpty())
				loadGapTableData(gtv);

			//forward to Report if parameter present.
			if (req.hasParameter("buildReport")) {
				buildReport(gtv, req);
			} else {
				putModuleData(gtv);
			}

		} else if (req.hasParameter("getProducts")) {
			String sectionId = req.getParameter("sectionId");
			String regionId = req.getParameter("regionId");
			String companyId = req.getParameter("companyId");
			String columnId = req.getParameter("columnId");
			putModuleData(getProductList(regionId, companyId, columnId, sectionId));

		} else if (req.hasParameter("getState")) {
			UserVO vo = (UserVO) req.getSession().getAttribute(Constants.USER_DATA);
			String userId = StringUtil.checkVal(vo.getUserId());

			String saveStateId = req.getParameter("saveStateId");
			putModuleData(getSaveStates(userId, saveStateId));
		}
	}
	

	/**
	 * turns the GapTableVO into a report to download from WC.
	 * @param gtv
	 * @param req
	 */
	protected void buildReport(GapTableVO gtv, ActionRequest req) {
		//Set State on the GapTableVO.
		gtv.setState(JSONObject.fromObject(req.getParameter("state")));

		//Build Report
		GapAnalysisReportVO rpt = new GapAnalysisReportVO((String) attributes.get(Constants.QS_PATH));
		rpt.setData(gtv);
		rpt.setSite((SiteVO)req.getAttribute(Constants.SITE_DATA));

		//Set Report on Attributes Map.
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}

	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.ContentHierarchyAction#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) {
		DBProcessor dbp = new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		try {
			dbp.save(new SaveStateVO(req));
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Problem Saving State Object.", e.getCause());
		}
	}


	/**
	 * Helper method returns cache key for Content Hierarchy Object.
	 */
	@Override
	public String getCacheKey() {
		return GAP_CACHE_KEY;
	}


	/**
	 * Return list of products that are of a given regionId, companyId and columnId
	 * @param regionId
	 * @param companyId
	 * @param columnId
	 * @param sectionId 
	 * @return
	 */
	private List<Object> getProductList(String regionId, String companyId, String columnId, String sectionId) {

		//Load Attributes for the Given Column.
		Map<String, Map<AType, String>> attrs = loadProductAttributes(columnId);

		//Using Retrieved Attributes, load Products.
		return loadProducts(attrs, regionId, companyId, sectionId);
	}


	/**
	 * Helper method retrieves all the products for the given company related
	 * to the given region and column attributes.
	 *
	 * @param colAttrs
	 * @param regionId
	 * @param companyId
	 * @return
	 */
	private List<Object> loadProducts(Map<String, Map<AType, String>> colAttrs, String regionId, String companyId, String sectionId) {
		/*
		 * Get the Attribute Data Map.
		 * For this call there will only ever be one entry for the requested column.
		 */
		Entry<String, Map<AType, String>> aData  = colAttrs.entrySet().iterator().next();

		//Build the List of Query Params.
		List<Object> params = buildProductParams(aData, companyId, sectionId);

		//Build the Product List Sql Query
		String sql = getProductListSql(regionId.startsWith("u"), aData.getValue(), sectionId);

		//Retrieve Products
		DBProcessor db = new DBProcessor(dbConn, (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA));
		List<Object>  data = db.executeSelect(sql, params, new GapProductVO());
		if(!data.isEmpty()) {
			log.debug("loaded " + ((GapProductVO)data.get(0)).getProducts().size() + " products");
		}

		//Return
		return data;
	}

	/**
	 * Helper method builds the Parameter Map for the Product List Query.
	 * @param aData
	 * @param companyId
	 * @param sectionId 
	 * @return
	 */
	private List<Object> buildProductParams(Entry<String, Map<AType, String>> aData, String companyId, String sectionId) {
		List<Object> params = new ArrayList<>();
		//Set company Id
		params.add(companyId);

		//Set Published Flag
		params.add("P");

		//Set RegionId Flag
		params.add("1");

		//Set ColumnId
		params.add(aData.getKey());

		//Add SectionId for Product
		params.add(sectionId);

		//Add Attribute Ids.
		addAttributeParams(params, aData);

		return params;
	}

	/**
	 * Helper method that adds Attribute Ids to the given Params Map.
	 * Set Attribute Ids.
	 * Iterate over the AType values and check for presence to ensure that the
	 * same order is used in both the query building and the value setting.
	 * @param params
	 * @param aData
	 */
	private void addAttributeParams(List<Object> params, Entry<String, Map<AType, String>> aData) {
		for(AType a : AType.values()) {
			if(aData.getValue().containsKey(a)) {
				for(String s : aData.getValue().get(a).split(",")) {
					params.add(s);
				}
			}
		}
	}

	/**
	 * Helper method loads All Product Attribute XRs for a given column.
	 * @param columnId
	 * @return
	 */
	private Map<String, Map<AType, String>> loadProductAttributes(String columnId) {
		Map<String, Map<AType, String>> attrs = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(getColumnAttrs(1))) {
				ps.setString(1, columnId);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				attrs.put(rs.getString("ga_column_id"), buildAttributesMap(rs));
			}
		} catch (SQLException e) {
			log.error("Problem Retrieving Gap Column Attributes.", e);
		}
		return attrs;
	}

	/**
	 * Helper method returns list of SaveStates.
	 * @param req
	 * @return
	 */
	private List<SaveStateVO> getSaveStates(String userId, String saveStateId) {
		List<SaveStateVO> saveStates = new ArrayList<>();
		boolean hasSaveStateId = !StringUtil.isEmpty(saveStateId);
		try(PreparedStatement ps = dbConn.prepareStatement(getSaveStateSql(hasSaveStateId))) {
			ps.setString(1, userId);

			if(hasSaveStateId) {
				ps.setString(2, saveStateId);
			}

			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				saveStates.add(new SaveStateVO(rs));
			}
		} catch (SQLException e) {
			log.error(e);
		}
		return saveStates;
	}

	/**
	 * Helper method retrieves Gap Analysis Save States.
	 * @param hasSaveStateId
	 * @return
	 */
	private String getSaveStateSql(boolean hasSaveStateId) {
		StringBuilder sql = new StringBuilder(200);
		sql.append("select * ").append(FROM).append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_ga_savestate where user_id = ? ");

		if (hasSaveStateId)
			sql.append("and save_state_id = ? ");

		sql.append("order by order_no");
		return sql.toString();
	}


	/**
	 * Helper method that returns all the representing Columns Data.
	 * 
	 * TODO Figure out how to cache the full tree.  Was running into weird
	 * cache poisoning issues after initial request where the cached tree
	 * was filtered already and couldn't re-filter it when selOptions were
	 * changed.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private List<Node> getColData(ActionRequest req) throws ActionException {
		//Get Sections from super.
		super.retrieve(req);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		List<Node> nodes = (List<Node>) mod.getActionData();

		//Get All the columns.
		nodes.addAll(getColumns());

		//Build a tree and sort nodes so children are set properly.
		Tree t = new OrderedTree(nodes);

		//Filter down to the Gap Node and retrieve it's children.
		Node n = t.findNode(MASTER_ROOT);
		if(n != null) {
			nodes = n.getChildren();
		} else {
			throw new ActionException("No Node Found");
		}

		//Get Columns
		return nodes;
	}


	/**
	 * Helper method that filters the Selected Child Nodes out of the main tree.
	 * @param selNodes
	 * @return
	 */
	private List<Node> filterNodes(List<Node> nodes) {
		List<Node> filteredNodes = new ArrayList<>();
		for(Node g : nodes) {
			for(Node p : g.getChildren()) {
				if(((SectionVO)p.getUserObject()).isGapNo()) {
					ListIterator<Node> nIter = p.getChildren().listIterator();
					filteredNodes.addAll(filterChildNodes(nIter));
				}
			}
		}
		return filteredNodes;
	}


	/**
	 * Helper method that filters childNodes out by selected Nodes.
	 * @param nIter
	 * @return
	 */
	private Collection<Node> filterChildNodes(ListIterator<Node> nIter) {
		List<Node> filteredNodes = new ArrayList<>();
		while(nIter.hasNext()) {
			Node n = nIter.next();
			for (int i = 0; i < selNodes.length; i++) {
				if (n.getNodeId().equals(selNodes[i]) && n.getNumberChildren() > 0) {
					filteredNodes.add(n);
					nIter.remove();
					selNodes = (String[]) ArrayUtils.remove(selNodes, i);
					break;
				}
			}
		}
		return filteredNodes;
	}


	/**
	 * Get All Columns from the system.
	 * @param gtv
	 * @param selNodes
	 */
	private List<Node> getColumns() {
		GapColumnVO gap;
		Node n;
		List<Node> nodes = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(getColumnListSql())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				gap = new GapColumnVO(rs);
				n = new Node(gap.getGaColumnId(), gap.getSectionId());
				n.setNodeName(gap.getButtonTxt());
				n.setUserObject(gap);
				n.setOrderNo(gap.getOrderNo());
				nodes.add(n);
			}
		} catch (SQLException e) {
			log.error("Error retrieving Columns", e);
		}
		return nodes;
	}


	/**
	 * Helper method returns SQL to get all Columns in the system.
	 * @param size
	 * @return
	 */
	private String getColumnListSql() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * ").append(FROM).append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_ga_column order by order_no");
		return sql.toString();
	}


	/**
	 * Helper method that manages retrieving the Gap Table Data and organizing it into the GapTable
	 * @param selNodes
	 * @return
	 */
	private void loadGapTableData(GapTableVO gtv) {
		Map<String, Map<AType, String>> attrs = loadColumnAttributes(gtv);

		Map<String, GapCompanyVO> companies = loadCompanies(attrs, gtv);
		log.debug("Retrieved " + companies.size() + " company Records.");
		gtv.setCompanies(companies);
	}

	/**
	 * Helper method that retrieves all the Column Attributes for a Gap Table
	 * Request.
	 * ColumnId -> Map of Attributes (AType -> Comma,Delim,List,of,Attribute,Ids)
	 * @param gtv
	 * @return
	 */
	private Map<String, Map<AType, String>> loadColumnAttributes(GapTableVO gtv) {
		int i = 1;
		Map<String, Map<AType, String>> attrs = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(getColumnAttrs(gtv.getColumns().size()))) {
			for (String id : gtv.getColumnMap().keySet())
				ps.setString(i++, id);

			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				attrs.put(rs.getString("ga_column_id"), buildAttributesMap(rs));
			}
		} catch (SQLException e) {
			log.error("Problem Retrieving Gap Table Data", e);
		}

		return attrs;
	}

	/**
	 * Helper method processes a ResultSet and returns Map of
	 * AType -> Comma,Delim,List,of,Attribute,Ids
	 * @param rs
	 * @return
	 * @throws SQLException 
	 */
	private Map<AType, String> buildAttributesMap(ResultSet rs) throws SQLException {
		Map<AType, String> attrs = new EnumMap<>(AType.class);
		for(AType a : AType.values()) {
			if(!StringUtil.isEmpty(rs.getString(a.toString()))) {
				attrs.put(a, rs.getString(a.toString()));
			}
		}
		return attrs;
	}

	/**
	 * Load Company Data based on attributes information.
	 * @param attrs
	 * @param gtv 
	 */
	private Map<String, GapCompanyVO> loadCompanies(Map<String, Map<AType, String>> attrs, GapTableVO gtv) {
		Map<String, GapCompanyVO> companies = new HashMap<>();

		for(Entry<String, Map<AType, String>> aData : attrs.entrySet()) {
			int i = 1;
			List<Object> params = new ArrayList<>();
			addAttributeParams(params, aData);
			String sectionId = gtv.getHeaderCols().get(ColumnKey.CHILD.toString()).get(aData.getKey()).getSectionId();
			try (PreparedStatement ps = dbConn.prepareStatement(getTableBuilderSql(aData.getValue(), sectionId))) {

				for(String s : sectionId.split(",")) {
					ps.setString(i++, s);
				}

				//Populate PreparedStatement
				for(Object p : params)
					ps.setString(i++, (String)p);

				//Query for Companies.
				addCompanies(companies, ps.executeQuery(), aData.getKey());

			} catch (SQLException e) {
				log.error("Problem Retrieving Gap Table Data", e);
			}
		}
		return companies;
	}

	/**
	 * Helper method iterates the loadCompanies ResultSet and builds Company
	 * VOs and adds Regulations.
	 * @param companies
	 * @param executeQuery
	 * @throws SQLException 
	 */
	private void addCompanies(Map<String, GapCompanyVO> companies, ResultSet rs, String columnId) throws SQLException {
		GapCompanyVO c;

		while (rs.next()) {
			c = companies.get(rs.getString("company_id"));
			if (c == null) {
				c = new GapCompanyVO(rs);
				companies.put(c.getCompanyId(), c);
			}
			c.addRegulation(columnId, rs.getString("status_txt"), rs.getInt("region_Id"));
		}
	}


	/**
	 * Helper method that builds the Table Body Query.
	 * @param colAttrs
	 * @return
	 */
	private String getTableBuilderSql(Map<AType, String> colAttrs, String sectionId) {
		StringBuilder sql = new StringBuilder(2500);
		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select distinct g.short_nm_txt, g.company_nm, g.company_id, s.status_txt, r.region_id ");
		sql.append(FROM).append(custom).append("biomedgps_product f ");
		sql.append("left outer join ").append(custom).append("biomedgps_product_alliance_xr axr ");
		sql.append("on axr.product_id = f.product_id and (axr.alliance_type_id != 'PROD_4' or axr.ga_display_flg > 0) ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_product_regulatory r ");
		sql.append("on f.product_id = r.product_id ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_regulatory_status s ");
		sql.append("on r.status_id = s.status_id ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_company g ");
		sql.append("on f.company_id = g.company_id and g.status_no = 'P' ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_company_attribute_xr h ");
		sql.append("on g.company_id = h.company_id ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_company_attribute i ");
		sql.append("on h.attribute_id = i.attribute_id ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_section j ");
		sql.append("on i.section_id = j.section_id ");
		sql.append("where 1 = 1 ");
		sql.append("and f.product_id in ( ");
		buildProdAttributeFilterQuery(sql, colAttrs, sectionId);
		sql.append(") order by g.company_nm");

		log.debug(sql);
		return sql.toString();
	}

	/**
	 * Helper method builds the Product List Sql Query.
	 * @param isUSRegion 
	 * @return
	 */
	private String getProductListSql(boolean isUSRegion, Map<AType, String> colAttrs, String sectionId) {
		StringBuilder sql = new StringBuilder(2500);
		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select distinct f.short_nm, f.product_id, c.column_nm, g.company_nm ");
		sql.append(FROM).append(custom).append("biomedgps_ga_column c ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_ga_column_attribute_xr d ");
		sql.append("on d.ga_column_id = c.ga_column_id ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_product_attribute pa ");
		sql.append("on d.attribute_id = pa.attribute_id ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_product_attribute_xr e ");
		sql.append("on d.attribute_id = e.attribute_id ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_product f ");
		sql.append("on e.product_id = f.product_id ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_product_regulatory r ");
		sql.append("on f.product_id = r.product_id ");
		sql.append(INNER_JOIN).append(custom).append("biomedgps_company g ");
		sql.append("on f.company_id = g.company_id ");
		sql.append("where g.company_id = ? and f.status_no = ? ");
		if(isUSRegion) {
			sql.append("and r.region_id = ? ");
		} else {
			sql.append("and r.region_id != ? ");
		}
		sql.append("and c.ga_column_id = ? ");
		sql.append("and f.product_id in ( ");
		buildProdAttributeFilterQuery(sql, colAttrs, sectionId);
		sql.append(") order by f.short_nm");
		log.debug(sql.toString());
		return sql.toString();
	}

	/**
	 * Helper Method builds the Product Id Selection Query nested inside
	 * other queries that ensures only products we care about are selected.
	 * @param colAttrs
	 * @param sectionId 
	 */
	private void buildProdAttributeFilterQuery(StringBuilder sql, Map<AType, String> colAttrs, String sectionId) {
		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);

		sql.append("select product_id ").append(FROM).append("( ");
			sql.append("select distinct ip.product_id ");
			sql.append(FROM).append(custom).append("biomedgps_product ip ");
			sql.append(INNER_JOIN).append(custom).append("biomedgps_product_section ps ");
			sql.append("on ip.product_id = ps.product_id and ps.section_id = ? ");
			for(AType a : AType.values()) {
				if(colAttrs.containsKey(a)) {
					sql.append(INNER_JOIN).append(custom).append("biomedgps_product_attribute_xr ").append(a.toString()).append(" ");
					sql.append("on ip.product_id = ").append(a.toString()).append(".product_id and ip.status_no = 'P' ");
				}
			}
			sql.append("where 1 = 1 ");
			for(AType a : AType.values()) {
				if(colAttrs.containsKey(a)) {
					sql.append("and ").append(a.toString()).append(".attribute_id in (");
					DBUtil.preparedStatmentQuestion(colAttrs.get(a).split(",").length, sql);
					sql.append(") ");
				}
			}
		sql.append(") as res ");
	}

	/**
	 * Helper method gets comma delimited list of attributes for each column.
	 * @param numColumns
	 * @return
	 */
	protected String getColumnAttrs(int numColumns) {
		StringBuilder sql = new StringBuilder(2500);
		String custom = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select c.ga_column_id, ");
		for(AType a : AType.values()) {
			sql.append("(select string_agg(tm.attribute_id, ',') ");
			sql.append(FROM).append(custom).append("biomedgps_ga_column_attribute_xr xr ");
			sql.append("join ").append(custom).append("biomedgps_product_attribute tm on tm.attribute_id = xr.attribute_id ");
			sql.append("where xr.ga_column_id = c.ga_column_id and tm.parent_id= '").append(a.toString()).append("') as ").append(a.toString()).append(", ");
		}

		//Remove trailing comma
		sql.setLength(sql.length() - 2);

		sql.append(" ").append(FROM).append(custom).append("biomedgps_ga_column c ");
		sql.append("where c.ga_column_id in( ");
		DBUtil.preparedStatmentQuestion(numColumns, sql);
		sql.append(") group by c.ga_column_id ");
		sql.append("order by c.order_no;");

		return sql.toString();
	}
}