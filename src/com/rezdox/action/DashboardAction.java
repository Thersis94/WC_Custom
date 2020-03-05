package com.rezdox.action;

import java.util.ArrayList;
// JDK 1.8.x
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.BusinessAttributeVO;
import com.rezdox.vo.BusinessVO;
// WC Custom Libs
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.ResidenceVO;
// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.tools.NotificationLogVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

/****************************************************************************
 * <b>Title</b>: DashboardAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Gathers the necessary data for the display of the rez dox 
 * dashboard
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Mar 23, 2018
 * @updates:
 ****************************************************************************/
public class DashboardAction extends SimpleActionAdapter {
	private static final String RECENT_SQL = "and create_dt > CURRENT_DATE-90 ";
	private static final String BUS_ID_GROUP = "group by business_id ";
	private static final String BUS_REVIEW_TABLE = "rezdox_member_business_review ";
	private static final String PROJECT_TABLE = "rezdox_project ";
	private static final String PROJECT_COST_VIEW = "rezdox_project_cost_view ";
	private static final String WHERE_BUSINESS_ID = "where business_id = ? ";

	public DashboardAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public DashboardAction(ActionInitVO arg0) {
		super(arg0);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SBUserRole role = (SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA);
		MemberVO member = (MemberVO) req.getSession().getAttribute(Constants.USER_DATA);
		String schema = getCustomSchema();
		Map<String, Object> allData = new HashMap<>();
		boolean isBusinessOnlyRole = RezDoxUtils.REZDOX_BUSINESS_ROLE.equalsIgnoreCase(role.getRoleId());
		boolean isResidenceOnlyRole = RezDoxUtils.REZDOX_RESIDENCE_ROLE.equalsIgnoreCase(role.getRoleId());

		//not strictly a business user?  Then you'll need residence data loaded
		if (!isBusinessOnlyRole)
			allData.putAll(getResidenceData(member, schema));

		//not strictly a residence user?  Then you'll need business data loaded
		if (!isResidenceOnlyRole)
			allData.putAll(getBusinessData(member, schema));

		allData.put("notifications", loadMyNotifications(req));
		allData.put("projecthistory", loadMyRecentProjects(member, isBusinessOnlyRole, isResidenceOnlyRole));

		setModuleData(allData);
	}


	/**
	 * Loads a list of notifications for the logged-in user 
	 * @param profileId
	 * @return
	 * @throws ActionException 
	 */
	@SuppressWarnings("unchecked")
	private List<NotificationLogVO> loadMyNotifications(ActionRequest req) throws ActionException {
		MyNotificationsAction action = new MyNotificationsAction(getDBConnection(), getAttributes());
		action.retrieve(req);
		ModuleVO mod = (ModuleVO) action.getAttribute(Constants.MODULE_DATA);
		return (List<NotificationLogVO> ) mod.getActionData();
	}


