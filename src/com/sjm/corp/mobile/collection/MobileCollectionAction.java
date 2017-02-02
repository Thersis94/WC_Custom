package com.sjm.corp.mobile.collection;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.io.SMBFileManager;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.sjm.corp.mobile.collection.action.CollectionActionFactory;

import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.admin.action.SBModuleAction;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteBuilderUtil;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: MobileCollectionAction.java<p/>
 * <b>Description: Manages Mobile Collection Actions for SJM</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since June 21, 2012
 ****************************************************************************/
 
 public class MobileCollectionAction extends SBActionAdapter {
	private SiteBuilderUtil util = null;
	public static final String PORTLET_ID = "MOB_COL_DOC";
	private String practiceTable; //These members store the custom location of the various tables that we handle.
	private String themeTable;
	private CollectionActionFactory fac;
	
	public MobileCollectionAction() {
		super();
		util = new SiteBuilderUtil();
		fac = new CollectionActionFactory();
	}
	
	public MobileCollectionAction(ActionInitVO init){
		super(init);
		util = new SiteBuilderUtil();
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionControllerdelete(com.siliconmtn.http.SMTServletRequest)
	 */
	public void delete(ActionRequest req) throws ActionException {
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		practiceTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_practice";
		themeTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_theme";
		if(req.getParameter("facadeType") != null){
			deleteTheme(req,req.getParameter("themeId")); // We're only deleting a theme
			util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	        String url = req.getParameter("url"); //Redirect url
	        req.setAttribute(Constants.REDIRECT_URL, url);
		}
		else{
			deleteTables(req); //Delete the data associated with a given practice/theme
			try {
				super.delete(req); //remove from the sb_action table
			} catch(Exception e) {
				msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}
			util.moduleRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		}
	}
	
	/* (non-Javadoc)
	 * @see com.siliconmtn.action.AbstractActionControllerdelete(com.siliconmtn.http.SMTServletRequest)
	 */
	 public void update(ActionRequest req) throws ActionException{
		practiceTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_practice";
		themeTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_theme";
		
		if(req.getParameter("facadeType") != null)
			updateFacade(req);
		else{
			super.update(req);
			updateRegion(req);
		}
	 }

	 /*
	  * (non-Javadoc)
	  * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	  */
	 public void list(ActionRequest req) throws ActionException{
		String facadeType = req.getParameter("facadeType");
		if(facadeType != null && !facadeType.isEmpty()){
			listThemes(req);
		} else {
			listMainPortlet(req);
		}
	}
	 /**
	  * Grabs a list of Theme names and locations, and stores it in the ModVO's actionData
	  * @param req
	  * @throws ActionException
	  */
	 public void listThemes(ActionRequest req) throws ActionException{
		ThemeVO vo = new ThemeVO();
		themeTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_theme";
		StringBuffer sql = new StringBuffer();
		ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
		if(mod.getActionData() == null)
			vo = new ThemeVO();
		super.list(req);
		String themeId = req.getParameter("themeEntryId");
        if (themeId == null || themeId.length() == 0) return;
        
		 sql.append("select theme_name, location, thumb_loc, color_1_front, color_2_front, ");
		 sql.append("color_1_back, color_2_back from ").append(themeTable).append(" where ");
		 sql.append("theme_id = ?");
		 
		 PreparedStatement ps = null;
		 try{
			 ps = dbConn.prepareStatement(sql.toString());
			 ps.setString(1, themeId);
			 ResultSet rs = ps.executeQuery();
			 if(rs.next()){
				 vo.getName().add(rs.getString(1));
				 vo.setLocation(rs.getString(2));
				 vo.getThumbLoc().add(rs.getString(3));
				 vo.getColorOneFront().add(rs.getString(4));
				 vo.getColorTwoFront().add(rs.getString(5));
				 vo.getColorOneBack().add(rs.getString(6));
				 vo.getColorTwoBack().add(rs.getString(7));
			 }
		 }  catch(Exception e) {
				log.error("Error fetching data from the sjm_mobile_theme table", e);
		} finally {
			if (ps != null) {
		 		try {
		        	ps.close();
		        } catch(Exception e) {}
		 	}
		}
		 mod.setActionData(vo);
		this.setAttribute(AdminConstants.ADMIN_MODULE_DATA, mod);
		log.debug("ModVO Admin" +this.getAttribute(AdminConstants.ADMIN_MODULE_DATA));
	 }
	 
	 /**
	  * Lists the details for the main non-facade portlet
	  * @param req
	  */
	 public void listMainPortlet(ActionRequest req) throws ActionException{
		MobileCollectionVO vo = new MobileCollectionVO();
		ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
		if(mod.getActionData() == null)
			vo = new MobileCollectionVO();
		super.list(req);
		String actionId = req.getParameter(SBModuleAction.SB_ACTION_ID);
	    if (actionId == null || actionId.length() == 0) return;  
	    StringBuffer sql = new StringBuffer();
	    sql.append("select name,max_selected from ").append(getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_region");
	    sql.append(" where action_id = ?");
	        
	    PreparedStatement ps = null;
	    try{
        	ps = dbConn.prepareStatement(sql.toString());
        	ps.setString(1, actionId);
        	Map<String, String> map = new HashMap<String,String>();
        	ResultSet rs = ps.executeQuery();
        	while(rs.next())
        		map.put(rs.getString(1), rs.getString(2));
        	vo.getRegion().setNameMap(map);
        }  catch(Exception e) {
			log.error("Error fetching data from the sjm_mobile_region table", e);
		} finally {
			if (ps != null) {
		 		try {
		        	ps.close();
		        } catch(Exception e) {}
		 	}
		} 
        getNameAndDesc(req,vo);
		mod.setActionData(vo);
		this.setAttribute(AdminConstants.ADMIN_MODULE_DATA, mod);
		log.debug("ModVO Admin" +this.getAttribute(AdminConstants.ADMIN_MODULE_DATA));
	 }
	 /*
	  * (non-Javadoc)
	  * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	  */
	public void retrieve(ActionRequest req) throws ActionException{		
		MobileCollectionVO vo = (MobileCollectionVO) req.getSession().getAttribute("vo");
		if(req.getParameter("cPage") != null && req.getParameter("cPage").equals("facade")){
			ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
			mod.setDataSize(getThemeSize(req));
			this.setAttribute(AdminConstants.ADMIN_MODULE_DATA, mod);
		} else {
			try{
				if(vo == null)
					vo = new MobileCollectionVO(); // Create a new VO if we need to
				if(req.getParameter("back") == null){
					vo.setActionId(((ModuleVO)attributes.get("moduleData")).getActionId());
					updateData(req,vo); // only update the data when the user clicks 'next'
				}
				if(getPageNumber(req) > 11 && vo.isEmailSent())
						vo = null;
			
				req.getSession().setAttribute("vo", vo); // Store the VO
			} catch(NullPointerException npe) {
				log.error("Error updating data: ", npe);
			}
		}
	}
	 
	/**
	 * Returns the total number of themes for the sjm mobile data collection portlet
	 * @param req
	 * @return Total number of themes
	 */
	public int getThemeSize(ActionRequest req) {
		int size = 0;
		themeTable = (String) getAttribute(Constants.CUSTOM_DB_SCHEMA) + "sjm_mobile_theme";
		StringBuffer sql = new StringBuffer();
		sql.append("select * from ").append(themeTable);
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				size++;
		}  catch(Exception e) {
			log.error("Error fetching number of entries in sjm_mobile_theme table", e);
		} finally {
			if (ps != null) {
		 		try {
		        	ps.close();
		        } catch(Exception e) {}
		 	}
		} 
		return size;
	}

	/**
	 * Gets the path to the theme files on the file system
	 * @param req
	 * @return
	 */
	public String getPath(ActionRequest req){
		StringBuilder path = new StringBuilder();
		path.append(StringUtil.checkVal(attributes.get("pathToBinary")));
		path.append((String)attributes.get("orgAlias") + req.getParameter("organizationId") + "/");
		path.append(req.getParameter("actionName") +"/" + PORTLET_ID + "/");
		path.append(req.getParameter("themeName") + "/");
		return path.toString(); 
	}
	 
	 /**
	  * corrects the pageNumber parameter
	  * @param req
	  * @return
	  */
	 public int getPageNumber(ActionRequest req){
		try{
			int page = Integer.parseInt(req.getParameter("pageNumber"));
			if(req.getParameter("back") != null){
				page = page - 1; // correct the page parameter
			}
			else if(req.getParameter("next") != null){
				page = page + 1; // correct the page parameter
			}
			return page;
		} catch(Exception e) {
			return -1;
		}
	 }
	 
	 /**
	  * Determines which data has been updated, based on the page number
	  * @param req
	  * @param vo
	  */
	 public void updateData(ActionRequest req, MobileCollectionVO vo){
		 int pageNumber = getPageNumber(req);
		 req.setAttribute("dbConn", dbConn);
		 req.setAttribute("custom", (String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		 req.setAttribute("pathToBinary", StringUtil.checkVal(attributes.get("pathToBinary")));
		 log.debug("Page Number: " + pageNumber);
		 fac = new CollectionActionFactory();
		 try{
			 if(fac.getAction(pageNumber) != null)
				 fac.getAction(pageNumber).update(req,vo);
		 } catch(Exception e) {
			 log.debug("Error getting an action from the CollectionActionFactory: ", e);
		 }
		 req.removeAttribute("dbConn");
		 req.removeAttribute("custom");
		 req.removeAttribute("pathToBinary");
	 }
	 
	 /**
	  * Updates the SJM_MOBILE_REGION table with max data
	  * @param req
	  */
	 public void updateRegion(ActionRequest req){
		 List<String> names = getRegions(req);
		 for(String s : names){
			 writeRegion(req,s);
		 }
	 }	 
	 /**
	  * Grabs the pre-exsisting regions from 
	  * @param req
	  */
	 public List<String> getRegions(ActionRequest req){
		List<String> names = new ArrayList<String>();
		StringBuffer sql = new StringBuffer();
		sql.append("select region_nm from ").append((String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("ans_sales_region");
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				names.add(rs.getString(1));
		} catch(Exception e) {
			log.error("Error fetching data from ans_sales_region", e);
		} finally {
			if (ps != null) {
		 		try {
		        	ps.close();
		        } catch(Exception e) {}
		 	}
		} 
		return names;
	 }
	 
	 /**
	  * Writes the 
	  * @param req
	  * @param s
	  */
	 public void writeRegion(ActionRequest req,String s){
		 StringBuffer sql = new StringBuffer();
		 boolean isInsert =  (StringUtil.checkVal(req.getParameter(SB_ACTION_ID))).length() == 0;
		 if(isInsert){
			 sql.append("insert into ").append((String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
			 sql.append("sjm_mobile_region(name, max_selected, action_id) values (?,?,?)");
		 } else {
			 sql.append("update ").append((String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
			 sql.append("sjm_mobile_region set name = ?, max_selected = ? where action_id = ? and name like ? ");
		 }
		 
		 PreparedStatement ps = null;
		 try{
			 ps = dbConn.prepareStatement(sql.toString());
			 ps.setString(1, s);
			 String num = req.getParameter(s);
			 if(num == null || num.equals(""))
				 num = "1";
			 ps.setInt(2, Integer.parseInt(num));
			 ps.setString(3, (String) req.getAttribute(SB_ACTION_ID));
			 if(!isInsert)
				 ps.setString(4, s);
			 ps.execute();
		 } catch(Exception e){
			 log.error("Error writing to sjm_mobile_region: ",e);
		 } finally {
			if (ps != null) {
				try {
					ps.close();
			 	} catch(Exception e) {}
			}
		}  
	 }
	/**
	 * Stores the files from the facade, as well as 
	 * @param req
	 */
	public void updateFacade(ActionRequest req){
		Object msg = getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);
		String path = getPath(req); // Get the filesystem path for where we're putting the file
		List<FilePartDataBean>files = req.getFiles();
		List<String> paths = new ArrayList<String>();
		for(FilePartDataBean file : files){
			if(file.isFileData() == true){
				try{
					SMBFileManager fm = new SMBFileManager();
					fm.setPath(path);
					fm.makeDir(true);
					fm.writeFiles(file.getFileData(), path, file.getFileName(), false, true);
					//we don't store the path to the file in the filesystem, we store the path that we need to access it from the url
					String urlPath = urlPath(req);
					paths.add(urlPath + fm.getFileName());
				} catch (Exception e) {
					log.error("Error writing files to the file system: ", e);
					msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
				}
			}
		}
		for(String s : req.getParameterValues(("locaction"))){//store the path information for the files we just wrote
			try{
				updateThemeTable(req, paths, s);
			} catch(Exception e){
			msg = getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
			}
		}
		
        ModuleVO mod = (ModuleVO) attributes.get(AdminConstants.ADMIN_MODULE_DATA);
		MobileCollectionVO vo = new MobileCollectionVO();		
		if(mod.getActionData() == null){ //if there isn't a VO for this action yet, we create one
			vo = new MobileCollectionVO();
			mod.setActionData(vo);
		}
		log.debug("ModVO:" + mod.getActionData()); 		
		util.adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH)); //redirect
		String url = req.getParameter("url");
    	req.setAttribute(Constants.REDIRECT_URL, url);
	}
	 
	/**
	 * Here we determine what the path is that we need to use for accessing the file from the URL(this is what we actually store in the DB)
	 * @param req
	 * @return
	 */
	public String urlPath(ActionRequest req){
		StringBuilder path = new StringBuilder();
   		path.append((String)attributes.get("orgAlias"));
		path.append(req.getParameter("organizationId") + "/");
		path.append(req.getParameter("actionName") +"/" + PORTLET_ID + "/");
		path.append(req.getParameter("themeName") + "/");
		return path.toString();
	}
	 
	/**
	 * Update the theme table
	 * @param req
	 * @param paths
	 */
	public void updateThemeTable(ActionRequest req, List<String> paths, String loc){
		Boolean isInsert = (req.getParameter("themeId").equals(""));
		StringBuilder sb = new StringBuilder();
		String themeId;
		
		if (isInsert) {  
			sb.append("insert into ").append(themeTable);
			sb.append("(location, color_1_front, color_1_back, color_2_front, color_2_back, ");
			sb.append("thumb_loc, theme_pdf, theme_name, theme_id ) values (?,?,?,?,?,?,?,?,?)");
			
			UUIDGenerator uuid = new UUIDGenerator();
			themeId = uuid.getUUID();
		} else{
			sb.append("update ").append(themeTable).append(" set location = ?, color_1_front = ?, ");
			sb.append("color_1_back = ?, color_2_front = ?, color_2_back = ?, thumb_loc = ?, ");
			sb.append("theme_pdf = ? , theme_name = ? where theme_id = ?");
			themeId = req.getParameter("themeId");
		}
		
     	PreparedStatement ps = null;
     	try{
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(1, loc);
			ps.setString(2, paths.get(1));
			ps.setString(3, paths.get(2));
			ps.setString(4, paths.get(3));
			ps.setString(5, paths.get(4));
			ps.setString(6, paths.get(5));
			ps.setString(7, paths.get(0));
			ps.setString(8, req.getParameter("themeName"));
			ps.setString(9, themeId);
			ps.execute();
		} catch(Exception sqle){
		 	log.error("Error updating theme table",sqle);
		} finally {
			if (ps != null) {
		 		try {
		        	ps.close();
		        } catch(Exception e) {}
		 	}
		} 
	}
	 
	/**
	 * Deletes all of the tables for the portlet, not the theme however
	 * @param req
	 */
	public void deleteTables(ActionRequest req){
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append(practiceTable).append(" where action_id = ?");
		
		PreparedStatement ps = null;		
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, (String)req.getParameter(SB_ACTION_ID));
			ps.execute();
		} catch(Exception e){
			log.error("Error deleting practice table: ", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
		deleteRegion(req);
	}

	/**
	 * deletes a given theme
	 * @param req
	 * @param keyId
	 */
	//because there is both data for the theme in the table as well as in the fs, this method is separated out
	public void deleteTheme(ActionRequest req, String keyId){
		deleteThemeFiles(req, keyId);
		deleteThemeTable(req, keyId);
	}
	 
	/**
	 * grabs the file location information from the db, and deletes the actual files
	 * @param req
	 * @param keyId
	 */
	public void deleteThemeFiles(ActionRequest req, String keyId){
		StringBuffer sql = new StringBuffer();
		sql.append("select thumb_loc, color_1_front, color_1_back, color_2_front, ");
		sql.append("color_2_back, theme_pdf from ").append(themeTable).append(" where ");
		sql.append("theme_id = ?");
		
		List<String> paths = new ArrayList<String>();		
		PreparedStatement ps = null;		
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, keyId);
			ResultSet rs = ps.executeQuery();
			while(rs.next()){
				paths.add((String)rs.getObject(1));
				paths.add((String)rs.getObject(2));
				paths.add((String)rs.getObject(3));
				paths.add((String)rs.getObject(4));
				paths.add((String)rs.getObject(5));
				paths.add((String)rs.getObject(6));
			}
		} catch(Exception e){
			log.error("Error grabbing the theme file paths from the db: ",e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
		
		int numOfCopies = getNumberOfSimilarThemes(req,paths);		
		if(numOfCopies == 1)
			removeFiles(req,paths); //basicly, we only delete the files if there's only one theme pointing to these files
	}
	
	/**
	 * Returns the number of duplicate themes(detects via the thumb_loc column). We use this to ensure that we only delete the files when the last theme that references said files is deleted.
	 * @param req
	 * @param paths
	 * @return number of themes with the same path for the thumb_loc column
	 */
	public int getNumberOfSimilarThemes(ActionRequest req, List<String> paths){
		//We need to find out if this is the last theme refering to these files
		StringBuffer sql = new StringBuffer();
		sql.append("select thumb_loc from ").append(themeTable).append(" where thumb_loc like ?");
				
		log.debug("Grabing file paths to delete files: " + sql.toString());
		List<String> numOfSimilarResults = new ArrayList<String>();
		PreparedStatement ps = null;		
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, paths.get(0));
			ResultSet rs = ps.executeQuery();
			while(rs.next())
				numOfSimilarResults.add((String) rs.getObject(1));
		} catch(Exception e){
			log.error("Error grabbing the theme file paths from the db: ",e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
		
		return numOfSimilarResults.size();
	}
	/**
	 * removes that particular theme's entry in the table
	 * @param req
	 * @param keyId
	 */
	public void deleteThemeTable(ActionRequest req, String keyId){
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append(themeTable).append(" where theme_id = ?");
		 
		PreparedStatement ps = null;			
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, keyId);
			ps.execute();
		} catch(Exception e){
			log.error("Error removing theme table: ", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
	}
	 
	/**
	 * removes all the files in the paths parameter
	 * @param req
	 * @param paths
	 */
	public void removeFiles(ActionRequest req, List<String> paths){
		for(String path : paths){
			try{ 
				SMBFileManager fm = new SMBFileManager();
				fm.deleteFile(StringUtil.checkVal(attributes.get("pathToBinary")) + path);
			} catch(Exception e){
				log.error("Error deleting files: ", e);
			}
		}
	}
	
	/**
	 * Deletes the region config for a specific sjm mobile data collection portlet
	 * @param req
	 */
	public void deleteRegion(ActionRequest req){
		StringBuffer sql = new StringBuffer();
		sql.append("delete from ").append((String) getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("sjm_mobile_region where action_id = ?");
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, (String)req.getParameter(SB_ACTION_ID));
			ps.execute();
		} catch(Exception e){
			log.error("Error removing region table: ", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
	}
	
	/**
	 * Grabs the portlet name and description from the sb_action table
	 * @param req
	 * @param vo
	 */
	public void getNameAndDesc(ActionRequest req, MobileCollectionVO vo){
		StringBuffer sql = new StringBuffer();
		sql.append("Select action_nm, action_desc from sb_action where action_id = ?");
		
		PreparedStatement ps = null;
		try{
			ps = dbConn.prepareStatement(sql.toString());
			ps.setString(1, req.getParameter(SBModuleAction.SB_ACTION_ID));
			ResultSet rs = ps.executeQuery();
			if(rs.next()){
				vo.setActionName(rs.getString(1));
				vo.setActionDesc(rs.getString(2));
			}
		} catch(Exception e){
			log.error("Error grabbing name and region from db: ", e);
		} finally {
			if (ps != null) {
				try {
					ps.close();
				} catch(Exception e) {}
			}
		}
	}
 }