package com.wsla.action;

// WSLA Statics
import static com.wsla.action.admin.ProductCategoryAction.PROD_CAT_ID;
import static com.wsla.action.admin.ProductMasterAction.REQ_PRODUCT_ID;
import static com.wsla.action.admin.ProviderAction.REQ_PROVIDER_ID;

// JDK 1.8.x
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.PhoneNumberFormat;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.ResourceBundleManagerAction;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.resource.WCResourceBundle;
import com.smt.sitebuilder.security.SBUserRole;

// WSLA Libs
import com.wsla.action.admin.BillableActivityAction;
import com.wsla.action.admin.InventoryAction;
import com.wsla.action.admin.ProductCategoryAction;
import com.wsla.action.admin.ProductMasterAction;
import com.wsla.action.admin.ProductSetAction;
import com.wsla.action.admin.ProviderAction;
import com.wsla.action.admin.ProviderLocationAction;
import com.wsla.action.admin.StatusCodeAction;
import com.wsla.action.admin.WarrantyAction;
import com.wsla.action.admin.WarrantyAction.ServiceTypeCode;
import com.wsla.action.ticket.CASSelectionAction;
import com.wsla.action.ticket.TicketListAction;
import com.wsla.action.ticket.TicketSearchAction;
import com.wsla.action.ticket.TicketEditAction;
import com.wsla.common.LocaleWrapper;
import com.wsla.common.WSLALocales;
import com.wsla.action.ticket.transaction.RefundReplacementTransaction;
import com.wsla.action.ticket.transaction.RefundReplacementTransaction.ApprovalTypes;
import com.wsla.action.ticket.transaction.RefundReplacementTransaction.DispositionCodes;
import com.wsla.common.WSLAConstants.WSLARole;
import com.wsla.data.product.LocationItemMasterVO;
import com.wsla.data.product.ProductSetVO;
import com.wsla.data.product.ProductVO;
import com.wsla.data.product.WarrantyType;
import com.wsla.data.provider.ProviderLocationVO;
import com.wsla.data.provider.ProviderPhoneVO;
import com.wsla.data.provider.ProviderType;
import com.wsla.data.ticket.BillableActivityVO;
import com.wsla.data.ticket.BillableActivityVO.BillableTypeCode;
import com.wsla.data.ticket.StatusCode;
import com.wsla.data.ticket.StatusCodeVO;
import com.wsla.data.ticket.TicketAssignmentVO;
import com.wsla.data.ticket.TicketAssignmentVO.TypeCode;
import com.wsla.data.ticket.TicketCommentVO.CommunicationRole;
import com.wsla.data.ticket.TicketScheduleVO;
import com.wsla.data.ticket.TicketVO.Standing;
import com.wsla.data.ticket.UserVO;

/****************************************************************************
 * <b>Title</b>: SelectLookupAction.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Provides a mechanism for looking up key/values for select lists.
 * Each type will return a collection of GenericVOs, which will automatically be
 * available in a select picker
 * as listType
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Sep 11, 2018
 * @updates:
 ****************************************************************************/
public class SelectLookupAction extends SBActionAdapter {

	/**
	 * Key to be passed to utilize this action
	 */
	public static final String SELECT_KEY = "selectType";

	private static final String PROVIDER_TYPE = "providerType";

	private static final String REQ_SEARCH = "search";

	private static Map<String, GenericVO> keyMap = new HashMap<>(16);

