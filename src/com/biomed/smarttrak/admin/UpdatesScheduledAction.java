package com.biomed.smarttrak.admin;

//jdk 1.8.x
import java.util.ArrayList;
import java.util.List;


//WC_Custom libs
import com.biomed.smarttrak.vo.UpdateVO;

//base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * Title: UpdatesScheduledAction.java <p/>
 * Project: WC_Custom <p/>
 * Description: Handles retrieving the updates for a scheduled email send<p/>
 * Copyright: Copyright (c) 2017<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since Apr 10, 2017
 ****************************************************************************/

public class UpdatesScheduledAction extends SBActionAdapter {
	
	/**
	 * No-arg constructor for initialization
	 */
	public UpdatesScheduledAction(){
		super();
	}
	/**
	 * 
	 * @param init
	 */
	public UpdatesScheduledAction(ActionInitVO init){
		super(init);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException{
		
		//get req params
		String timeRangeCd = StringUtil.checkVal(req.getParameter("timeRangeCd"));
		String profileId = StringUtil.checkVal(req.getParameter("profileId"));
		
		//get list of updates
		List<Object> updates = getUpdates(profileId, timeRangeCd);
		
		putModuleData(updates);
	}
	
	/**
	 * Returns a list of scheduled updates for a specified profile
	 * @param profileId
	 * @param timeRangeCd
	 * @return
	 */
	protected  List<Object> getUpdates(String profileId, String timeRangeCd){
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		
		//build the query
		String sql = fetchScheduledSQL(schema, timeRangeCd);
		log.debug("Scheduled updates query: " + sql);
		
		//build params
		List<Object> params = new ArrayList<>();
		params.add(profileId);
		
		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdateVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}

	/**
	 * Builds the scheduled query
	 * @param schema
	 * @param timeRangeCd
	 * @return
	 */
	protected String fetchScheduledSQL(String schema, String timeRangeCd){		
		StringBuilder sql = new StringBuilder(800);
		sql.append("select distinct up.*, us.* from core.profile p ");
		sql.append("inner join ").append(schema).append("biomedgps_user u on p.profile_id = u.profile_id ");
		sql.append("inner join ").append(schema).append("biomedgps_account a on a.account_id = u.account_id ");
		sql.append("inner join ").append(schema).append("biomedgps_account_acl sec on sec.account_id = ");
		sql.append("a.account_id and sec.updates_no = 1 ");
		sql.append("inner join ").append(schema).append("biomedgps_section s on s.section_id = sec.section_id ");
		sql.append("inner join ").append(schema).append("biomedgps_update_section us on us.section_id = s.parent_id ");
		sql.append("inner join ").append(schema).append("biomedgps_update up on up.update_id = us.update_id ");
		sql.append("where p.profile_id = ? ");
		if("weekly".equalsIgnoreCase(timeRangeCd)){
			sql.append("and up.create_dt >= cast(date_trunc('week', current_date) as date) - 1 ");
			sql.append("and up.create_dt < cast(date_trunc('week', current_date) as date) + 5 ");
		}else{//default to daily
			sql.append("and up.create_dt >= date_trunc('day', current_timestamp) - interval '1' day ");
			sql.append("and up.create_dt < date_trunc('day', current_timestamp) ");
		}
		sql.append("order by up.create_dt desc ");
		
		return sql.toString();	
	}
}
