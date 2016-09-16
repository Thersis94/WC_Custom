package com.nltek.action.contact;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;

import com.siliconmtn.exception.ApplicationException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.exception.MailException;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ExpiringAccountEmailer.java<p/>
 * <b>Description: emails the NLT admins 3wks prior to user accounts expiring, 
 * so they can contact individuals to renew their subscription.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Sep 16, 2016
 ****************************************************************************/
public class ExpiringAccountEmailer extends CommandLineUtil {

	private StringEncrypter se = null;
	
	/**
	 * @param args
	 */
	public ExpiringAccountEmailer(String[] args) {
		super(args);
		loadProperties("scripts/nlt-acct-emailer.properties");
		super.loadDBConnection(props);
		
		try {
			se = new StringEncrypter(props.getProperty(Constants.ENCRYPT_KEY));
		} catch (Exception e) {
			log.error("could not make StringEncrypter", e);
		}
	}


	/**
	 * @param args
	 */
	public static void main(String[] args) {
		ExpiringAccountEmailer eam = new ExpiringAccountEmailer(args);
		eam.run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		StringBuilder emailMsg = new StringBuilder(5000);
		emailMsg.append("<p>The following AutoIngest user accounts are approaching their expiration:</p>");
		emailMsg.append("<table width='100%' border='1'>");
		emailMsg.append("<tr><th align='left'>Name (Email)</th><th align='left'>Expires</th></tr>");

		try (PreparedStatement ps = dbConn.prepareStatement(makeQuery())) {
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				appendRowToEmail(emailMsg, rs);

		} catch (SQLException sqle) {
			log.error("could not load expiring accounts", sqle);
			emailMsg.append("<p color='red'>ERROR: ").append(sqle.getMessage()).append("</p>");
		} catch (Exception e2) {
			log.error("something went wrong, likely encryption related", e2);
			emailMsg.append("<p color='red'>ERROR: ").append(e2.getMessage()).append("</p>");
		}

		//send the admin email
		try {
			super.sendEmail(emailMsg, null);
		} catch (ApplicationException|InvalidDataException|MailException e) {
			log.error("could not send admin email", e);
		}
	}


	/**
	 * the query we need to execute
	 * @return
	 */
	private String makeQuery() {
		StringBuilder sb = new StringBuilder(1000);
		sb.append("select email_address_txt, first_nm, last_nm, role_expire_dt, ");
		sb.append("DATE_PART('day', ROLE_EXPIRE_DT-CURRENT_DATE) as date_diff ");
		sb.append("from PROFILE inner join PROFILE_ROLE on PROFILE.PROFILE_ID=PROFILE_ROLE.PROFILE_ID ");
		sb.append("where PROFILE_ROLE.STATUS_ID='20' AND PROFILE_ROLE.SITE_ID='AUTO_INGEST' ");
		sb.append("AND date(PROFILE_ROLE.ROLE_EXPIRE_DT) < date(CURRENT_TIMESTAMP + interval '61 days') "); //within 60 days
		sb.append("and PROFILE_ROLE.ROLE_EXPIRE_DT > CURRENT_DATE "); //but after today
		sb.append("order by date_diff");

		log.debug(sb);
		return sb.toString();
	}


	/**
	 * adds a row from the RS to the email message
	 * @param emailMsg
	 * @param rs
	 * @param se
	 * @throws SQLException 
	 * @throws IllegalArgumentException 
	 * @throws EncryptionException 
	 */
	private void appendRowToEmail(StringBuilder emailMsg, ResultSet rs) 
			throws SQLException, EncryptionException {
		String email = se.decrypt(StringUtil.checkVal(rs.getString("email_address_txt")));
		String name = se.decrypt(StringUtil.checkVal(rs.getString("first_nm")));
		name += " " + se.decrypt(StringUtil.checkVal(rs.getString("last_nm")));

		emailMsg.append("<tr><td>").append(name);
		emailMsg.append(" (<a href=\"mailto:").append(email).append("\">").append(email).append("</a>)</td>");

		Date d = rs.getDate("role_expire_dt");
		int dateDiff = rs.getInt("date_diff");
		String expiryStr = " (" + dateDiff;
		expiryStr += (dateDiff == 1) ? " day)" : " days)";

		emailMsg.append("<td>").append(Convert.formatDate(d, Convert.DATE_SLASH_PATTERN));
		emailMsg.append(expiryStr).append("</td></tr>");
	}
}