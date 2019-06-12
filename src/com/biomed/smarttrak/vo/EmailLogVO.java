package com.biomed.smarttrak.vo;

// Java 8
import java.util.Date;

import com.siliconmtn.db.orm.Column;
// SMTBaseLibs
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;

/*****************************************************************************
 <p><b>Title</b>: EmailLogVO.java</p>
 <p><b>Description: </b>POJO for EmailReport UI.</p>
 <p> 
 <p>Copyright: (c) 2000 - 2017 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Apr 3, 2017
 <b>Changes:</b> 
 ***************************************************************************/
public class EmailLogVO extends UserDataVO implements HumanNameIntfc {
	private static final long serialVersionUID = 86197623098299951L;
	private String campaignLogId;
	private String campaignInstanceId;
	private String messageBody;
	private int openCnt;
	private String status;
	private Date sentDate;
	private String subject;
	private String filePathText;

	public EmailLogVO() {
		super();
	}

	public String getCampaignLogId() {
		return campaignLogId;
	}

	public void setCampaignLogId(String campaignLogId) {
		this.campaignLogId = campaignLogId;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public int getOpenCnt() {
		return openCnt;
	}

	public void setOpenCnt(int openCnt) {
		this.openCnt = openCnt;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public String getCampaignInstanceId() {
		return campaignInstanceId;
	}

	public void setCampaignInstanceId(String campaignInstanceId) {
		this.campaignInstanceId = campaignInstanceId;
	}

	public String getMessageBody() {
		return messageBody;
	}

	public void setMessageBody(String messageBody) {
		this.messageBody = messageBody;
	}

	/**
	 * @return
	 */
	@Column(name="file_path_txt")
	public String getFilePathText() {
		return filePathText;
	}

	/**
	 * 
	 * @param fileWritten
	 */
	public void setFilePathText(String filePathText) {
		this.filePathText = filePathText;
	}

	/**
	 * @return
	 */
	public boolean isFileWritten() {
		return StringUtil.isEmpty(filePathText);
	}
}