package com.ansmed.sb.report;

import java.sql.ResultSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/****************************************************************************
 * <b>Title</b>:SummaryVO.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Mar 13, 2008
 ****************************************************************************/
public class SummaryVO extends AbstractSiteBuilderVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Map<String, AreaVO> areas = new LinkedHashMap<String, AreaVO>();
	private Map<String, Integer> totals = new LinkedHashMap<String, Integer>();
	
	/**
	 * 
	 */
	public SummaryVO() {
		
	}

	/**
	 * 
	 * @param rs
	 */
	public SummaryVO(ResultSet rs) {
		super();
		this.setData(rs);
	}
	
	/**
	 * Assigns the overall total and individual totals for the area
	 * @param type
	 * @param element
	 */
	public void addElement(String type, int element) {
		Integer val = Convert.formatInteger(totals.get(type));
		
		// Update the totals
		if (totals.containsKey(type)) {
			val += element;
			totals.put(type, val);
		} else {
			totals.put(type, element);
		}
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		String areaName = db.getStringVal("area_nm", rs);
		
		// Summary totals for each type
		this.addElement(db.getStringVal("type_nm", rs), db.getIntVal("total", rs));
		
		// Get the area info.  
		AreaVO vo = null;
		if (areas.containsKey(areaName)) {
		 	vo = areas.get(areaName);
		 	vo.setData(rs);
		} else {
			vo = new AreaVO(rs);
			areas.put(areaName, vo);
		}
	}
	
	/**
	 * 
	 * @return
	 */
	public int getSummaryTotal() {
		int tot = 0;
		
		Set<String> s = totals.keySet();
		for(Iterator<String> iter = s.iterator(); iter.hasNext(); ) {
			
			tot += totals.get(iter.next());
		}
		
		return tot;
	}

	/**
	 * @return the areas
	 */
	public Map<String, AreaVO> getAreas() {
		return areas;
	}

	/**
	 * @param areas the areas to set
	 */
	public void setAreas(Map<String, AreaVO> areas) {
		this.areas = areas;
	}

	/**
	 * @return the totals
	 */
	public Map<String, Integer> getTotals() {
		return totals;
	}

	/**
	 * @param totals the totals to set
	 */
	public void setTotals(Map<String, Integer> totals) {
		this.totals = totals;
	}
}
