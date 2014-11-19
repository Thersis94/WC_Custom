package com.codman.cu.tracking.vo;

import java.io.Serializable;

import com.codman.cu.tracking.AbstractTransAction;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: RequestSearchVO<p/>
 * <b>Description: simple container for holding the search parameters so we don't
 *    have to worry about passing them around on the request URLs. </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 29, 2010
 ****************************************************************************/
public class RequestSearchVO implements Serializable {
	private static final long serialVersionUID = 5358729392488250434L;
	
	protected String accountName = null;
	protected Integer statusId = null;
	protected String serialNoText = null;
	protected String repLastName = null;
	protected String repId = null;
	protected String repName = null;  //cosmetic equiv of repId, not used for search 
	protected String territoryId = null;
	protected int rpp = 25;
	protected int page = 1;
	protected String sort = null;
	private String SESSION_VAR = "CodmanCURequestSearchVO";
	
	public RequestSearchVO(SMTServletRequest req, String sessionVarNm) {
		this.SESSION_VAR = sessionVarNm;
		this.init(req);
		
	}
	public RequestSearchVO(SMTServletRequest req) {
		this.init(req);
	}
	
	private void init(SMTServletRequest req) {
		if (req.getParameter("sBtn") != null) { //indicates a search was performed
			accountName = StringUtil.checkVal(req.getParameter("sAccountName"), null);
			statusId = Convert.formatInteger(req.getParameter("sStatusId"), null);
			serialNoText = StringUtil.checkVal(req.getParameter("sSerialNoText"), null);
			repLastName = StringUtil.checkVal(req.getParameter("sRepLastName"), null);
			repId = StringUtil.checkVal(req.getParameter("sRepId"), null);
			repName = StringUtil.checkVal(req.getParameter("sRepName"), null);
			territoryId = StringUtil.checkVal(req.getParameter("sTerritoryId"), null);
			sort = StringUtil.checkVal(req.getParameter("sort"), null);
			rpp = Convert.formatInteger(req.getParameter("rpp"), Integer.valueOf(25)).intValue();
			
			req.getSession().setAttribute(SESSION_VAR, this);
			
		//the 'unfiltered' check gets passed from TransAction.  
		//This is a bug fix when approving pending transactions 
		//(they're no longer pending, so lookup query prior to email notifications fails) -JM 07-25-13
		} else if (req.getSession().getAttribute(SESSION_VAR) != null && !req.hasParameter("unfiltered")) {
			RequestSearchVO s = (RequestSearchVO) req.getSession().getAttribute(SESSION_VAR);
			accountName = s.getAccountName();
			statusId = s.getStatusId();
			serialNoText = s.getSerialNoText();
			repLastName = s.getRepLastName();
			repId = s.getRepId();
			territoryId = s.getTerritoryId();
			sort = s.getSort();
			rpp = s.getRpp();
			page = 1;
			//orderBy = s.getOrderBy();
		}

		page = Convert.formatInteger(req.getParameter("page"), Integer.valueOf(1)).intValue();
	}

	public String getSort() {
		return sort;
	}
	
	public int getRpp() {
		return rpp;
	}

	public String getAccountName() {
		return accountName;
	}

	public void setAccountName(String accountName) {
		this.accountName = accountName;
	}

	public Integer getStatusId() {
		return statusId;
	}

	public void setStatusId(Integer statusId) {
		this.statusId = statusId;
	}

	public String getSerialNoText() {
		return serialNoText;
	}

	public void setSerialNoText(String serialNoText) {
		this.serialNoText = serialNoText;
	}

	public String getRepLastName() {
		return repLastName;
	}

	public void setRepLastName(String repLastName) {
		this.repLastName = repLastName;
	}

	public String getTerritoryId() {
		return territoryId;
	}

	public void setTerritoryId(String territoryId) {
		this.territoryId = territoryId;
	}
	
	public String getCriteria() {
		StringBuffer val = new StringBuffer();
		if (accountName != null) val.append("Account Name like: ").append(accountName).append("<br/>");
		if (statusId != null) val.append("Request Status: ").append(AbstractTransAction.getStatusName(statusId)).append("<br/>");
		if (serialNoText != null) val.append("Unit Serial No.: ").append(serialNoText).append("<br/>");
		if (repLastName != null) val.append("Rep Last Name: ").append(repLastName).append("<br/>");
		if (territoryId != null) val.append("Territory: ").append(territoryId).append("<br/>");
		if (repId != null) val.append("Rep: ").append(repName).append("<br/>");
		
		return val.toString();
	}
	public String getRepId() {
		return repId;
	}
	public void setRepId(String repId) {
		this.repId = repId;
	}
	public String getRepName() {
		return repName;
	}
	public void setRepName(String repName) {
		this.repName = repName;
	}
	public int getPage() {
		return page;
	}
	public void setPage(int page) {
		this.page = page;
	}
	
	public int getStart() {
		if (page > 1) return (page-1) * rpp + 1;
		return 1;
	}
	public int getEnd() {
		return getStart() + rpp -1;
	}
	
}
