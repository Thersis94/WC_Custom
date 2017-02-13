/**
 *
 */
package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import com.biomed.smarttrak.admin.ContentHierarchyAction;
import com.biomed.smarttrak.admin.vo.GapColumnVO;
import com.biomed.smarttrak.vo.GapCompanyVO;
import com.biomed.smarttrak.vo.GapTableVO;
import com.biomed.smarttrak.vo.SaveStateVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;

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
public class GapAnalysisAction extends ContentHierarchyAction {

	public static final String GAP_ROOT_ID = "GAP_ANALYSIS_ROOT";
	public static final String GAP_CACHE_KEY = "GAP_ANALYSIS_TREE_CACHE_KEY";
	private String [] selNodes;
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
		if(req.hasParameter("selNodes")) {

			//Instantiate GapTableVO to Store Data.
			GapTableVO gtv = new GapTableVO();

			//Filter the List of Nodes to just the ones we want.
			selNodes = req.getParameterValues("selNodes");
			gtv.setHeaders(filterNodes(getColData(req)));

			//Get Table Body Data based on columns in the GTV.
			loadGapTableData(gtv);

			super.putModuleData(gtv);
		} else {

			//TODO - replace with actual userId value.
			String userId = StringUtil.checkVal(req.getParameter("userId"), "user1");
			String saveStateId = req.getParameter("saveStateId");
			super.putModuleData(getSaveStates(userId, saveStateId));
		}
	}

	/**
	 * Helper method returns list of SaveStates.
	 * @param req
	 * @return
	 */
	private List<SaveStateVO> getSaveStates(String userId, String saveStateId) {
		List<SaveStateVO> saveStates = new ArrayList<SaveStateVO>();
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
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_ga_savestate where user_id = ? ");

		if(hasSaveStateId) {
			sql.append("and save_state_id = ? ");
		}

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
	protected List<Node> getColData(ActionRequest req) throws ActionException {

		List<Node> nodes;

		//Get Sections from super.
		super.retrieve(req);
		ModuleVO mod = (ModuleVO) this.getAttribute(Constants.MODULE_DATA);
		nodes = (List<Node>) mod.getActionData();

		//Get All the columns.
		nodes.addAll(getColumns());

		//Build a tree and sort nodes so children are set properly.
		Tree t = new Tree(nodes);

		//Filter down to the Gap Node and retrieve it's children.
		Node n = t.findNode(GAP_ROOT_ID);
		nodes = n.getChildren();

		//Get Columns
		return nodes;
	}

	/**
	 * Helper method that filters the Selected Child Nodes out of the main tree.
	 * @param selNodes
	 * @return
	 */
	protected List<Node> filterNodes(List<Node> nodes) {
		List<Node> filteredNodes = new ArrayList<>();
		for(Node g : nodes) {
			for(Node p : g.getChildren()) {
				ListIterator<Node> nIter = p.getChildren().listIterator();
				while(nIter.hasNext()) {
					Node n = nIter.next();
					for(int i = 0; i < selNodes.length; i++) {
						if(n.getNodeId().equals(selNodes[i]) && n.getNumberChildren() > 0) {  
							filteredNodes.add(n);
							nIter.remove();
							selNodes = (String[]) ArrayUtils.remove(selNodes, i);
							break;
						}
					}
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
	protected List<Node> getColumns() {
		List<Node> nodes = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(getColumnListSql())) {
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				GapColumnVO gap = new GapColumnVO(rs);
				Node n = new Node(gap.getGaColumnId(), gap.getSectionId());
				n.setNodeName(gap.getButtonTxt());
				n.setUserObject(gap);
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
	protected String getColumnListSql() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_ga_column order by order_no");
		return sql.toString();
	}

	/**
	 * Helper method that manages retrieving the Gap Table Data and organizing it into the GapTable
	 * @param selNodes
	 * @return
	 */
	protected void loadGapTableData(GapTableVO gtv) {
		Map<String, GapCompanyVO> companies = new HashMap<>();
		try(PreparedStatement ps = dbConn.prepareStatement(getTableBuilderSql(gtv.getColumns().size()))) {
			int i = 1;
			for(String id : gtv.getColumnMap().keySet()) {
				ps.setString(i++, id);
			}

			ResultSet rs = ps.executeQuery();

			GapCompanyVO c;
			while(rs.next()) {
				if(companies.containsKey(rs.getString("company_id"))) {
					c = companies.get(rs.getString("company_id"));
				} else {
					c = new GapCompanyVO(rs);
					companies.put(c.getCompanyId(), c);
				}

				c.addRegulation(rs.getString("ga_column_id"), rs.getInt("status_id"), rs.getInt("region_Id"));
			}
		} catch (SQLException e) {
			log.error("Problem Retrieving Gap Table Data", e);
		}

		log.debug("Retrieved " + companies.size() + " company Records.");
		gtv.setCompanies(companies);
	}

	/**
	 * Helper method that builds the Table Body Query
	 * @param length
	 * @return
	 */
	protected String getTableBuilderSql(int numColumns) {
		StringBuilder sql = new StringBuilder(850);

		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select distinct c.ga_column_id, g.short_nm_txt, g.company_nm, g.company_id, r.status_id, r.region_id ");
		sql.append("from ").append(custom).append("biomedgps_section a ");
		sql.append("inner join ").append(custom).append("biomedgps_section b ");
		sql.append("on a.section_id = b.parent_id ");
		sql.append("inner join ").append(custom).append("biomedgps_ga_column c ");
		sql.append("on b.section_id = c.section_id ");
		sql.append("left outer join ").append(custom).append("biomedgps_ga_column_attribute_xr d ");
		sql.append("on d.ga_column_id = c.ga_column_id ");
		sql.append("inner join ").append(custom).append("biomedgps_product_attribute_xr e ");
		sql.append("on d.attribute_id = e.attribute_id ");
		sql.append("inner join ").append(custom).append("biomedgps_product f ");
		sql.append("on e.product_id = f.product_id ");
		sql.append("inner join ").append(custom).append("biomedgps_product_regulatory r ");
		sql.append("on f.product_id = r.product_id ");
		sql.append("inner join ").append(custom).append("biomedgps_company g ");
		sql.append("on f.company_id = g.company_id ");
		sql.append("where c.ga_column_id in ( ");
		for(int i = 0; i < numColumns; i++) {
			if(i > 0) {
				sql.append(", ");
			}
			sql.append("?");
		}
		sql.append(") order by g.company_nm");

		return sql.toString();
	}

	/*
	 * (non-Javadoc)
	 * @see com.biomed.smarttrak.admin.ContentHierarchyAction#build(com.siliconmtn.action.ActionRequest)
	 */
	public void build(ActionRequest req) {
		SaveStateVO ss = new SaveStateVO(req);

		DBProcessor dbp = new DBProcessor(dbConn, (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));

		try {
			dbp.save(ss);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Problem Saving State Object.", e.getCause());
		}
	}

	/**
	 * Helper method returns cache key for Content Hierarchy Object.
	 */
	public String getCacheKey() {
		return GAP_CACHE_KEY;
	}
}