package com.ansmed.sb.physician;

import java.util.ArrayList;
import java.util.List;
import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.smt.sitebuilder.action.AbstractSiteBuilderVO;

/*****************************************************************************
 <p><b>Title</b>: FellowsVO.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author David Bargerhuff
 @version 1.0
 @since Feb 17, 2009
 Last Updated:
 ***************************************************************************/

public class FellowsVO extends AbstractSiteBuilderVO {
	private static final long serialVersionUID = 1l;
	private String fellowsId = null;
	private String programNm = null;
	private String programNotes = null;
	private String coordNm = null;
	private String coordPhone =  null;
	private String coordEmail = null;
	private String surgeonId = null;
	private List<FellowsGoalVO> fellowsGoal = null;
	private List<FellowsSurgeonVO> fellowsSurgeon = null;
	private String repId = null;
	private String repFirstNm = null;
	private String repLastNm = null;
	private String surgeonFirstNm = null;
	private String surgeonLastNm = null;
	
	/**
	 * 
	 */
	public FellowsVO() {
		fellowsGoal = new ArrayList<FellowsGoalVO>();
		fellowsSurgeon = new ArrayList<FellowsSurgeonVO>();
	}
	
	/**
	 * Initializes the VO to the params provided in the row object
	 * @param rs
	 */
	public FellowsVO(ResultSet rs) {
		super();
		fellowsGoal = new ArrayList<FellowsGoalVO>();
		fellowsSurgeon = new ArrayList<FellowsSurgeonVO>();
		setData(rs);
	}
	
	/**
	 * Initializes the VO to the params provided in the request object
	 * @param rs
	 */
	public FellowsVO(SMTServletRequest req) {
		super();
		fellowsGoal = new ArrayList<FellowsGoalVO>();
		fellowsSurgeon = new ArrayList<FellowsSurgeonVO>();
		setData(req);
	}
	
	/**
	 * Sets the VO to the params provided in the row object
	 * @param rs
	 */
	public void setData(ResultSet rs) {
		StringEncoder se = new StringEncoder();
		DBUtil db = new DBUtil();
		fellowsId = db.getStringVal("fellows_id", rs);
		programNm = se.decodeValue(db.getStringVal("program_nm", rs));
		programNotes = se.decodeValue(db.getStringVal("program_notes_txt", rs));
		coordNm = db.getStringVal("coord_nm", rs);
		coordPhone = db.getStringVal("coord_phone_no", rs);
		coordEmail = db.getStringVal("coord_email_txt", rs);
		surgeonId = db.getStringVal("surgeon_id", rs);
		this.addGoal(new FellowsGoalVO(rs));
		repId = db.getStringVal("sales_rep_id", rs);
		repFirstNm = se.decodeValue(db.getStringVal("rep_first_nm", rs));
		repLastNm = se.decodeValue(db.getStringVal("rep_last_nm", rs));
		surgeonFirstNm = se.decodeValue(db.getStringVal("phys_first_nm", rs));
		surgeonLastNm = se.decodeValue(db.getStringVal("phys_last_nm", rs));
	}
	
	/**
	 * Sets the VO to the params provided in the request object
	 * @param req
	 */
	public void setData(SMTServletRequest req) {
		fellowsId = req.getParameter("fellowsId");
		programNm = req.getParameter("programNm");
		programNotes = req.getParameter("programNotes");
		coordNm = req.getParameter("coordNm");
		coordPhone = req.getParameter("coordPhone");
		coordEmail = req.getParameter("coordEmail");
		surgeonId = req.getParameter("surgeonId");
		this.addGoal(new FellowsGoalVO(req));
	}
	
	public void addGoal(FellowsGoalVO fgvo) {
		this.fellowsGoal.add(fgvo);
	}
	
	public void addFellowsSurgeon(FellowsSurgeonVO fsvo) {
		this.fellowsSurgeon.add(fsvo);
	}

	/**
	 * @return the fellowsId
	 */
	public String getFellowsId() {
		return fellowsId;
	}

