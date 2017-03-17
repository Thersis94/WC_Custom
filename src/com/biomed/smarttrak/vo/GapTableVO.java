/**
 *
 */
package com.biomed.smarttrak.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.data.Node;

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

	/**
	 *
	 */
	private static final long serialVersionUID = 7621234595265372737L;

	public enum ColumnKey {GPARENT, PARENT, CHILD}
	private Map<String, GapCompanyVO> companies;
	private List<Node> headers;
	private Map<String, List<GapColumnVO>> headerCols;
	private Map<String, Node> columnMap;
	private int scaffolding;

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

	private void buildRows() {
		for(GapCompanyVO c : companies.values()) {
			c.buildCellData(getColumns());
		}
	}

	/**
	 * Helper method that manages building out the Gap Analysis Table Header
	 * Columns.
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
						parent.add(new GapColumnVO(altCol, null, p.getNodeName(), cNodes.size()));
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
				if(p.isLeaf()) {
					cols.put(p.getNodeId(), p);
				} else {
					for(Node c : p.getChildren()) {
						if(c.isLeaf()) {
							cols.put(c.getNodeId(), c);
						}
					}
				}
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
}