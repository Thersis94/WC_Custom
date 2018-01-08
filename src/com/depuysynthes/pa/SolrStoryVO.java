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
	private String surgeonName;
	private String hospitalName;
	private String incisionName;
	private String implantName;

	public SolrStoryVO() {
		super(INDEX_TYPE);
	}

	@SolrField(name="category_other_t")
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

	@SolrField(name="surgeon_name_s")
	public String getSurgeonName() {
		return surgeonName;
	}

	public void setSurgeonName(String surgeonName) {
		this.surgeonName = surgeonName;
	}

	/**
	 * @return the hospitalName
	 */
	@SolrField(name="hospital_name_s")
	public String getHospitalName() {
		return hospitalName;
	}

	/**
	 * @param hospitalName the hospitalName to set
	 */
	public void setHospitalName(String hospitalName) {
		this.hospitalName = hospitalName;
	}

	/**
	 * @return the incisionName
	 */
	@SolrField(name="incision_name_s")
	public String getIncisionName() {
		return incisionName;
	}

	/**
	 * @param incisionName the incisionName to set
	 */
	public void setIncisionName(String incisionName) {
		this.incisionName = incisionName;
	}

	/**
	 * @return the implantName
	 */
	@SolrField(name="implant_name_s")
	public String getImplantName() {
		return implantName;
	}

	/**
	 * @param implantName the implantName to set
	 */
	public void setImplantName(String implantName) {
		this.implantName = implantName;
	}
	
}