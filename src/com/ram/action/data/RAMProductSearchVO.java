
package com.ram.action.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.ram.action.util.SecurityUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.AbstractRoleModule;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: ProductSearchVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Helper VO for managing Search and Query Params for
 * Product and Vision System Calls.
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3
 * @since Aug 2, 2017
 ****************************************************************************/
public class RAMProductSearchVO extends BSTableDataVO {

	private List<String> advFilter = new ArrayList<>();
	private String term = null;
	
	private Integer activeFlag = null;
	private int providerId;
	private int customerId;
	private int productId;
	private int layoutDepthNo;
	private boolean isSpd;

	/**
	 * @param req 
	 */
	public RAMProductSearchVO(ActionRequest req) {
		setData(req);
	}

	/**
	 * Helper method that parses the necessary req params off the RequestObject.
	 * @param req
	 */
	protected void setData(ActionRequest req) {
		super.setData(req);
		SBUserRole r = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);

		if (req.hasParameter("advFilter[]")) advFilter = Arrays.asList(req.getParameterValues("advFilter[]"));
		if (req.hasParameter("activeFlag")) activeFlag = Convert.formatInteger(req.getParameter("activeFlag"));
		
		term = StringUtil.checkVal(req.getParameter("search")).toLowerCase();
		isSpd = Convert.formatBoolean(req.getParameter("isSpd"));
		productId = Convert.formatInteger(req.getParameter("productId"));
		layoutDepthNo = Convert.formatInteger(req.getParameter("layoutDepthNo"));
		if(StringUtil.isEmpty(req.getParameter("customerId")) || r != null) {
			//Check for providerId, providers are only allowed to see products at their locations.
			providerId = SecurityUtil.isProviderRole(r.getRoleId()) ? Convert.formatInteger((String) r.getAttribute(AbstractRoleModule.ATTRIBUTE_KEY_1)) : 0;

			//Check for oem, oem are only allowed to see their products.
			customerId = SecurityUtil.isOEMRole(r.getRoleId()) ? Convert.formatInteger((String) r.getAttribute(AbstractRoleModule.ATTRIBUTE_KEY_1)) : Convert.formatInteger(req.getParameter("customerId"));
		} else {
			customerId = Convert.formatInteger(StringUtil.checkVal(req.getParameter("customerId")), 0);
		}
	}

	//Getters
	public List<String> getAdvFilter() {return advFilter;}
	public String getTerm() {return term;}
	public boolean isSpd() {return isSpd;}
	public Integer getActiveFlag() {return activeFlag;}
	public int getProviderId() {return providerId;}
	public int getCustomerId() {return customerId;}
	public int getProductId() {return productId;}
	public int getLayoutDepthNo() {return layoutDepthNo;}
	

	//Setters
	public void setAdvFilter(List<String> advFilter) {this.advFilter = advFilter;}
	public void setTerm(String term) {this.term = term;}
	public void setIsSpd(boolean isSpd) {this.isSpd = isSpd;}
	public void setActiveFlag(Integer activeOnly) {this.activeFlag = activeOnly;}
	public void setProviderId(int providerId) {this.providerId = providerId;}
	public void setCustomerId(int customerId) {this.customerId = customerId;}
	public void setProductId(int productId) {this.productId = productId;}
	public void setLayoutDepthNo(int layoutDepthNo) {this.layoutDepthNo = layoutDepthNo;}
	
}