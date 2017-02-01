package com.depuy.events.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import com.depuy.events.vo.DePuyEventAddtlPostcardVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.event.vo.EventEntryVO;

/*****************************************************************************
 <p><b>Title</b>: DePuyEventEntryVO.java</p>
 <p>Data Bean that stores information for a single event</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Mar 9, 2006
 Code Updates
 James Camire, Mar 9, 2006 - Creating Initial Class File
 ***************************************************************************/

public class DePuyEventEntryVO extends EventEntryVO {
    private static final long serialVersionUID = 1l;
    
    private List<DePuyEventAddtlPostcardVO> addtlPostcards = new ArrayList<DePuyEventAddtlPostcardVO>();
    private String clinicName = null;
    private int showNameOnPostcardFlg = 0;
    private int featuredProductFlg = 0;
    private int focusMrktFlg = 0;
    private int showServiceFlg = 0;
    //NOTE shortDesc is used for eventMealType for DePuyLocalEvents website.
    private Integer attendanceCnt = null;
    private String surgeonBioText = null;

    
    public DePuyEventEntryVO() {
        super();
    }
    
    public DePuyEventEntryVO(ResultSet rs) {
        super(rs);
        DBUtil db = new DBUtil();
        showNameOnPostcardFlg = db.getIntVal("display_nm_flg", rs);
        featuredProductFlg = db.getIntVal("feat_prod_flg", rs);
        clinicName = db.getStringVal("clinic_nm", rs);
        focusMrktFlg = db.getIntVal("focus_mrkt_flg", rs);
        showServiceFlg = db.getIntVal("display_srvc_flg",rs);
        attendanceCnt = db.getIntegerVal("attendance_cnt", rs);
        surgeonBioText = db.getStringVal("surgeon_bio_txt", rs);
        db = null;
    }
    
    
    /**
     * Parses the request data and stores it in the appropriate variables
     * @param req
     */
	public DePuyEventEntryVO(ActionRequest req) {
		super(req); //call parent class
    	showNameOnPostcardFlg = Convert.formatInteger(req.getParameter("displayNameFlg"),0);
        featuredProductFlg = Convert.formatInteger(req.getParameter("featProductFlg"),0);
        clinicName = req.getParameter("clinicName");
        focusMrktFlg = Convert.formatInteger(req.getParameter("focusMrktFlg"),0);
        showServiceFlg = Convert.formatInteger(req.getParameter("showServiceFlg"),0);
        attendanceCnt = Convert.formatInteger(req.getParameter("attendanceCnt"));
        surgeonBioText = req.getParameter("surgeonBioText");
    }

 
    public String getEventDescFinal() {
    	if (getShowNameOnPostcardFlg() == 1) {
    		return "Local Orthopaedic Surgeon";
    	} else if (getShowNameOnPostcardFlg() == 2) {
    		return "Local Orthopaedic Surgeons";
    	} else {
    		return this.getEventDesc();
    	}
    }

	public String getClinicName() {
		return clinicName;
	}

	public int getFeaturedProductFlg() {
		return featuredProductFlg;
	}

	public int getShowNameOnPostcardFlg() {
		return showNameOnPostcardFlg;
	}

	public void setClinicName(String clinicName) {
		this.clinicName = clinicName;
	}

	public void setFeaturedProductFlg(Integer featuredProductFlg) {
		this.featuredProductFlg = featuredProductFlg;
	}

	public void setShowNameOnPostcardFlg(Integer showNameOnPostcardFlg) {
		this.showNameOnPostcardFlg = showNameOnPostcardFlg;
	}
	
	public List<DePuyEventAddtlPostcardVO> getAddtlPostcards() {
		List<DePuyEventAddtlPostcardVO> newCards = new ArrayList<DePuyEventAddtlPostcardVO>(addtlPostcards);
		
		//ensure always 4 results, even if empty (for jsp display iteration)
		switch (newCards.size()) {
			case 0: newCards.add(new DePuyEventAddtlPostcardVO());
			case 1: newCards.add(new DePuyEventAddtlPostcardVO());
			case 2: newCards.add(new DePuyEventAddtlPostcardVO());
			case 3: newCards.add(new DePuyEventAddtlPostcardVO());
		}
		return newCards;
	}
	
	public Integer getAddtlPostcardCount() {
		return addtlPostcards.size();
	}

	public void setAddtlPostcards(List<DePuyEventAddtlPostcardVO> addtlPostcards) {
		this.addtlPostcards = addtlPostcards;
	}

	public int getFocusMrktFlg() {
		return focusMrktFlg;
	}

	public void setFocusMrktFlg(Integer focusMrktFlg) {
		this.focusMrktFlg = focusMrktFlg;
	}

	public int getShowServiceFlg() {
		return showServiceFlg;
	}

	public void setShowServiceFlg(Integer showServiceFlg) {
		if (showServiceFlg == null) showServiceFlg = 0;
		this.showServiceFlg = showServiceFlg;
	}
	
	public String toString() {
		return super.toString();
	}

	public Integer getAttendanceCnt() {
		return attendanceCnt;
	}

	public void setAttendanceCnt(Integer attendanceCnt) {
		this.attendanceCnt = attendanceCnt;
	}
	
	public String getStatusName() { //used in the Roll-Up Report
		String status = "";
		Date today = Calendar.getInstance().getTime();
		
		if (getStartDate().before(today) && getStatusFlg() == 2) {
			status = "Completed";
		} else if (getStartDate().after(today) && getStatusFlg() == 1) {
			status = "Pending";
		} else if (getStatusFlg() == 4) {
			status = "Deleted";
		} else if (getStartDate().after(today) && getStatusFlg() == 2) {
			status = "Active";
		} else { //if (getStartDate().before(today)) {
			status = "Abandoned/Bogus";
		}
		return status;
	}

	public String getSurgeonBioText() {
		return surgeonBioText;
	}

	public void setSurgeonBioText(String surgeonBioText) {
		this.surgeonBioText = surgeonBioText;
	}

}
