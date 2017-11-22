package com.mindbody.vo.classes;

import com.mindbody.MindBodyClassApi.ClassDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyRemoveClientsFromClassesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages RemoveClientsFromClasses Endpoint unique
 * Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 8, 2017
 ****************************************************************************/
public class MindBodyRemoveClientsFromClassesConfig extends MindBodyClassConfig {

	private boolean isTest;
	private boolean sendEmail;
	private boolean allowLateCancel;

	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyRemoveClientsFromClassesConfig(MindBodyCredentialVO source) {
		super(ClassDocumentType.REMOVE_CLIENTS_FROM_CLASS, source, null);
	}

	/**
	 * Verify that there is at least one clientId and classId on the config.
	 */
	@Override
	public boolean isValid() {
		return super.isValid() && !getClientIds().isEmpty() && !getClassIds().isEmpty();
	}

	/**
	 * @return the isTest
	 */
	public boolean isTest() {
		return isTest;
	}
	/**
	 * @return the sendEmail
	 */
	public boolean isSendEmail() {
		return sendEmail;
	}
	/**
	 * @return the allowLateCancel
	 */
	public boolean isAllowLateCancel() {
		return allowLateCancel;
	}
	/**
	 * @param isTest the isTest to set.
	 */
	public void setTest(boolean isTest) {
		this.isTest = isTest;
	}
	/**
	 * @param sendEmail the sendEmail to set.
	 */
	public void setSendEmail(boolean sendEmail) {
		this.sendEmail = sendEmail;
	}
	/**
	 * @param allowLateCancel the allowLateCancel to set.
	 */
	public void setAllowLateCancel(boolean allowLateCancel) {
		this.allowLateCancel = allowLateCancel;
	}
}