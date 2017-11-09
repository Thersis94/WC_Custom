package com.mindbody.vo.classes;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodyClassApi.ClassDocumentType;

/****************************************************************************
 * <b>Title:</b> MindBodyGetCoursesConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetCourses Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 8, 2017
 ****************************************************************************/
public class MindBodyGetCoursesConfig extends MindBodyClassConfig {

	private List<Integer> semesterIds;

	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyGetCoursesConfig(String sourceName, String sourceKey, List<Integer> siteIds) {
		super(ClassDocumentType.GET_COURSES, sourceName, sourceKey, siteIds);
		this.semesterIds = new ArrayList<>();
	}

	public List<Integer> getSemesterIds() {
		return semesterIds;
	}

	public void setSemesterIds(List<Integer> semesterIds) {
		this.semesterIds = semesterIds;
	}
}