package com.biomed.smarttrak.admin;

//java 8
import java.util.ArrayList;
import java.util.List;
//WC_Customs
import com.biomed.smarttrak.action.AdminControllerAction;
import com.biomed.smarttrak.vo.AccountVO;
import com.biomed.smarttrak.vo.UserVO;
//SMT Baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.user.HumanNameIntfc;
import com.siliconmtn.util.user.NameComparator;
//WebCrescendo
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ManageAuthorAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> manages displaying and updating author images
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Mar 10, 2017<p/>
 * @updates:
 ****************************************************************************/
public class ManageAuthorAction extends SimpleActionAdapter {

	public static final String LOAD_AUTHOR_LIST = "loadAuthorList";

	public ManageAuthorAction() {
		super();
	}

	public ManageAuthorAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.debug("manage author action retrieve called");

		if (!req.hasParameter(LOAD_AUTHOR_LIST)) return;

		loadAuthors(req);
		List<AccountVO> managers = (List<AccountVO>) req.getAttribute(AccountAction.MANAGERS);

		try {
			List<UserVO> users = processManagers(managers);
			new NameComparator().decryptNames((List<? extends HumanNameIntfc>)(List<?>)users, (String)getAttribute(Constants.ENCRYPT_KEY));
			putModuleData(users);
		} catch (DatabaseException e) {
			log.error("failed to load all managers as users", e);
		}
	}

	/**
	 * takes a list of managers and returns a list of users.
	 * @param managers
	 * @return
	 * @throws ActionException 
	 * @throws DatabaseException 
	 */
	private List<UserVO> processManagers(List<AccountVO> managers) throws DatabaseException {

		List<UserVO> users = new ArrayList<>();

		for (AccountVO manager : managers){
			UserVO uvo = new UserVO();
			uvo.setProfileId(manager.getOwnerProfileId());
			uvo.setManagerTitle(manager.getTitle());
			if (!users.contains(uvo)){
				users.add(uvo);
			}
		}

		ProfileManager sbpm =  ProfileManagerFactory.getInstance(getAttributes());
		sbpm.populateRecords(dbConn, users);
		return users;
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		log.debug("manage author action build called");
		processImage(req);
		sendRedirect(buildRedirectLink(req), "Sucessfully Updated Author Image", req);
	}

	/**
	 * gets the image from the form and updates the profile for the user
	 * @param req
	 */
	private void processImage(ActionRequest req) {
		//Set the profile image and remove from profile Data
		FilePartDataBean profileImg = req.getFile("profileImg");
		UserDataVO uvo = new UserDataVO(req);

		if(profileImg == null || uvo.getProfileId() == null) return;
		ProfileManager sbpm =  ProfileManagerFactory.getInstance(getAttributes());

		try {
			sbpm.updateProfileImage(uvo,profileImg,dbConn);
		} catch(Exception ide) {
			log.error("Error updating profile Image: " + ide.getMessage());
		}
	}

	/**
	 * builds a redirect link back to the manage author page
	 * @param req
	 */
	private String buildRedirectLink(ActionRequest req) {
		String actionType = req.getParameter(AdminControllerAction.ACTION_TYPE);
		String loadAuthorList = req.getParameter(LOAD_AUTHOR_LIST);
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);

		StringBuilder url = new StringBuilder(200);
		url.append(page.getFullPath());
		if (!StringUtil.isEmpty(actionType)) url.append("?actionType=").append(actionType);
		if (!StringUtil.isEmpty(loadAuthorList) && !StringUtil.isEmpty(actionType)) url.append("&loadAuthorList=").append(loadAuthorList);

		return url.toString();
	}

	/**
	 * loads a list of to the request.
	 * @param req
	 * @throws ActionException 
	 */
	private void loadAuthors(ActionRequest req) throws ActionException {
		log.debug("loaded authors");

		AccountAction aa = new AccountAction();
		aa.setActionInit(actionInit);
		aa.setAttributes(attributes);
		aa.setDBConnection(dbConn);
		aa.loadManagerList(req, (String)getAttributes().get(Constants.CUSTOM_DB_SCHEMA));
	}
}
