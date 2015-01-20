/**
 * 
 */
package com.fastsigns.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBModuleVO;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:InstagramAction.java<p/>
 * <b>Description: Displays an image from an Instagram post.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Erik Wingo
 * @version 1.0
 * @since Jan 8, 2015
 ****************************************************************************/
public class InstagramAction extends SimpleActionAdapter {

	public static final String URL_DELIMITER = "~";
	public static final String URL_FIELD = "attrib1Text";
	public static final String PARAM_NAME = "urlList";
	
	/**
	 * Default Constructor
	 */
	public InstagramAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public InstagramAction(ActionInitVO arg0) {
		super(arg0);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException{
		//Id used by the adapter was null, so setting it here before calling super
		req.setParameter(InstagramAction.SB_ACTION_ID, actionInit.getActionId());
		super.retrieve(req);
		
		//Get the action data set by the super class
		ModuleVO modVO = (ModuleVO) this.getAttribute(Constants.MODULE_DATA);
		SBModuleVO actionData = (SBModuleVO) modVO.getActionData();
		
		//Parse URL's, and add them to the request as a list
		String urlString = (String) actionData.getAttribute(SBModuleVO.ATTRIBUTE_1);
		List<String> urlList = parseUrlString(urlString);
		
		actionData.setAttribute(PARAM_NAME, urlList);
		this.putModuleData(actionData);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void update(SMTServletRequest req) throws ActionException{
		//get field names
		List<String> paramList = Collections.list(req.getParameterNames());
		//Initialize StringBuilder with rough estimate of url length * number of urls in the list
		StringBuilder urlString = new StringBuilder(paramList.size()*20);
		
		//Create a delimited list of instagram url's
		for (String s : paramList){
			if (s.startsWith(URL_FIELD)){
				urlString.append(req.getParameter(s));
				urlString.append(URL_DELIMITER);
			}
		}
		req.setParameter(URL_FIELD, urlString.toString());
		super.update(req);
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(SMTServletRequest req) throws ActionException{
		super.list(req);
		
		//Get the action data set by the super class
		ModuleVO modVO = (ModuleVO) this.getAttribute(AdminConstants.ADMIN_MODULE_DATA);
		SBModuleVO actionData = (SBModuleVO) modVO.getActionData();
		
		if (actionData != null){
			//Parse URL's, and add them to the request as a list
			String urlString = (String) actionData.getAttribute(SBModuleVO.ATTRIBUTE_1);
			List<String> urlList = parseUrlString(urlString);
			
			actionData.setAttribute(PARAM_NAME, urlList);
			this.putModuleData(actionData,1,true);
		}
	}
	
	/**
	 * Parse the urls stored in the attribute 1 field
	 * @param urlString
	 * @return URL list
	 */
	private List<String> parseUrlString(String urlString){
		List<String> urlList = new ArrayList<>();
		
		if (!StringUtil.checkVal(urlString).isEmpty()){
			//Split the url string with the URL_DELIMITER
			urlList.addAll(Arrays.asList(urlString.split(URL_DELIMITER)));
		}
		
		return urlList;
	}
}
