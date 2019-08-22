package com.mts.action;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// MTS Imports
import com.mts.admin.action.UserAction;
import com.mts.publication.action.IssueAction;
import com.mts.publication.action.PublicationAction;
import com.mts.publication.data.AssetVO.AssetType;
import com.mts.publication.data.IssueVO;
import com.mts.publication.data.PublicationVO;
import com.mts.subscriber.action.SubscriptionAction.SubscriptionType;
import com.mts.subscriber.data.MTSUserVO;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.*;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.metadata.MetadataVO;
import com.smt.sitebuilder.action.metadata.OrgMetadataAction;

/****************************************************************************
 * <b>Title</b>: SelectLookupAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Provides a mechanism for looking up key/values for select lists.
 * Each type will return a collection of GenericVOs, which will automatically be
 * available in a select picker
 * as listType
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Feb 4, 2019
 * @updates:
 ****************************************************************************/
public class SelectLookupAction extends SBActionAdapter {

	/**
	 * Key to be passed to utilize this action
	 */
	public static final String SELECT_KEY = "selectType";

	/**
	 * Req value passed form a BS Table search
	 */
	public static final String REQ_SEARCH = "search";

		/**
	 * Assigns the keys for the select type to method mapping.  In the generic vo
	 * the key is the method name.  The value is a boolean which indicates whether
	 * or not the request object is needed in that method 
	 */
	private static Map<String, GenericVO> keyMap = new HashMap<>(16);
	static {
		keyMap.put("activeFlag", new GenericVO("getYesNoLookup", Boolean.FALSE));
		keyMap.put("role", new GenericVO("getRoles", Boolean.FALSE));
		keyMap.put("prefix", new GenericVO("getPrefix", Boolean.FALSE));
		keyMap.put("gender", new GenericVO("getGenders", Boolean.FALSE));
		keyMap.put("category", new GenericVO("getCategories", Boolean.TRUE));
		keyMap.put("users", new GenericVO("getUsers", Boolean.TRUE));
		keyMap.put("editors", new GenericVO("getEditors", Boolean.FALSE));
		keyMap.put("assetTypes", new GenericVO("getAssetTypes", Boolean.FALSE));
		keyMap.put("publications", new GenericVO("getPublications", Boolean.TRUE));
		keyMap.put("issues", new GenericVO("getIssues", Boolean.TRUE));
		keyMap.put("subscriptions", new GenericVO("getSubscriptions", Boolean.FALSE));
		keyMap.put("articles", new GenericVO("getArticles", Boolean.FALSE));
		keyMap.put("searchAC", new GenericVO("getArticlesAC", Boolean.TRUE));
	}

	/**
	 * 
	 */
	public SelectLookupAction() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public SelectLookupAction(ActionInitVO actionInit) {
		super(actionInit);
	}
	
	/**
	 * @param actionInit
	 */
	public SelectLookupAction(SMTDBConnection dbConn, Map<String, Object> attributes) {
		super();
		setDBConnection(dbConn);
		setAttributes(attributes);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String listType = req.getStringParameter(SELECT_KEY);
		GenericVO vo = keyMap.get(listType);
		
		// If the key is not found, throw a json error
		if (vo == null) {
			putModuleData(null, 0, false, "List Type Not Found in KeyMap", true);
			return;
		}

		try {
			if (Convert.formatBoolean(vo.getValue())) {
				Method method = this.getClass().getMethod(vo.getKey().toString(), req.getClass());
				putModuleData(method.invoke(this, req));
			} else {
				Method method = this.getClass().getMethod(vo.getKey().toString());
				putModuleData(method.invoke(this));
			}

		} catch (Exception e) {
			log.error("Unable to retrieve list: " + listType, e);
		}
	}

	/**
	 * Load a yes no list
	 * @return
	 */
	public List<GenericVO> getYesNoLookup() {
		List<GenericVO> yesNo = new ArrayList<>();

		yesNo.add(new GenericVO("1","Yes"));
		yesNo.add(new GenericVO("0","No"));

		return yesNo;
	}

