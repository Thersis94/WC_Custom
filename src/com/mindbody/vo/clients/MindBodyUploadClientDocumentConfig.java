package com.mindbody.vo.clients;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

import opennlp.tools.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyUploadClientDocumentConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages UploadClientDocument Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyUploadClientDocumentConfig extends MindBodyClientConfig {

	private String fileName;
	private byte [] bytes;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyUploadClientDocumentConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.UPLOAD_CLIENT_DOCUMENT, source, user);
	}

	@Override
	public boolean isValid() {
		return super.isValid() && !getClientIds().isEmpty() && !StringUtil.isEmpty(fileName) && bytes != null;
	}

	/**
	 * @return the fileName
	 */
	public String getFileName() {
		return fileName;
	}

	/**
	 * @return the bytes
	 */
	public byte[] getBytes() {
		return bytes;
	}

	/**
	 * @param fileName the fileName to set.
	 */
	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	/**
	 * @param bytes the bytes to set.
	 */
	public void setBytes(byte[] bytes) {
		this.bytes = bytes;
	}

	public String getClientId() {
		return getClientIds().get(0);
	}
}