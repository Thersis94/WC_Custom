package com.biomed.smarttrak.admin.vo;

import java.util.Date;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: DataFeedNoteVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO container information related to a single note on a CRM customer
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 18, 2017
 ****************************************************************************/

@Table(name="CUSTOMER_NOTE")
public class DataFeedNoteVO {
	
	private String noteName;
	private String noteText;
	private String customerId;
	private Date createDate;
	private UserDataVO author;
	
	public DataFeedNoteVO() {
		author = new UserDataVO();
	}
	
	public DataFeedNoteVO(ActionRequest req) {
		this();
		setData(req);
	}
	
	public void setData(ActionRequest req) {
		noteName = req.getParameter("noteName");
		noteText = req.getParameter("noteText");
		customerId = req.getParameter("customerId");
		author = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);
	}

	@Column(name="note_nm")
	public String getNoteName() {
		return noteName;
	}
	public void setNoteName(String noteName) {
		this.noteName = noteName;
	}
	@Column(name="note_txt")
	public String getNoteText() {
		return noteText;
	}
	public void setNoteText(String noteText) {
		this.noteText = noteText;
	}
	@Column(name="customer_id")
	public String getCustomerId() {
		return customerId;
	}
	public void setCustomerId(String customerId) {
		this.customerId = customerId;
	}

	@Column(name="create_dt", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDate() {
		return createDate;
	}

	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	public UserDataVO getAuthor() {
		return author;
	}

	public void setAuthor(UserDataVO author) {
		this.author = author;
	}

	@Column(name="profile_id")
	public String getProfileId() {
		if (author == null) return null;
		return author.getProfileId();
	}
	
	public void setProfileId(String profileId) {
		author = new UserDataVO();
		author.setProfileId(profileId);
	}

}
