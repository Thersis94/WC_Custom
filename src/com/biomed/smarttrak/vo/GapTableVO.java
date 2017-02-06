/**
 *
 */
package com.biomed.smarttrak.vo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
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
 * @author raptor
 * @version 1.0
 * @since Feb 6, 2017
 ****************************************************************************/
public class GapTableVO implements Serializable {

	/**
	 *
	 */
	private static final long serialVersionUID = 7621234595265372737L;
	private Map<String, GapCompanyVO> companies;
	private List<Node> columns;

	public GapTableVO() {
		companies = new HashMap<>();
		columns = new ArrayList<>();
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
	public List<Node> getColumns() {
		return columns;
	}

	/**
	 * @param companies the companies to set.
	 */
	public void setCompanies(Map<String, GapCompanyVO> companies) {
		this.companies = companies;
	}

	/**
	 * @param columns the columns to set.
	 */
	public void setColumns(List<Node> columns) {
		this.columns = columns;
	}
}