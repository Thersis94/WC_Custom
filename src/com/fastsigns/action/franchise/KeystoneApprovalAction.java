package com.fastsigns.action.franchise;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
	
	@SuppressWarnings("unchecked")
	public void retrieve(SMTServletRequest req) throws ActionException {
		if (AdminConstants.REQ_LIST.equalsIgnoreCase(req.getParameter(AdminConstants.REQUEST_TYPE))) {
			super.retrieve(req);
			return;
		}
		String franchiseId = req.getParameter("apprFranchiseId");
		if (franchiseId == null || franchiseId.length() == 0) return; //no franchise selected, yet.
		
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
				CenterPageAction cpa = new CenterPageAction(actionInit);
				cpa.setDBConnection(dbConn);
				cpa.setAttributes(attributes);
				FranchiseLocationInfoAction fla = new FranchiseLocationInfoAction(actionInit);
				fla.setDBConnection(dbConn);
				fla.setAttributes(attributes);
				Map<String, CenterModuleVO> ctrMod = cpa.getModuleData(franchiseId, req);
				if (isPreview && isHomepage) fc.setModuleData(ctrMod);
				fc.setFranchiseLocation(fla.getLocationInfo(franchiseId, true));
				
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
