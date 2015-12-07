/**
 *
 */
package com.depuysynthes.aabp;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.http.SMTServletRequest;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: SymptomsAnalyzerAction.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Simple Action that replicates the Symptoms Analyzer
 * Functionality on the original AABP Site.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Dec 7, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class SymptomsAnalyzerAction extends SimpleActionAdapter {

	/**
	 * 
	 */
	public SymptomsAnalyzerAction() {
	}

	/**
	 * @param init
	 */
	public SymptomsAnalyzerAction(ActionInitVO init) {
		super(init);
	}

	public void retrieve(SMTServletRequest req) {

		//Extract Data from Request into Search VO.
		SymptomsAnalyzerVO data = new SymptomsAnalyzerVO(req);

		//Query for Articles using passed params.
		Map<String, String> docs = getResults(data);

		//Put Results back on the request.
		putModuleData(docs);
	}

	/**
	 * Helper method that retrieves a map of Page Paths and Page Titles for
	 * @param data
	 * @return
	 */
	public Map<String, String> getResults(SymptomsAnalyzerVO data) {
		Map<String, String> results = new HashMap<>();

		try(PreparedStatement ps = dbConn.prepareStatement(getSymptomsAnalyzerRetrieveSql(data))) {
			int i = 1;
			ps.setInt(i++, 1);
			ps.setString(i++, data.getPain());

			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				results.put(rs.getString("PAGE_PATH"), rs.getString("PAGE_TITLE"));
			}

			log.debug("retrieved " + results.size() + " records");
		} catch (SQLException e) {
			log.error(e);
		}
		return results;
	}

	/**
	 * Helper method that builds the proper Sql Query for the given Params.
	 * @param params
	 * @return
	 */
	public String getSymptomsAnalyzerRetrieveSql(SymptomsAnalyzerVO data) {
		StringBuilder sql = new StringBuilder(900);
		sql.append("select PAGE_PATH, PAGE_TITLE from ");
		sql.append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("AABP_CONTENT_XR ");
		sql.append("where (PUBLIC_FLAG = ? AND ANATOMY_KEYWORD = ? ");

		/*
		 * Build the Sql Statement according to the control Logic in original
		 * ASP file.
		 */
		if(!data.getPain().equals("None")) {
			sql.append("and ");
			if(data.getOnset().equals("Gradual")) {
				if(!data.isLegPain() && !data.isArmPain() && data.isCurve() && data.isHunchback()) {
					sql.append("PROCESS_KEYWORD <> 'Injury') and ");
					sql.append("(MISC_KEYWORD LIKE '%Spondylosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylolisthesis%' or");
					sql.append("MISC_KEYWORD LIKE '%Stenosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Mechanical%' or ");
					sql.append("MISC_KEYWORD LIKE '%Fracture%' or ");
					sql.append("MISC_KEYWORD LIKE '%Kyphosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Osteoporosis%') ");			
				} else if(!data.isCurve() || !data.isHunchback()) {
					sql.append("PROCESS_KEYWORD <> 'Injury')");
				} else if(data.isLegPain() && data.isArmPain() && data.isCurve() && data.isHunchback()) {
					sql.append("PROCESS_KEYWORD <> 'Injury') and ");
					sql.append("(MISC_KEYWORD LIKE '%Ruptured%' or ");
					sql.append("MISC_KEYWORD LIKE '%Herniated%' or ");
					sql.append("MISC_KEYWORD LIKE '%Radiculopathy%' or ");
					sql.append("MISC_KEYWORD LIKE '%Compressive%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylolisthesis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Stenosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Scoliosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Kyphosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Osteoporosis%') ");
				} else if((data.isLegPain() && !data.isArmPain()) || (!data.isLegPain() && data.isArmPain()) && data.isCurve() && data.isHunchback()) {
					sql.append("PROCESS_KEYWORD <> 'Injury') and ");
					sql.append("(MISC_KEYWORD LIKE '%Ruptured%' or ");
					sql.append("MISC_KEYWORD LIKE '%Herniated%' or ");
					sql.append("MISC_KEYWORD LIKE '%Radiculopathy%' or ");
					sql.append("MISC_KEYWORD LIKE '%Compressive%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylolisthesis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Stenosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Mechanical%' or ");
					sql.append("MISC_KEYWORD LIKE '%Fracture%' or ");
					sql.append("MISC_KEYWORD LIKE '%Scoliosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Kyphosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Osteoporosis%') ");
				}
			} else {
				if((data.isLegPain() && !data.isArmPain()) || (!data.isLegPain() && data.isArmPain()) && data.isCurve() && data.isHunchback()) {
					sql.append("PROCESS_KEYWORD = 'Injury') and ");
					sql.append("(MISC_KEYWORD LIKE '%Ruptured%' or ");
					sql.append("MISC_KEYWORD LIKE '%Herniated%' or ");
					sql.append("MISC_KEYWORD LIKE '%Radiculopathy%' or ");
					sql.append("MISC_KEYWORD LIKE '%Compressive%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylolisthesis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Stenosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Mechanical%' or ");
					sql.append("MISC_KEYWORD LIKE '%Fracture%' or ");
					sql.append("MISC_KEYWORD LIKE '%Scoliosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Kyphosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Osteoporosis%') ");
				} else if(!data.isLegPain() && !data.isArmPain() && data.isCurve() && data.isHunchback()) {
					sql.append("PROCESS_KEYWORD = 'Injury') AND ");
					sql.append("(MISC_KEYWORD LIKE '%Spondylosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylolisthesis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Stenosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Mechanical%' or ");
					sql.append("MISC_KEYWORD LIKE '%Fracture%' or ");
					sql.append("MISC_KEYWORD LIKE '%Kyphosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Osteoporosis%')");
				} else if(!data.isCurve() || !data.isHunchback()) {
					sql.append("PROCESS_KEYWORD = 'Injury') ");
				} else if(data.isArmPain() && data.isCurve() && data.isHunchback() && data.isLegPain()) {
					sql.append("PROCESS_KEYWORD = 'Injury') AND ");
					sql.append("(MISC_KEYWORD LIKE '%Ruptured%' or ");
					sql.append("MISC_KEYWORD LIKE '%Herniated%' or ");
					sql.append("MISC_KEYWORD LIKE '%Radiculopathy%' or ");
					sql.append("MISC_KEYWORD LIKE '%Compressive%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylolisthesis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Stenosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Spondylosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Scoliosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Kyphosis%' or ");
					sql.append("MISC_KEYWORD LIKE '%Osteoporosis%')");
				}
			}
		} else {
			sql.append(")");
		}

		log.debug(sql.toString());
		return sql.toString();
	}
}