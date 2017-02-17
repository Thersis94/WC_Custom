package com.biomed.smarttrak.vo;

// java
import java.sql.ResultSet;
import java.util.Date;
import java.util.List;


//SMT baselibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentVO;

/****************************************************************************
 * <b>Title</b>: NoteVO.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Value object that will hold all the data for one note
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Jan 24, 2017<p/>
 * @updates:
 ****************************************************************************/
@Table(name="BIOMEDGPS_NOTE")
public class NoteVO extends SBModuleVO {

	private static final long serialVersionUID = -4071567645204934866L;
	private String noteId;
	private String userId;
	private String teamId;
	private String companyId;
	private String attributeId;
	private String productId;
	private String marketId;
	private String noteName;
	private String noteText;
	private String filePathText;
	private Date expirationDate;
	private String userName;
	private String teamName;
	private List<ProfileDocumentVO> profileDocuments;


	public NoteVO() {super();}
	public NoteVO(ResultSet rs) {
		this();
		setData(rs);
	}

	public NoteVO(ActionRequest req) {
		this();
		setData(req);
	}

	/**
	 * @param req
	 */
	private void setData(ActionRequest req) {

		setNoteId(StringUtil.checkVal(req.getParameter("noteId")));
		setUserId(StringUtil.checkVal(req.getParameter("userId")));
		setTeamId(req.getParameter("teamId"));
		setCompanyId(req.getParameter("companyId"));
		setAttributeId(req.getParameter("attributeId"));
		setProductId(req.getParameter("productId"));
		setMarketId(req.getParameter("marketId"));
		setNoteName(StringUtil.checkVal(req.getParameter("noteName")));
		setNoteText(StringUtil.checkVal(req.getParameter("noteText")));
		setFilePathText(StringUtil.checkVal(req.getParameter("filePathText")));
		setExpirationDate(Convert.formatDate(Convert.DATE_SLASH_SHORT_PATTERN, 
				StringUtil.checkVal(req.getParameter("expirationDate"))));
		setCreateDate(Convert.formatDate(Convert.DATE_SLASH_SHORT_PATTERN, 
				StringUtil.checkVal(req.getParameter("createDate"))));
		setUpdateDate(Convert.formatDate(Convert.DATE_SLASH_SHORT_PATTERN, 
				StringUtil.checkVal(req.getParameter("updateDate"))));

	}
	/**
	 * @param rs
	 */
	private void setData(ResultSet rs) {

		DBUtil util = new DBUtil();

		setNoteId(util.getStringVal("NOTE_ID", rs));
		setUserId(util.getStringVal("USER_ID", rs));
		setTeamId(util.getStringVal("TEAM_ID", rs));
		setCompanyId(util.getStringVal("COMPANY_ID", rs));
		setAttributeId(util.getStringVal("ATTRIBUTE_ID", rs));
		setProductId(util.getStringVal("PRODUCT_ID", rs));
		setMarketId(util.getStringVal("MARKET_ID", rs));
		setNoteName(util.getStringVal("NOTE_NM", rs));
		setNoteText(util.getStringVal("NOTE_TXT", rs));
		setFilePathText( util.getStringVal("FILE_PATH_TXT", rs));
		setExpirationDate(util.getDateVal("EXPIRATION_DT", rs));
		setCreateDate(util.getDateVal("CREATE_DT", rs));
		setUpdateDate(util.getDateVal("UPDATE_DT", rs));

	}

	/**
	 * checks the fields that are required in order to save a note,
	 * @return
	 */
	public boolean isNoteSaveable() {
		if (StringUtil.isEmpty(userId)) return false;
		if (StringUtil.isEmpty(noteText) || StringUtil.isEmpty(noteName)) return false;
		//ensure we have one of the 3 bindings - company, market or product
		return !StringUtil.isEmpty(companyId) || !StringUtil.isEmpty(marketId) || !StringUtil.isEmpty(productId);
	}

	/*
	 * @return the noteId
	 */
	@Column(name="NOTE_ID", isPrimaryKey=true)
	public String getNoteId() {
		return noteId;
	}

