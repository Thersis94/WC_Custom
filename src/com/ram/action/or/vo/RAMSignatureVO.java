package com.ram.action.or.vo;

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
public class RAMSignatureVO {

	private String signatureId;
	private String profileId;
	private byte [] signature;

	public RAMSignatureVO() {
		
	}

	/**
	 * @return the signatureId
	 */
	public String getSignatureId() {
		return signatureId;
	}

	/**
	 * @return the profileId
	 */
	public String getProfileId() {
		return profileId;
	}

	/**
	 * @return the signature
	 */
	public byte[] getSignature() {
		return signature;
	}

	/**
	 * @param signatureId the signatureId to set.
	 */
	public void setSignatureId(String signatureId) {
		this.signatureId = signatureId;
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
	public void setSignature(byte[] signature) {
		this.signature = signature;
	}
}