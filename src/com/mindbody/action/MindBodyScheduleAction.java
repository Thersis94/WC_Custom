package com.mindbody.action;

import java.util.Date;
import java.util.Map;

import com.mindbody.MindBodyClassApi;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.classes.MindBodyGetClassScheduleConfig;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
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
		if(req.hasParameter("getClasses") || req.hasParameter(MindBodyClassAction.MB_CLASS_ID)) {
			MindBodyClassAction mbca = new MindBodyClassAction(this.actionInit);
			mbca.setAttributes(attributes);
			mbca.setDBConnection(dbConn);
			mbca.retrieve(req);
		} else {
			putModuleData(getClassSchedules(req));
		}

	}

	/**
	 * @param req
	 * @return
	 */
	private MindBodyResponseVO getClassSchedules(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();
		MindBodyClassApi api = new MindBodyClassApi();

		Date startDt;
		Date endDt;
		if(req.hasParameter(MindBodyClassAction.MB_START_DT)) {
			startDt = Convert.parseDateUnknownPattern(req.getParameter(MindBodyClassAction.MB_START_DT));
		} else {
			startDt = Convert.getFirstOfMonth();
		}
		if(req.hasParameter(MindBodyClassAction.MB_END_DT)) {
			endDt = Convert.parseDateUnknownPattern(req.getParameter(MindBodyClassAction.MB_END_DT));
		} else {
			endDt = Convert.getLastOfMonth();
		}
		MindBodyGetClassScheduleConfig conf = new MindBodyGetClassScheduleConfig(MindBodyUtil.buildSourceCredentials(config), false);
		conf.setStartDt(startDt);
		conf.setEndDt(endDt);
		if(req.hasParameter("scheduleId")) {
			conf.addClassScheduleId(req.getIntegerParameter("scheduleId"));
		}
		
		return api.getAllDocuments(conf);
	}
}