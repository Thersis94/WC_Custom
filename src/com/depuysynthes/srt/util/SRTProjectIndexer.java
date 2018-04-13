package com.depuysynthes.srt.util;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.SolrClient;

import com.depuysynthes.srt.vo.SRTProjectSolrVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.security.EncryptionException;
import com.siliconmtn.security.StringEncrypter;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.user.LastNameComparator;
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
		Map<String, SRTProjectSolrVO> projects;
		String sql = buildRetrieveSql(projectId);

		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (projectId != null) ps.setString(1, projectId);

			ResultSet rs = ps.executeQuery();
			projects = buildProjectsMap(rs);

			//Decrypt User Names
			LastNameComparator c = new LastNameComparator();
			c.decryptNames(new ArrayList<>(projects.values()), config.getProperty(Constants.ENCRYPT_KEY));

			//Descrypt Other Names.
			decryptNames(projects);

			//Add Project Data
			populateProjectData(projects);

			//Return Projects
			return new ArrayList<>(projects.values());
		} catch (SQLException e) {
			log.error("could not retrieve products", e);
		}

		return Collections.emptyList();
	}

	/**
	 * Iterated the ResultSet and Build the Projects Map.
	 * @param rs
	 * @return
	 * @throws SQLException
	 */
	private Map<String, SRTProjectSolrVO> buildProjectsMap(ResultSet rs) throws SQLException {
		Map<String, SRTProjectSolrVO> projects = new HashMap<>();
		SRTProjectSolrVO p = null;
		while (rs.next()) {
			if(p == null || !p.getDocumentId().equals(rs.getString("DOCUMENT_ID"))) {
				if(p != null) {
					projects.put(p.getDocumentId(), p);
				}
				p = new SRTProjectSolrVO(rs);
				p.setSolrIndex(INDEX_TYPE);
				p.addRole(100);
				p.addOrganization(SRTUtil.SRT_ORG_ID);
			}
		}
		if(p != null)
			projects.put(p.getDocumentId(), p);

		return projects;
	}

	/**
	 * Decrypts Engineer, Designer and QA Names as they come from Profile.
	 * @param projects
	 */
	private void decryptNames(Map<String, SRTProjectSolrVO> projects) {
		try {
			StringEncrypter se = new StringEncrypter((String) config.getProperty(Constants.ENCRYPT_KEY));
			for(SRTProjectSolrVO p : projects.values()) {
				p.setEngineerNm(SRTUtil.decryptName(p.getEngineerNm(), se));
				p.setDesignerNm(SRTUtil.decryptName(p.getDesignerNm(), se));
				p.setQualityEngineerNm(SRTUtil.decryptName(p.getQualityEngineerNm(), se));
			}
		} catch (EncryptionException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Load Master Record Data into the Project Map.
	 * @param projects
	 */
	private void populateProjectData(Map<String, SRTProjectSolrVO> projects) {
		boolean singleSearch = projects.size() == 1;
		try(PreparedStatement ps = dbConn.prepareStatement(loadMasterRecordSql(singleSearch))) {
			if(singleSearch) {
				ps.setString(1, projects.keySet().iterator().next());
			}

			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				String pId = rs.getString(PROJECT_ID);
				if(!StringUtil.isEmpty(pId) && projects.containsKey(pId)) {
					SRTProjectSolrVO p =projects.get(pId);
					p.addPartNumbers(rs.getString("PART_NO"));
					p.addProductCategory(rs.getString("prod_cat_id"));
					p.addProductFamily(rs.getString("prod_family_id"));
				}
			}
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Create the sql for the master record retrieve
	 * @param singleSearch 
	 * @return
	 */
	private String loadMasterRecordSql(boolean singleSearch) {
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		StringBuilder sql = new StringBuilder(300);
		sql.append("select x.project_id, m.prod_cat_id, m.prod_family_id, ");
		sql.append("m.part_no from ").append(customDb);
		sql.append("dpy_syn_srt_master_record_project_xr x ");
		sql.append(DBUtil.INNER_JOIN).append(customDb);
		sql.append("dpy_syn_srt_master_record m ");
		sql.append("on x.master_record_id = m.master_record_id ");

		if(singleSearch) {
			sql.append(DBUtil.WHERE_CLAUSE).append(" x.project_id = ? ");
		}
		return sql.toString();
	}

	/**
	 * Create the sql for the project retrieve
	 * @param projectId
	 * @return
	 */
	protected String buildRetrieveSql(String projectId) {
		StringBuilder sql = new StringBuilder(1600);
		String customDb = config.getProperty(Constants.CUSTOM_DB_SCHEMA);
		sql.append("select p.project_id as document_id, p.op_co_id, project_name as title, ");
		sql.append("p.create_dt as update_dt, hospital_po, engineer_id, designer_id, "); 
		sql.append("quality_engineer_id, make_from_order_no, buyer_id, supplier_id, ");
		sql.append("concat(trim(both ' ' from r.surgeon_first_nm), ' ', trim(both ' ' from r.surgeon_last_nm)) as surgeon_nm, r.request_territory_id, "); 
		sql.append("profile.first_nm, profile.last_nm, p.proj_stat_id, m.MILESTONE_ID, m.MILESTONE_DT, ");
		sql.append("concat(ep.first_nm, ' ', ep.last_nm) as engineer_nm, ");
		sql.append("concat(dp.first_nm, ' ', dp.last_nm) as designer_nm, ");
		sql.append("concat(qp.first_nm, ' ', qp.last_nm) as quality_engineer_nm ");
		sql.append(DBUtil.FROM_CLAUSE).append(customDb);
		sql.append("DPY_SYN_SRT_PROJECT p ");
		sql.append(DBUtil.INNER_JOIN).append(customDb);
		sql.append("DPY_SYN_SRT_REQUEST r ");
		sql.append("on p.request_id = r.request_id ");
		sql.append(DBUtil.INNER_JOIN).append(customDb);
		sql.append("DPY_SYN_SRT_ROSTER u ");
		sql.append("on u.roster_id = r.roster_id ");
		sql.append(DBUtil.INNER_JOIN).append("profile profile ");
		sql.append("on u.profile_id = profile.profile_id ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb);
		sql.append("dpy_syn_srt_project_milestone_xr m ");
		sql.append("on p.project_id = m.project_id ");

		//Get Optional Engineer Information
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("DPY_SYN_SRT_ROSTER e ");
		sql.append("on p.ENGINEER_ID = e.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE ep ");
		sql.append("on e.PROFILE_ID = ep.PROFILE_ID ");

		//Get Optional Designer Information
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("DPY_SYN_SRT_ROSTER d ");
		sql.append("on p.DESIGNER_ID = d.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE dp ");
		sql.append("on d.PROFILE_ID = dp.PROFILE_ID ");

		//Get Optional QA Engineer Information
		sql.append(DBUtil.LEFT_OUTER_JOIN).append(customDb).append("DPY_SYN_SRT_ROSTER q ");
		sql.append("on p.QUALITY_ENGINEER_ID = q.ROSTER_ID ");
		sql.append(DBUtil.LEFT_OUTER_JOIN).append("PROFILE qp ");
		sql.append("on q.PROFILE_ID = qp.PROFILE_ID ");

		if(!StringUtil.isEmpty(projectId)) {
			sql.append(DBUtil.WHERE_CLAUSE).append("p.PROJECT_ID = ? ");
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
		try (SolrActionUtil util = new SolrActionUtil(server)) {
			for (String projectId : projectIds) 
				util.addDocuments(retrieveProjects(projectId));

		} catch (Exception e) {
			log.error("Failed to index project with id: " + projectIds, e);
		}
	}
}