/**
 *
 */
package com.biomed.smarttrak.action;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.apache.commons.lang.ArrayUtils;

import com.biomed.smarttrak.admin.ContentHierarchyAction;
import com.biomed.smarttrak.admin.vo.GapColumnVO;
import com.biomed.smarttrak.vo.GapTableVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.Node;
import com.siliconmtn.data.Tree;
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

			List<Node> colData = getColData(req);

			//Instantiate GapTableVO to Store Data.
			GapTableVO gtv = new GapTableVO();

			//Filter the List of Nodes to just the ones we want.
			String [] selNodes = req.getParameterValues("selNodes");
			gtv.setColumns(filterNodes(colData, selNodes));

			//Get Table Body Data based on columns in the GTV.
			//loadGapTableData(gtv);

			super.putModuleData(gtv);
		}
	}

	/**
	 * Helper method that returns all the representing Columns Data.
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private List<Node> getColData(ActionRequest req) throws ActionException {

		List<Node> nodes = null;

		//Attempt to read GapColumnData from Cache.
		ModuleVO mod = super.readFromCache(GAP_CACHE_KEY);

		//If not found in cache Load data.
		if(mod == null) {

			//Get Sections from super.
			super.retrieve(req);
			mod = (ModuleVO) this.getAttribute(Constants.MODULE_DATA);
			nodes = (List<Node>) mod.getActionData();

			//Get All the columns.
			nodes.addAll(getColumns());

			//Build a tree and sort nodes so children are set properly.
			Tree t = new Tree(nodes);

			//Filter down to the Gap Node and retrieve it's children.
			Node n = t.findNode(GAP_ROOT_ID);
			nodes = n.getChildren();

			//Save List to cache.
			super.writeToCache(nodes, "SMARTTRAK", GAP_CACHE_KEY);

		} else {
			//Get the Tree off the actionData
			nodes = (List<Node>) mod.getActionData();
		}

		//Get Columns
		return nodes;
	}

	/**
	 * Helper method that filters the Selected Child Nodes out of the main tree.
	 * @param selNodes
	 * @return
	 */
	private List<Node> filterNodes(List<Node> nodes, String[] selNodes) {
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
	private List<Node> getColumns() {
		List<Node> nodes = new ArrayList<Node>();
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
	private String getColumnListSql() {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select * from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_ga_column order by order_no");
		return sql.toString();
	}

//	/**
//	 * Helper method that manages retrieving the Gap Table Data and organizing it into the GapTable
//	 * @param selNodes
//	 * @return
//	 */
//	private void loadGapTableData(GapTableVO gtv) {
//
//		try(PreparedStatement ps = dbConn.prepareStatement(getTableBuilderSql(gtv.getColumns.size()))) {
//
//		} catch (SQLException e) {
//			log.error("Problem Retrieving Gap Table Data", e);
//		}
//	}

//	/**
//	 * @param length
//	 * @return
//	 */
//	private String getTableBuilderSql(int length) {
//		StringBuilder sql = new StringBuilder(850);
//
//		String custom = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
//		sql.append("select a.section_id, a.section_nm, b.section_id, b.section_nm, ");
//		sql.append("c.section_id, c.button_txt, c.ga_column_id, r.*, g.company_nm ");
//		sql.append("from ").append(custom).append("biomedgps_section a ");
//		sql.append("inner join ").append(custom).append("biomedgps_section b ");
//		sql.append("on a.section_id = b.parent_id ");
//		sql.append("inner join ").append(custom).append("biomedgps_ga_column c ");
//		sql.append("on b.section_id = c.section_id ");
//		sql.append("left outer join ").append(custom).append("biomedgps_ga_column_attribute_xr d ");
//		sql.append("on d.ga_column_id = c.ga_column_id ");
//		sql.append("left outer join ").append(custom).append("biomedgps_product_attribute_xr e ");
//		sql.append("on d.attribute_id = e.attribute_id ");
//		sql.append("left outer join ").append(custom).append("biomedgps_product f ");
//		sql.append("on e.product_id = f.product_id ");
//		sql.append("left outer join ").append(custom).append("biomedgps_product_regulatory r ");
//		sql.append("on f.product_id = r.product_id ");
//		sql.append("left outer join ").append(custom).append("biomedgps_company g ");
//		sql.append("on f.company_id = g.company_id ");
//		sql.append("where b.section_nm in ( ");
//		for(int i = 0; i < length; i++) {
//			if(i > 0) {
//				sql.append(", ");
//			}
//			sql.append("?");
//		}
//		sql.append(") order by a.order_no, b.order_no, c.order_no, g.company_nm");
//
//		return sql.toString();
//	}

	public String getCacheKey() {
		return GAP_CACHE_KEY;
	}
}