	/**
	 * Loads a list of home-history or project-history for the logged-in user 
	 * @param profileId
	 * @return
	 * @throws ActionException 
	 */
	@SuppressWarnings("unchecked")
	private List<GenericVO> loadMyRecentProjects(MemberVO member, boolean isBusinessOnlyRole, boolean isResidenceOnlyRole) {
		String schema = getCustomSchema();
		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		List<GenericVO> data = new ArrayList<>();
		List<Object> params = Arrays.asList(member.getMemberId());
		StringBuilder sql = new StringBuilder(400);
		if (!isBusinessOnlyRole) {
			sql.append("select 'SMT1'+a.project_id as key, a.project_nm as value, coalesce(a.update_dt, a.create_dt) from ").append(schema).append("REZDOX_PROJECT a ");
			sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE r on a.residence_id=r.residence_id ");
			sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on r.residence_id=rm.residence_id and rm.member_id=? and rm.status_flg>=1 ");
			sql.append("where coalesce(a.update_dt, a.create_dt) >= CURRENT_DATE-90 and a.residence_view_flg != 0 ");
			sql.append("order by 3 desc "); //most recent first
			log.debug(sql);
			data.addAll(dbp.executeSelect(sql.toString(), params, new GenericVO()));
			log.debug("datasize=" + data.size());
		}
		
		if (!isResidenceOnlyRole) {
			sql = new StringBuilder(400);
			sql.append("select 'SMT2'+ a.project_id as key, a.project_nm as value, coalesce(a.update_dt, a.create_dt) from ").append(schema).append("REZDOX_PROJECT a ");
			sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_BUSINESS b on a.business_id=b.business_id ");
			sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_BUSINESS_MEMBER_XR bm on b.business_id=bm.business_id and bm.member_id=? and bm.status_flg>=1 ");
			sql.append("where coalesce(a.update_dt, a.create_dt) >= CURRENT_DATE-90 and a.business_view_flg != 0 ");
			sql.append("order by 3 desc"); //most recent first
			log.debug(sql);
			dbp.setGenerateExecutedSQL(log.isDebugEnabled());
			data.addAll(dbp.executeSelect(sql.toString(), params, new GenericVO()));
			log.debug("datasize=" + data.size());
		}

		return data;
	}


	/**
	 * Retrieve the business dashboard data
	 * @param member
	 * @param schema
	 * @return
	 */
	protected Map<String, Object> getBusinessData(MemberVO member, String schema) {
		Map<String, Object> data = new HashMap<>();
		data.put("rewards", getRewardsInfo(member.getMemberId(), schema));
		data.put("connections", getConnections(member.getMemberId(), schema));
		data.put("business", getBusinessInfo(member.getMemberId(), schema));

		return data;
	}

