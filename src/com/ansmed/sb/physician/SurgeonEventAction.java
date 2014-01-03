package com.ansmed.sb.physician;

// JDk 1.5.0
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// SB Libs
import com.smt.sitebuilder.action.event.vo.EventEntryVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

// SMT Base Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.SMTMail;
import com.siliconmtn.util.UUIDGenerator;


/*****************************************************************************
 <p><b>Title</b>: SurgeonEventAction.java</p>
 <p>Description: <b/></p>
 <p>Copyright: Copyright (c) 2000 - 2006 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author James Camire
 @version 1.0
 @since Sep 8, 2007
 Last Updated:
 ***************************************************************************/

public class SurgeonEventAction extends SBActionAdapter {

	/**
	 * 
	 */
	public SurgeonEventAction() {
	}

	/**
	 * @param actionInit
	 */
	public SurgeonEventAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#build(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void build(SMTServletRequest req) throws ActionException {
		log.debug("building Surgeon Event");
		String message = "You have successfully assigned the events";
		String schema = (String)getAttribute("customDbSchema");
		String surgeonId = req.getParameter("surgeonId");
		String[] events = req.getParameterValues("eventId");
		if (events == null) events = new String[0];
		
		log.debug("Number of events: " + events.length);
		req.setAttribute(PhysicianDataFacadeAction.ANS_PHYSICIAN_MESSAGE, message);
		
		// Build the delete and the insert sql statements
		String del = "delete from " + schema + "ans_xr_event_surgeon where surgeon_id = ?";
		StringBuffer ins = new StringBuffer();
		ins.append("insert into ").append(schema).append("ans_xr_event_surgeon ");
		ins.append("(event_surgeon_id, surgeon_id, event_entry_id, create_dt) ");
		ins.append("values (?,?,?,?) ");
		
		PreparedStatement psDel = null;
		PreparedStatement psIns = null;
		try {
			// Delete the existing records
			psDel = dbConn.prepareStatement(del);
			psDel.setString(1, surgeonId);
			psDel.executeUpdate();
			
			// Insert the new records
			psIns = dbConn.prepareStatement(ins.toString());
			
			for (int i=0; i < events.length; i++) {
				psIns.setString(1, new UUIDGenerator().getUUID());
				psIns.setString(2, surgeonId);
				psIns.setString(3, events[i]);
				psIns.setTimestamp(4, Convert.getCurrentTimestamp());
				
				// add to the batch
				psIns.addBatch();
				
				// send the email message 
				try {
					this.sendEmailMessage(events[i], surgeonId);
				} catch (Exception e) {
					log.error("Unable to send email message", e);
				}
			}
			
			// Execute the batch
			psIns.executeBatch();
		} catch(SQLException sqle) {
			message = "Unable to assign events";
			log.error("Error retrieving ans sales areas", sqle);
		} finally {
			try {
				psIns.close();
			} catch (Exception e) {}
			
			try {
				psDel.close();
			} catch (Exception e) {}
		}
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#list(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		log.debug("retrieving Physician Events");
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		String schema = (String)getAttribute("customDbSchema");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		StringBuffer sql = new StringBuffer();
		sql.append("select b.* from ");
		sql.append(schema).append("ans_xr_event_surgeon a inner join event_entry b ");
		sql.append("on a.event_entry_id = b.event_entry_id ");
		sql.append("inner join event_group c on b.action_id = c.action_id ");
		sql.append("where organization_id = ? and c.action_id = ? ");
		
