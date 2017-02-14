package com.ansmed.sb.physician;

// JDK 1.5.0
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMT Base Libs 2.0
//import com.ansmed.sb.security.ANSRoleFilter;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// SB Libs
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/*****************************************************************************
<p><b>Title</b>: FellowsAction.java</p>
<p>Description: <b/></p>
<p>Copyright: Copyright (c) 2000 - 2009 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author David Bargerhuff
@version 1.0
@since Feb 18, 2009
Last Updated:
 ***************************************************************************/

public class FellowsAction extends SBActionAdapter {


	/**
	 * 
	 */
	public FellowsAction() {

	}

	/**
	 * @param actionInit
	 */
	public FellowsAction(ActionInitVO actionInit) {
		super(actionInit);

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("Build FellowsAction...");

		String deleteEle = StringUtil.checkVal(req.getParameter("deleteEle"));
		if (deleteEle.equalsIgnoreCase("true")) {
			delete(req);
		} else {
			update(req);
		}
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		String schema = (String)getAttribute("customDbSchema");
		String type = StringUtil.checkVal(req.getParameter("processType"));
		String fellowsId = StringUtil.checkVal(req.getParameter("fellowsId"));
		String typeId = "";
		String message = "";

		StringBuffer del = new StringBuffer();

		if (type.equalsIgnoreCase("goal")) {

			message = "You have successfully removed the Fellows objective information.";
			typeId = StringUtil.checkVal(req.getParameter("fellowsGoalId"));
			del.append("delete from ").append(schema).append("ans_fellows_goal ");
			del.append("where fellows_goal_id = ? and fellows_id = ?");

		} else if (type.equalsIgnoreCase("fellowsSurgeon")) {

			message = "You have successfully removed the Fellows surgeon information.";
			typeId = StringUtil.checkVal(req.getParameter("fellowsSurgeonId"));
			del.append("delete from ").append(schema).append("ans_fellows_surgeon ");
			del.append("where fellows_surgeon_id = ? and fellows_id = ?");

		} else {

			message = "Error: Invalid process type specified for this delete action.";
			req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
			return;

		}

		log.info("Fellows delete SQL: " + del + "|" + typeId + "|" + req.getParameter("fellowsId"));
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(del.toString());
			ps.setString(1, typeId);
			ps.setString(2, fellowsId);

			// Execute the delete
			ps.executeUpdate();

		} catch(SQLException sqle) {
			log.error("Error deleting physician's Fellows program data.", sqle);
		} finally {
			try {
				ps.close();
			} catch(Exception e) {}
		}

		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		//ANSRoleFilter filter = new ANSRoleFilter();

		log.debug("Retrieving Fellows program data...");
		log.debug("Role: " + role);

		String surgeonId = req.getParameter("surgeonId");
		String schema = (String)getAttribute("customDbSchema");

		// Retrieve program and goals
		StringBuffer sql = new StringBuffer();
		sql.append("select a.*, b.* from ").append(schema);
		sql.append("ans_fellows a left join ").append(schema);
		sql.append("ans_fellows_goal b on a.fellows_id = b.fellows_id ");
		sql.append("where a.surgeon_id = ?");

		log.debug("Fellows program/goal SQL: " + sql.toString() + " | " + surgeonId);

