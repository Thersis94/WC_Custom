package com.rezdox.action;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletResponse;

import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.ConnectionReportVO;
import com.rezdox.vo.ConnectionVO;
import com.rezdox.vo.MemberVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <b>Title</b>: ConnectionAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> This class controls building and maintaining connections between users and businesses
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author ryan
 * @version 3.0
 * @since Mar 26, 2018
 * @updates:
 ****************************************************************************/
public class ConnectionAction extends SimpleActionAdapter {
	public static final String MEMBER = "member";
	public static final String BUSINESS = "business";
	public static final String CONNECTION_DATA = "connectionData";
	public static final String BUSINESS_OPTIONS = "businessOptions";

	public ConnectionAction() {
		super();
	}

	public ConnectionAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("R################## Connections retrieve called");
		req.setAttribute(BUSINESS_OPTIONS, loadBusinessOptions(StringUtil.checkVal(RezDoxUtils.getMemberId(req))));
		
		if (req.getBooleanParameter("addConn")) {
			log.debug("### called backend");
			String search = StringUtil.checkVal(req.getParameter("search"));
			String sendingId = StringUtil.checkVal(req.getParameter("sendingId"));
			
			if(MEMBER.equalsIgnoreCase(StringUtil.checkVal(req.getParameter("receiverType")))) {
				putModuleData(searchMembers(search,sendingId));
			}else {
				putModuleData(searchBusiness(search,sendingId));
			}
		}
		
		//populate a list of connections per request
		if(req.getBooleanParameter("myPros")) {
			//ret pros
		}
			
		//if there is a target id use the token to identify the type of id sent and make the correct
		//  request. getting member data is differnt then getting business data.  
		if (!StringUtil.isEmpty(req.getParameter("targetId"))) {
			String[] idParts = StringUtil.checkVal(req.getParameter("targetId")).split("_");
			if (idParts != null && idParts.length > 0 && "m".equalsIgnoreCase(idParts[0])) {
				setModuleData(generateConnections(idParts[1], MEMBER, req));
			}	
			if (idParts != null && idParts.length > 0 && "b".equalsIgnoreCase(idParts[0])) {
				setModuleData(generateConnections(idParts[1], BUSINESS, req));
			}
		}
		
