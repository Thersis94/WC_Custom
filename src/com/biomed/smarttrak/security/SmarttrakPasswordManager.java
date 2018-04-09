package com.biomed.smarttrak.security;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Map;
import java.util.ResourceBundle;

import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.PasswordManager;

/****************************************************************************
 * <b>Title</b>: SmarttrakPasswordManager.java<p/>
 * <b>Description: Custom password manager that sends a BCC email to 
 * a user's source when a password reset is sent.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2018<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Apr 6, 2018
 ****************************************************************************/

public class SmarttrakPasswordManager extends PasswordManager {

	public SmarttrakPasswordManager(Map<String, Object> attributes, SMTDBConnection conn, SiteVO site) {
		super(attributes, conn, site);
	}
	
	
	@Override
	protected EmailMessageVO buildPublicEmail(UserDataVO user, ResourceBundle rb, String msg) throws InvalidDataException {
		EmailMessageVO emailVo = new EmailMessageVO();
		emailVo.setFrom(site.getMainEmail());
		emailVo.addRecipient(user.getEmailAddress());
		emailVo.setSubject(rb.getString("login.emailsubject") + " " + site.getSiteName());
		emailVo.setHtmlBody(msg);
		String sourceEmail = getSource(user.getProfileId());
		if (!StringUtil.isEmpty(sourceEmail))
			emailVo.addBCC(sourceEmail);
		return emailVo;
	}
	
	
	/**
	 * Get the email address of the user listed as the sales source for the supplied user
	 * @param profileId
	 * @return
	 */
	private String getSource(String profileId) {
		StringBuilder sql = new StringBuilder(150);
		
		sql.append("select p.email_address_txt from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA)).append("biomedgps_user u ");
		sql.append("left join register_data rd on rd.register_submittal_id = u.register_submittal_id and register_field_id = ? ");
		sql.append("left outer join profile p on p.profile_id = rd.value_txt ");
		sql.append("where u.profile_id = ? ");
		
		String emailAddress = "";
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, UserVO.RegistrationMap.SOURCE.getFieldId());
			ps.setString(2, profileId);
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next())
				emailAddress = rs.getString("email_address_txt");
			if (StringUtil.isEmpty(emailAddress)) {
				return "";
			} else {
				StringEncrypter se = new StringEncrypter((String) attributes.get(Constants.ENCRYPT_KEY));
				return se.decrypt(emailAddress);
			}
		} catch (Exception e) {
			return "";
		}
	}

}
