package com.mindbody.vo.clients;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

import opennlp.tools.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClientServicesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetClientServices Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 13, 2017
 ****************************************************************************/
public class MindBodyGetClientServicesConfig extends MindBodyClientConfig {

	private Integer classId;
	private List<Integer> programIds;
	private List<Integer> sessionTypeIds;
	private Integer visitCount;
	private boolean showActiveOnly;

	/**
	 * @param type
	 * @param source
	 * @param user
	 */
	public MindBodyGetClientServicesConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClientDocumentType.GET_CLIENT_SERVICES, source, user);
		this.programIds = new ArrayList<>();
		this.sessionTypeIds = new ArrayList<>();
	}

	@Override
	public boolean isValid() {
		return super.isValid() && !StringUtil.isEmpty(getClientId());
	}

	/**
	 * @return
	 */
	public String getClientId() {
		return getClientIds().get(0);
	}

	/**
	 * @return
	 */
	public Integer getClassId() {
		return classId;
	}

	public void setClassId(Integer classId) {
		this.classId = classId;
	}

	/**
	 * @return
	 */
	public List<Integer> getProgramIds() {
		return programIds;
	}

	public void setProgramIds(List<Integer> programIds) {
		this.programIds = programIds;
	}

	public void addProgramId(Integer programId) {
		this.programIds.add(programId);
	}

	/**
	 * @return
	 */
	public List<Integer> getSessionTypeIds() {
		return sessionTypeIds;
	}

	public void addSessionTypeId(Integer sessionTypeId) {
		this.sessionTypeIds.add(sessionTypeId);
	}
	/**
	 * @return
	 */
	public Integer getVisitCount() {
		return visitCount;
	}

	/**
	 * @return
	 */
	public boolean isShowActiveOnly() {
		return showActiveOnly;
	}

	/**
	 * @param sessionTypeIds the sessionTypeIds to set.
	 */
	public void setSessionTypeIds(List<Integer> sessionTypeIds) {
		this.sessionTypeIds = sessionTypeIds;
	}

	/**
	 * @param visitCount the visitCount to set.
	 */
	public void setVisitCount(Integer visitCount) {
		this.visitCount = visitCount;
	}

	/**
	 * @param showActiveOnly the showActiveOnly to set.
	 */
	public void setShowActiveOnly(boolean showActiveOnly) {
		this.showActiveOnly = showActiveOnly;
	}

	
}