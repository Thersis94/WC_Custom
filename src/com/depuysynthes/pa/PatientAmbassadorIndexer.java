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

import org.apache.solr.client.solrj.SolrClient;

import com.depuysynthes.pa.PatientAmbassadorStoriesTool.PAFConst;
import com.siliconmtn.exception.DatabaseException;
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
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#addIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@SuppressWarnings("resource")
	@Override
	public void addIndexItems(SolrClient server) {
		SolrActionUtil solrUtil = new SolrActionUtil(server);
		List<FormTransactionVO> formVOs = retreiveAllSubmissions();

		// Loop over each form transaction and turn it into a SolrStoryVO for processing
		for (FormTransactionVO vo : formVOs) {
			try {
				SolrStoryVO story = buildSolrStoryVO(vo);
				log.debug("adding to Solr: " + story.getTitle());
				solrUtil.addDocument(story);
			} catch (Exception e) {
				log.error("could not create document to add to Solr", e);
			}
		}
	}

	/**
	 * Turns the form transaction vo into a SolrStoryVO that we
	 * can just drop into the SolrActionutil
	 * @param vo
	 * @return
	 */
	private SolrStoryVO buildSolrStoryVO(FormTransactionVO vo) {
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
		if(fields.get(PAFConst.PROFILE_IMAGE_ID.getId()) != null){
			ssv.setDetailImage(fields.get(PAFConst.PROFILE_IMAGE_ID.getId()).getResponses().get(0));
		}		
		ssv.setCategories(fields.get(PAFConst.HOBBIES_ID.getId()).getResponses());
		if (fields.get(PAFConst.SURGEON_NM.getId()) != null)
			ssv.setSurgeonName(fields.get(PAFConst.SURGEON_NM.getId()).getResponses().get(0));
		if (fields.get(PAFConst.HOSPITAL_NM.getId()) != null)
			ssv.setSurgeonName(fields.get(PAFConst.HOSPITAL_NM.getId()).getResponses().get(0));
		ssv.setHierarchies(fields.get(PAFConst.JOINT_ID.getId()).getResponses());

		if (fields.get(PAFConst.INCISION_NM_ID.getId()) != null)
			ssv.setSurgeonName(fields.get(PAFConst.INCISION_NM_ID.getId()).getResponses().get(0));

		if (fields.get(PAFConst.IMPLANT_NM_ID.getId()) != null)
			ssv.setSurgeonName(fields.get(PAFConst.IMPLANT_NM_ID.getId()).getResponses().get(0));

		if (fields.get(PAFConst.OTHER_HOBBY_ID.getId()) != null) {
			ssv.setOtherHobbies(fields.get(PAFConst.OTHER_HOBBY_ID.getId()).getResponses().get(0));
		}
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
		List<FormTransactionVO> vos = new ArrayList<>();
		String srQuery = getSubmittalRecordQuery();
		log.debug("Query = " + srQuery + "|" + PAFConst.FORM_ID.getId() + "|" + PAFConst.STATUS_ID.getId());

		//Retrieve Data from DB
		String submittalId = "";
		String fieldId = "";
		FormTransactionVO vo = null;
		FormFieldVO field = null;
		try (PreparedStatement  ps = dbConn.prepareStatement(srQuery)) {
			ps.setString(1, PAFConst.STATUS_ID.getId());
			ps.setString(2, PAFConst.FORM_ID.getId());

			ResultSet rs = ps.executeQuery();
			while(rs.next()) {
				String formSubmittalId = rs.getString("FORM_SUBMITTAL_ID");

				// If we are dealing with a new submission store the old one
				// and create a new vo for the new submission.
				if (!submittalId.equals(formSubmittalId)) {
					if (vo != null) vos.add(vo);
					vo = new FormTransactionVO(rs);
					submittalId = formSubmittalId;
				}

				// If we are dealing with a new field we add the current
				// one to the vo and create a new field vo.
				// This needs to be done before the vo so that all fields are
				// associated with the proper submissions.
				if (!fieldId.equals(rs.getString("FORM_FIELD_ID")) || !submittalId.equals(formSubmittalId)) {
					if (field != null && vo != null)
						vo.addCustomData(fieldId, field);
					field = new FormFieldVO(rs);
					fieldId = rs.getString("FORM_FIELD_ID");
				}
				if (field != null)
					field.addResponse(rs.getString("VALUE_TXT"));
			}

			// Add the dangling field and vo.
			if (vo != null) {
				if (field != null) vo.addCustomData(fieldId, field);
				vos.add(vo);
			}

			//Loop over the Transactions and retrieve the profile data for them.
			Map<String, Object> vals = new HashMap<>();
			vals.put(Constants.ENCRYPT_KEY, config.get(Constants.ENCRYPT_KEY));
			ProfileManager pm = new SBProfileManager(vals);
			pm.setOrganizationId(ORG_ID);
			pm.populateRecords(dbConn, vos);

			if (vos.isEmpty())
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
		sb.append("and filter.VALUE_TXT='published' and filter.FORM_FIELD_ID=? ");
		sb.append("WHERE FORM_ID = ? and filter.FORM_DATA_ID is not null ");
		sb.append("ORDER BY fs.FORM_SUBMITTAL_ID, fd.FORM_FIELD_ID");
		return sb.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.search.SMTIndexIntfc#purgeIndexItems(org.apache.solr.client.solrj.SolrClient)
	 */
	@Override
	public void purgeIndexItems(SolrClient server) throws IOException {
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


	@Override
	public void indexItems(String... id) {
		// Nothing uses this class's indexItems method right now.
		// should it be required it can be filled out at that time.
	}
}