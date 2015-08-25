package com.depuysynthes.scripts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.depuysynthesinst.DSIUserDataVO.RegField;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.MailHandlerFactory;
import com.siliconmtn.io.mail.mta.MailTransportAgentIntfc;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: DSIGraduationWorkflow.java<p/>
 * <b>Description:</b>  Find Residents & Fellows who graduate 'today' and if they 
 * have survey responses (emailed questionnaire that updates registration data), 
 * roll those values over onto their parent fields.
 * 	DSI_SRVY_FELLOW_OBTD - not used
	DSI_SRVY_SPECIALTY - moved to RegField.Specialty
	DSI_SRVY_FELLOW_START_DT - not used
	DSI_SRVY_FELLOW_END_DT - moved to RegField.DSI_GRAD_DT
	DSI_SRVY_CONT_ED - not used
	DSI_SRVY_CONT_ED_DESC - not used
	DSI_SRVY_SEC_JOB - not used, but used to determine if we're looking at a Fellow's conversion
	DSI_SRVY_EMP_NM - moved to RegField.DSI_ACAD_NM
	DSI_SRVY_EMP_ADDR - moved to RegField.DSI_ACAD_ADDR
	DSI_SRVY_EMP_CITY - moved to RegField.DSI_ACAD_CITY
	DSI_SRVY_EMP_STATE - moved to RegField.DSI_ACAD_ST
	DSI_SRVY_EMP_ZIP - moved to RegField.DSI_ACAD_ZIP
	DSI_SRVY_JOB_DT - not used
	DSI_SRVY_CONTACT_FLG - not used
	
	Intended outcome: The next time the user logs-in they'll be a Fellow instead of a Resident
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 29, 2015
 ****************************************************************************/
public class DSIGraduationWorkflow extends CommandLineUtil {

	private String gradDateStr; // MM/DD/YYYY formatted graduation date used in the lookup query 
	
	/**
	 * List of errors 
	 */
	List <Exception> failures = new ArrayList<Exception>();
	
	/**
	 * @param args
	 */
	public DSIGraduationWorkflow(String[] args) {
		super(args);
		loadProperties("scripts/dsi/workflow.properties");
		super.loadDBConnection(props);
		if (args.length > 0) {
			gradDateStr = args[0]; 
		} else {
			gradDateStr = Convert.formatDate(new Date(), Convert.DATE_SLASH_PATTERN);
		}
	}
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {        
		//Create an instance of the DSIGraduationWorkflow
		DSIGraduationWorkflow klass = new DSIGraduationWorkflow(args);
		klass.run();
		klass.closeDBConnection();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		log.info("starting script for " + gradDateStr);
		
		//find registration accounts that are Residents, Cheifs, and Fellows that graduate 'today'
		Map<String,List<DataVO>> accounts = loadRegistrations();
		
		int cnt = 0;
		//for each account, move the desired fields
		for (String rsId : accounts.keySet()) {
			//loop the fields and see if they're a converting user (Resident to Fellow)
			boolean converting = false;
			for (DataVO vo : accounts.get(rsId)) {
				//if they have the Profession field, they're converting!  This logic resides in the transposeData method
				if (vo.profileFieldId.equals(RegField.c0a80241b71c9d40a59dbd6f4b621260.toString())) {
					converting = true;
					break;
				}
			}
			if (!converting) continue;
			
			try {
				saveAccount(accounts.get(rsId));
				++cnt;
			} catch (SQLException sqle) {
				log.error("could not update account for " + rsId, sqle);
			}
			
			try {
				deleteOldSrvyResponses(accounts.get(rsId));
				++cnt;
			} catch (SQLException sqle) {
				log.error("could not update account for " + rsId, sqle);
			}
		}
		
		//noitify the admin we did what something
		sendAdminEmail(accounts.size(), cnt);
	}

	
	/**
	 * grab all registration accounts for user who are of specific professions 
	 * and have a graduation date of today
	 * @return
	 */
	private Map<String,List<DataVO>> loadRegistrations() {
		Map<String,List<DataVO>> regData = new HashMap<String,List<DataVO>>();
		StringBuilder sql = new StringBuilder(400);
		sql.append("select rd.value_txt, rd.register_field_id, rd.register_submittal_id, prof.value_txt as 'profession' ");
		sql.append("from register_data rd ");
		sql.append("inner join register_submittal rs on rd.register_submittal_id=rs.register_submittal_id and rs.site_id=? ");
		sql.append("inner join register_data prof on rs.register_submittal_id=prof.register_submittal_id and prof.register_field_id=? and prof.value_txt in (?,?,?) ");
		sql.append("inner join register_data grad on rs.register_submittal_id=grad.register_submittal_id and grad.register_field_id=? and grad.value_txt=? ");
		//sql.append("where rd.register_field_id like 'DSI_SRVY_%' "); //we only need the survey responses
		sql.append("order by rd.register_submittal_id");
		log.debug(sql);
		
		List<DataVO> acctData = null;
		String rsid = null;
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, "DPY_SYN_INST_1"); // siteId for main site
			ps.setString(2, RegField.c0a80241b71c9d40a59dbd6f4b621260.toString()); //Profession field
			ps.setString(3, "RESIDENT");
			ps.setString(4, "CHIEF");
			ps.setString(5, "FELLOW");
			ps.setString(6, RegField.DSI_GRAD_DT.toString()); //Graduation Date field
			ps.setString(7, gradDateStr);
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				rsid = rs.getString("register_submittal_id");
				if (regData.containsKey(rsid)) {
					acctData = regData.get(rsid);
				} else {
					acctData = new ArrayList<>();
					log.debug("starting record for " + rsid);
				}
				DataVO vo = transposeData(rs);
				if (vo != null)  acctData.add(vo);
				
