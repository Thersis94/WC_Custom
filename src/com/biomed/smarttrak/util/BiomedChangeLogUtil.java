package com.biomed.smarttrak.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.BiomedChangeLogDecoratorAction;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.changelog.ChangeLogUtil;
import com.smt.sitebuilder.changelog.ChangeLogVO;

/****************************************************************************
 * <b>Title</b>: BiomedChangelogUtil.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Utility Class that Extends ChangeLogUtil with biomed
 * specific information.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Mar 9, 2017
 ****************************************************************************/
public class BiomedChangeLogUtil extends ChangeLogUtil {

	/**
	 * @param dbConn
	 * @param attributes
	 */
	public BiomedChangeLogUtil(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super(dbConn, attributes);
	}

	/**
	 * Helper method that loads ChangeLog Records from the Database.
	 * @param origId
	 * @param changeLogId
	 * @return
	 */
	public List<Object> loadRecords(String wcOrigKeyId, String changeLogId, String orgId, boolean hideReviewed) {

		String sql = formatSqlQuery(wcOrigKeyId, changeLogId, orgId, hideReviewed);
		List<Object> params = new ArrayList<>();
		if(!StringUtil.isEmpty(wcOrigKeyId)) params.add(wcOrigKeyId);
		if(!StringUtil.isEmpty(changeLogId)) params.add(changeLogId);
		if(!StringUtil.isEmpty(orgId)) params.add(orgId);

		List<Object> data = new DBProcessor(dbConn).executeSelect(sql, params, new ChangeLogVO());

		updateNames(data);

		return data;
	}

	/**
	 * Helper method returns ChangeLog Query.
	 * @param appOrigId
	 * @param changeLogId
	 * @param orgId
	 * @return
	 */
	protected String formatSqlQuery(String wcOrigKeyId, String changeLogId, String orgId, boolean hideReviewed) {
		String updateType = StringUtil.checkVal(BiomedChangeLogDecoratorAction.EditPath.UPDATE, true);
		
		StringBuilder sql = new StringBuilder();
		sql.append("select a.*, b.*, d.first_nm as creator_first_nm, d.last_nm as creator_last_nm, ");
		sql.append("c.first_nm as disposition_first_nm, c.last_nm as disposition_last_nm ");
		sql.append("from change_log a inner join wc_sync b on ");
		sql.append("a.wc_sync_id = b.wc_sync_id ");
		sql.append("left outer join profile c on b.disposition_by_id = c.profile_id ");
		sql.append("left outer join profile d on b.admin_profile_id = d.profile_id ");
		sql.append("where 1 = 1 ");

		if(hideReviewed) sql.append("and b.disposition_by_id is null ");
		if(!StringUtil.isEmpty(wcOrigKeyId)) sql.append("and b.wc_orig_key_id = ? ");
		if(!StringUtil.isEmpty(changeLogId)) sql.append("and a.change_log_id = ? ");
		if(!StringUtil.isEmpty(orgId)) sql.append("and b.organization_id = ? ");

		sql.append("and type_cd != ").append(updateType); //exclude updates from listing
		sql.append(" order by a.create_dt desc");
		return sql.toString();
	}
}

