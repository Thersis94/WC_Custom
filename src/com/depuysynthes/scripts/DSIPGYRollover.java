package com.depuysynthes.scripts;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import com.depuysynthesinst.DSIUserDataVO.RegField;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.MailHandlerFactory;
import com.siliconmtn.io.mail.mta.MailTransportAgentIntfc;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title</b>: DSIPGYRollover.java<p/>
 * <b>Description:</b>  Find Residents who graduate on an anniversary of today's calendar month/day 
 * and increments their PGY year. 
	
	Intended outcome: The next time the user logs-in they'll be a in a new PGY bucket, approaching their graduation
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jul 29, 2015
 ****************************************************************************/
public class DSIPGYRollover extends CommandLineUtil {

	private String gradDateStr; // MM/DD/YYYY formatted graduation date used in the lookup query 
	
	/**
	 * List of errors 
	 */
	List <Exception> failures = new ArrayList<Exception>();
	
	/**
	 * @param args
	 */
	public DSIPGYRollover(String[] args) {
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
		//Create an instance of the DSIPGYRollover
		DSIPGYRollover klass = new DSIPGYRollover(args);
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
		List<DataVO> accounts = loadRegistrations();
		
		int cnt = 0;
		try {
			cnt = saveAccounts(accounts);
		} catch (SQLException sqle) {
			log.error("could not update accounts", sqle);
		}
		
		//noitify the admin we did what something
		sendAdminEmail(accounts.size(), cnt);
	}

	
	private List<DataVO> loadRegistrations() {
		List<DataVO> regData = new ArrayList<DataVO>();
		StringBuilder sql = new StringBuilder(400);
		//grab all registration accounts for user who are of specific professions 
		//and have a graduation date of today
		sql.append("select pgy.value_txt, grad.value_txt, pgy.register_submittal_id ");
		sql.append("from register_submittal rs ");
		sql.append("inner join register_data pgy on pgy.register_submittal_id=rs.register_submittal_id and pgy.register_field_id=? and (pgy.update_dt is null or pgy.update_dt < cast(getdate() as DATE)) "); //we've not JUST (today) incremented their value; this could be damaging
		sql.append("inner join register_data prof on rs.register_submittal_id=prof.register_submittal_id and prof.register_field_id=? and prof.value_txt in (?,?) ");
		sql.append("inner join register_data grad on rs.register_submittal_id=grad.register_submittal_id and grad.register_field_id=? and SUBSTRING(grad.value_txt,0,6)=? ");
		sql.append("where rs.site_id=? ");
		sql.append("order by pgy.register_submittal_id");
		log.info(sql + " " + gradDateStr.substring(0, 5));
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1,  RegField.DSI_PGY.toString());
			ps.setString(2, RegField.c0a80241b71c9d40a59dbd6f4b621260.toString()); //Profession field
			ps.setString(3, "RESIDENT");
			ps.setString(4, "CHIEF");
			ps.setString(5, RegField.DSI_GRAD_DT.toString()); //Graduation Date field
			ps.setString(6, gradDateStr.substring(0, 5));
			ps.setString(7, "DPY_SYN_INST_1");
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				Date gradDt = Convert.formatDate(Convert.DATE_SLASH_PATTERN, rs.getString(2));
				if (gradDt == null) continue;
				
				//if the student has graduated we stop rolling them over, so only act on those who haven't graduated
				if (gradDt.after(Calendar.getInstance().getTime())) {
					DataVO vo = new DataVO();
					vo.registerSubmittalId = rs.getString("register_submittal_id");
					vo.profileFieldId = RegField.DSI_PGY.toString();
					vo.value = "" + (Convert.formatInteger(rs.getString(1))+1);
					regData.add(vo);
				}
			}
		} catch (SQLException sqle) {
			log.error("could not load user accounts", sqle);
			failures.add(sqle);
		}
		
		log.info("loaded records: " + regData.size());
		return regData;
	}
	
	
	/**
	 * Batch updates the data for specific questions in the register_data table
	 * "update this field for this user/submittal"
	 * @param rsId
	 * @param data
	 * @throws SQLException
	 */
	private int saveAccounts(Collection<DataVO> data) throws SQLException {
		int cnt = 0;
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
			int[] cntArr = ps.executeBatch();
			cnt = cntArr.length;
		} catch (SQLException sqle) {
			failures.add(sqle);
			throw sqle;
		}
		log.info("updated " + cnt + " records in register_data");
		return cnt;
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
			msg.setSubject("DSI PGY Rollover - " + gradDateStr);
			msg.setFrom("appsupport@siliconmtn.com");
			
			StringBuilder html= new StringBuilder();
			html.append("<h4>Affected Residents: " + acctCnt + "</h4>");
			html.append("<h4>Converted Residents: " + updateCnt + "</h4>");
			
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
	}
}
