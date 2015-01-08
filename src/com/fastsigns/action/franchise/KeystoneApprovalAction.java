package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import com.fastsigns.action.LogAction;
import com.fastsigns.action.approval.ApprovalFacadeAction;
import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.ApprovalVO;
import com.fastsigns.action.approval.vo.CareerLogVO;
import com.fastsigns.action.approval.vo.CenterImageLogVO;
import com.fastsigns.action.approval.vo.ModuleLogVO;
import com.fastsigns.action.approval.vo.PageLogVO;
import com.fastsigns.action.approval.vo.PageModuleLogVO;
import com.fastsigns.action.approval.vo.WhiteBoardLogVO;
import com.fastsigns.action.franchise.centerpage.FranchiseLocationInfoAction;
import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.fastsigns.action.franchise.vo.CenterModuleVO;
import com.fastsigns.action.franchise.vo.FranchiseContainer;
import com.fastsigns.action.franchise.vo.pages.PageContainerVO;
import com.fastsigns.action.vo.CareersVO;
import com.fastsigns.util.ChangeLogReportVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.content.ContentVO;
import com.smt.sitebuilder.action.menu.MenuObj;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: KeystoneApprovalAction.java<p/>
 * <b>Description: </b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 26, 2011
 ****************************************************************************/
public class KeystoneApprovalAction extends SimpleActionAdapter {

	public static final int PAGE_APPROVAL = 1;
	public static final int MODULE_APPROVAL = 2;
	public static final int CENTER_IMAGE_APPROVAL = 3;
	public static final int WHITEBOARD_APPROVAL = 4;
	public static final int CAREER_APPROVAL = 5;
	public static final int PAGE_MODULE_APPROVAL = 6;
	public static final int GET_REPORT = 7;
	public static final String CACHE_GROUP = "FTS_CAREERS";

	public void build(SMTServletRequest req) throws ActionException {
		String msg = "msg.updateSuccess";
		Integer bType = Convert.formatInteger(req.getParameter("bType"));
		log.debug("type=" + bType);
		log.debug(req.getParameter("revComments"));
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		String redir = page.getFullPath() + "?apprFranchiseId=" + req.getParameter("apprFranchiseId");
		boolean takeRedirect = true;
		ApprovalVO avo = new ApprovalVO();
		try {
			switch(bType) {
				case PAGE_APPROVAL:
					avo.setChangeLogList(PageLogVO.TYPE_ID, new PageLogVO(req));
					req.setAttribute("approvalVO", avo);
					this.forwardToApprovalAction(req);
					break;
				case MODULE_APPROVAL:
					avo.setChangeLogList(ModuleLogVO.TYPE_ID, new ModuleLogVO(req));
					req.setAttribute("approvalVO", avo);
					this.forwardToApprovalAction(req);
					break;
				case CENTER_IMAGE_APPROVAL:
					avo.setChangeLogList(CenterImageLogVO.TYPE_ID, new CenterImageLogVO(req));
					req.setAttribute("approvalVO", avo);
					this.forwardToApprovalAction(req);
					break;
				case WHITEBOARD_APPROVAL:
					avo.setChangeLogList(WhiteBoardLogVO.TYPE_ID, new WhiteBoardLogVO(req));
					req.setAttribute("approvalVO", avo);
					this.forwardToApprovalAction(req);
					break;
				case CAREER_APPROVAL:
					if(Convert.formatInteger(req.getParameter("jobApprovalFlg")) == ApprovalFacadeAction.APPROVE)
						super.clearCacheByGroup(CACHE_GROUP);
					avo.setChangeLogList(CareerLogVO.TYPE_ID, new CareerLogVO(req));
					req.setAttribute("approvalVO", avo);
					this.forwardToApprovalAction(req);
					break;
				case PAGE_MODULE_APPROVAL:
					avo.setChangeLogList(PageModuleLogVO.TYPE_ID, new PageModuleLogVO(req));
					req.setAttribute("approvalVO", avo);
					this.forwardToApprovalAction(req);
					break;
				case GET_REPORT:
					LogAction lA = new LogAction(actionInit);
					lA.setDBConnection(dbConn);
					lA.setAttributes(attributes);
					Object vos = lA.getLogs(true, req);
					ChangeLogReportVO clr = new ChangeLogReportVO();
					clr.setData(vos);
					msg = "msg.reportRetr";
					req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
					req.setAttribute(Constants.BINARY_DOCUMENT, clr);
					lA = null;
					takeRedirect = false;
					break;
				}
		} catch (ActionException ae) {
			log.error(ae);
			msg = "msg.cannotUpdate";
		}
		
		//redirect the user if no attachment.
		if(takeRedirect){
			log.debug("Sending Redirect to: " + redir);
			req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
			req.setAttribute(Constants.REDIRECT_URL, redir + "&msg=" + msg);
		}
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		String franchiseId = req.getParameter("apprFranchiseId");
		if (AdminConstants.REQ_LIST.equalsIgnoreCase(req.getParameter(AdminConstants.REQUEST_TYPE))) {
			super.retrieve(req);
		} else if  (franchiseId == null || franchiseId.length() == 0) {
			getApprovableFranchises(req); 
		}  else  {
			getApprovalsByCenter(req, franchiseId);
		}
	}

