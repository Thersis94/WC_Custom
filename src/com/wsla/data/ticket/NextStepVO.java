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
	private String prompt;
	private String buttonUrl;
	private boolean needsReload;
	
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
			setPrompt(bundle.getString("wsla.ticket.nextStep." + status.name()));
		} catch (MissingResourceException e) {
			setPrompt("");
		}
		
		setButtonUrl("");
		setNeedsReload(false);
	}

	/**
	 * @return the prompt
	 */
	public String getPrompt() {
		return prompt;
	}

	/**
	 * @param prompt the prompt to set
	 */
	public void setPrompt(String prompt) {
		this.prompt = prompt;
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
	 * @return the needsReload
	 */
	public boolean isNeedsReload() {
		return needsReload;
	}

	/**
	 * @param needsReload the needsReload to set
	 */
	public void setNeedsReload(boolean needsReload) {
		this.needsReload = needsReload;
	}
}