		if(req.getBooleanParameter("generateCookie")) {
			int count = getMemeberConnectionCount(req);
			log.debug(" found " +count+ "Connection");
			//set the total in a cookie.  This may be excessive for repeat calls to the rewards page, but ensures cached data is flushed
			HttpServletResponse resp = (HttpServletResponse) getAttribute(GlobalConfig.HTTP_RESPONSE);
			CookieUtil.add(resp, "rezdoxConnectionPoints", String.valueOf(count), "/", -1);
		}
	}
	
	/**
	 * gets the number of connections the member has
	 * @param req 
	 * @return
	 */
	private int getMemeberConnectionCount(ActionRequest req) {
		StringBuilder sql = new StringBuilder();
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		params.add(RezDoxUtils.getMemberId(req));
		params.add(RezDoxUtils.getMemberId(req));
		sql.append("select *  from ").append(schema).append("rezdox_connection where sndr_member_id = ? or rcpt_member_id = ? ");	
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<ConnectionVO> data = dbp.executeSelect(sql.toString(), params, new ConnectionVO());
		
		log.debug(" sql " +sql.toString()+ "|"+params );
		
		return data.size();
	}

	/**
	 * controls when a business is looking for new connections
	 * @param search
	 * @param sendingId 
	 * @return
	 */
	private List<BusinessVO> searchBusiness(String search, String sendingId) {
		log.debug("searching businesses");
		StringBuilder sql = new StringBuilder(88);
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		String[] idParts = sendingId.split("_");
		
		sql.append("select * from ").append(schema).append("rezdox_business where LOWER(business_nm) like ? ");
		params.add("%"+search.toLowerCase()+"%");
		sql.append("and business_id != ? and business_id not in ( select case  ");
		params.add(idParts[1]);
		
		if("m".equalsIgnoreCase(idParts[0])) {
			//member looking for business
			sql.append("when sndr_member_id is not null and sndr_member_id = ? and rcpt_business_id is not null then rcpt_business_id ");
			sql.append("when rcpt_member_id is not null and rcpt_member_id = ? and sndr_business_id is not null then sndr_business_id ");		
			params.add(idParts[1]);
			params.add(idParts[1]);
		}else {
			//business looking for business
			sql.append("when sndr_business_id is not null and sndr_business_id = ? and rcpt_business_id is not null then rcpt_business_id ");
			sql.append("when rcpt_business_id is not null and rcpt_business_id = ? and sndr_business_id is not null then sndr_business_id ");
			params.add(idParts[1]);
			params.add(idParts[1]);
		}
		sql.append("else '-1' end as business_id from ").append(schema).append("rezdox_connection group by business_id ) ");
		sql.append(" order by business_nm asc ");
		
		log.debug("sql " + sql.toString() +"|"+params);
		//generate a list of VO's
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		return dbp.executeSelect(sql.toString(), params, new BusinessVO());
	}

	/**
	 * controls when a memeber is looking for a new connection
	 * @param search
	 * @param sendingId 
	 * @return
	 */
	private List<MemberVO> searchMembers(String search, String sendingId) {
		log.debug("searching members");
		StringBuilder sql = new StringBuilder(88);
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		String[] idParts = sendingId.split("_");
		
		sql.append("select * from ").append(schema).append("rezdox_member where LOWER(first_nm || ' ' || last_nm) like ? ");
		params.add("%"+search.toLowerCase()+"%");
		sql.append("and member_id != ? and member_id not in ( select case  ");
		params.add(idParts[1]);
		
		if("m".equalsIgnoreCase(idParts[0])) {
			//member looking for member
			sql.append("when sndr_member_id is not null and sndr_member_id = ? and rcpt_member_id is not null then rcpt_member_id ");
			sql.append("when rcpt_member_id is not null and rcpt_member_id = ? and sndr_member_id is not null then sndr_member_id ");
			params.add(idParts[1]);
			params.add(idParts[1]);
		}else {
			//business looking for member
			sql.append("when sndr_business_id is not null and sndr_business_id = ? and rcpt_member_id is not null then rcpt_member_id ");
			sql.append("when rcpt_business_id is not null and rcpt_business_id = ? and sndr_member_id is not null then sndr_member_id ");
			params.add(idParts[1]);
			params.add(idParts[1]);
		}
		
		sql.append("else '-1' end as member_id from ").append(schema).append("rezdox_connection group by member_id ) ");
		sql.append(" order by last_nm, first_nm asc ");
		
		log.debug("sql " + sql.toString() +"|"+params);
		//generate a list of VO's
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<MemberVO> data = dbp.executeSelect(sql.toString(), params, new MemberVO());
		
		log.debug("number of members found " + data.size());

		return data;
	}

	/**
	 * @param checkVal
	 * @return 
	 */
	private List<GenericVO> loadBusinessOptions(String memberId) {
		log.debug("loading busines list for member");
		StringBuilder sql = new StringBuilder(230);
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		
		sql.append("select b.business_nm as value, b.business_id as key from ").append(schema).append("rezdox_business_member_xr bmxr ");
		sql.append("inner join ").append(schema).append("rezdox_business b on bmxr.business_id = b.business_id and bmxr.member_id = ? ");
		params.add(memberId);
		
		log.debug(" sql " + sql.toString() +"|"+ memberId);
		
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<GenericVO> data = dbp.executeSelect(sql.toString(), params, new GenericVO());
		
		if (data != null)
		log.debug("number of business found for member " + data.size());
				
		return data;
	}

	/**
	 * @param targetId
	 * @param inqueryType
	 * @param req 
	 * @param b
	 * @return 
	 */
	private List<ConnectionReportVO> generateConnections(String targetId, String inqueryType, ActionRequest req) {
		log.debug("generating connections");
		String order = DBUtil.ORDER_BY + " approved_flg asc, create_dt desc ";
		if (req.hasParameter("order") && !StringUtil.isEmpty(req.getParameter("order"))) order = DBUtil.ORDER_BY + req.getParameter("order") + " " + req.getParameter("dir");
		
		
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(2680);
		
		String idField = BUSINESS; 
		if (inqueryType.equals(MEMBER)) {
			idField = MEMBER;
		}
		
		//getting the records where target id sent the connection to an other member
		sql.append("select * from ( ");
		sql.append("select connection_id, sndr_").append(idField).append("_id as sndr_id, b.member_id as rcpt_id, 'sending' as direction_cd, b.first_nm, b.last_nm, b.profile_pic_pth, ");
		sql.append("pa.city_nm, pa.state_cd, '' as business_summary, cast(0 as numeric) as rating, b.create_dt, 'MEMBER' as category_nm, approved_flg, privacy_flg ");
		sql.append("from ").append(schema).append("rezdox_connection a ");
		sql.append("inner join ").append(schema).append("rezdox_member b on a.rcpt_member_id = b.member_id ");
		sql.append("inner join profile_address pa on b.profile_id = pa.profile_id ");
		sql.append("where sndr_").append(idField).append("_id = ? ");
		params.add(targetId);
		//getting the records where target id was sent a connection by an other memeber
		sql.append("union all ");
		sql.append("select connection_id, b.member_id as sender_id, rcpt_").append(idField).append("_id as rcpt_id, 'receiving' as directionCd, b.first_nm, b.last_nm, b.profile_pic_pth, ");
		sql.append("pa.city_nm, pa.state_cd, '', cast(0 as numeric) as rating, b.create_dt, 'MEMBER', approved_flg, privacy_flg ");
		sql.append("from ").append(schema).append("rezdox_connection a " );
		sql.append("inner join ").append(schema).append("rezdox_member b on a.sndr_member_id = b.member_id ");
		sql.append("inner join profile_address pa on b.profile_id = pa.profile_id ");
		sql.append("where rcpt_").append(idField).append("_id = ? ");
		params.add(targetId);
		//getting the records where target id sent a connection to a business
		sql.append("union all ");
		sql.append("select connection_id, sndr_").append(idField).append("_id , b.business_id, 'sending', '', business_nm, b.photo_url, ");
		sql.append("city_nm, state_cd, value_txt, cast(coalesce(rating, 0) as numeric) , b.create_dt, category_nm, approved_flg, privacy_flg ");
		sql.append("from ").append(schema).append("rezdox_connection a ");
		sql.append("inner join ").append(schema).append("rezdox_business b on a.rcpt_member_id = b.business_id ");
		sql.append("left outer join ( ");
		sql.append("select business_id, avg(rating_no) as rating from ").append(schema).append("rezdox_member_business_review ");
		sql.append("group by business_id");
		sql.append(") as r on b.business_id = r.business_id ");
		sql.append("left outer join ( ");
		sql.append("select business_id, value_txt from ").append(schema).append("rezdox_business_attribute ");
		sql.append("where slug_txt = 'BUSINESS_SUMMARY' ");
		sql.append(") as d on b.business_id = d.business_id ");
		sql.append("left outer join ( ");
		sql.append("select business_id, category_nm from ").append(schema).append("rezdox_business_category_xr a ");
		sql.append("inner join ").append(schema).append("rezdox_business_category b on a.business_category_cd = b.business_category_cd ");
		sql.append(") as cat on b.business_id = cat.business_id ");
		sql.append("where sndr_").append(idField).append("_id = ? ");
		params.add(targetId);
		//getting the records where a business sent a connection to the target id
		sql.append("union all ");
		sql.append("select connection_id, b.business_id, rcpt_").append(idField).append("_id, 'receiving', '', business_nm, b.photo_url, ");
		sql.append("city_nm, state_cd, value_txt, cast( coalesce(rating, 0)as numeric), b.create_dt, category_nm, approved_flg, privacy_flg ");
		sql.append("from ").append(schema).append("rezdox_connection a ");
		sql.append("inner join ").append(schema).append("rezdox_business b on a.sndr_member_id = b.business_id ");
		sql.append("left outer join ( ");
		sql.append("select business_id, avg(rating_no) as rating from ").append(schema).append("rezdox_member_business_review ");
		sql.append("group by business_id ");
		sql.append(") as r on b.business_id = r.business_id ");
		sql.append("left outer join ( ");
		sql.append("select business_id, value_txt from ").append(schema).append("rezdox_business_attribute ");
		sql.append("where slug_txt = 'BUSINESS_SUMMARY' ");
		sql.append(") as d on b.business_id = d.business_id ");
		sql.append("left outer join ( ");
		sql.append("select business_id, category_nm from ").append(schema).append("rezdox_business_category_xr a ");
		sql.append("inner join ").append(schema).append("rezdox_business_category b on a.business_category_cd = b.business_category_cd ");
		sql.append(") as cat on b.business_id = cat.business_id ");
		sql.append("where rcpt_").append(idField).append("_id = ? ");
		params.add(targetId);
		sql.append(") as all_member ");
		//where for whole multiplexed union would go here
		generateWhere(sql, req, params);
		sql.append(order);
		
		log.debug("sql " + sql.toString()+"|"+ params);
		
		//generate a list of VO's
		DBProcessor dbp = new DBProcessor(dbConn, schema);

		return dbp.executeSelect(sql.toString(), params, new ConnectionReportVO());
		
	}

	/**
	 * @param sql
	 * @param req
	 * @param params 
	 * @param approvedOnly 
	 */
	private void generateWhere(StringBuilder sql, ActionRequest req, List<Object> params) {
		sql.append("where 1=1 ");

		if(req.hasParameter("categorySearch") && !"All".equalsIgnoreCase(StringUtil.checkVal(req.getParameter("categorySearch")))){
			sql.append("and category_nm = ? ");
			log.debug("category name " + req.getParameter("categorySearch"));
			params.add(req.getParameter("categorySearch"));
		}
		
		if(req.hasParameter("stateSearch")) {
			sql.append("and state_cd = ? ");
			log.debug("state search " + req.getParameter("stateSearch"));
			params.add(req.getParameter("stateSearch"));
		}
		
		if(req.hasParameter("search")) {
			log.debug("search param " + req.getParameter("search"));
			sql.append("and (LOWER(first_nm) like ? or LOWER(last_nm) like ? ) ");
			params.add("%"+StringUtil.checkVal(req.getParameter("search")).toLowerCase()+"%");
			params.add("%"+StringUtil.checkVal(req.getParameter("search")).toLowerCase()+"%");
		}
		
		if(req.getIntegerParameter("approvedFlag") != null && req.getIntegerParameter("approvedFlag") < 3 && req.getIntegerParameter("approvedFlag") >= 0) {
			sql.append("and approved_flg = ? ");
			log.debug("approved "+req.getIntegerParameter("approvedFlag"));
			params.add(req.getIntegerParameter("approvedFlag"));
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug(" Connections build called");
		ConnectionVO cvo = new ConnectionVO(req);
		log.debug("# cvo "+cvo);
		
		String schema = getCustomSchema();
		DBProcessor dbp = new DBProcessor(dbConn, schema);
		
		if(req.getBooleanParameter("approvalUpdate")) {
			try {
				dbp.getByPrimaryKey(cvo);
			} catch (InvalidDataException | DatabaseException e) {
				log.error("could not fill connection vo ",e);
			}
			log.debug("# cvo "+cvo);
			cvo.setApprovedFlag(req.getIntegerParameter("approvedFlag"));
			
		}
		
		//if a business is the recipient instant approve
		if (cvo.getRecipientBusinessId() != null && !req.getBooleanParameter("approvalUpdate")) {
			cvo.setApprovedFlag(1);
		}
		
		try {
				dbp.save(cvo);
		} catch (InvalidDataException | DatabaseException e) {
				log.error("could not save connection ",e);
		}

	}
}
