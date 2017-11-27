package com.mindbody.action;

import java.util.Map;

import com.mindbody.MindBodySiteApi;
import com.mindbody.MindBodySiteApi.SiteDocumentType;
import com.mindbody.util.MindBodyUtil;
import com.mindbody.vo.MindBodyResponseVO;
import com.mindbody.vo.site.MindBodyGetLocationsConfig;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> MindBodySiteAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages MindBody Site Data Interactions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Nov 26, 2017
 ****************************************************************************/
public class MindBodySiteAction extends SimpleActionAdapter {

	public MindBodySiteAction() {
		super();
	}

	public MindBodySiteAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SiteDocumentType callType = getDocumentType(req.getParameter("callType"));
		SiteVO site = (SiteVO)req.getAttribute(Constants.SITE_DATA);
		switch(callType) {
			case GET_LOCATIONS:
				super.setModuleData(getLocations(site.getSiteConfig()));
				break;
			default:
				log.warn("Endpoint not supported for give CallType: " + callType.toString());
				break;

		}
	}

	/**
	 * Retrieve All Locations from MindBody System.
	 * @param siteConfig
	 * @return
	 */
	private MindBodyResponseVO getLocations(Map<String, String> siteConfig) {
		return new MindBodySiteApi().getAllDocuments(new MindBodyGetLocationsConfig(MindBodyUtil.buildSourceCredentials(siteConfig)));
	}

	/**
	 * @param parameter
	 * @return
	 * @throws ActionException 
	 */
	private SiteDocumentType getDocumentType(String callType) throws ActionException {
		try {
			return SiteDocumentType.valueOf(callType);
		} catch(Exception e) {
			throw new ActionException("Given callType is invalid for this request: " + callType);
		}
	}
}
