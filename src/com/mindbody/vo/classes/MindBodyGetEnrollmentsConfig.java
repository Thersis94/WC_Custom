package com.mindbody.vo.classes;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodyClassApi.ClassDocumentType;

/****************************************************************************
 * <b>Title:</b> MindBodyGetEnrollmentsConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 8, 2017
 ****************************************************************************/
public class MindBodyGetEnrollmentsConfig extends MindBodyClassConfig {

	public static final String INCLUDE_CLASSES_FIELD = "Enrollments.Classes";

	private List<Integer> classScheduleIds;
	private List<Integer> sessionTypeIds;
	private List<Integer> semesterIds;
	private List<Integer> courseIds;

	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyGetEnrollmentsConfig(String sourceName, String sourceKey, List<Integer> siteIds) {
		super(ClassDocumentType.GET_ENROLLMEMTS, sourceName, sourceKey, siteIds);
		this.classScheduleIds = new ArrayList<>();
		this.sessionTypeIds = new ArrayList<>();
		this.semesterIds = new ArrayList<>();
		this.courseIds = new ArrayList<>();
	}

	public MindBodyGetEnrollmentsConfig(String sourceName, String sourceKey, List<Integer> siteIds, boolean includeClasses) {
		this(sourceName, sourceKey, siteIds);
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
	 * @return the semesterIds
	 */
	public List<Integer> getSemesterIds() {
		return semesterIds;
	}

	/**
	 * @return the courseIds
	 */
	public List<Integer> getCourseIds() {
		return courseIds;
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

	/**
	 * @param semesterIds the semesterIds to set.
	 */
	public void setSemesterIds(List<Integer> semesterIds) {
		this.semesterIds = semesterIds;
	}

	/**
	 * @param courseIds the courseIds to set.
	 */
	public void setCourseIds(List<Integer> courseIds) {
		this.courseIds = courseIds;
	}
}