package com.biomed.smarttrak.vo;

// java
import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

//SMT baselibs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: NoteVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
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
public class NoteVO implements Serializable {

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
	private Date creationDate;
	private Date updateDate;
	private String userName;


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

		this.noteId = StringUtil.checkVal(req.getParameter("noteId"));
		this.userId = StringUtil.checkVal(req.getParameter("userId"));
		this.teamId = req.getParameter("teamId");
		this.companyId = req.getParameter("companyId");
		this.attributeId = req.getParameter("attributeId");
		this.productId = req.getParameter("productId");
		this.marketId  = req.getParameter("marketId");
		this.noteName = StringUtil.checkVal(req.getParameter("noteName"));
		this.noteText = StringUtil.checkVal(req.getParameter("noteText"));
		this.filePathText = StringUtil.checkVal(req.getParameter("filePath"));
		this.expirationDate = Convert.formatDate(Convert.DATE_SLASH_SHORT_PATTERN, 
				StringUtil.checkVal(req.getParameter("expirationDate")));
		this.creationDate = Convert.formatDate(Convert.DATE_SLASH_SHORT_PATTERN, 
				StringUtil.checkVal(req.getParameter("createDate")));
		this.updateDate = Convert.formatDate(Convert.DATE_SLASH_SHORT_PATTERN, 
				StringUtil.checkVal(req.getParameter("updateDate")));

	}
	/**
	 * @param rs
	 */
	private void setData(ResultSet rs) {

		DBUtil util = new DBUtil();

		this.noteId = util.getStringVal("NOTE_ID", rs);
		this.userId = util.getStringVal("USER_ID", rs);
		this.teamId = util.getStringVal("TEAM_ID", rs);
		this.companyId = util.getStringVal("COMPANY_ID", rs);
		this.attributeId = util.getStringVal("ATTRIBUTE_ID", rs);
		this.productId = util.getStringVal("PRODUCT_ID", rs);
		this.marketId  = util.getStringVal("MARKET_ID", rs);
		this.noteName = util.getStringVal("NOTE_NM", rs);
		this.noteText = util.getStringVal("NOTE_TXT", rs);
		this.filePathText = util.getStringVal("FILE_PATH_TXT", rs);
		this.expirationDate = util.getDateVal("EXPIRATION_DT", rs);
		this.creationDate = util.getDateVal("CREATE_DT", rs);
		this.updateDate = util.getDateVal("UPDATE_DT", rs);

	}

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
	 * @return the creationDate
	 */
	@Column(name="CREATE_DT " )
	public Date getCreationDate() {
		return creationDate;
	}
	/**
	 * @return the updateDate
	 */
	@Column(name="UPDATE_DT" )
	public Date getUpdateDate() {
		return updateDate;
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
	 * @param creationDate the creationDate to set
	 */
	public void setCreationDate(Date creationDate) {
		this.creationDate = creationDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
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
	public String getAttributeId() {
		return attributeId;
	}
	/**
	 * @param attributeId the attributeId to set
	 */
	public void setAttributeId(String attributeId) {
		this.attributeId = attributeId;
	}

}