	/**
	 * Get all the franchises that have approvable content.
	 * @param req
	 */
	private void getApprovableFranchises(SMTServletRequest req) {
		SiteVO siteData = (SiteVO) req.getAttribute(Constants.SITE_DATA);
		if (siteData == null) return;
		String orgId = siteData.getOrganizationId();
		String countryCd = siteData.getCountryCode();
		
		PreparedStatement ps = null;
		Set<String> approvalNeeded = new TreeSet<String>();
		try {
			ps = dbConn.prepareStatement(buildFranchiseQuery());
			ps.setString(1, countryCd);
			ps.setString(2, countryCd);
			ps.setString(3, orgId);
			ps.setString(4, orgId);
			
			ResultSet rs = ps.executeQuery();
			String siteId;
			while (rs.next()) {
				log.debug(rs.getString(2));
				switch(rs.getInt(1)) {
					case 1: 
					case 3:
					case 4:
					case 5:
						approvalNeeded.add(rs.getString(2));
						break;
					case 2:
						siteId = rs.getString(2);
						siteId = rs.getString(2).substring(0, siteId.lastIndexOf('_')).replace(orgId, "");
						approvalNeeded.add(siteId);
						break;
				}
			}
		} catch (SQLException e) {
			log.error("Could not get list of franchises with items needing approval. ", e);
		}
		putModuleData(approvalNeeded);
	}
	
	/**
	 * Create the query that gathers all centers that have modules, whiteboard changes, sub-pages,
	 * center images, asset updates, and job postings that are awaiting approval right now.
	 * @return
	 */
	private String buildFranchiseQuery() {
		String customDb = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA);	
		StringBuilder sql = new StringBuilder();
		
		// Get the new whiteboard and center images
		sql.append("SELECT 1 as QUERY, FRANCHISE_ID, null as OPTION_FRANCHISE_ID FROM ");
		sql.append(customDb).append("FTS_FRANCHISE ");
		sql.append("WHERE (NEW_CENTER_IMAGE_URL is not null or NEW_WHITE_BOARD_TEXT is not null) ");
		sql.append("AND COUNTRY_CD = ? ");
		
		sql.append("union ");
		
		// Get the subpages
		sql.append("SELECT distinct 2 as QUERY, s.SITE_ID, null as OPTION_FRANCHISE_ID FROM ");
		sql.append(customDb).append("FTS_CHANGELOG fc ");
		sql.append("inner join PAGE_MODULE pm on fc.COMPONENT_ID = pm.PAGE_MODULE_ID ");
		sql.append("inner join PAGE p on p.PAGE_ID = pm.PAGE_ID ");
		sql.append("inner join SITE s on p.SITE_ID = s.SITE_ID ");
		sql.append("WHERE TYPE_ID = 'pageModule' and STATUS_NO = 0 and s.COUNTRY_CD = ? ");
		
		sql.append("union ");
		
		// Get the job postings
		sql.append("SELECT distinct 3 as QUERY, FRANCHISE_ID, null as OPTION_FRANCHISE_ID FROM ");
		sql.append(customDb).append("FTS_JOB_POSTING ");
		sql.append("WHERE JOB_APPROVAL_FLG = 0 and ORGANIZATION_ID = ? and len(FRANCHISE_ID) > 0 ");
		
		sql.append("union ");
		
