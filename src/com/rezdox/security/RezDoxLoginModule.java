package com.rezdox.security;

// Java 8
import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

//SMTBaseLibs
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.security.UserDataVO;

//WebCrescendo libs
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.DBLoginModule;

//WC_Custom libs
import com.rezdox.vo.MemberVO;

/*****************************************************************************
 <p><b>Title</b>: RezDoxLoginModule</p>
 <p><b>Description: </b>Custom login module for RezDox user login.  Authenticates 
 user against WebCrescendo core login, then retrieves RezDox member data.</p>
 <p> 
 <p>Copyright: (c) 2018 SMT, All Rights Reserved</p>
 <p>Company: Silicon Mountain Technologies</p>
 @author Tim Johnson
 @version 1.0
 @since Jan 25, 2018
 <b>Changes:</b>
 ***************************************************************************/
public class RezDoxLoginModule extends DBLoginModule {

	public RezDoxLoginModule() {
		super();
	}

	public RezDoxLoginModule(Map<String, Object> config) {
		super(config);
	}

	/**
	 * After authentication, gets RezDox member data for the user who just logged in and
	 * merges the user data into the MemberVO.
	 * 
	 * @param profileId
	 * @param authenticationId
	 * @return
	 */
	@Override
	public MemberVO loadUserData(String profileId, String authenticationId) {
		UserDataVO user = super.loadUserData(profileId, authenticationId);
		Connection dbConn = (Connection) getAttribute(GlobalConfig.KEY_DB_CONN);

		// Get the sql and parameters for the query
		List<Object> params = new ArrayList<>();
		String sql = getMemberSql(user, params);

		// Get the member data
		DBProcessor db = new DBProcessor(dbConn);
		List<MemberVO> memberData = db.executeSelect(sql, params, new MemberVO());

		// Populate the member/user data
		MemberVO member = memberData.get(0);
		member.setData(user.getDataMap());
		member.setAttributes(user.getAttributes());
		member.setAuthenticated(user.isAuthenticated());

		return member;
	}


	/**
	 * Load the member record joined to their active business (optionally).
	 * Loading the businessId here avoid compelx joins later (particularly in Projects).
	 * @return
	 */
	protected String getMemberSql(UserDataVO user, List<Object> params) {
		String schema = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);

		StringBuilder sql = new StringBuilder(100);
		sql.append("select m.*, bxr.business_id from ").append(schema).append("rezdox_member m ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_BUSINESS_MEMBER_XR bxr on m.member_id=bxr.member_id and bxr.status_flg=1 ");

		sql.append("where m.profile_id=?");
		log.debug(sql + " | " + user.getProfileId());

		params.add(user.getProfileId());

		return sql.toString();
	}
}