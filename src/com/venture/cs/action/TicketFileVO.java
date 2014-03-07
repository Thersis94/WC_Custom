package com.venture.cs.action;

// JDK 1.6
import java.io.Serializable;
import java.sql.ResultSet;
import java.util.Date;

// SMTBaseLibs 2.0
import com.siliconmtn.db.DBUtil;

/****************************************************************************
 *<b>Title</b>: TicketFileVO<p/>
 * Contains ticket file information for files associated with a ticket. <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Mar 05, 2014
 * Changes:
 * Mar 05, 2014: DBargerhuff: created class.
 ****************************************************************************/

public class TicketFileVO implements Serializable {

	private static final long serialVersionUID = -4458140979748239619L;
	private String ticketFileId;
	private String ticketId;
	private String fileUrl;
	private Date createDt;
	
	/**
	 * default constructor
	 */
	public TicketFileVO() { }
	
	/**
	 * Constructor
	 * @param rs
	 */
	public TicketFileVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.setTicketFileId(db.getStringVal("VENTURE_TICKET_FILE_ID", rs));
		this.setTicketId(db.getStringVal("VENTURE_TICKET_ID", rs));
		this.setFileUrl(db.getStringVal("FILE_URL", rs));
		this.setCreateDt(db.getDateVal("CREATE_DT", rs));
	}

	/**
	 * @return the ticketFileId
	 */
	public String getTicketFileId() {
		return ticketFileId;
	}

	/**
	 * @param ticketFileId the ticketFileId to set
	 */
	public void setTicketFileId(String ticketFileId) {
		this.ticketFileId = ticketFileId;
	}

	/**
	 * @return the ticketId
	 */
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @return the fileUrl
	 */
	public String getFileUrl() {
		return fileUrl;
	}

	/**
	 * @param fileUrl the fileUrl to set
	 */
	public void setFileUrl(String fileUrl) {
		this.fileUrl = fileUrl;
	}

	/**
	 * @return the createDt
	 */
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @param createDt the createDt to set
	 */
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

}
