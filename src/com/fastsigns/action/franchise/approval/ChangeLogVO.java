package com.fastsigns.action.franchise.approval;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.Map;

import javax.servlet.http.HttpSession;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserRoleVO;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.common.constants.Constants;
/****************************************************************************
 * <b>Title</b>: ChangeLogVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Dec 30, 2011<p/>
 * <b>Changes: Migrated all Approval action code to new package and updated code 
 * to use new workflow</b>
 ****************************************************************************/
@Deprecated
public class ChangeLogVO {
	String ftsChangelogId = null;
	String componentId = null;
	String typeId = null;
	String submitterId = null;
	String reviewerId = null;
	Integer statusNo = 0;
	String descTxt = null;
	String resolutionTxt = null;
	String franchiseId = null;
	String modName = null;
	String modDescTxt = null;
	String submitterName = null;
	Date reviewDate = null;
	Date submittedDate = null;
	Date updateDate = null;
	public static enum Status {PENDING, APPROVED, DENIED}
	
	public ChangeLogVO(){
	}
	/**
	 * Constructor that takes a request object and initializes based off that.  
	 * Use caution as this only reads initial data used to create new rows in 
	 * the changelog table and defaults status to pending.
	 * @param req contains the data for creating a new changelog
	 */
	public ChangeLogVO(SMTServletRequest req){
		HttpSession ses = req.getSession();
    	UserRoleVO role = (UserRoleVO) ses.getAttribute(Constants.ROLE_DATA);
     	componentId = req.getParameter("cmpId");
    	typeId = req.getParameter("cmpType");
    	submitterId = role.getProfileId();
    	descTxt = req.getParameter("subComments");
    	statusNo = Status.PENDING.ordinal();
	}
	/**
	 * Constructor that takes a resultset to set data members
	 * @param rs contains the data for a particular changelog
	 * @throws SQLException
	 */
	public ChangeLogVO(ResultSet rs) throws SQLException{
		ftsChangelogId = rs.getString("FTS_CHANGELOG_ID");
		componentId = rs.getString("COMPONENT_ID");
		typeId = rs.getString("TYPE_ID");
		submitterId = rs.getString("SUBMITTER_ID");
		descTxt = rs.getString("DESC_TXT");
		submittedDate = rs.getTimestamp("SUBMITTED_DT");
		statusNo = rs.getInt("STATUS_NO");
	}
	/**
	 * Constructor that takes a map of values to set data members.
	 * @param vals
	 */
	public ChangeLogVO(Map<String, String> vals){
		ftsChangelogId = vals.get("FTS_CHANGELOG_ID");
		componentId = vals.get("COMPONENT_ID");
		typeId = vals.get("TYPE_ID");
		submitterId = vals.get("SUBMITTER_ID");
		descTxt = vals.get("DESC_TXT");
		submittedDate = Convert.formatTimestamp("MM/dd/yyyy HH:mm", vals.get("SUBMITTED_DT"));
		statusNo = Convert.formatInteger(vals.get("STATUS_NO"));
		franchiseId = vals.get("FRANCHISE_ID");
		reviewerId = vals.get("REVIEWER_ID");
		resolutionTxt = vals.get("RESOLUTION_TXT");
		modName = vals.get("MOD_NAME");
		reviewDate = Convert.formatTimestamp("MM/dd/yyyy HH:mm", vals.get("REVIEW_DT"));
		updateDate = Convert.formatTimestamp("MM/dd/yyyy HH:mm", vals.get("UPDATE_DT"));
	}
	/**
	 * Used by report to properly store table data.
	 * @param rs ResultSet that contains data for populating the table.
	 * @return The Changelog instance modified.
	 * @throws SQLException
	 */
	public ChangeLogVO setData(ResultSet rs) throws SQLException{
		typeId = rs.getString("TYPE_ID");
		submitterId = rs.getString("SUBMITTER_ID");
		descTxt = rs.getString("DESC_TXT");
		submittedDate = rs.getTimestamp("SUBMITTED_DT");
		updateDate = rs.getTimestamp("UPDATE_DT");
		modDescTxt = rs.getString("OPTION_DESC");
		if(rs.getString("page_display_nm") != null)
			modName = rs.getString("PAGE_DISPLAY_NM");
		else
			modName = rs.getString("OPTION_NM");
		if(rs.getString("modfranchise_id") != null)
			franchiseId = rs.getString("modfranchise_id");
		else if(rs.getString("franchise_id") != null)
			franchiseId = rs.getString("franchise_id");
		else if(rs.getString("site_id") != null){
			franchiseId = rs.getString("site_id");
			//Check if the franchiseId is the Main Site and set accordingly
			if(franchiseId.equalsIgnoreCase("FTS_7")){
				franchiseId = "FASTSIGNS Main Site";
				//if not main site, parse to get the franchise number.
			} else 
				franchiseId = franchiseId.substring(4, franchiseId.length() - 2);
		} else 
			franchiseId = "Global Item";
		return this;
	}
	
	public String getFtsChangelogId() {
		return ftsChangelogId;
	}
	public void setFtsChangelogId(String ftsChangelogId) {
		this.ftsChangelogId = ftsChangelogId;
	}
	public String getComponentId() {
		return componentId;
	}
	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}
	public String getTypeId() {
		return typeId;
	}
	public void setTypeId(String typeId) {
		this.typeId = typeId;
	}
	public String getSubmitterId() {
		return submitterId;
	}
	public void setSubmitterId(String submitterId) {
		this.submitterId = submitterId;
	}
	public String getReviewerId() {
		return reviewerId;
	}
	public void setReviewerId(String reviewerId) {
		this.reviewerId = reviewerId;
	}
	public Integer getStatusNo() {
		return statusNo;
	}
	public void setStatusNo(Integer statusNo) {
		this.statusNo = statusNo;
	}
	public String getDescTxt() {
		return descTxt;
	}
	public void setDescTxt(String descTxt) {
		this.descTxt = descTxt;
	}
	public String getResolutionTxt() {
		return resolutionTxt;
	}
	public void setResolutionTxt(String resolutionTxt) {
		this.resolutionTxt = resolutionTxt;
	}
	public String getFranchiseId(){
		return franchiseId;
	}
	public void setFranchiseId(String franchiseId){
		this.franchiseId = franchiseId;
	}
	public String getModName(){
		return modName;
	}
	public void setModName(String modName){
		this.modName = modName;
	}
	public Date getReviewDate() {
		return reviewDate;
	}
	public void setReviewDate(Date reviewDate) {
		this.reviewDate = reviewDate;
	}
	public Date getSubmittedDate() {
		return submittedDate;
	}
	public void setSubmittedDate(Date submittedDate) {
		this.submittedDate = submittedDate;
	}
	public Date getUpdateDate() {
		return updateDate;
	}
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}
	public String getModDescTxt() {
		return modDescTxt;
	}
	public void setModDescTxt(String modDescTxt) {
		this.modDescTxt = modDescTxt;
	}
	public String getSubmitterName() {
		return submitterName;
	}
	public void setSubmitterName(String submitterName) {
		this.submitterName = submitterName;
	}
	public String getFriendlyStatus(){
		return Status.values()[this.statusNo].toString().toLowerCase();
	}
	
}