		// Get the modules and module assets 
		sql.append("SELECT distinct 4 as QUERY, lmx.FRANCHISE_ID, cmo.FRANCHISE_ID as OPTION_FRANCHISE_ID FROM ");
		sql.append(customDb).append("FTS_CP_MODULE_OPTION cmo ");
		sql.append("left join ").append(customDb).append("FTS_CP_MODULE_FRANCHISE_XR cmfx on ");
		sql.append("cmo.CP_MODULE_OPTION_ID = cmfx.CP_MODULE_OPTION_ID or cmo.PARENT_ID = cmfx.CP_MODULE_OPTION_ID ");
		sql.append("left join ").append(customDb).append("FTS_CP_LOCATION_MODULE_XR lmx on ");
		sql.append("lmx.CP_LOCATION_MODULE_XR_ID = cmfx.CP_LOCATION_MODULE_XR_ID ");
		sql.append("WHERE cmo.APPROVAL_FLG = 100 and cmo.ORG_ID = ? and lmx.FRANCHISE_ID is not null ");
		
		sql.append("union ");
		
		// Get the global modules
		sql.append("SELECT distinct 5 as QUERY, f.FRANCHISE_ID, null ");
		sql.append("FROM ").append(customDb).append("FTS_FRANCHISE f ");
		sql.append("inner join ").append(customDb).append("FTS_CP_MODULE_OPTION cmo on f.USE_GLOBAL_MODULES_FLG * -1 = cmo.FRANCHISE_ID ");
		sql.append("WHERE APPROVAL_FLG = 100 ");
		log.debug(sql);
		
