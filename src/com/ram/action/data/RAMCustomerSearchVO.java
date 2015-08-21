/**
 *
 */
package com.ram.action.data;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: RAMCustomerSearchVO.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Helper method for Storing RAM Customer Search Param Values.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jun 25, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class RAMCustomerSearchVO extends EXTJSDataVO {

	private int customerId = 0;
	private boolean isAddCustomer = false;
	private String customerTypeId = null;
	private String excludeTypeId = null;
	private boolean kitsOnly = false;
	/**
	 * 
	 */
	public RAMCustomerSearchVO() {
	}

	public RAMCustomerSearchVO(SMTServletRequest req) {
		setData(req);
	}

	public void setData(SMTServletRequest req) {
		super.setData(req);
		customerId = Convert.formatInteger(req.getParameter("customerId"), 0);
		isAddCustomer = Convert.formatBoolean(req.getParameter("addCustomer"));
		customerTypeId = StringUtil.checkVal(req.getParameter("customerTypeId"));
		excludeTypeId = StringUtil.checkVal(req.getParameter("excludeTypeId"));
		kitsOnly = Convert.formatBoolean(req.getParameter("kitsOnly"));
	}

	//Getters
	public int getCustomerId() {return customerId;}
	public boolean isAddCustomer() {return isAddCustomer;}
	public String getCustomerTypeId() {return customerTypeId;}
	public String getExcludeTypeId() {return excludeTypeId;}
	public boolean isKitsOnly() {return kitsOnly;}

	//Setters
	public void setCustomerId(int customerId) {this.customerId = customerId;}
	public void setAddCustomer(boolean isAddCustomer) {this.isAddCustomer = isAddCustomer;}
	public void setCustomerTypeId(String customerTypeId) {this.customerTypeId = customerTypeId;}
	public void setExcludeTypeId(String excludeTypeId) {this.excludeTypeId = excludeTypeId;}
	public void setKitsOnly(boolean kitsOnly) {this.kitsOnly = kitsOnly;}
}