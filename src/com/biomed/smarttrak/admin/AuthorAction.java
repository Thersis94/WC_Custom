package com.biomed.smarttrak.admin;

//jdk 1.8
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//smt base libs
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * Title: AuthorAction.java <p/>
 * Project: WC_Custom <p/>
 * Description: Wrapper action around AccountAction and extends AbstractTreeAction, 
 * handles Author related responsibilities for actions. <p/>
 * Copyright: Copyright (c) 2017<p/>
 * Company: Silicon Mountain Technologies<p/>
 * @author Devon Franklin
 * @version 1.0
 * @since May 8, 2017
 ****************************************************************************/
public class AuthorAction extends SimpleActionAdapter {

	/**
	 * Default no-arg constructor
	 */
	public AuthorAction(){
		super();
	}

	/**
	 * Takes ActionInitVO for initialization
	 * @param init
	 */
	public AuthorAction(ActionInitVO init){
		super(init);
	}


	/**
	 * loads a list of authors to the request. Defaults to not load author titles
	 * @param req
	 * @throws ActionException
	 */
	protected void loadAuthors(ActionRequest req) {
		loadAuthors(req, false);
	}

	/**
	 * loads a list of authors to the request.
	 * @param req
	 * @param loadTitles
	 * @throws ActionException 
	 */
	protected void loadAuthors(ActionRequest req, boolean loadTitles) {
		log.debug("loaded authors");
		AccountAction aa = new AccountAction();
		aa.setActionInit(actionInit);
		aa.setAttributes(attributes);
		aa.setDBConnection(dbConn);
		aa.loadManagerList(req, (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA), loadTitles, null, Convert.formatBoolean(req.getParameter("includeInactive")));
	}

	/**
	 * Helper method that builds map of User Titles keyed by profileId.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, String> loadAuthorTitles(String profileId) {

		//Generate ActionRequest for Retrieving Author Data.
		ActionRequest req = new ActionRequest();

		//Some down stream code requires ModuleData to be present.
		if(getAttribute(Constants.MODULE_DATA) == null) {
			setAttribute(Constants.MODULE_DATA, new ModuleVO());
		}

		//Some down stream code requires SiteData to be present.
		SiteVO site = new SiteVO();
		site.setOrganizationId(AdminControllerAction.BIOMED_ORG_ID);
		req.setAttribute(Constants.SITE_DATA, site);

		//Map to hold author Titles in <ProfileId, Title> pairs.
		Map<String, String> authorTitles = new HashMap<>();

		try {
			//Get Authors.
			loadAuthors(req);
			List<AccountVO> authors = (List<AccountVO>) req.getAttribute(AccountAction.MANAGERS);

			//Action to retrieve AccountUsers Data.
			AccountUserAction aua = new AccountUserAction(this.actionInit);
			aua.setAttributes(attributes);
			aua.setDBConnection(dbConn);

			//Loop over Authors and load Profile Data for them.
			for(Object o : authors) {
				AccountVO a = (AccountVO)o;
				if(StringUtil.isEmpty(profileId) || a.getOwnerProfileId().equals(profileId)) {
					List<UserVO> authorData = aua.loadAccountUsers(req, a.getOwnerProfileId());

					/*
					 * If authorData is found for the profile, store their title on
					 * the map.
					 */
					if(authorData != null && !authorData.isEmpty()) {
						UserVO u = authorData.get(0);
						authorTitles.put(a.getOwnerProfileId(), u.getTitle());
					}
				}
			}
		} catch(Exception e) {
			log.error("There was a problem Loading Author Titles.", e);
		}
		return authorTitles;
	}
}