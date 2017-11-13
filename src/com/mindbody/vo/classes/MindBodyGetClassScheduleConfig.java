package com.mindbody.vo.classes;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodyClassApi.ClassDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClassScheduleConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetClassSchedules Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 8, 2017
 ****************************************************************************/
public class MindBodyGetClassScheduleConfig extends MindBodyClassConfig {

	public static final String INCLUDE_CLASSES_FIELD = "ClassSchedules.Classes";
	private List<Integer> classScheduleIds;
	private List<Integer> sessionTypeIds;

	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyGetClassScheduleConfig(MindBodyCredentialVO source, MindBodyCredentialVO user) {
		super(ClassDocumentType.GET_CLASS_SCHEDULE, source, user);
		this.classScheduleIds = new ArrayList<>();
		this.sessionTypeIds = new ArrayList<>();
	}

	public MindBodyGetClassScheduleConfig(MindBodyCredentialVO source, MindBodyCredentialVO user, boolean includeClasses) {
		this(source, user);
		if(includeClasses) {
			super.addField(INCLUDE_CLASSES_FIELD);
		}
	}
	/**
	 * @return the classScheduleIds
	 */
	public List<Integer> getClassScheduleIds() {
		return classScheduleIds;
	}

	/**
	 * @return the sessionTypeIds
	 */
	public List<Integer> getSessionTypeIds() {
		return sessionTypeIds;
	}

	/**
	 * @param classScheduleIds the classScheduleIds to set.
	 */
	public void setClassScheduleIds(List<Integer> classScheduleIds) {
		this.classScheduleIds = classScheduleIds;
	}

	/**
	 * @param sessionTypeIds the sessionTypeIds to set.
	 */
	public void setSessionTypeIds(List<Integer> sessionTypeIds) {
		this.sessionTypeIds = sessionTypeIds;
	}
}