		return sql.toString();
	}

	/**
	 * Get all the approvable items related to a franchise
	 * @param req
	 * @throws ActionException
	 */
	@SuppressWarnings("unchecked")
	private void getApprovalsByCenter(SMTServletRequest req, String franchiseId) throws ActionException {
		
		log.debug("retrieving");
		req.setAttribute("isKeystone", true); //passes through to CenterPageAction
		Map<Object, Object> data = new HashMap<Object, Object>();
		Map<PageVO, Object> pageMap = new LinkedHashMap<PageVO, Object>();
		Boolean isPreview = Convert.formatBoolean(req.getParameter("preview"));
		boolean isHomepage = Convert.formatBoolean(req.getParameter("isHome"));
		
		//initially check for jobs if we are in approval mode.
		if(!isHomepage){
			List<CareersVO> pendingJobs = addJobApprovals(req, isPreview);
			if(pendingJobs.size() > 0)
				data.put("jobs", pendingJobs);
		}
		
		//load ALL the pages for this website (this does not load page_roles!)
		List<MenuObj> pages = this.loadPages(req, franchiseId);
		FranchiseContainer fc = new FranchiseContainer();
		
		Date today = Calendar.getInstance().getTime();
		for (MenuObj p : pages) {
			if (p.isDefaultPage()) {
				
				//load the CP_MODULES that need to be approved
				FranchiseLocationInfoAction fla = new FranchiseLocationInfoAction(actionInit);
				fla.setDBConnection(dbConn);
				fla.setAttributes(attributes);
				fc.setFranchiseLocation(fla.getLocationInfo(franchiseId, true));
				
				// Get the module information for a particular franchise
				CenterPageAction cpa = new CenterPageAction(actionInit);
				cpa.setDBConnection(dbConn);
				cpa.setAttributes(attributes);
				req.setAttribute(Constants.PAGE_PREVIEW, true);
				Map<String, CenterModuleVO> ctrMod = cpa.getModuleData(franchiseId, req, fc.getFranchiseLocation().getUseGlobalMod());
				req.setAttribute(Constants.PAGE_PREVIEW, false);
				if (isPreview && isHomepage) fc.setModuleData(ctrMod);
				
				cpa = null;
				boolean pageApproval= false, needsApproval = false;
				for (String key : ctrMod.keySet()) {
					needsApproval = false;
					CenterModuleVO modLoc = ctrMod.get(key);

					Map<Integer, CenterModuleOptionVO> modOpts = new HashMap<Integer, CenterModuleOptionVO>();
					for (CenterModuleOptionVO vo : modLoc.getModuleOptions().values()) {
						if (vo.getApprovalFlag() == 100 || isPreview) {
							needsApproval = true;
							modOpts.put(vo.getModuleOptionId(), vo);
							log.debug("added option " + vo.getOptionName() + " for " + key);
						}
					}					
					if (needsApproval) {
						log.debug("modOptsSize=" + modLoc.getModuleOptions().size());
						modLoc.setModuleOptions(modOpts);
						pageApproval = true;
					} else {
						modLoc.setModuleOptions(null);
					}
					
					ctrMod.put(key, modLoc);
				}
				
				//does the Center Image need approval?
				if (fc.getFranchiseLocation().isPendingImgChange()) {
					req.setAttribute("approveImage", true);
					pageApproval = true;
					//needsApproval = true;
				}
				if (fc.getFranchiseLocation().isPendingWbChange()) {
					req.setAttribute("approveWb", true);
					pageApproval = true;
					//needsApproval = true;
				}
				
				log.debug("homepage needs approval? " + needsApproval);
				if (pageApproval) pageMap.put(p, ctrMod);
				
			} else if ((p.getStartDate() != null && p.getStartDate().after(today)) || p.isFooterFlag()) {
				if(((Map<String, ContentVO>)req.getAttribute("contents")).containsKey(p.getPageId())){
					log.debug("site page content needs approval: " + p.isFooterFlag());
					//SitePage content needs approval
					pageMap.put(p, ((Map<String, ContentVO>)req.getAttribute("contents")).get(p.getPageId()));
				} else {
				log.debug("site page needs approval: " + p.isFooterFlag());
				pageMap.put(p, null); //SitePage needs approval, but has no CP_MODULES to worry about.
				//since custom pages only have a single CONTENT Module on them, 
				//we'll treat the PAGE approval as approval of both the Page and the Content
				}
			} else {
				log.debug("removed page " + p.getDisplayName());
			}
			
		}
		
		//structure the data how it's used on the public site, so we can re-use those JSPs for previews
		if (isPreview && isHomepage) {
			putModuleData(fc);
		} else {
			if(pageMap.size() > 0)
				data.put("pages", pageMap);
			putModuleData(data);
		}
	}
	
	private List<MenuObj> loadPages(SMTServletRequest req, String franchiseId) 
	throws ActionException {
		//load a list of Site Pages for this Franchise
		FranchisePageAction  fpe = new FranchisePageAction(actionInit);
		fpe.setDBConnection(dbConn);
		fpe.setAttributes(attributes);
		fpe.retrieve(req);
		
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		PageContainerVO pc = (PageContainerVO) mod.getActionData();
		
		//merge all the Site's pages into a single Collection
		List<MenuObj> pages = new ArrayList<MenuObj>();
		List<MenuObj> retPages = new ArrayList<MenuObj>();
		Map<String, ContentVO> content = new HashMap<String, ContentVO>();
		pages.addAll(pc.getSecPages());
		pages.addAll(pc.getMenuPages());
		
		for(PageVO p : pages){
			ContentVO c = fpe.getContent(p, null, pc.getSharedId());
			if(c.getAttribute("isPublic") == null)
				content.put(p.getPageId(), c);
		}
		fpe = null;
		
		if(content.size() > 0){
			req.setAttribute("contents", content);
		}

		for(MenuObj pg : pages){
			//the year 2200 is what we use to show a page is pending
			if(pg.getStartDate().toString().contains("2200") || pg.isDefaultPage() || content.get(pg.getPageId()) != null){
				retPages.add(pg);
			}
		}
		return retPages;
	}
		
	/**
	 * Creates approval action and calls action.  
	 * @param req
	 * @throws ActionException
	 */
	private void forwardToApprovalAction(SMTServletRequest req) throws ActionException{
		ApprovalFacadeAction aA = new ApprovalFacadeAction(actionInit);
		aA.setDBConnection(dbConn);
		aA.setAttributes(attributes);
		aA.update(req);
		aA = null;
	}
	
	@SuppressWarnings("unchecked")
	private List<CareersVO> addJobApprovals(SMTServletRequest req, boolean isPreview) throws ActionException{
		SBActionAdapter kca = new KeyStoneCareersAction(this.actionInit);
		kca.setDBConnection(dbConn);
		kca.setAttributes(attributes);
		kca.retrieve(req);
		ModuleVO mod = (ModuleVO) attributes.get(Constants.MODULE_DATA);
		List<CareersVO> vos = (List<CareersVO>) mod.getActionData();
		List<CareersVO> retVos = new ArrayList<CareersVO>();
		if(vos != null && vos.size() > 0 && !isPreview){
			for(CareersVO c : vos){
				if(c != null && c.getJobApprovalFlg() == AbstractChangeLogVO.Status.PENDING.ordinal())
					retVos.add(c);
			}
			return retVos;
		} else
		 return vos;
	}
	
}