		log.debug("ANS Surgeon Event SQL: " + sql);
		log.debug("Attrib: " + mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		// Retrieve the data and store into a Map
		List<EventEntryVO> data = new ArrayList<EventEntryVO>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, site.getOrganizationId());
			ps.setString(2, (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.add(new EventEntryVO(rs));
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving ans sales areas", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
		log.debug("Size: " + data.size());
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.debug("retrieving Physician Events");
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		String schema = (String)getAttribute("customDbSchema");
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		StringBuffer sql = new StringBuffer();
		sql.append("select a.event_entry_id from ");
		sql.append(schema).append("ans_xr_event_surgeon a inner join event_entry b ");
		sql.append("on a.event_entry_id = b.event_entry_id ");
		sql.append("inner join event_group c on b.action_id = c.action_id ");
		sql.append("where organization_id = ? and c.action_id = ? ");
		
		log.debug("ANS Surgeon Event SQL: " + sql);
		log.debug("Attrib: " + mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		
		// Retrieve the data and store into a Map
		Map<String, String> data = new HashMap<String, String>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, site.getOrganizationId());
			ps.setString(2, (String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				data.put(rs.getString(1), rs.getString(1));
			}
		} catch(SQLException sqle) {
			log.error("Error retrieving ans sales areas", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		// Store the retrieved data in the ModuleVO.actionData and replace into
		// the Map
		mod.setActionData(data);
		attributes.put(Constants.MODULE_DATA, mod);
		log.debug("Size: " + data.size());
	}

	
	/**
	 * Retrieves the physician info for the email send
	 * @param surgeonId
	 * @return
	 */
	protected SurgeonVO getSurgeonData(String surgeonId) {
		StringBuffer sql = new StringBuffer();
		String schema = (String)getAttribute("customDbSchema");
		sql.append("select a.*, specialty_nm from ").append(schema);
		sql.append("ans_surgeon a inner join ").append(schema);
		sql.append("ans_specialty b on a.specialty_id = b.specialty_id ");
		sql.append("where surgeon_id = ? ");
		log.debug("Surgeon data for the email send: " + sql + "|" + surgeonId);
		
		SurgeonVO surgeon  = new SurgeonVO();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				surgeon = new SurgeonVO(rs);
			}
		} catch(Exception e) {
			log.error("Error retrieving phys info for email send: " + sql, e);
		} finally {
			try {
				ps.close();
			}catch(Exception e) { }
		}
		
		return surgeon;
	}
	
	/**
	 * Retrieves the latest Business Plan data for the physician
	 * @param surgeonId
	 * @return
	 */
	protected List<String> getBPData(String surgeonId) {
		StringBuffer sql = new StringBuffer();
		String schema = (String)getAttribute("customDbSchema");
		sql.append("select field_nm, category_nm, a.* from ").append(schema);
		sql.append("ans_xr_surgeon_busplan a inner join ").append(schema);
		sql.append("ans_business_plan b on a.business_plan_id = b.business_plan_id ");
		sql.append("inner join ").append(schema).append("ans_bp_category c ");
		sql.append("on b.category_id = c.category_id where surgeon_id = ? ");
		sql.append("and b.category_id < 5 ");
		sql.append("and bp_year_no = (select max(bp_year_no) from ").append(schema);
		sql.append("ans_xr_surgeon_busplan where surgeon_id = ?) ");
		sql.append("order by b.category_id ");
		log.debug("BP data for the email send: " + sql + "|" + surgeonId);
		
		List<String> data  = new ArrayList<String>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, surgeonId);
			ps.setString(2, surgeonId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Boolean selected = Convert.formatBoolean(rs.getInt("selected_flg"));
				StringBuffer sb = new StringBuffer("<br/>");
				sb.append(rs.getString("category_nm")).append(" - ");
				sb.append(rs.getString("field_nm")).append(": ");
				sb.append("selected = ").append(selected).append(" : ");
				sb.append(rs.getString("value_txt")).append("\r\n");
				
				// Add the line item to the List
				data.add(sb.toString());
			}
		} catch(Exception e) {
			log.debug("Error retrieving phys info for email send: ", e);
		} finally {
			try {
				ps.close();
			}catch(Exception e) { }
		}
		
