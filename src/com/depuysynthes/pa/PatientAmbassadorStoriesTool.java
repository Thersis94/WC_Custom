/**
 * 
 */
package com.depuysynthes.pa;

import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.form.FormFacadeAction;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: PatientAmbassadrStoriesTool.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Data Tool for managing Patient Ambassador Stories in the
 * admintool.  Works off the data provided by the Patient Ambassador Story Form.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 9, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class PatientAmbassadorStoriesTool extends SBActionAdapter {
	public enum PAFConst {
		JOINT_ID("c0a80241bba73b0a49493776bd9f999d"),
		HIDDEN_ID("c0a80237dfd90f5476e88743821c96b3"),
		PROFILE_IMAGE_ID("c0a80241bbb8c55aae6fabe3fe143767"),
		HOBBIES_ID("c0a80241bba4e916f3c24b11c6d6c26f"),
		OTHER_HOBBY_ID("c0a80241bf9cfab2648d4393cf3bb062"),
		HAS_REPLACED_ID("c0a80241bba861705b540c2e91d3bf6a"),
		LIFE_BEFORE_ID("c0a80241bbaa0d063448036ce9a37a9d"),
		TURNING_POINT_ID("c0a80241bbaaa185cd2c542570a03b69"),
		LIFE_AFTER_ID("c0a80241bbab26d391814dedd1b1857d"),
		ADVICE_ID("c0a80241bbb2d50c11b6f3652f008aa6"),
		STORY_TITLE_ID("c0a80237dfd89c30f3b7848d499d28a0"),
		STORY_TEXT_ID("c0a80237dfd8ca8957bec8575c5f35e5"),
		STATUS_ID("c0a80237eaa74b1245d3a04296472ffd");

		private final String id;
		PAFConst(String id) {
			this.id = id;
		}
		public String getId() {
			return id;
		}
	}
	
	public enum PAFStatus {saved, published, removed}
	
	private static final String EXPORT_FILE_NAME = "PatientStories.xls";
	/**
	 * 
	 */
	public PatientAmbassadorStoriesTool() {
	}
	
	public PatientAmbassadorStoriesTool(ActionInitVO init) {
		super(init);
	}
	
	/**
	 * Calls out to solr to remove the story from the index and adds the hidden
	 * flag to the FORM_DATA for the given submission.
	 */
	public void delete(SMTServletRequest req) throws ActionException {
		String msg = "Story successfuly removed.";
		try {
			if(req.hasParameter("fsi"))
				new SolrActionUtil(attributes).removeDocument(req.getParameter("fsi"));

			//Hide Submission from list.
			writeStoryElement(getElement("1", req.getParameter("storyDeleteFieldId"), null), req.getParameter("fsi"));

			//Update Status Element.
			writeStoryElement(getElement(PAFStatus.removed.name(), PAFConst.STATUS_ID.getId(), req.getParameter("storyStatusDataId")), req.getParameter("fsi"));
		} catch (Exception e) {
			msg = "There was an error removing the story from the site";
		}
		sendRedirect(req, msg);
	}
	
	/**
	 * On update, Save the story and title information on the FORM_DATA table
	 * and call out to solr to update the index.
	 */
	@Override 
	public void update(SMTServletRequest req) throws ActionException {
		
		String submittalId = req.getParameter("fsi");
		String msg = "Story Successfully Saved";
		
		if(req.hasParameter("publish")) {

			//Create SolrStoryVO
			SolrStoryVO ssv = createStoryVO(req);

			try {
				//Submit to Solr
				new SolrActionUtil(attributes).addDocument(ssv);

				//Update Status Element.
				writeStoryElement(getElement(PAFStatus.published.name(), PAFConst.STATUS_ID.getId(), req.getParameter("storyStatusDataId")), submittalId);
				log.debug("Status Written");

				msg = "Story Successfully Published";

			} catch(Exception e) {
				log.error(e);
				msg = "There was an error publishing the story.";
			}
		} else {

			//Write story Title
			writeStoryElement(getElement(req.getParameter("storyTitle"), req.getParameter("storyTitleFieldId"), req.getParameter("storyTitleDataId")), submittalId);
			log.debug("Title Written");

			//Write story Text
			writeStoryElement(getElement(req.getParameter("storyText"), req.getParameter("storyTextFieldId"), req.getParameter("storyTextDataId")), submittalId);
			log.debug("Text Written");

			//Update Status Element.
			writeStoryElement(getElement(PAFStatus.saved.name(), PAFConst.STATUS_ID.getId(), req.getParameter("storyStatusDataId")), submittalId);
			log.debug("Status Written");
		}
		sendRedirect(req, msg);
	}
	
	/**
	 * Return either a single submission with all Form Data present or a list
	 * of Form Submissions matching given search criteria.
	 */
	@Override
	public void list(SMTServletRequest req) throws ActionException {		

		/*
		 * Choose whether we Export a report, retrieve a particular submission
		 * or list all results of a search.
		 */
		if(req.hasParameter("export")) {
			exportAllSubmissions(req);
		} else if(req.hasParameter("searchSubmitted")){
			List<FormTransactionVO> vos = retreiveAllSubmissions(req, true);
			this.putModuleData(vos, vos.size(), true);
		} else if(req.hasParameter("fsi")) {
			retrieveSubmittalData(req);
		}
	}
	
	/**
	 * Helper method that calls out to FormFacadeAction for the form data, 
	 * creates a SolrStoryVO and then stores the data from the form transaction
	 * on the solrVO.
	 * @param req
	 */
	private SolrStoryVO createStoryVO(SMTServletRequest req) {
		SolrStoryVO ssv = new SolrStoryVO();

		//Retrieve Submittal Data
		FormFacadeAction ffa = new FormFacadeAction(actionInit);
		ffa.setDBConnection(dbConn);
		ffa.setAttributes(attributes);
		//req.setParameter("formId", PAFConst.FORM_ID.getId());
		DataContainer dc = ffa.retrieveSubmittedForm(req);

		//Get the Fields off the TransactionVO.
		FormTransactionVO trans = dc.getTransactions().values().iterator().next();
		Map<String, FormFieldVO> fields = trans.getCustomData();

		//Store Data on the SolrStoryVO
		ssv.setDocumentId(trans.getFormSubmittalId());
		ssv.setAuthor(trans.getFirstName());
		ssv.setZip(trans.getZipCode());
		ssv.setCity(trans.getCity());
		ssv.setState(trans.getState());
		ssv.setLat(trans.getLatitude().toString());
		ssv.setLng(trans.getLongitude().toString());
		ssv.setDetailImage(fields.get(PAFConst.PROFILE_IMAGE_ID.getId()).getResponses().get(0));
		ssv.setCategories(fields.get(PAFConst.HOBBIES_ID.getId()).getResponses());
		ssv.setHierarchies(fields.get(PAFConst.JOINT_ID.getId()).getResponses());
		ssv.setOtherHobbies(fields.get(PAFConst.OTHER_HOBBY_ID.getId()).getResponses().get(0));
		ssv.setTitle(fields.get(PAFConst.STORY_TITLE_ID.getId()).getResponses().get(0));
		ssv.setSummary(fields.get(PAFConst.STORY_TEXT_ID.getId()).getResponses().get(0));
		ssv.addOrganization(req.getParameter("organizationId"));
		ssv.addRole("0");

		return ssv;
	}

	/**
	 * Helper method for Writing a FORM_DATA record to the database.  Used to
	 * either insert or update a FormFieldVO record for a given Submission.
	 * We use this to write the Story title, text and hidden flag upon admin
	 * edit.
	 * @param req
	 */
	private void writeStoryElement(FormFieldVO vo, String submittalId) {
		String sql = null;
		boolean isInsert = StringUtil.checkVal(vo.getFormDataId()).length() == 0;
		
		//Get Query
		if(isInsert)
			sql = getInsertQuery();
		else 
			sql = getUpdateQuery();
		log.debug(sql);
		PreparedStatement ps = null;
		
		//Call to Database for insert or update.
		int i = 1;
		try {
			ps = dbConn.prepareStatement(sql);
			if(isInsert) {
				ps.setString(i++, vo.getFormFieldId());
				ps.setString(i++, submittalId);
				ps.setInt(i++, 0);
			}
			ps.setString(i++, vo.getResponses().get(0));
			ps.setTimestamp(i++, Convert.getCurrentTimestamp());
			ps.setString(i++, isInsert ? new UUIDGenerator().getUUID() : vo.getFormDataId());
			
			ps.executeUpdate();
		} catch (Exception e) {
			log.error("Error saving Story Field: " + vo.getFormDataId(), e);
		} finally { DBUtil.close(ps);}
	}

	/**
	 * Helper method for preparing a FormFieldVO for insert/update.
	 * @param valParam - The value of the FormFieldVO
	 * @param fieldParam - The related FormFieldId
	 * @param dataParam - The Form_data_id if present.  determines insert/update.
	 * @return
	 */
	private FormFieldVO getElement(String valParam, String fieldParam, String dataParam) {
		FormFieldVO f = new FormFieldVO();
		f.addResponse(valParam);
		f.setFormFieldId(fieldParam);
		f.setFormDataId(dataParam);
		
		return f;
	}

	/**
	 * Helper method that returns the insert query for a FormFieldVO
	 * @param req
	 */
	private String getInsertQuery() {
		StringBuilder sb = new StringBuilder(130);
		sb.append("insert into FORM_DATA (FORM_FIELD_ID, FORM_SUBMITTAL_ID, ");
		sb.append("DATA_ENC_FLG, VALUE_TXT, CREATE_DT, FORM_DATA_ID) values(?,?,?,?,?,?)");
		return sb.toString();
	}
	
	/**
	 * Helper method that returns the update query for a FormFieldVO
	 * @return
	 */
	private String getUpdateQuery() {
		StringBuilder sb = new StringBuilder(75);
		sb.append("update FORM_DATA set VALUE_TXT = ?, UPDATE_DT = ? where FORM_DATA_ID = ?");
		return sb.toString();
	}

	/**
	 * Call out to FormFacadeAction and retrieve the form data for the given
	 * FormSubmittalId
	 * @param req
	 */
	private void retrieveSubmittalData(SMTServletRequest req) {
		FormFacadeAction ffa = new FormFacadeAction(actionInit);
		ffa.setDBConnection(dbConn);
		ffa.setAttributes(attributes);
		//req.setParameter("formId", PAFConst.FORM_ID.getId());
		DataContainer dc = ffa.retrieveSubmittedForm(req);
		this.putModuleData(dc.getTransactions().get(req.getParameter("fsi")), 1, true);
	}

	/**
	 * Retrieve all form submittals related to Patient Ambassador Stories
	 * and bring back some relevant information about them.  Filtered by
	 * city, state and joint search parameters as well as hidden flag. 
	 * @param req
	 * @return 
	 */
	private List<FormTransactionVO> retreiveAllSubmissions(SMTServletRequest req, boolean filterHidden) {

		List<FormTransactionVO> vos = new ArrayList<FormTransactionVO>();
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);

		//Retrieve Search Params and build Query
		String stateId = StringUtil.checkVal(req.getParameter("searchState"));
		String cityNm = StringUtil.checkVal(req.getParameter("searchCity"));
		String joint = StringUtil.checkVal(req.getParameter("searchJoint"));
		String srQuery = getSubmittalRecordQuery(stateId, cityNm, joint, filterHidden);
		log.debug("Query = " + srQuery);

		//Retrieve Data from DB
		PreparedStatement ps = null;
		int i = 1;
		try {
			ps = dbConn.prepareStatement(srQuery);
			ps.setString(i++, PAFConst.STATUS_ID.getId());
			if(filterHidden)
				ps.setString(i++, PAFConst.HIDDEN_ID.getId());
			ps.setString(i++, req.getParameter("formId"));
			ps.setString(i++, PAFConst.JOINT_ID.getId());
			if(stateId.length() > 0)
				ps.setString(i++, stateId);
			if(cityNm.length() > 0)
				ps.setString(i++, cityNm);
			if(joint.length() > 0)
				ps.setString(i++, joint + '%');

			//Retrieve Results
			ResultSet rs = ps.executeQuery();

			//Process Results
			while(rs.next()) {
				String [] data = new String []{rs.getString("Joints"), rs.getString("STATUS"), rs.getString("STATUS_ID")};
				//Skip any results that have the Hidden flag (Have been deleted)
				if(Convert.formatBoolean(rs.getString("HIDE")))
					continue;
				FormTransactionVO vo = new FormTransactionVO(rs);
				vo.setUserExtendedInfo(data);
				
				vos.add(vo);
			}
			
			//Loop over the Transactions and retrieve the profile data for them.
			for(FormTransactionVO f : vos) {
				UserDataVO t = pm.getProfile(f.getProfileId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, req.getParameter("organizationId"));
				f.setData(t.getDataMap());
			}
			
			if(vos.size() == 0)
				log.debug("No Results Found");
		} catch(SQLException sqle) {
			log.error("Problem Retrieving Data from Database.", sqle);
		} catch (DatabaseException e) {
			log.error("Problem Retrieveing Profile Data", e);
		} finally {DBUtil.close(ps);}
		
		return vos;
	}

	/**
	 * Builds the search query for the Submittal List.  We always filter by
	 * FORM_ID, the Joint FORM_FIELD_ID and the hidden flag parameter.  Optionally 
	 * can further filter by City, State or selected Joint.
	 * @param state - Optional State field for filtering
	 * @param city - Optional City field for filtering
	 * @param joint - Optional Joint field for filtering
	 * @param filterHidden - Optional Form_Data field param that we use to determine if a record has been deleted.
	 * @return
	 */
	private String getSubmittalRecordQuery(String state, String city, String joint, boolean filterHidden) {
		StringBuilder sb = new StringBuilder(750);
		sb.append("select distinct a.*, ");
		sb.append("STUFF((SELECT ', ' + cast(FD.VALUE_TXT as nvarchar) FROM FORM_DATA FD ");
		sb.append("WHERE (FD.FORM_SUBMITTAL_ID = a.FORM_SUBMITTAL_ID and FD.FORM_FIELD_ID = b.FORM_FIELD_ID) ");
		sb.append("FOR XML PATH('')), 1, 1, '') Joints, cast(f.VALUE_TXT as nvarchar) as 'STATUS', f.FORM_DATA_ID as 'STATUS_ID' ");
		if(filterHidden)
			sb.append(", cast(e.VALUE_TXT as nvarchar) as 'HIDE' ");
		sb.append("from FORM_SUBMITTAL a ");
		sb.append("inner join FORM_DATA b on a.FORM_SUBMITTAL_ID = b.FORM_SUBMITTAL_ID ");
		sb.append("left outer join FORM_DATA f on a.FORM_SUBMITTAL_ID = f.FORM_SUBMITTAL_ID and f.FORM_FIELD_ID= ? ");
		if(filterHidden)
			sb.append("left outer join FORM_DATA e on a.FORM_SUBMITTAL_ID = e.FORM_SUBMITTAL_ID and e.FORM_FIELD_ID= ? ");
		sb.append("left outer join PROFILE c on a.PROFILE_ID = c.PROFILE_ID ");
		sb.append("left outer join PROFILE_ADDRESS d on c.PROFILE_ID = d.PROFILE_ID ");
		sb.append("where FORM_ID = ? and b.FORM_FIELD_ID = ? ");

		//Add Optional Params to Where Clause
		if(state.length() > 0)
			sb.append("and d.STATE_CD = ? ");
		if(city.length() > 0)
			sb.append("and d.CITY_NM = ? ");
		if(joint.length() > 0)
			sb.append("and b.VALUE_TXT like ? ");

		//Add Ordering for newest first.
		sb.append("order by a.CREATE_DT desc");
		return sb.toString();
	}
	
	/**
	 * Method retrieves all Submittal ids for a given date range then calls out
	 * to FormFacadeAction on each of them to retrieve the Submission Data.
	 * The data is added to a DataContainer and then we send to the ReportVO
	 * for formatting before redirecting back to the browser.
	 * @param req
	 */
	private void exportAllSubmissions(SMTServletRequest req) {
		DataContainer results = new DataContainer();
		String formId = req.getParameter("formId");
		Date startDate = Convert.formatSQLDate(Convert.formatStartDate(req.getParameter("startDate"), "1/1/2015"));
		Date endDate = Convert.formatSQLDate(Convert.formatEndDate(req.getParameter("endDate")), true);

		/*
		 * Retrieve Form Submittal Ids.  We may be doing an individual export so
		 * look on request for fsi param first.  If not present then get all
		 * ids between start and end date.
		 */
		List<String> fsids = null;
		if(req.hasParameter("fsi")) {
			fsids = new ArrayList<String>();
			fsids.add(req.getParameter("fsi"));
		} else {
			fsids = getAllSubmissionIds(formId, startDate, endDate);
		}
		Map<String, FormTransactionVO> t = new HashMap<String, FormTransactionVO>(fsids.size());
		
		//Iterate over list to get FormTransactionVOs
		FormFacadeAction ffa = new FormFacadeAction(actionInit);
		ffa.setDBConnection(dbConn);
		ffa.setAttributes(attributes);
		for(String f : fsids) {
			req.setParameter("fsi", f);
			DataContainer dc = ffa.retrieveSubmittedForm(req);
			t.putAll(dc.getTransactions());
		}
		log.debug("retrieved " + t.size() + " submissions.");
		results.setTransactions(t);
		
		//Build Report
		PatientAmbassadorReportVO report = new PatientAmbassadorReportVO(EXPORT_FILE_NAME);
		report.setSiteUrl(req.getHostName());
		report.setData(results);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
	}
	
	/**
	 * Helper method that retrieves all the Submittal Ids for a form in the given
	 * date range.  
	 * @param formId
	 * @param startDate
	 * @param endDate
	 * @return
	 */
	private List<String> getAllSubmissionIds(String formId, Date startDate, Date endDate) {
		List<String> ids = new ArrayList<String>();
		StringBuilder sb = new StringBuilder(100);
		sb.append("select FORM_SUBMITTAL_ID from FORM_SUBMITTAL where FORM_ID = ? ");
		sb.append("and CREATE_DT between ? and ?");
		log.debug(sb.toString() + " | " + startDate + " | " + endDate);
		int i = 1;
		PreparedStatement ps = null;
		try {
			ps = dbConn.prepareStatement(sb.toString());
			ps.setString(i++, formId);
			ps.setDate(i++, startDate);
			ps.setDate(i++, endDate);
			ResultSet rs = ps.executeQuery();
			
			while(rs.next())
				ids.add(rs.getString("FORM_SUBMITTAL_ID"));
		} catch(Exception e) {
			log.error("Could not retrieve list of Submission Ids", e);
		} finally {DBUtil.close(ps);}
		
		return ids;
	}
	//send the browser back to the appropriate page
	private void sendRedirect(SMTServletRequest req, String msg) {
		StringBuilder pg = new StringBuilder();
		pg.append("/").append(attributes.get(Constants.CONTEXT_NAME));
		pg.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		pg.append("?dataMod=true&actionId=").append(req.getParameter("actionId"));
		pg.append("&organizationId=").append(req.getParameter("organizationId"));
		pg.append("&cPage=").append(req.getParameter("cPage"));
		pg.append("&formId=").append(req.getParameter("formId"));
		pg.append("&searchSubmitted=true");
		pg.append("&searchJoint=").append(StringUtil.checkVal(req.getParameter("searchJoint")));
		pg.append("&searchCity=").append(StringUtil.checkVal(req.getParameter("searchCity")));
		pg.append("&searchState=").append(StringUtil.checkVal(req.getParameter("searchState")));
		pg.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, pg.toString());
	}
}
