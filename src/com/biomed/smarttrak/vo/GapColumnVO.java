package com.biomed.smarttrak.vo;

import java.util.List;

/****************************************************************************
 * <b>Title</b>: GapColumnVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages Table Cell Data for Gap Analysis.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 15, 2017
 ****************************************************************************/
public class GapColumnVO {

	private List<GapColumnVO> columns;
	private String name;
	private String id;
	private boolean selected;
	private int rowSpan = 1;
	private int colSpan = 1;
	private int colGroupNo;

	/**
	 * @param altCol
	 * @param nodeId
	 * @param nodeName
	 */
	public GapColumnVO(int colGroupNo, String nodeId, String nodeName) {
		this.id = nodeId;
		this.name = nodeName;
		this.colGroupNo = colGroupNo;
	}

	/**
	 * @param altCol
	 * @param nodeId
	 * @param nodeName
	 * @param colSpan
	 */
	public GapColumnVO(int colGroupNo, String nodeId, String nodeName, int colSpan) {
		this(colGroupNo, nodeId, nodeName);
		this.colSpan = colSpan;
	}

	/**
	 * @param altCol
	 * @param nodeId
	 * @param nodeName
	 * @param colSpan
	 * @param rowSpan
	 */
	public GapColumnVO(int colGroupNo, String nodeId, String nodeName, int colSpan, int rowSpan) {
		this(colGroupNo, nodeId, nodeName, colSpan);
		this.rowSpan = rowSpan;
	}

	/**
	 * @return the columns
	 */
	public List<GapColumnVO> getColumns() {
		return columns;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the id
	 */
	public String getId() {
		return id;
	}

	/**
	 * @return the altCol
	 */
	public int getColGroupNo() {
		return colGroupNo;
	}

	/**
	 * @return the rowSpan
	 */
	public int getRowSpan() {
		return rowSpan;
	}

	/**
	 * @return the colSpan
	 */
	public int getColSpan() {
		return colSpan;
	}

	/**
	 * @param columns the columns to set.
	 */
	public void setColumns(List<GapColumnVO> columns) {
		this.columns = columns;
	}

	/**
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * @param altCol the altCol to set.
	 */
	public void setColGroupNo(int colGroupNo) {
		this.colGroupNo = colGroupNo;
	}

	/**
	 * @param rowSpan the rowSpan to set.
	 */
	public void setRowSpan(int rowSpan) {
		this.rowSpan = rowSpan;
	}

	/**
	 * @param colSpan the colSpan to set.
	 */
	public void setColSpan(int colSpan) {
		this.colSpan = colSpan;
	}

	/**
	 * @param selected set if selected.
	 */
	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	/**
	 * @return is selected
	 */
	public boolean isSelected() {
		return this.selected;
	}
}