package com.fastsigns.action.franchise.vo;

// JDK 1.6.0
import java.io.Serializable;
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.db.DBUtil;


/****************************************************************************
 * <b>Title</b>: ButtonVO.java <p/>
 * <b>Project</b>: SB_FastSigns <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Nov 19, 2010<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class ButtonVO implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	// Parameters
	private String franchiseId = null;
	private String buttonId = null;
	private String buttonName = null;
	private String buttonUrl = null;
	private String buttonImage = null;
	private String altText = null;
	private String title = null;
	private String family = null;
	private int order = 0;
	private String cssClass = null;
	/**
	 * 
	 */
	public ButtonVO() {
		
	}
	
	/**
	 * 
	 * @param rs
	 */
	public ButtonVO(ResultSet rs) {
		this.assignValues(rs);
	}
	
	/**
	 * 
	 * @param rs
	 */
	public void assignValues(ResultSet rs) {
		DBUtil db = new DBUtil();
		buttonName = db.getStringVal("button_nm", rs);
		buttonId = db.getStringVal("center_button_id", rs);
		buttonUrl = db.getStringVal("button_url", rs);
		buttonImage = db.getStringVal("button_img_path", rs);
		altText = db.getStringVal("alt_text_desc", rs);
		title = db.getStringVal("title_nm", rs);
		order = db.getIntVal("order_no", rs);
		franchiseId = db.getStringVal("franchise_id", rs);
		family = db.getStringVal("family", rs);
		cssClass = db.getStringVal("css_class", rs);
	}

	public String getButtonName() {
		return buttonName;
	}
	
	public void setButtonName(String buttonName) {
		this.buttonName = buttonName;
	}

	/**
	 * Added a parsed for the dealer location id so that the button can pass
	 * the franchise id in the URL
	 * @return the buttonUrl
	 */
	public String getButtonUrl() {
		StringBuilder sb = new StringBuilder(buttonUrl);
		int index = sb.indexOf("${DLID}");
		if (index > -1)
			sb.replace(index, index + 7, franchiseId);
		
		return sb.toString();
	}
	
	public void setButtonUrl(String buttonUrl) {
		this.buttonUrl = buttonUrl;
	}
	
	public String getButtonImage() {
		return buttonImage;
	}
	
	public void setButtonImage(String buttonImage) {
		this.buttonImage = buttonImage;
	}
	
	public String getAltText() {
		return altText;
	}
	
	public void setAltText(String altText) {
		this.altText = altText;
	}
	
	public String getTitle() {
		return title;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}
	
	public int getOrder() {
		return order;
	}
	
	public void setOrder(int order) {
		this.order = order;
	}
	
	public String getButtonId() {
		return buttonId;
	}
	
	public void setButtonId(String buttonId) {
		this.buttonId = buttonId;
	}
	
	public void setFamily(String family) {
		this.family = family;
	}

	public String getFamily() {
		return family;
	}

	public void setCssClass(String cssClass) {
		this.cssClass = cssClass;
	}

	public String getCssClass() {
		return cssClass;
	}

}
