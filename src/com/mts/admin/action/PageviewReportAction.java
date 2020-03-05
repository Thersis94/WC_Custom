package com.mts.admin.action;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.SimpleActionAdapter;

/****************************************************************************
 * <p><b>Title:</b> PageviewReportAction.java</p>
 * <p><b>Description:</b> Loads a list of pages viewed during the given user session.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Oct 23, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class PageviewReportAction extends SimpleActionAdapter {

	/**
	 * These tie us to the ", schema, "mts_publication table.  Entries will need to be added
	 * over time.  Ideally there continues to be correlation between publicationId and pageAlias.
	 */
	public enum Publication {
		MEDTECH_STRATEGIST,
		MARKET_PATHWAYS,
		BLOG
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		if (!req.hasParameter("json")) return;

		List<PageviewVO> data = loadPageviews(req.getParameter("sessionId"));
		EnumMap<Publication, List<PageviewArticleVO>> titles = new EnumMap<>(Publication.class);
		List<PageviewArticleVO> pTitles;

		//extract document URLs so we can lookup their names
		for (PageviewVO view : data) {
			log.debug("parsing: " + view.getUrl());
			String[] urlTokens = StringUtil.checkVal(view.getUrl(), "/").substring(1).split("/");
			String path = urlTokens.length > 0 ? urlTokens[0].replace('-','_').toUpperCase() : null;
			Publication p = EnumUtil.safeValueOf(Publication.class, path);
			if (p == null) continue; //this view isn't a page we can improve upon.  It's not part of a publication

			log.debug(String.format("found publication %s from %s", p, path));
			view.pub = p;

			if (urlTokens.length > 2) {
				view.documentPath = urlTokens[2];
				pTitles = titles.get(p);
				if (pTitles == null) pTitles = new ArrayList<>();
				pTitles.add(PageviewArticleVO.from(view));
				titles.put(p, pTitles);
				log.debug(String.format("found documentPath %s", view.documentPath));
			}
		}

		loadDocuments(titles);

		//merge the titles back into the view data
		for (PageviewVO view : data) {
			mergeDetails(view, titles.get(view.pub));
			if (StringUtil.isEmpty(view.item))
				view.item = view.qs;
		}

		putModuleData(data, data.size(), false);
	}


	/**
	 * find the article matching the pageview URL and merge whatever data we need for display
	 * @param view
	 * @param pTitles
	 */
	private void mergeDetails(PageviewVO view, List<PageviewArticleVO> pTitles) {
		if (pTitles == null) return;
		for (PageviewArticleVO vo : pTitles) {
			if (vo.getDocumentPath().equalsIgnoreCase(view.documentPath)) {
				view.item = vo.getDocumentNm();
				view.issue = vo.getIssueNm();
				view.page = vo.getPublicationNm();
				break;
			}
		}
	}


	/**
	 * load a list of issues and articles for each of the publications
	 * @param titles
	 */
	private void loadDocuments(EnumMap<Publication, List<PageviewArticleVO>> titles) {
		String schema = getCustomSchema();
		String baseSql = StringUtil.join("select distinct pub.publication_id, pub.publication_nm, iss.issue_id, ",
				"iss.issue_nm, doc.direct_access_pth, sa.action_id, sa.action_nm ",
				DBUtil.FROM_CLAUSE, schema, "mts_publication pub ",
				DBUtil.INNER_JOIN, schema, "mts_issue iss on pub.publication_id=iss.publication_id ",
				DBUtil.INNER_JOIN, schema, "mts_document mdoc on iss.issue_id=mdoc.issue_id ",
				DBUtil.INNER_JOIN, "document doc on mdoc.document_id=doc.action_id ",
				DBUtil.INNER_JOIN, "sb_action sa on doc.action_id=sa.action_group_id and sa.pending_sync_flg !=1 ");

		DBProcessor db = new DBProcessor(getDBConnection(), schema);
		db.setGenerateExecutedSQL(log.isDebugEnabled());
		for (Map.Entry<Publication, List<PageviewArticleVO>> entry : titles.entrySet()) {
			StringBuilder sql = new StringBuilder(baseSql);
			sql.append("where pub.publication_id=? and doc.direct_access_pth in (");
			DBUtil.preparedStatmentQuestion(entry.getValue().size(), sql);
			sql.append(")");
			log.debug(sql);

			List<Object> vals = new ArrayList<>(entry.getValue().size());
			vals.add(entry.getKey().name().replace('_', '-')); //DB uses dashes, enums can't
			for (PageviewArticleVO vo : entry.getValue())
				vals.add(vo.getDocumentPath());

			entry.setValue(db.executeSelect(sql.toString(), vals, new PageviewArticleVO()));
		}

	}



	/**
	 * Load the pageviews in the given session
	 * @param sessionId
	 * @return
	 */
	private List<PageviewVO> loadPageviews(String sessionId) {
		String sql = StringUtil.join("select p.page_display_nm, pu.pageview_user_id, ",
				"pu.request_uri_txt, pu.query_str_txt, pu.visit_dt ", 
				"from pageview_user pu ",
				"left join page p on pu.page_id=p.page_id ",
				"where pu.request_uri_txt not like '/portal%' and pu.session_id=? ",
				"order by pu.visit_dt desc");
		log.debug(sql);

		DBProcessor db = new DBProcessor(getDBConnection(), getCustomSchema());
		return db.executeSelect(sql, Arrays.asList(sessionId), new PageviewVO());
	}
}
