package com.depuysynthes.srt.util;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.pool.SMTDBConnection;

/****************************************************************************
 * <b>Title:</b> USOrthoUserImport.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> US Ortho Centric User Importer for SRT Roster Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 18, 2018
 ****************************************************************************/
public class USOrthoUserImport extends SRTUserImport {
	private static final String SOURCE_FILE_CONFIG = "scripts/srt/ortho_user_import_config.properties";

	/**
	 * @param args
	 */
	public USOrthoUserImport(String ... args) {
		super(args);

		//Ensure Ortho Source config is used.
		if(Files.exists(Paths.get(SOURCE_FILE_CONFIG))) {
			configFilePath = SOURCE_FILE_CONFIG;
		}
	}

	/**
	 * Constructor that takes Attributes map as config.
	 * @param smtdbConnection 
	 * @param attributes
	 */
	public USOrthoUserImport(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super(dbConn, attributes);
	}

	public static void main(String[] args) {
		SRTUserImport udi = new USOrthoUserImport(args);
		udi.run();
	}

	
	/* (non-Javadoc)
	 * @see com.depuysynthes.srt.util.SRTUserImport#buildMainQuery()
	 */
	@Override
	protected StringBuilder buildMainQuery() {
		StringBuilder megaQuery = new StringBuilder(2200);
		megaQuery.append("select * from (");
		megaQuery.append(buildAdminUserQuery());
		megaQuery.append(" union ");
		megaQuery.append(buildSalesRosterQuery());
		megaQuery.append(") as users ");
		megaQuery.append("order by is_admin desc, CO_ROSTER_ID desc;");
		return megaQuery;
	}

	protected StringBuilder buildSalesRosterQuery() {
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select ");
		sql.append("sales_first_nm as first_nm, ");
		sql.append("sales_last_nm as last_nm, ");
		sql.append("1 as allow_comm_flg, ");
		sql.append("'' as wwid, ");
		sql.append("'1' as IS_ACTIVE, ");
		sql.append("'5' as ROLE_TXT, ");
		sql.append("shipping_address as ADDRESS_TXT, ");
		sql.append("email_address_txt, ");
		sql.append("email_address_txt as ROSTER_EMAIL_ADDRESS_TXT, ");
		sql.append("'8' as workgroup_id, ");
		sql.append("null as PASSWORD_TXT, ");
		sql.append("null as MOBILE_PHONE_TXT, ");
		sql.append("cast(territoryid as varchar) as TERRITORY_ID, ");
		sql.append("null as REGION_ID, ");
		sql.append("null as AREA_ID, ");
		sql.append("'US_ORTHO' as OP_CO_ID, ");
		sql.append("0 as is_admin, ");
		sql.append("US_ORTHO_REP as ACCOUNT_NO, ");
		sql.append("null as CO_ROSTER_ID, ");
		sql.append("'replace' as ENGINEERING_CONTACT ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.us_ortho_2014_2015 ");

		return sql;
	}

	protected StringBuilder buildAdminUserQuery() {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select ");
		sql.append("split_part(user_nm, ' ', 1) as first_nm, ");
		sql.append("split_part(user_nm, ' ', 2) as last_nm, ");
		sql.append("1 as allow_comm_flg, ");
		sql.append("'' as wwid, ");
		sql.append("'1' as IS_ACTIVE, ");
		sql.append("ROLE_TXT, ");
		sql.append("null as ADDRESS_TXT, ");
		sql.append("null as CITY_NM, ");
		sql.append("null as STATE_CD, ");
		sql.append("null as ZIP_CD, ");
		sql.append("lower(email_address_txt) as EMAIL_ADDRESS_TXT, ");
		sql.append("lower(email_address_txt) as ROSTER_EMAIL_ADDRESS_TXT, ");
		sql.append("role_txt as workgroup_id, ");
		sql.append("null as PASSWORD_TXT, ");
		sql.append("null as MAIN_PHONE_TXT, ");
		sql.append("null as TERRITORY_ID, ");
		sql.append("null as REGION_ID, ");
		sql.append("null as AREA_ID, ");
		sql.append("opco as OP_CO_ID, ");
		sql.append("1 as is_admin, ");
		sql.append("'US_ORTHO_ADMIN' as ACCOUNT_NO, ");
		sql.append("null as CO_ROSTER_ID, ");
		sql.append("null as ENGINEERING_CONTACT ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.us_ortho_team ");
		return sql;
	}
}
