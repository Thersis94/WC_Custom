package com.sjm.corp.mobile.collection.action;

import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.sjm.corp.mobile.collection.MobileCollectionVO;

/****************************************************************************
 * <b>Title</b>: updateGoals.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Updates the GoalVO for the SJM Mobile Collection app
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class UpdateGoals extends CollectionAbstractAction {

	/*
	 * (non-Javadoc)
	 * @see com.sjm.corp.mobile.collection.action.CollectionAbstractAction#update(com.siliconmtn.http.SMTServletRequest, com.sjm.corp.mobile.collection.MobileCollectionVO)
	 */
	public void update(SMTServletRequest req, MobileCollectionVO vo) {
		vo.getGoals().setRebrandPractice(Convert.formatBoolean(req.getParameter("rebrandPractice")));
		vo.getGoals().setOverallPatients(Convert.formatBoolean(req.getParameter("overallPatients")));
		vo.getGoals().setHcpPatients(Convert.formatBoolean(req.getParameter("hcpPatients")));
		vo.getGoals().setInterventionalPatients(Convert.formatBoolean(req.getParameter("interventionalPatients")));
		vo.getGoals().setNewPractice(Convert.formatBoolean(req.getParameter("newPractice")));
		vo.getGoals().setConsolidation(Convert.formatBoolean(req.getParameter("consolidation")));
	}
}
