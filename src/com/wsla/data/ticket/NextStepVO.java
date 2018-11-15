package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: NextStepVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Holds data regarding the next step for a service order.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Tim Johnson
 * @version 3.0
 * @since Nov 8, 2018
 * @updates:
 ****************************************************************************/
public class NextStepVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 7595095552093529941L;
	
	// Member Variables
	private String promptText;
	private String buttonUrl;
	private String buttonName;
	private boolean needsReloadFlag;
	
	/**
	 * 
	 */
	public NextStepVO() {
		super();
	}

	/**
	 * @param req
	 */
	public NextStepVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public NextStepVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * @param status
	 * @param bundle
	 */
	public NextStepVO(StatusCode status, ResourceBundle bundle) {
		try {
			setPromptText(bundle.getString("wsla.ticket.nextStep." + status.name()));
		} catch (MissingResourceException e) {
			setPromptText("");
		}
		
		setButtonUrl("");
		setButtonName("");
		setNeedsReloadFlag(false);
	}

	/**
	 * @return the promptText
	 */
	public String getPromptText() {
		return promptText;
	}

	/**
	 * @param promptText the promptText to set
	 */
	public void setPromptText(String promptText) {
		this.promptText = promptText;
	}

	/**
	 * @return the buttonUrl
	 */
	public String getButtonUrl() {
		return buttonUrl;
	}

	/**
	 * @param buttonUrl the buttonUrl to set
	 */
	public void setButtonUrl(String buttonUrl) {
		this.buttonUrl = buttonUrl;
	}

	/**
	 * @return the buttonName
	 */
	public String getButtonName() {
		return buttonName;
	}

	/**
	 * @param buttonName the buttonName to set
	 */
	public void setButtonName(String buttonName) {
		this.buttonName = buttonName;
	}

	/**
	 * @return the needsReloadFlag
	 */
	public boolean isNeedsReloadFlag() {
		return needsReloadFlag;
	}

	/**
	 * @param needsReloadFlag the needsReloadFlag to set
	 */
	public void setNeedsReloadFlag(boolean needsReloadFlag) {
		this.needsReloadFlag = needsReloadFlag;
	}
}

