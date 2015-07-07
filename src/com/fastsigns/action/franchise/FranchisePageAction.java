package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.approval.WebeditApprover;
import com.fastsigns.action.approval.WebeditApprover.WebeditType;
import com.fastsigns.action.franchise.centerpage.FranchiseLocationInfoAction;
import com.fastsigns.action.franchise.vo.CenterModuleOptionVO;
import com.fastsigns.action.franchise.vo.FranchiseVO;
import com.fastsigns.action.franchise.vo.pages.PageContainerVO;
import com.fastsigns.action.wizard.SiteWizardAction;
import com.fastsigns.action.wizard.SiteWizardFactoryAction;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.SMTActionInterface;
import com.siliconmtn.html.tool.RegexParser;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.content.ContentAction;
import com.smt.sitebuilder.action.content.ContentVO;
import com.smt.sitebuilder.action.menu.MenuObj;
import com.smt.sitebuilder.admin.action.PageModuleAction;
import com.smt.sitebuilder.admin.action.SitePageAction;
import com.smt.sitebuilder.approval.ApprovalController;
import com.smt.sitebuilder.approval.ApprovalController.ModuleType;
import com.smt.sitebuilder.approval.ApprovalController.SyncTransaction;
import com.smt.sitebuilder.approval.ApprovalDecoratorAction;
import com.smt.sitebuilder.approval.ApprovalController.SyncStatus;
import com.smt.sitebuilder.approval.ApprovalException;
import com.smt.sitebuilder.approval.ApprovalVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageParser;
import com.smt.sitebuilder.util.RecordDuplicatorUtility;

