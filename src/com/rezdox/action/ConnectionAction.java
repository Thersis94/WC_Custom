package com.rezdox.action;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.ConnectionReportVO;
import com.rezdox.vo.ConnectionVO;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.MyProVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DBUtil.SortDirection;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.SQLTotalVO;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.security.SBUserRole;

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
	// Maps the sort field values passed in to database fields
	private Map<String, String> sortFields;

	public static final String MEMBER = "member";
	public static final String BUSINESS = "business";
	public static final String CONNECTION_DATA = "connectionData";
	public static final String BUSINESS_OPTIONS = "businessOptions";
	public static final String SEARCH = "search";
	public static final String REZDOX_CONNECTION_A= "rezdox_connection a ";
	public static final String STATE_SEARCH = "stateSearch";
	public static final String APPROVED_FLAG = "approvedFlag";
	public static final String CATEGORY_SEARCH = "categorySearch";
	public static final String TARGET_ID = "targetId"; 
	public static final String REZDOX_CONNECTION_POINTS = "rezdoxConnectionPoints";
	private static final String ID_SUFFIX = "_id = ? ";

	public ConnectionAction() {
		super();
		sortFields = new HashMap<>();
		sortFields.put("sortableName", "sortable_nm");
		sortFields.put("cityName", "city_nm");
		sortFields.put("stateCode", "state_cd");
		sortFields.put(APPROVED_FLAG, "approved_flg");
	}

	public ConnectionAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * @param dbConn
	 * @param attributes
	 */
	public ConnectionAction(Connection dbConn, Map<String, Object> attributes) {
		this();
		this.setAttributes(attributes);
		this.setDBConnection((SMTDBConnection) dbConn);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("Connections retrieve called");

		// Member/business directory
		if (req.hasParameter("directory")) {
			new DirectoryAction(dbConn, attributes).getDirectory(req);
			return;
		}

		if (req.hasParameter("ListBusinesses")) {
			putModuleData(loadBusinessOptions(StringUtil.checkVal(RezDoxUtils.getMemberId(req))));
		}

		if (req.getBooleanParameter("addConn")) {
			String search = StringUtil.checkVal(req.getParameter(SEARCH));
			String sendingId = StringUtil.checkVal(req.getParameter("sendingId"));

			if (MEMBER.equalsIgnoreCase(StringUtil.checkVal(req.getParameter("receiverType")))) {
				putModuleData(searchMembers(search,sendingId));
			} else {
				putModuleData(searchBusiness(search,sendingId));
			}
		}

		loadPros(req);

		if (req.hasParameter(TARGET_ID))
			findConnections(req);

		if (req.getBooleanParameter("generateCookie"))
			generateConnCookie(req);
	}


	/**
	 * populate a list of connections per request
	 * @param req
	 */
	private void loadPros(ActionRequest req) {
		SBUserRole role = (SBUserRole)req.getSession().getAttribute(Constants.ROLE_DATA);

		//cache this list - it won't change often enough to be rebuilding on every pageview
		if ((!req.hasParameter("reloadPros") && req.getSession().getAttribute("MY_PROS") != null) || role == null || role.getRoleLevel() == 0) return;

		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(250);
		sql.append("select b.*, m.first_nm, m.last_nm, m.profile_pic_pth from ").append(schema).append(REZDOX_CONNECTION_A);
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_BUSINESS b on a.rcpt_business_id=b.business_id or a.sndr_business_id=b.business_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_BUSINESS_MEMBER_XR mxr on b.business_id=mxr.business_id and mxr.status_flg=1 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("REZDOX_MEMBER m on m.member_id=mxr.member_id and m.status_flg=1 ");
		sql.append("where (a.sndr_member_id=? or a.rcpt_member_id=?) and m.member_id != ? and a.approved_flg=1 ");  //exclude connections to 'myself'
		sql.append("order by a.create_dt desc limit 6");
		log.debug(sql);

		String memberId = RezDoxUtils.getMemberId(req);
		List<Object> params = new ArrayList<>();
		params.add(memberId);
		params.add(memberId);
		params.add(memberId);

		//generate a list of VO's
		DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
		req.getSession().setAttribute("MY_PROS", dbp.executeSelect(sql.toString(), params, new MyProVO()));
	}


	/**
	 * @param req
	 */
	private void findConnections(ActionRequest req) {
		//if there is a target id use the token to identify the type of id sent and make the correct
		//  request. getting member data is different then getting business data.
		String[] idParts = StringUtil.checkVal(req.getParameter(TARGET_ID)).split("_");

		if (idParts != null && idParts.length > 0 && "m".equalsIgnoreCase(idParts[0])) {
			setModuleData(generateConnections(idParts[1], MEMBER, req));

		} else  if (idParts != null && idParts.length > 0 && "b".equalsIgnoreCase(idParts[0])) {
			setModuleData(generateConnections(idParts[1], BUSINESS, req));
		}
	}

	/**
	 * controls generating the cookie
	 * @param req
	 */
	private void generateConnCookie(ActionRequest req) {
		int count = getMemeberConnectionCount(RezDoxUtils.getMemberId(req));
		log.debug(" found " +count+ "Connection");
		//set the total in a cookie.  This may be excessive for repeat calls to the rewards page, but ensures cached data is flushed
		CookieUtil.add(req, REZDOX_CONNECTION_POINTS, String.valueOf(count), "/", -1);
	}


	/**
	 * gets the number of connections the member has
	 * @param req 
	 * @return
	 */
	public int getMemeberConnectionCount(String memberId) {
		if (memberId == null) return 0;

		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		params.add(memberId);
		params.add(memberId);
		StringBuilder sql = new StringBuilder(200);
		sql.append("select cast(count(*) as int) as total_rows_no  from ").append(schema).append("rezdox_connection ");
		sql.append("where (sndr_member_id = ? or rcpt_member_id = ?) and approved_flg = 1 ");
		log.debug(sql + "|"+params );

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<SQLTotalVO> data = dbp.executeSelect(sql.toString(), params, new SQLTotalVO());
		return data != null && !data.isEmpty() ? data.get(0).getTotal() : 0;
	}


	/**
	 * controls when a business is looking for new connections
	 * @param search
	 * @param sendingId 
	 * @return
	 */
	private List<BusinessVO> searchBusiness(String search, String sendingId) {
		log.debug("searching businesses");
		StringBuilder sql = new StringBuilder(250);
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		String[] idParts = sendingId.split("_");

		sql.append("select * from ").append(schema).append("rezdox_business b ");
		sql.append(DBUtil.INNER_JOIN).append("(select business_id from ").append(schema).append("rezdox_business_member_xr where status_flg = 1 group by business_id) bmxa on b.business_id = bmxa.business_id ");
		
		sql.append("where LOWER(business_nm) like ? ");
		params.add("%"+search.toLowerCase()+"%");
		
		sql.append("and b.business_id != ? and b.business_id not in ( select case  ");
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
		sql.append("else '-1' end as business_id from ").append(schema).append("rezdox_connection group by business_id) ");
		sql.append("order by business_nm asc ");
		log.debug(sql +"|"+params);

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
		StringBuilder sql = new StringBuilder(250);
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		String[] idParts = sendingId.split("_");

		sql.append("select * from ").append(schema).append("rezdox_member where LOWER(first_nm || ' ' || last_nm) like ? ");
		params.add("%"+search.toLowerCase()+"%");
		sql.append("and member_id != ? and member_id not in ( select case  ");
		params.add(idParts[1]);

		if ("m".equalsIgnoreCase(idParts[0])) {
			//member looking for member
			sql.append("when sndr_member_id is not null and sndr_member_id = ? and rcpt_member_id is not null then rcpt_member_id ");
			sql.append("when rcpt_member_id is not null and rcpt_member_id = ? and sndr_member_id is not null then sndr_member_id ");
			params.add(idParts[1]);
			params.add(idParts[1]);
		} else {
			//business looking for member
			sql.append("when sndr_business_id is not null and sndr_business_id = ? and rcpt_member_id is not null then rcpt_member_id ");
			sql.append("when rcpt_business_id is not null and rcpt_business_id = ? and sndr_member_id is not null then sndr_member_id ");
			params.add(idParts[1]);
			params.add(idParts[1]);
		}

		sql.append("else '-1' end as member_id from ").append(schema).append("rezdox_connection group by member_id ) ");
		sql.append(" order by last_nm, first_nm asc ");
		log.debug(sql+"|"+params);

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
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(230);
		sql.append("select b.business_nm as value, b.business_id as key from ").append(schema).append("rezdox_business_member_xr bmxr ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business b on bmxr.business_id = b.business_id and bmxr.member_id = ? ");
		log.debug(sql +"|"+ memberId);

		DBProcessor dbp = new DBProcessor(dbConn, schema);
		List<GenericVO> data = dbp.executeSelect(sql.toString(), Arrays.asList(memberId), new GenericVO());

		if (data != null)
			log.debug("number of businesses found for member " + data.size());

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

		// Sort on two fields in default view
		String order = "order by approved_flg asc, create_dt desc ";

		// Otherwise sort on one selected field if sort & order are passed
		if (!StringUtil.isEmpty(req.getParameter(DBUtil.TABLE_SORT)) && !StringUtil.isEmpty(req.getParameter(DBUtil.TABLE_ORDER))) {
			String sortField = StringUtil.checkVal(sortFields.get(req.getParameter(DBUtil.TABLE_SORT)), "approved_flg");
			SortDirection sortDirection = EnumUtil.safeValueOf(SortDirection.class, req.getParameter(DBUtil.TABLE_ORDER).toUpperCase(), SortDirection.ASC);
			order = StringUtil.join(DBUtil.ORDER_BY, sortField, " ", sortDirection.name());
		}

		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();
		String idField = (inqueryType.equals(MEMBER)) ? MEMBER : BUSINESS;

		//getting the records where target id sent the connection to an other member
		StringBuilder sql = new StringBuilder(2680);
		sql.append(DBUtil.SELECT_FROM_STAR);
		sql.append("( select connection_id, sndr_").append(idField).append("_id as sndr_id, b.member_id as rcpt_id, 'sending' as direction_cd, b.first_nm, b.last_nm, b.profile_pic_pth, ");
		sql.append("pa.city_nm, pa.state_cd, '' as business_summary, cast(0 as numeric) as rating, b.create_dt, 'MEMBER' as category_cd, approved_flg, privacy_flg, concat(b.first_nm, ' ', b.last_nm) as sortable_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(REZDOX_CONNECTION_A);
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_member b on a.rcpt_member_id = b.member_id ");
		sql.append("inner join profile_address pa on b.profile_id = pa.profile_id ");
		sql.append("where sndr_").append(idField).append(ID_SUFFIX);
		params.add(targetId);
		//getting the records where target id was sent a connection by an other memeber
		sql.append(DBUtil.UNION_ALL);
		sql.append("select connection_id, b.member_id as sender_id, rcpt_").append(idField).append("_id as rcpt_id, 'receiving' as directionCd, b.first_nm, b.last_nm, b.profile_pic_pth, ");
		sql.append("pa.city_nm, pa.state_cd, '', cast(0 as numeric) as rating, b.create_dt, 'MEMBER', approved_flg, privacy_flg, concat(b.first_nm, ' ', b.last_nm)  ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(REZDOX_CONNECTION_A );
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_member b on a.sndr_member_id = b.member_id ");
		sql.append("inner join profile_address pa on b.profile_id = pa.profile_id ");
		sql.append("where rcpt_").append(idField).append(ID_SUFFIX);
		params.add(targetId);
		//getting the records where target id sent a connection to a business
		sql.append(DBUtil.UNION_ALL);
		sql.append("select connection_id, sndr_").append(idField).append("_id , b.business_id, 'sending', '', business_nm, b.photo_url, ");
		sql.append("city_nm, state_cd, value_txt, cast(coalesce(rating, 0) as numeric) , b.create_dt, business_category_cd, approved_flg, privacy_flg, business_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(REZDOX_CONNECTION_A);
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business b on a.rcpt_business_id = b.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select business_id, avg(rating_no) as rating from ").append(schema).append("rezdox_member_business_review ");
		sql.append("where parent_id is null group by business_id");
		sql.append(") as r on b.business_id = r.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select business_id, value_txt from ").append(schema).append("rezdox_business_attribute ");
		sql.append("where slug_txt = 'BUSINESS_SUMMARY' ");
		sql.append(") as d on b.business_id = d.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select business_id, c.business_category_cd from ").append(schema).append("rezdox_business_category_xr a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business_category b on a.business_category_cd = b.business_category_cd ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business_category c on b.parent_cd = c.business_category_cd ");
		sql.append(") as cat on b.business_id = cat.business_id ");
		sql.append("where sndr_").append(idField).append(ID_SUFFIX);
		params.add(targetId);
		//getting the records where a business sent a connection to the target id
		sql.append(DBUtil.UNION_ALL);
		sql.append("select connection_id, b.business_id, rcpt_").append(idField).append("_id, 'receiving', '', business_nm, b.photo_url, ");
		sql.append("city_nm, state_cd, value_txt, cast( coalesce(rating, 0)as numeric), b.create_dt, business_category_cd, approved_flg, privacy_flg, business_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append(REZDOX_CONNECTION_A);
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business b on a.sndr_business_id = b.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select business_id, avg(rating_no) as rating from ").append(schema).append("rezdox_member_business_review ");
		sql.append("where parent_id is null group by business_id ");
		sql.append(") as r on b.business_id = r.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select business_id, value_txt from ").append(schema).append("rezdox_business_attribute ");
		sql.append("where slug_txt = 'BUSINESS_SUMMARY' ");
		sql.append(") as d on b.business_id = d.business_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN);
		sql.append("(select business_id, c.business_category_cd from ").append(schema).append("rezdox_business_category_xr a ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business_category b on a.business_category_cd = b.business_category_cd ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("rezdox_business_category c on b.parent_cd = c.business_category_cd ");
		sql.append(") as cat on b.business_id = cat.business_id ");
		sql.append("where rcpt_").append(idField).append(ID_SUFFIX);
		params.add(targetId);
		sql.append(") as all_member ");
		//where for whole multiplexed union would go here
		generateWhere(sql, req, params);
		sql.append(order);
		log.debug(sql+"|"+ params);

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
		sql.append(DBUtil.WHERE_1_CLAUSE);

		if (req.hasParameter(CATEGORY_SEARCH) && !"All".equalsIgnoreCase(StringUtil.checkVal(req.getParameter(CATEGORY_SEARCH)))) {
			String cs = req.getParameter(CATEGORY_SEARCH);
			sql.append("and category_cd = ? ");
			log.debug("category code " + cs);
			params.add(cs);
		}

		if (req.hasParameter(STATE_SEARCH)) {
			String ss =  req.getParameter(STATE_SEARCH);
			sql.append("and state_cd = ? ");
			log.debug("state search " + ss);
			params.add(ss);
		}

		if (req.hasParameter(SEARCH)) {
			String sp = StringUtil.checkVal(req.getParameter(SEARCH)).toLowerCase();
			log.debug("search param " + sp);
			sql.append("and (LOWER(first_nm) like ? or LOWER(last_nm) like ? ) ");
			params.add("%"+sp+"%");
			params.add("%"+sp+"%");
		}

		if (req.getIntegerParameter(APPROVED_FLAG) != null && req.getIntegerParameter(APPROVED_FLAG) < 3 && req.getIntegerParameter(APPROVED_FLAG) >= 0) {
			sql.append("and approved_flg = ? ");
			log.debug("approved "+req.getIntegerParameter(APPROVED_FLAG));
			params.add(req.getIntegerParameter(APPROVED_FLAG));
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
		DBProcessor dbp = new DBProcessor(dbConn, getCustomSchema());

		if (req.getBooleanParameter("approvalUpdate")) {
			try {
				dbp.getByPrimaryKey(cvo);
			} catch (InvalidDataException | DatabaseException e) {
				log.error("could not fill connection vo ",e);
			}
			cvo.setApprovedFlag(req.getIntegerParameter(APPROVED_FLAG));
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

		processEmails(cvo, req);
	}


	/**
	 * sends an email when an a user starts or accepts a connection
	 * @param cvo 
	 */
	private void processEmails(ConnectionVO cvo, ActionRequest req) {
		EmailCampaignBuilderUtil emailer = new EmailCampaignBuilderUtil(getDBConnection(), getAttributes());
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());

		if (cvo.getApprovedFlag() == 0 && !StringUtil.isEmpty(cvo.getSenderMemberId()) && !StringUtil.isEmpty(cvo.getRecipientMemberId())) {
			sendRequestEmail(cvo, emailer, notifyUtil);
		} else if (cvo.getApprovedFlag() == 1) {
			sendApprovedEmail(cvo, emailer, notifyUtil);
		}
	}
	
	/**
	 * Email received by a recipient when someone wants to connect with them.
	 * 
	 * @param cvo
	 * @param emailer
	 * @param notifyUtil
	 */
	private void sendRequestEmail(ConnectionVO cvo, EmailCampaignBuilderUtil emailer, RezDoxNotifier notifyUtil) {
		Map<String, Object> dataMap = new HashMap<>();
		Map<String, String> emailMap = new HashMap<>();

		// Pass the email map for the user we want to send the email/notification to
		addUserToMap(cvo.getSenderMemberId(), cvo.getSenderBusinessId(), "senderName", dataMap, null);
		addUserToMap(cvo.getRecipientMemberId(), cvo.getRecipientBusinessId(), "recipientName", dataMap, emailMap);
		
		// Send the email notification
		emailer.sendMessage(dataMap, emailMap, RezDoxUtils.EmailSlug.CONNECTION_REQUEST.name());

		// Add the browser notification
		String notifyProfileId = emailMap.entrySet().iterator().next().getKey();
		notifyUtil.send(RezDoxNotifier.Message.CONNECTION_REQ, dataMap, null, notifyProfileId);

	}
	
	/**
	 * Email received by a sender when connection is approved.
	 * 
	 * @param cvo
	 * @param emailer
	 * @param notifyUtil
	 */
	private void sendApprovedEmail(ConnectionVO cvo, EmailCampaignBuilderUtil emailer, RezDoxNotifier notifyUtil) {
		Map<String, Object> dataMap = new HashMap<>();
		Map<String, String> emailMap = new HashMap<>();

		// Pass the email map for the user we want to send the email/notification to
		addUserToMap(cvo.getSenderMemberId(), cvo.getSenderBusinessId(), "senderName", dataMap, emailMap);
		addUserToMap(cvo.getRecipientMemberId(), cvo.getRecipientBusinessId(), "recipientName", dataMap, null);

		// Send the email notification
		emailer.sendMessage(dataMap, emailMap, RezDoxUtils.EmailSlug.CONNECTION_APPROVED.name());

		// Add the browser notification
		String notifyProfileId = emailMap.entrySet().iterator().next().getKey();
		notifyUtil.send(RezDoxNotifier.Message.CONNECTION_APPRVD, dataMap, null, notifyProfileId);
	}
	
	/**
	 * Only pass the email map if you intend to send the email/notification to this user.
	 * 
	 * @param memberId
	 * @param businessId
	 * @param key
	 * @param dataMap
	 * @param emailMap
	 */
	private void addUserToMap(String memberId, String businessId, String key, Map<String, Object> dataMap, Map<String, String> emailMap) {
		String name = null;
		String profileId = null;
		String emailAddress = null;
		
		// Determine if this key should be populated by the member or business
		if (!StringUtil.isEmpty(memberId)) {
			MemberAction ma = new MemberAction(dbConn, attributes);
			MemberVO member = ma.retrieveMemberData(memberId);
			name = member.getFirstName() + " " + member.getLastName();
			profileId = member.getProfileId();
			emailAddress = member.getEmailAddress();
		} else {
			ActionRequest bizReq = new ActionRequest();
			bizReq.setParameter(BusinessAction.REQ_BUSINESS_ID, businessId);
			BusinessAction ba = new BusinessAction(dbConn, attributes);
			BusinessVO business = ba.retrieveBusinesses(bizReq).get(0);
			MemberVO member = business.getMembers().entrySet().iterator().next().getValue();
			name = business.getBusinessName();
			
			// Get a profile id (required for sending emails), but use the business email address
			profileId = member.getProfileId();
			emailAddress = business.getEmailAddressText();
		}
		
		// Add the name to the map
		dataMap.put(key, name);
		
		// If the email map was passed, add the email address
		if (emailMap != null) {
			emailMap.put(profileId, emailAddress);
		}
	}
}