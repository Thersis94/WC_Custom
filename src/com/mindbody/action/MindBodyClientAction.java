package com.mindbody.action;

import java.util.List;

import com.mindbody.MindBodyClientApi.ClientDocumentType;
import com.mindbody.util.ClientApiUtil;
import com.mindbody.util.MindBodyUtil;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MindBodyAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action for building Client related requests for the
 * MindBody Client Apis.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 19, 2017
 ****************************************************************************/
public class MindBodyClientAction extends SBActionAdapter {

	/**
	 * 
	 */
	public MindBodyClientAction() {
	}


	/**
	 * @param actionInit
	 */
	public MindBodyClientAction(ActionInitVO actionInit) {
		super(actionInit);
	}

	public void retrieve(ActionRequest req) throws ActionException {
		ClientDocumentType callType = getDocumentType(req.getParameter("callType"));
		SiteVO site = (SiteVO)req.getSession().getAttribute(Constants.SITE_DATA);
		UserDataVO user = (UserDataVO)req.getSession().getAttribute(Constants.USER_DATA);
		ClientApiUtil util = new ClientApiUtil(site.getSiteConfig());
		List<String> fields;
		switch(callType) {
			case GET_CLIENT_ACCOUNT_BALANCES:
				super.setModuleData(util.getAccountBalances((String)user.getAttribute(MindBodyUtil.MINDBODY_CLIENT_ID)));
				break;
			case GET_CLIENT_PURCHASES:
				super.setModuleData(util.getClientPurchases((String)user.getAttribute(MindBodyUtil.MINDBODY_CLIENT_ID)));
				break;
			case GET_CLIENT_SCHEDULE:
				super.setModuleData(util.getClientSchedule((String)user.getAttribute(MindBodyUtil.MINDBODY_CLIENT_ID)));
				break;
			case GET_CLIENT_SERVICES:
				super.setModuleData(util.getClientServices((String)user.getAttribute(MindBodyUtil.MINDBODY_CLIENT_ID)));
				break;
			case GET_CLIENT_VISITS:
				super.setModuleData(util.getClientVisits((String)user.getAttribute(MindBodyUtil.MINDBODY_CLIENT_ID)));
				break;
			case GET_CUSTOM_CLIENT_FIELDS:
				fields = util.getCustomClientFields();
				super.setModuleData(fields, fields.size(), null);
				break;
			case GET_REQUIRED_CLIENT_FIELDS:
				fields = util.getRequiredClientFields();
				super.setModuleData(fields, fields.size(), null);
				break;
			default:
				log.warn("Endpoint not supported for give CallType: " + callType.toString());
		}
	}

	/**
	 * Get The ClientDocumentType off the request.
	 * @return
	 * @throws ActionException 
	 */
	private ClientDocumentType getDocumentType(String callType) throws ActionException {
		if(!StringUtil.isEmpty(callType)) {
			try {
				return ClientDocumentType.valueOf(callType);
			} catch(Exception e) {
				log.error("Could not determine Client CallType.");
				throw new ActionException("Could not determine Client CallType.");
			}
		}

		throw new ActionException("Client CallType not passed.");
	}
}