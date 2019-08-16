package com.biomed.smarttrak.action.rss;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.rss.vo.SmarttrakRssEntityVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.session.SMTCookie;
import com.siliconmtn.io.mail.EmailRecipientVO;
import com.siliconmtn.sb.email.util.EmailCampaignBuilderUtil;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.rss.RSSEntityVO;
import com.smt.sitebuilder.action.rss.RSSFeedAction;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SmarttrakRSSFeedAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Custom Smarttrak RSSFeedAction that loads Extra data for
 * the Feeds Dashboard.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.0
 * @since May 15, 2017
 ****************************************************************************/
public class SmarttrakRSSFeedAction extends SBActionAdapter {
	
	public static final String FEEDBACK_ADMIN_EMAIL = "feedbackAdminEmail";
	public static final String FEEDBACK_EMAIL_SLUG = "BMG_RSS_FEEDBACK";

	public SmarttrakRSSFeedAction() {
		super();
	}

	public SmarttrakRSSFeedAction(ActionInitVO init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		try {
			SmarttrakRssEntityVO s = new SmarttrakRssEntityVO();
			s.setRssEntityId(req.getParameter("pkId"));
			new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).delete(s);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Deleting Parsing Filter", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("feedback"))) {
			sendFeedback(req);
		} else if(req.hasParameter("articleFilterId") || req.hasParameter("ids")) {
			NewsroomAction nra = new NewsroomAction(this.actionInit);
			nra.setAttributes(attributes);
			nra.setDBConnection(dbConn);
			nra.build(req);
		} else {
			update(req);
		}
		
	}

	
	/**
	 * Gather feedback information and send an email to the feedback admin.
	 * @param req
	 * @throws ActionException 
	 */
	private void sendFeedback(ActionRequest req) throws ActionException {
		EmailCampaignBuilderUtil ecbu = new EmailCampaignBuilderUtil(dbConn, attributes);
		UserDataVO user = (UserDataVO) req.getSession().getAttribute(Constants.USER_DATA);	

		Map<String, Object> config = new HashMap<>();
		config.put("request", req.getParameter("request"));
		config.put("feedGroup", getFeedGroup(req));
		config.put("submittalName", user.getFullName());
		config.put("articleName", req.getParameter("articleName"));
		
		ecbu.sendMessage(config, getAdminEmail(), FEEDBACK_EMAIL_SLUG);
	}
	
	
	/**
	 * Get the current feed group's name
	 * @param req
	 * @return
	 * @throws ActionException
	 */
	private String getFeedGroup(ActionRequest req) throws ActionException {
		SMTCookie c = req.getCookie("ACTIVE_FEED_GROUP");
		StringBuilder sql = new StringBuilder(100);
		sql.append("select feed_group_nm from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_feed_group where feed_group_id = ? ");
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, c.getValue());
			
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) return rs.getString("feed_group_nm");
		} catch (SQLException e) {
			throw new ActionException(e);
		}
		return "";
	}
	
	/**
	 * Get the profile id of the supplied admin email
	 * @return
	 */
	private List<EmailRecipientVO> getAdminEmail() {
		String sql = "SELECT profile_id from profile where search_email_txt = ? ";
		String emailAddress = StringUtil.checkVal(attributes.get(FEEDBACK_ADMIN_EMAIL));
		List<EmailRecipientVO> recipient = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			String encKey = (String)getAttribute(Constants.ENCRYPT_KEY);
			StringEncrypter se = new StringEncrypter(encKey);
			ps.setString(1, se.encrypt(emailAddress.toUpperCase()));
			log.debug(sql+"|"+se.encrypt(emailAddress.toUpperCase()));
			ResultSet rs = ps.executeQuery();
			
			if (rs.next()) {
				recipient.add(new EmailRecipientVO(rs.getString("profile_id"), emailAddress, EmailRecipientVO.TO));
			}
			
		} catch (Exception e) {
			log.error("Failed to get profile id for email address " + emailAddress);
		}
		return recipient;
		
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void update(ActionRequest req) throws ActionException {
		SmarttrakRssEntityVO fv = new SmarttrakRssEntityVO(req);
		RSSEntityVO re = new RSSEntityVO(req);
		try {
			updateRSSEntityId(re, fv);
			saveFeedGroupXRs(fv, req.getParameterValues("feedEntityGroupXrId"));
		} catch (InvalidDataException | DatabaseException | SQLException e) {
			log.error("Error Saving RSS Feed Source", e);
		}
	}

	/**
	 * Helper method manages sacing Feed Group Xrs.  Flushes old ones from the
	 * system and then creates new ones.
	 * @param fv
	 * @param parameterValues
	 */
	protected void saveFeedGroupXRs(SmarttrakRssEntityVO entity, String[] groupIds) {
		delFilterGroupXRVals(entity.getRssEntityId());
		saveFeedGroupXRVals(entity.getRssEntityId(), groupIds);
	}

	/**
	 * Helper method manages Deleting Filter group Xrs
	 * @param filterId
	 */
	protected void delFilterGroupXRVals(String filterId) {
		StringBuilder s = new StringBuilder(100);
		s.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		s.append("BIOMEDGPS_FEED_SOURCE_GROUP_XR where rss_entity_id = ?");

		try(PreparedStatement ps = dbConn.prepareStatement(s.toString())) {
			ps.setString(1, filterId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Deleting Filter Group XRs", e);
		}
	}

	/**
	 * Helper method manages saving all the Feed Group XRs
	 * @param filterId
	 * @param groupIds
	 */
	protected void saveFeedGroupXRVals(String entityId, String[] groupIds) {
		StringBuilder s = new StringBuilder(175);
		s.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		s.append("BIOMEDGPS_FEED_SOURCE_GROUP_XR (source_group_id, ");
		s.append("feed_group_id, rss_entity_id, create_dt) values(?,?,?,?)");
		Map<String, List<Object>> insertValues = new HashMap<>();

		UUIDGenerator uuid = new UUIDGenerator();
		for (String g : groupIds) {
				String xrId = uuid.getUUID();
				List<Object> insertData = new ArrayList<>();
				insertData.addAll(Arrays.asList(xrId, g, entityId, Convert.getCurrentTimestamp()));	
				insertValues.put(xrId, insertData);
		}

		try {
			new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA)).executeBatch(s.toString(), insertValues);
		} catch (DatabaseException e) {
			log.error("Couldn't save Filter Group XRs", e);
		}
	}

	/**
	 * Helper method manages updating the core RSSEntity Record and then calls
	 * appropriate insert/update on the custom data.
	 * @param re 
	 * @param fv
	 * @throws DatabaseException 
	 * @throws InvalidDataException 
	 * @throws SQLException 
	 */
	protected void updateRSSEntityId(RSSEntityVO re, SmarttrakRssEntityVO entity) throws InvalidDataException, DatabaseException, SQLException {
		DBProcessor dbpc = new DBProcessor(dbConn);
		boolean isInsert = StringUtil.isEmpty(re.getRssEntityId());
		dbpc.save(re);
		entity.setRssEntityId(re.getRssEntityId());
		writeCustomRssEntity(entity, isInsert);
	}

	/**
	 * Helper method manages writing the custom RSS Entity Data.
	 * @param entity
	 * @param isInsert
	 * @throws SQLException
	 */
	protected void writeCustomRssEntity(SmarttrakRssEntityVO entity, boolean isInsert) throws SQLException {
		String sql = isInsert ? getCustomEntityInsertSql() : getCustomEntityUpdateSql();

		try(PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, entity.getConfigUrlTxt());
			ps.setString(2, entity.getFeedTypeId());
			ps.setTimestamp(3, Convert.getCurrentTimestamp());
			ps.setString(4, entity.getRssEntityId());
			ps.executeUpdate();
		}
	}

	/**
	 * Helper method manages building the Custom RSS Entity Update Query
	 * @return
	 */
	protected String getCustomEntityUpdateSql() {
		StringBuilder sql = new StringBuilder(175);
		sql.append("update ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_rss_entity set config_url_txt = ?, feed_type_id = ?, ");
		sql.append("update_dt = ? where rss_entity_id = ? ");
		return sql.toString();
	}

	/**
	 * Helper method manages building the Custom RSS Entity Insert Query
	 * @return
	 */
	protected String getCustomEntityInsertSql() {
		StringBuilder sql = new StringBuilder(175);
		sql.append("insert into ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_rss_entity (config_url_txt, feed_type_id, create_dt, ");
		sql.append("rss_entity_id) values (?,?,?,?) ");
		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		loadFeedGroups(req);
		loadFeedTypes(req);
		this.putModuleData(loadFeeds(req.getParameter(RSSFeedAction.RSS_ENTITY_ID)));
	}

	/**
	 * Helper method manages loading Feed Groups.
	 * @param req
	 * @throws ActionException 
	 */
	protected void loadFeedGroups(ActionRequest req) {
		RSSGroupAction rga = new RSSGroupAction(this.actionInit);
		rga.setAttributes(attributes);
		rga.setDBConnection(dbConn);
		req.setAttribute("groups", rga.loadGroupXrs(null, null, req.getParameter(RSSFeedAction.RSS_ENTITY_ID)));
	}

	/**
	 * Helper method manages loading feed Types.
	 * @param req
	 */
	protected void loadFeedTypes(ActionRequest req) {
		RSSTypeAction rta = new RSSTypeAction(this.actionInit);
		rta.setAttributes(attributes);
		rta.setDBConnection(dbConn);
		req.setAttribute("types", rta.loadTypes(null));
	}

	/**
	 * Helper method responsible for loading single or many RSS Entities.
	 * @param rssEntityId
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected List<SmarttrakRssEntityVO> loadFeeds(String rssEntityId) {
		List<Object> vals = new ArrayList<>();
		vals.add(AdminControllerAction.BIOMED_ORG_ID);
		if(!StringUtil.isEmpty(rssEntityId)) {
			vals.add(rssEntityId);
		}
		return (List<SmarttrakRssEntityVO>)(List<?>) new DBProcessor(dbConn).executeSelect(loadFeedSql(!StringUtil.isEmpty(rssEntityId)), vals, new SmarttrakRssEntityVO());
	}

	/**
	 * Helper method builds the sql for loading feeds.  Can filter down to a
	 * single given entityId.
	 * @return
	 */
	protected String loadFeedSql(boolean hasEntityId) {
		StringBuilder sql = new StringBuilder(275);
		sql.append("select * from rss_entity e ");
		sql.append("inner join ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_RSS_ENTITY re on e.rss_entity_id = re.rss_entity_id ");
		sql.append("where e.organization_id = ? ");
		if(hasEntityId) {
			sql.append("and e.rss_entity_id = ? ");
		}
		sql.append("order by rss_feed_nm");
		return sql.toString();
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		NewsroomAction nra = new NewsroomAction(this.actionInit);
		nra.setAttributes(attributes);
		nra.setDBConnection(dbConn);
		nra.retrieve(req);
	}
}
