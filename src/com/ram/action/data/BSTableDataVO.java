package com.ram.action.data;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> BSTableDataVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Aug 3, 2017
 ****************************************************************************/
public class BSTableDataVO {

	private int start = 0;
	private int limit = 0;
	private boolean isCount = false;
	private boolean isPaginated = false;
	private boolean isAjax = false;
	private boolean isDropDown = false;

	public BSTableDataVO() {
		
	}

	public BSTableDataVO(ActionRequest req) {
		this.setData(req);
	}
	protected void setData(ActionRequest req) {
		start = Convert.formatInteger(req.getParameter("offset") , 0);
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
