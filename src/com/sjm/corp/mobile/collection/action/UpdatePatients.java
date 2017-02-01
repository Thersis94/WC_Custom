package com.sjm.corp.mobile.collection.action;

import com.siliconmtn.action.ActionRequest;
import com.sjm.corp.mobile.collection.MobileCollectionVO;

/****************************************************************************
 * <b>Title</b>: UpdatePatients.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Updates the PatientsVO for the SJM Mobile Collection app
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class UpdatePatients extends CollectionAbstractAction {

	/*
	 * (non-Javadoc)
	 * @see com.sjm.corp.mobile.collection.action.CollectionAbstractAction#update(com.siliconmtn.http.SMTServletRequest, com.sjm.corp.mobile.collection.MobileCollectionVO)
	 */
	public void update(ActionRequest req, MobileCollectionVO vo) {
		vo.getPatients().setChiropractorRef(parseInt(req.getParameter("chiropractorRef")));
		vo.getPatients().setOrthopedicRef(parseInt(req.getParameter("orthopedicRef")));
		vo.getPatients().setOtherRef(parseInt(req.getParameter("otherRef")));
		vo.getPatients().setOtherRefName(req.getParameter("otherRefName"));
		vo.getPatients().setPhysicalTherepistRef(parseInt(req.getParameter("physicalTherepistRef")));
		vo.getPatients().setPodiatristRef(parseInt(req.getParameter("podiatristRef")));
		vo.getPatients().setPrimaryCareRef(parseInt(req.getParameter("primaryCareRef")));
	}
}
