/**
 *
 */
package com.biomed.smarttrak.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.siliconmtn.data.Node;

import net.sf.json.JSONObject;

/****************************************************************************
 * <b>Title</b>: GapTableVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Object manages all the data for the Gap Table.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 6, 2017
 ****************************************************************************/
public class GapTableVO implements Serializable {

	private static final long serialVersionUID = 7621234595265372737L;

	/**
	 * Enum containing Sort Types.
	 */
	public enum SortMethod {NAME("name"), PORTFOLIO("portfolio");
		private String methodName;
		SortMethod(String methodName) {
			this.methodName = methodName;
		}

		public String getMethodName() {
			return this.methodName;
		}
	}

	/**
	 * Enum containing Column Keys for the Headers.
	 */
	public enum ColumnKey {GPARENT, PARENT, CHILD}

	private Map<String, GapCompanyVO> companies;
	private List<Node> headers;
	private Map<String, List<GapColumnVO>> headerCols;
	private Map<String, Node> columnMap;
	private int scaffolding;
	private JSONObject state;

	public GapTableVO() {
		companies = new HashMap<>();
		headers = new ArrayList<>();
	}

	/**
	 * @return the companies
	 */
	public Map<String, GapCompanyVO> getCompanies() {
		return companies;
	}

	/**
	 * @return the columns
	 */
	public List<Node> getHeaders() {
		return headers;
	}

	/**
	 * @param companies the companies to set.
	 */
	public void setCompanies(Map<String, GapCompanyVO> companies) {
		this.companies = companies;

		//Populate Company Rows with Column data.
		buildRows();
	}

	/**
	 * @param columns the columns to set.
	 */
	public void setHeaders(List<Node> headers) {
		this.headers = headers;
		buildHeaderCols();
		this.columnMap = getColumnMap();
	}

	/**
	 * @return the headerCols
	 */
	public Map<String, List<GapColumnVO>> getHeaderCols() {
		return headerCols;
	}

	/**
	 * @return the scaffolding
	 */
	public int getScaffolding() {
		return scaffolding;
	}

	/**
	 * @param headerCols the headerCols to set.
	 */
	public void setHeaderCols(Map<String, List<GapColumnVO>> headerCols) {
		this.headerCols = headerCols;
	}

	/**
	 * @param scaffolding the scaffolding to set.
	 */
	public void setScaffolding(int scaffolding) {
		this.scaffolding = scaffolding;
	}

	/**
	 * Build the Row Data for GapCompanies.
	 */
	private void buildRows() {
		for(GapCompanyVO c : companies.values()) {
			c.buildCellData(getColumns());
		}
	}

	/**
	 * Helper method, converted from JSTL that manages building out the Gap
	 * Analysis Table Header Columns.
	 * @return
	 */
	private void buildHeaderCols() {
		List<GapColumnVO> gParent = new ArrayList<>();
		List<GapColumnVO> parent = new ArrayList<>();
		List<GapColumnVO> child = new ArrayList<>();
		scaffolding = 0;
		for(int i = 0; i < headers.size(); i++) {
			Node g = headers.get(i);
			boolean altCol = i % 2 != 0;
			int numKids = 0;
			List<GapColumnVO> primParent = new ArrayList<>();
			List<GapColumnVO> primChild = new ArrayList<>();
			List<GapColumnVO> altChild = new ArrayList<>();
			List<Node> pNodes = g.getChildren();

			boolean hasGrandKids = false;

			if(pNodes != null && !pNodes.isEmpty()) {
				for(int j = 0; j < pNodes.size(); j++) {
					Node p = pNodes.get(j);
					List<Node> cNodes = p.getChildren();

					if(cNodes != null && !cNodes.isEmpty()) {
						for(int k = 0; k < cNodes.size(); k++) {
							Node c = cNodes.get(k);
							primChild.add(new GapColumnVO(altCol, c.getNodeId(), c.getNodeName()));
							altChild.add(new GapColumnVO(altCol, p.getNodeId(), p.getNodeName()));
							numKids++;
							hasGrandKids = true;
						}
						primParent.add(new GapColumnVO(altCol, null, p.getNodeName(), cNodes.size()));
					} else {
						numKids++;
						primParent.add(new GapColumnVO(altCol, null, null));
						primChild.add(new GapColumnVO(altCol, p.getNodeId(), p.getNodeName()));
						altChild.add(new GapColumnVO(altCol, p.getNodeId(), p.getNodeName()));
					}
				}
			}

			scaffolding += numKids;

			if(hasGrandKids) {
				gParent.add(new GapColumnVO(altCol, null, g.getNodeName(), numKids));
				parent.addAll(primParent);
				child.addAll(primChild);
			} else {
				gParent.add(new GapColumnVO(altCol, null, g.getNodeName(), numKids, 2));
				child.addAll(altChild);
			}
		}

		headerCols = new HashMap<>(); 
		headerCols.put(ColumnKey.GPARENT.name(), gParent);
		headerCols.put(ColumnKey.PARENT.name(), parent);
		headerCols.put(ColumnKey.CHILD.name(), child);
	}

	/**
	 * Helper method that extracts Columns out from all header data.  Stores
	 * results in a map for easy retrieval.
	 * @return
	 */
	public Map<String, Node> getColumnMap() {
		Map<String, Node> cols = new LinkedHashMap<>();

		for(Node g : headers) {
			for(Node p : g.getChildren()) {

				/*
				 * If Parent is a leaf, add it to the column Map and continue.
				 * Else add all Children Nodes.
				 */
				if(p.isLeaf()) {
					cols.put(p.getNodeId(), p);
					continue;
				}
				cols.putAll(getChildrenNodes(p));
			}
		}
		return cols;
	}