	/**
	 * Gets the summary attributes for the business
	 * @param bus
	 * @param schema
	 */
	protected void getBusinessAttributes(BusinessVO bus, String schema) {
		String bId = bus.getBusinessId();
		StringBuilder sql = new StringBuilder(1144);
		sql.append("select 'all_reviews_avg' as attribute_id, 'all_reviews_avg' as slug_txt, avg(rating_no)::text as value_txt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(BUS_REVIEW_TABLE);
		sql.append(WHERE_BUSINESS_ID).append("and parent_id is null ").append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);
		sql.append("select 'all_reviews_total', 'all_reviews_total', count(*)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(BUS_REVIEW_TABLE);
		sql.append(WHERE_BUSINESS_ID).append("and parent_id is null ").append(RECENT_SQL).append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);
		sql.append("select 'all_projects_count', 'all_projects_count', count(*)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(PROJECT_TABLE);
		sql.append(WHERE_BUSINESS_ID).append(BUS_ID_GROUP);
		//get the grand total of the projects
		sql.append(DBUtil.UNION);
		sql.append("select 'all_projects_value', 'all_projects_value', sum(gt.grand_total)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(" ( ");
		
		sql.append("select project_id, max(project_cost), sum(material_cost), (max(project_cost) + sum(material_cost)) as grand_total, business_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_project_cost_view rpcv ");
		sql.append("where business_view_flg = '1' group by project_id, business_id ");
		
		sql.append(") as gt  ");
		sql.append(WHERE_BUSINESS_ID).append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);		
		
		sql.append("select 'recent_projects_count', 'recent_projects_count', count(*)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(PROJECT_TABLE);
		sql.append(WHERE_BUSINESS_ID).append(RECENT_SQL).append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);
		sql.append("select 'recent_projects_value', 'recent_projects_value', sum(project_cost+material_cost)::text"); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(PROJECT_COST_VIEW);
		sql.append(WHERE_BUSINESS_ID).append(RECENT_SQL).append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);
		sql.append("select 'bus_connections', 'bus_connections', count(*)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_connection");
		sql.append(DBUtil.WHERE_CLAUSE).append("(sndr_business_id=? or rcpt_business_id=?) and approved_flg=1 ");
		sql.append(DBUtil.UNION);
		sql.append("select 'bus_connections', 'bus_connections', count(*)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_connection");
		sql.append(DBUtil.WHERE_CLAUSE).append("(sndr_business_id=? or rcpt_business_id=?) and approved_flg=1 ");
		sql.append(DBUtil.UNION);
		sql.append("select 'bus_connections_lic', 'bus_connections_lic', quota_no::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_connection_quota_view where business_id=? ");
		sql.append("order by slug_txt ");
		
		// Add the attributes to the business
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		List<Object> vals = Arrays.asList(bId,bId,bId,bId,bId,bId,bId,bId,bId,bId,bId);
		log.debug(sql +"|"+vals );
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		bus.addAttributes(db.executeSelect(sql.toString(), vals, new BusinessAttributeVO()));
	}

	/**
	 * Gets a list of businesses assigned to the member
	 * @param memberId
	 * @param schema
	 * @return
	 */
	protected List<BusinessVO> getBusinessInfo(String memberId, String schema) {
		StringBuilder sql = new StringBuilder(512);
		sql.append("select b.* ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_business_member_xr a ");  
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business b on a.business_id=b.business_id "); 
		sql.append("where member_id=? and a.status_flg=1 order by business_nm");

		DBProcessor db = new DBProcessor(getDBConnection());
		List<BusinessVO> businesses = db.executeSelect(sql.toString(), Arrays.asList(memberId), new BusinessVO());
		for(BusinessVO business : businesses) 
			getBusinessAttributes(business, schema);

		return businesses;
	}

	/**
	 * Retrieve the data for the residence dashboard
	 * @param member
	 * @param schema
	 * @return
	 */
	protected Map<String, Object> getResidenceData(MemberVO member, String schema) {
		Map<String, Object> data = new HashMap<>();
		data.put("residence", getResidenceInfo(member.getMemberId(), schema));
		data.put("rewards", getRewardsInfo(member.getMemberId(), schema));
		data.put("connections", getConnections(member.getMemberId(), schema));
		data.put("reviews", getReviews(member.getMemberId(), schema));
		return data;
	}

	/**
	 * retrieves the number of connections assigned to a member
	 * @param memberId
	 * @param schema
	 * @return
	 */
	protected GenericVO getConnections(String memberId, String schema) {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select count(*) as key from ").append(schema).append("rezdox_connection ");
		sql.append("where (sndr_member_id=? or rcpt_member_id=?) and approved_flg=1");

		DBProcessor db = new DBProcessor(getDBConnection());
		List<GenericVO> data = db.executeSelect(sql.toString(), Arrays.asList(memberId, memberId), new GenericVO());
		return data.isEmpty() ? new GenericVO(0,0) : data.get(0);
	}

	/**
	 * Retrieves review summary information
	 * @param memberId
	 * @param schema
	 * @return
	 */
	protected GenericVO getReviews(String memberId, String schema) {
		StringBuilder sql = new StringBuilder(768);
		sql.append("select count(*) as key, coalesce(sum(rating_no), 0) as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(BUS_REVIEW_TABLE);
		sql.append("where member_id=? and parent_id is null ");

		DBProcessor db = new DBProcessor(getDBConnection());
		List<GenericVO> data = db.executeSelect(sql.toString(), Arrays.asList(memberId), new GenericVO());
		return data.isEmpty() ? new GenericVO(12,3.3) : data.get(0);
	}

	/**
	 * Retrieves the summarized residence data for each location for a given member
	 * @param memberId
	 * @param schema
	 * @return
	 */
	protected List<ResidenceVO> getResidenceInfo(String memberId, String schema) {
		StringBuilder sql = new StringBuilder(768);
		sql.append("select c.*, d.*, coalesce(gt.grand_total, 0) as projects_total, coalesce(p.project_valuation, 0) as projects_valuation, ");
		sql.append("coalesce(i.inventory_total, 0) as inventory_total ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_residence_member_xr b ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_residence c on b.residence_id = c.residence_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_residence_attribute d ");
		sql.append("on c.residence_id = d.residence_id ");
		sql.append("and slug_txt in ('lastSoldPrice', 'RESIDENCE_ZESTIMATE', 'RESIDENCE_WALK_SCORE', ");
		sql.append("'RESIDENCE_TRANSIT_SCORE', 'RESIDENCE_SUN_NUMBER') ");
		//join projects for IMPROVEMENT projects total
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select a.residence_id, sum(project_cost+material_cost) as project_total, sum(b.project_valuation) as project_valuation ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_residence_member_xr a ");
		sql.append(DBUtil.INNER_JOIN).append(" ( ");
		
		sql.append("SELECT residence_id, business_id, project_id, residence_view_flg, business_view_flg, create_dt, ");
		sql.append("project_cost, sum(material_cost) as material_cost, project_valuation, is_improvement ");
		sql.append("from ").append(schema).append(PROJECT_COST_VIEW);
		sql.append("group by residence_id, business_id, project_id, residence_view_flg, business_view_flg, create_dt, project_cost, project_valuation, is_improvement ");
		sql.append(" ) as b on a.residence_id=b.residence_id  ");
		
		sql.append("and b.is_improvement=1 and b.residence_view_flg=1 ");
		sql.append("where a.member_id=? and a.status_flg=1 ");
		sql.append("group by a.residence_id ");
		sql.append(") as p on c.residence_id=p.residence_id ");
		//join inventory (treasure box) for personal items total
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select a.residence_id, sum(valuation_no) as inventory_total ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_residence_member_xr a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_treasure_item b on a.residence_id=b.residence_id ");
		sql.append("where a.member_id=? and a.status_flg=1 ");
		sql.append("group by a.residence_id ");
		sql.append(") as i on c.residence_id=i.residence_id ");
		//get the grand total of the projects
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select sum(m.grand_total) as grand_total, residence_id ");
		sql.append(DBUtil.FROM_CLAUSE).append("( select project_id, max(project_cost), sum(material_cost), (max(project_cost) + sum(material_cost)) as grand_total,	residence_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_project_cost_view rpcv ");
		sql.append("where residence_view_flg = '1' 	group by project_id, residence_id ");
		sql.append(") as m group by residence_id ) as gt on c.residence_id = gt.residence_id  ");
		
		sql.append("where b.member_id=? and b.status_flg=1 order by c.create_dt");
		

		DBProcessor db = new DBProcessor(getDBConnection());
		List<Object> params = Arrays.asList(memberId, memberId, memberId);
		log.debug(sql +"|"+params);
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<ResidenceVO> data = db.executeSelect(sql.toString(), params, new ResidenceVO(), "residence_id");
		
		if(data != null && ! data.isEmpty())log.debug( data.get(0));
		
		return data;
	}

	/**
	 * Gets the current number of rewards and the number earned in last 90 days
	 * @param memberId
	 * @param schema
	 * @return
	 */
	protected GenericVO getRewardsInfo(String memberId, String schema) {
		StringBuilder sql = new StringBuilder(512);
		sql.append("select a.member_id, coalesce(sum(point_value_no), 0) as key, ");
		sql.append("coalesce(recent_rewards_total, 0) as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member_reward a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select member_id, sum(point_value_no) recent_rewards_total ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_member_reward a ");
		sql.append("where member_id=? ");
		sql.append(RECENT_SQL);
		sql.append("group by member_id ");
		sql.append(") as b on a.member_id=b.member_id ");
		sql.append("where a.member_id=? ");
		sql.append("group by a.member_id, recent_rewards_total ");

		DBProcessor db = new DBProcessor(getDBConnection());
		List<GenericVO> rewards = db.executeSelect(sql.toString(), Arrays.asList(memberId, memberId), new GenericVO());

		if (! rewards.isEmpty()) return rewards.get(0);
		return new GenericVO(0,0);
	}
}