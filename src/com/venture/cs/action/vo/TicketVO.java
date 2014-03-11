package com.venture.cs.action.vo;

import java.io.Serializable;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 *<b>Title</b>: TicketVO<p/>
 * Stores the information related to customer service tickets pertaining to a vehicle <p/>
 *Copyright: Copyright (c) 2013<p/>
 *Company: SiliconMountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since July 23, 2013
 * Changes:
 * Mar 05, 2014: DBargerhuff: converted files field to List<TicketFileVO> from ArrayList<String>
 ****************************************************************************/

public class TicketVO implements Serializable {

	private static final long serialVersionUID = 1L;
	private String ticketId;
	private String profileId;
	private String firstName;
	private String lastName;
	private String comment;
	private int actionReqFlag;
	private Date createDt;
	private List<TicketFileVO> files;
	
	public TicketVO(ResultSet rs) {
		DBUtil db = new DBUtil();
		this.setTicketId(db.getStringVal("VENTURE_TICKET_ID", rs));
		this.setProfileId(db.getStringVal("PROFILE_ID", rs));
		this.setFirstName(db.getStringVal("FIRST_NM", rs));
		this.setLastName(db.getStringVal("LAST_NM", rs));
		this.setComment(db.getStringVal("COMMENT", rs));
		this.setActionReqFlag(db.getIntVal("ACTION_REQ_FLG", rs));
		this.setCreateDt(db.getDateVal("CREATE_DT", rs));
		this.setFiles(new ArrayList<TicketFileVO>());
	}

	public String getTicketId() {
		return ticketId;
	}

	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	public String getProfileId() {
		return profileId;
	}

	public void setProfileId(String profileId) {
		this.profileId = profileId;
	}

	public String getSubmittedBy() {
		return firstName + " " + lastName;
	}

	public String getFirstName() {
		return firstName;
	}

	public void setFirstName(String firstName) {
		this.firstName = firstName;
	}

	public String getLastName() {
		return lastName;
	}

	public void setLastName(String lastName) {
		this.lastName = lastName;
	}

	public String getComment() {
		return comment;
	}

	public void setComment(String comment) {
		this.comment = comment;
	}

	public int getActionReqFlag() {
		return actionReqFlag;
	}

	public void setActionReqFlag(int actionReqFlag) {
		this.actionReqFlag = actionReqFlag;
	}

	public Date getCreateDt() {
		return createDt;
	}
	
	public void setCreateDt(Date date) {
		this.createDt = date;
	}
	
	public void addFile(TicketFileVO file) {
		files.add(file);
	}

	public List<TicketFileVO> getFiles() {
		return files;
	}

	public void setFiles(ArrayList<TicketFileVO> files) {
		this.files = files;
	}

}