	/**
	 * Process Child Nodes of parents.
	 * @param p
	 * @return
	 */
	private Map<String, Node> getChildrenNodes(Node p) {
		Map<String, Node> cols = new LinkedHashMap<>();
		for(Node c : p.getChildren()) {
			if(c.isLeaf()) {
				cols.put(c.getNodeId(), c);
			}
		}
		return cols;
	}

	/**
	 * Returns Collection of all Columns from the map. 
	 * @return
	 */
	public Collection<Node> getColumns() {
		if(columnMap != null) {
			return columnMap.values();
		}
		return Collections.<Node>emptyList();
	}

	/**
	 * Sets and Applies State to underlying data.
	 * @param state
	 */
	public void setState(JSONObject state) {
		this.state = state;

		applyState();
	}

	/**
	 * Apply Filtering and Sorting on the Companies Data to reflect State Object.
	 */
	protected void applyState() {

		//Quick Fail if state is null
		if(state == null) {
			return;
		}

		//Filter Companies by Selected.
		filterCompanies();

		//Sort Companies
		sortCompanies();
	}

	/**
	 * Sort Companies according to Sort Method and Direction that was submitted
	 * in the State Data.
	 */
	private void sortCompanies() {

		List<Map.Entry<String, GapCompanyVO>> list = new LinkedList<>(companies.entrySet());

		//Sort the Companies.
		Collections.sort(list, new CompanyComparator());

		companies = new LinkedHashMap<>();
		for (Map.Entry<String, GapCompanyVO> entry : list) {
			companies.put(entry.getKey(), entry.getValue());
		}
	}

	/**
	 * Perform Filtering on the Companies Map like we would on front end so that
	 * Report matches screen user Exported.
	 */
	private void filterCompanies() {

		//Create Temporary Map of comanies data.
		Map<String, GapCompanyVO> tComp = companies;

		//Filter By Selected if applicable.
		if(state.containsKey("LOCKED_ITEMS")) {
			Object [] selRows = state.getJSONArray("LOCKED_ITEMS").toArray();

			//If there are locked Items, filter out the rest.
			if(selRows.length > 0) {
				tComp = filterLockedItems(tComp, selRows);
			}
		}

		//Filter by Column.
		if(state.containsKey("SEL_COLUMNS")) {
			Object [] selCols = state.getJSONArray("SEL_COLUMNS").toArray();

			/*
			 * If there Selected Columns, Filter out rows without data and select
			 * the children Rows.
			 */
			if(selCols.length > 0) {

				//Filter Rows By Column.
				for(Object s : selCols) {
					tComp = filterRows(tComp, s.toString());
				}

				//Set Selected on Columns.
				setSelectedColumns(selCols);
			}
		}

		//Update companies map with Filter Results.
		companies = tComp;
	}

	/**
	 * Iterate over selRows Array and add get the matching rows off Companies to
	 * store on temp Map.
	 * @param tComp
	 */
	private Map<String, GapCompanyVO> filterLockedItems(Map<String, GapCompanyVO> tComp, Object [] selRow) {
		Map<String, GapCompanyVO> newCompanies = new HashMap<>();
		for(Object rowId : selRow) {
			newCompanies.put(rowId.toString(), tComp.get(rowId.toString()));
		}

		return newCompanies;
	}

	/**
	 * Iterate over the given selColumns Array and tag Children headers as
	 * selected where they match.
	 * @param sel
	 */
	private void setSelectedColumns(Object[] selColumns) {
		List<GapColumnVO> children = headerCols.get(ColumnKey.CHILD.name());
		for(GapColumnVO g : children) {
			for(Object s : selColumns) {
				if(g.getId().equals(s)) {
					g.setSelected(true);
				}
			}
		}
	}

	/**
	 * Iterate over the Companies map and remove rows that don't have data in
	 * the column matching the given columnId.
	 * @param tComp
	 * @param columnId
	 * @return
	 */
	private Map<String, GapCompanyVO> filterRows(Map<String, GapCompanyVO> tComp, String columnId) {
		Map<String, GapCompanyVO> newCompanies = new HashMap<>();
		for(Entry<String, GapCompanyVO> e : tComp.entrySet()) {
			GapCompanyVO c = e.getValue();
			for(GapCellVO td : c.getCells()) {
				if(td.getColumnId().equals(columnId) && td.getScore() > 0) {
					newCompanies.put(e.getKey(), e.getValue());
					break;
				}
			}
		}
		return newCompanies;
	}


	/**
	 * Return State
	 * @return
	 */
	public JSONObject getState() {
		return this.state;
	}

	/**
	 * **************************************************************************
	 * <b>Title</b>: GapTableVO.java
	 * <b>Project</b>: WC_Custom
	 * <b>Description: </b> Inner Comparator for Sorting Companies based on State.
	 * <b>Copyright:</b> Copyright (c) 2017
	 * <b>Company:</b> Silicon Mountain Technologies
	 * 
	 * @author Billy Larsen
	 * @version 1.0
	 * @since Mar 20, 2017
	 ***************************************************************************
	 */
	private class CompanyComparator implements Comparator<Map.Entry<String, GapCompanyVO>> {

		@Override
		public int compare(Map.Entry<String, GapCompanyVO> o1, Map.Entry<String, GapCompanyVO> o2) {

			int order = state.getInt("SORT_DIR");

			//Sort by Name or Portfolio.
			if(SortMethod.PORTFOLIO.getMethodName().equals(state.getString("ACTIVE_SORT"))) {
				return (Integer.valueOf(o1.getValue().getPortfolioNo())).compareTo(Integer.valueOf(o2.getValue().getPortfolioNo())) * order;
			} else {
				return (o1.getValue().getCompanyName()).compareTo(o2.getValue().getCompanyName()) * order;
			}
	    }
	}
}