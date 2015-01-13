package com.depuysynthes.pa;

import java.util.ArrayList;
import java.util.List;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: SolrStoryVO.java<p/>
 * <b>Description: Used to store information for ambassador stories for use in the 
 * SolrStoryIndexer.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2014<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Jan 5, 2015
 ****************************************************************************/

public class SolrStoryVO extends SBModuleVO {

	private static final long serialVersionUID = 1L;
	private String storyId;
	private String author;
	private String zip;
	private String city;
	private String state;
	private String lat;
	private String lng;
	private String detailImage;
	private List<String> joints;
	private List<String> hobbies;
	private String otherHobby;
	private String title;
	private String content;
	
	public SolrStoryVO() {
		joints = new ArrayList<String>();
		hobbies = new ArrayList<String>();
	}
	
	public String getStoryId() {
		return storyId;
	}
	
	public void setStoryId(String storyId) {
		this.storyId = storyId;
	}
	
	public String getAuthor() {
		return author;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public String getZip() {
		return zip;
	}

	public void setZip(String zip) {
		this.zip = zip;
	}

	public String getCity() {
		return city;
	}
	
	public void setCity(String city) {
		this.city = city;
	}
	
	public String getState() {
		return state;
	}
	
	public void setState(String state) {
		this.state = state;
	}
	
	public String getLat() {
		return lat;
	}

	public void setLat(String lat) {
		this.lat = lat;
	}

	public String getLng() {
		return lng;
	}

	public void setLng(String lng) {
		this.lng = lng;
	}

	public String getDetailImage() {
		return detailImage;
	}
	
	public void setDetailImage(String detailImage) {
		this.detailImage = detailImage;
	}
	
	public List<String> getJoints() {
		return joints;
	}
	
	public void addJoint(String joint) {
		joints.add(joint);
	}
	
	public void setJoints(List<String> joints) {
		this.joints = joints;
	}

	public List<String> getHobbies() {
		return hobbies;
	}
	
	public void setHobbies(List<String> hobbies) {
		this.hobbies = hobbies;
	}
	
	public void addHobby(String hobby) {
		hobbies.add(hobby);
	}
	
	public String getOtherHobbies() {
		return otherHobby;
	}

	public void setOtherHobbies(String otherHobby) {
		this.otherHobby = otherHobby;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}
	
	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

}