	/**
	 * Assigns the keys for the select type to method mapping.  In the generic vo
	 * the key is the method name.  The value is a boolean which indicates whether
	 * or not the request object is needed in that method 
	 */
	static {
		keyMap.put("statusCode", new GenericVO("getStatusCodes", Boolean.TRUE));
		keyMap.put("oem", new GenericVO("getOems", Boolean.TRUE));
		keyMap.put("attributeGroupCode", new GenericVO("getAttributeGroups", Boolean.FALSE));
		keyMap.put("attributes", new GenericVO("getAttributes", Boolean.TRUE));
		keyMap.put(PROVIDER_TYPE, new GenericVO("getProviderTypes", Boolean.FALSE));
		keyMap.put("provider", new GenericVO("getProviders", Boolean.TRUE));
		keyMap.put("providerWLoc", new GenericVO("getProvidersWithLocations", Boolean.TRUE));
		keyMap.put("userProvider", new GenericVO("getUserProviders", Boolean.TRUE));
		keyMap.put("oemParts", new GenericVO("getProviderParts", Boolean.TRUE));
		keyMap.put("providerLocations", new GenericVO("getProviderLocations", Boolean.TRUE));
		keyMap.put("activeFlag", new GenericVO("getYesNoLookup", Boolean.TRUE));
		keyMap.put("warrantyServiceTypeCode", new GenericVO("getServiceTypeCode", Boolean.FALSE));
		keyMap.put("role", new GenericVO("getOrgRoles", Boolean.TRUE));
		keyMap.put("locale", new GenericVO("getLocales", Boolean.FALSE));
		keyMap.put("gender", new GenericVO("getGenders", Boolean.TRUE));
		keyMap.put("prefix", new GenericVO("getPrefix", Boolean.TRUE));
		keyMap.put("defect", new GenericVO("getDefects", Boolean.TRUE));
		keyMap.put("product", new GenericVO("getProducts", Boolean.TRUE));
		keyMap.put("productSetParts", new GenericVO("getProductSetParts", Boolean.TRUE));
		keyMap.put("warranty", new GenericVO("getWarrantyList", Boolean.TRUE));
		keyMap.put("warrantyType", new GenericVO("getWarrantyTypeList", Boolean.FALSE));
		keyMap.put("category", new GenericVO("getProductCategories", Boolean.TRUE));
		keyMap.put("acRetailer", new GenericVO("getRetailerACList", Boolean.TRUE));
		keyMap.put("categoryGroup", new GenericVO("getCategoryGroups", Boolean.FALSE));
		keyMap.put("ticketAssignment", new GenericVO("getTicketAssignments", Boolean.TRUE));
		keyMap.put("tickets", new GenericVO("getTickets", Boolean.TRUE));
		keyMap.put("scheduleTransferType", new GenericVO("getScheduleTransferTypes", Boolean.TRUE));
		keyMap.put("acCas", new GenericVO("getAcCas", Boolean.TRUE));
		keyMap.put("harvestCas", new GenericVO("getAcCas", Boolean.TRUE));
		keyMap.put("closestCas", new GenericVO("getClosestCas", Boolean.TRUE));
		keyMap.put("inventorySuppliers", new GenericVO("getInventorySuppliers", Boolean.TRUE));
		keyMap.put("locationInventory", new GenericVO("getLocationInventory", Boolean.TRUE));
		keyMap.put("emailCampaigns", new GenericVO("getEmailCampaigns", Boolean.TRUE));
		keyMap.put("billable", new GenericVO("getBillableCodes", Boolean.TRUE));
		keyMap.put("billableType", new GenericVO("getBillableTypes", Boolean.FALSE));
		keyMap.put("supportNumbers", new GenericVO("getSupportNumbers", Boolean.TRUE));
		keyMap.put("ticketSearch", new GenericVO("ticketSearch", Boolean.TRUE));
		keyMap.put("refRepApprovalType", new GenericVO("getRefRepApprovalType", Boolean.TRUE));
		keyMap.put("refRepDispostionType", new GenericVO("getRefRepDispostionType", Boolean.TRUE));
		keyMap.put("standing", new GenericVO("getStanding", Boolean.TRUE));
		keyMap.put("acShipLocation", new GenericVO("getAcShippingLocation", Boolean.TRUE));
		keyMap.put("surveyResults", new GenericVO("getSurveyResults", Boolean.TRUE));
		keyMap.put("profeco", new GenericVO("getProfecoList", Boolean.TRUE));
		keyMap.put("commRole", new GenericVO("getCommRole", Boolean.TRUE));
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

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		String listType = req.getStringParameter(SELECT_KEY);
		GenericVO vo = keyMap.get(listType);
		if (vo == null)
			throw new ActionException("List type Not Found in KeyMap");

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
	 * Compile a list of Provider Type from the database, ordered by type_cd
	 * @return
	 */
	public List<GenericVO> getProviderTypes() {
		StringBuilder sql = new StringBuilder(128);
		sql.append("select provider_type_id as key, type_cd as value from ");
		sql.append(getCustomSchema()).append("wsla_provider_type order by type_cd");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), null, new GenericVO());
	}

	/**
	 * gets the refund replacement approval type and returns an internationalized name and value
	 * @return
	 */
	public List<GenericVO> getRefRepApprovalType(ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		ResourceBundle rb = WCResourceBundle.getBundle(site.getResourceBundleClass(),user.getUserLocale());
		String prefix = "refrep.approvalType.";

		List<GenericVO> list = new ArrayList<>();
		for (ApprovalTypes at : RefundReplacementTransaction.ApprovalTypes.values()) {
			list.add(new GenericVO(at.name(), rb.getString(prefix+at.name())));
		}
		return list;
	}

	/**
	 * gets the refund replacement disposition type and return an internationalized name and value
	 * @return
	 */
	public List<GenericVO> getRefRepDispostionType(ActionRequest req){
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		ResourceBundle rb = WCResourceBundle.getBundle(site.getResourceBundleClass(),user.getUserLocale());
		String prefix = "refund.disposition.";

		List<GenericVO> list = new ArrayList<>();
		for (DispositionCodes dt : RefundReplacementTransaction.DispositionCodes.values()) {
			list.add(new GenericVO(dt.name(), rb.getString(prefix+dt.name())));
		}
		return list;
	}


	/**
	 * selects the existing attribute groups
	 * @return
	 */
	public List<GenericVO> getAttributeGroups(){
		StringBuilder sql = new StringBuilder(128);
		sql.append("select attribute_group_cd as key, group_nm as value from ");
		sql.append(getCustomSchema()).append("wsla_attribute_group order by group_nm");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), null, new GenericVO());

	}

	/**
	 * 
	 * @param req
	 * @return
	 */
	public List<GenericVO> getAttributes(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		StringBuilder sql = new StringBuilder(128);
		sql.append("select attribute_cd as key, attribute_nm as value from ");
		//limit the credit memo asset to only using the refund tab
		sql.append(getCustomSchema()).append("wsla_ticket_attribute where 1=1 and attribute_cd != 'attr_credit_memo' ");

		if (req.hasParameter("groupCode")) {
			sql.append("and attribute_group_cd = ? ");
			vals.add(req.getParameter("groupCode"));
		}

		sql.append("order by attribute_nm");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), vals, new GenericVO());
	}

	/**
	 * Load a yes no list
	 * @return
	 */
	public List<GenericVO> getYesNoLookup(ActionRequest req) {
		LocaleWrapper lw = new LocaleWrapper(req);
		List<GenericVO> yesNo = new ArrayList<>();

		if (lw.getLocale().equals(new Locale("es", "MX"))) {
			yesNo.add(new GenericVO("1","Sí"));
			yesNo.add(new GenericVO("0","No"));
		} else {
			yesNo.add(new GenericVO("1","Yes"));
			yesNo.add(new GenericVO("0","No"));
		}

		return yesNo;
	}

	/**
	 * loads a list of the warranty service types
	 * @return
	 */
	public List<GenericVO> getServiceTypeCode(){
		List<GenericVO> types = new ArrayList<>();
		for (ServiceTypeCode e : WarrantyAction.ServiceTypeCode.values()) {
			types.add(new GenericVO(e.name(),e.getValue()));
		}

		return types;
	}

	/**
	 * Returns the list of roles for the WSLA org
	 * @param req
	 * @return
	 */
	public List<GenericVO> getOrgRoles(ActionRequest req) {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);

		StringBuilder sql = new StringBuilder(64);
		sql.append("select role_id as key, role_nm as value from role ");
		sql.append("where organization_id = ? or role_id = '100' order by role_nm");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), Arrays.asList(site.getOrganizationId()), new GenericVO());
	}

	/**
	 * Load a list of Providers that have users assigned to locations.  
	 * @return
	 */
	public List<GenericVO> getUserProviders(ActionRequest req) {

		List<Object> vals = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(150);
		sql.append("select pl.provider_id as key, p.provider_nm as value from ").append(getCustomSchema()).append("wsla_provider_location pl ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider p on pl.provider_id = p.provider_id ");
		sql.append("where location_id in ( ");
		sql.append("select location_id from ").append(getCustomSchema()).append("wsla_provider_user_xr group by location_id ");
		sql.append(") group by pl.provider_id, p.provider_nm order by value ");
		
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<GenericVO> data = db.executeSelect(sql.toString(), vals, new GenericVO());
		log.debug("user providers size " + data.size());
		return data;
		
	}
	
	/**
	 * Load a list of Providers from the ProviderAction.  Must pass in the 
	 * ProviderType to determine which type to retrieve.  If providerType is not passed, 
	 * default to OEM
	 * @return
	 */
	public List<GenericVO> getProviders(ActionRequest req) {
		String search = req.getParameter(REQ_SEARCH);
		boolean incUnknown = req.getBooleanParameter("incUnknown");
		String providerType = StringUtil.checkVal(req.getParameter(PROVIDER_TYPE));
		ProviderType pt = null;
		if (! providerType.isEmpty() && ! "ALL".equalsIgnoreCase(providerType)) 
			pt = EnumUtil.safeValueOf(ProviderType.class, providerType);

		int limit = req.getIntegerParameter("limit", 0);
		return new ProviderAction(getAttributes(), getDBConnection()).getProviderOptions(pt, search, incUnknown, limit);
	}

	
	/**
	 *Loads a list of provides that have provider locations
	 * @return
	 */
	public List<GenericVO> getProvidersWithLocations(ActionRequest req) {
		List<Object> vals = new ArrayList<>();
		
		StringBuilder sql = new StringBuilder(100);
		sql.append(DBUtil.SELECT_CLAUSE).append("distinct pro.provider_id as key, pro.provider_nm as value from ").append(getCustomSchema()).append("wsla_provider pro ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location pl on pro.provider_id = pl.provider_id order by value ");

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		List<GenericVO> data = db.executeSelect(sql.toString(), vals, new GenericVO());
		log.debug("data size " + data.size());
		return data;
	}
	
	/**
	 * return a list of OEMs - a specific type of providers.  Distinguished from 'getProvider' to avoid coupling in View logic.
	 * @param req
	 * @return
	 */
	public List<GenericVO> getOems(ActionRequest req) {
		if (req.hasParameter(PROD_CAT_ID)) {
			boolean incUnknown = req.getBooleanParameter("incUnknown");
			return new ProviderAction(getAttributes(), getDBConnection())
					.getOEMsByProductCategory(req.getParameter(PROD_CAT_ID), incUnknown);

		} else {
			req.setParameter(PROVIDER_TYPE, ProviderType.OEM.toString());
			return getProviders(req);
		}
	}


	/**
	 * load a list of PARTS tied to the given provider (likely an OEM)
	 * @param req
	 * @return
	 */
	public List<GenericVO> getProviderParts(ActionRequest req) {
		ProductMasterAction pa = new ProductMasterAction(getAttributes(), getDBConnection());
		return pa.listProducts(req.getParameter(REQ_PROVIDER_ID), null, Integer.valueOf(0));
	}

	/**
	 * List all provider locations
	 * Optional: pass providerId on the request for a subset of data.
	 * @param req
	 * @return
	 */
	public List<GenericVO> getProviderLocations(ActionRequest req) {
		BSTableControlVO bst = new BSTableControlVO(req, ProviderLocationVO.class);
		bst.setLimit(100000); //get them all
		ProviderLocationAction pla = new ProviderLocationAction(getAttributes(), getDBConnection());
		GridDataVO<ProviderLocationVO> locations = pla.getLocations(req.getParameter(REQ_PROVIDER_ID), bst, req.getBooleanParameter("getProvider"));

		//turn the locations into relevant <locationId,locationName> pairs (GenericVO)
		String name;
		List<GenericVO> data = new ArrayList<>(locations.getRowData().size());
		for (ProviderLocationVO vo : locations.getRowData()) {
			name = StringUtil.join(StringUtil.checkVal(vo.getProviderName()), ": ", StringUtil.checkVal(vo.getLocationName()), " ", StringUtil.checkVal(vo.getStoreNumber()));
			data.add(new GenericVO(vo.getLocationId(), name));
		}

		log.debug("loaded " + data.size() + " provider locations");
		return data;
	}

	/**
	 * Returns a list of matching provider locations for autocomplete
	 * @param req
	 * @return
	 */
	public List<GenericVO> getRetailerACList(ActionRequest req) {
		StringBuilder term = new StringBuilder(32);
		String[] terms = StringUtil.checkVal(req.getParameter(REQ_SEARCH)).split(" ");
		for (int i=0; i < terms.length; i++) {
			if (i > 0) term.append(" & ");
			term.append(terms[i]).append(":*");
		}
		log.debug("Search Term: " + term);

		StringBuilder sql = new StringBuilder(512);
		sql.append("select key, value from ( ");
		sql.append("select location_id as key, coalesce(provider_nm, '') || ' - ' ");
		sql.append("|| coalesce(location_nm, '') || ' (' || coalesce(store_no, '') || ')  ' ");
		sql.append("|| coalesce(city_nm, '') || ', ' || coalesce(state_cd, '') as value, ");
		sql.append("to_tsvector(provider_nm) || "); 
		sql.append("to_tsvector(location_nm) || ");
		sql.append("to_tsvector(coalesce(store_no, '')) || ");
		sql.append("to_tsvector(coalesce(city_nm, '')) as document ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_provider a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location b ");
		sql.append("on a.provider_id = b.provider_id ");
		sql.append("where provider_type_id = 'RETAILER' ) as search ");
		sql.append("where search.document @@ to_tsquery('").append(term).append("') ");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		return db.executeSelect(sql.toString(), null, new GenericVO(),null,req.getIntegerParameter("offset").intValue(), req.getIntegerParameter("limit").intValue());
	}

	/**
	 * Gets the list of the closest cas
	 * @param req
	 * @return
	 */
	public List<GenericVO> getClosestCas(ActionRequest req) {

		String ticketId = req.getParameter("ticketId");
		UserVO user = (UserVO)getAdminUser(req).getUserExtendedInfo();

		CASSelectionAction csa = new CASSelectionAction(getDBConnection(), attributes);
		return csa.getUserSelectionList(ticketId, user.getLocale());
	}

	/**
	 * Returns a list of matching provider locations for auto-complete
	 * @param req
	 * @return
	 */
	public List<GenericVO> getAcCas(ActionRequest req) {
		String providerId = req.getParameter("providerId");
		StringBuilder term = new StringBuilder(16);
		term.append("%").append(StringUtil.checkVal(req.getParameter(REQ_SEARCH)).toLowerCase()).append("%");

		StringBuilder sql = new StringBuilder(512);
		sql.append("select location_id as key, coalesce(provider_nm, '') || ' - ' ");
		sql.append("|| coalesce(location_nm, '') || ' (' || coalesce(store_no, '') || ')  ' ");
		sql.append("|| coalesce(city_nm, '') || ', ' || coalesce(state_cd, '') as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_provider a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema()).append("wsla_provider_location b ");
		sql.append("on a.provider_id = b.provider_id ");
		sql.append("where provider_type_id = 'CAS' ");
		sql.append("and (lower(provider_nm) like ? or lower(location_nm) like ? ");
		sql.append("or lower(city_nm) like ? or store_no like ?) ");
		List<Object> vals = new ArrayList<>();

		vals.add(term);
		vals.add(term);
		vals.add(term);
		vals.add(term);

		if (!StringUtil.isEmpty(req.getParameter("providerId"))) {
			sql.append(" and a.provider_id = ? ");
			vals.add(providerId);
		}

		if("harvestCas".equalsIgnoreCase(req.getStringParameter(SELECT_KEY))) {
			sql.append("and b.location_id in (select location_id from ").append(getCustomSchema());
			sql.append("wsla_ticket_assignment where assg_type_cd = 'CAS' and ticket_id in ");
			sql.append("( SELECT ticket_id FROM ").append(getCustomSchema());
			sql.append("wsla_ticket_data where attribute_cd = 'attr_harvest_status' and value_txt = 'HARVEST_APPROVED') ");
		}

		sql.append(") order by provider_nm");
		log.info(" sql " + sql +"|" + vals);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		List<GenericVO> data = db.executeSelect(sql.toString(), vals, new GenericVO());
		log.debug("acCas size " + data.size());
		return data;
	}


	/**
	 * Returns an auto-complete list of locations for shipping (providers and users)
	 * 
	 * @param req
	 * @return
	 */
	public List<GenericVO> getAcShippingLocation(ActionRequest req) {
		StringBuilder term = new StringBuilder(20);
		term.append("%").append(StringUtil.checkVal(req.getParameter(REQ_SEARCH)).toLowerCase()).append("%");

		// Set the "fuzzy" search term
		String docTerm = StringUtil.checkVal(req.getParameter(REQ_SEARCH)).toLowerCase();
		while (docTerm.contains("  ")) {
			docTerm = StringUtil.replace(docTerm, "  ", " ");
		}
		docTerm = docTerm.trim();
		docTerm = StringUtil.replace(docTerm, " ", ":*& ") + ":*";

		// Set the first part of the query
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(750);
		sql.append(DBUtil.SELECT_CLAUSE).append("key, value").append(DBUtil.FROM_CLAUSE).append("(");
		sql.append(DBUtil.SELECT_CLAUSE).append("location_id as key, coalesce(provider_nm, '') || ' - ' ");
		sql.append("|| coalesce(location_nm, '') || ' (' || coalesce(store_no, '') || ') ' ");
		sql.append("|| coalesce(city_nm, '') || ', ' || coalesce(state_cd, '') as value, ");
		sql.append("to_tsvector(provider_nm) || ' ' || to_tsvector(location_nm) || ' ' || to_tsvector(coalesce(store_no, '')) || ' ' || ");
		sql.append("to_tsvector(coalesce(city_nm, '')) || ' ' || to_tsvector(coalesce(state_cd, '')) as document");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_provider p");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_provider_location pl on p.provider_id = pl.provider_id");
		sql.append(") as search");
		sql.append(DBUtil.WHERE_CLAUSE).append("search.document @@ to_tsquery(?) ");

		List<Object> vals = new ArrayList<>();
		vals.add(docTerm);

		// If a ticket id exists, set the second part of the query
		String ticketId = req.getParameter(TicketEditAction.TICKET_ID);
		if (!StringUtil.isEmpty(ticketId)) {
			sql.append(DBUtil.UNION);
			sql.append(DBUtil.SELECT_CLAUSE).append("user_id as key, first_nm || ' ' || last_nm ");
			sql.append("|| ' - ' || coalesce(city_nm, '') || ', ' || coalesce(state_cd, '') as value");
			sql.append(DBUtil.FROM_CLAUSE).append(schema).append("wsla_ticket t");
			sql.append(DBUtil.INNER_JOIN).append(schema).append("wsla_user u on t.originator_user_id = u.user_id");
			sql.append(DBUtil.INNER_JOIN).append("profile_address pa on u.profile_id = pa.profile_id");
			sql.append(DBUtil.WHERE_CLAUSE).append("(lower(first_nm || ' ' || last_nm) like ? or lower(city_nm) like ?)");
			sql.append("and ticket_id = ? ");
			vals.add(term);
			vals.add(term);
			vals.add(ticketId);
		}

		sql.append(DBUtil.ORDER_BY).append("value");

		// Execute the query
		DBProcessor dbp = new DBProcessor(getDBConnection(), getCustomSchema());
		dbp.setGenerateExecutedSQL(log.isDebugEnabled());
		return dbp.executeSelect(sql.toString(), vals, new GenericVO());
	}

	/**
	 * Returns a list of status codes and their descriptions
	 * @return
	 */
	public List<GenericVO> getStatusCodes(ActionRequest req) {
		LocaleWrapper lw = new LocaleWrapper(req);
		List<GenericVO> data = new ArrayList<>(64);
		StatusCodeAction sca = new StatusCodeAction(getDBConnection(), getAttributes());
		List<StatusCodeVO> codes = sca.getStatusCodes(req.getParameter("roleId"), lw.getLocale(), null);

		for(StatusCodeVO sc : codes) {
			data.add(new GenericVO(sc.getStatusCode(), sc.getStatusName().trim()));
		}

		return data;
	}
	/**
	 * Gets the supported locales for the app
	 * @return
	 */
	public List<GenericVO> getLocales() {
		List<GenericVO> data = new ArrayList<>(8);
		List<WSLALocales> base = Arrays.asList(WSLALocales.getBaseLocales());

		for (WSLALocales val : WSLALocales.values()) {
			if (!base.contains(val)) {
				data.add(new GenericVO(val, val.getDesc()));
			}
		}

		return data;
	}

	/**
	 * Gets the supported genders for the app
	 * @return
	 */
	public List<GenericVO> getGenders(ActionRequest req) {
		LocaleWrapper lw = new LocaleWrapper(req);
		List<GenericVO> data = new ArrayList<>(8);

		if (lw.getLocale().equals(new Locale("es", "MX"))) {
			data.add(new GenericVO("F", "Mujer"));
			data.add(new GenericVO("M", "Hombre"));
		} else {
			data.add(new GenericVO("F", "Female"));
			data.add(new GenericVO("M", "Male"));
		}

		return data;
	}

	/**
	 * Gets the list of defects adds the 
	 * @return
	 */
	public List<GenericVO> getDefects(ActionRequest req) {

		// Get the Locale to pull correct language and add to the DB params
		Locale locale = new ResourceBundleManagerAction().getUserLocale(req);
		List<Object> params = new ArrayList<>();
		params.add(locale.getLanguage());
		params.add(locale.getCountry());
		params.add(req.getStringParameter("defectType", "DEFECT_CODE"));

		StringBuilder sql = new StringBuilder(64);
		sql.append("select defect_cd as key, ");
		sql.append("case when value_txt is null then defect_nm else value_txt end as value ");
		sql.append(DBUtil.FROM_CLAUSE).append(getCustomSchema()).append("wsla_defect a ");
		sql.append("left outer join resource_bundle_key c on a.defect_cd = c.key_id ");
		sql.append("left outer join resource_bundle_data d on c.key_id = d.key_id ");
		sql.append("and language_cd = ? and country_cd = ? ");
		sql.append("where a.active_flg = 1 and defect_type_cd in ('BOTH', ?) ");
		sql.append("and (a.provider_id is null ");
		if (req.hasParameter(REQ_PROVIDER_ID)) {
			sql.append("or a.provider_id = ? ");
			params.add(req.getParameter(REQ_PROVIDER_ID));
		}

		sql.append(") order by value");
		log.debug("defects SQL " + sql + "|" + params);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql.toString(), params, new GenericVO());
	}

	/**
	 * Retruns a list of user prefixes
	 * @return
	 */
	public List<GenericVO> getPrefix(ActionRequest req) {
		LocaleWrapper lw = new LocaleWrapper(req);
		List<GenericVO> selectList = new ArrayList<>(8);

		if (lw.getLocale().equals(new Locale("es", "MX"))) {
			selectList.add(new GenericVO("Mr.", "Señor"));
			selectList.add(new GenericVO("Mrs.", "Señora"));
			selectList.add(new GenericVO("Miss", "Srta."));
		} else {
			selectList.add(new GenericVO("Mr.", "Mr."));
			selectList.add(new GenericVO("Mrs.", "Mrs."));
			selectList.add(new GenericVO("Ms", "Ms."));
			selectList.add(new GenericVO("Miss", "Miss"));
		}

		return selectList;
	}

	/**
	 * Returns a list of user products
	 * @return
	 */
	public List<GenericVO> getProducts(ActionRequest req) {
		ProductMasterAction ai =  new ProductMasterAction(getAttributes(), getDBConnection());
		BSTableControlVO bst = new BSTableControlVO(req, ProductVO.class);
		bst.setLimit(1000);
		bst.setOffset(0);
		String providerId = req.getParameter(REQ_PROVIDER_ID);
		Integer setFlag = req.getIntegerParameter("setFlag");
		GridDataVO<ProductVO> products = ai.getProducts(null, providerId, setFlag, 1, bst);

		List<GenericVO> data = new ArrayList<>(products.getTotal());
		for (ProductVO product : products.getRowData()) {
			String name = product.getCustomerProductId() + " : " + product.getProductName();
			data.add(new GenericVO(product.getProductId(), name	));
		}

		return data;
	}

	/**
	 * Returns a list of products that are part of the set matching the passed productId
	 * @return
	 */
	public List<GenericVO> getProductSetParts(ActionRequest req) {
		ProductSetAction psa =  new ProductSetAction(getAttributes(), getDBConnection());
		BSTableControlVO bst = new BSTableControlVO(req, ProductSetVO.class);
		bst.setLimit(1000);
		bst.setOffset(0);
		GridDataVO<ProductSetVO> products = psa.getSet(req.getParameter("productId"), bst);

		List<GenericVO> data;
		if (products != null) {
			data = new ArrayList<>(products.getTotal());	
			for (ProductVO product : products.getRowData()) {
				String name = product.getCustomerProductId() + " - " + product.getProductName();
				data.add(new GenericVO(product.getProductId(), name));
			}
		} else {
			data = new ArrayList<>();
		}

		return data;
	}


	/**
	 * Returns a list of warranties in the DB
	 * @return
	 */
	public List<GenericVO> getWarrantyList(ActionRequest req) {
		String providerId = req.getParameter(REQ_PROVIDER_ID);

		//possibly use productId to find providerId
		if (req.hasParameter(REQ_PRODUCT_ID)) {
			String sql = StringUtil.join("select provider_id as key from ", getCustomSchema(), 
					"wsla_product_master where product_id=?");
			log.debug(sql);
			DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
			List<GenericVO> data = db.executeSelect(sql, Arrays.asList(req.getParameter(REQ_PRODUCT_ID)), new GenericVO());
			if (data != null && !data.isEmpty())
				providerId = StringUtil.checkVal(data.get(0).getKey());
		}

		return new WarrantyAction(getAttributes(), getDBConnection()).listWarranties(providerId);
	}


	/**
	 * Returns a list of WarrantyType values - from the Enum
	 * @return
	 */
	public List<GenericVO> getWarrantyTypeList() {
		List<GenericVO> data = new ArrayList<>();
		for (WarrantyType type : WarrantyType.values())
			data.add(new GenericVO(type.name(), type.typeName));
		return data;
	}


	/**
	 * Default state is to receive the high level (parent) parent categories.
	 * Passing in the productCategoryId will get all of the children for the provided category
	 * passing in allLevels=true will get the entire list of categories
	 * @param req
	 * @return
	 */
	public List<GenericVO> getProductCategories(ActionRequest req) {
		return new ProductCategoryAction(getAttributes(), getDBConnection()).getCategoryList(req);
	}


	/**
	 * Return a distinct list of Group_CD values from the product_group table
	 * @param req
	 * @return
	 */
	public List<GenericVO> getCategoryGroups() {
		return new ProductCategoryAction(getAttributes(), getDBConnection()).getGroupList();
	}


	/**
	 * Return a list of ticket assignments for the given ticket
	 * 
	 * @param req
	 * @return
	 * @throws DatabaseException 
	 */
	public List<GenericVO> getTicketAssignments(ActionRequest req) throws DatabaseException {
		String ticketId = req.getParameter("ticketId");
		List<TicketAssignmentVO> assignments = new TicketEditAction(getAttributes(), getDBConnection()).getAssignments(ticketId);
		String[] excludeId = req.getParameterValues("excludeId") == null ? new String[] {} : req.getParameterValues("excludeId");

		List<GenericVO> data = new ArrayList<>();
		for (TicketAssignmentVO assignment : assignments) {
			if (Arrays.stream(excludeId).anyMatch(assignment.getTicketAssignmentId()::equals)) continue;
			String assignmentName = assignment.getTypeCode() == TypeCode.CALLER ? assignment.getUser().getFirstName() + ' ' + assignment.getUser().getLastName() : assignment.getLocation().getLocationName();
			data.add(new GenericVO(assignment.getTicketAssignmentId(), assignmentName));
		}

		return data;
	}


	/**
	 * Return a list of tickets - optionally by status
	 * @param req
	 * @return
	 */
	public List<GenericVO> getTickets(ActionRequest req) {
		TicketListAction tla = new TicketListAction(getAttributes(), getDBConnection());
		String search = StringUtil.checkVal(req.getParameter("search"));
		return tla.getTickets(EnumUtil.safeValueOf(StatusCode.class, req.getParameter("statusCode")), search );
	}


	/**
	 * Return a list of ticket schedule transfer types
	 * 
	 * @param req
	 * @return
	 */
	public List<GenericVO> getScheduleTransferTypes(ActionRequest req) {
		ResourceBundle bundle = new BasePortalAction().getResourceBundle(req);

		List<GenericVO> data = new ArrayList<>();
		for (TicketScheduleVO.TypeCode type : TicketScheduleVO.TypeCode.values()) {
			data.add(new GenericVO(type.name(), bundle.getString("wsla.ticket.schedule." + type.name())));
		}

		return data;
	}

	/**
	 * Return a list of provider locations who have inventory (records).
	 * Optionally - inventory for a specific productId or custProductId (case insensitive)
	 * @param req
	 * @return
	 */
	public List<GenericVO> getInventorySuppliers(ActionRequest req) {
		String partId = req.getParameter(REQ_PRODUCT_ID, req.getParameter("custProductId"));
		Integer min = req.getIntegerParameter("minInventory", 0); //the minimum inventory to be on hand in order to match

		UserVO user = null;
		String roleId = ((SBUserRole) req.getSession().getAttribute(Constants.ROLE_DATA)).getRoleId();
		if (StringUtil.isEmpty(partId) && !WSLARole.ADMIN.getRoleId().equals(roleId)) {
			user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		}

		InventoryAction ia = new InventoryAction(getAttributes(), getDBConnection());
		List<LocationItemMasterVO> data = ia.listInvetorySuppliers(partId, min, user);

		// Convert the location item master to a generic vo
		List<GenericVO> results = new ArrayList<>(data.size());
		for (LocationItemMasterVO im : data) {
			StringBuilder name = new StringBuilder(64);
			name.append(im.getProvider().getProviderName()).append(": ");
			name.append(im.getLocation().getLocationName()).append(" ");
			name.append(im.getLocation().getStoreNumber());
			results.add(new GenericVO(im.getLocationId(), name));
		}

		return results;
	}

	/**
	 * Return a list of available products (inventory) at the given location.
	 * @param req
	 * @return
	 */
	public List<GenericVO> getLocationInventory(ActionRequest req) {
		String locationId = req.getParameter("locationId");

		BSTableControlVO bst = new BSTableControlVO(req, LocationItemMasterVO.class);
		InventoryAction pa = new InventoryAction(getAttributes(), getDBConnection());
		boolean setFlag = req.getBooleanParameter("setFlag");
		bst.setLimit(40000);
		GridDataVO<LocationItemMasterVO> data = pa.listInventory(locationId, null, bst, setFlag);

		List<GenericVO> products = new ArrayList<>(data.getRowData().size());
		for (LocationItemMasterVO lim : data.getRowData()) {
			products.add(new GenericVO(lim.getProductId(), lim.getProductName()));
		}


		return products;
	}

	/**
	 * Gets a list of email campaigns utilized for notifications
	 * @param req
	 * @return
	 */
	public List<GenericVO> getEmailCampaigns(ActionRequest req) {
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		StringBuilder sql = new StringBuilder(156);
		sql.append("select campaign_instance_id as key, instance_nm as value ");
		sql.append("from email_campaign a ");
		sql.append("inner join email_campaign_instance b on a.email_campaign_id = b.email_campaign_id ");
		sql.append("where organization_id = ? and slug_txt like 'PORTAL_%' ");

		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), Arrays.asList(site.getOrganizationId()), new GenericVO());
	}


	/**
	 * Gets a list of billable codes
	 * @return
	 */
	public List<GenericVO> getBillableCodes(ActionRequest req) {
		String btc = req.getParameter("billableTypeCode");
		boolean isMisc = req.getBooleanParameter("isMiscActivites");
		BSTableControlVO bst = new BSTableControlVO(req, BillableActivityVO.class);
		bst.setLimit(1000);

		// Get the codes
		BillableActivityAction ba = new BillableActivityAction(dbConn, attributes);
		GridDataVO<BillableActivityVO> codes = ba.getCodes(btc, isMisc, bst);
		List<GenericVO> data = new ArrayList<>();

		// Loop the codes and convert to Generic
		for (BillableActivityVO code : codes.getRowData()) {
			if (code.getActiveFlag() == 0) continue;
			data.add(new GenericVO(code.getBillableActivityCode(), code.getActivityName()));
		}

		return data;
	}


	/**
	 * Gets a list of billable codes
	 * @return
	 */
	public List<GenericVO> getBillableTypes() {
		List<GenericVO> data = new ArrayList<>();

		for (BillableTypeCode code : BillableTypeCode.values()) {
			data.add(new GenericVO(code.name(), code.getTypeName()));
		}

		return data;
	}

	/**
	 * Returns any OEMs that have an 800 / support number assigned  Key is the 
	 * name and the value is the phone number
	 * @return
	 */
	public List<GenericVO> getSupportNumbers(ActionRequest req) {
		boolean phoneNumberKey = req.getBooleanParameter("phoneNumberKey");

		StringBuilder sql = new StringBuilder(192);
		sql.append("select b.*, a.provider_nm ").append(DBUtil.FROM_CLAUSE);
		sql.append(getCustomSchema()).append("wsla_provider a ");
		sql.append(DBUtil.INNER_JOIN).append(getCustomSchema());
		sql.append("wsla_provider_phone b on a.provider_id = b.provider_id ");
		sql.append("where b.active_flg = 1 order by provider_nm ");
		log.debug(sql.length() + "|" + sql);

		DBProcessor db = new DBProcessor(getDBConnection());
		List<ProviderPhoneVO> phones = db.executeSelect(sql.toString(), null, new ProviderPhoneVO());
		List<GenericVO> data = new ArrayList<>(phones.size());
		// Format the phone number for display
		for (ProviderPhoneVO entry : phones) {
			String pn = entry.getPhoneNumber();
			PhoneNumberFormat pnf = new PhoneNumberFormat(pn, entry.getCountryCode(), PhoneNumberFormat.INTERNATIONAL_FORMAT);

			if (phoneNumberKey) {
				data.add(new GenericVO(pn, pnf.getFormattedNumber()));

			} else {
				data.add(new GenericVO(entry.getProviderName(), pnf.getFormattedNumber()));
			}
		}

		return data;
	}

	/**
	 * Performs a fuzzy search against multiple fields
	 * @param req
	 * @return
	 */
	public List<GenericVO> ticketSearch(ActionRequest req) {
		TicketSearchAction tsa = new TicketSearchAction(getDBConnection(), getAttributes());

		return tsa.getTickets(req.getParameter(REQ_SEARCH));
	}

	/**
	 * Gets the standing list
	 * @return
	 */
	public List<GenericVO> getStanding(ActionRequest req) {
		LocaleWrapper lw = new LocaleWrapper(req);
		List<GenericVO> data = new ArrayList<>();
		for(Standing standing : Standing.values()) {
			String value = standing.name();
			if (lw.getLocale().equals(new Locale("es", "MX"))) {
				if (standing.equals(Standing.GOOD)) value="Bueno";
				else if (standing.equals(Standing.CRITICAL)) value="Crítico";
				else value="Retrasado";
			}

			data.add(new GenericVO(standing.name(), StringUtil.capitalize(value)));
		}

		// Sort the collection by the value
		Collections.sort(data, (a, b) -> ((String)a.getValue()).compareTo(((String)b.getValue())));

		return data;
	}
	
	/**
	 * Gets the questions and responses for the survey
	 * @param req - Need "fsi" req parameter
	 * @return
	 */
	public List<GenericVO> getSurveyResults(ActionRequest req) {
		String fsi = req.getParameter("fsi");
		StringBuilder sql = new StringBuilder(384);
		sql.append("select b.field_label_nm as key, d.label_txt as value ");
		sql.append("from form_data a ");
		sql.append("inner join form_field b on a.form_field_group_id = b.form_field_group_id ");
		sql.append("left outer join form_field_attr c on b.form_field_id = c.form_field_id and attr_key = 'srcList' ");
		sql.append("left outer join list_data d on c.attr_val = d.list_id and a.value_txt = d.value_txt ");
		sql.append("where form_submittal_id = ? order by a.value_txt");
		log.debug(sql.length() + "|" + sql + "|" + fsi);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSelect(sql.toString(), Arrays.asList(fsi), new GenericVO());
	}
	
	/**
	 * Get comm role
	 * @param req
	 * @return
	 */
	public List<GenericVO> getCommRole(ActionRequest req) {
		List<GenericVO> data = new ArrayList<>();
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		UserVO user = (UserVO) getAdminUser(req).getUserExtendedInfo();
		ResourceBundle rb = WCResourceBundle.getBundle(site.getResourceBundleClass(),user.getUserLocale());
		
		for (CommunicationRole cr : CommunicationRole.values()) {
			data.add(new GenericVO(cr.name(), rb.getString(cr.getCommRoleId())));
		}
		
		Collections.sort(data);
		return data;
	}
	
	
}
