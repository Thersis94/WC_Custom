package com.mindbody.vo.classes;

import java.util.ArrayList;
import java.util.List;

import com.mindbody.MindBodyClassApi.ClassDocumentType;
import com.mindbody.vo.MindBodyCredentialVO;

/****************************************************************************
 * <b>Title:</b> MindBodyGetClassDescConfig.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages GetClassDescriptions Endpoint unique Configuration.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 8, 2017
 ****************************************************************************/
public class MindBodyGetClassDescConfig extends MindBodyClassConfig {

	private List<Integer> classDescriptionIds;


	/**
	 * @param type
	 * @param sourceName
	 * @param sourceKey
	 * @param siteIds
	 */
	public MindBodyGetClassDescConfig(MindBodyCredentialVO source) {
		super(ClassDocumentType.GET_CLASS_DESC, source, null);
		classDescriptionIds = new ArrayList<>();
	}


	/**
	 * @return the classDescriptionIds
	 */
	public List<Integer> getClassDescriptionIds() {
		return classDescriptionIds;
	}


	/**
	 * @param classDescriptionIds the classDescriptionIds to set.
	 */
	public void setClassDescriptionIds(List<Integer> classDescriptionIds) {
		this.classDescriptionIds = classDescriptionIds;
	}
}