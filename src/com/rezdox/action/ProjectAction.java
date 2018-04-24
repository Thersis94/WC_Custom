package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.DataManagerUtil;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.data.vo.GenericQueryVO;
import com.smt.sitebuilder.data.vo.QueryParamVO;
import com.rezdox.data.InvoiceReportPDF;
import com.rezdox.data.ProjectFormProcessor;
import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.PhotoVO;
import com.rezdox.vo.ProjectMaterialVO;
import com.rezdox.vo.ProjectVO;

/****************************************************************************
 * <b>Title:</b> ProjectAction.java<br/>
 * <b>Description:</b> Manages RezDox Projects.  See data model.
 * This is the action for a Business Owner's view of projects (aka Home History).
 * This action is invoked from the ProjectFacadeAction - not directly.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Mar 22, 2018
 ****************************************************************************/
public class ProjectAction extends SimpleActionAdapter {

	protected static final String REQ_PROJECT_ID = "projectId";
	protected static final String FILTER_DATA_LST = "filterListData";


	public ProjectAction() {
		super();
	}

	public ProjectAction(ActionInitVO arg0) {
		super(arg0);
	}

	/**
	 * overloaded constructor to simplify calling actions
	 * @param dbConnection
	 * @param attributes
	 */
	public ProjectAction(SMTDBConnection dbConnection, Map<String, Object> attributes) {
		this();
		setDBConnection(dbConnection);
		setAttributes(attributes);
	}


	/*
	 * Retrieves a list of projects visible to the logged-in business member
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		String projectId = req.getParameter(REQ_PROJECT_ID);
		String page = req.getParameter("page");

		loadPrefilter(req, mod);

		List<ProjectVO> data = loadProjectList(req, projectId);
		populateUserProfiles(data);

		mod.setActionData(data);
		setAttribute(Constants.MODULE_DATA, mod);

		//if we're looking at a single project, load additional pieces depending on which tab is being displayed
		if (!StringUtil.isEmpty(projectId)) {
			loadTabSpecifics(mod, req, page, projectId, data);

		} else {
			calculateValuation(mod, data);
		}
	}


	/**
	 * Calculate total valuation of all projects
	 * @param mod
	 * @param data
	 */
	protected void calculateValuation(ModuleVO mod, List<ProjectVO> data) {
		double total = 0;
		double improvementTotal = 0;
		for (ProjectVO proj : data) {
			total += proj.getTotalNo();
			if ("IMPROVEMENT".equals(proj.getProjectCategoryCd()))
				improvementTotal += proj.getTotalNo();
		}
		mod.setAttribute("totalValue", total);
		mod.setAttribute("totalValueImprovements", improvementTotal * RezDoxUtils.IMPROVEMENTS_VALUE_COEF);
	}

	/**
	 * The prefilter is the dropdown at the top of the list page.  For business users its a list of businesses (this action),
	 * for HomeHistory its a list of residences - for which this method is overwritten in HomeHistoryAction.
	 * @param req
	 * @param mod
	 */
	protected void loadPrefilter(ActionRequest req, ModuleVO mod) {
		//load a list of businesses.  If there's only one, then choose the 1st as the default if one wasn't provided.
		List<BusinessVO> bizList = new BusinessAction(getDBConnection(), getAttributes()).loadBusinessList(req);
		if (!req.hasParameter(BusinessAction.REQ_BUSINESS_ID) && !bizList.isEmpty())
			req.setParameter(BusinessAction.REQ_BUSINESS_ID, bizList.get(0).getBusinessId());

		//make sure session is loaded - this gets used by our <select> list loaders (MyResidences)
		String sesBizId = StringUtil.checkVal(req.getSession().getAttribute(BusinessAction.REQ_BUSINESS_ID));
		if (!sesBizId.equals(req.getParameter(BusinessAction.REQ_BUSINESS_ID)))
			req.getSession().setAttribute(BusinessAction.REQ_BUSINESS_ID, req.getParameter(BusinessAction.REQ_BUSINESS_ID));

		log.debug(String.format("loaded %d businesses", bizList.size()));
		mod.setAttribute(FILTER_DATA_LST, bizList);
	}