	/**
	 * @param fellowsId the fellowsId to set
	 */
	public void setFellowsId(String fellowsId) {
		this.fellowsId = fellowsId;
	}

	/**
	 * @return the surgeonId
	 */
	public String getSurgeonId() {
		return surgeonId;
	}

	/**
	 * @param surgeonId the surgeonId to set
	 */
	public void setSurgeonId(String surgeonId) {
		this.surgeonId = surgeonId;
	}

	/**
	 * @return the programNm
	 */
	public String getProgramNm() {
		return programNm;
	}

	/**
	 * @param programNm the programNm to set
	 */
	public void setProgramNm(String programNm) {
		this.programNm = programNm;
	}

	/**
	 * @return the coordNm
	 */
	public String getCoordNm() {
		return coordNm;
	}

	/**
	 * @param coordNm the coordNm to set
	 */
	public void setCoordNm(String coordNm) {
		this.coordNm = coordNm;
	}

	/**
	 * @return the coordPhone
	 */
	public String getCoordPhone() {
		return coordPhone;
	}

	/**
	 * @param coordPhone the coordPhone to set
	 */
	public void setCoordPhone(String coordPhone) {
		this.coordPhone = coordPhone;
	}

	/**
	 * @return the coordEmail
	 */
	public String getCoordEmail() {
		return coordEmail;
	}

	/**
	 * @param coordEmail the coordEmail to set
	 */
	public void setCoordEmail(String coordEmail) {
		this.coordEmail = coordEmail;
	}

	/**
	 * @return the fellowsGoal
	 */
	public List<FellowsGoalVO> getFellowsGoal() {
		return fellowsGoal;
	}

	/**
	 * @param fellowsGoal the fellowsGoal to set
	 */
	public void setFellowsGoal(List<FellowsGoalVO> fellowsGoal) {
		this.fellowsGoal = fellowsGoal;
	}

	/**
	 * @return the fellowsSurgeon
	 */
	public List<FellowsSurgeonVO> getFellowsSurgeon() {
		return fellowsSurgeon;
	}

	/**
	 * @param fellowsSurgeon the fellowsSurgeon to set
	 */
	public void setFellowsSurgeon(List<FellowsSurgeonVO> fellowsSurgeon) {
		this.fellowsSurgeon = fellowsSurgeon;
	}

	/**
	 * @return the programNotes
	 */
	public String getProgramNotes() {
		return programNotes;
	}

	/**
	 * @param programNotes the programNotes to set
	 */
	public void setProgramNotes(String programNotes) {
		this.programNotes = programNotes;
	}
	
	/**
	 * @return the repId
	 */
	public String getRepId() {
		return repId;
	}

	/**
	 * @param repId the repId to set
	 */
	public void setRepId(String repId) {
		this.repId = repId;
	}

	/**
	 * @return the repFirstNm
	 */
	public String getRepFirstNm() {
		return repFirstNm;
	}

	/**
	 * @param repFirstNm the repFirstNm to set
	 */
	public void setRepFirstNm(String repFirstNm) {
		this.repFirstNm = repFirstNm;
	}

	/**
	 * @return the repLastNm
	 */
	public String getRepLastNm() {
		return repLastNm;
	}

	/**
	 * @param repLastNm the repLastNm to set
	 */
	public void setRepLastNm(String repLastNm) {
		this.repLastNm = repLastNm;
	}

	/**
	 * @return the surgeonFirstNm
	 */
	public String getSurgeonFirstNm() {
		return surgeonFirstNm;
	}

	/**
	 * @param surgeonFirstNm the surgeonFirstNm to set
	 */
	public void setSurgeonFirstNm(String surgeonFirstNm) {
		this.surgeonFirstNm = surgeonFirstNm;
	}

	/**
	 * @return the surgeonLastNm
	 */
	public String getSurgeonLastNm() {
		return surgeonLastNm;
	}

	/**
	 * @param surgeonLastNm the surgeonLastNm to set
	 */
	public void setSurgeonLastNm(String surgeonLastNm) {
		this.surgeonLastNm = surgeonLastNm;
	}

}
