package com.depuysynthes.pa;
//java 8
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//SMT base lib
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
//WebCrescendo
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.form.FormAction;
import com.smt.sitebuilder.action.user.ProfileManager;
import com.smt.sitebuilder.action.user.ProfileManagerFactory;
import com.smt.sitebuilder.admin.action.OrganizationAction;
import com.smt.sitebuilder.common.constants.AdminConstants;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;

/****************************************************************************
 * <b>Title</b>: PatientAmbassadorStoriesTool.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data Tool for managing Patient Ambassador Stories in the
 * admintool.  Works off the data provided by the Patient Ambassador Story Form.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 3.0
 * @since Jan 9, 2015
 * @updates:
 * 	RjR code clean up May 23, 2017
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
		STATUS_ID("c0a80237eaa74b1245d3a04296472ffd"),
		EMAIL_CONSENT_ID("c0a80237feea61107a662ea060005c35"),
		MODAL_OPENED_ID("c0a80237fee851245d6f6f073c07573e"),
		SURGEON_NM("c0a802413aea94a8e9b70d61e07832fc"),
		HOSPITAL_NM("1e365f3fdb597794c0a8024b81e62b60"),
		INCISION_NM_ID("578d9b64dbb05ce3c0a802552fc1a8df"),
		IMPLANT_NM_ID("c79b06eadba570b3c0a80255f51d6312"),
		PERMISSION_TO_CONTACT("163adc3c2cadfa83c0a80255deabac8c"),
		AGREED_CONSENT_ID(""),
		FIRST_NM("f536599c804b4aec82ae62a2e47d02c5"),
		LAST_NM("d8b59cbc15d341aab1cbb5400b2455e6"),
		EMAIL_ADDRESS("8175fe87846847779b0d6db3e525cde8"),
		CITY_NM("132f22fd8e174adba1a944cf78acdd7d"),
		STATE_CD("52b6eb8f7eca4ccba22e0b7f8e857fea"),
		ZIP_CD("66f1df3528874dfd97c6d22ba9569a03"),

		//the ID of the form itself (containing all these fields)
		FORM_ID("c0a80241bb7b15cc1bff05ed771c527d");

		private final String id;
		PAFConst(String id) {
			this.id = id;
		}
		public String getId() {
			return id;
		}
	}

	public enum PAFStatus {saved, published, removed, republish}

	private static final String EXPORT_FILE_NAME = "PatientStories.xls";
	private static final String STORY_STS_ID = "storyStatusDataId";
	private static final String PERMISSION_TO_CONTACT_VALUE = "Yes";

	public PatientAmbassadorStoriesTool() {
		super();
	}

	public PatientAmbassadorStoriesTool(ActionInitVO init) {
		super(init);
	}

	/**
	 * Calls out to solr to remove the story from the index and adds the hidden
	 * flag to the FORM_DATA for the given submission.
	 */
	@Override
	public void delete(ActionRequest req) throws ActionException {
		String msg = "Story successfuly removed.";
		try (SolrActionUtil util = new SolrActionUtil(attributes)) {
			if(req.hasParameter("fsi"))
				util.removeDocument(req.getParameter("fsi"));

			//Hide Submission from list.
			writeStoryElement(getElement("1", req.getParameter("storyDeleteFieldId"), null), req.getParameter("fsi"));

			//Update Status Element.
			writeStoryElement(getElement(PAFStatus.removed.name(), PAFConst.STATUS_ID.getId(), req.getParameter(STORY_STS_ID)), req.getParameter("fsi"));
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
	public void update(ActionRequest req) throws ActionException {
		String submittalId = req.getParameter("fsi");
		String msg = "Story Successfully Saved";

		if (req.hasParameter("publish")) {
			//Create SolrStoryVO
			SolrStoryVO ssv = createStoryVO(req);

			try (SolrActionUtil util = new SolrActionUtil(attributes)) {
				//Update Status Element.
				writeStoryElement(getElement(PAFStatus.published.name(), PAFConst.STATUS_ID.getId(), req.getParameter(STORY_STS_ID )), submittalId);

				//Submit to Solr
				util.addDocument(ssv);

				msg = "Story Successfully Published";

			} catch(Exception e) {
				log.error("could not publish story", e);
				msg = "There was an error publishing the story.";
			}
		} else {
			//write consent flag -- this only happens once, when the admin tags the override field to set it to true (acceptPrivacyFlg=1)
			if (req.hasParameter("acceptPrivacyFlg"))
				this.setPrivacyFlag(submittalId);

			//Write story Title
			writeStoryElement(getElement(req.getParameter("storyTitle"), req.getParameter("storyTitleFieldId"), req.getParameter("storyTitleDataId")), submittalId);

			//Write story Text
			writeStoryElement(getElement(req.getParameter("storyText"), req.getParameter("storyTextFieldId"), req.getParameter("storyTextDataId")), submittalId);

			//Write Surgeon Name
			writeStoryElement(getElement(req.getParameter("surgeonNm"), req.getParameter("surgeonNameFieldId"), req.getParameter("surgeonNameDataId")), submittalId);

			//Write Hospital Name
			writeStoryElement(getElement(req.getParameter("hospitalNm"), req.getParameter("hospitalNameFieldId"), req.getParameter("hospitalNameDataId")), submittalId);

			//Write Incision type
			writeStoryElement(getElement(req.getParameter("incisionNm"), req.getParameter("incisionFieldId"), req.getParameter("incisionDataId")), submittalId);

			//Write Implant type
			writeStoryElement(getElement(req.getParameter("implantNm"), req.getParameter("implantFieldId"), req.getParameter("implantDataId")), submittalId);

			//save image if provided
			if (req.getFile("replacePhoto") != null) {
				String filePath = saveFile(req);
				writeStoryElement(getElement(filePath, PAFConst.PROFILE_IMAGE_ID.getId(), req.getParameter("filePathDataId")), submittalId);
			}

			if(req.hasParameter("storyStatusLevel") && req.getParameter("storyStatusLevel").equals(PAFStatus.published.name())) {
				//Update Status Element.
				writeStoryElement(getElement(PAFStatus.republish.name(), PAFConst.STATUS_ID.getId(), req.getParameter(STORY_STS_ID)), submittalId);
			} else {
				//Update Status Element.
				writeStoryElement(getElement(PAFStatus.saved.name(), PAFConst.STATUS_ID.getId(), req.getParameter(STORY_STS_ID)), submittalId);
			}
		}
		sendRedirect(req, msg);
	}

	/**
	 * Return either a single submission with all Form Data present or a list
	 * of Form Submissions matching given search criteria.
	 */
	@Override
	public void list(ActionRequest req) throws ActionException {		
		log.debug(" starting list");
		/*
		 * Choose whether we Export a report, retrieve a particular submission
		 * or list all results of a search.
		 */
		if (req.hasParameter("export")) {
			exportAllSubmissions(req);
		} else if (req.hasParameter("searchSubmitted")){
			List<FormTransactionVO> vos = retreiveAllSubmissions(req, true, false);
			this.putModuleData(vos, vos.size(), true);
		} else if (req.hasParameter("fsi")) {
			retrieveSubmittalData(req);
		}
	}

	/**
	 * Helper method that calls out to FormFacadeAction for the form data, 
	 * creates a SolrStoryVO and then stores the data from the form transaction
	 * on the solrVO.
	 * @param req
	 */
	private SolrStoryVO createStoryVO(ActionRequest req) {
		SolrStoryVO ssv = new SolrStoryVO();

		//Retrieve Submittal Data
		FormAction ffa = new FormAction(actionInit);
		ffa.setDBConnection(dbConn);
		ffa.setAttributes(attributes);
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
		ssv.setDetailImage(getFirstResponse(fields.get(PAFConst.PROFILE_IMAGE_ID.getId())));
		ssv.setCategories(fields.get(PAFConst.HOBBIES_ID.getId()).getResponses());
		if (fields.get(PAFConst.SURGEON_NM.getId()) != null)
			ssv.setSurgeonName(fields.get(PAFConst.SURGEON_NM.getId()).getResponses().get(0));
		if (fields.get(PAFConst.HOSPITAL_NM.getId()) != null)
			ssv.setSurgeonName(fields.get(PAFConst.HOSPITAL_NM.getId()).getResponses().get(0));
		ssv.setHierarchies(fields.get(PAFConst.JOINT_ID.getId()).getResponses());
		if (fields.get(PAFConst.INCISION_NM_ID.getId()) != null)
			ssv.setIncisionName(fields.get(PAFConst.INCISION_NM_ID.getId()).getResponses().get(0));
		if (fields.get(PAFConst.IMPLANT_NM_ID.getId()) != null)
			ssv.setImplantName(fields.get(PAFConst.IMPLANT_NM_ID.getId()).getResponses().get(0));
		ssv.setOtherHobbies(getFirstResponse(fields.get(PAFConst.OTHER_HOBBY_ID.getId())));
		ssv.setTitle(getFirstResponse(fields.get(PAFConst.STORY_TITLE_ID.getId())));
		ssv.setSummary(getFirstResponse(fields.get(PAFConst.STORY_TEXT_ID.getId())));
		ssv.addOrganization(req.getParameter(OrganizationAction.ORGANIZATION_ID));
		ssv.addRole(SecurityController.PUBLIC_ROLE_LEVEL);

		return ssv;
	}

	private String getFirstResponse(FormFieldVO field) {
		try {
			return field.getResponses().get(0);
		} catch (Exception e) {
			//ignoreable
		}
		return "";
	}

	/**
	 * Helper method for Writing a FORM_DATA record to the database.  Used to
	 * either insert or update a FormFieldVO record for a given Submission.
	 * We use this to write the Story title, text and hidden flag upon admin
	 * edit.
	 * @param req
	 */
	private void writeStoryElement(FormFieldVO vo, String submittalId) {
		boolean isInsert = StringUtil.checkVal(vo.getFormDataId()).length() == 0;
		String sql = (isInsert) ? getInsertQuery() : getUpdateQuery();
		log.debug(sql);
		int i = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			if (isInsert) {
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
		}
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
		sb.append("insert into FORM_DATA (form_field_group_id, FORM_SUBMITTAL_ID, ");
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
	 * sets the privacy flag for the submission.  This is an override for the Admin to use.
	 * @param formSubmittalId
	 */
	private void setPrivacyFlag(String formSubmittalId) {
		String sql = "update form_submittal set accepted_privacy_flg=1, update_dt=getDate() where form_submittal_id=?";
		log.debug(sql + " " + formSubmittalId);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			ps.setString(1, formSubmittalId);
			ps.executeUpdate();
		} catch (SQLException sqle) {
			log.error("could not update privacy flag for " + formSubmittalId, sqle);
		}
	}

	/**
	 * Call out to FormFacadeAction and retrieve the form data for the given
	 * FormSubmittalId
	 * @param req
	 */
	protected void retrieveSubmittalData(ActionRequest req) {
		log.debug(" retrieveSubmittalData " );
		FormAction ffa = new FormAction(actionInit);
		ffa.setDBConnection(dbConn);
		ffa.setAttributes(attributes);
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
	private List<FormTransactionVO> retreiveAllSubmissions(ActionRequest req, 
			boolean filterHidden, boolean isExportLeads) {

		List<FormTransactionVO> vos = new ArrayList<>();
		ProfileManager pm = ProfileManagerFactory.getInstance(attributes);

		//Retrieve Search Params and build Query
		String stateId = StringUtil.checkVal(req.getParameter("searchState"));
		String cityNm = StringUtil.checkVal(req.getParameter("searchCity"));
		String joint = StringUtil.checkVal(req.getParameter("searchJoint"));
		Date reportStart = Convert.formatSQLDate(Convert.formatStartDate(req.getParameter("reportStart"), "1/1/2015"));
		Date reportEnd = Convert.formatSQLDate(Convert.formatEndDate(req.getParameter("reportEnd")), true);
		String srQuery;
		if (isExportLeads) {
			srQuery = getLeadsQuery();
		} else {
			srQuery = getSubmittalRecordQuery(stateId, cityNm, joint, filterHidden);
		}
		log.debug("Query = " + srQuery);

		//Retrieve Data from DB
		int i = 1;
		try (PreparedStatement ps = dbConn.prepareStatement(srQuery)) {

			if (isExportLeads) {
				ps.setString(i++, PAFConst.PERMISSION_TO_CONTACT.getId());
				ps.setString(i++, PERMISSION_TO_CONTACT_VALUE);
				ps.setString(i++, req.getParameter("formId"));
				ps.setDate(i++, reportStart);
				ps.setDate(i++, reportEnd);
			} else {
				ps.setString(i++, PAFConst.JOINT_ID.getId());
				ps.setString(i++, PAFConst.STATUS_ID.getId());
				if(filterHidden)
					ps.setString(i++, PAFConst.HIDDEN_ID.getId());
				ps.setString(i++, req.getParameter("formId"));
				ps.setString(i++, PAFConst.JOINT_ID.getId());
				ps.setDate(i++, reportStart);
				ps.setDate(i++, reportEnd);
				if(stateId.length() > 0)
					ps.setString(i++, stateId);
				if(cityNm.length() > 0)
					ps.setString(i++, cityNm);
				if(joint.length() > 0)
					ps.setString(i++, joint + '%');
			}

			//Retrieve Results
			ResultSet rs = ps.executeQuery();

			//Process Results
			while(rs.next()) {
				FormTransactionVO vo = null;
				if (isExportLeads) {
					vo = new FormTransactionVO(rs);
				} else {
					String [] data = new String []{rs.getString("Joints"), rs.getString("STATUS"), rs.getString("STATUS_ID")};
					//Skip any results that have the Hidden flag (Have been deleted)
					if(filterHidden && Convert.formatBoolean(rs.getString("HIDE")))
						continue;
					vo = new FormTransactionVO(rs);
					vo.setUserExtendedInfo(data);
				}
				vos.add(vo);
			}

			//Loop over the Transactions and retrieve the profile data for them.
			for(FormTransactionVO f : vos) {
				UserDataVO t = pm.getProfile(f.getProfileId(), dbConn, ProfileManager.PROFILE_ID_LOOKUP, req.getParameter(OrganizationAction.ORGANIZATION_ID));
				f.setData(t.getDataMap());
			}

			if(vos.isEmpty())
				log.debug("No Results Found");
		} catch(SQLException sqle) {
			log.error("Problem Retrieving Data from Database.", sqle);
		} catch (DatabaseException e) {
			log.error("Problem Retrieveing Profile Data", e);
		}

		return vos;
	}

	/**
	 * Builds the search query for the Submittal List.  We always filter by
	 * FORM_ID, the Joint form_field_group_id and the hidden flag parameter.  Optionally 
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
		sb.append("string_agg( distinct replace(replace(cast(FD.VALUE_TXT as varchar), '").append(SearchDocumentHandler.HIERARCHY_DELIMITER);
		sb.append("Left', ''), '").append(SearchDocumentHandler.HIERARCHY_DELIMITER).append("Right', '') , ',')  as Joints, ");
		sb.append("cast(f.VALUE_TXT as varchar) as STATUS, f.FORM_DATA_ID as STATUS_ID ");
		if (filterHidden)
			sb.append(", cast(e.VALUE_TXT as varchar) as HIDE ");
		sb.append("from FORM_SUBMITTAL a ");
		sb.append("inner join FORM_DATA b on a.FORM_SUBMITTAL_ID = b.FORM_SUBMITTAL_ID ");
		sb.append("left outer join form_data fd on a.FORM_SUBMITTAL_ID = fd.FORM_SUBMITTAL_ID and fd.form_field_group_id= ? ");
		sb.append("left outer join FORM_DATA f on a.FORM_SUBMITTAL_ID = f.FORM_SUBMITTAL_ID and f.form_field_group_id= ? ");
		if (filterHidden)
			sb.append("left outer join FORM_DATA e on a.FORM_SUBMITTAL_ID = e.FORM_SUBMITTAL_ID and e.form_field_group_id= ? ");
		sb.append("left outer join PROFILE c on a.PROFILE_ID = c.PROFILE_ID ");
		sb.append("left outer join PROFILE_ADDRESS d on c.PROFILE_ID = d.PROFILE_ID ");
		sb.append("where FORM_ID = ? and b.form_field_group_id = ? and a.CREATE_DT between ? and ? ");

		//Add Optional Params to Where Clause
		if(state.length() > 0)
			sb.append("and d.STATE_CD = ? ");
		if(city.length() > 0)
			sb.append("and d.CITY_NM = ? ");
		if(joint.length() > 0)
			sb.append("and b.VALUE_TXT like ? ");

		//Add Ordering for newest first.
		sb.append("and (a.robot_flg is null or a.robot_flg=0) ");
		sb.append("group by a.FORM_SUBMITTAL_ID, f.value_txt, e.value_txt, f.form_data_id ");
		sb.append("order by a.CREATE_DT desc");

		return sb.toString();
	}
	
	/**
	 * Retrieves submissions for visitors who indicated that they want to have information
	 * sent to them.
	 * @return
	 */
	private String getLeadsQuery() {
		StringBuilder sb = new StringBuilder(750);
		sb.append("select distinct a.*, b.form_field_group_id, b.value_txt ");
		sb.append("from FORM_SUBMITTAL a ");
		sb.append("inner join FORM_DATA b on a.FORM_SUBMITTAL_ID = b.FORM_SUBMITTAL_ID ");
		sb.append("inner join FORM_DATA x on b.FORM_SUBMITTAL_ID = x.FORM_SUBMITTAL_ID ");
		sb.append("and x.form_field_group_id = ? and cast(x.value_txt as varchar) = ? ");
		sb.append("left outer join PROFILE c on a.PROFILE_ID = c.PROFILE_ID ");
		sb.append("left outer join PROFILE_ADDRESS d on c.PROFILE_ID = d.PROFILE_ID ");
		sb.append("where FORM_ID = ? and a.CREATE_DT between ? and ? ");

		//Add Ordering for newest first.
		sb.append("and (a.robot_flg is null or a.robot_flg=0) ");
		sb.append("group by a.FORM_SUBMITTAL_ID, b.form_field_group_id, b.value_txt ");
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
	private void exportAllSubmissions(ActionRequest req) {
		DataContainer results = new DataContainer();
		boolean exportLeads = Convert.formatBoolean(req.getParameter("exportLeads"));
		/*
		 * Retrieve Form Submittal Ids.  We may be doing an individual export so
		 * look on request for fsi param first.  If not present then get all
		 * ids between start and end date.
		 */
		List<FormTransactionVO> fsids = null;
		if(req.hasParameter("fsi")) {
			fsids = new ArrayList<>();
			FormTransactionVO f = new FormTransactionVO();
			f.setFormSubmittalId(req.getParameter("fsi"));
			fsids.add(f);
		} else {
			fsids = retreiveAllSubmissions(req, true, exportLeads);
		}
		Map<String, FormTransactionVO> t = new HashMap<>(fsids.size());

		//Iterate over list to get FormTransactionVOs

		FormAction ffa = new FormAction(actionInit);
		ffa.setDBConnection(dbConn);
		ffa.setAttributes(attributes);
		for(FormTransactionVO f : fsids) {
			req.setParameter("fsi", f.getFormSubmittalId());
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
	 * Stores the uploaded image to the file system
	 *
	 * @param req
	 * @return
	 */
	protected String saveFile(ActionRequest req) {
		// Build the file location
		log.debug("attempting to save any files uploaded");
		String pathToBinary = (String) getAttribute("pathToBinary");
		String uploadPathName = "/org/DPY_SYN/images/module/form/";
		List<FilePartDataBean> files = req.getFiles();
		// Write out each file to the file system
		try {
			FilePartDataBean fpdb = files.get(0);
			if (fpdb.isFileData()) {
				FileLoader fl = new FileLoader(attributes);
				fl.setFileName(fpdb.getFileName());
				fl.setPath(pathToBinary + uploadPathName);
				fl.setData(fpdb.getFileData());
				fl.setOverWrite(false);
				return "/binary" + uploadPathName + fl.writeFiles();
			}
		} catch (Exception e) {
			log.error("Error Writing Contact File", e);
		}
		return null;
	}

	//send the browser back to the appropriate page
	private void sendRedirect(ActionRequest req, String msg) {
		StringBuilder pg = new StringBuilder(250);
		pg.append("/").append(attributes.get(Constants.CONTEXT_NAME));
		pg.append(getAttribute(AdminConstants.ADMIN_TOOL_PATH));
		pg.append("?dataMod=true&actionId=").append(req.getParameter("actionId"));
		pg.append("&organizationId=").append(req.getParameter(OrganizationAction.ORGANIZATION_ID));
		pg.append("&cPage=").append(req.getParameter("cPage"));
		pg.append("&formId=").append(req.getParameter("formId"));
		pg.append("&searchSubmitted=true");
		pg.append("&searchJoint=").append(StringUtil.checkVal(req.getParameter("searchJoint")));
		pg.append("&searchCity=").append(StringUtil.checkVal(req.getParameter("searchCity")));
		pg.append("&searchState=").append(StringUtil.checkVal(req.getParameter("searchState")));
		pg.append("&reportStart=").append(StringUtil.checkVal(req.getParameter("reportStart")));
		pg.append("&reportEnd=").append(StringUtil.checkVal(req.getParameter("reportEnd")));
		pg.append("&msg=").append(msg);
		req.setAttribute(Constants.REDIRECT_REQUEST, Boolean.TRUE);
		req.setAttribute(Constants.REDIRECT_URL, pg.toString());
	}
}
