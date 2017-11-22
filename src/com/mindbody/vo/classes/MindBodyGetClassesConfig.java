package com.mindbody.vo.classes;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mindbody.MindBodyClassApi.ClassDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClassesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetClasses Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 8, 2017
 ****************************************************************************/
public class MindBodyGetClassesConfig extends MindBodyClassConfig {

	private List<Integer> classDescriptionIds;
	private List<Integer> sessionTypeIds;
	private List<Integer> semesterIds;
	private boolean hideCanceledClasses;
	private boolean useSchedulingWindow;
	private Date modifiedDt;

	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyGetClassesConfig(MindBodyCredentialVO source) {
		super(ClassDocumentType.GET_CLASSES, source, null);
		this.classDescriptionIds = new ArrayList<>();
		this.sessionTypeIds = new ArrayList<>();
		this.semesterIds = new ArrayList<>();
	}

	/**
	 * @return the classDescriptionIds
	 */
	public List<Integer> getClassDescriptionIds() {
		return classDescriptionIds;
	}

	/**
	 * @return the sessionTypeIds
	 */
	public List<Integer> getSessionTypeIds() {
		return sessionTypeIds;
	}

	/**
	 * @return the semesterIds
	 */
	public List<Integer> getSemesterIds() {
		return semesterIds;
	}

	/**
	 * @return the hideCanceledClasses
	 */
	public boolean isHideCanceledClasses() {
		return hideCanceledClasses;
	}

	/**
	 * @return the useSchedulingWindow
	 */
	public boolean isUseSchedulingWindow() {
		return useSchedulingWindow;
	}

	/**
	 * @return the modifiedDt
	 */
	public Date getModifiedDt() {
		return modifiedDt;
	}

	/**
	 * @param classDescriptionIds the classDescriptionIds to set.
	 */
	public void setClassDescriptionIds(List<Integer> classDescriptionIds) {
		this.classDescriptionIds = classDescriptionIds;
	}

	/**
	 * @param sessionTypeIds the sessionTypeIds to set.
	 */
	public void setSessionTypeIds(List<Integer> sessionTypeIds) {
		this.sessionTypeIds = sessionTypeIds;
	}

	/**
	 * @param semesterIds the semesterIds to set.
	 */
	public void setSemesterIds(List<Integer> semesterIds) {
		this.semesterIds = semesterIds;
	}

	/**
	 * @param hideCanceledClasses the hideCanceledClasses to set.
	 */
	public void setHideCanceledClasses(boolean hideCanceledClasses) {
		this.hideCanceledClasses = hideCanceledClasses;
	}

	/**
	 * @param useSchedulingWindow the useSchedulingWindow to set.
	 */
	public void setUseSchedulingWindow(boolean useSchedulingWindow) {
		this.useSchedulingWindow = useSchedulingWindow;
	}

	/**
	 * @param modifiedDt the modifiedDt to set.
	 */
	public void setModifiedDt(Date modifiedDt) {
		this.modifiedDt = modifiedDt;
	}
}