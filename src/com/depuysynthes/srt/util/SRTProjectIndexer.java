package com.depuysynthes.srt.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.biomed.smarttrak.util.SmarttrakSolrUtil;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: SRTProjectIndexer.java <p/>
 * <b>Project</b>: WC_Custom <p/>
 * <b>Description: </b> Index all projects.
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Billy Larsen
 * @version 1.0
 * @since Feb 15, 2017<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class SRTProjectIndexer  extends SMTAbstractIndex {
	public static final String INDEX_TYPE = "SRT_PROJECT";
	public static final String PROJECT_ID = "PROJECT_ID";
	public SRTProjectIndexer(Properties config) {
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@Override
	@SuppressWarnings("resource")
	public void addIndexItems(SolrClient server) {
		// Never place this in a try with resources.
		// This server was given to this method and it is not this method's
		// job or right to close it.
		SolrActionUtil util = new SolrActionUtil(server);
		try {
			util.addDocuments(retrieveProjects(null));
		} catch (Exception e) {
			log.error("could not index products", e);
		}
	}


	/**
	 * loads a list of products to push to Solr
	 * @param projectId
	 * @return
	 */
	protected List<SolrDocumentVO> retrieveProjects(String projectId) {
		List<SolrDocumentVO> projects = new ArrayList<>();
		String sql = buildRetrieveSql(projectId);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (projectId != null) ps.setString(1, projectId);

			ResultSet rs = ps.executeQuery();
			SRTProjectVO p = null;
			while (rs.next()) {
				if(p == null || !p.getProjectId().equals(rs.getString(PROJECT_ID))) {
					if(p != null) {
						projects.add(p);
					}
					p = new SRTProjectVO(rs);
					p.setSolrIndex(INDEX_TYPE);
				}

				//Add Milestones as attributes on the SolrDocument
				p.addAttribute(rs.getString("MILESTONE_ID"), rs.getDate("MILESTONE_DT"));
			}
			if(p != null)
				projects.add(p);
		} catch (SQLException e) {
			log.error("could not retrieve products", e);
		}

		return projects;
	}

	/**
	 * Create the sql for the product retrieve
	 * @param projectId
	 * @return
	 */
	protected String buildRetrieveSql(String projectId) {
		StringBuilder sql = new StringBuilder(275);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select p.*, m.*, r.roster_id ").append(DBUtil.FROM_CLAUSE);
		sql.append(customDb).append("DPY_SYN_SRT_PROJECT p ");
		sql.append(DBUtil.INNER_JOIN).append(customDb);
		sql.append("DPY_SYN_SRT_REQUEST r ");
		sql.append("on p.request_id = r.request_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb);
		sql.append("dpy_syn_srt_project_milestone_xr m ");
		sql.append("on p.project_id = m.project_id ");
		if(!StringUtil.isEmpty(projectId)) {
			sql.append(DBUtil.WHERE_CLAUSE).append("PROJECT_ID = ? ");
		}
		sql.append("ORDER BY p.project_id, p.create_dt ");
		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return INDEX_TYPE;
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#indexItems(java.lang.String[])
	 */
	@Override
	public void indexItems(String... projectIds) {
		SolrClient server = makeServer(); //the server will get closed by the auto-closeable util below.
		try (SolrActionUtil util = new SmarttrakSolrUtil(server)) {
			for (String projectId : projectIds) 
				util.addDocuments(retrieveProjects(projectId));

		} catch (Exception e) {
			log.error("Failed to index product with id: " + projectIds, e);
		}
	}
}