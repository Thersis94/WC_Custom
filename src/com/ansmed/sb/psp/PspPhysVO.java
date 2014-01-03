package com.ansmed.sb.psp;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>:PspPhysVO.java<p/>
 * <b>Description</b>: Value object for a PSP physician.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 20098<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Oct 2, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PspPhysVO {
	
	private String name;
	private String profile;
	private String photo;
	private String photoCustom;
	private String cv;
	private String cvPath;
	
	/**
	 * default constructor
	 */
	public PspPhysVO() {
		
	}
	
	public PspPhysVO(ResultSet rs) {
		setData(rs);
	}
	
	//sets the physician's profile page data
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		name = db.getStringVal("name", rs);
		profile = (db.getStringVal("profile", rs)).replace("\n", "<br/>");
		photo = db.getStringVal("photo", rs);
		photoCustom = db.getStringVal("photocustom", rs);
		cv = db.getStringVal("cv", rs);
		cvPath = db.getStringVal("cvpath", rs);
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the profile
	 */
	public String getProfile() {
		return profile;
	}

	/**
	 * @param profile the profile to set
	 */
	public void setProfile(String profile) {
		this.profile = profile;
	}

	/**
	 * @return the photo
	 */
	public String getPhoto() {
		return photo;
	}

	/**
	 * @param photo the photo to set
	 */
	public void setPhoto(String photo) {
		this.photo = photo;
	}

	/**
	 * @return the photoCustom
	 */
	public String getPhotoCustom() {
		return photoCustom;
	}

	/**
	 * @param photoCustom the photoCustom to set
	 */
	public void setPhotoCustom(String photoCustom) {
		this.photoCustom = photoCustom;
	}

	/**
	 * @return the cv
	 */
	public String getCv() {
		return cv;
	}

	/**
	 * @param cv the cv to set
	 */
	public void setCv(String cv) {
		this.cv = cv;
	}

	/**
	 * @return the cvPath
	 */
	public String getCvPath() {
		return cvPath;
	}

	/**
	 * @param cvPath the cvPath to set
	 */
	public void setCvPath(String cvPath) {
		this.cvPath = cvPath;
	}

}
