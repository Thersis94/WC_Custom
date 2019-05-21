package com.biomed.smarttrak.util;

import com.biomed.smarttrak.vo.UserVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.http.session.SMTSession;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentVO;
import com.smt.sitebuilder.common.SiteVO;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: QuickLinkAction.java <p/>
 * <b>Project</b>: SC_Custom <p/>
 * <b>Description: </b> Handles the creation of quick images for the ckeditor plugin. 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2019<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since May 21, 2019<p/>
 * @updates:
 ****************************************************************************/

public class QuickLinkAction extends SBActionAdapter{

	private static final String PROFILE_DOC_ID = "profileDocumentId";
	
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (!req.hasParameter(PROFILE_DOC_ID)) return;
		ProfileDocumentAction pda = new ProfileDocumentAction();
		pda.setActionInit(actionInit);
		pda.setDBConnection(dbConn);
		pda.setAttributes(attributes);
		ProfileDocumentVO pvo = pda.getDocumentByProfileDocumentId(req.getParameter(PROFILE_DOC_ID));
		
		putModuleData(pvo);
	}

	@Override
	public void build (ActionRequest req)  throws ActionException{
		SMTSession ses = req.getSession();
		UserVO user = (UserVO) ses.getAttribute(Constants.USER_DATA);

		ProfileDocumentAction pda = new ProfileDocumentAction();
		pda.setAttributes(attributes);
		pda.setDBConnection(dbConn);
		pda.setActionInit(actionInit);

		String orgId = ((SiteVO)req.getAttribute(Constants.SITE_DATA)).getOrganizationId();

		req.setParameter("profileId", user.getProfileId());
		req.setParameter("organizationId", orgId);
		req.setParameter("actionId", actionInit.getActionId());
		String filePath = req.getParameter("filePathText");
		req.setParameter("fileType", filePath.substring(filePath.indexOf('.')+1));
		req.setParameter("fileName", filePath.substring(filePath.indexOf("--")+2));

		try {
			//adds the new record and file
			pda.build(req);
		} catch (ActionException e) {
			log.error("error occured during profile document generation " , e);
		}
		super.setModuleData(req.getParameter(PROFILE_DOC_ID));
	}
}