	/**
	 * @return the userId
	 */
	@Column(name="USER_ID" )
	public String getUserId() {
		return userId;
	}
	/**
	 * @return the teamId
	 */
	@Column(name="TEAM_ID" )
	public String getTeamId() {
		return teamId;
	}

	/**
	 * @return the companyId
	 */
	@Column(name="COMPANY_ID" )
	public String getCompanyId() {
		return companyId;
	}

	/**
	 * @return the productId
	 */
	@Column(name="PRODUCT_ID" )
	public String getProductId() {
		return productId;
	}

	/**
	 * @return the marketId
	 */
	@Column(name="MARKET_ID" )
	public String getMarketId() {
		return marketId;
	}
	/**
	 * @return the noteName
	 */
	@Column(name="NOTE_NM" )
	public String getNoteName() {
		return noteName;
	}
	/**
	 * @return the noteText
	 */
	@Column(name="NOTE_TXT" )
	public String getNoteText() {
		return noteText;
	}
	/**
	 * @return the filePathText
	 */
	@Column(name="FILE_PATH_TXT" )
	public String getFilePathText() {
		return filePathText;
	}

	/**
	 * @return the expirationDate
	 */
	@Column(name="EXPIRATION_DT" )
	public Date getExpirationDate() {
		return expirationDate;
	}
	/**
	 * Must override the method to apply an annotation
	 */
	@Column(name = "create_dt", isInsertOnly = true, isAutoGen = true)
	public Date getCreateDate() {
		return super.getCreateDate();
	}


	/**
	 * Must override the method to apply an annotation
	 */
	@Column(name = "update_dt", isUpdateOnly = true, isAutoGen = true)
	public Date getUpdateDate() {
		return super.getUpdateDate();
	}

	/**
	 * @param noteId the noteId to set
	 */
	public void setNoteId(String noteId) {
		this.noteId = noteId;
	}

	/**
	 * @param userId the userId to set
	 */
	public void setUserId(String userId) {
		this.userId = userId;
	}


	/**
	 * @param teamId the teamId to set
	 */
	public void setTeamId(String teamId) {
		this.teamId = teamId;
	}

	/**
	 * @param companyId the companyId to set
	 */
	public void setCompanyId(String companyId) {
		this.companyId = companyId;
	}

	/**
	 * @param productId the productId to set
	 */
	public void setProductId(String productId) {
		this.productId = productId;
	}

	/**
	 * @param marketId the marketId to set
	 */
	public void setMarketId(String marketId) {
		this.marketId = marketId;
	}

	/**
	 * @param noteName the noteName to set
	 */
	public void setNoteName(String noteName) {
		this.noteName = noteName;
	}

	/**
	 * @param noteText the noteText to set
	 */
	public void setNoteText(String noteText) {
		this.noteText = noteText;
	}

	/**
	 * @param filePathText the filePathText to set
	 */
	public void setFilePathText(String filePathText) {
		this.filePathText = filePathText;
	}

	/**
	 * @param expirationDate the expirationDate to set
	 */
	public void setExpirationDate(Date expirationDate) {
		this.expirationDate = expirationDate;
	}

	/**
	 * @return the userName
	 */
	public String getUserName() {
		return userName;
	}
	/**
	 * @param userName the userName to set
	 */
	public void setUserName(String userName) {
		this.userName = userName;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return StringUtil.getToString(this);
	}
	/**
	 * @return the attributeId
	 */
	@Column(name="ATTRIBUTE_ID" )
	public String getAttributeId() {
		return attributeId;
	}
	/**
	 * @param attributeId the attributeId to set
	 */

	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}
	/**
	 * @return the teamName
	 */
	public String getTeamName() {
		return teamName;
	}
	/**
	 * @param teamName the teamName to set
	 */
	public void setTeamName(String teamName) {
		this.teamName = teamName;
	}
	/**
	 * @return the profileDocuments
	 */
	public List<ProfileDocumentVO> getProfileDocuments() {
		return profileDocuments;
	}
	/**
	 * @param profileDocuments the profileDocuments to set
	 */
	public void setProfileDocuments(List<ProfileDocumentVO> profileDocuments) {
		this.profileDocuments = profileDocuments;
	}

}
