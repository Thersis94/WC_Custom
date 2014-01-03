package com.ansmed.sb.psp;

import java.sql.ResultSet;

import com.siliconmtn.db.DBUtil;

/****************************************************************************
 * <b>Title</b>: PspSiteTemplateVO.java<p/>
 * <b>Description</b>: Value object for a PSP site template.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2009<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Nov 18, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class PspSiteTemplateVO {
	
	private String themeName = null;
	private String themePath = null;
	private int themeColumns = 0;
	private int contColNo = 0;
	private int imgColNo = 0;
	private int logoColNo = 0;
	private int addrColNo = 0;
	private int menuColNo = 0;
	private int titlesFlg = 0;
	private int profileTypeFlg = 0;
	private int addrTypeFlg = 0;
	
	public PspSiteTemplateVO() {
		
	}
	
	public PspSiteTemplateVO(ResultSet rs) {
		setData(rs);
	}
	
	public void setData(ResultSet rs) {
		DBUtil db = new DBUtil();
		setThemeName(db.getStringVal("theme_nm", rs));
		themePath = db.getStringVal("theme_path", rs);
		themeColumns = db.getIntVal("theme_cols_no", rs);
		contColNo = db.getIntVal("ctnt_col_no", rs);
		imgColNo = db.getIntVal("img_col_no", rs);
		logoColNo = db.getIntVal("logo_col_no", rs);
		addrColNo = db.getIntVal("addr_col_no", rs);
		menuColNo = db.getIntVal("menu_col_no", rs);
		titlesFlg = db.getIntVal("titles_flg", rs);
		profileTypeFlg = db.getIntVal("profile_type_flg", rs);
		addrTypeFlg = db.getIntVal("addr_type_flg", rs);
		db = null;
	}

	/**
	 * @return the themeName
	 */
	public String getThemeName() {
		return themeName;
	}

	/**
	 * @param themeName the themeName to set
	 */
	public void setThemeName(String themeName) {
		// replace space with underscore, then store in uppercase
		this.themeName = themeName.replace(" ", "_").toUpperCase();
	}

	/**
	 * @return the themePath
	 */
	public String getThemePath() {
		return themePath;
	}

	/**
	 * @param themePath the themePath to set
	 */
	public void setThemePath(String themePath) {
		this.themePath = themePath;
	}

	/**
	 * @return the themeColumns
	 */
	public int getThemeColumns() {
		return themeColumns;
	}

	/**
	 * @param themeColumns the themeColumns to set
	 */
	public void setThemeColumns(int themeColumns) {
		this.themeColumns = themeColumns;
	}

	/**
	 * @return the contColNo
	 */
	public int getContColNo() {
		return contColNo;
	}

	/**
	 * @param contColNo the contColNo to set
	 */
	public void setContColNo(int contColNo) {
		this.contColNo = contColNo;
	}

	/**
	 * @return the imgColNo
	 */
	public int getImgColNo() {
		return imgColNo;
	}

	/**
	 * @param imgColNo the imgColNo to set
	 */
	public void setImgColNo(int imgColNo) {
		this.imgColNo = imgColNo;
	}

	/**
	 * @return the logoColNo
	 */
	public int getLogoColNo() {
		return logoColNo;
	}

	/**
	 * @param logoColNo the logoColNo to set
	 */
	public void setLogoColNo(int logoColNo) {
		this.logoColNo = logoColNo;
	}

	/**
	 * @return the addrColNo
	 */
	public int getAddrColNo() {
		return addrColNo;
	}

	/**
	 * @param addrColNo the addrColNo to set
	 */
	public void setAddrColNo(int addrColNo) {
		this.addrColNo = addrColNo;
	}

	/**
	 * @return the menuColNo
	 */
	public int getMenuColNo() {
		return menuColNo;
	}

	/**
	 * @param menuColNo the menuColNo to set
	 */
	public void setMenuColNo(int menuColNo) {
		this.menuColNo = menuColNo;
	}

	/**
	 * @return the titleFlg
	 */
	public int getTitlesFlg() {
		return titlesFlg;
	}

	/**
	 * @param titleFlg the titleFlg to set
	 */
	public void setTitlesFlg(int titlesFlg) {
		this.titlesFlg = titlesFlg;
	}

	/**
	 * @return the profileTypeFlg
	 */
	public int getProfileTypeFlg() {
		return profileTypeFlg;
	}

	/**
	 * @param profileTypeFlg the profileTypeFlg to set
	 */
	public void setProfileTypeFlg(int profileTypeFlg) {
		this.profileTypeFlg = profileTypeFlg;
	}

	/**
	 * @return the addrTypeFlg
	 */
	public int getAddrTypeFlg() {
		return addrTypeFlg;
	}

	/**
	 * @param addrTypeFlg the addrTypeFlg to set
	 */
	public void setAddrTypeFlg(int addrTypeFlg) {
		this.addrTypeFlg = addrTypeFlg;
	}
	
}
