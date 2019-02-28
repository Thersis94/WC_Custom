package com.biomed.smarttrak.admin;

// Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// J2EE libs
import javax.servlet.http.HttpServletResponse;

// Solr libs
import org.apache.solr.common.SolrDocument;

//WC Custom libs
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.action.SmarttrakSolrAction;
import com.biomed.smarttrak.util.BiomedLinkCheckerUtil;
import com.biomed.smarttrak.util.SmarttrakSolrUtil;
import com.biomed.smarttrak.util.SmarttrakTree;
import com.biomed.smarttrak.util.UpdateIndexer;
import com.biomed.smarttrak.vo.UpdateVO;
import com.biomed.smarttrak.vo.UpdateXRVO;
//SMT base libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionInterface;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.common.http.CookieUtil;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
import com.smt.sitebuilder.action.search.SolrFieldVO.FieldType;
// WC Core libs
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: UpdatesAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Action for managing Update Notifications.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 14, 2017
 ****************************************************************************/
public class UpdatesAction extends ManagementAction {
	public static final String UPDATE_ID = "updateId"; //req param
	public static final String SORT = "sort"; //req param
	public static final String ORDER = "order"; //req param
	public static final String START_DATE = "startDt"; //req param
	public static final String END_DATE = "endDt"; //req param
	public static final String STATUS_CD = "statusCd"; //req param
	public static final String TYPE_CD = "typeCd"; //req param
	public static final String SEARCH = "search"; //req param
	public static final String ANNOUNCEMENT_TYPE = "announcementType"; //req param
	private static final String COOK_UPD_START_DT = "updateStartDt";
	private static final String COOK_UPD_END_DT = "updateEndDt";

	private static final String HTML_REGEX = "(<\\/?(([uo]l)|(b)|(li)|(s((trong)|(ub))?)|(d((etails)|(iv))?)|(u)|(img)|(hr)|(font))(?!up)[^<>]*\\/?>)|(<p>&nbsp;<\\/p>)|((style|align|bgcolor|border|color)[ ]?=[ ]?['\"][^'\"]*['\"])";
	private static final String P_REGEX = "<p[^<>]*>";

	/**
	 * @deprecated not sure where this is used, possibly JSPs.  Unlikely it belongs here so reference it from it's source location.
	 */
	@Deprecated
	public static final String ROOT_NODE_ID = SectionHierarchyAction.MASTER_ROOT;

	//ChangeLog TypeCd.  Using the key we swap on for actionType in AdminControllerAction so we can get back.
	public static final String UPDATE_TYPE_CD = "updates";


	public enum UpdateType {
		//NOTE: The order of these UpdateType values impacts the compareTo() method.  Impacts UpdatesEditionSorter if you rearrange these.
		// you cannot override the compareTo() method, but an order and "int compare(int i)" method could be added here.
		MARKET(12, "Market"),
		REVENUES(15, "Revenues"),
		NEW_PRODUCTS(17, "New Products"),
		DEALS_FINANCING(20, "Deals/Financing"),
		CLINICAL_REGULATORY(30, "Clinical/Regulatory"),
		PATENTS(35, "Patents"),
		REIMBURSEMENT(37, "Reimbursement"),
		ANNOUNCEMENTS(38, "Announcements"),
		STUDIES(40, "Studies");

		private int val;
		private String text;

		UpdateType(int val, String text) {
			this.val = val;
			this.text = text;
		}

		public int getVal() {
			return this.val;
		}
		public String getText() {
			return this.text;
		}
		public static UpdateType getFromCode(int typeCd) {
			for(UpdateType type : UpdateType.values()){
				if (type.getVal() == typeCd){
					return type;
				}
			}
			return null;
		}
	}

	public UpdatesAction() {
		super();
	}

	public UpdatesAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		//loadData gets passed on the ajax call.  If we're not loading data simply go to view to render the bootstrap 
		//table into the view (which will come back for the data).
		configureCookies(req);
		
		if (!req.hasParameter("loadData") && !req.hasParameter("loadhistory") && !req.hasParameter(UPDATE_ID) ) return;
		int count = 0;

