package com.rezdox.action;

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
	private static final String RECENT_SQL = "and create_dt > now() - interval '90 day' ";
	private static final String BUS_ID_GROUP = "group by business_id ";
	private static final String BUS_REVIEW_TABLE = "rezdox_member_business_review ";
	private static final String PROJECT_TABLE = "rezdox_project ";
	private static final String WHERE_BUSINESS_ID = "where business_id = ? ";
	
	/**
	 * 
	 */
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
		
		SBUserRole role = ((SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA));
		MemberVO member = (MemberVO) req.getSession().getAttribute(Constants.USER_DATA);
		String schema = (String)attributes.get(Constants.CUSTOM_DB_SCHEMA);
		
		// Retrieve the business or residence data based upon the user role
		if ("REZDOX_RESIDENCE".equalsIgnoreCase(role.getRoleId())) {
			this.setModuleData(getResidenceData(member, schema));
		} else if ("REZDOX_BUSINESS".equalsIgnoreCase(role.getRoleId())) { 
			this.setModuleData(getBusinessData(member, schema));
		} else { 
			Map<String, Object> allData = getBusinessData(member, schema);
			allData.putAll(getResidenceData(member, schema));
			this.setModuleData(allData);
		}
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
		sql.append(WHERE_BUSINESS_ID).append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);
		sql.append("select 'all_reviews_total', 'all_reviews_total', count(*)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(BUS_REVIEW_TABLE);
		sql.append(WHERE_BUSINESS_ID).append(RECENT_SQL).append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);
		sql.append("select 'all_projects_count', 'all_projects_count', count(*)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(PROJECT_TABLE);
		sql.append(WHERE_BUSINESS_ID).append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);
		sql.append("select 'all_projects_value', 'all_projects_value', sum(total_no)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(PROJECT_TABLE);
		sql.append(WHERE_BUSINESS_ID).append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);
		sql.append("select 'recent_projects_count', 'recent_projects_count', count(*)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(PROJECT_TABLE);
		sql.append(WHERE_BUSINESS_ID).append(RECENT_SQL).append(BUS_ID_GROUP);
		sql.append(DBUtil.UNION);
		sql.append("select 'recent_projects_value', 'recent_projects_value', sum(total_no)::text "); 
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(PROJECT_TABLE);
		sql.append(WHERE_BUSINESS_ID).append(RECENT_SQL).append(BUS_ID_GROUP);
		sql.append("order by slug_txt ");
		
		// Add the attributes to the business
		DBProcessor db = new DBProcessor(getDBConnection());
		List<Object> vals = Arrays.asList(bId,bId,bId,bId,bId,bId);
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
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business b on a.business_id = b.business_id "); 
		sql.append("where member_id = ? order by business_nm");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<BusinessVO> businesses = db.executeSelect(sql.toString(), Arrays.asList(memberId), new BusinessVO());
		for(BusinessVO business : businesses) getBusinessAttributes(business, schema);
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
		sql.append("where (sndr_member_id = ? ");
		sql.append("or rcpt_member_id = ? ) ");
		sql.append("and approved_flg = 1 ");
		
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
		sql.append("where member_id = ? ");
		
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
		sql.append("select c.*, d.*, coalesce(p.project_total, 0) as projects_total ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_residence_member_xr b ");
		sql.append("inner join ").append(schema).append("rezdox_residence c on b.residence_id = c.residence_id ");
		sql.append("left outer join ").append(schema).append("rezdox_residence_attribute d ");
		sql.append("on c.residence_id = d.residence_id ");
		sql.append("and slug_txt in ('lastSoldPrice', 'RESIDENCE_ZESTIMATE', 'RESIDENCE_WALK_SCORE', ");
		sql.append("'RESIDENCE_TRANSIT_SCORE', 'RESIDENCE_SUN_NUMBER') ");
		sql.append("left outer join ( ");
		sql.append("select a.residence_id, sum(total_no) as project_total ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("rezdox_residence_member_xr a ");
		sql.append("inner join ").append(schema).append("rezdox_project b on a.residence_id = b.residence_id ");
		sql.append("where a.member_id = ? ");
		sql.append("group by a.residence_id ");
		sql.append(") as p on c.residence_id = p.residence_id ");
		sql.append("where b.member_id = ? ");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), Arrays.asList(memberId, memberId), new ResidenceVO(), "residence_id");
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
		sql.append("from ").append(schema).append("rezdox_member_reward a ");
		sql.append("left outer join ( ");
		sql.append("select member_id, sum(point_value_no) recent_rewards_total ");
		sql.append("from ").append(schema).append("rezdox_member_reward ");
		sql.append("where member_id = ? ");
		sql.append(RECENT_SQL);
		sql.append("group by member_id ");
		sql.append(") as b on a.member_id = b.member_id ");
		sql.append("where a.member_id = ? ");
		sql.append("group by a.member_id, recent_rewards_total ");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		List<GenericVO> rewards = db.executeSelect(sql.toString(), Arrays.asList(memberId, memberId), new GenericVO());
		
		if (! rewards.isEmpty()) return rewards.get(0);
		return new GenericVO(0,0);
	}
}

