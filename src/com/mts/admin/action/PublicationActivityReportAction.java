package com.mts.admin.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.mts.common.MTSConstants;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <p><b>Title:</b> PublicationActivityReportAction.java</p>
 * <p><b>Description:</b> Loads pageview activity (counts only) by publication->issue->article.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Nov 05, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class PublicationActivityReportAction extends SimpleActionAdapter {


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (!req.hasParameter("json")) return;

		String pubId = req.getParameter("publicationId");
		Date startDt = req.getDateParameter("startDate");
		Date endDt = req.getDateParameter("endDate");
		BSTableControlVO bst = new BSTableControlVO(req);
		GridDataVO<PageviewArticleVO> data = loadPageviews(pubId, startDt, endDt, bst); 
		log.info("SQL Count: " + data.getSqlTotal());
		setModuleData(data);
	}



	/**
	 * Load the pageviews in the given session
	 * @param sessionId
	 * @return
	 */
	private GridDataVO<PageviewArticleVO> loadPageviews(String publicationId, Date startDt, Date endDt, BSTableControlVO bst) {
		String schema = getCustomSchema();
		String baseSql = StringUtil.join("select pub.publication_id, pub.publication_nm, ",
				"iss.issue_id, iss.issue_nm, sa.action_id, sa.action_nm, sum(ps.hit_cnt)::int as pageviews, ",
				"visit_dt as dt ",
				DBUtil.FROM_CLAUSE, schema, "mts_publication pub ",
				DBUtil.INNER_JOIN, schema, "mts_issue iss on pub.publication_id=iss.publication_id ",
				DBUtil.INNER_JOIN, schema, "mts_document mdoc on iss.issue_id=mdoc.issue_id ",
				DBUtil.INNER_JOIN, "document doc on mdoc.document_id=doc.action_id ",
				DBUtil.INNER_JOIN, "sb_action sa on doc.action_id=sa.action_group_id and sa.pending_sync_flg !=1 ",
				DBUtil.INNER_JOIN, "pageview_summary ps on '/'||lower(pub.publication_id)||'/article/'||doc.direct_access_pth=ps.request_uri and ps.site_id=? ",
				"where 1=1 ");

		List<Object> vals = new ArrayList<>();
		vals.add(MTSConstants.SUBSCRIBER_SITE_ID);
		StringBuilder sql = new StringBuilder(baseSql.length() + 200);
		sql.append(baseSql);
		if (!StringUtil.isEmpty(publicationId)) {
			sql.append("and pub.publication_id=? ");
			vals.add(publicationId);
		}
		if (startDt != null) {
			sql.append("and ps.visit_dt >= ? ");
			vals.add(startDt);
		}
		if (endDt != null) {
			sql.append("and ps.visit_dt < ? ");
			vals.add(endDt);
		}

		sql.append("group by pub.publication_id, pub.publication_nm, iss.issue_id, iss.issue_nm, sa.action_id, sa.action_nm, visit_dt ");
		sql.append("order by visit_dt desc"); //transparent sorting puts the most recently viewed articles at the top of the list
		log.info(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSQLWithCount(sql.toString(), vals, new PageviewArticleVO(), bst);
	}
}
