package com.depuysynthesinst.lms;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import com.depuysynthesinst.DSIUserDataVO;
import com.depuysynthesinst.DSIUserDataVO.RegField;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.Convert;

/**
 * **************************************************************************
 * <b>Title</b>: PatchDupAccounts.java<p/>
 * <b>Description: This class was written to re-migrate DSI users who were given NEW
 * TTLMS accounts when our call to Migrate failed on the LMS side.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 4, 2016
 ***************************************************************************
 */
public class PatchDupAccounts extends CommandLineUtil {
	
	public PatchDupAccounts(String[] args) {
		super(args);
		super.loadProperties("scripts/MediaBin.properties");
		super.loadDBConnection(props);
	}
	
	public static void main(String[] args) {
		PatchDupAccounts pda = new PatchDupAccounts(args);
		pda.run();
		
	}
	
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		List<DSIUserDataVO> data = loadUsers();
		log.info("loaded " + data.size() + " records to migrate");
		for (DSIUserDataVO vo : data)
			migrateUser(vo);
		
		closeDBConnection();
	}
	
	
	private void migrateUser(DSIUserDataVO vo) {
		LMSWSClient client = new LMSWSClient("183742B231C69E28");
		try {
			//migrate the user
			vo.setTtLmsId(client.migrateUser(vo));
			log.info("ttlmsid=" + vo.getTtLmsId());
			
			//check for a failed transaction before we update the database
			if ("0".equals(vo.getTtLmsId())) {
				log.error("BAD TTLMSID " + vo.getProfileId() + " " + vo.getEmailAddress());
				return;
			}
			
			//save to DSI database
			String sql = "update register_data set update_dt=getDate(), value_txt=? " +
					"where register_field_id=? and register_submittal_id=?";
			log.info(sql);
			try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
				ps.setString(1, vo.getTtLmsId());
				ps.setString(2, RegField.DSI_TTLMS_ID.toString());
				ps.setString(3, vo.getAddress2());
				int cnt = ps.executeUpdate();
				log.info("updated " + cnt + " record, profileId=" + vo.getProfileId());
			} catch (SQLException sqle) {
				log.error("sqle for " + vo.getTtLmsId() + " and profileId=" + vo.getProfileId(), sqle);
			}
			
		} catch (Exception e) {
			log.error("exception migrating user: " + vo, e);
		}
	}
	
	
	private List<DSIUserDataVO> loadUsers() {
		List<DSIUserDataVO> data = new ArrayList<>();
		String sql = "select Login, old_U_ID, First, Last, Email, " +
				"VerifiedFlg, EligibleFlg, OLD_SNYTHES_ID, OLD_LMS_ID, b.register_submittal_id, " +
				"Country, Program, Profession,rtrim(SUBSTRING([Group],0,4)) as grp " +
				"from WebCrescendo_custom.dbo.dsi_bad_lms_data a " +
				"inner join REGISTER_SUBMITTAL b on a.Login=b.PROFILE_ID and b.SITE_ID='DPY_SYN_INST_1' ";
		log.info(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				DSIUserDataVO vo = new DSIUserDataVO();
				vo.setProfileId(rs.getString(1));
				vo.setTtLmsId(rs.getString(2));
				vo.setFirstName(rs.getString(3));
				vo.setLastName(rs.getString(4));
				vo.setEmailAddress(rs.getString(5));
				vo.setVerified(Convert.formatBoolean(rs.getString(6)));
				vo.setEligible(Convert.formatBoolean(rs.getString(7)));
				vo.setSynthesId(rs.getString(8));
				vo.setAddress(rs.getString(9));
				vo.setAddress2(rs.getString(10));
				vo.setCountryCode(rs.getString("Country"));
				vo.setHospital(rs.getString("Program"));
				vo.setProfession(rs.getString("Profession"));
				vo.setSpecialty(rs.getString("grp"));
				data.add(vo);
			}
		} catch (SQLException sqle) {
			log.error("could not load records", sqle);
		}
		return data;
	}

}
