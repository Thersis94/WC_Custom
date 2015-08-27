/**
 *
 */
package com.ram.action.data;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: EXTJSDataVO.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Bean for managing common fields that are passed with
 * ExtJs Views.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jun 26, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class EXTJSDataVO {

	private int start = 0;
	private int limit = 0;
	private boolean isCount = false;
	private boolean isPaginated = false;
	private boolean isAjax = false;
	private boolean isDropDown = false;
	/**
	 * 
	 */
	public EXTJSDataVO() {
	}

	public EXTJSDataVO(SMTServletRequest req) {
		this();
		setData(req);
	}

	protected void setData(SMTServletRequest req) {
		start = Convert.formatInteger(req.getParameter("start"), 0);
		limit = Convert.formatInteger(req.getParameter("limit"), 25) + start;
		isPaginated = req.hasParameter("isPaginated") ? Convert.formatBoolean(req.getParameter("isPaginated")) : req.hasParameter("limit");
		isAjax = req.hasParameter("amid");
		isDropDown = Convert.formatBoolean(req.getParameter("isDropDown"));
	}

	//Getters
	public int getStart() {return start;}
	public int getLimit() {return limit;}
	public boolean isCount() {return isCount;}
	public boolean isPaginated() {return isPaginated;}
	public boolean isAjax() {return isAjax;}
	public boolean isDropDown() {return isDropDown;}

	//Setters
	public void setStart(int start) {this.start = start;}
	public void setLimit(int limit) {this.limit = limit;}
	public void setCount(boolean isCount) {this.isCount = isCount;}	
	public void setPaginated(boolean isPaginated) {this.isPaginated = isPaginated;}
	public void setAjax(boolean isAjax) {this.isAjax = isAjax;}
	public void setIsDropDown(boolean isDropDown) {this.isDropDown = isDropDown;}
}