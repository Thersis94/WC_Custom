package com.wsla.util.migration.vo;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.siliconmtn.annotations.Importable;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.TicketCommentVO;
import com.wsla.data.ticket.TicketCommentVO.ActivityType;
import com.wsla.util.migration.SOHeader;

/****************************************************************************
 * <p><b>Title:</b> SOCMTFileVO.java</p>
 * <p><b>Description:</b> Represents a row in the Comments (ZZ-OSCMT) file.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Aug 9, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class SOCMTFileVO {

	private String soNumber;
	private String ticketId;
	private Date receivedDate;
	private String comment1;
	private String comment2;
	private String comment3;
	private String comment4;
	private String comment5;
	private String comment6;

	public String getSoNumber() {
		return soNumber;
	}

	public String getTicketId() {
		return ticketId;
	}

	public String getComment1() {
		return comment1;
	}

	public String getComment2() {
		return comment2;
	}

	public String getComment3() {
		return comment3;
	}

	public String getComment4() {
		return comment4;
	}

	public String getComment5() {
		return comment5;
	}

	public String getComment6() {
		return comment6;
	}

	@Importable(name="SO Number")
	public void setSoNumber(String soNumber) {
		this.soNumber = soNumber;
	}

	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	@Importable(name="Date Received")
	public void setReceivedDate(Date receivedDate) {
		this.receivedDate = receivedDate;
	}

	@Importable(name="Comment Line 1")
	public void setComment1(String comment1) {
		this.comment1 = comment1;
	}

	@Importable(name="Comment Line 2")
	public void setComment2(String comment2) {
		this.comment2 = comment2;
	}

	@Importable(name="Comment Line 3")
	public void setComment3(String comment3) {
		this.comment3 = comment3;
	}

	@Importable(name="Comment Line 4")
	public void setComment4(String comment4) {
		this.comment4 = comment4;
	}

	@Importable(name="Comment Line 5")
	public void setComment5(String comment5) {
		this.comment5 = comment5;
	}

	@Importable(name="Comment Line 6")
	public void setComment6(String comment6) {
		this.comment6 = comment6;
	}

	/**
	 * return a list of comments from this row of data that need to be written to the DB.
	 * If Steve decides to concat all 6 down to one DB entry we can make this method return a one-item list
	 * and not change downstream code.
	 * @return
	 */
	public List<TicketCommentVO> getComments() {
		List<TicketCommentVO> data = new ArrayList<>();

		for (int x=1; x < 7; x++) {
			String comment = getComment(x);
			if (StringUtil.isEmpty(comment)) continue;

			//create a comment VO for each and add it to the list
			TicketCommentVO vo = new TicketCommentVO();
			vo.setActivityType(ActivityType.COMMENT);
			vo.setUserId(SOHeader.LEGACY_USER_ID);
			vo.setTicketId(getTicketId());
			vo.setCreateDate(getReceivedDate(x));
			vo.setComment(comment);
			data.add(vo);
		}
		return data;
	}


	/**
	 * Return the requested comment agnostically
	 * @param x
	 * @return
	 */
	private String getComment(int x) {
		switch (x) {
			case 6: return getComment6();
			case 5: return getComment5();
			case 4: return getComment4();
			case 3: return getComment3();
			case 2: return getComment2();
			default: return getComment1();
		}
	}

	/**
	 * combine the line number as a factor of minutes to the recieved date to resemble some sort of chronological ordering of the rows
	 * @return
	 */
	private Date getReceivedDate(int incrHrs) {
		if (incrHrs < 2 || receivedDate == null) return receivedDate;
		Calendar cal = Calendar.getInstance();
		cal.setTime(receivedDate);
		cal.add(Calendar.HOUR_OF_DAY, incrHrs);
		return cal.getTime();
	}
}