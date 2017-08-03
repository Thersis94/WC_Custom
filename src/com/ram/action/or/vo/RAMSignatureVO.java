package com.ram.action.or.vo;

import java.io.IOException;
import java.io.Serializable;
import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> RAMSignatureVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> VO Manages RAM Signature Information.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 28, 2017
 ****************************************************************************/
@Table(name="RAM_CASE_SIGNATURE")
public class RAMSignatureVO implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	public enum SignatureType {
		PROVIDER ("Hospital Rep"), 
		SALES_REP("OEM Sales Rep"), 
		SURGEON("Surgeon");				
		private String name;		

		private SignatureType(String name) {			
			this.name = name;		}				
		public String getName() { 
			return name; 
		}	
	}

	private String signatureId;
	private String caseId;
	private String profileId;
	private String signatureTxt;
	private String firstNm;
	private String lastNm;
	private SignatureType type;
	private Date createDt;

	public RAMSignatureVO() {
		super();
	}

	/**
	 * @param req
	 * @throws IOException 
	 */
	public RAMSignatureVO(ActionRequest req) throws IOException {
		this();
		setData(req);
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}

	/**
	 * 
	 * @param req
	 * @throws IOException
	 */
	public void setData(ActionRequest req) throws IOException {
		signatureId = req.getParameter("signatureId");
		caseId = req.getParameter("caseId");
		profileId = req.getParameter("profileId");
		setSignatureTypeTxt(req.getParameter("signatureType"));
		signatureTxt = req.getParameter("signatureTxt");
	}

	/**
	 * @return the signatureId
	 */
	@Column(name="case_signature_id", isPrimaryKey=true)
	public String getSignatureId() {
		return signatureId;
	}

	/**
	 * @return the caseId
	 */
	@Column(name="case_id")
	public String getCaseId() {
		return caseId;
	}

	/**
	 * @return the profileId
	 */
	@Column(name="profile_id")
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @return the signature
	 */
	@Column(name="signature_txt")
	public String getSignatureTxt() {
		return signatureTxt;
	}

	/**
	 * @return the type
	 */
	public SignatureType getSignatureType() {
		return type;
	}

	/**
	 * @return the type
	 */
	@Column(name="signature_type_id")
	public String getSignatureTypeTxt() {
		return type.toString();
	}

	/**
	 * @return the createDt
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param signatureId the signatureId to set.
	 */
	public void setSignatureId(String signatureId) {
		this.signatureId = signatureId;
	}

	/**
	 * @param caseId the caseId to set.
	 */
	public void setCaseId(String caseId) {
		this.caseId = caseId;
	}

	/**
	 * @param profileId the profileId to set.
	 */
	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	/**
	 * @param signature the signature to set.
	 */
	public void setSignatureTxt(String signatureTxt) {
		this.signatureTxt = signatureTxt;
	}

	/**
	 * @param type the signature type to set.
	 */
	public void setSignatureType(SignatureType type) {
		this.type = type;
	}

	/**
	 * @param type the signature type to set.
	 */
	public void setSignatureTypeTxt(String type) {
		try {
			this.type = SignatureType.valueOf(type);
		} catch(Exception e) {
			//Ignore Exception.
		}
	}

	/**
	 * @param createDt the createDt to set.
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	/**
	 * @return the firstNm
	 */
	public String getFirstNm() {
		return firstNm;
	}

	/**
	 * @param firstNm the firstNm to set
	 */
	public void setFirstNm(String firstNm) {
		this.firstNm = firstNm;
	}

	/**
	 * @return the lastNm
	 */
	public String getLastNm() {
		return lastNm;
	}

	/**
	 * @param lastNm the lastNm to set
	 */
	public void setLastNm(String lastNm) {
		this.lastNm = lastNm;
	}
	
	/**
	 * @return
	 */
	public String getFullName() {
		return firstNm + " " + lastNm;
	}
}