	/**
	 * Gets the supported locales for the app
	 * @return
	 */
	public List<GenericVO> getRoles() {

		StringBuilder sql = new StringBuilder(128);
		sql.append("select role_id as key, role_nm as value from role ");
		sql.append("where role_id = '100' or organization_id = 'MTS' "); 
		sql.append("order by role_nm ");
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * Retruns a list of user prefixes
	 * @return
	 */
	public List<GenericVO> getPrefix() {
		List<GenericVO> selectList = new ArrayList<>(8);
		selectList.add(new GenericVO("Mr.", "Mr."));
		selectList.add(new GenericVO("Mrs.", "Mrs."));
		selectList.add(new GenericVO("Ms", "Ms."));
		selectList.add(new GenericVO("Miss", "Miss"));

		return selectList;
	}
	
	/**
	 * Gets the supported genders for the app
	 * @return
	 */
	public List<GenericVO> getGenders() {
		List<GenericVO> data = new ArrayList<>(8);
		data.add(new GenericVO("F", "Female"));
		data.add(new GenericVO("M", "Male"));

		return data;
	}
	
	/**
	 * Gets a list of attribute categories
	 * @return
	 */
	public List<GenericVO> getCategories(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(64);
		OrgMetadataAction oma = new OrgMetadataAction(getDBConnection(), getAttributes());
		List<MetadataVO> items = oma.getOrgMetadata("MTS", null, false);
		String filter = req.getParameter("parentId");
		
		for (MetadataVO md : items) {
			if (StringUtil.isEmpty(filter)) data.add(new GenericVO(null, md.getFieldName()));
			if (! StringUtil.isEmpty(filter) &&  !md.getMetadataId().equals(filter)) continue;
			
			for (MetadataVO option : md.getOptions()) {
				if (! StringUtil.isEmpty(filter) && StringUtil.isEmpty(option.getParentId())) continue;
				data.add(new GenericVO(option.getMetadataId(), option.getFieldName()));
			}
		}
		
		return data;
	}

	/**
	 * Returns a list of users
	 * @param req
	 * @return
	 */
	public List<GenericVO> getUsers(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(10);
		String roleId = req.getParameter("roleId");
		String subType = req.getParameter("subscriptionTypeCode");
		BSTableControlVO bst = new BSTableControlVO(req, MTSUserVO.class);
		bst.setLimit(1000);
		
		UserAction ua = new UserAction(getDBConnection(), getAttributes());
		GridDataVO<MTSUserVO> users = ua.getAllUsers(bst, roleId, subType, null);
		
		for (MTSUserVO user : users.getRowData()) {
			data.add(new GenericVO(user.getUserId(), user.getFullName()));
		}

		return data;
	}
	
	/**
	 * Gets a list of the editors in the system
	 * @return
	 */
	public List<GenericVO> getEditors() {
		List<GenericVO> data = new ArrayList<>(16);
		UserAction ua = new UserAction(getDBConnection(), getAttributes());
		List<MTSUserVO> editors = ua.getEditors();
		
		for (MTSUserVO editor : editors) {
			data.add(new GenericVO(editor.getUserId(), editor.getFullName()));
		}
		
		return data;
	}
	
	/**
	 * gets a lit of asset types for uploading of assets
	 * @return
	 */
	public List<GenericVO> getAssetTypes() {
		List<GenericVO> data = new ArrayList<>(16);
		
		for (AssetType at : AssetType.values()) {
			data.add(new GenericVO(at.name(), at.getAssetName()));
		}
		
		Collections.sort(data, (a, b) -> ((String)a.getValue()).compareTo((String)b.getValue()));
		
		return data;
	}
	
	/**
	 * gets a list of publications
	 * @return
	 */
	public List<GenericVO> getPublications(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(16);
		
		// Get params
		boolean hasIssues = req.getBooleanParameter("hasIssues");
		boolean hidePublic = req.getBooleanParameter("hidePublic");
		
		PublicationAction pa = new PublicationAction(getDBConnection(), getAttributes());
		List<PublicationVO> pubs = pa.getPublications();
		for (PublicationVO pub : pubs) {
			boolean addRecord = true;
			
			if (hasIssues && pub.getNumberIssues() == 0) addRecord = false;
			if (hidePublic && pub.getPublicFlag() == 1) addRecord = false;
			if (addRecord) data.add(new GenericVO(pub.getPublicationId(), pub.getName()));
		}
		
		return data;
	}
	
	/**
	 * Gets a list of issues for a given publication
	 * @return
	 */
	public List<GenericVO> getIssues(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>(16);
		BSTableControlVO bst = new BSTableControlVO(req, IssueVO.class);
		bst.setLimit(1000);
		String publicationId = StringUtil.checkVal(req.getParameter("publicationId")).toUpperCase();
		
		IssueAction ia = new IssueAction(getDBConnection(), getAttributes());
		GridDataVO<IssueVO> issues = ia.getIssues(publicationId, false, bst);
		for (IssueVO issue : issues.getRowData()) {
			data.add(new GenericVO(issue.getIssueId(), issue.getName()));
		}
		
		return data;
	}

	/**
	 * gets a lit of asset types for uploading of assets
	 * @return
	 */
	public List<GenericVO> getSubscriptions() {
		List<GenericVO> data = new ArrayList<>(16);
		
		for (SubscriptionType st : SubscriptionType.values()) {
			data.add(new GenericVO(st.name(), st.getTypeName()));
		}
		
		Collections.sort(data, (a, b) -> ((String)a.getValue()).compareTo((String)b.getValue()));
		
		return data;
	}
	
	/**
	 * Gets a lit of articles.  supports type ahead and full list
	 * @return
	 */
	public List<GenericVO> getArticles() {
		StringBuilder sql = new StringBuilder(256);
		sql.append("select a.document_id as key, "); 
		sql.append("substring(action_nm, 0, 50 + position(' ' in substring(action_nm, 50))) || ");
		sql.append("CASE WHEN position(' ' in substring(action_nm, 50)) > 0  THEN ' ...' else '' END as value ");
		sql.append("from ").append(getCustomSchema()).append("mts_document a ");
		sql.append("inner join sb_action b on a.document_id = b.action_group_id and pending_sync_flg = 0 ");
		sql.append("order by action_nm ");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new GenericVO());
	}
	
