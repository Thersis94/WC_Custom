package com.sjm.corp.mobile.collection;

import java.util.ArrayList;
import java.util.List;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: ThemeVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Stores all of the theme data for a specific Location for the SJM 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jun 29, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ThemeVO extends SBModuleVO{

	private static final long serialVersionUID = 1L;
	private List<String> name;
	private List<String> thumbLoc; //Stores the file locations for the thumbLoc images
	private List<String> themeId;  //Theme ID for a specific theme. This is what the template VO is populated with(linking it to this object)
	private List<String> colorOneFront;
	private List<String> colorOneBack; //The file locations for the first color for a theme
	private List<String> colorTwoFront;
	private List<String> colorTwoBack;//The file locations for the second color for a given theme;
	private List<String> themePdf; //The file locations for the pdf for a given theme;
	private int maxSelected; // the maximum number of themes that can be selected
	private String location;
	
	public ThemeVO(){
		name = new ArrayList<String>();
		thumbLoc = new ArrayList<String>();
		themeId = new ArrayList<String>();
		colorOneFront = new ArrayList<String>();
		colorOneBack = new ArrayList<String>();
		colorTwoFront = new ArrayList<String>();
		colorTwoBack = new ArrayList<String>();
		themePdf = new ArrayList<String>();
		maxSelected = 1;
	}
	
	public List<String> getThumbLoc() {
		return thumbLoc;
	}

	public void setThumbLoc(List<String> thumbLoc) {
		this.thumbLoc = thumbLoc;
	}

	public List<String> getThemeId() {
		return themeId;
	}

	public void setThemeId(List<String> themeId) {
		this.themeId = themeId;
	}

	public List<String> getThemePdf() {
		return themePdf;
	}

	public void setThemePdf(List<String> themePdf) {
		this.themePdf = themePdf;
	}

	public List<String> getColorOneFront() {
		return colorOneFront;
	}

	public void setColorOneFront(List<String> colorOneFront) {
		this.colorOneFront = colorOneFront;
	}

	public List<String> getColorOneBack() {
		return colorOneBack;
	}

	public void setColorOneBack(List<String> colorOneBack) {
		this.colorOneBack = colorOneBack;
	}

	public List<String> getColorTwoFront() {
		return colorTwoFront;
	}

	public void setColorTwoFront(List<String> colorTwoFront) {
		this.colorTwoFront = colorTwoFront;
	}

	public List<String> getColorTwoBack() {
		return colorTwoBack;
	}

	public void setColorTwoBack(List<String> colorTwoBack) {
		this.colorTwoBack = colorTwoBack;
	}

	public List<String> getName() {
		return name;
	}

	public void setName(List<String> name) {
		this.name = name;
	}

	public int getMaxSelected() {
		return maxSelected;
	}

	public void setMaxSelected(int maxSelected) {
		this.maxSelected = maxSelected;
	}

	public String getLocation() {
		return location;
	}

	public void setLocation(String location) {
		this.location = location;
	}
}
