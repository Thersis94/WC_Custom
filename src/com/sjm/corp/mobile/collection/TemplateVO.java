package com.sjm.corp.mobile.collection;

import java.util.ArrayList;
import java.util.List;

import com.smt.sitebuilder.action.SBModuleVO;

/****************************************************************************
 * <b>Title</b>: TemplateVO.java<p/>
 * <b>Description: Object that handles the data collected from SJM related to the Templates and stores it temporarily(until we put it in the db at the end)</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since June 21, 2012
 ****************************************************************************/

public class TemplateVO extends SBModuleVO{
	private static final long serialVersionUID = 1L;
	private List<String> themes; // ThemeID is stored in here, for the selected themes...
	private List<String> thumb;
	private boolean choice1 = false;
	private boolean choice2 = false;
	private boolean choice3 = false;
	private String templateId;
	
	public TemplateVO(){
		super();
		themes = new ArrayList<String>();
		thumb = new ArrayList<String>();
	}

	public boolean isChoice1() {
		return choice1;
	}

	public void setChoice1(boolean choice1) {
		this.choice1 = choice1;
	}


	public boolean isChoice3() {
		return choice3;
	}

	public void setChoice3(boolean choice3) {
		this.choice3 = choice3;
	}

	public boolean isChoice2() {
		return choice2;
	}

	public void setChoice2(boolean choice2) {
		this.choice2 = choice2;
	}

	public String getTemplateId() {
		return templateId;
	}

	public void setTemplateId(String templateId) {
		this.templateId = templateId;
	}

	public List<String> getThemes() {
		return themes;
	}

	public void setThemes(List<String> themes) {
		this.themes = themes;
	}

	public List<String> getThumb() {
		return thumb;
	}

	public void setThumb(List<String> thumb) {
		this.thumb = thumb;
	}
}