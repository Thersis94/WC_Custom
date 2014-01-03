package com.ansmed.sb.orderlit;

//JDK 1.6.0 imports

import javax.servlet.http.HttpSession;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

// SMT Base Libs 2.0

import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;

// SB Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: OrderLitAction.java<p/>
 * <b>Description: Retrieves user's email address and passes it to the ANS'
 * Order Literature login process.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since June 2, 2008
 ****************************************************************************/

public class OrderLitAction extends SimpleActionAdapter {
	
	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionController#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	@Override
	public void retrieve(SMTServletRequest req) throws ActionException {
		
		HttpSession ses = req.getSession(); 
		
		UserDataVO uvo = (UserDataVO)ses.getAttribute(Constants.USER_DATA);
		String ansEmail = uvo.getEmailAddress();
		String ansLoginId = null;
		
		log.debug("ANS email address: " + ansEmail);
		
		if (ansEmail == null || ansEmail.length() == 0) {
			
			ansLoginId = uvo.getAuthenticationId();
			
			if (ansLoginId != null && ansLoginId.length() > 0){
				 log.debug("No ANS email address, checking sales rep table.");
			
				 StringBuffer sql = new StringBuffer();
			
				 sql.append("select email_address_txt from ");
				 sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA)).append("ans_sales_rep");
				 sql.append(" where ans_login_id = ?");
			
				 Connection conn = getDBConnection();
				 PreparedStatement ps = null;

				 try {
				
					ps = conn.prepareStatement(sql.toString());
					ps.setString(1,ansLoginId);
					ResultSet rs = ps.executeQuery();
					
					if (rs.next()) {
						ansEmail = rs.getString("EMAIL_ADDRESS_TXT");
						log.debug("Using sales rep email address.");
					}
					
				} catch (SQLException sqle) {
				
					log.error("Error retrieving sales rep email address.", sqle);
					
				} finally {
					
					if (ps != null) {
						try {
							
							ps.close();
							
						} catch (Exception e) {
							log.error("Error closing PreparedStatement.");
						}
					}
				}
			}
		}

		if (ansEmail != null && ansEmail.length() > 0) {
			StringBuffer url = new StringBuffer("http://webstore.ans-medical.com");
			url.append("/login.asp?LoginEmail=").append(ansEmail);
			url.append("&LoginEncryptVal=3B81DD13-1031-A133-9EC605925C84C1A1");
			
			req.setAttribute(Constants.REDIRECT_REQUEST,Boolean.TRUE);
	        req.setAttribute(Constants.REDIRECT_URL, url.toString());
			log.debug("Redirect URL is: " + url.toString());
		} 
	}
}