	/**
	 * Fork logic to load data needed for specific tab displays
	 * @param mod
	 * @param req
	 * @param page
	 * @param projectId
	 * @param data
	 * @throws ActionException
	 */
	protected void loadTabSpecifics(ModuleVO mod, ActionRequest req, String page, 
			String projectId, List<ProjectVO> data) throws ActionException {
		boolean isInvoice = "invoice".equals(page);
		if ("edit".equals(page) || "view".equals(page) || isInvoice) {
			loadProjectDetails(mod, req, projectId, data, isInvoice);

		} else if ("materials".equals(page)) {
			mod.setAttribute("projects", mod.getActionData());
			//load the project materials
			loadMaterials(req);
		}

		//photos are used in two tabs
		if ("files".equals(page) || "view".equals(page)) {
			//load the photos/documents
			mod.setAttribute("photos", loadPhotos(req));
		}
	}


	/**
	 * populates the project details needed for View
	 * @param mod
	 * @param req
	 * @param projectId
	 * @param data
	 * @param isInvoice
	 */
	@SuppressWarnings("unchecked")
	private void loadProjectDetails(ModuleVO mod, ActionRequest req, 
			String projectId, List<ProjectVO> data, boolean isInvoice) {
		//load the form if we're in edit mode
		DataContainer dc = loadForm(req);
		mod.setAttribute("dataContainer", dc);

		if (data.size() == 1) {
			//move the form data over to the ProjectVO for transparency in View display
			ProjectVO vo = data.get(0);
			FormTransactionVO trans = dc.getTransactions().get(projectId);
			if (trans == null) trans = new FormTransactionVO();
			for (FormFieldVO ff: trans.getCustomData().values())
				vo.addAttribute(ff.getSlugTxt(), ff.getResponseText());

			if (isInvoice) {
				//load the project materials
				vo.setMaterials(loadInvoiceMaterials(req));
				vo.setBusiness(getBusiness((List<BusinessVO>)mod.getAttribute(FILTER_DATA_LST), vo.getBusinessId()));
			}
		}
	}

	/**
	 * loads the business tied to this project for generating their invoice.
	 * @param req
	 * @return
	 */
	private BusinessVO getBusiness(List<BusinessVO> data, String businessId) {
		if (data == null || data.isEmpty()) return null;
		for (BusinessVO vo : data) {
			if (vo.getBusinessId().equals(businessId))
				return vo;
		}
		return null;
	}

