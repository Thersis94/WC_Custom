package com.mindbody.action;

import java.util.Date;
import java.util.Map;

import com.mindbody.MindBodyClassApi;
import com.mindbody.MindBodyClassApi.ClassDocumentType;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.classes.MindBodyAddClientsToClassConfig;
import com.mindbody.vo.classes.MindBodyGetClassScheduleConfig;
import com.mindbody.vo.classes.MindBodyGetClassesConfig;
import com.mindbody.vo.classes.MindBodyRemoveClientsFromClassesConfig;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MindBodyClassAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Process Mindbody Class Interactions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 20, 2017
 ****************************************************************************/
public class MindBodyClassAction extends SBActionAdapter {

	public static final String MB_START_DT = "MBStartDt";
	public static final String MB_END_DT = "MBEndDt";
	public static final String MB_CLASS_ID = "MBClassId";
	public static final String MB_CLIENT_SERVICE_ID = "MBClientServiceId";

	/**
	 * 
	 */
	public MindBodyClassAction() {
	}


	/**
	 * @param actionInit
	 */
	public MindBodyClassAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		ClassDocumentType callType = getDocumentType(req.getParameter("callType"));

		switch(callType) {
			case GET_CLASS_SCHEDULE:
				putModuleData(getClassSchedules(req));
				break;
			case GET_CLASSES:
				putModuleData(getClasses(req));
				break;
			case GET_CLASS_DESC:
				putModuleData(getClassDescriptions(req));
				break;
			default:
				break;
		}
	}

	/**
	 * @param req
	 * @return
	 */
	private MindBodyResponseVO getClassSchedules(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getSession().getAttribute(Constants.SITE_DATA)).getSiteConfig();
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
//		MindBodyGetClassScheduleConfig conf = new MindBodyGetClassScheduleConfig(MindBodyUtil.getSourceCredentials(config), true);
//		conf.setStartDt(startDt);
//		conf.setEndDt(endDt);
//		return api.getDocument(conf);
		return null;
	}


	/**
	 * @param req
	 * @return
	 */
	private MindBodyResponseVO getClasses(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getSession().getAttribute(Constants.SITE_DATA)).getSiteConfig();
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
		MindBodyGetClassesConfig conf = new MindBodyGetClassesConfig(MindBodyUtil.getSourceCredentials(config));
		conf.setStartDt(startDt);
		conf.setEndDt(endDt);
		//return api.getDocument(conf);
		return null;
	}


	/**
	 * @param req
	 * @return
	 */
	private MindBodyResponseVO getClassDescriptions(ActionRequest req) {
		return null;
	}


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
		Map<String, String> config = ((SiteVO)req.getSession().getAttribute(Constants.SITE_DATA)).getSiteConfig();
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		MindBodyClassApi api = new MindBodyClassApi();

		MindBodyAddClientsToClassConfig conf = new MindBodyAddClientsToClassConfig(MindBodyUtil.getSourceCredentials(config), MindBodyUtil.getStaffCredentials(config));
		conf.addClassId(Integer.parseInt(req.getParameter(MB_CLASS_ID)));
		conf.addClientId(user.getProfileId());
		conf.setClientServiceId(Integer.parseInt(MB_CLIENT_SERVICE_ID));

		//api.getDocument(conf);
	}


	/**
	 * @param req
	 * @param user
	 */
	private void removeFromClass(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getSession().getAttribute(Constants.SITE_DATA)).getSiteConfig();
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		MindBodyClassApi api = new MindBodyClassApi();

		MindBodyRemoveClientsFromClassesConfig conf = new MindBodyRemoveClientsFromClassesConfig(MindBodyUtil.getSourceCredentials(config));
		conf.addClientId(user.getProfileId());
		conf.addClassId(Integer.parseInt(req.getParameter(MB_CLASS_ID)));

		//api.getDocument(conf);
	}


	/**
	 * Get The ClientDocumentType off the request.
	 * @return
	 * @throws ActionException 
	 */
	private ClassDocumentType getDocumentType(String callType) throws ActionException {
		if(!StringUtil.isEmpty(callType)) {
			try {
				//return ClassDocumentType.valueOf(callType);
				return null;
			} catch(Exception e) {
				log.error("Could not determine Class CallType.");
				throw new ActionException("Could not determine Class CallType.");
			}
		}

		throw new ActionException("Class CallType not passed.");
	}
}