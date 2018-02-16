package com.depuysynthes.srt;

import java.util.ArrayList;
import java.util.List;

import com.depuysynthes.srt.vo.SRTRequestVO;
import com.depuysynthes.srt.vo.SRTRosterVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title:</b> SRTRequestAction.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Manages SRT Request Data.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 15, 2018
 ****************************************************************************/
public class SRTRequestAction extends FormAction {

	public static final String SRT_REQUEST_ID = "srtRequestId"; 
	public SRTRequestAction() {
		super();
	}

	public SRTRequestAction(ActionInitVO init) {
		super(init);
	}

	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		SRTRosterVO roster = (SRTRosterVO)req.getSession().getAttribute(Constants.USER_DATA);

		/*
		 * If requestId is present, Load Data from Form Retrieval,
		 * Else list SRTRequestVOs for Tables.
		 */
		if(req.hasParameter(SRT_REQUEST_ID)) {
			super.retrieve(req);
		} else {
			putModuleData(loadRequests(roster));
		}
	}

	/**
	 * Load SRT Requests.
	 * @param roster
	 * @param parameter
	 * @return
	 */
	private List<SRTRequestVO> loadRequests(SRTRosterVO roster) {
		List<Object> vals = new ArrayList<>();

		//if user is not an admin, add rosterId.
		if(!roster.isAdmin()) {
			vals.add(roster.getRosterId());
		}

		return new DBProcessor(dbConn).executeSelect(buildRequestLoadSql(!vals.isEmpty()), vals, new SRTRequestVO());
	}

	/**
	 * Build the SRT Request Load Sql.
	 * @return
	 */
	private String buildRequestLoadSql(boolean hasUser) {
		String schema = getCustomSchema();
		StringBuilder sql = new StringBuilder(250);
		sql.append("select * from ").append(schema);
		sql.append("SRT_REQUEST r ");
		sql.append("left outer join ").append(schema);
		sql.append("SRT_REQUEST_ADDRESS a on r.REQUEST_ID = a.REQUEST_ID ");
		sql.append("inner join ").append(schema);
		sql.append("SRT_ROSTER u on r.ROSTER_ID = u.ROSTER_ID ");
		sql.append("inner join PROFILE p on u.PROFILE_ID = p.PROFILE_ID ");
		sql.append("order by r.create_dt desc ");
		return sql.toString();
	}
}