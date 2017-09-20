package com.depuysynthes.action;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

/***************************************************************************
 * <b>Title:</b> MIRSubmissionVO.java<br/>
 * <b>Description:</b> POJO data structure to use for generating the email.  
 * Populated from the ActionRequest using Annotations
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2017<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Sep 7, 2017
 ***************************************************************************/
public class MIRSubmissionVO extends UserDataVO {

	private static final long serialVersionUID = -8316947595121339926L;

	private String region;
	private String subregion;
	private String requestType;
	private String consentFlg;
	private String hcpType;
	private String hcpTypeOther;
	private String hcpSpecialty;
	private String hcpTitle;
	private String hcpInstitution;
	private String responseType;
	private String responseTypeOther;
	private String jjRep;
	private String jjRepEmail;
	private String[] productCompany;
	private String productCompanyOther;
	private String[] products;
	private String productOther;
	private String partNumber;
	private String question;

	public MIRSubmissionVO(ActionRequest req) {
		super(req);
		super.setMobilePhone(req.getParameter("fax")); //a little stuffing to avoid extra code
		setRegion(req.getParameter("region"));
		setSubregion(req.getParameter("subregion"));
		setRequestType(req.getParameter("requestType"));
		setConsentFlg(req.getParameter("consent"));
		setHcpType(req.getParameter("hcpType"));
		setHcpTypeOther(req.getParameter("hcpTypeOther"));
		setHcpSpecialty(req.getParameter("hcpSpecialty"));
		setHcpTitle(req.getParameter("hcpTitle"));
		setHcpInstitution(req.getParameter("hcpInstitution"));
		setResponseType(req.getParameter("responseType"));
		setResponseTypeOther(req.getParameter("responseTypeOther"));
		setJjRep(req.getParameter("jjRep"));
		setJjRepEmail(req.getParameter("jjRepEmail"));
		setProductCompany(req.getParameterValues("productCompany"));
		setProductCompanyOther(req.getParameter("productCompanyOther"));
		setProducts(req.getParameterValues("products"));
		setProductOther(req.getParameter("productOther"));
		setPartNumber(req.getParameter("partNumber"));
		setQuestion(req.getParameter("question"));
	}

	@Override
	public String toString() {
		return StringUtil.getToString(this, false, 0, "|");
	}

	public String getRequestType() {
		return requestType;
	}

	public String getHcpType() {
		return hcpType;
	}

	public String getHcpTypeOther() {
		return hcpTypeOther;
	}

	public String getHcpSpecialty() {
		return hcpSpecialty;
	}

	public String getHcpTitle() {
		return hcpTitle;
	}

	public String getHcpInstitution() {
		return hcpInstitution;
	}

	public String getResponseType() {
		return responseType;
	}

	public String getResponseTypeOther() {
		return responseTypeOther;
	}

	public String getJjRep() {
		return jjRep;
	}

	public String[] getProductCompany() {
		return productCompany;
	}

	public String[] getProducts() {
		return products;
	}

	public String getProductOther() {
		return productOther;
	}

	public String getPartNumber() {
		return partNumber;
	}

	public String getQuestion() {
		return question;
	}

	public void setRequestType(String requestType) {
		this.requestType = requestType;
	}

	public void setHcpType(String hcpType) {
		this.hcpType = hcpType;
	}

	public void setHcpTypeOther(String hcpTypeOther) {
		this.hcpTypeOther = hcpTypeOther;
	}

	public void setHcpSpecialty(String hcpSpecialty) {
		this.hcpSpecialty = hcpSpecialty;
	}

	public void setHcpTitle(String hcpTitle) {
		this.hcpTitle = hcpTitle;
	}

	public void setHcpInstitution(String hcpInstitution) {
		this.hcpInstitution = hcpInstitution;
	}

	public void setResponseType(String responseType) {
		this.responseType = responseType;
	}

	public void setResponseTypeOther(String responseTypeOther) {
		this.responseTypeOther = responseTypeOther;
	}

	public void setJjRep(String jjRep) {
		this.jjRep = jjRep;
	}

	public void setProductCompany(String[] productCompany) {
		this.productCompany = productCompany;
	}

	public void setProducts(String[] products) {
		this.products = products;
	}

	public void setProductOther(String productOther) {
		this.productOther = productOther;
	}

	public void setPartNumber(String partNumber) {
		this.partNumber = partNumber;
	}

	public void setQuestion(String question) {
		this.question = question;
	}

	public String getProductCompanyOther() {
		return productCompanyOther;
	}

	public void setProductCompanyOther(String productCompanyOther) {
		this.productCompanyOther = productCompanyOther;
	}

	/**
	 * Combines the array values into a flattened String we can email
	 * @return
	 */
	public String getProductCompanies() {
		StringBuilder sb = new StringBuilder(100);
		if (productCompany != null && productCompany.length > 0) {
			for (String s : productCompany) {
				if ("other".equals(s)) continue; //this is just a placeholder on the form
				if (sb.length() > 0) sb.append("<br/>");
				sb.append(s);
			}
		}
		return sb.toString();
	}

	/**
	 * Combines the array values into a flattened String we can email
	 * @return
	 */
	public String getMergedProducts() {
		StringBuilder sb = new StringBuilder(100);
		if (products != null && products.length > 0) {
			for (String s : products) {
				if (sb.length() > 0) sb.append("<br/>");
				sb.append(s);
			}
		}
		return sb.toString();
	}

	public String getRegion() {
		return region;
	}

	public void setRegion(String region) {
		this.region = region;
	}

	public String getSubregion() {
		return subregion;
	}

	public void setSubregion(String subregion) {
		this.subregion = subregion;
	}

	public String getConsentFlg() {
		if ("1".equals(consentFlg)) return "Submitted by HCP";
		if ("2".equals(consentFlg)) return "Submitted by Other";
		return "Consent not given";
	}

	public void setConsentFlg(String consentFlg) {
		this.consentFlg = consentFlg;
	}

	public String getJjRepEmail() {
		return jjRepEmail;
	}

	public void setJjRepEmail(String jjRepEmail) {
		this.jjRepEmail = jjRepEmail;
	}
}