/****************************************************************************
 * <b>Title</b>: FranchisePageAction.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Dec 27, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FranchisePageAction extends SBActionAdapter {

	public static final int ADD_PAGE = 1;
	public static final int EDIT_PAGE_COPY = 5;
	public static final int EDIT_PAGE_FILE = 6;
	public static final int REORDER_PAGE = 7;
	public static final int ARCHIVE_PAGE = 9;
	public static final int DEL_PAGE = 10;
	public static final int SUBMIT_PAGE_MODULE = 12;
	public static final int SUBMIT_PAGE = 13;
	
	public FranchisePageAction() {
		
	}

	public FranchisePageAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#delete(com.siliconmtn.http.SMTServletRequest)
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		String pageId = req.getParameter("pageId");
        String sbActionId = "";
		String[] sql = {"select action_id from page_module where module_display_id=? and page_id=?",
						"delete from content where action_id=?", 
						"delete from file_gallery where action_id=?",
						"delete from sb_action where action_id=?"};
		PreparedStatement ps = null;
		try {
			log.debug(sql[0] + " " + req.getParameter("moduleDisplayId"));
			ps = dbConn.prepareStatement(sql[0]);
			ps.setString(1, req.getParameter("moduleDisplayId"));
			ps.setString(2, pageId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) sbActionId = rs.getString(1);
			ps.close();
			
			for(int i = 1; i < sql.length; i++){
			//delete from Action Framework
			log.debug(sql[i] + " " + sbActionId);
			ps = dbConn.prepareStatement(sql[i]);
			ps.setString(1, sbActionId);
			ps.executeUpdate();
			ps.close();
			}
		} catch (SQLException sqle) {
			log.error(sqle);
		}
		
		//delete the PAGE, which will cascade PAGE_ROLE, PAGE_MODULE, and PAGE_MODULE_ROLE
		SMTActionInterface ai = new SitePageAction(this.actionInit);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		ai.delete(req);
		//SitePageAction will flush the cache for us.
		// Insert a redirect in order to cut down on 404 errors
		createRedirect(req);
	}
	
	private void createRedirect(SMTServletRequest req) {
		StringBuilder sql = new StringBuilder();
		
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		log.debug(req.getSession().getAttribute("webeditIsMobile"));
		String siteId = orgId + "_" + CenterPageAction.getFranchiseId(req) + "_" + (StringUtil.checkVal(req.getSession().getAttribute("webeditIsMobile")).equals("true") ? 2 : 1);

		
		sql.append("INSERT INTO SITE_REDIRECT (SITE_REDIRECT_ID, SITE_ID, REDIRECT_ALIAS_TXT, ");
		sql.append("DESTINATION_URL, ACTIVE_FLG, CREATE_DT, PERMANENT_REDIR_FLG)");
		sql.append("SELECT ?, SITE_ID, '/'+ALIAS_PATH_NM + ?, '/'+ALIAS_PATH_NM, ?, ?, ? ");
		sql.append("FROM SITE where SITE_ID = ?");

		try {
			PreparedStatement ps = dbConn.prepareStatement(sql.toString());
			
			ps.setString(1, new UUIDGenerator().getUUID());
			ps.setString(2, req.getParameter("pageUrl"));
			ps.setInt(3, 1);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setInt(5, 0);
			ps.setString(6, siteId);
			
			ps.executeUpdate();
			
		} catch (SQLException e) {
			log.error("Site redirect failed when deleting page. ", e );
		}
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.http.SMTServletRequest)
	 */
	public void build(SMTServletRequest req) throws ActionException {
		req.setValidateInput(Boolean.FALSE); //this is a secure action, don't escape HTML from our WYSIWYG!
		int type = Convert.formatInteger(req.getParameter("bType"));
		int lvl = Convert.formatInteger(req.getParameter("lvl"));
		log.debug("type=" + type);

		String msg = "msg.updateSuccess";
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder redir = new StringBuilder(page.getFullPath());
		redir.append("?");
		String orgId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		log.debug(req.getSession().getAttribute("webeditIsMobile"));
		String siteId = orgId + "_" + CenterPageAction.getFranchiseId(req) + "_" + (StringUtil.checkVal(req.getSession().getAttribute("webeditIsMobile")).equals("true") ? 2 : 1);
		try {
			switch (type) {
				case ADD_PAGE:
					boolean noPageModule = false;
					
					//create a new the Site Page
					req.setParameter("startDate", Convert.formatDate(new java.util.Date()));  //new pages must be approved before being released
					
					//we don't need Content or PMID/permissions if this is just a redirect to another resource
					//redirects do not require admin approval, so we can set a valid start/end date for these pages.
					if ((lvl >= 50 || !Convert.formatBoolean(req.getParameter("customPg"))) 
							&& Convert.formatBoolean(req.getParameter("visible")))
					{
						noPageModule = true;
					}
					
					this.savePage(req);
					if("gallery".equals(req.getParameter("pageNm"))){
						log.debug("adding gallery page.");
						redir.append(addPhotoGallery(req));
					}
					if (noPageModule && lvl != 50) 
						break;
					
					//progress the user to the next form, to edit the page they just created.
					redir.append("pageId=").append(req.getAttribute("pageId"));
					redir.append("&lvl=").append(lvl).append("&template=");
					redir.append(req.getParameter("aliasName"));
					redir.append("&selectCol=true");
					break;
					
				case EDIT_PAGE_COPY:

					//Check if the user wants to add a page with no left or right rails
					Boolean showMenu = Convert.formatBoolean(req.getParameter("showMenu"), true);
					
					//check for null so default is not the empty layout
					if (showMenu != null && showMenu.booleanValue() == false){
						req.setParameter("siteId", siteId);
						setEmptyColLayout(req);
					}
					
					this.savePage(req);
					
					if(req.hasParameter("galleryId"))
						updatePhotoGallery(req);
					else
						this.saveModule(req);
					
					if (Convert.formatBoolean(req.getParameter("makeNew")))
						this.savePageModule(req);
					
					break;
				
				case EDIT_PAGE_FILE:
					if (!Convert.formatBoolean(req.getParameter("defaultPage"))) {
						String s = this.saveFile(req);
						if (s != null && s.length() > 0) req.setParameter("externalPageUrl", s);
					}
					
					this.savePage(req);
					break;
				
				case SUBMIT_PAGE:
				case SUBMIT_PAGE_MODULE:
					submitPage(req);
					break;
				
				case REORDER_PAGE:
					this.reorderPages(req);
					break;
				
				case ARCHIVE_PAGE:
					this.archivePage(req);
					break;
					
				case DEL_PAGE:
					this.delete(req);
					break;
			}
			
		} catch (ActionException ae) {
			log.error(ae);
			msg = "msg.cannotUpdate";
		} catch (Exception e) {
			log.error(e);
			msg = "msg.cannotUpdate";
		}
		super.clearCacheByGroup(siteId);
		//finish the redirect
		redir.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, redir.toString());
	}
	
	/**
	 * Submit the page and its content module for approval
	 */
	private void submitPage(SMTServletRequest req) {
		ApprovalController controller = new ApprovalController(dbConn, attributes, req);
		WebeditApprover app = new WebeditApprover(dbConn, getAttributes());
		try {
			List<ApprovalVO> appr = getApprovalVOs(req);
			for(ApprovalVO vo : appr) {
				controller.process(vo);
				if (vo.getModuleType() == ModuleType.Page) {
					app.submit(vo);
				}
			}
		} catch (ApprovalException e) {
			log.error("Unable to get submit items for approval", e);
		}
		
	}

	/**
	 * Get the approval vos for the content and page that are being submitted
	 * This also supports mass submissions from the submit all button
	 */
	private List<ApprovalVO> getApprovalVOs(SMTServletRequest req) throws ApprovalException {
		String[] pages = StringUtil.checkVal(req.getParameter("pagesToSubmit")).split(",");
		String[] modules = StringUtil.checkVal(req.getParameter("modulesToSubmit")).split(",");
		StringBuilder sql = new StringBuilder(60);
		sql.append("SELECT * FROM WC_SYNC WHERE WC_SYNC_ID in (?,?");
		for(String page : pages) 
			if (!"0".equals(page)) sql.append(",?");
		for(String module : modules) 
			if (!"0".equals(module)) sql.append(",?");
		sql.append(")");
			
		List<ApprovalVO> approvals = new ArrayList<>();
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())){
			int i = 1;
			ps.setString(i++, req.getParameter("moduleSyncId"));
			ps.setString(i++, req.getParameter("pageSyncId"));
			for(String page : pages) 
				if (!"0".equals(page)) ps.setString(i++, page);
			for(String module : modules) 
				if (!"0".equals(module)) ps.setString(i++, module);
			
			ResultSet rs = ps.executeQuery();
			
			while(rs.next()) {
				ApprovalVO appr = new ApprovalVO(rs);
				appr.setSyncTransaction(SyncTransaction.Submit);
				appr.setUserDataVo((UserDataVO) req.getSession().getAttribute(Constants.USER_DATA));
				approvals.add(appr);
			}
		} catch(SQLException e) {
			throw new ApprovalException(e);
		}
		return approvals;
	}
		
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String pageId = StringUtil.checkVal(req.getParameter("pageId"));
		String franId = CenterPageAction.getFranchiseId(req);
		attributes.put("webeditFranId", franId);
		
        String previewApiKey = ApprovalController.generatePreviewApiKey(attributes);
        req.setParameter(Constants.PAGE_PREVIEW, previewApiKey);
		
		Map<String, ContentVO> contents = new HashMap<String, ContentVO>();
		// Get the page data
		PageContainerVO pc = this.listPages(req, pageId);
		log.debug("template = " + req.getParameter("template") + " sharedId = " + pc.getSharedId());
		if (pageId.length() > 0) {
			PageVO pageVo = (PageVO) pc.getSelectedPage().getKey();
			pc.setSelectedPageContent(this.getContent(pageVo, req.getParameter("template"), pc.getSharedId()));
			
			//setup session params for the WYSIWYG editor
			KeystoneCenterPageAction cpa = new KeystoneCenterPageAction();
			cpa.polymorphRoles(req);
			cpa = null;
			
		} else {
			//determine which pages have pending content, so we can flag them as such
			//merge all the Site's pages into a single Collection
			List<MenuObj> pages = new ArrayList<MenuObj>();
			pages.addAll(pc.getSecPages());
			pages.addAll(pc.getMenuPages());
			
			//loop the page and determine which ones will require approval on the JSP
			contents = this.getContentSyncRecords(pages);
			for (PageVO p : pages) {
				if (p.getPageId() == null || p.isDefaultPage() || StringUtil.checkVal(p.getExternalPageUrl()).length() > 0) 
					continue; //skip these, they will never require approval!
				
				ContentVO vo = contents.get(p.getPageId());
				if (vo == null) continue; //nothing exist on this page, or nothing is pending
				log.debug("loaded " + p.getDisplayName() + " with Content permissions " + vo.getAttribute("isPublic"));
				if (vo.getActionId() != null) {
					 p.setFooterFlag(true);
				}
				contents.put(p.getPageId(), vo);
				vo = null;
			}
//			List<String> l = new ArrayList<String>();
//
//			for(ContentVO s : contents.values()){
//				l.add((String) s.getAttribute("pmid"));
//			}
			
		}
		req.setAttribute("contents", contents);
		if (Convert.formatBoolean(req.getParameter("preview")))
			req.setAttribute("pageContainer", pc);
		
		// Add the data to the Module Container
		this.putModuleData(pc);
	}
	
	
	/**
	 * loads the sync records for the portlets (Content) attached to each page in our list.
	 * We need these values so we can submit them for approval when we submit the page for approval.
	 * @param pages
	 * @return
	 */
	private Map<String, ContentVO> getContentSyncRecords(List<MenuObj> pages) {
		Map<String, ContentVO> data = new HashMap<>(pages.size());
		StringBuilder s = new StringBuilder(350);
		s.append("select isnull(g.action_id, f.action_id) as action_id, ");
		s.append("isnull(g.action_group_id, f.action_group_id) as action_group_id, a.page_module_id, ");
		s.append("isnull(g.attrib1_txt, f.attrib1_txt) as attrib1_txt, ");
		s.append("isnull(g.attrib2_txt, f.attrib2_txt) as attrib2_txt, ");
		s.append("isnull(g.action_nm,f.action_nm) as action_nm, ");
		s.append("isnull(g.action_desc,f.action_desc) as action_desc, ws.*, a.page_id ");
		s.append("from page_module a ");
		s.append("left join sb_action f on a.action_id = f.action_id "); //the approved record
		s.append("left outer join sb_action g on f.action_group_id = g.action_group_id ");  //the pending record
		s.append("left outer join CONTENT c on ISNULL(g.ACTION_ID, f.ACTION_ID)=c.ACTION_ID ");
		s.append("left join wc_sync ws on (ISNULL(g.ACTION_ID, f.ACTION_ID) = ws.wc_key_id) and wc_sync_status_cd not in (?,?) ");
		s.append("where a.page_id in (''");
		for (@SuppressWarnings("unused") MenuObj menu : pages) s.append(",?");
		s.append(") order by ISNULL(c.update_dt, '1900-06-05T23:59:00') desc "); //ordering by date gets us the most recent revision and ensures nulls are left at the bottom of the list
		
		log.debug(s);
		ContentVO content = null;
		int i = 0;
		try (PreparedStatement ps = dbConn.prepareStatement(s.toString())){
			ps.setString(++i, SyncStatus.Approved.toString());
			ps.setString(++i, SyncStatus.Declined.toString());
			for (MenuObj menu : pages)
				ps.setString(++i, menu.getPageId());
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				if (data.containsKey(rs.getString("page_id"))) continue; //we already have the record we want (pending one)
				content = new ContentVO(rs.getString("action_id"), null);
				content.setActionGroupId(rs.getString("action_group_id"));
				content.setActionDesc(rs.getString("action_desc"));
				content.setActionName(rs.getString("action_nm"));
				content.setAttribute("pmid", rs.getString("page_module_id"));
				content.setSyncData(new ApprovalVO(rs));
				data.put(rs.getString("page_id"), content);
			}
		} catch(Exception e) {
			log.error("Unable to retrieve franchise page sync records ", e);
		}
		return data;
	}
	
	
	/**
	 * Retrieves the Content Portlet associated to the page
	 * @param pageId
	 * @return
	 * @throws ActionException
	 */
	public ContentVO getContent(PageVO page, String templateId, String sharedId) throws ActionException {
		//templateId: loads default page/content copy from the shared org, and 
		//pre-populates select fields using Freemarker
		if (templateId != null && templateId.length() > 0) {
			log.debug("loading template " + templateId);
			ContentVO content = loadContentFromTemplate(page, templateId, sharedId);
			if (content != null) return content;
		}
		
		StringBuilder s = new StringBuilder(300);
		s.append("select top 1 c.action_id, c.article_txt, ");
		s.append("isnull(g.action_group_id, f.action_group_id) as action_group_id, a.page_module_id, ");
		s.append("isnull(g.attrib1_txt, f.attrib1_txt) as attrib1_txt, ");
		s.append("isnull(g.attrib2_txt, f.attrib2_txt) as attrib2_txt, ");
		s.append("isnull(g.action_nm,f.action_nm) as action_nm, ");
		s.append("isnull(g.action_desc,f.action_desc) as action_desc, ws.* ");
		s.append("from page_module a ");
		s.append("left join sb_action f on a.action_id = f.action_id "); //the approved record
		s.append("left outer join sb_action g on f.action_group_id = g.action_group_id ");  //the pending record
		s.append("left outer join CONTENT c on ISNULL(g.ACTION_ID, f.ACTION_ID)=c.ACTION_ID ");
		s.append("left join wc_sync ws on (ISNULL(g.ACTION_ID, f.ACTION_ID) = ws.wc_key_id) and wc_sync_status_cd not in (?,?) ");
		s.append("where a.page_id=? order by ISNULL(c.update_dt, '1900-06-05T23:59:00') desc "); //ordering by date gets us the most recent revision and ensures nulls are left at the bottom of the list
		log.debug(s+"|"+page.getPageId());
		ContentVO content = null;
		try (PreparedStatement ps = dbConn.prepareStatement(s.toString())){
			ps.setString(1, SyncStatus.Approved.toString());
			ps.setString(2, SyncStatus.Declined.toString());
			ps.setString(3, page.getPageId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				content = new ContentVO(rs.getString(1), rs.getString(2));
				content.setActionGroupId(rs.getString("action_group_id"));
				content.setActionDesc(rs.getString("action_desc"));
				content.setActionName(rs.getString("action_nm"));
				content.setAttribute("pmid", rs.getString("page_module_id"));
				content.setSyncData(new ApprovalVO(rs));
			} else {
				//setup some default copy to aid the Users...
				content = new ContentVO(null, "Insert page copy here");
				content.setActionDesc("Insert Headline Text here");
				content.setActionName("Insert Headline here");
				content.setAttribute("isPublic", "");
			}
		} catch(Exception e) {
			log.error("Unable to retrieve franchise page: " + page.getPageId(), e);
		}
		
		return content;
	}
	
	/**
	 * Build the photo gallery for the mobile gallery page.  We have to build
	 * it here instead of in the action b/c the SBActionAdapter Update is looking
	 * for AdminModuleData and we do not have that here since its a public call.
	 * @param req
	 * @throws ActionException
	 */
	public String addPhotoGallery(SMTServletRequest req) throws ActionException {
		
		//Build Action Id
		String actionId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId() + "_GALLERY_" + CenterPageAction.getFranchiseId(req);

		/*
		 * Build sb_action entry for the File Gallery 
		 */
		StringBuilder sql = new StringBuilder("insert into sb_action (action_nm, action_desc, organization_id, ");
		sql.append("module_type_id, action_id, create_dt) values (?,?,?,?,?,?)");
		PreparedStatement ps = null;

		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, "Mobile Gallery");
			ps.setString(2, "Mobile Gallery");
			ps.setString(3, req.getParameter("organizationId"));
			ps.setString(4, "FILE_GALLERY");
			ps.setString(5, actionId);
			ps.setTimestamp(6, Convert.getCurrentTimestamp());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}	
		
        //Build Gallery
		sql = new StringBuilder();
		sql.append("insert into file_gallery (organization_id, ");
        sql.append("create_dt, action_id) ");
        sql.append("values (?,?,?)");
	
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter("organizationId"));
			ps.setTimestamp(2, Convert.getCurrentTimestamp());
			ps.setString(3, actionId);
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error(e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
		}
		
		//Build Album
		sql = new StringBuilder();
		sql.append("insert into file_gallery_album (ACTION_ID, ALBUM_NM, ");
		sql.append("ORDER_NO, CREATE_DT, GALLERY_ALBUM_ID) ");
		sql.append("values (?,?,?,?,?)");
		
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, actionId);
			ps.setString(2, "Mobile Gallery Album");
			ps.setInt(3, 1);
			ps.setTimestamp(4, Convert.getCurrentTimestamp());
			ps.setString(5, actionId + "_ALBUM");
			ps.executeUpdate();			
		} catch (SQLException sqle) {
			log.error("Error Update GalleryAlbum", sqle);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {
			}
		}
		req.setAttribute("SB_ACTION_ID", actionId);
		//Ensure the parameters we need for Gallery are set properly.
		StringBuilder redir = new StringBuilder();
		redir.append("galleryId=").append(actionId);
		redir.append("&galleryAlbumId=").append(actionId).append("_ALBUM");
		redir.append("&moduleTypeId=").append("FILE_GALLERY");
		redir.append("&moduleDisplayId=").append("c0a8022daaf0da55997c811521e2a3d7&");
		return redir.toString();
	}
	
	public Map<String, CenterModuleOptionVO> getCPModuleOptions(String [] optIds, SMTServletRequest req) {
		Map<String, CenterModuleOptionVO> options = new HashMap<String, CenterModuleOptionVO>();
		StringBuilder sql = new StringBuilder();
		sql.append("select * from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("FTS_CP_MODULE_OPTION where CP_MODULE_OPTION_ID in ('");
		for(String opt : optIds) {
			sql.append(opt).append("','");
		}
		sql = new StringBuilder(sql.substring(0, sql.lastIndexOf(",")));
		sql.append(")");
		PreparedStatement ps = null;
		log.debug(sql.toString());
		try {
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				options.put(rs.getString("CP_MODULE_OPTION_ID"), new CenterModuleOptionVO(rs));
			}
		} catch(SQLException sqle) {
			log.error("Could not retrieve Options.", sqle);
		}
		
		return options;
	}
	
	public void updatePhotoGallery(SMTServletRequest req) {
		String [] selected = req.getParameterValues("selectedElements");
		String actionId = ((SiteVO)req.getAttribute("siteData")).getOrganizationId() + "_GALLERY_" + CenterPageAction.getFranchiseId(req);
		String albumId = req.getParameter("galleryAlbumId");
		String franId = CenterPageAction.getFranchiseId(req);
		PreparedStatement ps = null;
		
		//Delete existing
		StringBuilder sql = new StringBuilder();
		sql.append("delete from FILE_GALLERY_ITEM where GALLERY_ALBUM_ID = ?");
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, albumId);
			ps.executeUpdate();
			ps.close();
		} catch (SQLException sqle){
			log.error("Could not delete old photo items", sqle);
		}
		//iterate and add new
		sql = new StringBuilder();
		sql.append("insert into FILE_GALLERY_ITEM (GALLERY_ITEM_ID, ");
		sql.append("GALLERY_ALBUM_ID, MAIN_IMG_URL, THUMB_IMG_URL, LINK_URL, ");
		sql.append("ENTRY_NM, DESC_TXT, ORDER_NO, CREATE_DT) values (?,?,?,?,?,?,?,?,?)");
		Map<String, CenterModuleOptionVO> options = getCPModuleOptions(selected, req);
		try{
			ps = dbConn.prepareStatement(sql.toString());
			for(String itemId : selected) {
				CenterModuleOptionVO opt = options.get(itemId);
				ps.setString(1, franId + "_" + itemId);
				ps.setString(2, albumId);
				ps.setString(3, opt.getFilePath());
				ps.setString(4, opt.getThumbPath());
				ps.setString(5, opt.getLinkUrl());
				ps.setString(6, stripHTML(opt.getOptionName()));
				ps.setString(7, opt.getOptionDesc());
				ps.setInt(8, opt.getOrderNo());
				ps.setTimestamp(9, Convert.getCurrentTimestamp());
				ps.addBatch();
			}
			ps.executeBatch();
		} catch (SQLException sqle){
			log.error("Could not delete old photo items", sqle);
		}
		this.clearCacheByActionId(actionId);

		req.setAttribute(SB_ACTION_ID, req.getParameter("sbActionId"));
	}
	
	/**
	 * Retrieves a list of pages for the franchise
	 * @param req
	 * @throws ActionException
	 */
	public PageContainerVO listPages(SMTServletRequest req, String pageId) throws ActionException {
		int franId = Convert.formatInteger(CenterPageAction.getFranchiseId(req));
		boolean isMobile = Convert.formatBoolean(req.getSession().getAttribute("webeditIsMobile"));
		String localization = StringUtil.checkVal(((SiteVO)req.getAttribute("siteData")).getLocale());
		if (req.getParameter("apprFranchiseId") != null)
			franId = Convert.formatInteger((String)req.getParameter("apprFranchiseId"));
		StringBuilder s = new StringBuilder(425);
		s.append("select p.*, pr.ROLE_ID, ws.* from page p left outer join page_role pr ");
		s.append("on p.page_id = pr.page_id and pr.role_id = '1000' left join wc_sync ws on ");
		s.append("ws.wc_key_id = p.page_id and wc_sync_status_cd not in (?,?) ");
		s.append("where p.site_id = ? and p.page_id not in (");
		s.append("select page_group_id from page where page_group_id is not null and page_id != page_group_id) ");
		if (pageId.length() > 0) s.append("and p.page_id = ? ");
		s.append("order by p.parent_id, order_no, page_display_nm");
		log.debug("Franchise Page SQL: " + s +"|"+pageId);
		String sitePrefix = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		log.debug("OrgId = " + ((SiteVO)req.getAttribute("siteData")).getOrganizationId());
		log.debug("Serving pages for localization: " + localization + "\n Serving Site ID: " + sitePrefix);

		List<MenuObj> data = new ArrayList<MenuObj>();
		PageContainerVO pc = PageContainerFactory.getInstance(localization, isMobile);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			int ctr = 1;
			ps.setString(ctr++, SyncStatus.Approved.toString());
			ps.setString(ctr++, SyncStatus.Declined.toString());
			if(isMobile)
				ps.setString(ctr++, sitePrefix +"_"+ franId + "_2");
			else
				ps.setString(ctr++, sitePrefix +"_"+ franId + "_1");
			if (pageId.length() > 0) ps.setString(ctr++, pageId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MenuObj p = new MenuObj();
				p.setData(rs);
				p.setSyncData(new ApprovalVO(rs));
				
				// Check if this page if this page is allowed to be edited via webedit
				if (StringUtil.checkVal(rs.getString("ROLE_ID")).length() > 0)
					p.setLevel(100);
				
				data.add(p);
			}
		} catch(Exception e) {
			log.error("Unable to retrieve franchise pages", e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
			
			pc.addPages(data);
		}
		
		return pc;
	}
	
	private void savePage(SMTServletRequest req) throws ActionException {
		try {
		if (ADD_PAGE != Convert.formatInteger(req.getParameter("bType"))){
			//change the column number for the content for page edits
			changeDisplayColumn(req, Convert.formatBoolean(req.getParameter("showMenu"), true));
		}
		SitePageAction ai = new SitePageAction(this.actionInit);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		populatePageFreemarker(req);
		if (req.getFile("image1") != null && req.getFile("image1").isFileData()) {
			try {
				req.setParameter("src", ai.writePageImage(req));
			} catch (FileWriterException fwe) {
				//this has already been written to the logs, and we don't need to do anything more here
			}
		}
		ApprovalDecoratorAction dec = new ApprovalDecoratorAction(ai);
		req.setParameter("wcKeyId", (String)req.getParameter("pageId"));
		//req.setParameter("pageGroupId", req.getParameter("pageId"), true);
		req.setParameter("ignoreTemplates", "true");
		req.setParameter("actionId", "Page", true);
		req.setParameter("siteName", WebeditType.CenterPage.toString());
		dec.update(req);
		} catch(Exception e) {
			log.error(e);
		}
	}
	
	/**
	 * Changes the display column, used for keeping content visible when switching
	 * between menu and menu-less pages.
	 * @param showMenu true if the menus are shown on this page, false if not
	 */
	private void changeDisplayColumn(SMTServletRequest req, boolean showMenu){
		//retrive the replace vals map
		@SuppressWarnings("unchecked")
		Map<String,Object> repl = (Map<String,Object>) attributes.get(RecordDuplicatorUtility.REPLACE_VALS);
		if (repl == null) {
			repl = new HashMap<>();
			
			//attempt to grab existing page information
			PageVO page = (PageVO) req.getAttribute("PAGE_INFO");
			if (page == null){
				page = new PageVO(req);
			}
			//Add data required by SitePageAction.copy() to add new pages
			repl.put("SITE_ID",page.getSiteId());
			repl.put("TEMPLATE_ID", page.getTemplateId());
		}
		
		//change the column for the page module so content shows up
		if (showMenu){
			repl.put("display_column_no", Integer.valueOf(2));
		} else {
			repl.put("display_column_no", Integer.valueOf(1));
		}
		//add replace vals back to attributes (in case we just created a new one)
		attributes.put(RecordDuplicatorUtility.REPLACE_VALS, repl);
	}
	
	private void savePageModule(SMTServletRequest req) throws ActionException {
		req.setParameter("displayOrder", String.valueOf(5));
		req.setParameter("moduleActionName", req.getParameter("actionName"));
		req.setParameter("moduleId", (String)req.getAttribute(SB_ACTION_ID));
		SMTActionInterface ai = new PageModuleAction(this.actionInit);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		ai.update(req);
	}
	
	/**
	 * content must be created before the pageModule associative!
	 * @param req
	 * @throws ActionException
	 */
	private void saveModule(SMTServletRequest req) throws ActionException {
		ModuleVO mod = new ModuleVO();
		if (Convert.formatBoolean(req.getParameter("makeNew"))) req.setParameter("insertAction", "true");
		
		req.setParameter("wcKeyId", req.getParameter(SB_ACTION_ID), true);
		req.setParameter("wcOrigKeyId", req.getParameter(SB_ACTION_GROUP_ID), true);
		req.setParameter("actionId", "Portlet", true);
		req.setAttribute(SB_ACTION_ID, req.getParameter(SB_ACTION_ID));
		mod.setActionId("CONTENT");
		attributes.put(AdminConstants.ADMIN_MODULE_DATA, mod);
		SMTActionInterface ai = new ContentAction(this.actionInit);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		ApprovalDecoratorAction dec = new ApprovalDecoratorAction(ai);
		dec.update(req);
	}
	
	private void reorderPages(SMTServletRequest req) throws ActionException {
		attributes.put(AdminConstants.ADMIN_MODULE_DATA, new ModuleVO());
		SitePageAction ai = new SitePageAction(this.actionInit);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		ai.build(req);
	}
	
	private void archivePage(SMTServletRequest req) throws ActionException {
		String pageId = req.getParameter("pageId");
		Timestamp t = null;
		if (Convert.formatBoolean(req.getParameter("hidePg"))) {
			t = Convert.getCurrentTimestamp();
		} else {
			t = Convert.formatTimestamp(Convert.DATE_SLASH_PATTERN, "01/01/2050");
		}
		log.info("setting page " + pageId + " to disappear on " + t.toString());
		
		String sql = "update page set live_end_dt=? where page_id=?";
		PreparedStatement ps = null;
		
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setTimestamp(1, t);
			ps.setString(2, pageId);
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}

		log.debug("clearing cache for siteId=" + req.getParameter("siteId"));
		super.clearCacheByGroup(req.getParameter("siteId"));
	}
	
	private String saveFile(SMTServletRequest req) throws FileWriterException {
		String retVal = "";
		StringBuffer filePath =  new StringBuffer((String)getAttribute("pathToBinary"));
		filePath.append(getAttribute("orgAlias")).append(req.getParameter("organizationId"));
		filePath.append("/docs/");

    	FileLoader fl = null;
    	FilePartDataBean fpdb = req.getFile("pFile");
		String newFileNm = (fpdb != null) ? fpdb.getFileName() : "";
		log.debug("newFile=" + newFileNm);
		
		// Write new file
    	if (newFileNm.length() > 0) {
    		StringBuilder fPath = new StringBuilder("/binary/org/");
    		fPath.append(req.getParameter("organizationId")).append("/docs/");

    		try {
	    		fl = new FileLoader(attributes);
	        	fl.setFileName(fpdb.getFileName());
	        	fl.setPath(filePath.toString());
	        	fl.setRename(Boolean.FALSE); //replaces an existing image
	    		fl.setOverWrite(Boolean.TRUE);
	        	fl.setData(fpdb.getFileData());
	        	retVal = fPath.toString() + fl.writeFiles();
	    	} catch (Exception e) {
	    		log.error("Error Writing PageImage File", e);
	    		throw new FileWriterException(e);
	    	}
	    	log.debug("finished write of " + retVal);
    	}
    	
    	fpdb = null;
    	fl = null;
		return retVal;
	}
	
	private ContentVO loadContentFromTemplate(PageVO page, String templateId, String sharedId) {
		ContentVO vo = null;
		String sql = "select * from content a inner join sb_action b on a.action_id=b.action_id " +
				"and b.action_nm=? and b.organization_id='" + sharedId + "'";
		log.debug("Load Content From Template Sql = " + sql);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sql);
			ps.setString(1, "subpageTemplate-" + templateId);
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				log.debug("template loaded");
				vo = new ContentVO();
				vo.setActionName(rs.getString("action_nm")); //reserved
				vo.setActionDesc(rs.getString("action_desc")); //unused
				vo.setArticle(rs.getString("article_txt")); //article body
				vo.setIntroText(rs.getString("intro_txt")); //page's meta_desc
				vo.setAttribute(SBModuleVO.ATTRIBUTE_1, rs.getString("attrib1_txt")); //page's title
				log.debug(vo);
				populateFreemarker(vo);
				page.setMetaDesc(vo.getIntroText());
				page.setTitleName(StringUtil.checkVal(vo.getAttribute(SBModuleVO.ATTRIBUTE_1)).toString());
			}
			
		} catch (SQLException sqle) {
			log.error(sqle);
		} finally {
			try { ps.close(); } catch (Exception e) {}
		}
		
		return vo;
	}
	
	private ContentVO populateFreemarker(ContentVO vo) {
		FranchiseLocationInfoAction cpa = new FranchiseLocationInfoAction(actionInit);
		cpa.setDBConnection(dbConn);
		cpa.setAttributes(attributes);
		FranchiseVO franVo = cpa.getLocationInfo(attributes.get("webeditFranId").toString(), false);
		Map<String, Object> data = franVo.getFreemarkerTags();
		String actionNm = vo.getActionName();
		try {
			vo.setArticle(MessageParser.getParsedMessage(vo.getArticle().toString(), data, actionNm + "-body").toString());
			
			StringBuffer text = new StringBuffer(StringUtil.checkVal(vo.getIntroText()));
			if (text.length() > 0) 
				vo.setIntroText(MessageParser.getParsedMessage(text.toString(), data, actionNm + "-metaDesc").toString());
			
			text = new StringBuffer(StringUtil.checkVal(vo.getAttribute(SBModuleVO.ATTRIBUTE_1)));
			if (text.length() > 0) 
				vo.setAttribute(SBModuleVO.ATTRIBUTE_1, MessageParser.getParsedMessage(text.toString(), data, actionNm + "-title").toString());
			
		} catch (Exception e) {
			log.error("could not make freemarker substitutions", e);
		}
		log.debug("freemarker complete");
		return vo;
	}
	
	private void populatePageFreemarker(SMTServletRequest req){
		String external1 = req.getParameter("externalPageUrl");
		String localization = StringUtil.checkVal(((SiteVO)req.getAttribute("siteData")).getLocale());
		StringBuffer external2 = new StringBuffer();
		PageContainerVO pc = PageContainerFactory.getInstance(localization, Convert.formatBoolean(req.getSession().getAttribute("webeditIsMobile")));
		Map<String, Object> data = pc.getFreemarkerTags(req);
		
		try{
			if(StringUtil.checkVal(external1).length() > 0){
				external2 = MessageParser.getParsedMessage(external1, data, req.getParameter("webeditFranId") + "-alias");
				req.setParameter("externalPageUrl", external2.toString());
			}
		} catch (Exception e) {
			log.error("could not make freemarker substitutions", e);
		}
	}
	
	protected String stripHTML(String target) {
		return RegexParser.regexReplace(RegexParser.Patterns.STRIP_ALL_HTML, StringUtil.checkVal(target), "");
	}
	
	/**
	 * Sets the template Id on the request to point to the single column layout
	 * @param req
	 * @throws Exception 
	 */
	private void setEmptyColLayout(SMTServletRequest req) throws Exception{
		log.debug("Single Column Layout Selected");
		
		//Get the proper SiteWizardAction
		SiteVO site = (SiteVO)req.getAttribute("siteData");
		String siteId = StringUtil.checkVal(req.getParameter("siteId"));
		SiteWizardFactoryAction wizardFactory = new SiteWizardFactoryAction();
		SiteWizardAction swa = wizardFactory.retrieveWizard(site.getCountryCode());
		swa.setAttributes(attributes);
		swa.setDBConnection(dbConn);
		
		String tId = null;
		try{
			//Check if the layout was already created
			tId = swa.getSecondaryLayoutId(siteId, SiteWizardAction.EMPTY_COL_LABEL);
		} catch (Exception e){ 
			log.error("Couldn't fetch layout",e); 
			throw e;
		}
		//If the layout doesn't exits yet, create it
		if (StringUtil.checkVal(tId).isEmpty()){
			log.debug("***********Creating new "+SiteWizardAction.EMPTY_COL_LABEL);
			tId = swa.addEmptyColLayout(req);
			
			//Grab the center number from the siteId
			String[] cId = req.getParameter("organizationId").split("_");
			swa.setCenterId(Convert.formatInteger(cId[cId.length-1]));
			swa.assignTypes();
			swa.associateCenterPage(tId, null, null, 2);
		}
		
		req.setParameter("templateId", tId);
		req.setParameter("displayColumn","1");
	}
}
