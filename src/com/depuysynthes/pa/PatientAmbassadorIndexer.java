package com.depuysynthes.pa;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.solr.client.solrj.impl.HttpSolrServer;

import com.depuysynthes.pa.PatientAmbassadorStoriesTool.PAFConst;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.SBProfileManager;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.search.SMTAbstractIndex;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: PatientAmbassadorIndexer.java<p/>
 * <b>Description: Indexes all patient ambassador stories that are currently
 * published on the patient ambassador site.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since Mar 4, 2015
 ****************************************************************************/
public class PatientAmbassadorIndexer extends SMTAbstractIndex {
	private static final String ORG_ID = "DPY_SYN";

	public PatientAmbassadorIndexer(Properties config) {
		this.config = config;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.impl.HttpSolrServer)
	 */
	@Override
	public void addIndexItems(HttpSolrServer server) {
		SolrActionUtil solrUtil = new SolrActionUtil(server);
		List<FormTransactionVO> formVOs = retreiveAllSubmissions();
		
		// Loop over each form transaction and turn it into a SolrStoryVO for processing
		for (FormTransactionVO vo : formVOs) {
			try {
				SolrStoryVO story = buildSolrStoryVO(vo);
				log.debug("adding to Solr: " + story.toString());
				solrUtil.addDocument(story);
			} catch (Exception e) {
				log.error("could not create document to add to Solr", e);
			}
		}
		solrUtil = null;
	}
	
	/**
	 * Turns the form transaction vo into a SolrStoryVO that we
	 * can just drop into the SolrActionutil
	 * @param vo
	 * @return
	 */
	private SolrStoryVO buildSolrStoryVO(FormTransactionVO vo) throws Exception {
		Map<String, FormFieldVO> fields = vo.getCustomData();

		//Store Data on the SolrStoryVO
		SolrStoryVO ssv = new SolrStoryVO();
		ssv.setDocumentId(vo.getFormSubmittalId());
		ssv.setAuthor(vo.getFirstName());
		ssv.setZip(vo.getZipCode());
		ssv.setCity(vo.getCity());
		ssv.setState(vo.getState());
		ssv.setLat(vo.getLatitude().toString());
		ssv.setLng(vo.getLongitude().toString());
		ssv.setDetailImage(fields.get(PAFConst.PROFILE_IMAGE_ID.getId()).getResponses().get(0));
		ssv.setCategories(fields.get(PAFConst.HOBBIES_ID.getId()).getResponses());
		ssv.setHierarchies(fields.get(PAFConst.JOINT_ID.getId()).getResponses());
		ssv.setOtherHobbies(fields.get(PAFConst.OTHER_HOBBY_ID.getId()).getResponses().get(0));
		ssv.setTitle(fields.get(PAFConst.STORY_TITLE_ID.getId()).getResponses().get(0));
		ssv.setSummary(fields.get(PAFConst.STORY_TEXT_ID.getId()).getResponses().get(0));
		ssv.addOrganization(ORG_ID);
		ssv.addRole(SecurityController.PUBLIC_ROLE_LEVEL);

		return ssv;
	}

	/**
	 * Retrieve all published form submittals related to Patient Ambassador 
	 * and bring back some relevant information about them
	 * @return 
	 */
	private List<FormTransactionVO> retreiveAllSubmissions() {
		List<FormTransactionVO> vos = new ArrayList<FormTransactionVO>();
		Map<String, Object> vals = new HashMap<String, Object>();
		vals.put(Constants.ENCRYPT_KEY, config.get(Constants.ENCRYPT_KEY));
		ProfileManager pm = new SBProfileManager(vals);
		
		
		String srQuery = getSubmittalRecordQuery();
		log.debug("Query = " + srQuery + "|" + PAFConst.FORM_ID.getId() + "|" + PAFConst.STATUS_ID.getId());

		//Retrieve Data from DB
		try (PreparedStatement  ps = dbConn.prepareStatement(srQuery)) {
			ps.setString(1, PAFConst.STATUS_ID.getId());
			ps.setString(2, PAFConst.FORM_ID.getId());

			//Retrieve Results
			ResultSet rs = ps.executeQuery();
			String submittalId = "";
			String fieldId = "";
			FormTransactionVO vo = null;
			FormFieldVO field = null;
			//Process Results
			while(rs.next()) {
				// If we are dealing with a new field we add the current
				// one to the vo and create a new field vo.
				// This needs to be done before the vo so that all fields are
				// associated with the proper submissions.
				if (!fieldId.equals(rs.getString("FORM_FIELD_ID")) || !submittalId.equals(rs.getString("FORM_SUBMITTAL_ID"))) {
					if (field != null) vo.addCustomData(fieldId, field);
					field = new FormFieldVO(rs, true);
					fieldId = rs.getString("FORM_FIELD_ID");
				}
				
				// If we are dealing with a new submission store the old one
				// and create a new vo for the new submission.
				if (!submittalId.equals(rs.getString("FORM_SUBMITTAL_ID"))) {
					if (vo != null) vos.add(vo);
					vo = new FormTransactionVO(rs);
					submittalId = rs.getString("FORM_SUBMITTAL_ID");
				}
				
				field.addResponse(rs.getString("VALUE_TXT"));
			}
			
			// Add the dangling field and vo.
			if (vo != null) {
				if (field != null) vo.addCustomData(fieldId, field);
				vos.add(vo);
			}
			
			//Loop over the Transactions and retrieve the profile data for them.
			for(FormTransactionVO f : vos) {
				log.debug("Getting infromation for use " + f.getProfileId());
				UserDataVO t = pm.getProfile(f.getProfileId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, ORG_ID);
				f.setData(t.getDataMap());
			}
			
			if(vos.size() == 0)
				log.warn("No Results Found");
		} catch(SQLException sqle) {
			log.error("Problem Retrieving Data from Database.", sqle);
		} catch (DatabaseException e) {
			log.error("Problem Retrieveing Profile Data", e);
		}
		
		return vos;
	}
	
	/**
	 * Builds the search query for the Submittal List.  Since we are building
	 * a list of documents for solr we only care about published submissions.
	 * @return
	 */
	private String getSubmittalRecordQuery() {
		StringBuilder sb = new StringBuilder(750);
		sb.append("SELECT fs.*, fd.* FROM FORM_SUBMITTAL fs ");
		sb.append("left join FORM_DATA fd on fd.FORM_SUBMITTAL_ID = fs.FORM_SUBMITTAL_ID ");
		sb.append("left join FORM_DATA filter on filter.FORM_SUBMITTAL_ID = fs.FORM_SUBMITTAL_ID ");
		sb.append("and filter.FORM_FIELD_ID = ? and convert(nvarchar(max), filter.VALUE_TXT) = 'published' ");
		sb.append("WHERE FORM_ID = ? and filter.FORM_DATA_ID is not null ");
		sb.append("ORDER BY fs.FORM_SUBMITTAL_ID, fd.FORM_FIELD_ID");
		
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#purgeIndexItems(org.apache.solr.client.solrj.impl.HttpSolrServer)
	 */
	@Override
	public void purgeIndexItems(HttpSolrServer server) throws IOException {
		try {
			server.deleteByQuery(SearchDocumentHandler.INDEX_TYPE + ":" + getIndexType());
		} catch (Exception e) {
			throw new IOException(e);
		}
	}


	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTAbstractIndex#getIndexType()
	 */
	@Override
	public String getIndexType() {
		return SolrStoryVO.INDEX_TYPE;
	}
}