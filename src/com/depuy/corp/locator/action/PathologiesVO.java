package com.depuy.corp.locator.action;

import java.sql.ResultSet;
import java.sql.SQLException;

import com.siliconmtn.util.StringUtil;

public class PathologiesVO {
	private final String [] level = {"pathology", "diagnosis", "approach", "technique"};
	
	private String pathologyId = null;
	private String parentId = null;
	private Integer levelNo = null;
	private Boolean selected = false;
	private String pathologyNm = null;
	private Boolean showChildren = false;
	public PathologiesVO() {

	}

	public PathologiesVO(ResultSet rs) throws SQLException {
		pathologyId = rs.getString("pathology_id");
		parentId = rs.getString("parent_id");
		levelNo = rs.getInt("level_no");
		pathologyNm = rs.getString("pathology_nm");
		selected = StringUtil.checkVal(rs.getString("pathology_xr_id"), null) != null;
	}

	/**
	 * @return the pathologyId
	 */
	public String getPathologyId() {
		return pathologyId;
	}

	/**
	 * @param pathologyId the pathologyId to set
	 */
	public void setPathologyId(String pathologyId) {
		this.pathologyId = pathologyId;
	}

	/**
	 * @return the parentId
	 */
	public String getParentId() {
		return parentId;
	}

	/**
	 * @param parentId the parentId to set
	 */
	public void setParentId(String parentId) {
		this.parentId = parentId;
	}

	/**
	 * @return the levelNo
	 */
	public Integer getLevelNo() {
		return levelNo;
	}

	/**
	 * @param levelNo the levelNo to set
	 */
	public void setLevelNo(Integer levelNo) {
		this.levelNo = levelNo;
	}

	/**
	 * @return the selected
	 */
	public Boolean getSelected() {
		return selected;
	}

	/**
	 * @param selected the selected to set
	 */
	public void setSelected(Boolean selected) {
		this.selected = selected;
	}

	/**
	 * @return the pathologyNm
	 */
	public String getPathologyNm() {
		return pathologyNm;
	}

	/**
	 * @param pathologyNm the pathologyNm to set
	 */
	public void setPathologyNm(String pathologyNm) {
		this.pathologyNm = pathologyNm;
	}
	/**
	 * 
	 * @return the levels Class Text
	 */
	public String getLevel(){
		return level[levelNo-1];
	}

	/**
	 * @return the showChildren
	 */
	public Boolean getShowChildren() {
		return showChildren;
	}

	/**
	 * @param showChildren the showChildren to set
	 */
	public void setShowChildren(Boolean showChildren) {
		this.showChildren = showChildren;
	}
	
	
	
}
