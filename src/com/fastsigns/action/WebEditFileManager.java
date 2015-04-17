package com.fastsigns.action;

import com.siliconmtn.action.ActionInitVO;
import com.smt.sitebuilder.action.file.FileManagerAction;

/****************************************************************************
 * <b>Title</b>: WebEditFileManager,java <p/>
 * <b>Description: </b> Custom wrapper for the file manager that allows Fastsigns
 * to bypass the standard approval process in thier webedit tool.<p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 27, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class WebEditFileManager extends FileManagerAction {
	
	/**
	 * 
	 */
	public WebEditFileManager() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public WebEditFileManager(ActionInitVO actionInit) {
		super(actionInit);
	}

}
