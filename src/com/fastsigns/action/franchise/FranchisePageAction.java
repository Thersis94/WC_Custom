package com.fastsigns.action.franchise;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fastsigns.action.LogAction;
import com.fastsigns.action.approval.ApprovalFacadeAction;
import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.fastsigns.action.approval.vo.ApprovalVO;
import com.fastsigns.action.approval.vo.PageLogVO;
import com.fastsigns.action.approval.vo.PageModuleLogVO;
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
import com.smt.sitebuilder.admin.action.sync.SyncTransactionAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.util.MessageParser;

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
		
		//delete from the Changelog 
		LogAction lA = new LogAction(this.actionInit);
		lA.setDBConnection(dbConn);
		lA.setAttributes(attributes);
		List<String> ids = new ArrayList<String>();
		ids.add(pageId);
		lA.deleteFromChangelog(ids);
		
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
					req.setParameter("startDate", "01/01/2100");  //new pages must be approved before being released
					req.setParameter("titleName", "Insert browser title here");
					
					//we don't need Content or PMID/permissions if this is just a redirect to another resource
					//redirects do not require admin approval, so we can set a valid start/end date for these pages.
					if ((lvl >= 50 || !Convert.formatBoolean(req.getParameter("customPg"))) 
							&& Convert.formatBoolean(req.getParameter("visible")))
					{
						
						req.setParameter("startDate", Convert.formatDate(new java.util.Date()));
						req.setParameter("endDate", null);
						noPageModule = true;
					}
					
					//Check if the user wants to add a page with no left or right rails
					if ( Convert.formatBoolean( req.getParameter("singleCol"), false )){
						setSingleColLayout(req);
					}
					
					this.savePage(req);
					if(req.getParameter("pageNm").equals("gallery")){
						log.debug("adding gallery page.");
						redir.append(addPhotoGallery(req));
					}
					if (noPageModule && lvl != 50) 
						break;
					
					//progress the user to the next form, to edit the page they just created.
					redir.append("pageId=").append(req.getAttribute("pageId"));
					redir.append("&lvl=").append(lvl).append("&template=");
					redir.append(req.getParameter("aliasName"));
					break;
					
				case EDIT_PAGE_COPY:
					this.savePage(req);
					if(req.hasParameter("galleryId"))
						updatePhotoGallery(req);
					else
						this.saveModule(req);
					
					//we don't need the page_module association unless a new portlet was created.
					if (Convert.formatBoolean(req.getParameter("makeNew"))) {
						String basePgId = StringUtil.checkVal(req.getAttribute("pageId"));
						String baseModId = (String)req.getAttribute(SB_ACTION_ID);
						req.setParameter("pageId", basePgId);
						req.setParameter("moduleId", baseModId);
						this.savePageModule(req);
					}
					break;
				
				case EDIT_PAGE_FILE:
					if (!Convert.formatBoolean(req.getParameter("defaultPage"))) {
						String s = this.saveFile(req);
						if (s != null && s.length() > 0) req.setParameter("externalPageUrl", s);
					}
					
					this.savePage(req);
					break;
				
				case REORDER_PAGE:
					this.reorderPages(req);
					break;
				
				case ARCHIVE_PAGE:
					this.archivePage(req);
					break;
				
				case SUBMIT_PAGE:
					req.setParameter("startDate", "01/01/2200");  //new pages must be approved before being released
					this.submitPage(req);
					break;	
					
				case SUBMIT_PAGE_MODULE:
					req.setParameter("approvalType", ApprovalFacadeAction.SUBMIT + "");
					this.submitContent(req);
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
	
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		String pageId = StringUtil.checkVal(req.getParameter("pageId"));
		String franId = CenterPageAction.getFranchiseId(req);
		attributes.put("webeditFranId", franId);
		
        String previewApiKey = SyncTransactionAction.generatePreviewApiKey(attributes);
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
			for (PageVO p : pages) {
				if (p.getPageId() == null || p.isDefaultPage() || StringUtil.checkVal(p.getExternalPageUrl()).length() > 0) 
					continue; //skip these, they will never require approval!
				
				ContentVO vo = this.getContent(p, null, pc.getSharedId());
				log.debug("loaded " + p.getDisplayName() + " with Content permissions " + vo.getAttribute("isPublic"));
				if(StringUtil.checkVal(vo.getAttribute("isPublic")).length() == 0 && p.isLive()){
					if (vo.getActionId() != null) {
						 p.setFooterFlag(true);
					}
					contents.put(p.getPageId(), vo);
				}
				vo = null;
			}
			List<String> l = new ArrayList<String>();

			for(ContentVO s : contents.values()){
				l.add((String) s.getAttribute("pmid"));
			}
			
			ApprovalFacadeAction sb = new ApprovalFacadeAction(this.actionInit);
			sb.setAttributes(attributes);
			sb.setDBConnection(dbConn);
			req.setAttribute("changelogs", sb.getChangeLogStatus(l, null));
			
		}
		req.setAttribute("contents", contents);
		if (Convert.formatBoolean(req.getParameter("preview")))
			req.setAttribute("pageContainer", pc);
		
		// Add the data to the Module Container
		this.putModuleData(pc);
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
		
		StringBuilder s = new StringBuilder();
		s.append("select top 1 a.action_id, article_txt, a.page_module_id, c.*, d.page_module_role_id ");
		s.append("from page_module a inner join content b ");
		s.append("on a.action_id = b.action_id inner join sb_action c ");
		s.append("on b.action_id=c.action_id ");
		s.append("left outer join page_module_role d on ");
		s.append("a.page_module_id=d.page_module_id and d.role_id='0' ");  //this tells us whether the module is live or not
		s.append("where a.page_id = ? order by b.create_dt desc "); //ordering by date gets us the most recent revision
		
		ContentVO content = null;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			ps.setString(1, page.getPageId());
			ResultSet rs = ps.executeQuery();
			if (rs.next()) {
				content = new ContentVO(rs.getString(1), rs.getString(2));
				content.setActionDesc(rs.getString("action_desc"));
				content.setActionName(rs.getString("action_nm"));
				content.setAttribute("isPublic", rs.getString("page_module_role_id"));
				content.setAttribute("pmid", rs.getString("page_module_id"));
			} else {
				//setup some default copy to aid the Users...
				content = new ContentVO(null, "Insert page copy here");
				content.setActionDesc("Insert Headline Text here");
				content.setActionName("Insert Headline here");
				content.setAttribute("isPublic", "");
			}
		} catch(Exception e) {
			log.error("Unable to retrieve franchise page: " + page.getPageId(), e);
		} finally {
			try {
				ps.close();
			} catch (Exception e) {}
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
		StringBuilder s = new StringBuilder("select p.*, pr.ROLE_ID from page p left outer join page_role pr ");
		s.append("on p.page_id = pr.page_id and pr.role_id = '1000' where p.site_id = ? ");
		if (pageId.length() > 0) s.append("and p.page_id = ? ");
		s.append("order by parent_id, order_no, page_display_nm");
		log.debug("Franchise Page SQL: " + s);
		String sitePrefix = ((SiteVO)req.getAttribute("siteData")).getOrganizationId();
		log.debug("OrgId = " + ((SiteVO)req.getAttribute("siteData")).getOrganizationId());
		log.debug("Serving pages for localization: " + localization + "\n Serving Site ID: " + sitePrefix);

		List<MenuObj> data = new ArrayList<MenuObj>();
		PageContainerVO pc = PageContainerFactory.getInstance(localization, isMobile);
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(s.toString());
			if(isMobile)
				ps.setString(1, sitePrefix +"_"+ franId + "_2");
			else
				ps.setString(1, sitePrefix +"_"+ franId + "_1");
			if (pageId.length() > 0) ps.setString(2, pageId);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				MenuObj p = new MenuObj();
				p.setData(rs);
				
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
		req.setParameter("skipApproval", "true");
		ai.update(req);
	}
	
	private void savePageModule(SMTServletRequest req) throws ActionException {
        req.setParameter("displayOrder", String.valueOf(5));
        log.debug(req.getParameter("displayName"));
        req.setParameter("moduleActionName", req.getParameter("displayName"));
        if (Convert.formatBoolean(req.getParameter("makeNew"))) {        	
        	// Add the admin and custom franchisee role to the request
			if(!Convert.formatBoolean(req.getParameter("isGallery"))) {
				for(String role : req.getParameterValues("roles")) {
					if (role.length() > 5)
						req.setParameter("roleId", new String[]{role, "100"}, true);
				}
			}
		}
    	
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
		if (Convert.formatBoolean(req.getParameter("makeNew"))) {
			//forces WC to create a new Portlet instead of updating the existing one
			req.setParameter(SB_ACTION_ID, null);
			//submitContent(req);
		}
		mod.setActionId("CONTENT");
		attributes.put(AdminConstants.ADMIN_MODULE_DATA, mod);
		SMTActionInterface ai = new ContentAction(this.actionInit);
		ai.setDBConnection(dbConn);
		ai.setAttributes(attributes);
		ai.update(req);
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
	
	/** submits the content to the FTS admins for approval 
	 * @throws ActionException 
	 **/
	protected void submitContent(SMTServletRequest req) throws ActionException {
		ApprovalFacadeAction aA = new ApprovalFacadeAction(this.actionInit);
		aA.setDBConnection(dbConn);
		aA.setAttributes(attributes);
		ApprovalVO avo = new ApprovalVO();
		String [] pmIds = req.getParameter("modulesToSubmit").split(",");
		List<AbstractChangeLogVO> vos = new ArrayList<AbstractChangeLogVO>();
		for(String id : pmIds){
			if(!id.equals("0")){
			req.setParameter("componentId", id.trim());
			vos.add(new PageModuleLogVO(req));
			}
		}
		avo.setChangeLogList(PageModuleLogVO.TYPE_ID, vos);
		req.setAttribute("approvalVO", avo);
		aA.update(req);
	}
	
	/** submits the page to the FTS admins for approval 
	 * @throws ActionException **/
	protected void submitPage(SMTServletRequest req) throws ActionException {
		ApprovalFacadeAction aA = new ApprovalFacadeAction(this.actionInit);
		aA.setDBConnection(dbConn);
		aA.setAttributes(attributes);
		ApprovalVO avo = new ApprovalVO();
		String [] pageIds = req.getParameter("pagesToSubmit").split(",");
		List<AbstractChangeLogVO> vos = new ArrayList<AbstractChangeLogVO>();
		for(String id : pageIds){
			if(!id.equals("0")){
			req.setParameter("componentId", id.trim());
			vos.add(new PageLogVO(req));
			}
		}
		avo.setChangeLogList(PageLogVO.TYPE_ID, vos);
		req.setAttribute("approvalVO", avo);
		aA.update(req);
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
	private void setSingleColLayout(SMTServletRequest req) throws Exception{
		log.debug("Single Column Layout Selected");
		
		//Get the proper SiteWizardAction
		SiteVO site = (SiteVO)req.getAttribute("siteData");
		SiteWizardFactoryAction wizardFactory = new SiteWizardFactoryAction();
		SiteWizardAction swa = wizardFactory.retrieveWizard(site.getCountryCode());
		
		//Check if the layout was already created
		String tId = swa.getSecondaryLayoutId(site.getSiteId(), 
				SiteWizardAction.SINGLE_COL_LABEL);
		
		//If the layout doesn't exits yet, create it
		if (StringUtil.checkVal(tId).isEmpty()){
			tId = swa.addSingleColLayout(req);
		}
		
		req.setParameter("templateId", tId);
	}
}