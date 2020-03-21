package com.rezdox.action;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.rezdox.action.RewardsAction.Reward;
import com.rezdox.action.RezDoxNotifier.Message;
import com.rezdox.data.InvoiceReportPDF;
import com.rezdox.data.ProjectFormProcessor;
import com.rezdox.util.ValuationCoefficientUtil;
import com.rezdox.vo.BusinessVO;
import com.rezdox.vo.MemberVO;
import com.rezdox.vo.PhotoVO;
import com.rezdox.vo.ProjectMaterialVO;
import com.rezdox.vo.ProjectVO;
import com.rezdox.vo.ResidenceVO;
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
			//set a marker on the request to signal whether the invoice tab should be displayed to homeowners.
			if (!data.isEmpty()) {
				ProjectVO proj = data.get(0);
				boolean isSharedProj = proj.getResidenceViewFlg() == 1 && proj.getBusinessViewFlg() == 1;
				req.setAttribute("isSharedProj", isSharedProj);
			}

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
			total += proj.getInvoiceSubTotal();
			if ("IMPROVEMENT".equals(proj.getProjectCategoryCd())) {
				improvementTotal += calculateProjectValuation(proj);
			}	
		}
		mod.setAttribute("totalValue", total);
		mod.setAttribute("totalValueImprovements", improvementTotal);
	}
	
	/**
	 * takes all the projects and returns the project valuation total
	 * @param data
	 * @return
	 */
	public double calculateTotalProjectValuation( List<ProjectVO> data) {
		double improvementTotal = 0;
		for (ProjectVO proj : data) {
			
			processMaterials(proj);
			
			if ("IMPROVEMENT".equals(proj.getProjectCategoryCd())) {
				improvementTotal += calculateProjectValuation(proj);
			}	
		}
		log.debug("project total valuation: "  + improvementTotal);

		return improvementTotal;
	}
	
	
	/**
	 * sets each project materials cost and returns the materials total cost
	 * @param proj
	 */
	private double processMaterials(ProjectVO proj) {
		double meterialTotal = 0.0;
		if(proj.getMaterials() != null) {
			
			for (ProjectMaterialVO mrt : proj.getMaterials()) {
				if(proj.getProjectId().equals(mrt.getProjectId())) {
					meterialTotal += mrt.getCostNo() * mrt.getQuantityNo();
				}
			}
			proj.setMaterialSubtotal(meterialTotal);
		}
		
		return meterialTotal;
	}

	/**
	 * takes all the projects and returns the project total cost
	 * @param data
	 * @return
	 */
	public double calculateTotalProjectValue( List<ProjectVO> data) {
		double total = 0;
		
		for (ProjectVO proj : data) {		
			total += proj.getInvoiceSubTotal()+ processMaterials(proj);	
		}
		return total;
	}
	
	/**
	 * gets a lits of projects
	 * @param ResId
	 * @return
	 */
	public List<ProjectVO> getProjectsByResId(String resId){
		
		String schema = getCustomSchema();
		List<Object> vals = new ArrayList<>();

		StringBuilder sql = new StringBuilder(173);
		
		sql.append(DBUtil.SELECT_FROM_STAR).append(schema).append("rezdox_project p ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("rezdox_project_material pm  on p.project_id = pm.project_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("residence_id = ? and p.residence_view_flg = '1' ");
		vals.add(resId);
		
		log.debug("sql " + sql.toString()+ "|" +vals);
		 
		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		return db.executeSelect(sql.toString(), vals, new ProjectVO());
	}
	
	/**
	 * a utility method that calculates a project valuation 
	 * @param pvo
	 * @return
	 */
	public double calculateProjectValuation(ProjectVO pvo) {
			//dont apply any discounts or tax to a valuation total
		return (pvo.getTotalNo() + pvo.getMaterialSubtotal() ) * ValuationCoefficientUtil.getValueCoefficient(pvo.getEndDate());
	}

	/**
	 * The prefilter is the dropdown at the top of the list page.  For business users its a list of businesses (this action),
	 * for HomeHistory its a list of residences - for which this method is overwritten in HomeHistoryAction.
	 * @param req
	 * @param mod
	 */
	protected void loadPrefilter(ActionRequest req, ModuleVO mod) {
		String bizId = StringUtil.checkVal(req.getParameter(BusinessAction.REQ_BUSINESS_ID), 
				(String)req.getSession().getAttribute(BusinessAction.REQ_BUSINESS_ID));

		//load a list of businesses.  If there's only one, then choose the 1st as the default if one wasn't provided.
		List<BusinessVO> bizList = new BusinessAction(getDBConnection(), getAttributes()).loadBusinessList(req);
		if (bizList == null || bizList.isEmpty()) return;

		if (StringUtil.isEmpty(bizId)) {
			bizId = bizList.get(0).getBusinessId();
		}

		//make sure session is loaded - this gets used by our <select> list loaders (MyBusinesses)
		if (!bizId.equals(req.getSession().getAttribute(BusinessAction.REQ_BUSINESS_ID)))
			req.getSession().setAttribute(BusinessAction.REQ_BUSINESS_ID, bizId);

		//always make sure a healthy value is on the request, for JSPs
		req.setParameter(BusinessAction.REQ_BUSINESS_ID, bizId);

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
				if (vo.getBusiness() == null)
					vo.setBusiness(getBusiness((List<BusinessVO>)mod.getAttribute(FILTER_DATA_LST), vo.getBusinessId()));
			}
		}
	}

	/**
	 * loads the business tied to this project for generating their invoice.
	 * @param req
	 * @return
	 */
	protected BusinessVO getBusiness(List<BusinessVO> data, String businessId) {
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
		sql.append("r.residence_nm, rr.room_nm, m.member_id, m.profile_id as homeowner_profile_id, pc.material_cost, pc.project_valuation, ");
		sql.append("cast(sum(case when ph.photo_id is not null then 1 else 0 end) as integer) as photo_cnt ");
		sql.append(DBUtil.FROM_CLAUSE).append(schema).append("REZDOX_PROJECT a ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_ATTRIBUTE b on a.project_id=b.project_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_CATEGORY c on a.project_category_cd=c.project_category_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PROJECT_TYPE d on a.project_type_cd=d.project_type_cd ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE r on a.residence_id=r.residence_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_RESIDENCE_MEMBER_XR rm on r.residence_id=rm.residence_id and rm.status_flg=1 ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_MEMBER m on rm.member_id=m.member_id "); //this is the home owner
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_ROOM rr on a.room_id=rr.room_id ");		
		
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(" ( ");
		sql.append("SELECT residence_id, business_id, project_id, residence_view_flg, business_view_flg, create_dt, ");
		sql.append("sum(project_cost) as project_cost, sum(material_cost) as material_cost, sum(project_valuation) as project_valuation, is_improvement ");
		sql.append("from ").append(schema).append("rezdox_project_cost_view ");
		sql.append("group by residence_id, business_id, project_id, residence_view_flg, business_view_flg, create_dt, is_improvement ");
		sql.append(" ) as pc on a.project_id=pc.project_id ");
		
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(schema).append("REZDOX_PHOTO ph on a.project_id=ph.project_id ");
		sql.append("where a.business_view_flg != 0 ");// 1=approved, -1=pending

		if (!StringUtil.isEmpty(projectId)) {
			sql.append("and a.project_id=? ");
			params.add(projectId);
		} else { //list page - filter by businessId
			sql.append("and a.business_id=? ");
			params.add(req.getParameter(BusinessAction.REQ_BUSINESS_ID));
		}
		sql.append("group by a.project_id, a.residence_id, a.room_id, a.business_id, a.project_category_cd, a.project_type_cd, a.project_nm, ");
		sql.append("a.labor_no, a.total_no, a.residence_view_flg, a.business_view_flg, a.create_dt, a.update_dt, a.end_dt, a.desc_txt, ");
		sql.append("a.proj_discount_no, a.proj_tax_no, a.mat_discount_no, a.mat_tax_no, pc.material_cost, pc.project_valuation, ");
		sql.append("b.attribute_id, b.slug_txt, b.value_txt, c.category_nm, d.type_nm, ");
		sql.append("r.residence_nm, rr.room_nm, m.member_id, m.profile_id ");
		sql.append("order by a.end_dt desc, a.project_nm");
		log.debug(sql + "|"+params);

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
		} else {
			putModuleData(req.getParameter(REQ_PROJECT_ID));
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
		/*
		 * Use the DISK path to /secBinary, which aligns with a file:// protocol in the PDF rendered.
		 * This skates permission issues, /secBinary is secure and the renderer cannot forward the user's credentials.
		 * JM - 7.19.18 - REZDOX-270
		 */
		String secBin = StringUtil.join((String)getAttribute(Constants.SBINARY_PATH), 
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
		boolean isOwner = req.hasParameter("isOwner");
		int visibleFlg = Convert.formatInteger(req.getParameter("visibleFlg"), 0).intValue();

		StringBuilder sql = new StringBuilder (150);
		String column = isOwner ? "residence_view_flg" : "business_view_flg";
		sql.append(DBUtil.UPDATE_CLAUSE).append(getCustomSchema()).append("REZDOX_PROJECT set ");
		sql.append(column).append("=?, update_dt=CURRENT_TIMESTAMP where project_id=?");
		log.debug(sql);

		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setInt(1, visibleFlg);
			ps.setString(2, req.getParameter(REQ_PROJECT_ID));
			ps.executeUpdate();

		} catch (SQLException sqle) {
			log.error("could not hide project", sqle);
		}

		//send email & notifications
		if (visibleFlg == 1 && isOwner) {
			sendEmail(RezDoxUtils.EmailSlug.PROJ_ACCPT_BUSINESS, req); // send to biz, owner accepted share from biz
			awardPoints(true, RezDoxUtils.getMemberId(req), Reward.NEW_PROJ_RES, req); //reward the homeowner for accepting this project share onto their HHL.
		} else if (visibleFlg == 1) {
			sendEmail(RezDoxUtils.EmailSlug.PROJ_ACCPT_HOMEOWNER, req); // send to owner, biz accepted share from owner
			awardPoints(true, RezDoxUtils.getMemberId(req), Reward.NEW_PROJ_BUS, req); //reward the business owner for accepting this project share onto their project list.
		}
	}


	/**
	 * proxies off to the share action to deliver a specific email msg/workflow.
	 * @param slugTxt
	 * @param req
	 */
	protected void sendEmail(RezDoxUtils.EmailSlug slugTxt, ActionRequest req) {
		ProjectShareAction psa = new ProjectShareAction(getDBConnection(), getAttributes());
		psa.sendEmail(slugTxt, req);
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
				boolean isNew = StringUtil.isEmpty(vo.getProjectId());
				db.save(vo);
				//transpose the primary key
				req.setParameter(REQ_PROJECT_ID, vo.getProjectId());

				if (req.hasParameter("isHomeowner")) { //HomeHistoryAction - alerts to go the business
					notifyBiz(req, vo);
					awardPoints(isNew, RezDoxUtils.getMemberId(req), Reward.NEW_PROJ_RES, req);

				} else { //this action - alerts go to the homeowner
					notifyRez(req, vo);
					awardPoints(isNew, RezDoxUtils.getMemberId(req), Reward.NEW_PROJ_BUS, req);
				}
			}

		} catch (Exception e) {
			throw new ActionException("could not save project", e);
		}
	}


	/**
	 * award points to the homeowner who just created an HHL entry
	 * @param isNew
	 * @param memberId
	 * @throws ActionException 
	 */
	private void awardPoints(boolean isNewOrShare, String memberId, Reward reward, ActionRequest req) {
		if (!isNewOrShare) return;
		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		try {
			ra.applyReward(reward.name(), memberId, req);
		} catch (ActionException e) {
			log.error("could not award reward points", e);
		}
	}

	/**
	 * If this was an add, from a business to a linked residence (view=pending), 
	 * 	then notify those members of the new home history log (entry).  Also send 
	 * if the project changed from an unlinked to a linked residence, send the notifications.
	 * 
	 * Send similar messages to business contacts; when a member creates a project linked to their business.
	 * @param req
	 * @param vo
	 */
	protected void notifyRez(ActionRequest req, ProjectVO vo) {
		if (StringUtil.isEmpty(vo.getResidenceId())) return; //no connection, no notifications

		//verify the residence is different from the last save-point.  If not don't re-send stuff.
		String lastResId = req.getParameter("prevResidenceId");
		if (vo.getResidenceId().equals(lastResId)) return;

		alertRez(vo, req);
		sendEmail(RezDoxUtils.EmailSlug.PROJ_SHARE_BUSINESS, req);
	}

	/**
	 * Notify the residence's members of the new home history log (entry) that's pending their approval.
	 * @param vo
	 * @param req
	 */
	protected void alertRez(ProjectVO vo, ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
		String[] profileIds = notifyUtil.getProfileIds(vo.getResidenceId(), true);

		//quit while we're ahead if there's nobody to inform
		if (profileIds == null || profileIds.length == 0) return;

		//get the company name from the BusinessAction
		BusinessAction ba = new BusinessAction(getDBConnection(), getAttributes());
		List<BusinessVO> bizList = ba.retrieveBusinesses(vo.getBusinessId());
		BusinessVO biz = bizList != null && !bizList.isEmpty() ? bizList.get(0) : new BusinessVO();
		Map<String, Object> params = new HashMap<>();
		params.put("companyName", StringUtil.checkVal(biz.getBusinessName(), "A RezDox Business")); //fallback to something somewhat elegant

		notifyUtil.send(Message.PROJ_SHARE, params, null, profileIds);
	}


	/**
	 * If this was an add, from a business to a linked residence (view=pending), 
	 * 	then notify those members of the new home history log (entry).  Also send 
	 * if the project changed from an unlinked to a linked residence, send the notifications.
	 * 
	 * Send similar messages to business contacts; when a member creates a project linked to their business.
	 * @param req
	 * @param vo
	 */
	protected void notifyBiz(ActionRequest req, ProjectVO vo) {
		if (StringUtil.isEmpty(vo.getBusinessId())) return; //no connection, no notifications

		//verify the business is different from the last save-point.  If not don't re-send stuff.
		String lastBizId = req.getParameter("prevBusinessId");
		if (vo.getBusinessId().equals(lastBizId)) return;

		alertBiz(vo, req);
		sendEmail(RezDoxUtils.EmailSlug.PROJ_SHARE_HOMEOWNER, req);
	}


	/**
	 * Notify the residence's members of the new home history log (entry) that's pending their approval.
	 * @param vo
	 * @param req
	 */
	protected void alertBiz(ProjectVO vo, ActionRequest req) {
		SiteVO site = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		RezDoxNotifier notifyUtil = new RezDoxNotifier(site, getDBConnection(), getCustomSchema());
		String[] profileIds = notifyUtil.getProfileIds(vo.getBusinessId(), false);

		//quit while we're ahead if there's nobody to inform
		if (profileIds == null || profileIds.length == 0) return;

		if (StringUtil.isEmpty(vo.getResidenceName())) {
			String schema = getCustomSchema();
			String sql = StringUtil.join("select residence_nm from ", schema, "rezdox_residence where residence_id=?");
			DBProcessor dbp = new DBProcessor(getDBConnection(), schema);
			List<ResidenceVO> data = dbp.executeSelect(sql, Arrays.asList(vo.getResidenceId()), new ResidenceVO());
			if (data.size() == 1) vo.setResidenceName(data.get(0).getResidenceName());
		}

		MemberVO member = RezDoxUtils.getMember(req);
		Map<String, Object> params = new HashMap<>();
		params.put("firstName", member.getFirstName());
		params.put("lastName",  member.getLastName());
		params.put("residenceName", StringUtil.checkVal(vo.getResidenceName(), "their home")); //fallback to something somewhat elegant

		notifyUtil.send(Message.PROJ_SHARE_TO_BIZ, params, null, profileIds);
	}
}