package com.biomed.smarttrak.action;

//Java 8
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

//app libs 
import org.apache.commons.lang.StringUtils;
import org.apache.solr.common.SolrDocument;

//Wc_custom
import com.biomed.smarttrak.security.SmarttrakRoleVO;
import com.biomed.smarttrak.vo.UserVO;

//baselibs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.NotAuthorizedException;
import com.siliconmtn.http.session.SMTSession;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.solr.AccessControlQuery;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentVO;

//Webcrescendo
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.action.search.SolrAction;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: FeaturedInsightAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> featured insights are handled differently then regular insights
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2017<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Ryan Riker
 * @version 2.0
 * @since Mar 17, 2017<p/>
 * @updates:
 ****************************************************************************/
public class FeaturedInsightAction extends InsightAction {

	protected static final String ACL_GRANTED_DELIMITER = "" + AccessControlQuery.GRANT + AccessControlQuery.GROUP + AccessControlQuery.ACL_DELIMITER;

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {

		SMTSession ses = req.getSession();
		UserVO user = (UserVO) ses.getAttribute(Constants.USER_DATA);
		
		//this section of the retrieve is used by file handler to process and send back the correct vo
		if (!StringUtil.isEmpty(req.getParameter("profileDocumentId"))) {
			String profileDocumentId = req.getParameter("profileDocumentId");
			processProfileDocumentRequest(profileDocumentId, user, ses.getAttribute(Constants.ROLE_DATA));
		} else {
		processFeaturedRequest(req);
		}

	}

	/**
	 * processes a request for a profile document
	 * @param user 
	 * @param profileDocumentId 
	 * @throws ActionException 
	 */
	private void processProfileDocumentRequest(String profileDocumentId, UserVO user, Object roleVo) throws ActionException {
		
				ProfileDocumentAction pda = new ProfileDocumentAction();
				pda.setActionInit(actionInit);
				pda.setDBConnection(dbConn);
				pda.setAttributes(attributes);
				ProfileDocumentVO pvo = pda.getDocumentByProfileDocumentId(profileDocumentId);
				
				StringBuilder sql = new StringBuilder(205);
				sql.append("select  count(*) from profile_document pd ");
				sql.append("inner join custom.biomedgps_insight bi on bi.insight_id = pd.feature_id ");
				sql.append("where bi.featured_flg = '1' and profile_document_id = ? ");
				
				log.debug("sql: " + sql + "|" + profileDocumentId );

				authorizeFeaturedImage(sql,profileDocumentId, pvo);
				
	}
	
	/**
	 * this method checks that the profile document id matches one that is an insight and also has the featured flg set 
	 * to 1 or it throws and catches a not authorized exception.
	 * @param profileDocumentId 
	 * @param sql 
	 * @param pvo 
	 */
	private void authorizeFeaturedImage(StringBuilder sql, String profileDocumentId, ProfileDocumentVO pvo){
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, profileDocumentId);
			int rowCount = 0;
			
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				rowCount = rs.getInt(1);
			}
			
			if (rowCount == 0 )
				throw new NotAuthorizedException("This profile doc. is not related to a featured insight");

			//will need a module data vo to send data back to the file handler
			ModuleVO modVo = new ModuleVO();
			modVo.setActionData(pvo);
			attributes.put(Constants.MODULE_DATA, modVo);
			
		} catch(NotAuthorizedException|SQLException sqle) {
			log.error("could not load or verify profile document ", sqle);
		}
	}
	
	
	/**
	 * @param req 
	 * @throws ActionException 
	 * 
	 */
	private void processFeaturedRequest(ActionRequest req) throws ActionException {
		//setting pmid for solr action check
		ModuleVO mod = (ModuleVO)attributes.get(Constants.MODULE_DATA);
		actionInit.setActionId((String)mod.getAttribute(ModuleVO.ATTRIBUTE_1));

		//get the users roles
		Set<String> userRoles = getUsersRoles(req);

		//build the solr action
		executeSolrRequest(req);

		//after solr retreive get the solr documents 
		SolrResponseVO solVo = (SolrResponseVO)mod.getActionData();
		mod.setAttribute("solarRes",solVo );

		log.debug("Total uncheck number of responses" + solVo.getTotalResponses());
		//get each document permissions and split them
		List<SolrDocument> authorizedFeatures = new ArrayList<>();

		for (SolrDocument solDoc : solVo.getResultDocuments()){
			checkDocumentForAuthorization(solDoc, userRoles, authorizedFeatures);
		}
		//change out results sets
		transposeResults(solVo, authorizedFeatures);
		
	}

	/**
	 * @param authorizedFeatures 
	 * @param solVo 
	 */
	private void transposeResults(SolrResponseVO solVo, List<SolrDocument> authorizedFeatures) {
		//replace un-filtered insights with the authorized insights  
		log.debug("number of authorized insights " + authorizedFeatures.size());

		solVo.setResultDocuments(authorizedFeatures, 1 , authorizedFeatures.size()+1 );

		//place insight vo data on req.
		putModuleData(solVo);
	}

	/**
	 * 
	 * @param authorizedFeatures 
	 * @param userRoles 
	 * @param solDoc 
	 */
	private void checkDocumentForAuthorization(SolrDocument solDoc, Set<String> userRoles, List<SolrDocument> authorizedFeatures) {
		log.debug("Document permissions " + solDoc.getFieldValue(SearchDocumentHandler.ACL));
		String docPermissions = (String) solDoc.getFieldValue(SearchDocumentHandler.ACL);
		String[] docP = docPermissions.split(" ");
		//compare them to the set
		for (String item : Arrays.asList(docP)){
			for( String userRole : userRoles){
				if (item.replace(ACL_GRANTED_DELIMITER, "").startsWith(userRole)){
					authorizedFeatures.add(solDoc);
				}
			}
			//once authorzied there is no reason to keep checking it
			if (authorizedFeatures.contains(solDoc)) break;
		}
	}	

	/**
	 * generates the solr action associated with the this widget and executes solr retrieve
	 * @param req 
	 * @throws ActionException 
	 */
	private void executeSolrRequest(ActionRequest req) throws ActionException {

		//making a new solr action
		SolrAction sa = new SolrAction(actionInit);
		sa.setDBConnection(dbConn);
		sa.setAttributes(attributes);

		//transform some incoming reqParams to where Solr expects to see them
		transposeRequest(req);
		sa.retrieve(req);
	}

	/**
	 * goes through the users hashed roles and gets a set of the ones that are two roles deep
	 * @param req 
	 * @return
	 */
	private Set<String> getUsersRoles(ActionRequest req) {
		SmarttrakRoleVO role = (SmarttrakRoleVO)req.getSession().getAttribute(Constants.ROLE_DATA);
		String[] roleAcl = role.getAuthorizedSections();

		//find all the two level ones and put in them in a set
		Set<String> secondLevel = new HashSet<>();
		for (String item : Arrays.asList(roleAcl)){
			int count = StringUtils.countMatches(item, SearchDocumentHandler.HIERARCHY_DELIMITER);
			if (count == 1){
				secondLevel.add(item);
			}
		}
		log.debug("user second level premissions " + secondLevel);
		return secondLevel;
	}
}
