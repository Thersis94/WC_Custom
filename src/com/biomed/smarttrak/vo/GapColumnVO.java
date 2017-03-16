package com.biomed.smarttrak.vo;

import java.util.Collection;
import java.util.List;

import com.siliconmtn.data.Node;

/****************************************************************************
 * <b>Title</b>: GapColumnVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Mar 15, 2017
 ****************************************************************************/
public class GapColumnVO {

	private List<GapColumnVO> columns;
	private String name;
	private String id;
	private boolean altCol;
	private int rowSpan = 1;
	private int colSpan = 1;

	/**
	 * 
	 */
	public GapColumnVO() {
	}

	/**
	 * @param altCol
	 * @param nodeId
	 * @param nodeName
	 */
	public GapColumnVO(boolean altCol, String nodeId, String nodeName) {
		this.id = nodeId;
		this.name = nodeName;
		this.altCol = altCol;
	}

	/**
	 * @param altCol
	 * @param nodeId
	 * @param nodeName
	 * @param colSpan
	 */
	public GapColumnVO(boolean altCol, String nodeId, String nodeName, int colSpan) {
		this(altCol, nodeId, nodeName);
		this.colSpan = colSpan;
	}

	/**
	 * @param altCol
	 * @param nodeId
	 * @param nodeName
	 * @param colSpan
	 * @param rowSpan
	 */
	public GapColumnVO(boolean altCol, String nodeId, String nodeName, int colSpan, int rowSpan) {
		this(altCol, nodeId, nodeName, colSpan);
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
	public boolean isAltCol() {
		return altCol;
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
	public void setAltCol(boolean altCol) {
		this.altCol = altCol;
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
}