		return data;
	}
		
	/**
	 * Retrieves the RSD email for the provided region
	 * @param region
	 * @return
	 */
	protected List<String> getRSDEmail(String repId) {
		StringBuffer sql = new StringBuffer();
		String schema = (String)getAttribute("customDbSchema");
		sql.append("select email_address_txt from ").append(schema);
		sql.append("ans_sales_rep where sales_rep_id = ? or sales_rep_id in (");
		sql.append("select sales_rep_id from ").append(schema);
		sql.append("ans_sales_rep a inner join role b  ");
		sql.append("on a.role_id = b.role_id where region_id = ( ");
		sql.append("select region_id from ").append(schema).append("ans_sales_rep ");
		sql.append("where sales_rep_id = ?)	and role_nm = 'RSD' ) ");
		log.debug("RSD Email Info SQL: " + sql + "|" + repId);
		
		List<String> email  = new ArrayList<String>();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, repId);
			ps.setString(2, repId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				email.add(rs.getString(1)); 
			}
		} catch(Exception e) {
			log.error("Error retrieving RSD info for email send: " + sql, e);
		} finally {
			try {
				ps.close();
			}catch(Exception e) { }
		}
		
		return email;
	}
	
	
	/**
	 * Retrieves the Event information for the provided region
	 * @param region
	 * @return
	 */
	protected EventEntryVO getEventInfo(String eventId) {
		StringBuffer sql = new StringBuffer();
		sql.append("select * from event_entry where event_entry_id = ?");
		log.debug("Event data for the email send: " + sql + "|" + eventId);
		
		EventEntryVO event  = new EventEntryVO();
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, eventId);
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				event = new EventEntryVO(rs);
			
		} catch(Exception e) {
			log.error("Error retrieving RSD info for email send: " + sql, e);
		} finally {
			try {
				ps.close();
			}catch(Exception e) { }
		}
		
		return event;
	}
	
	/**
	 * Sends the email message for physicians signed up for the event
	 * @param eventId
	 * @param surgeonId
	 * @throws MailException
	 */
	protected void sendEmailMessage(String eventId, String surgeonId) 
	throws MailException {
		
		// get the data and format the message
		EventEntryVO event = getEventInfo(eventId);
		SurgeonVO surgeon = getSurgeonData(surgeonId);
		List<String> bpData = getBPData(surgeonId);
		StringBuffer sb = getMergeMessage(event, surgeon, bpData);
		
		// Set the recipients.  Add Amanda Wright to the list
		List<String> rcpt = this.getRSDEmail(surgeon.getSalesRepId());
		rcpt.add("amanda.wright@sjmneuro.com");
		String[] recipients = new String[rcpt.size()];
		for (int i=0; i < rcpt.size(); i++) {
			recipients[i] = rcpt.get(i);
			log.debug("Recipient: " + recipients[i]);
		} 
		
		// Send the email
		SMTMail mail = new SMTMail();
		mail.setSmtpServer((String)getAttribute(Constants.CFG_SMTP_SERVER));
		mail.setUser((String)getAttribute(Constants.CFG_SMTP_USER));
		mail.setPassword((String)getAttribute(Constants.CFG_SMTP_PASSWORD));
		mail.setPort(Convert.formatInteger((String)getAttribute(Constants.CFG_SMTP_PORT), 25));
		mail.setSubject("Submission Acknowledgement for an SJM event");
		mail.setFrom("amanda.wright@sjmneuro.com", "ANS Corporate");
		mail.setRecpt(recipients);
		//mail.setRecpt(new String[] {"james@siliconmtn.com" });
		mail.setHtmlBody(sb.toString());
		mail.postMail();
		log.debug("Email Message sent");
	}
	
	/**
	 * Merges the data to the email message
	 * @param event
	 * @param surgeon
	 * @param bpData
	 * @return
	 */
	public StringBuffer getMergeMessage(EventEntryVO event, SurgeonVO surgeon, List<String> bpData) {
		
		String msg = getBaseMessage();
		msg = msg.replaceAll("#surgeon_name#", surgeon.getFirstName() + " " + surgeon.getLastName());
		msg = msg.replaceAll("#surgeon_title#", surgeon.getTitle());
		msg = msg.replaceAll("#surgeon_specialty#", surgeon.getSpecialtyName());
		msg = msg.replaceAll("#event_name#", event.getEventName());
		msg = msg.replaceAll("#event_location#", event.getLocationDesc() + "," + event.getCityName() + "," + event.getStateCode());
		msg = msg.replaceAll("#event_date#", Convert.formatDate(event.getStartDate(), Convert.DATE_SLASH_PATTERN));
		StringBuffer sb = new StringBuffer(msg);
		
		// Loop the BP Data
		for (int i = 0; i < bpData.size(); i++) {
			sb.append(bpData.get(i));
		}
		
		// add the footer
		sb.append("<p>&nbsp;</p>\r\n");
		sb.append(getFooter());
		return sb;
	}
	
	/**
	 * Text message to be sent to the recipients
	 * @return
	 */
	public String getBaseMessage() {
		StringBuffer sb = new StringBuffer();
		sb.append("<p>You have submitted #surgeon_name#, #surgeon_title#, ");
		sb.append("#surgeon_specialty# for consideration to be invited to the ");
		sb.append("#event_name#, #event_location#, #event_date#. Several ");
		sb.append("factors are used to determine the final list of physicians ");
		sb.append("that are invited to attend an SJM meeting. Please note just ");
		sb.append("because you have submitted the physician there is no ");
		sb.append("guarantee of his/her being invited to the event. </p>\r\n");
		sb.append("<p>Some data that is reviewed is: (first 4 sections of the business plan)</p>\r\n");
		return sb.toString();
	}
	
	/**
	 * Message footer
	 * @return
	 */
	public String getFooter() {
		StringBuffer sb = new StringBuffer();
		sb.append("<p>If you have left any of the above questions blank within the ");
		sb.append("database, please enter the data within the database. Any ");
		sb.append("questions regarding this process, you should contact your manager.</p>\r\n");
		
		return sb.toString();
	}
}
