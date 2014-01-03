package com.depuy.events.vo.report;

// JDK 1.5.0
import java.util.List;
import java.util.ArrayList;
import java.util.Iterator;

import com.depuy.events.vo.DePuyEventPostcardVO;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

/*****************************************************************************
 <p><b>Title</b>: EventPostalLeadsReportVO.java</p>
 <p>Description: <b/>compiles leads data for postal and email sends</p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James McKain
 @version 1.0
 @since Nov 6, 2006
 ***************************************************************************/

public class EventEmailListReportVO extends AbstractSBReportVO {
    private static final long serialVersionUID = 1l;
    private List<UserDataVO> leads = new ArrayList<UserDataVO>();


    /**
     * 
     */
    public EventEmailListReportVO() {
        super();
        setContentType("text/plain");
        isHeaderAttachment(Boolean.TRUE);
        setFileName("Email-Addresses.txt");
    }
    
    /**
     * Assigns the event postcard data retrieved from the parent action
     * variables
     * @param data (List<DePuyEventPostcardVO>)
     * @throws SQLException
     */
    public void setData(Object o) {
    	DePuyEventPostcardVO pc = (DePuyEventPostcardVO) o; 
    	this.leads = pc.getLeadsData();
    }
    
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		StringBuffer rpt = new StringBuffer();
		Iterator<UserDataVO> iter = leads.iterator();
		UserDataVO vo = null;
		
		//subtract two days from earliest Event for RSVP date
		
		while (iter.hasNext()) {
			vo = (UserDataVO) iter.next();
			rpt.append(StringUtil.checkVal(vo.getEmailAddress()) + "\r");
			vo = null;
		}
		
		return rpt.toString().getBytes();
	}
	
}
