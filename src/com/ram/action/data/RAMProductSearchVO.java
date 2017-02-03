/**
 *
 */
package com.ram.action.data;

import com.ram.action.user.RamUserAction;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: ProductSearchVO.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Helper VO for managing Search and Query Params for
 * Product and Vision System Calls.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jun 18, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class RAMProductSearchVO extends EXTJSDataVO {

	private int advFilter = 0;
	private String term = null;
	
	private boolean activeOnly = false;
	private int providerId = 0;
	private int customerId = 0;
	private int productId = 0;
	private int layoutDepthNo = 0;

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

		advFilter = Convert.formatInteger(req.getParameter("advFilter"), -1);
		term = StringUtil.checkVal(req.getParameter("term"));
		
		activeOnly = Convert.formatBoolean(req.getParameter("activeFlag"));
		productId = Convert.formatInteger(req.getParameter("productId"));
		layoutDepthNo = Convert.formatInteger(req.getParameter("layoutDepthNo"));
		if(r != null) {
			//Check for providerId, providers are only allowed to see products at their locations.
			providerId = r.getRoleLevel() == RamUserAction.ROLE_LEVEL_PROVIDER ? Convert.formatInteger((String) r.getAttribute("roleAttributeKey_1")) : 0;

			//Check for oem, oem are only allowed to see their products.
			customerId = r.getRoleLevel() == RamUserAction.ROLE_LEVEL_OEM ? Convert.formatInteger((String) r.getAttribute("roleAttributeKey_1")) : Convert.formatInteger(req.getParameter("customerId"));
		}
	}

	//Getters
	public int getAdvFilter() {return advFilter;}
	public String getTerm() {return term;}
	
	public boolean isActiveOnly() {return activeOnly;}
	public int getProviderId() {return providerId;}
	public int getCustomerId() {return customerId;}
	public int getProductId() {return productId;}
	public int getLayoutDepthNo() {return layoutDepthNo;}
	

	//Setters
	public void setAdvFilter(int advFilter) {this.advFilter = advFilter;}
	public void setTerm(String term) {this.term = term;}
	
	public void setActiveOnly(boolean activeOnly) {this.activeOnly = activeOnly;}
	public void setProviderId(int providerId) {this.providerId = providerId;}
	public void setCustomerId(int customerId) {this.customerId = customerId;}
	public void setProductId(int productId) {this.productId = productId;}
	public void setLayoutDepthNo(int layoutDepthNo) {this.layoutDepthNo = layoutDepthNo;}
	
}