package com.depuy.events.vo;

// JDK
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.db.DBUtil;

// SB Libs
import com.smt.sitebuilder.action.SBModuleVO;

/*****************************************************************************
 <p><b>Title</b>: DePuyEventAddtlPostcardVO.java</p>
 <p>Data Bean that stores information for a single event category</p>
 <p>Copyright: Copyright (c) 2000 - 2005 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 2.0
 @since Mar 9, 2006
 Code Updates
 James Camire, Mar 9, 2006 - Creating Initial Class File
 ***************************************************************************/

public class DePuyEventAddtlPostcardVO extends SBModuleVO {
    private static final long serialVersionUID = 1l;
 
    private String surgeonName = null;
    private int postcardQnty = 0;
    private String eventAddtlPostcardId = null;
    private String eventPostcardId = null;
    
    public DePuyEventAddtlPostcardVO() {
    	super();
    }
    
	
    public void setData(ResultSet rs) {
    	DBUtil db = new DBUtil();
        
        setSurgeonName(db.getStringVal("surgeon_nm", rs));
        setEventAddtlPostcardId(db.getStringVal("event_addtl_postcard_id", rs));
        setPostcardQnty(db.getIntVal("postcard_qnty_no", rs));
    	
        db = null;
    }

	public String getEventAddtlPostcardId() {
		return eventAddtlPostcardId;
	}
	public void setEventAddtlPostcardId(String eventAddtlPostcardId) {
		this.eventAddtlPostcardId = eventAddtlPostcardId;
	}
	public String getEventPostcardId() {
		return eventPostcardId;
	}
	public void setEventPostcardId(String eventPostcardId) {
		this.eventPostcardId = eventPostcardId;
	}
	public int getPostcardQnty() {
		return postcardQnty;
	}
	public void setPostcardQnty(int postcardQnty) {
		this.postcardQnty = postcardQnty;
	}
	public String getSurgeonName() {
		return surgeonName;
	}
	public void setSurgeonName(String surgeonName) {
		this.surgeonName = surgeonName;
	}
}
