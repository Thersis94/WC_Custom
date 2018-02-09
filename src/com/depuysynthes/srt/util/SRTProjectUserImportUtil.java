package com.depuysynthes.srt.util;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.UUIDGenerator;

/****************************************************************************
 * <b>Title:</b> SRTProjectUserImportUtil.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Load user information using Projects records.  These
 * users are not active and are only loaded for historical reasons.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 8, 2018
 ****************************************************************************/
public class SRTProjectUserImportUtil extends SRTUserImport {

	/**
	 * @param args
	 */
	public SRTProjectUserImportUtil(String[] args) {
		super(args);
	}

	public static void main(String[] args) {
		SRTProjectUserImportUtil udi = new SRTProjectUserImportUtil(args);
		udi.run();
	}

	/**
	 * Parses the result set into a Map of String (key) to Object (value).
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	protected List<Map<String,Object>> parseResults(ResultSet rs) throws SQLException {
		Map<String,Object> record;
		List<Map<String,Object>> records = new ArrayList<>();
		UUIDGenerator gen = new UUIDGenerator();
		while(rs.next()) {
			record = new HashMap<>();
			for (ImportField field : ImportField.values()) {

				//We loaded distinct users.  Add a UUID for the Roster_id.
				if(ImportField.ROSTER_ID.equals(field)) {
					record.put(field.name(), gen.getUUID());
				} else {
					record.put(field.name(), rs.getObject(field.name().toLowerCase()));
				}
			}
			records.add(record);
		}
		return records;
	}

	/* (non-Javadoc)
	 * @see com.depuysynthes.srt.util.SRTUserImport#buildMainQuery()
	 */
	@Override
	protected StringBuilder buildMainQuery() {
		StringBuilder sql = new StringBuilder(1000);
		sql.append("select distinct ");
		sql.append("case when salesrep like '%,%' then trim(split_part(salesrep, ',', 1)) ");
		sql.append("when salesrep not like '%,%' then trim(split_part(salesrep, ' ', 1)) ");
		sql.append("else salesrep end as FIRST_NM, ");
		sql.append("case when salesrep like '%,%' then trim(split_part(salesrep, ',', 2)) ");
		sql.append("when salesrep not like '%,%' then trim(split_part(salesrep, ' ', 2)) end as LAST_NM, ");
		sql.append("case when salesrepemail like '%@%' then salesrepemail ");
		sql.append("when salesrepemail not like '%@%' then concat(replace(salesrep, ' ', ''), '@srt.com') end ");
		sql.append("as EMAIL_ADDRESS_TXT, ");
		sql.append("'' as USER_EMAIL, 0 as allow_comm_flg, ");
		sql.append("'' as WWID, 'false' as IS_ACTIVE, 0 as ROLE_TXT, ");
		sql.append("'6' as WORKGROUP_ID, '' as PASSWORD_TXT, ");
		sql.append("case when salesrepcellphone is not null and salesrepcellphone != '' ");
		sql.append("then salesrepcellphone ");
		sql.append("when salesreppager is not null and salesreppager != '' ");
		sql.append("then salesreppager else '' end as MOBILE_PHONE_TXT, ");
		sql.append("territoryid as TERRITORY_ID, 'US_SPINE' as OP_CO_ID ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.projects ");
		sql.append("where salesrep not in ");
		sql.append("(select concat(s.last_name, ', ', s.first_name) ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.tbl_pt_sales_roster s) ");
		sql.append("and salesrep not in ");
		sql.append("(select concat(s.first_name, ' ', s.last_name) ");
		sql.append(DBUtil.FROM_CLAUSE).append("dbo.tbl_pt_sales_roster s) ");
		sql.append("and salesrep != '' order by FIRST_NM, LAST_NM limit 10;");
		return sql;
	}

}
