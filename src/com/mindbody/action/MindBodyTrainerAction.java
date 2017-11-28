package com.mindbody.action;

import java.util.Map;

import com.mindbody.MindBodyStaffApi;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.staff.MindBodyGetStaffConfig;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MindBodyTrainerAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages MindBody Staff interactions with regards
 * to trainers.  Retrieves a list of Trainers from the MindBody System.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 28, 2017
 ****************************************************************************/
public class MindBodyTrainerAction extends SimpleActionAdapter {

	public static final String MB_TRAINER_ID = "MBTrainerId";
	/**
	 * 
	 */
	public MindBodyTrainerAction() {
		super();
	}

	/**
	 * @param arg0
	 */
	public MindBodyTrainerAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void retrieve(ActionRequest req) {
		putModuleData(getTrainers(req));
	}

	/**
	 * @param req
	 * @return
	 */
	private MindBodyResponseVO getTrainers(ActionRequest req) {
		Map<String, String> config = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getSiteConfig();

		MindBodyGetStaffConfig conf = new MindBodyGetStaffConfig(MindBodyUtil.buildSourceCredentials(config), MindBodyUtil.buildStaffCredentials(config));
		conf.addClassInstructorFilter();
		conf.addStaffViewableFilter();

		//Filter by TrainerId if present.
		if(req.hasParameter(MB_TRAINER_ID)) {
			conf.addStaffId(req.getLongParameter(MB_TRAINER_ID));
		}

		return new MindBodyStaffApi().getAllDocuments(conf);
	}
}