		PreparedStatement ps = null;
		FellowsVO fvo = new FellowsVO();

		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);

			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				fvo.setData(rs);
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving physician's Fellows program and goal data.", sqle);
		}

		//Retrieve the surgeon fellows for associated with this program
		ps = null;
		sql = new StringBuffer();
		sql.append("select b.* from ").append(schema).append("ans_fellows a left join ");
		sql.append(schema).append("ans_fellows_surgeon b on a.fellows_id = b.fellows_id ");
		sql.append("where a.surgeon_id = ?");

		log.debug("Fellows surgeon SQL: " + sql.toString() + " | " + surgeonId);

		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				fvo.addFellowsSurgeon(new FellowsSurgeonVO(rs));
			}

		} catch (SQLException sqle) {
			log.error("Error retrieving physician's Fellows surgeon information.", sqle);
		}

		//Retrieve the surgeons for this rep who are "Fellows" (surgeon_type_id = 2)
		List<SurgeonVO> sv = new ArrayList<>();
		ps = null;
		sql = new StringBuffer();
		sql.append("select a.* from ").append(schema).append("ans_surgeon a ");
		sql.append("where sales_rep_id  = (select distinct(sales_rep_id) from ");
		sql.append(schema).append("ans_surgeon where surgeon_id = ?) ");
		sql.append("and surgeon_type_id = 2 and status_id < 10 order by last_nm");

		log.debug("Surgeon search SQL: " + sql.toString());

		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ResultSet rs = ps.executeQuery();

			while (rs.next()) {
				sv.add(new SurgeonVO(rs));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving 'Fellows' surgeons list.", sqle);
		}

		try {
			ps.close();
		} catch(Exception e) {}

		log.debug("FellowsVO goal size: " + fvo.getFellowsGoal().size());
		log.debug("SurgeonVO size: " + sv.size());

		//Put the data on the ModuleVO and on the request
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		mod.setActionData(fvo);
		req.setAttribute("fellowsList", sv);
		attributes.put(Constants.MODULE_DATA, mod);

	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#update(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		log.debug("Updating Fellows program/goal/surgeon information...");

		String schema = (String)getAttribute("customDbSchema");
		String message = "You have successfully updated the Fellows information.";	
		String surgeonId = StringUtil.checkVal(req.getParameter("surgeonId"));	

		String type = StringUtil.checkVal(req.getParameter("processType"));
		String typeId = "";
		String fellowsId = StringUtil.checkVal(req.getParameter("fellowsId"));
		String[] reps = null;

		log.debug("processType: " + type);

		// If trying to insert a goal or surgeon without a fellowsId value, return
		// with an error msg.
		if (fellowsId.length() <= 0 && (type.equalsIgnoreCase("goal") || type.equalsIgnoreCase("fellowsSurgeon"))) {
			message = "Please add a program before attempting to add a program objective or fellow.";
			req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
			return;
		}

		StringBuffer sql = new StringBuffer();
		if (type.equalsIgnoreCase("program")) {
			if (fellowsId.length() > 0) { //update
				sql.append("update ").append(schema).append("ans_fellows set ");
				sql.append("program_nm = ?, program_notes_txt = ?, ");
				sql.append("coord_nm = ?, coord_phone_no = ?, coord_email_txt = ?, ");
				sql.append("create_dt = ?, surgeon_id = ? where fellows_id = ?");
			} else { //insert
				fellowsId = new UUIDGenerator().getUUID();
				sql.append("insert into ").append(schema);
				sql.append("ans_fellows (program_nm, program_notes_txt, ");
				sql.append("coord_nm, coord_phone_no, coord_email_txt, create_dt, ");
				sql.append("surgeon_id, fellows_id) values (?,?,?,?,?,?,?,?)");
			}
		} else if (type.equalsIgnoreCase("goal")) {
			typeId = StringUtil.checkVal(req.getParameter("fellowsGoalId"));
			if (typeId.length() > 0) { //update 
				sql.append("update ").append(schema).append("ans_fellows_goal ");
				sql.append("set program_needs_txt = ?, program_goal_txt = ?, ");
				sql.append("program_action_txt = ?, program_month_no = ?, ");
				sql.append("program_yr_no = ?, update_dt = ? ");
				sql.append("where fellows_goal_id = ? and fellows_id = ?");
			} else { //insert
				typeId = new UUIDGenerator().getUUID();
				sql.append("insert into ").append(schema);
				sql.append("ans_fellows_goal (program_needs_txt, program_goal_txt, ");
				sql.append("program_action_txt, program_month_no, program_yr_no, create_dt, ");
				sql.append("fellows_goal_id, fellows_id) values (?,?,?,?,?,?,?,?)");
			}
		} else if (type.equalsIgnoreCase("fellowsSurgeon")) {
			typeId = StringUtil.checkVal(req.getParameter("fellowsSurgeonId"));
			reps = req.getParameterValues("fellowsRepNames");
			if (typeId.length() > 0) { //update
				sql.append("update ").append(schema).append("ans_fellows_surgeon ");
				sql.append("set fellows_nm = ?, fellows_specialty_id = ?, ");
				sql.append("fellows_email_addr_txt = ?, fellows_phone_no = ?, ");
				sql.append("fellows_start_month_no = ?, fellows_start_yr_no = ?, ");
				sql.append("fellows_end_month_no = ?, fellows_end_yr_no = ?, ");
				sql.append("fellows_plan_txt = ?, fellows_ed_txt = ?, ");
				sql.append("fellows_rep_id= ?, update_dt = ? ");
				sql.append("where fellows_surgeon_id = ? and fellows_id = ?");
			} else { //insert
				typeId = new UUIDGenerator().getUUID();
				sql.append("insert into ").append(schema).append("ans_fellows_surgeon ");
				sql.append("(fellows_nm, fellows_specialty_id, fellows_email_addr_txt, ");
				sql.append("fellows_phone_no, fellows_start_month_no, ");
				sql.append("fellows_start_yr_no, fellows_end_month_no, ");
				sql.append("fellows_end_yr_no, fellows_plan_txt, fellows_ed_txt, ");
				sql.append("fellows_rep_id, create_dt, fellows_surgeon_id, ");
				sql.append("fellows_id) values (?,?,?,?,?,?,?,?,?,?,?,?,?,?)");
			}
		} else {
			message = "Error: Invalid process type specified for this update action.";
			req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
			return;
		}

		log.debug("FellowsAction SQL: " + sql.toString());

		PreparedStatement ps = null;
		try {
			dbConn.setAutoCommit(true);
			ps = dbConn.prepareStatement(sql.toString());
			if (type.equalsIgnoreCase("program")) {
				ps.setString(1,req.getParameter("programNm"));
				ps.setString(2,req.getParameter("programNotes"));
				ps.setString(3,req.getParameter("coordNm"));
				String phone = StringUtil.checkVal(req.getParameter("coordPhone"));
				if (phone.length() > 0) {
					StringEncoder.removeNonAlphaNumeric(phone);
				}
				ps.setString(4,req.getParameter("coordPhone"));
				ps.setString(5,req.getParameter("coordEmail"));
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.setString(7, surgeonId);
				ps.setString(8, fellowsId);

			} else if (type.equalsIgnoreCase("goal")) {
				ps.setString(1, req.getParameter("programNeeds"));
				ps.setString(2,req.getParameter("fellowsGoal"));
				ps.setString(3,req.getParameter("fellowsAction"));
				ps.setInt(4,Convert.formatInteger(req.getParameter("fellowsGoalMonth")));
				ps.setInt(5,Convert.formatInteger(req.getParameter("fellowsGoalYear")));
				ps.setTimestamp(6, Convert.getCurrentTimestamp());
				ps.setString(7, typeId);
				ps.setString(8, fellowsId);

			} else if (type.equalsIgnoreCase("fellowsSurgeon")){
				ps.setString(1, req.getParameter("fellowsNm"));
				ps.setString(2, req.getParameter("specialtyId"));
				ps.setString(3, req.getParameter("fellowsEmail"));
				ps.setString(4, req.getParameter("fellowsPhone"));
				ps.setString(5, req.getParameter("fellowsStartMonth"));
				ps.setString(6, req.getParameter("fellowsStartYear"));
				ps.setString(7, req.getParameter("fellowsEndMonth"));
				ps.setString(8, req.getParameter("fellowsEndYear"));
				ps.setString(9, req.getParameter("fellowsPlan"));
				ps.setString(10, req.getParameter("fellowsEd"));
				ps.setString(11, packVals(reps));
				ps.setTimestamp(12, Convert.getCurrentTimestamp());
				ps.setString(13, typeId);
				ps.setString(14, fellowsId);
			}

			int count = ps.executeUpdate();
			if (count == 0) message = "Unable to update the Fellows program information.";

		} catch(SQLException sqle) {
			sqle.printStackTrace();
			log.error("Error updating Physician's Fellows program information.", sqle);
			message = "Error updating the physician's Fellows program information.";
		} 
		try {
			ps.close();
		} catch(Exception e) {}

		// Check to see if we need to email any reps.
		if (type.equalsIgnoreCase("fellowsSurgeon")) {
			String notifyRep = req.getParameter("notifyRep");
			if (notifyRep != null && notifyRep.equalsIgnoreCase("on")) {
				log.debug("Notify rep is 'on' - need to send email.");
				//log.debug("Sending RSD email notification...);
				FellowsSurgeonVO vo = new FellowsSurgeonVO(req);
				try {
					this.sendEmail(req, vo, reps);
				} catch (MailException m) {
					log.error("Error sending RSD notification email.", m);
				}
			}
		}

		// Add the message to the req object
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);

	}

	/**
	 * Returns comma-delimited String given the String[] array passed in.
	 * @param vals
	 * @return
	 */
	public String packVals(String[] vals) {

		StringBuffer sb = new StringBuffer("");
		if (vals != null && vals.length > 0) {
			for (int i = 0; i < vals.length; i++) {
				if (vals[i].length() > 0) sb.append(vals[i]);
				if ((i + 1) < vals.length) sb.append(",");
			}
		}
		return sb.toString();
	}

	public void sendEmail(ActionRequest req, FellowsSurgeonVO fsvo, String[] reps) throws MailException {

		//No reps, no email.
		if (reps == null || reps.length == 0 || (reps.length == 1 && reps[0].length() == 0)) return;

		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);

		String schema = (String)getAttribute("customDbSchema");

		StringBuffer sql = new StringBuffer();
		sql.append("select first_nm, last_nm, email_address_txt ");
		sql.append("from ").append(schema).append("ans_sales_rep ");
		sql.append("where sales_rep_id in (");
		for (int i = 0; i < reps.length; i++) {
			if (i > 0) sql.append(",");
			if (reps[i] != null && reps[i].length() > 0) {
				sql.append(StringUtil.checkVal(reps[i],true));
			}
		}
		sql.append(") order by last_nm, first_nm");

		log.debug("sendEmail SQL: " + sql.toString());

		List<String> names = new ArrayList<String>();
		List<String> emailTo = new ArrayList<String>();

		PreparedStatement ps = null;

		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				names.add(rs.getString("first_nm") + " " + rs.getString("last_nm"));
				//log.debug("Rep: " + rs.getString("first_nm") + " " + rs.getString("last_nm"));
				emailTo.add(rs.getString("email_address_txt"));
				//log.debug("Email: " + rs.getString("email_address_txt"));
			}	
		} catch (SQLException sqle) {
			log.error("Error retrieving Fellows surgeon data for email notification.", sqle);
		}

		Map<Integer,String> specialty = new HashMap<Integer,String>();

		StringBuffer ssql = new StringBuffer();
		ssql.append("select specialty_id, specialty_nm from ").append(schema);
		ssql.append("ans_specialty");
		ps = null;

		try {
			ps = dbConn.prepareStatement(ssql.toString());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				specialty.put(rs.getInt("specialty_id"), rs.getString("specialty_nm"));
			}
		} catch (SQLException sqle) {
			log.error("Error retrieving specialty id's and names.", sqle);
		}
		try {
			ps.close();
		} catch(Exception e) {}

		StringBuffer msg = new StringBuffer();
		msg.append("<table style=\"width:750px;border:solid 1px black;\">");
		msg.append("<tr style=\"background:#E1EAFE;\"><th colspan='2'>").append("SJM Surgeon Fellows Notification");
		msg.append("</th></tr>");
		msg.append("<tr style=\"background:#c0d2ec;\"><td style=\"padding-right:10px;\">Website");
		msg.append("</td><td>");
		msg.append(site.getSiteName()).append("</td></tr>");

		String color="#e1eafe";
		String color1 = "#c0d2ec";

		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Physician Fellow Name").append("</td><td>").append(fsvo.getFellowsNm()).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color1);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Specialty").append("</td><td>").append(specialty.get(fsvo.getSpecialtyId())).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Email Address").append("</td><td>").append(fsvo.getFellowsEmail()).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color1);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Phone Number").append("</td><td>").append(fsvo.getFellowsPhone()).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Program Start (month/year)").append("</td><td>").append(this.getCalendarMonthName(fsvo.getFellowsStartMonth() - 1));
		msg.append(", ").append(fsvo.getFellowsStartYear()).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color1);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Program End (month/year)").append("</td><td>").append(this.getCalendarMonthName(fsvo.getFellowsEndMonth() - 1));
		msg.append(", ").append(fsvo.getFellowsEndYear()).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Post Graduation Plans").append("</td><td>").append(fsvo.getFellowsPlan()).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color1);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("Education Provided").append("</td><td>").append(fsvo.getFellowsEd()).append("&nbsp;</td></tr>");

		msg.append("<tr style=\"background:").append(color);
		msg.append(";\"><td style=\"padding-right:10px;\" nowrap valign=\"top\">");
		msg.append("RSD(s)").append("</td><td>");
		for (int i = 0; i < names.size(); i++) {
			msg.append(names.get(i));
			if ((i + 1) < names.size()) msg.append(", ");
		}
		msg.append("&nbsp;</td></tr>");

		msg.append("</table>");
		msg.append("<br>");

		SMTMail mail = new SMTMail((String)getAttribute(Constants.CFG_SMTP_SERVER));
		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
		mail.setRecpt(emailTo.toArray(new String[]{}));
		//mail.setRecpt(new String[]{"dave@siliconmtn.com"});
		mail.setSubject("The SalesNet - Program Fellows Add/Update Notification");
		mail.setFrom(site.getMainEmail());
		mail.setHtmlBody(msg.toString());

		log.debug("Mail Info: " + mail.toString());
		mail.postMail();

	}

	/**
	 * Returns month name based on Calendar month int passed as param.
	 * @param month
	 * @return
	 */
	private String getCalendarMonthName(int month) {

		String monthName = "";

		switch (month) {
			case 0:  monthName = "January"; break;
			case 1:  monthName = "February"; break;
			case 2:  monthName = "March"; break;
			case 3:  monthName = "April"; break;
			case 4:  monthName = "May"; break;
			case 5:  monthName = "June"; break;
			case 6:  monthName = "July"; break;
			case 7:  monthName = "August"; break;
			case 8:  monthName = "September"; break;
			case 9:  monthName = "October"; break;
			case 10: monthName = "November"; break;
			case 11: monthName = "December"; break;
			default: monthName = "Invalid month."; break; 
		}

		return monthName;
	}
}