	/**
	 * load the list of projects to display - or one if we're in edit/view mode
	 * @param req
	 * @param projectId
	 * @return
	 */
	protected List<ProjectVO> loadProjectList(ActionRequest req, String projectId) {
		String schema = getCustomSchema();
		List<Object> params = new ArrayList<>();

		StringBuilder sql = new StringBuilder(1000);
		sql.append("select a.*, b.attribute_id, b.slug_txt, b.value_txt, c.category_nm, d.type_nm, ");
		sql.append("r.residence_nm, rr.room_nm, m.member_id, m.profile_id as homeowner_profile_id ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_ATTRIBUTE b on a.project_id=b.project_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_CATEGORY c on a.project_category_cd=c.project_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_TYPE d on a.project_type_cd=d.project_type_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE r on a.residence_id=r.residence_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on r.residence_id=rm.residence_id and rm.status_flg=1 ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_MEMBER m on rm.member_id=m.member_id "); //this is the home owner
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_ROOM rr on a.room_id=rr.room_id ");
		sql.append("where a.business_view_flg=1 ");

		if (!StringUtil.isEmpty(projectId)) {
			sql.append("and a.project_id=? ");
			params.add(projectId);
		} else { //list page - filter by businessId
			sql.append("and a.business_id=? ");
			params.add(req.getParameter(BusinessAction.REQ_BUSINESS_ID));
		}
		sql.append("order by a.end_dt desc, a.project_nm");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), params, new ProjectVO());
	}


	/**
	 * populate the UserDataVO for each homeowner for the loaded projects
	 * @param data
	 */
	protected void populateUserProfiles(List<ProjectVO> projects) {
		ProfileManager pm = ProfileManagerFactory.getInstance(getAttributes());
		Map<String, UserDataVO> userMap = new HashMap<>(projects.size());
		UserDataVO user;
		for (ProjectVO vo : projects) {
			user = vo.getHomeowner();
			if (user != null && !StringUtil.isEmpty(user.getProfileId()))
				userMap.put(user.getProfileId(), user);
		}
		if (userMap.isEmpty()) return;


		List<UserDataVO> users = new ArrayList<>(userMap.values());
		try {
			pm.populateRecords(getDBConnection(), users);
		} catch (DatabaseException e) {
			log.error("could not load user profiles");
			return;
		}

		//turn the list into a Map for fast lookups
		userMap = users.stream().collect(Collectors.toMap(UserDataVO::getProfileId, Function.identity()));

		//marry the users back to their project
		for (ProjectVO vo : projects) {
			user = vo.getHomeowner();
			if (user == null || StringUtil.isEmpty(user.getProfileId())) continue;

			vo.setHomeowner(userMap.get(user.getProfileId()));
		}
	}


	/**
	 * @param req
	 * @return
	 */
	private DataContainer loadForm(ActionRequest req) {
		String formId = RezDoxUtils.getFormId(getAttributes());
		log.debug("Retrieving Project Form: " + formId);

		// Set the requried params
		QueryParamVO param = new QueryParamVO("PROJECT_ID", Boolean.FALSE);
		param.setValues(req.getParameterValues(REQ_PROJECT_ID));
		GenericQueryVO query = new GenericQueryVO(formId);
		query.addConditional(param);

		// Get the form and the saved data for re-display onto the form.
		DataManagerUtil util = new DataManagerUtil(getAttributes(), getDBConnection());
		return util.loadFormWithData(formId, req, query, ProjectFormProcessor.class);
	}


	/**
	 * load a list of photos tied to this treasure box item
	 * @param vo
	 * @param req
	 * @return 
	 */
	private List<PhotoVO> loadPhotos(ActionRequest req) {
		return new PhotoAction(getDBConnection(), getAttributes()).retrievePhotos(req);
	}


	/**
	 * load a list of photos tied to this treasure box item
	 * @param vo
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private void savePhoto(ActionRequest req) throws ActionException {
		new PhotoAction(getDBConnection(), getAttributes()).build(req);
	}


	/**
	 * load a list of materials tied to this project
	 * @param vo
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private void loadMaterials(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		mod.setAttribute(ModuleVO.ATTRIBUTE_1, mod.getAttribute(ModuleVO.ATTRIBUTE_2)); //transpose form2 into slot 1
		setAttribute(Constants.ACTION_DATA, getActionInit());
		new ProjectMaterialAction(getDBConnection(), getAttributes()).retrieve(req);
	}


	/**
	 * load a list of materials tied to this project
	 * @param vo
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private List<ProjectMaterialVO> loadInvoiceMaterials(ActionRequest req) {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		mod.setAttribute(ModuleVO.ATTRIBUTE_1, mod.getAttribute(ModuleVO.ATTRIBUTE_2)); //transpose form2 into slot 1
		setAttribute(Constants.ACTION_DATA, getActionInit());
		return new ProjectMaterialAction(getDBConnection(), getAttributes()).retrieveMaterials(req);
	}


	/**
	 * save materials tied to this project
	 * @param req
	 * @return 
	 * @throws ActionException 
	 */
	private void saveMaterial(ActionRequest req) throws ActionException {
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		mod.setAttribute(ModuleVO.ATTRIBUTE_1, mod.getAttribute(ModuleVO.ATTRIBUTE_2)); //transpose form2 into slot 1
		setAttribute(Constants.ACTION_DATA, getActionInit());
		new ProjectMaterialAction(getDBConnection(), getAttributes()).build(req);
	}


	/*
	 * Saves or creates a project tied to the given residenceId.
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		boolean doRedirect = false;
		String url = "";

		if (req.hasParameter("savePhoto")) {
			savePhoto(req);

		} else if (req.hasParameter("saveMaterial")) {
			saveMaterial(req);

		} else if (req.hasParameter("deleteItem")) {
			hideProject(req);
			doRedirect = true;
			if (req.hasParameter(ResidenceAction.RESIDENCE_ID)) {
				url = StringUtil.join("?", ResidenceAction.RESIDENCE_ID, "=", req.getParameter(ResidenceAction.RESIDENCE_ID));
			} else if (req.hasParameter(BusinessAction.REQ_BUSINESS_ID)) {
				url = StringUtil.join("?", BusinessAction.REQ_BUSINESS_ID, "=", req.getParameter(BusinessAction.REQ_BUSINESS_ID));
			}

		} else if (req.hasParameter("discounts")) {
			saveDiscounts(req);
			doRedirect = true;
			url = StringUtil.join("?page=invoice&projectId=", req.getParameter(REQ_PROJECT_ID));

		} else if (req.hasParameter("makePDF")) {
			streamPDF(req);

		} else {
			// Place ActionInit on the Attributes map for the Data Save Handler.
			setAttribute(Constants.ACTION_DATA, getActionInit());

			// Call DataManagerUtil to save the form.
			String formId = RezDoxUtils.getFormId(getAttributes());
			DataManagerUtil util = new DataManagerUtil(getAttributes(), getDBConnection());
			util.saveForm(formId, req, ProjectFormProcessor.class);
		}

		//redirect the user if the request wasn't made over ajax
		if (doRedirect) {
			PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
			url = page.getFullPath() + url;
			sendRedirect(url, (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE), req);
		}
	}


	/**
	 * Streams the PDF
	 * @param req
	 * @throws ActionException 
	 */
	private void streamPDF(ActionRequest req) throws ActionException {
		retrieve(req);
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		String secBin = StringUtil.join((String)getAttribute(Constants.SEC_BINARY_DIRECTORY), 
				(String)getAttribute(Constants.ORGANIZATION_ALIAS), site.getOrganizationId());

		InvoiceReportPDF report = new InvoiceReportPDF(site.getFullSiteAlias(), secBin);
		report.setData(mod.getActionData());

		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
	}


	/**
	 * Save the4 discount columns to the DB - from the modal window on the Invoices tab
	 * @param req
	 */
	private void saveDiscounts(ActionRequest req) {
		StringBuilder sql = new StringBuilder (150);
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("REZDOX_PROJECT set ");
		sql.append("proj_discount_no=?, proj_tax_no=?, mat_discount_no=?, mat_tax_no=?, update_dt=? where project_id=?");
		log.debug(sql);

		ProjectVO vo = ProjectVO.instanceOf(req);
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setDouble(1, vo.getProjectDiscountNo());
			ps.setDouble(2, vo.getProjectTaxNo());
			ps.setDouble(3, vo.getMaterialDiscountNo());
			ps.setDouble(4, vo.getMaterialTaxNo());
			ps.setTimestamp(5, Convert.getCurrentTimestamp());
			ps.setString(6, vo.getProjectId());
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not save project discounts & taxes", sqle);
		}
	}


	/**
	 * Hides a project from the given user's view.
	 * Projects never get deleted because they're viewable to multiple people - instead we hide them.
	 * Support for isOwner bubbles up from HomeHistoryAction - a subclass.
	 * @param req
	 */
	private void hideProject(ActionRequest req) {
		StringBuilder sql = new StringBuilder (150);
		String column = req.hasParameter("isOwner") ? "residence_view_flg" : "business_view_flg";
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("REZDOX_PROJECT set ");
		sql.append(column).append("=?, update_dt=CURRENT_TIMESTAMP where project_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, Convert.formatInteger(req.getParameter("visibleFlg"), 0));
			ps.setString(2, req.getParameter(REQ_PROJECT_ID));
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not hide project", sqle);
		}
	}


	/**
	 * Saves the Project record (only)
	 * @param req
	 * @throws ActionException 
	 */
	public void save(ActionRequest req) throws ActionException {
		ProjectVO vo = ProjectVO.instanceOf(req);
		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		try {
			if (req.hasParameter("isDelete")) {
				db.delete(vo);
			} else {
				db.save(vo);
				//transpose the primary key
				req.setParameter(REQ_PROJECT_ID, vo.getProjectId());
			}

		} catch (Exception e) {
			throw new ActionException("could not save project", e);
		}
	}
}