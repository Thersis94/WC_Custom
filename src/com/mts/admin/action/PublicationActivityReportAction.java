package com.mts.admin.action;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.common.html.BSTableControlVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.orm.GridDataVO;
import com.siliconmtn.util.Convert;
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
	public static final String AJAX_KEY = "pubActivityreport";

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (!req.hasParameter("json")) return;

		BSTableControlVO bst = new BSTableControlVO(req, PageviewArticleVO.class);
		String pubId = req.getParameter("publicationId");
		String issueId = req.getParameter("issueId");
		Date startDt = Convert.formatStartDate(req.getParameter("startDate"), "1/1/2000");;
		Date endDt = Convert.formatEndDate(req.getParameter("endDate"));
		setModuleData(loadPageviews(pubId, startDt, endDt, issueId, bst));
	}


	/**
	 * Load the pageviews in the given session
	 * @param bst 
	 * @param sessionId
	 * @return
	 */
	private GridDataVO<PageviewArticleVO> loadPageviews(String publicationId, Date startDt, Date endDt,String issueId, BSTableControlVO bst) {
		List<Object> vals = new ArrayList<>();
		String schema = getCustomSchema();
		
		StringBuilder sql = new StringBuilder(1000);
		sql.append(DBUtil.SELECT_CLAUSE).append("i.issue_nm, p.publication_nm,sb.action_nm, cast(coalesce(pv.total_views, 0) as int )as pageviews, sb.action_id  ");
		sql.append(DBUtil.FROM_CLAUSE).append("document d ");
		sql.append(DBUtil.INNER_JOIN).append("sb_action sb on d.action_id = sb.action_id and sb.pending_sync_flg = 0 ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_document md on sb.action_group_id = md.action_group_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_issue i on md.issue_id = i.issue_id ");
		sql.append(DBUtil.INNER_JOIN).append(schema).append("mts_publication p on i.publication_id = p.publication_id ");
		sql.append(DBUtil.INNER_JOIN).append(" ( ");
		sql.append(DBUtil.SELECT_CLAUSE).append("split_part(request_uri, '/', 4) as doc, sum(hit_cnt) as total_views  from pageview_summary ps ");
		sql.append(DBUtil.INNER_JOIN).append("page p on ps.page_id = p.page_id ");
		sql.append(DBUtil.WHERE_CLAUSE).append("p.site_id = 'MTS_2' and page_alias_nm = 'article' ");
		
		if (startDt != null) {
			sql.append("and ps.visit_dt between ? and ? ");
			vals.add(startDt);
			vals.add(endDt);
		}

		sql.append("group by doc having sum(hit_cnt) > 0 ");
		
		sql.append(" ) as pv on d.direct_access_pth = pv.doc ");
		sql.append(DBUtil.WHERE_CLAUSE).append("d.organization_id = 'MTS' ");
		
		if(! StringUtil.isEmpty(publicationId)) {
			sql.append("and p.publication_id = ? ");
			vals.add(publicationId);
		}
		
		if(! StringUtil.isEmpty(issueId)) {
			sql.append("and i.issue_id = ? ");
			vals.add(issueId);
		}
		
		if (bst.hasSearch()) {
			sql.append("and (lower(sb.action_nm)) like ? ");
			vals.add(bst.getLikeSearch().toLowerCase());
		}
		
		sql.append(bst.getSQLOrderBy("action_nm", "desc"));
		
		log.debug(sql.toString() + "|" + vals);
		
		DBProcessor db = new DBProcessor(getDBConnection());
		return db.executeSQLWithCount(sql.toString(), vals, new PageviewArticleVO(), "action_id",bst);
	}
}
