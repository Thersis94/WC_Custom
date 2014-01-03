package com.fastsigns.cutover;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: FranchiseVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 4, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchiseVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Member Variables
	private int franchiseId = 0;
	private String franchiseName = null;
	private String franchiseAlias = null;
	private String franchiseBlurb = null;
	private String rightRailImage = null;
	private String variableLinks = null;
	
	// Flags
	private boolean varBtn1 = false;
	private boolean varBtn2 = false;
	private boolean displayHours = false;
	private boolean displaySameWeekday = false;
	
	// Collections
	private Map<String, String> links = new LinkedHashMap<String, String>();
	private Map<String, String> moduleElements = new LinkedHashMap<String, String>();
	private List<TextModuleVO> textModules = new ArrayList<TextModuleVO>();
	private int moduleOptionId = 0;
	
	private String variableLinkVal1 = null;
	private String varLink = null;
	private int variableLinkTarget1 = 0;
	
	private String variableLinkVal2 = null;
	private String variableLinkText2 = null;
	private int variableLinkTarget2 = -1;
	private String promotionalProductsLink = null;
	
	/**
	 * 
	 */
	public FranchiseVO() {
		
	}

	/**
	 * 
	 * @param rs
	 */
	public FranchiseVO(ResultSet rs) {
		this.assignVals(rs);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void assignVals(ResultSet rs) {
		DBUtil db = new DBUtil();
		franchiseId = db.getIntVal("StoreNumber", rs);
		franchiseName = db.getStringVal("StoreName", rs);
		franchiseAlias = db.getStringVal("StoreNumber", rs);
		franchiseBlurb = db.getStringVal("StoreBlurb", rs);
		rightRailImage = db.getStringVal("RightRailBackgroundImage", rs);
		variableLinks = db.getStringVal("VariableLinks", rs);
		this.setVariableButtons(db.getStringVal("VariableButtons", rs));
		
		// Set the variable link data
		variableLinkVal1 = db.getStringVal("VariableLinkValueOne", rs);
		varLink = db.getStringVal("VAR_LINK  ", rs);
		variableLinkTarget1 = db.getIntVal("VariableLinkTargetOne", rs);
		
		variableLinkVal2 = db.getStringVal("VariableLinkValueTwo", rs);
		variableLinkText2 = db.getStringVal("VariableLinkTextTwo", rs);
		variableLinkTarget2 = db.getIntVal("VariableLinkTargetTwo", rs);
		
		promotionalProductsLink = db.getStringVal("PromotionalProductsLink", rs);
	}
	
	/**
	 * @return the franchiseId
	 */
	public int getFranchiseId() {
		return franchiseId;
	}

	/**
	 * @param franchiseId the franchiseId to set
	 */
	public void setFranchiseId(int franchiseId) {
		this.franchiseId = franchiseId;
	}

	/**
	 * @return the franchiseName
	 */
	public String getFranchiseName() {
		return franchiseName;
	}

	/**
	 * @param franchiseName the franchiseName to set
	 */
	public void setFranchiseName(String franchiseName) {
		this.franchiseName = franchiseName;
	}

	/**
	 * @return the franchiseAlias
	 */
	public String getFranchiseAlias() {
		return franchiseAlias;
	}

	/**
	 * @param franchiseAlias the franchiseAlias to set
	 */
	public void setFranchiseAlias(String franchiseAlias) {
		this.franchiseAlias = franchiseAlias;
	}

	/**
	 * @return the franchiseBlurb
	 */
	public String getFranchiseBlurb() {
		return franchiseBlurb;
	}

	/**
	 * @param franchiseBlurb the franchiseBlurb to set
	 */
	public void setFranchiseBlurb(String franchiseBlurb) {
		this.franchiseBlurb = franchiseBlurb;
	}

	/**
	 * @return the rightRailImage
	 */
	public String getRightRailImage() {
		return rightRailImage;
	}

	/**
	 * @param rightRailImage the rightRailImage to set
	 */
	public void setRightRailImage(String rightRailImage) {
		this.rightRailImage = rightRailImage;
	}

	/**
	 * @return the varBtn1
	 */
	public boolean isVarBtn1() {
		return varBtn1;
	}

	/**
	 * @param varBtn1 the varBtn1 to set
	 */
	public void setVarBtn1(boolean varBtn1) {
		this.varBtn1 = varBtn1;
	}

	/**
	 * @return the varBtn2
	 */
	public boolean isVarBtn2() {
		return varBtn2;
	}

	/**
	 * @param varBtn2 the varBtn2 to set
	 */
	public void setVarBtn2(boolean varBtn2) {
		this.varBtn2 = varBtn2;
	}

	/**
	 * @return the displayHours
	 */
	public boolean isDisplayHours() {
		return displayHours;
	}

	/**
	 * @param displayHours the displayHours to set
	 */
	public void setDisplayHours(boolean displayHours) {
		this.displayHours = displayHours;
	}

	/**
	 * @return the displaySameWeekday
	 */
	public boolean isDisplaySameWeekday() {
		return displaySameWeekday;
	}

	/**
	 * @param displaySameWeekday the displaySameWeekday to set
	 */
	public void setDisplaySameWeekday(boolean displaySameWeekday) {
		this.displaySameWeekday = displaySameWeekday;
	}

	/**
	 * @return the variableLinks
	 */
	public Map<String, String> getLinks() {
		return links;
	}

	/**
	 * @param variableLinks the variableLinks to set
	 */
	public void setLinks(Map<String, String> variableLinks) {
		this.links = variableLinks;
	}

	/**
	 * @return the moduleElements
	 */
	public Map<String, String> getModuleElements() {
		return moduleElements;
	}

	/**
	 * @param moduleElements the moduleElements to set
	 */
	public void setModuleElements(Map<String, String> moduleElements) {
		this.moduleElements = moduleElements;
	}

	/**
	 * @return the variableLinks
	 */
	public String getVariableLinks() {
		return variableLinks;
	}

	/**
	 * @param variableLinks the variableLinks to set
	 */
	public void setVariableLinks(String variableLinks) {
		this.variableLinks = variableLinks;
	}
	
	/**
	 * Converts the pipe delimited data into array
	 * @param data
	 */
	public void setVariableButtons(String data) {
		if (data == null || data.length() == 0) return;
		
		int multiple = Convert.formatInteger(data);
		if (multiple == 1) this.varBtn1 = true;
		else if (multiple == 2) this.varBtn2 = true;
		else {
			varBtn1 = true;
			varBtn2 = true;
		}
	}
	
	/**
	 * 
	 * @param content
	 */
	public void addContent(List<String> content) {
		if (content.size() != textModules.size()) {
			//System.out.println("********************** Mismatch: " + franchiseId + "|" + textModules.size() + "|" + content.size());
		}
		
		for (int i=0; i < content.size(); i++) {
			if (i >= textModules.size()) {
				break;
			}
			TextModuleVO tmvo = textModules.get(i);
			tmvo.setDataText(content.get(i));
			System.out.println("TMVO: " + franchiseId + "|" + tmvo.getKey() + "|" + tmvo.getValue() +  "|" + StringUtil.checkVal(tmvo.getDataText()).length());
			//textModules.add(i, tmvo);
		}
	}
	
	public void setModuleLoc(String colLoc, String colNum) {
		textModules.add(new TextModuleVO(colLoc, colNum));
	}
	
	/**
	 * @return the textModules
	 */
	public List<TextModuleVO> getTextModules() {
		return textModules;
	}

	/**
	 * @param textModules the textModules to set
	 */
	public void setTextModules(List<TextModuleVO> textModules) {
		this.textModules = textModules;
	}

	/**
	 * @return the moduleOptionId
	 */
	public int getModuleOptionId() {
		return moduleOptionId;
	}

	/**
	 * @param moduleOptionId the moduleOptionId to set
	 */
	public void setModuleOptionId(int moduleOptionId) {
		this.moduleOptionId = moduleOptionId;
	}

	/**
	 * @return the variableLinkVal1
	 */
	public String getVariableLinkVal1() {
		return variableLinkVal1;
	}

	/**
	 * @param variableLinkVal1 the variableLinkVal1 to set
	 */
	public void setVariableLinkVal1(String variableLinkVal1) {
		this.variableLinkVal1 = variableLinkVal1;
	}

	/**
	 * @return the variableLinkText1
	 */
	public String getVariableLinkText1() {
		return varLink;
	}

	/**
	 * @param variableLinkText1 the variableLinkText1 to set
	 */
	public void setVariableLinkText1(String variableLinkText1) {
		this.varLink = variableLinkText1;
	}

	/**
	 * @return the variableLinkTarget1
	 */
	public int getVariableLinkTarget1() {
		return variableLinkTarget1;
	}

	/**
	 * @param variableLinkTarget1 the variableLinkTarget1 to set
	 */
	public void setVariableLinkTarget1(int variableLinkTarget1) {
		this.variableLinkTarget1 = variableLinkTarget1;
	}

	/**
	 * @return the variableLinkVal2
	 */
	public String getVariableLinkVal2() {
		return variableLinkVal2;
	}

	/**
	 * @param variableLinkVal2 the variableLinkVal2 to set
	 */
	public void setVariableLinkVal2(String variableLinkVal2) {
		this.variableLinkVal2 = variableLinkVal2;
	}

	/**
	 * @return the variableLinkText2
	 */
	public String getVariableLinkText2() {
		return variableLinkText2;
	}

	/**
	 * @param variableLinkText2 the variableLinkText2 to set
	 */
	public void setVariableLinkText2(String variableLinkText2) {
		this.variableLinkText2 = variableLinkText2;
	}

	/**
	 * @return the variableLinkTarget2
	 */
	public int getVariableLinkTarget2() {
		return variableLinkTarget2;
	}

	/**
	 * @param variableLinkTarget2 the variableLinkTarget2 to set
	 */
	public void setVariableLinkTarget2(int variableLinkTarget2) {
		this.variableLinkTarget2 = variableLinkTarget2;
	}

	/**
	 * @return the promotionalProductsLink
	 */
	public String getPromotionalProductsLink() {
		return promotionalProductsLink;
	}

	/**
	 * @param promotionalProductsLink the promotionalProductsLink to set
	 */
	public void setPromotionalProductsLink(String promotionalProductsLink) {
		this.promotionalProductsLink = promotionalProductsLink;
	}

}
