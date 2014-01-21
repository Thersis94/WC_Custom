package com.fastsigns.cutover;

import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.PageVO;

/****************************************************************************
 * <b>Title</b>: FSPageVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 4, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class FSPageVO extends PageVO {
	public static final String ROOT_PATH = "/Franchise/";
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	// Member Variables
	private Map<String, Integer> roles = new HashMap<String, Integer>();
	private String pageContent = null;
	private int nodeLevel = 0;
	/**
	 * 
	 */
	public FSPageVO() {
		roles.put("Public", 0);
		roles.put("Registered User", 10);
		roles.put("Site Administrator ", 100);
	}

	/**
	 * @param rs
	 */
	public FSPageVO(ResultSet rs, int ctr, int id) {
		this();
		this.assignVals(rs, ctr, id);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void assignVals(ResultSet rs, int ctr, int id) {
		DBUtil db = new DBUtil();
		this.setFullPath(this.parsePath(db.getStringVal("parsepath", rs), id));
		this.setTitleName(db.getStringVal("DocumentPageTitle", rs));
		this.setPageId(db.getStringVal("NodeGuid", rs).replaceAll("-", ""));
		this.setMetaKeyword(db.getStringVal("DocumentPageTitle", rs));
		this.setMetaDesc(db.getStringVal("DocumentPageTitle", rs));
		this.setDisplayName(db.getStringVal("NodeName", rs));
		this.setAliasName(db.getStringVal("NodeAlias", rs).replace("-(1)",""));
		this.setPageContent(parsePageContent(db.getStringVal("DocumentContent", rs)));
		nodeLevel = db.getIntVal("NodeLevel", rs);
		
		if (db.getIntVal("NodeLevel", rs) == 2) this.setDefaultPage(true);
		this.setDefaultColumn(2);
		this.setFooterFlag(false);
		this.setNumberColumns(3);
		this.setParentPath("/");
		this.setOrder(ctr * 3);
		this.setVisibleFlg(0);
		this.setRoles(roles);
		
		if (StringUtil.checkVal(this.getTitleName()).length() == 0)
			this.setTitleName(this.getDisplayName());
	}

	
	public String parseAlias(String alias) {
		
		return null;
	}
	
	/**
	 * 
	 * @param data
	 * @return
	 */
	public String parsePageContent(String data) {
		
		StringBuffer content = new StringBuffer(data);
		int begin = content.indexOf("editableimage1") - 13;
		int end = content.indexOf("</webpart>") + 10;
		if (begin < 0 ) return data;
		
		if (end < begin) {
			System.out.println("Parse PAge COntent: " + begin + "|" + end + "|" + content.length());
			return content.toString();
		}
		content.delete(begin, end);
		if (content.length() < 30) return "";
		
		return content.toString();
	}
	
	/**
	 * Parses out the Un-needed information at the front of the URL
	 * @param path
	 * @param id
	 * @return
	 */
	public String parsePath(String path, int id) {
		/*
		if (! path.endsWith("/")) path += "/";
		String newPath = path.substring(path.indexOf("/", (ROOT_PATH + id).length())); 
		if ("/".equals(newPath)) newPath = "/home";
		if (newPath.endsWith("/")) newPath = newPath.substring(0, newPath.length() - 1);
		*/
		String newPath = "";
		System.out.println("Orig Path: " + path + "|" + id);
		if (path.indexOf("/", 2) > -1)
			newPath = path.substring(path.indexOf("/", 2));
		
		if (newPath.length() < 2 || newPath.equals("/" + id)) newPath = "/home";
		System.out.println("New Path: " + newPath);
		return newPath;
	}

	/**
	 * @return the pageContent
	 */
	public String getPageContent() {
		return pageContent;
	}

	/**
	 * @param pageContent the pageContent to set
	 */
	public void setPageContent(String pageContent) {
		this.pageContent = pageContent;
	}

	/**
	 * @return the roles
	 */
	public Map<String, Integer> getRoles() {
		return roles;
	}

	/**
	 * @param roles the roles to set
	 */
	public void setRoles(Map<String, Integer> roles) {
		this.roles = roles;
	}

	/**
	 * @return the nodeLevel
	 */
	public int getNodeLevel() {
		return nodeLevel;
	}

	/**
	 * @param nodeLevel the nodeLevel to set
	 */
	public void setNodeLevel(int nodeLevel) {
		this.nodeLevel = nodeLevel;
	}
}
