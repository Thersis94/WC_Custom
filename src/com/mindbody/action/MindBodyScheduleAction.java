package com.mindbody.action;

import java.util.Date;
import java.util.Map;

import com.mindbody.MindBodyClassApi;
import com.mindbody.MindBodyClassApi.ClassDocumentType;
import com.mindbody.MindBodyClientApi;
import com.mindbody.security.MindBodyUserVO;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.classes.MindBodyAddClientsToClassConfig;
import com.mindbody.vo.classes.MindBodyGetClassScheduleConfig;
import com.mindbody.vo.classes.MindBodyGetClassesConfig;
import com.mindbody.vo.classes.MindBodyRemoveClientsFromClassesConfig;
import com.mindbody.vo.clients.MBClientServiceVO;
import com.mindbody.vo.clients.MindBodyGetClientServicesConfig;
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
 * <b>Title:</b> MindBodyScheduleAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages MindBody Schedule Interactions in the System.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 23, 2017
 ****************************************************************************/
public class MindBodyScheduleAction extends SimpleActionAdapter {

	public static final String MB_CLASS_SCHEDULE_ID = "mbClassScheduleId";
	public static final String MB_START_DT = "MBStartDt";
	public static final String MB_END_DT = "MBEndDt";
	public static final String MB_CLASS_ID = "MBClassId";
	public static final String MB_CLIENT_SERVICE_ID = "MBClientServiceId";
	public static final String MB_SERVICES = "mbServices";
	public static final String NO_SERVICE_REDIR_URL = "noServiceRedirectURL";

	public MindBodyScheduleAction() {
		super();
	}

	public MindBodyScheduleAction(ActionInitVO init) {
		super(init);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		if(req.hasParameter("getClasses") || req.hasParameter(MB_CLASS_ID)) {
			putModuleData(getClasses(req));
		} else {
			putModuleData(getClassSchedules(req));
		}

	}

	/**
	 * Manages MindBody Build type requests for adding and removing a client
	 * from a class.
	 */
	@Override
	public void build(ActionRequest req) throws ActionException {
		ClassDocumentType callType = getDocumentType(req.getParameter("callType"));

		switch(callType) {
			case ADD_CLIENTS_TO_CLASS:
				checkAddToClass(req);
				break;
			case REMOVE_CLIENTS_FROM_CLASS:
				removeFromClass(req);
				break;
			default:
				break;
		}
	}

	/**
	 * Retrieve the Services a Client has assigned to their account for a 
	 * given ClassId.  Users can only sign up for a class that they have
	 * a corresponding clientService for.
	 * @param req
	 * @return
	 */
	private MindBodyResponseVO getClientServices(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		MindBodyUserVO user = (MindBodyUserVO)req.getSession().getAttribute(Constants.USER_DATA);

		MindBodyGetClientServicesConfig conf = new MindBodyGetClientServicesConfig(MindBodyUtil.buildSourceCredentials(config));
		conf.setClassId(req.getIntegerParameter(MB_CLASS_ID));
		conf.addClientId(user.getClientId());

		return new MindBodyClientApi().getDocument(conf);

	}

	/**
	 * Retrieve the Class Schedule Objects within a given date range.  
	 * @param req
	 * @return
	 */
	private MindBodyResponseVO getClassSchedules(ActionRequest req) {
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
		MindBodyGetClassScheduleConfig conf = new MindBodyGetClassScheduleConfig(MindBodyUtil.buildSourceCredentials(config), false);
		conf.setStartDt(startDt);
		conf.setEndDt(endDt);

		//Filter by Trainer id if present.
		if(req.hasParameter(MindBodyTrainerAction.MB_TRAINER_ID)) {
			conf.addStaffId(req.getLongParameter(MindBodyTrainerAction.MB_TRAINER_ID));
		}

		//Filter by scheduleId if present.
		if(req.hasParameter(MB_CLASS_SCHEDULE_ID)) {
			conf.addClassScheduleId(req.getIntegerParameter(MB_CLASS_SCHEDULE_ID));
		}

		return api.getAllDocuments(conf);
	}

	/**
	 * Add a User to a class using the given clientServiceId and data
	 * on the request.
	 * @param req
	 * @param user
	 */
	private void addToClass(ActionRequest req, long clientServiceId) {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		MindBodyClassApi api = new MindBodyClassApi();

		MindBodyAddClientsToClassConfig conf = new MindBodyAddClientsToClassConfig(MindBodyUtil.buildSourceCredentials(config), MindBodyUtil.buildStaffCredentials(config));
		conf.addClassId(Integer.parseInt(req.getParameter(MB_CLASS_ID)));
		conf.addClientId(user.getProfileId());
		conf.setClientServiceId((int) clientServiceId);

		api.getDocument(conf);
	}

	/**
	 * Attempt to add a user to a class.  Performs check for clientService
	 * server side to prevent accidental signup when they don't have one.
	 * Redirects to NO_SERVICE_REDIR_URL if no service found.
	 * @param req
	 */
	private void checkAddToClass(ActionRequest req) {
		MindBodyResponseVO service = getClientServices(req);
		long serviceId = 0;
		if(service.isValid() && service.getResultCount() > 0) {
			MBClientServiceVO s = (MBClientServiceVO) service.getResults().get(0);
			serviceId = s.getId();
		}

		if(serviceId > 0) {
			addToClass(req, serviceId);
		} else if(req.hasParameter(NO_SERVICE_REDIR_URL)) {
			sendRedirect(req.getParameter(NO_SERVICE_REDIR_URL), "Please Purchase a Service.", req);
		}
	}

	/**
	 * Removes a user from a class.
	 * @param req
	 * @param user
	 */
	private void removeFromClass(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		MindBodyClassApi api = new MindBodyClassApi();

		MindBodyRemoveClientsFromClassesConfig conf = new MindBodyRemoveClientsFromClassesConfig(MindBodyUtil.buildSourceCredentials(config));
		conf.addClientId(user.getProfileId());
		conf.addClassId(Integer.parseInt(req.getParameter(MB_CLASS_ID)));

		api.getDocument(conf);
	}


	/**
	 * Retrieves all Classes for a given Date Window. 
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

		//Filter by classId if present for details page.
		if(req.hasParameter(MB_CLASS_ID)) {
			conf.addClassId(req.getIntegerParameter(MB_CLASS_ID));
		}

		//Filter by trainerId if present.
		if(req.hasParameter(MindBodyTrainerAction.MB_TRAINER_ID)) {
			conf.addStaffId(req.getLongParameter(MindBodyTrainerAction.MB_TRAINER_ID));
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