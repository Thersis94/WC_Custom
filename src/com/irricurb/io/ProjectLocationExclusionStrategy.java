package com.irricurb.io;

import java.util.HashSet;
import java.util.Set;

import com.google.gson.ExclusionStrategy;
import com.google.gson.FieldAttributes;

/****************************************************************************
 * <b>Title</b>: ProjectLocationExclusionStrategy.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Excludes the unnecessary fields for the serialization process
 * this ensures the json attributes will be as lean as possible
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 2, 2018
 * @updates:
 ****************************************************************************/
public class ProjectLocationExclusionStrategy implements ExclusionStrategy {
	
	/**
	 * Creates the list of fields to be ignored by the serialization process
	 */
	private static final Set<String> IGNORE_FIELDS = new HashSet<String>() {
		private static final long serialVersionUID = 1L; {
			add("manualFlag");
			add("points");
			add("longitudeNumber");
			add("latitudeNumber");
			add("latitude");
			add("longitude");
			add("matchCode");
			add("addressPart");
			add("geocodeType");
			add("pl");
			add("cassValidated");
			add("singleLineAddress");
			add("options");
		}
	};
	
	/**
	 * 
	 */
	public ProjectLocationExclusionStrategy() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.google.gson.ExclusionStrategy#shouldSkipClass(java.lang.Class)
	 */
	@Override
	public boolean shouldSkipClass(Class<?> arg0) {
		return false;
	}

	/* (non-Javadoc)
	 * @see com.google.gson.ExclusionStrategy#shouldSkipField(com.google.gson.FieldAttributes)
	 */
	@Override
	public boolean shouldSkipField(FieldAttributes attr) {
		if (IGNORE_FIELDS.contains(attr.getName())) return true;
		return false;
	}

}
