package com.depuy.sitebuilder.datafeed;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: RTFReportAction.java <p/>
 * <b>Project</b>: SB_DePuy <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 7, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class RTFReportAction extends SBActionAdapter {

	/**
	 * 
	 */
	public RTFReportAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public RTFReportAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(ActionRequest req) throws ActionException {
		log.debug("Getting zip report");
		Date startDate = Convert.formatStartDate(req.getParameter("startDate"), "1/1/2000");
		Date endDate = Convert.formatEndDate(req.getParameter("endDate"));
		String zip = StringUtil.checkVal(req.getParameter("zipCode").replace(" ", ""));
		String[] zips = null;
		if (zip.indexOf(",") > 0) zips = zip.split(",");
		else zips = new String[] { zip };
		
		//allow data reported by state
		if (req.hasParameter("state")) {
			this.buildByState(req);
			return;
		}
		
		
		StringBuilder sb = new StringBuilder();
		sb.append("select zip_cd, attempt_month_no, attempt_year_no, count(*) ");
		sb.append("from data_feed.dbo.customer a inner join profile_address b  ");
		sb.append("on a.profile_id = b.profile_id ");
		sb.append("where a.attempt_dt between ? and ? and (");
		
		for (int i=0; i < zips.length; i++) {
			String tempZip = zips[i].replace("X", "%");
			tempZip = tempZip.replace("x", "%");
			tempZip = tempZip.replace("%%", "%");
			log.debug("****************: " + tempZip);
			if (i > 0) sb.append(" or ");
			sb.append("zip_cd like ").append(StringUtil.checkVal(tempZip, true)).append(" ");
		}
		
		sb.append(") ");
		sb.append("group by zip_cd, attempt_month_no, attempt_year_no ");
		sb.append("order by zip_cd, attempt_year_no desc, attempt_month_no desc");
		log.debug("RTL Zip SQL: " + sb + "|" + startDate + "|" + endDate);
		PreparedStatement ps = null;
		List<GenericVO> data = new ArrayList<GenericVO>();
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setDate(1, Convert.formatSQLDate(startDate));
			ps.setDate(2, Convert.formatSQLDate(endDate));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String mYear = rs.getInt(2) + "/1/" + rs.getInt(3); 
				Date d = Convert.formatDate(mYear);
				GenericVO gvo = new GenericVO(d, rs.getInt(4));
				GenericVO ele = new GenericVO(rs.getString(1), gvo);
				
				data.add(ele);
			}
			
			// Add the data to the container
			this.putModuleData(data, data.size(), false);
			
		} catch(Exception e) {
			log.error("Unable to retrieve Data Feed zip report", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}
	
	
	private void buildByState(ActionRequest req) throws ActionException {
		log.debug("Getting state report");
		Date startDate = Convert.formatStartDate(req.getParameter("startDate"), "1/1/2000");
		Date endDate = Convert.formatEndDate(req.getParameter("endDate"));
		String state = req.getParameter("state");
		
		StringBuilder sb = new StringBuilder();
		sb.append("select state_cd, attempt_month_no, attempt_year_no, count(*) ");
		sb.append("from data_feed.dbo.customer a inner join profile_address b  ");
		sb.append("on a.profile_id = b.profile_id ");
		sb.append("where state_cd=? ");
		sb.append("and a.attempt_dt between ? and ? ");
		sb.append("group by state_cd, attempt_month_no, attempt_year_no ");
		sb.append("order by state_cd, attempt_year_no desc, attempt_month_no desc");
		log.debug("RTL State SQL: " + sb + "|" + startDate + "|" + endDate);
		PreparedStatement ps = null;
		List<GenericVO> data = new ArrayList<GenericVO>();
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, state);
			ps.setDate(2, Convert.formatSQLDate(startDate));
			ps.setDate(3, Convert.formatSQLDate(endDate));
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				String mYear = rs.getInt(2) + "/1/" + rs.getInt(3); 
				Date d = Convert.formatDate(mYear);
				GenericVO gvo = new GenericVO(d, rs.getInt(4));
				GenericVO ele = new GenericVO(rs.getString(1), gvo);
				
				data.add(ele);
			}
			
			// Add the data to the container
			this.putModuleData(data, data.size(), false);
			
		} catch(Exception e) {
			log.error("Unable to retrieve Data Feed zip report", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
	}

}
