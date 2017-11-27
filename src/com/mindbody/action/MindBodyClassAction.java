package com.mindbody.action;

import java.util.Date;
import java.util.Map;

import com.mindbody.MindBodyClassApi;
import com.mindbody.MindBodyClassApi.ClassDocumentType;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.classes.MindBodyAddClientsToClassConfig;
import com.mindbody.vo.classes.MindBodyGetClassesConfig;
import com.mindbody.vo.classes.MindBodyRemoveClientsFromClassesConfig;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MindBodyClassAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages Mindbody Class Interactions in the System.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MindBodyClassAction extends SimpleActionAdapter {

	public static final String MB_START_DT = "MBStartDt";
	public static final String MB_END_DT = "MBEndDt";
	public static final String MB_CLASS_ID = "MBClassId";
	public static final String MB_CLIENT_SERVICE_ID = "MBClientServiceId";

	/**
	 * 
	 */
	public MindBodyClassAction() {
		super();
	}


	/**
	 * @param actionInit
	 */
	public MindBodyClassAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		MindBodyResponseVO resp = getClasses(req); 
		putModuleData(resp);
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#build(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ClassDocumentType callType = getDocumentType(req.getParameter("callType"));

		switch(callType) {
			case ADD_CLIENTS_TO_CLASS:
				addToClass(req);
				break;
			case REMOVE_CLIENTS_FROM_CLASS:
				removeFromClass(req);
				break;
			default:
				break;
		}
	}


	/**
	 * @param req
	 * @param user
	 */
	private void addToClass(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		MindBodyClassApi api = new MindBodyClassApi();

		MindBodyAddClientsToClassConfig conf = new MindBodyAddClientsToClassConfig(MindBodyUtil.buildSourceCredentials(config), MindBodyUtil.buildStaffCredentials(config));
		conf.addClassId(Integer.parseInt(req.getParameter(MindBodyClassAction.MB_CLASS_ID)));
		conf.addClientId(user.getProfileId());
		conf.setClientServiceId(Integer.parseInt(MindBodyClassAction.MB_CLIENT_SERVICE_ID));

		api.getDocument(conf);
	}


	/**
	 * @param req
	 * @param user
	 */
	private void removeFromClass(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		MindBodyClassApi api = new MindBodyClassApi();

		MindBodyRemoveClientsFromClassesConfig conf = new MindBodyRemoveClientsFromClassesConfig(MindBodyUtil.buildSourceCredentials(config));
		conf.addClientId(user.getProfileId());
		conf.addClassId(Integer.parseInt(req.getParameter(MindBodyClassAction.MB_CLASS_ID)));

		api.getDocument(conf);
	}


	/**
	 * @param req
	 * @return
	 */
	private MindBodyResponseVO getClasses(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		MindBodyClassApi api = new MindBodyClassApi();

		Date startDt;
		Date endDt;
		if(req.hasParameter(MB_START_DT)) {
			startDt = Convert.parseDateUnknownPattern(req.getParameter(MB_START_DT));
		} else {
			startDt = Convert.getFirstOfMonth();
		}
		if(req.hasParameter(MB_END_DT)) {
			endDt = Convert.parseDateUnknownPattern(req.getParameter(MB_END_DT));
		} else {
			endDt = Convert.getLastOfMonth();
		}

		MindBodyGetClassesConfig conf = new MindBodyGetClassesConfig(MindBodyUtil.buildSourceCredentials(config));
		conf.setStartDt(startDt);
		conf.setEndDt(endDt);
		if(req.hasParameter(MB_CLASS_ID)) {
			conf.addClassId(req.getIntegerParameter(MB_CLASS_ID));
		}
		
		if(req.hasParameter("programId")) {
			conf.addProgramId(req.getIntegerParameter("programId"));
		}

		conf.setUseSchedulingWindow(false);
		return api.getAllDocuments(conf);
	}


	/**
	 * Get The ClientDocumentType off the request.
	 * @return
	 * @throws ActionException 
	 */
	private ClassDocumentType getDocumentType(String callType) throws ActionException {
		if(!StringUtil.isEmpty(callType)) {
			try {
				return ClassDocumentType.valueOf(callType);
			} catch(Exception e) {
				log.error("Could not determine Class CallType.");
				throw new ActionException("Could not determine Class CallType.");
			}
		}

		throw new ActionException("Class CallType not passed.");
	}
}