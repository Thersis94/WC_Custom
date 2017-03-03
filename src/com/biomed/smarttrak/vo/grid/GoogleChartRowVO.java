package com.biomed.smarttrak.vo.grid;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/********************************************************************
 * <b>Title: </b>GoogleChartRowVO.java<br/>
 * <b>Description: </b>Container holding a collection of cells<br/>
 * <b>Copyright: </b>Copyright (c) 2017<br/>
 * <b>Company: </b>Silicon Mountain Technologies
 * @author james
 * @version 3.x
 * @since Feb 28, 2017
 * Last Updated:
 * 	
 *******************************************************************/
public class GoogleChartRowVO implements Serializable, SMTGridRowIntfc {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	/**
	 * 
	 */
	private List<GoogleChartCellVO> c;

	/**
	 * 
	 */
	public GoogleChartRowVO() {
		super();
		
		c = new ArrayList<>(8);
	}

	/**
	 * Returns the collection of cells
	 * @return
	 */
	public List<GoogleChartCellVO> getC() {
		return c;
	}
	
	/**
	 * 
	 * @param cell
	 */
	public void addCell(GoogleChartCellVO cell) {
		c.add(cell);
	}
	
}

