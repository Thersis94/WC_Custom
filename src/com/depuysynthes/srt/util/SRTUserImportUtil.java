package com.depuysynthes.srt.util;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title:</b> SRTUserImportUtil.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages importing SRT User Records.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 6, 2018
 ****************************************************************************/
public class SRTUserImportUtil extends SRTUserImport {

	/**
	 * @param args
	 */
	public SRTUserImportUtil(String[] args) {
		super(args);
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		SRTUserImportUtil udi = new SRTUserImportUtil(args);
		udi.run();
	}

	/**
	 * 
	 * @return
	 */
	protected StringBuilder buildMainQuery() {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select cast(userid as varchar) AS ROSTER_ID, ");
		sql.append("userfirstname AS FIRST_NM, userlastname AS LAST_NM, ");
		sql.append("lower(emailaddresstxt) AS EMAIL_ADDRESS_TXT,  ");
		sql.append("wwid as WWID, status as IS_ACTIVE, role as ROLE_TXT, ");
		sql.append("workgroupid as workgroup_id, userpassword as PASSWORD_TXT, ");
		sql.append("useremail as USER_EMAIL, 1 as allow_comm_flg, -1 as territory_id, ");
		sql.append("'' as mobile_phone_txt, 'US_SPINE' as OP_CO_ID ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.users ");
		sql.append(DBUtil.ORDER_BY).append("userid order by FIRST_NM, LAST_NM;");
		return sql;
	}
}