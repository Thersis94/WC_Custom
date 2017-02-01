/**
 * 
 */
package com.codman.cu.tracking.vo;

import java.io.Serializable;

import com.codman.cu.tracking.UnitAction;
import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title</b>: UnitSearchVO<p/>
 * <b>Description: simple container for holding the search parameters so we don't
 *    have to worry about passing them around on the request URLs. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 30, 2010
 ****************************************************************************/
public class UnitSearchVO extends RequestSearchVO implements Serializable {
	private static final long serialVersionUID = 5358729392488250434L;
	
	public static final String SESSION_VAR = "CodmanCUUnitSearchVO";
	
	public UnitSearchVO(ActionRequest req, String prodCd) {
		super(req, SESSION_VAR + prodCd);
	}
	
	public String getCriteria() {
		StringBuffer val = new StringBuffer();
		if (accountName != null) val.append("Account Name like: ").append(accountName).append("<br/>");
		if (statusId != null) val.append("Unit Status: ").append(UnitAction.getStatusName(statusId)).append("<br/>");
		if (serialNoText != null) val.append("Unit Serial No.: ").append(serialNoText).append("<br/>");
		if (repLastName != null) val.append("Rep Last Name: ").append(repLastName).append("<br/>");
		
		return val.toString();
	}
}
