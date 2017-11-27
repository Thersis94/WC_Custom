package com.mindbody.vo.classes;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodyClassApi.ClassDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

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
	private List<Long> courseIds;

	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyGetCoursesConfig(MindBodyCredentialVO source) {
		super(ClassDocumentType.GET_COURSES, source, null);
		this.semesterIds = new ArrayList<>();
		this.courseIds = new ArrayList<>();
	}

	public List<Integer> getSemesterIds() {
		return semesterIds;
	}

	public List<Long> getCourseIds() {
		return courseIds;
	}

	public void setSemesterIds(List<Integer> semesterIds) {
		this.semesterIds = semesterIds;
	}

	public void setCourseIds(List<Long> courseIds) {
		this.courseIds = courseIds;
	}
}