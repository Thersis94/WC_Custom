package com.mindbody.vo.classes;

import java.util.List;

import com.siliconmtn.gis.GeocodeLocation;

/****************************************************************************
 * <b>Title:</b> MBLocationVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Mindbody Location Data.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MBLocationVO extends GeocodeLocation {

	private static final long serialVersionUID = 1L;
	private int siteId;
	private int id;
	private int facilitySquareFeet;
	private int treatmentRooms;
	private String businessDescription;
	private String phone;
	private String phoneExtension;
	private String name;
	private boolean hasClasses;
	private float tax1;
	private float tax2;
	private float tax3;
	private float tax4;
	private float tax5;
	private List<String> additionalImageUrls;

	public MBLocationVO() {
		//Default Constructor
	}

	/**
	 * @return the siteId
	 */
	public int getSiteId() {
		return siteId;
	}

	/**
	 * @return the id
	 */
	public int getId() {
		return id;
	}

	/**
	 * @return the facilitySquareFeet
	 */
	public int getFacilitySquareFeet() {
		return facilitySquareFeet;
	}

	/**
	 * @return the treatmentRooms
	 */
	public int getTreatmentRooms() {
		return treatmentRooms;
	}

	/**
	 * @return the businessDescription
	 */
	public String getBusinessDescription() {
		return businessDescription;
	}

	/**
	 * @return the phone
	 */
	public String getPhone() {
		return phone;
	}

	/**
	 * @return the phoneExtension
	 */
	public String getPhoneExtension() {
		return phoneExtension;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @return the hasClasses
	 */
	public boolean isHasClasses() {
		return hasClasses;
	}

	/**
	 * @return the tax1
	 */
	public float getTax1() {
		return tax1;
	}

	/**
	 * @return the tax2
	 */
	public float getTax2() {
		return tax2;
	}

	/**
	 * @return the tax3
	 */
	public float getTax3() {
		return tax3;
	}

	/**
	 * @return the tax4
	 */
	public float getTax4() {
		return tax4;
	}

	/**
	 * @return the tax5
	 */
	public float getTax5() {
		return tax5;
	}

	/**
	 * @return the additionalImageUrls
	 */
	public List<String> getAdditionalImageUrls() {
		return additionalImageUrls;
	}

	/**
	 * @param siteId the siteId to set.
	 */
	public void setSiteId(int siteId) {
		this.siteId = siteId;
	}

	/**
	 * @param id the id to set.
	 */
	public void setId(int id) {
		this.id = id;
	}

	/**
	 * @param facilitySquareFeet the facilitySquareFeet to set.
	 */
	public void setFacilitySquareFeet(int facilitySquareFeet) {
		this.facilitySquareFeet = facilitySquareFeet;
	}

	/**
	 * @param treatmentRooms the treatmentRooms to set.
	 */
	public void setTreatmentRooms(int treatmentRooms) {
		this.treatmentRooms = treatmentRooms;
	}

	/**
	 * @param businessDescription the businessDescription to set.
	 */
	public void setBusinessDescription(String businessDescription) {
		this.businessDescription = businessDescription;
	}

	/**
	 * @param phone the phone to set.
	 */
	public void setPhone(String phone) {
		this.phone = phone;
	}

	/**
	 * @param phoneExtension the phoneExtension to set.
	 */
	public void setPhoneExtension(String phoneExtension) {
		this.phoneExtension = phoneExtension;
	}

	/**
	 * @param name the name to set.
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @param hasClasses the hasClasses to set.
	 */
	public void setHasClasses(boolean hasClasses) {
		this.hasClasses = hasClasses;
	}

	/**
	 * @param tax1 the tax1 to set.
	 */
	public void setTax1(float tax1) {
		this.tax1 = tax1;
	}

	/**
	 * @param tax2 the tax2 to set.
	 */
	public void setTax2(float tax2) {
		this.tax2 = tax2;
	}

	/**
	 * @param tax3 the tax3 to set.
	 */
	public void setTax3(float tax3) {
		this.tax3 = tax3;
	}

	/**
	 * @param tax4 the tax4 to set.
	 */
	public void setTax4(float tax4) {
		this.tax4 = tax4;
	}

	/**
	 * @param tax5 the tax5 to set.
	 */
	public void setTax5(float tax5) {
		this.tax5 = tax5;
	}

	/**
	 * @param additionalImageUrls the additionalImageUrls to set.
	 */
	public void setAdditionalImageUrls(List<String> additionalImageUrls) {
		this.additionalImageUrls = additionalImageUrls;
	}
}