	/**
	 * 
	 * @param req
	 * @return
	 */
	public List<GenericVO> getArticlesAC(ActionRequest req) {
		// Build the serach terms
		StringBuilder term = new StringBuilder(32);
		String[] terms = StringUtil.checkVal(req.getParameter(REQ_SEARCH)).split(" ");

		for (int i=0; i < terms.length; i++) {
			if (i > 0) term.append(" & ");
			term.append(terms[i].replace("'","''")).append(":*");
		}
		
		// Build the sql using Full text indexing
		StringBuilder sql = new StringBuilder(512);
		sql.append("select key, value from ( ");
		sql.append("select action_nm, '/' || lower(publication_id) || '/article/' || direct_access_pth as key, ");
		sql.append("concat(action_nm, '|-- ', first_nm, ' ', last_nm)  as value, ");
		sql.append("to_tsvector(action_nm) || "); 
		sql.append("to_tsvector(coalesce(action_desc, '')) || ");
		sql.append("to_tsvector(coalesce(first_nm, '')) || ");
		sql.append("to_tsvector(coalesce(last_nm, '')) as document ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("mts_document a ");
		sql.append(DBUtil.INNER_JOIN).append("sb_action b ");
		sql.append("on a.action_group_id = b.action_group_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("mts_issue i ");
		sql.append("on a.issue_id = i.issue_id ");
		sql.append(DBUtil.INNER_JOIN).append("document d ");
		sql.append("on b.action_id = d.action_id ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("mts_user c ");
		sql.append("on a.author_id = c.user_id ) as search ");
		sql.append("where search.document @@ to_tsquery('").append(term).append("') ");
		sql.append("order by action_nm limit 10");
		log.debug(sql.length() + "|" + sql);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), null, new GenericVO());
	}
}
