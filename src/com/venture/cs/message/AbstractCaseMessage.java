package com.venture.cs.message;

// JDK 7
import java.util.List;



import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
// WC_Custom
import com.venture.cs.action.vo.VehicleVO;

/****************************************************************************
 *<b>Title</b>: AbstractCaseMessage<p/>
 * Abstract case message class.
 *Copyright: Copyright (c) 2014<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar 12, 2014
 * Changes:
 * Mar 12, 2014: DBargerhuff: created class
 ****************************************************************************/

public abstract class AbstractCaseMessage {

	/**
	 * Request object for messages that need
	 * to make use of request or session parameters/attributes
	 */
	protected SMTServletRequest req;
	
	/**
	 * List of VehicleVO
	 */
	protected List<VehicleVO> vehicles;

	/**
	 * String providing link to case
	 */
	protected String caseUrl;
	
	/**
	 * Abstract method for building message body as HTML
	 * @return
	 */
	public abstract String getMessageBodyHTML();
	
	/**
	 * Abstract method for building message body as text
	 * @return
	 */
	public abstract String getMessageBodyText();
	
	/**
	 * Abstract method for message subject
	 * @return
	 */
	public abstract String getMessageSubject();
	
	/**
	 * @param req the req to set
	 */
	public void setReq(SMTServletRequest req) {
		this.req = req;
	}

	/**
	 * @param vehicles the vehicles to set
	 */
	public void setVehicles(List<VehicleVO> vehicles) {
		this.vehicles = vehicles;
	}

	/**
	 * @param caseUrl the caseUrl to set
	 */
	public void setCaseUrl(String caseUrl) {
		this.caseUrl = caseUrl;
	}
	
	/**
	 * Utility method to build email vehicle header row if certain data exists.
	 * @param v
	 * @return
	 */
	protected StringBuilder addVehicleHeaderRow(VehicleVO v) {
		StringBuilder vInfo = new StringBuilder();
		StringBuilder body = new StringBuilder();
		if (v != null) {
			String tmp = StringUtil.checkVal(v.getYear());
			if (tmp.length() > 0) {
				vInfo.append(tmp).append(" ");
			}
			tmp = StringUtil.checkVal(v.getMake());
			if (tmp.length() > 0) {
				vInfo.append(tmp).append(" ");
			}
			tmp = StringUtil.checkVal(v.getModel());
			if (tmp.length() > 0) {
				vInfo.append(tmp);
			}
		}
		if (vInfo.length() > 0) {
			body.append("<tr style='background-color: #14548F;'>");
			body.append("<td colspan='3' style='color: #fff; text-align: center;'>");
			body.append(vInfo).append("</td><tr>");
		}
		return body;
	}
	
}
