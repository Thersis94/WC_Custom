package com.depuysynthes.pa;

import com.siliconmtn.annotations.SolrField;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;
/****************************************************************************
 * <b>Title</b>: SolrStoryVO.java<p/>
 * <b>Description: Used to store information specific to the ambassador stories
 * for the solr index.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 16, 2015
 ****************************************************************************/

public class SolrStoryVO extends SolrDocumentVO {
	public static final String INDEX_TYPE = "PATIENT_AMBASSADOR";
	
	private String otherHobby;
	
	public SolrStoryVO() {
		super(INDEX_TYPE);
	}

	@SolrField(name="category_other_s")
	public String getOtherHobbies() {
		return otherHobby;
	}

	public void setOtherHobbies(String otherHobby) {
		this.otherHobby = otherHobby;
	}

	@SolrField(name="multijoint_s")
	public String getMultiJoint() {
		if (this.getHierarchies().size() > 1) {
			return "multijoint";
		} else {
			return "single";
		}
	}

}