		List<Object> data;
		if(req.hasParameter("loadHistory")) {
			data = getHistory(req.getParameter("historyId"));
		} else if (req.hasParameter("updateId")) {
			// If we have an id just load directly from the database.
			List<Object> params = new ArrayList<>();
			params.add(req.getParameter("updateId"));
			data = loadDetails(params);
		} else {
			//Get the Filtered Updates according to Request.
			getFilteredUpdates(req);

			ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
			SolrResponseVO resp = (SolrResponseVO)mod.getActionData();
			count = (int) resp.getTotalResponses();
			if (count > 0) {

				List<Object> params = getIdsFromDocs(resp);
				
				data = loadDetails(params);
				log.debug("DB Count " + data.size());
			} else {
				data = Collections.emptyList();
			}
			//adjust the content links only for the manage list page, since this page allows user to bounce externally
			adjustContentLinks(data, req);
		}

		decryptNames(data);

		addProductCompanyData(data);

		putModuleData(data, count, false);

		//when an add/edit form, load list of BiomedGPS Staff for the "Author" drop-down
		if (req.hasParameter(UPDATE_ID))
			loadAuthors(req);
	}


	/**
	 * Get all the document ids from the solr documents and remove the
	 * custom identifier if it is present.
	 * @param resp
	 * @return
	 */
	private List<Object> getIdsFromDocs(SolrResponseVO resp) {
		List<Object> params = new ArrayList<>();

		for (SolrDocument doc : resp.getResultDocuments()) {
			String id = (String) doc.getFieldValue(SearchDocumentHandler.DOCUMENT_ID);

			// Replace the biomed update prefix if it exists.
			if (id.contains(UpdateVO.DOCUMENT_ID_PREFIX))
				id = id.replace(UpdateVO.DOCUMENT_ID_PREFIX, "");
			
			params.add(id);
		}
		return params;
	}

	/**
	 * Load the most up to date details for each update
	 * @param docs
	 * @return
	 */
	private List<Object> loadDetails(List<Object> ids) {
		String sql = formatDetailQuery(ids.size());
		log.debug(sql);

		DBProcessor db = new DBProcessor(dbConn);
		return db.executeSelect(sql, ids, new UpdateVO());
	}


	/**
	 * Ensure that the information pertaining to each update is up to date.
	 * Even if solr has not finished processing the changes.
	 * @param size
	 * @param dir 
	 * @param order
	 * @return
	 */
	private String formatDetailQuery(int size) {
		StringBuilder sql = new StringBuilder(200);
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		sql.append("SELECT * FROM ").append(schema).append("BIOMEDGPS_UPDATE up ");
		sql.append("LEFT JOIN ").append(schema).append("BIOMEDGPS_UPDATE_SECTION xr ");
		sql.append("on up.UPDATE_ID = xr.UPDATE_ID ");
		sql.append("WHERE up.UPDATE_ID in (").append(DBUtil.preparedStatmentQuestion(size)).append(") ");
		sql.append("order by publish_dt desc, order_no asc, up.create_dt desc ");

		return sql.toString();
	}


	/**
	 * Set all paramters neccesary for solr to be able to properly search for the desired documents.
	 * @param req
	 * @param dir 
	 * @param order
	 */
	private void setSolrParams(ActionRequest req) {
		int rpp = Convert.formatInteger(req.getParameter("limit"), 10);
		int page = Convert.formatInteger(req.getParameter("offset"), 0)/rpp;
		req.setParameter("rpp", StringUtil.checkVal(rpp));
		req.setParameter("page", StringUtil.checkVal(page));
		if(req.hasParameter(SEARCH)) 
			req.setParameter("searchData", req.getParameter(SEARCH));

		//build a list of filter queries
		List<String> fq = new ArrayList<>();
		String startDt = req.getParameter(COOK_UPD_START_DT);
		String endDt = req.getParameter(COOK_UPD_END_DT);
		String dates = SolrActionUtil.makeRangeQuery(FieldType.DATE, startDt, endDt);
		if (!StringUtil.isEmpty(dates))
			fq.add(SearchDocumentHandler.PUBLISH_DATE + ":" + dates);

		if(req.hasParameter(UPDATE_ID)) {
			StringBuilder id = new StringBuilder(req.getParameter(UPDATE_ID));
			if (id.length() < AdminControllerAction.DOC_ID_MIN_LEN)
				id.insert(0, "_").insert(0, UpdateIndexer.INDEX_TYPE);
			fq.add(SearchDocumentHandler.DOCUMENT_ID + ":" + id);
		}

		String typeCd = CookieUtil.getValue("updateType", req.getCookies());
		if (!StringUtil.isEmpty(typeCd))
			fq.add(SearchDocumentHandler.MODULE_TYPE + ":" + typeCd);

		String hierarchies = CookieUtil.getValue("updateMarkets", req.getCookies());
		if (!StringUtil.isEmpty(hierarchies)) {
			for (String s : hierarchies.split(","))
				fq.add(SearchDocumentHandler.HIERARCHY + ":" + s);
		}

		String announcementType = CookieUtil.getValue("updateAnnouncementType", req.getCookies());
		if(!StringUtil.isEmpty(announcementType)) {
			fq.add(StringUtil.join("announcement_type_i:", announcementType));
		}

		req.setParameter("fq", fq.toArray(new String[fq.size()]), true);
		req.setParameter("allowCustom", "true");
		req.setParameter("fieldOverride", SearchDocumentHandler.PUBLISH_DATE);
	}


	/**
	 * Sets the default search values - particularly a date range of 'today'.
	 * These can be flushed to <blank> by the user, but if they're nullified we'll restore them (e.g. next session)
	 * If the search end date cookie does not exists, set it with a default range of today.
	 * Otherwise, if it's blank, the user flushed the date values intentionally (which is ok)
	 * NOTE: Setting cookies goes into the response object.  We can't set a cookie and then 
	 * immediately read it's value.  For this reason we transpose the cookies into request parameters.
	 * @param req
	 */
	private void configureCookies(ActionRequest req) {
		String start = StringUtil.checkVal(CookieUtil.getValue(COOK_UPD_START_DT, req.getCookies()), "");
		String end = CookieUtil.getValue(COOK_UPD_END_DT, req.getCookies());
		if (end == null) {
			HttpServletResponse res = (HttpServletResponse) req.getAttribute(GlobalConfig.HTTP_RESPONSE);
			end = Convert.formatDate(Convert.convertTimeZoneOffset(Calendar.getInstance().getTime(), "EST5EDT"), Convert.DATE_SLASH_PATTERN); //today
			CookieUtil.add(res, COOK_UPD_END_DT, end, "/", -1);
		}
		req.setParameter(COOK_UPD_START_DT, start);
		req.setParameter(COOK_UPD_END_DT, end);
	}

	/**
	 * Helper method that returns list of Updates filtered by ActionRequest
	 * parameters.
	 * @param req
	 * @param dir 
	 * @param order
	 * @return
	 * @throws ActionException 
	 */
	private void getFilteredUpdates(ActionRequest req) throws ActionException {
		//parse the requet object
		setSolrParams(req);
		
		// Pass along the proper information for a search to be done.
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));
		req.setParameter("pmid", mod.getPageModuleId());

		// Build the solr action
		ActionInterface sa = new SmarttrakSolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);
		sa.retrieve(req);
	}

	/**
	 * Modifies public links to their corresponding manage tool link
	 * @param updates
	 * @param req
	 */
	protected void adjustContentLinks(List<Object> updates, ActionRequest req) {
		//create link checker util
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		BiomedLinkCheckerUtil linkUtil = new BiomedLinkCheckerUtil(dbConn, site);
		
		//modify appropriate content links for updates
		for (Object o : updates) {
			UpdateVO up = (UpdateVO)o;
			up.setMessageTxt(linkUtil.modifySiteLinks(up.getMessageTxt()));
		}
	}

	/**
	 * Retrieve all the updates - called by the Solr OOB indexer
	 * @param updateId
	 * @param statusCd
	 * @param typeCd
	 * @param dateRange
	 * @return
	 */
	public List<Object> getAllUpdates(String updateId) {
		String schema = (String)getAttribute(Constants.CUSTOM_DB_SCHEMA);
		String sql = formatRetrieveAllQuery(schema, updateId);

		log.debug(sql);
		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(updateId)) params.add(updateId);

		DBProcessor db = new DBProcessor(dbConn, schema);
		List<Object>  updates = db.executeSelect(sql, params, new UpdateVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}

	/**
	 * takes the current list of updates and adds the company information to any product updates
	 * @param updates
	 *TODO change query to "in (1,2,3)" instead of looping outside the query - Zoho SC-232
	 */
	private void addProductCompanyData(List<Object> updates) {
		log.debug("adding company short name and id ");

		int productCount = getProductCount(updates);
		if (productCount == 0) return;

		StringBuilder sb = new StringBuilder(161);
		sb.append("select c.company_id, c.short_nm_txt, p.product_id from ").append(customDbSchema).append("biomedgps_product p ");
		sb.append(INNER_JOIN).append(customDbSchema).append("biomedgps_company c on p.company_id = c.company_id ");
		sb.append("where p.product_id in (");
		DBUtil.preparedStatmentQuestion(productCount, sb);
		sb.append(")");
		log.debug(sb);

		Map<String, GenericVO> companies = new HashMap<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sb.toString())) {
			int i = 1;
			for (Object o : updates) {
				UpdateVO up = (UpdateVO)o;
				if (StringUtil.isEmpty(up.getProductId())) continue;
				ps.setString(i++, up.getProductId());
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				GenericVO vo = new GenericVO(rs.getString("company_id"), rs.getString("short_nm_txt"));
				companies.put(rs.getString("product_id"), vo);
			}
		} catch(SQLException sqle) {
			log.error("could not confirm security by id ", sqle);
		}

		mergeResults(updates, companies);
	}


	/**
	 * Add company information to the products updates.
	 * @param updates
	 * @param companies
	 */
	private void mergeResults(List<Object> updates, Map<String, GenericVO> companies) {
		for (Object o : updates) {
			UpdateVO up = (UpdateVO)o;
			if (StringUtil.isEmpty(up.getProductId())) continue;
			GenericVO company = companies.get(up.getProductId());
			up.setCompanyId((String) company.getKey());
			up.setCompanyNm((String) company.getValue());
		}
	}

	/**
	 * Get the count of how many updates refer to products
	 * @param updates
	 * @return
	 */
	private int getProductCount(List<Object> updates) {
		int productCount = 0;
		for (Object o : updates) {
			UpdateVO up = (UpdateVO)o;
			if (StringUtil.isEmpty(up.getProductId())) continue;
			productCount++;
		}
		return productCount;
	}

	/**
	 * Generates Query for getting all records/single record.  Used by Solr Indexer.
	 * Note: This query closely compliments what we use in UpdatesScheduledAction (for a real-time load of Updates)
	 * @param updateId
	 * @param schema
	 * @return
	 */
	protected String formatRetrieveAllQuery(String schema, String updateId) {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select up.update_id, up.title_txt, up.message_txt, up.publish_dt, up.type_cd, us.update_section_xr_id, us.section_id, ");
		sql.append("up.announcement_type, c.short_nm_txt as company_nm, prod.short_nm as product_nm, up.order_no, up.status_cd, ");
		sql.append("coalesce(up.product_id,prod.product_id) as product_id, coalesce(up.company_id, c.company_id) as company_id, ");
		sql.append("m.short_nm as market_nm, coalesce(up.market_id, m.market_id) as market_id, ");
		sql.append("'").append(getAttribute(Constants.QS_PATH)).append("' as qs_path, up.create_dt "); //need to pass this through for building URLs
		sql.append("from ").append(schema).append("biomedgps_update up ");
		sql.append("inner join profile p on up.creator_profile_id=p.profile_id ");
		sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_update_section us on up.update_id=us.update_id ");
		sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_product prod on up.product_id=prod.product_id ");
		sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_company c on (up.company_id is not null and up.company_id=c.company_id) or (up.product_id is not null and prod.company_id=c.company_id) "); //join from the update, or from the product.
		sql.append(LEFT_OUTER_JOIN).append(schema).append("biomedgps_market m on up.market_id=m.market_id ");
		if (!StringUtil.isEmpty(updateId))
			sql.append("where up.update_id = ? ");

		log.debug(sql);
		return sql.toString();
	}

	/**
	 * Retrieve list of Updates containing historical Revisions.
	 * @param parameter
	 * @return
	 */
	protected List<Object> getHistory(String updateId) {
		String sql = formatHistoryRetrieveQuery();

		List<Object> params = new ArrayList<>();
		if (!StringUtil.isEmpty(updateId)) params.add(updateId);

		DBProcessor db = new DBProcessor(dbConn);
		List<Object>  updates = db.executeSelect(sql, params, new UpdateVO());
		log.debug("loaded " + updates.size() + " updates");
		return updates;
	}

	/**
	 * Build History Sql Retrieval against ChangeLog Table.
	 * @param schema
	 * @return
	 */
	private String formatHistoryRetrieveQuery() {
		StringBuilder sql = new StringBuilder(400);
		sql.append("select b.wc_sync_id as update_id, a.diff_txt as message_txt, ");
		sql.append("a.create_dt as publish_dt, c.first_nm, c.last_nm from change_log a ");
		sql.append("inner join wc_sync b on a.wc_sync_id = b.wc_sync_id ");
		sql.append("inner join profile c on b.admin_profile_id = c.profile_id ");
		sql.append("where b.wc_key_id = ? order by a.create_dt");

		log.debug(sql);
		return sql.toString();
	}

	/**
	 * loop and decrypt owner names, which came from the profile table
	 * @param accounts
	 */
	@SuppressWarnings("unchecked")
	protected void decryptNames(List<Object> data) {
		new NameComparator().decryptNames((List<? extends HumanNameIntfc>)(List<?>)data, (String)getAttribute(Constants.ENCRYPT_KEY));
	}


	/**
	 * Load the Section Tree so that Hierarchies can be generated.
	 * @param req
	 * @throws ActionException
	 */
	public SmarttrakTree loadSections() {
		//load the section hierarchy Tree from superclass
		return loadDefaultTree();
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		if (Convert.formatBoolean(req.getParameter("markReviewed"))) {
			markReviewed(req.getParameter(UPDATE_ID));
		} else {
			saveRecord(req, false);
		}
	}


	/**
	 * Change the supplied update's status code to Reviewed
	 * @param updateId
	 * @throws ActionException
	 */
	private void markReviewed(String updateId) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("UPDATE ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("BIOMEDGPS_UPDATE set status_cd = 'R' where UPDATE_ID = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, updateId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		saveRecord(req, true);
	}


	/**
	 * reusable internal method for invoking DBProcessor
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected void saveRecord(ActionRequest req, boolean isDelete) throws ActionException {
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		UpdateVO u = new UpdateVO(req);
		//Set the redirect URL if applicable
		setRedirectUrl(req);

		try {
			if (isDelete) {
				u.setUpdateId(req.getParameter("pkId"));

				//Load the Record before deletion.
				db.getByPrimaryKey(u);

				//Delete the Record.
				db.delete(u);

				//Delete from Solr.
				deleteFromSolr(u);
			} else {
				filterText(u);

				//Set Converted Date for Create or Update Date depending on action being performed.
				if(StringUtil.isEmpty(u.getUpdateId())) {
					u.setCreateDt(Convert.convertTimeZoneOffset(new Date(), "EST5EDT"));
				} else {
					u.setUpdateDt(Convert.convertTimeZoneOffset(new Date(), "EST5EDT"));
				}

				u.setPublishDate(calcPublishDt(Convert.formatDate(req.getParameter("publishDt"))));

				db.save(u);

				fixPkids(u);

				//Save Update Sections.
				saveSections(u);

				//Save the Update Document to Solr
				writeToSolr(u);
			}

			req.setParameter(UPDATE_ID, u.getUpdateId());
		} catch (InvalidDataException | DatabaseException e) {
			throw new ActionException(e);
		}
	}

	/**
	 * Determine if Time needs Adjusted on the PublishDate.
	 * @param reqDate
	 * @return
	 */
	public Date calcPublishDt(Date reqDate) {
		if(reqDate == null) {
			return null;
		}

		Calendar pDate = Calendar.getInstance();
		Calendar now = Calendar.getInstance();
		pDate.setTime(reqDate);

		boolean isSameDay = pDate.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR);
		boolean zeroTime = pDate.get(Calendar.HOUR) == 0 && pDate.get(Calendar.MINUTE) == 0 && pDate.get(Calendar.SECOND) == 0;

		/**
		 * If the publishDate is the same as today and we don't have a time set,
		 * Set current time on the pDate Calendar.  If this is a future update,
		 * Look for the max date in the database, adjust by 1 minute later and
		 * set pDate with that value.
		 *
		 * Ensures that ordering is maintained using publish_dt as the rule
		 * moving forward.
		 */
		if(isSameDay && zeroTime) {
			log.debug("New Date");
			pDate.add(Calendar.HOUR, now.get(Calendar.HOUR));
			pDate.add(Calendar.MINUTE, now.get(Calendar.MINUTE));
			pDate.add(Calendar.SECOND, now.get(Calendar.SECOND));
			pDate.set(Calendar.AM_PM, now.get(Calendar.AM_PM));
		} else {
			Timestamp latestDbPubDt = loadLatestPubDate(reqDate);
			if(latestDbPubDt != null) {
				pDate.setTime(latestDbPubDt);
				pDate.add(Calendar.MINUTE, 1);
			}
		}
		return pDate.getTime();
	}

	/**
	 * Load latest publishDate Timestamp for a given reqDate.
	 * @param pDate
	 * @return
	 */
	private Timestamp loadLatestPubDate(Date reqDate) {
		StringBuilder sql = new StringBuilder(100);
		sql.append("select max(publish_dt) from ").append(getCustomSchema()).append("biomedgps_update where date_trunc('day', publish_dt) = ?");
		try(PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setObject(1, reqDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate());
			ResultSet rs = ps.executeQuery();
			if(rs.next()) {
				return rs.getTimestamp(1);
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}

		return null;
	}

	/**
	* The form data used for this update can come from several places. Either the updates tool, 
 	* from the updates review tool, or from the updates home page. If it has come from the review tool 
	* redirect the user there, if from home page redirect user to home page. Otherwise redirect to updates list.  
	 * @param req
	 */
	protected void setRedirectUrl(ActionRequest req) {
		String returnType =  StringUtil.checkVal(req.getParameter("returnType"));
		if(!StringUtil.isEmpty(returnType)) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			
			//build our redirect url
			StringBuilder redirectUrl = new StringBuilder(50);
			redirectUrl.append(page.getFullPath());
			if(!"homepage".equals(returnType)) {
				redirectUrl.append("?actionType=").append(returnType);
			}
			req.setAttribute(Constants.REDIRECT_URL, redirectUrl.toString());
		}
	}

	/**
	 * Filter out prohibited html tags from the message text
	 * @param u
	 */
	private void filterText(UpdateVO u) {
		if (StringUtil.isEmpty(u.getMessageTxt())) return;

		// Strip out undesired html tags and attributes and ensure that all <p> tags
		// contain no extra characters as it can cause issues with the updates emails.
		u.setMessageTxt(u.getMessageTxt().replaceAll(HTML_REGEX, "").replaceAll(P_REGEX, "<p>"));
	}

	/**
	 * Manages updating given UpdatesVO with generated PKID and updates sections
	 * to match.
	 * @param u
	 */
	protected void fixPkids(UpdateVO u) {
		//Set the UpdateId on UpdatesXRVOs
		if (!StringUtil.isEmpty(u.getUpdateId())) {
			for (UpdateXRVO uxr : u.getUpdateSections())
				uxr.setUpdateId(u.getUpdateId());
		}
	}


	/**
	 * Removes an Updates Record from Solr.
	 * @param u
	 */
	protected void deleteFromSolr(UpdateVO u) {
		try (SolrActionUtil sau = new SmarttrakSolrUtil(getAttributes())) {
			sau.removeDocument(u.getUpdateId());
		} catch (Exception e) {
			log.error("Error Deleting from Solr.", e);
		}
		log.debug("removed document from solr");
	}


	/**
	 * Save an UpdatesVO to solr.
	 * @param u
	 */
	protected void writeToSolr(UpdateVO u) {
		UpdateIndexer idx = UpdateIndexer.makeInstance(getAttributes());
		idx.setDBConnection(dbConn);
		idx.indexItems(u.getUpdateId());
	}


	/**
	 * Delete old Update Sections and save new ones.
	 * @param u
	 * @throws ActionException
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	protected void saveSections(UpdateVO u) throws ActionException, InvalidDataException, DatabaseException {
		//Delete old Update Section XRs
		deleteSections(u.getUpdateId());

		//Save new Sections.
		DBProcessor db = new DBProcessor(dbConn, (String)getAttribute(Constants.CUSTOM_DB_SCHEMA));
		for(UpdateXRVO uxr : u.getUpdateSections())
			db.save(uxr);
	}


	/**
	 * Delete old Update Section XRs 
	 * @param updateId
	 * @throws ActionException 
	 */
	protected void deleteSections(String updateId) throws ActionException {
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("biomedgps_update_section where update_id = ?");

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, updateId);
			ps.executeUpdate();
		} catch (SQLException e) {
			throw new ActionException(e);
		}
	}
}