				regData.put(rsid, acctData);
			}
		} catch (SQLException sqle) {
			log.error("could not load user accounts", sqle);
			failures.add(sqle);
		}
		
		log.info("loaded records: " + regData.size());
		return regData;
	}
	
	
	private DataVO transposeData(ResultSet rs) throws SQLException {
		boolean isResident = !"FELLOW".equals(rs.getString("profession"));
		String fieldId = rs.getString("register_field_id");
		DataVO vo = new DataVO();
		vo.value = rs.getString("value_txt");
		vo.registerSubmittalId = rs.getString("register_submittal_id");
		//log.debug("" + fieldId + " " + rs.getString("register_submittal_id") + " " + vo.value);
		
		switch (fieldId) {
			case "DSI_SRVY_SEC_JOB":
				//this is likely a Fellow, but let's verify
				if (isResident || !"yes".equals(vo.value)) return null;
				//it's a Fellow with a job!  Slide through to below case so we can convert them
				
			case "DSI_SRVY_FELLOW_OBTD":
				//this one is special, anyone going through this process is implied to be becoming a Fellow - change their Profession
				//users that do NOT have this VO on thier account will not get updated when we run the inserts.
				//we want to still capture them so we have a true count of graduating users in our email
				if (isResident && !"yes".equalsIgnoreCase(vo.value)) return null;
				vo.profileFieldId = RegField.c0a80241b71c9d40a59dbd6f4b621260.toString();
				vo.value = "FELLOW";
				//tag the record so we can flush all existing survey responses for this user; they'll get a new survey for their Fellowship
				vo.delSrvyResponses = true;
				return vo;
				
			case "DSI_SRVY_SPECIALTY":
				vo.profileFieldId = RegField.c0a80241b71d27b038342fcb3ab567a0.toString();
				if (isResident) return vo;
				else return null;
			
			case "DSI_SRVY_FELLOW_END_DT":
				vo.profileFieldId = RegField.DSI_GRAD_DT.toString();
				if (isResident) return vo;
				else return null;
				
			case "DSI_SRVY_EMP_NM":
				vo.profileFieldId = RegField.DSI_ACAD_NM.toString();
				return vo;
				
			case "DSI_SRVY_EMP_ADDR":
				vo.profileFieldId = "DSI_ACAD_ADDR";
				return vo;
				
			case "DSI_SRVY_EMP_CITY":
				vo.profileFieldId = "DSI_ACAD_CITY";
				return vo;
				
			case "DSI_SRVY_EMP_STATE":
				vo.profileFieldId = "DSI_ACAD_STATE";
				return vo;
				
			case "DSI_SRVY_EMP_ZIP":
				vo.profileFieldId = "DSI_ACAD_ZIP";
				return vo;
				
			default: return null;
		}
	}
	
	
	/**
	 * Batch updates the data for specific questions in the register_data table
	 * "update this field for this user/submittal"
	 * @param rsId
	 * @param data
	 * @throws SQLException
	 */
	private void saveAccount(List<DataVO> data) throws SQLException {
		String sql = "update register_data set value_txt=?, update_dt=? where register_field_id=? and register_submittal_id=?";
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			for (DataVO vo : data) {
				ps.setString(1, vo.value);
				ps.setTimestamp(2, Convert.getCurrentTimestamp());
				ps.setString(3, vo.profileFieldId);
				ps.setString(4, vo.registerSubmittalId);
				ps.addBatch();
				log.debug("set " + vo.profileFieldId + "=" + vo.value + " for user " + vo.registerSubmittalId);
			}
			int[] cnt = ps.executeBatch();
			log.info("updated " + cnt.length + " records in register_data");
		} catch (SQLException sqle) {
			failures.add(sqle);
			throw sqle;
		}
	}
	
	/**
	 * Purge survey responses for residents who just became Fellows;
	 * They'll get a new set of survey questions from here forward.
	 * @param data
	 * @throws SQLException
	 */
	private void deleteOldSrvyResponses(List<DataVO> data) throws SQLException {
		//loop their register data and see if we're going to delete anything
		boolean failFast = true;
		for (DataVO vo : data) if (vo.delSrvyResponses) failFast = false;
		if (failFast) return;
		
		String sql = "delete from register_data where register_field_id like ? and register_submittal_id=?";
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, "DSI_SRVY_%");
			ps.setString(2, data.get(0).registerSubmittalId);
			int cnt = ps.executeUpdate();
			log.debug("deleting survey values for registerSubmittalId=" + data.get(0).registerSubmittalId);
			log.info("deleted " + cnt + " records in register_data");
		} catch (SQLException sqle) {
			failures.add(sqle);
			throw sqle;
		}
	}
	
	
	/**
	 * sends a synopsys email to the admin of what was done and if errors were generated
	 * @param acctCnt
	 * @param updateCnt
	 */
	private void sendAdminEmail(int acctCnt, int updateCnt) {
		try {
			// Build the email message
			EmailMessageVO msg = new EmailMessageVO(); 
			msg.addRecipients(props.getProperty("adminEmail").split(","));
			msg.setSubject("DSI Graduation Workflow - " + gradDateStr);
			msg.setFrom("appsupport@siliconmtn.com");
			
			StringBuilder html= new StringBuilder();
			html.append("<h4>Graduating: " + acctCnt + "</h4>");
			html.append("<h4>Converted: " + updateCnt + "</h4>");
			
			if (failures.size() > 0) {
				html.append("<b>Script generated the following exceptions:</b><br/><br/>");
			
				// loop the errors and display them
				for (int i=0; i < failures.size(); i++) {
					html.append(failures.get(i).getMessage()).append("<hr/>\r\n");
				}
			}
			msg.setHtmlBody(html.toString());
			
			MailTransportAgentIntfc mail = MailHandlerFactory.getDefaultMTA(props);
			mail.sendMessage(msg);
		} catch (Exception e) {
			log.error("Could not send admin email, ", e);
		}
	}
	
	
	/**
	 * **************************************************************************
	 * <b>Title</b>: DataVO.java<p/>
	 * <b>Description: a helper bean for the workflow in this class</b> 
	 * <p/>
	 * <b>Copyright:</b> Copyright (c) 2015<p/>
	 * <b>Company:</b> Silicon Mountain Technologies<p/>
	 * @author James McKain
	 * @version 1.0
	 * @since Jul 29, 2015
	 ***************************************************************************
	 */
	private class DataVO {
		String profileFieldId;
		String value;
		String registerSubmittalId;
		boolean delSrvyResponses;
	}
}
