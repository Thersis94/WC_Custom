package com.restpeer.action.admin;

import java.util.ArrayList;
// JDK 1.8.x
import java.util.List;
import java.util.Map;

// RP Libs
import com.restpeer.data.MemberVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;

/****************************************************************************
 * <b>Title</b>: MemberWidget.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Manages the categories of attributes for locations
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 13, 2019
 * @updates:
 ****************************************************************************/

public class MemberWidget extends SBActionAdapter {

	/**
	 * Json key to access this action
	 */
	public static final String AJAX_KEY = "member";
	
	/**
	 * 
	 */
	public MemberWidget() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public MemberWidget(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * @param actionInit
	 */
	public MemberWidget(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		setModuleData(getAttributeData(req.getParameter("memberId")));
	}
	
	/**
	 * Get a list of categories
	 * @return
	 */
	public List<MemberVO> getAttributeData(String memberId) {
		List<Object> vals = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(128);
		sql.append("select a.*, coalesce(b.locations_no, 0) as locations_no from ");
		sql.append(getCustomSchema()).append("rp_member a ");
		sql.append("left outer join ( ");
		sql.append("select member_id, count(*) locations_no ");
		sql.append("from ").append(getCustomSchema()).append("rp_member_location ");
		sql.append("group by member_id ");
		sql.append(") as b on a.member_id = b.member_id ");
		
		// If the member id is passed, add to the filter
		if (! StringUtil.isEmpty(memberId)) {
			sql.append("where a.member_id = ? ");
			vals.add(memberId);
		}
		
		sql.append("order by member_nm");
		log.debug(sql.length() + "|" + sql + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), vals, new MemberVO());
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		MemberVO member = new MemberVO(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(true);
		try {
			db.save(member);
			log.info(db.getExecutedSql());
			setModuleData(member);
		} catch (Exception e) {
			log.error("Unable to add member:" + member, e);
			putModuleData(member, 0, false, e.getLocalizedMessage(), true);
		}
	}
}
