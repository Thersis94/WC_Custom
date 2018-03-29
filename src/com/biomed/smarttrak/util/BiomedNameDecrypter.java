package com.biomed.smarttrak.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.CommandLineUtil;

/*****************************************************************************
<p><b>Title</b>: BiomedNameDecrypter.java</p>
<p><b>Description: Save decrypted versions of the user's name and email
address in the biomedgps_user table.</b></p>
<p>Copyright: (c) 2000 - 2018 SMT, All Rights Reserved</p>
<p>Company: Silicon Mountain Technologies</p>
@author Eric Damschroder
@version 1.0
@since Mar 27, 2018
<b>Changes:</b> 
***************************************************************************/

public class BiomedNameDecrypter extends CommandLineUtil {
	
	
	public BiomedNameDecrypter(String[] args) {
		super(args);
		loadProperties("scripts/bmg_smarttrak/user_import_config.properties");
		super.loadDBConnection(props);
	}
	
	public static void main(String[] args) {
		BiomedNameDecrypter decrypt = new BiomedNameDecrypter(args);
		decrypt.run();
	}

	@Override
	public void run() {
		try {
			List<UserDataVO> users = getProfiles();
			decryptData(users);
			saveUsers(users);
		} catch (Exception e) {
			log.error(e);
		}
	}
	
	
	/**
	 * Save the user's name and email address
	 * @param users
	 * @throws SQLException
	 */
	private void saveUsers(List<UserDataVO> users) throws SQLException {
		StringBuilder sql = new StringBuilder(150);
		
		sql.append("update ").append(props.get("customSchema")).append("biomedgps_user set ");
		sql.append("first_nm = ?, last_nm = ?, email_address_txt = ? ");
		sql.append("where profile_id = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			for (UserDataVO user : users) {
				if (user.getProfileId() == null) continue;
				ps.setString(1, user.getFirstName());
				ps.setString(2, user.getLastName());
				ps.setString(3, user.getEmailAddress());
				ps.setString(4, user.getProfileId());
				
				ps.addBatch();
			}
			
			ps.executeBatch();
		}
	}
	
	
	/**
	 * Get the profile data for all smarttrak users
	 * @return
	 * @throws SQLException
	 */
	private List<UserDataVO> getProfiles() throws SQLException {
		List<UserDataVO> users = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(buildRetrieveSQL())) {
			ResultSet rs = ps.executeQuery();
			
			while (rs.next()) {
				users.add(new UserDataVO(rs));
			}
		}
		return users;
	}
	
	
	/**
	 * Decrypt the user's name and email address
	 * @param users
	 * @throws EncryptionException
	 */
	private void decryptData(List<UserDataVO> users) throws EncryptionException {
		StringEncrypter se = new StringEncrypter(props.getProperty("encryptionKey"));
		
		for (UserDataVO user : users) {
			try {
				user.setFirstName(se.decrypt(user.getFirstName()));
				user.setLastName(se.decrypt(user.getLastName()));
				user.setEmailAddress(se.decrypt(user.getEmailAddress()));
			} catch (EncryptionException e) {
				continue;
			}
		}
	}
	
	/**
	 * Build sql to retrieve all users and thier profile info.
	 * @return
	 */
	private String buildRetrieveSQL() {
		StringBuilder sql = new StringBuilder(200);
		
		sql.append("select p.first_nm, p.last_nm, p.email_address_txt, p.profile_id from ");
		sql.append(props.get("customSchema")).append("biomedgps_user u ");
		sql.append("left join profile p on p.profile_id = u.profile_id ");
		
		return sql.toString();
	}

}
