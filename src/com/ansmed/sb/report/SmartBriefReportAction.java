package com.ansmed.sb.report;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/*****************************************************************************
<p><b>Title</b>: SmartBriefReportAction.java</p>
<p>Description: <b/> Returns a report of who responded to an email send and
 when they responded.</p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Dave Bargerhuff
@version 1.0
@since May 13, 2009
Last Updated:
***************************************************************************/

public class SmartBriefReportAction extends SBActionAdapter {
	
	public SmartBriefReportAction() {
		
	}
	
	/**
	 * 
	 * @param arg0
	 */
	public SmartBriefReportAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * 
	 */
	public void update(SMTServletRequest req) throws ActionException {
		
		log.debug("Starting SmartBrief report retrieval...");

		StringBuffer sql = new StringBuffer();
		String schema = (String) this.getAttribute("customDbSchema");
		String instanceId = StringUtil.checkVal(req.getParameter("instanceId"));
		String start = req.getParameter("startDate");
		String end = req.getParameter("endDate");
		
		sql.append("select b.profile_id, a.region_nm, b.last_nm, b.first_nm, ");
		sql.append("c.campaign_instance_id, c.create_dt as send_dt, ");
		sql.append("d.create_dt as response_dt, d.response_type_id from ");
		sql.append(schema).append("ans_sales_region a ");
		sql.append("inner join ").append(schema).append("ans_sales_rep b "); 
		sql.append("on a.region_id = b.region_id inner join email_campaign_log c ");
		sql.append("on b.profile_id = c.profile_id left join email_response d ");
		sql.append("on c.campaign_log_id = d.campaign_log_id ");
		sql.append("where 1 = 1 ");
		sql.append("and c.campaign_instance_id = ? ");
		sql.append("and ((d.create_dt between ? and ?) or d.create_dt is null) ");
		sql.append("order by d.create_dt, region_nm, last_nm, first_nm ");
		
		log.debug("SmartBrief report SQL: " + sql.toString() +  " | " + instanceId);
		log.debug("SmartBrief report SQL params: " + instanceId + " | " 
				+ Convert.formatSQLDate(Convert.formatStartDate(start, "1/1/2000")) 
				+ " | " + Convert.formatSQLDate(Convert.formatEndDate(end)));
	
		PreparedStatement ps =  null;
		List<SmartBriefReportVO> data = new ArrayList<SmartBriefReportVO>();
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, instanceId);
			ps.setDate(2, Convert.formatSQLDate(Convert.formatStartDate(start, "1/1/2000")));
			ps.setDate(3, Convert.formatSQLDate(Convert.formatEndDate(end)));
			
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				data.add(new SmartBriefReportVO(rs));
			}
			
		} catch(SQLException sqle) {
			log.error("Error retrieving report data.", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}
		
		ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
		mod.setActionData(data);
		mod.setDataSize(data.size());
		
		req.setAttribute(Constants.REDIRECT_DATATOOL, Boolean.TRUE);
        this.setAttribute(AdminConstants.ADMIN_MODULE_DATA, mod);
        
		log.debug("Finished SmartBrief report retrieval...");
	
	}

}
