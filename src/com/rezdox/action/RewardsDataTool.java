package com.rezdox.action;

import java.util.List;

import static com.rezdox.action.RewardsAction.REQ_REWARD_ID;
import com.rezdox.vo.RewardVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.AdminConstants;

/****************************************************************************
 * <b>Title:</b> RewardsDataTool.java<br/>
 * <b>Description:</b> WC Data Tool for Managing Rewards in the system.
 * <br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies<br/>
 * @author James McKain
 * @version 1.0
 * @since Feb 23, 2018
 ****************************************************************************/
public class RewardsDataTool extends SimpleActionAdapter {

	public RewardsDataTool() {
		super();
	}

	public RewardsDataTool(ActionInitVO arg0) {
		super(arg0);
	}


	/**
	 * Return a list of all the rewards in the system.
	 * This is called from the WC Data Tool for RezDox Administrators to manage the data.
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {
		//only load data on the ajax call (list pg), or when a pkId is passed (edit pg)
		if (!req.hasParameter("loadData") && !req.hasParameter(REQ_REWARD_ID)) return;

		RewardsAction ra = new RewardsAction(getDBConnection(), getAttributes());
		List<RewardVO> data = ra.loadRewards(req.getParameter(REQ_REWARD_ID));
		putModuleData(data);
	}


	@Override
	public void update(ActionRequest req) throws ActionException {
		String msg = save(req, false);
		adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}


	@Override
	public void delete(ActionRequest req) throws ActionException {
		String msg = save(req, true);
		adminRedirect(req, msg, (String)getAttribute(AdminConstants.ADMIN_TOOL_PATH));
	}


	/**
	 * reusable internal method for invoking DBProcessor
	 * @param req
	 * @param isDelete
	 * @throws ActionException
	 */
	protected String save(ActionRequest req, boolean isDelete) {
		RewardVO vo = RewardVO.instanceOf(req);
		DBProcessor db = new DBProcessor(dbConn, getCustomSchema());
		try {
			if (isDelete) {
				db.delete(vo);
			} else {
				db.save(vo);
			}
			return (String)getAttribute(AdminConstants.KEY_SUCCESS_MESSAGE);

		} catch (Exception e) {
			log.error("could not save reward", e);
			return (String)getAttribute(AdminConstants.KEY_ERROR_MESSAGE);
		}
	}
}