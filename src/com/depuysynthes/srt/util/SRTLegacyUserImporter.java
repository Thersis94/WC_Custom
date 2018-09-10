package com.depuysynthes.srt.util;

import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.pool.SMTDBConnection;

/****************************************************************************
 * <b>Title:</b> SRTLegacyUserImporter.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Standard SRT User Importer.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 18, 2018
 ****************************************************************************/
public class SRTLegacyUserImporter extends SRTUserImport {

	/**
	 * @param args
	 */
	public SRTLegacyUserImporter(String ... args) {
		super(args);
	}

	/**
	 * Constructor that takes Attributes map as config.
	 * @param smtdbConnection 
	 * @param attributes
	 */
	public SRTLegacyUserImporter(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super(dbConn, attributes);
	}

	public static void main(String[] args) {
		SRTUserImport udi = new SRTLegacyUserImporter(args);
		udi.run();
	}

	/**
	 * @return
	 */
	@Override
	protected StringBuilder buildMainQuery() {
		StringBuilder megaQuery = new StringBuilder(2200);
		megaQuery.append(DBUtil.SELECT_FROM_STAR).append(" (");
		megaQuery.append(buildAdminUserQuery());
		megaQuery.append(" union ");
		megaQuery.append(buildSalesRosterQuery());
		megaQuery.append(" union ");
		megaQuery.append(buildProjectUserQuery());
		megaQuery.append(") as users ");
		megaQuery.append(DBUtil.ORDER_BY).append("is_admin desc, CO_ROSTER_ID desc;");
		return megaQuery;
	}

	/* (non-Javadoc)
	 * @see com.depuysynthes.srt.util.SRTUserImport#buildMainQuery()
	 */
	protected StringBuilder buildProjectUserQuery() {
		StringBuilder sql = new StringBuilder(1000);
		sql.append(DBUtil.SELECT_CLAUSE);
		sql.append("case when first_nm is not null and first_nm != '' ");
		sql.append("then first_nm else '(NO NAME)' end as first_nm, ");
		sql.append("case when last_nm is not null and last_nm != '' ");
		sql.append("then last_nm when first_nm is not null and first_nm != '' ");
		sql.append("then first_nm else '(NO NAME)' end as last_nm, ");
		sql.append("concat('\"', salesrep, '\"') as USER_NAME, ");
		sql.append("0 as allow_comm_flg, ");
		sql.append("null as WWID, ");
		sql.append("'0' as IS_ACTIVE, ");
		sql.append("'0' as ROLE_TXT, ");
		sql.append("null as ADDRESS_TXT, ");
		sql.append("null as CITY_NM, ");
		sql.append("null as STATE_CD, ");
		sql.append("null as ZIP_CD, ");
		sql.append("null as EMAIL_ADDRESS_TXT, ");
		sql.append("EMAIL_ADDRESS_TXT as ROSTER_EMAIL_ADDRESS_TXT, ");
		sql.append("'8' as workgroup_id, ");
		sql.append("null as PASSWORD_TXT, ");
		sql.append("MOBILE_PHONE_TXT, ");
		sql.append("cast(territoryid as varchar) as TERRITORY_ID, ");
		sql.append("null as REGION_ID, ");
		sql.append("null as AREA_ID, ");
		sql.append("'").append(opCoId).append("' as OP_CO_ID, ");
		sql.append("0 as is_admin, ");
		sql.append("jdeacctnumber as ACCOUNT_NO, ");
		sql.append("cast(projectid as varchar) as CO_ROSTER_ID, ");
		sql.append("srtcontact as ENGINEERING_CONTACT ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.projects p ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" projectid not in ( ");
		sql.append("select projectid ").append(DBUtil.FROM_CLAUSE);
		sql.append("dbo.projects").append(DBUtil.WHERE_CLAUSE);
		sql.append("jdeacctnumber in (select customerId from dbo.tbl_pt_sales_roster) ");
		sql.append("or lower(salesrepemail) in (select lower(email) from dbo.tbl_pt_sales_roster) ");
		sql.append("or concat(first_nm, ' ', last_nm) in (select firstlast from dbo.tbl_pt_sales_roster)) ");
		return sql;
	}

	protected StringBuilder buildSalesRosterQuery() {
		StringBuilder sql = new StringBuilder(1000);
		sql.append(DBUtil.SELECT_CLAUSE);
		sql.append("r.first_name as first_nm, ");
		sql.append("r.last_name as last_nm, ");
		sql.append("concat('\"', firstlast, '\"') as USER_NAME, ");
		sql.append("1 as allow_comm_flg, ");
		sql.append("r.wwid as WWID, ");
		sql.append("'1' as IS_ACTIVE, ");
		sql.append("'8' as ROLE_TXT, ");
		sql.append("r.address as ADDRESS_TXT, ");
		sql.append("r.city as CITY_NM, ");
		sql.append("r.state as STATE_CD, ");
		sql.append("r.zip as ZIP_CD, ");
		sql.append("r.alt_email as EMAIL_ADDRESS_TXT, ");
		sql.append("r.email as ROSTER_EMAIL_ADDRESS_TXT, ");
		sql.append("'8' as workgroup_id, ");
		sql.append("null as PASSWORD_TXT, ");
		sql.append("r.cell_phone as MOBILE_PHONE_TXT, ");
		sql.append("cast(r.territoryid as varchar) as TERRITORY_ID, ");
		sql.append("cast(r.region as varchar) as REGION_ID, ");
		sql.append("cast(r.area as varchar) as AREA_ID, ");
		sql.append("'").append(opCoId).append("' as OP_CO_ID, ");
		sql.append("0 as is_admin, ");
		sql.append("customerid as ACCOUNT_NO, ");
		sql.append("concat('SR_', r.id) as CO_ROSTER_ID, ");
		sql.append("'replace' as ENGINEERING_CONTACT ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.tbl_pt_sales_roster r ");

		return sql;
	}

	protected StringBuilder buildAdminUserQuery() {
		StringBuilder sql = new StringBuilder(400);
		sql.append(DBUtil.SELECT_CLAUSE);
		sql.append("userfirstname as first_nm, ");
		sql.append("userlastname as last_nm, ");
		sql.append("concat('\"', useremail, '\"') as USER_NAME, ");
		sql.append("1 as allow_comm_flg, ");
		sql.append("wwid as WWID, ");
		sql.append("case when status = 'Active' then '1' else '0' end as IS_ACTIVE, ");
		sql.append("cast (role as varchar) as ROLE_TXT, ");
		sql.append("null as ADDRESS_TXT, ");
		sql.append("null as CITY_NM, ");
		sql.append("null as STATE_CD, ");
		sql.append("null as ZIP_CD, ");
		sql.append("lower(emailaddresstxt) as EMAIL_ADDRESS_TXT, ");
		sql.append("lower(emailaddresstxt) as ROSTER_EMAIL_ADDRESS_TXT, ");
		sql.append("cast(workgroupid as varchar) as workgroup_id, ");
		sql.append("userpassword as PASSWORD_TXT, ");
		sql.append("null as MOBILE_PHONE_TXT, ");
		sql.append("'-1' as TERRITORY_ID, ");
		sql.append("null as REGION_ID, ");
		sql.append("null as AREA_ID, ");
		sql.append("'").append(opCoId).append("' as OP_CO_ID, ");
		sql.append("1 as is_admin, ");
		sql.append("'US_SPINE_ADMIN' as ACCOUNT_NO, ");
		sql.append("concat('ADM_', userid) as CO_ROSTER_ID, ");
		sql.append("srtcontact2 as ENGINEERING_CONTACT ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.users ");
		return sql;
	}
}