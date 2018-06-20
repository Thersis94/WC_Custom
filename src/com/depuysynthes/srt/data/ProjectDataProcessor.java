package com.depuysynthes.srt.data;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.depuysynthes.srt.SRTMasterRecordAction;
import com.depuysynthes.srt.SRTMilestoneAction;
import com.depuysynthes.srt.SRTProjectAction;
import com.depuysynthes.srt.util.SRTProjectIndexer;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTFileVO;
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.depuysynthes.srt.vo.SRTProjectMilestoneVO;
import com.depuysynthes.srt.vo.SRTProjectVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.IndexBeanDataMapper;
import com.siliconmtn.data.parser.PrefixBeanDataMapper;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction;
import com.smt.sitebuilder.common.constants.Constants;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.FormDataProcessor;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;

/****************************************************************************
 * <b>Title:</b> RequestDataTransactionHandler.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Data Transaction Handler for SRT Projects. 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 28, 2018
 ****************************************************************************/
public class ProjectDataProcessor extends FormDataProcessor {

	public enum ProjectField {
		PROJECT_NO("coProjectId"), PROJECT_NM("projectName"),
		PROJECT_TYPE("projectType"), PRIORITY("priority"),
		HOSPITAL_PO_NO("hospitalPONo"), SPECIAL_INSTRUCTIONS("specialInstructions"),
		ACTUAL_ROI("actualRoi"), SRT_CONTACT("srtContact"),
		ENGINEER_ID("engineerId"), SEC_ENGINEER_ID("secondaryEngineerId"),
		DESIGNER_ID("designerId"), SEC_DESIGNER_ID("secondaryDesignerId"),
		QUALITY_ENGINEER_ID("qualityEngineerId"), SEC_QUALITY_ENGINEER_ID("secondaryQualityEngineerId"),
		BUYER_ID("buyerId"), SEC_BUYER_ID("secondaryBuyerId"),
		SALES_ROSTER_ID("rosterId"), FUNCTIONAL_CHECK_ORDER_NO("funcCheckOrderNo"),
		MAKE_FROM_SCRATCH("makeFromScratch"), MAKE_FROM_ORDER_NO("makeFromOrderNo"),
		MFG_PO_TO_VENDOR("mfgPOToVendor"), SUPPLIER("supplierId"),
		ON_HOLD_FLG("projectHold"), CANCELLED_FLG("projectCancelled"),
		TRACKING_NO("warehouseTrackingNo"), MFG_DATE_CHANGE_REASON("mfgDtChangeReason"),
		SALES_ORDER_NO("warehouseSalesOrderNo");

		private String reqParam;
		private ProjectField(String reqParam) {
			this.reqParam = reqParam;
		}

		public String getReqParam() {return reqParam;}
	}

	private List<SRTFileVO> files;

	/**
	 * @param conn
	 * @param attributes
	 * @param req
	 */
	public ProjectDataProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
		super(conn, attributes, req);
		files = new ArrayList<>();
	}

	/**
	 * Process files on the form and Ensure we've written records to ProfileDocuments
	 * and SRT File.
	 * @param request
	 */
	private void writeSRTFileRecord() {
		SRTFileVO file = new SRTFileVO(req);
		file.setFileId(req.getParameter("profileDocumentId"));

		//Save SRTFile Record now that it's successfully saved to DocumentAction.
		files.add(file);
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractFormTransaction#flushTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	public void flushTransactions(DataContainer dc) {
		//Users can't delete a Master Record.
		return;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataTransaction#saveFormData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	public void saveFormData(FormTransactionVO data) throws DatabaseException {
		log.debug("Saving SRT Project Record");

		// Set the form fields that should not be saved as attributes, onto the request, with appropriate parameter names.
		// Remove from the form field map so they aren't saved as attributes.
		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			ProjectField param = EnumUtil.safeValueOf(ProjectField.class, entry.getValue().getSlugTxt());
			if (param != null) {
				req.setParameter(param.getReqParam(), StringUtil.checkVal(entry.getValue().getResponseText()).trim());
				iter.remove();
			}
		}

		log.info("Loading Project");
		// Get the project data
		SRTProjectVO project = new SRTProjectVO(req);

		//Move Milestone Date Records off request onto ledgerMap. 
		populateMilestoneRecords(project);

		//Add MasterRecords from the Request.
		project.setMasterRecords(new PrefixBeanDataMapper<SRTMasterRecordVO>(new SRTMasterRecordVO()).populate(req.getParameterMap(), SRTMasterRecordAction.SRT_MASTER_RECORD_ID));

		//Update Request Data.
		if(!StringUtil.isEmpty(project.getProjectId()) && !StringUtil.isEmpty(project.getRequestId())) {
			try {
				new RequestDataProcessor(dbConn, attributes, req).updateRequestData(data);
			} catch (com.siliconmtn.db.util.DatabaseException e) {
				throw new DatabaseException(e);
			}
		}

		// Save the project record
		saveProjectRecord(project);

		data.setFormSubmittalId(project.getProjectId());
	}

	/**
	 * Gather Milestones off Request.
	 * @param project
	 * @param data 
	 */
	private void populateMilestoneRecords(SRTProjectVO project) {
		SRTMilestoneAction sma = new SRTMilestoneAction();
		sma.setAttributes(attributes);
		sma.setDBConnection(dbConn);

		//Retrieve list of Milestones from DB for Request.
		List<SRTProjectMilestoneVO> milestones = sma.loadMilestoneData(SRTUtil.getOpCO(req), null, null, false);

		//Map List of Milestones to Map of MilestoneId, MilestoneVO.
		Map<String, SRTProjectMilestoneVO> mMap = milestones.stream().collect(Collectors.toMap(SRTProjectMilestoneVO::getMilestoneId, Function.identity()));

		//Load Milestone Data off the Request.
		List<SRTProjectMilestoneVO> reqMilestones = new IndexBeanDataMapper<>(new SRTProjectMilestoneVO()).populate(req.getParameterMap());

		//Loop vals off Request
		for(SRTProjectMilestoneVO m : reqMilestones) {

			//Attempt to find a Milestone for the current req Milestone.
			SRTProjectMilestoneVO mParam = mMap.get(m.getMilestoneId());

			//If we found an associated Milestone for the req Milestone, add it as a ledger Entry.
			if (mParam != null && m.getMilestoneDt() != null) {
				project.addLedgerDate(m.getMilestoneId(), m.getMilestoneDt());
			}
		}
	}

	/**
	 * Helper method that silos all DB Interactions for saving an SRT
	 * Project record.  Is called from a variety of places and acts as
	 * single point in the system where Project Records are saved.
	 *
	 * @param project
	 */
	public void saveProjectRecord(SRTProjectVO project) throws DatabaseException{
		boolean isAutoCommit = false;
		boolean isInsert = StringUtil.isEmpty(project.getProjectId());

		//Wrap entire Data Process in a DB Transactions.
		try {

			//Store current Commit Status.
			isAutoCommit = dbConn.getAutoCommit();

			//Turn off Auto Commit
			dbConn.setAutoCommit(false);

			//Save Project and update ProjectId on Request.
			DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
			dbp.save(project);
			req.setParameter(SRTProjectAction.SRT_PROJECT_ID, project.getProjectId());

			//Ensure Auto-populated Dates are on Project Record.
			if(isInsert)
				project.setCreateDt(Convert.getCurrentTimestamp());
			else
				project.setUpdateDt(Convert.getCurrentTimestamp());

			//Save Master Record XRs
			processMasterRecordXR(project);

			//Store current Status.
			String stat = StringUtil.checkVal(project.getProjectStatus());

			//Process Milestone Gates.
			processMilestones(project);

			//Check if status changed.  If so, update Project Record to reflect it.
			if(!stat.equals(StringUtil.checkVal(project.getProjectStatus()))) {
				dbp.save(project);
			}

			/*
			 * If The Database was in AutoCommit mode before, commit now.
			 * All dangerous work is complete.
			 */
			if(isAutoCommit) {
				dbConn.commit();
			}

			//Update Project record in Solr
			updateSolrProject(project);

		} catch(Exception e) {

			/*
			 * If something happened while saving, Rollback the DB and throw
			 * the Exception up the chain.
			 */
			log.error("Could not save SRT Project", e);
			DBUtil.rollback(dbConn);
			throw new DatabaseException("Error Saving SRT Project.", e);
		} finally {

			//If database was in autocommit mode originally, set it back.
			if(isAutoCommit)
				DBUtil.setAutoCommit(dbConn, isAutoCommit);
		}
	}


	/**
	 * Call the Project Indexer so that changes are updated in solr as soon
	 * as they're made by a user.
	 * @param project
	 */
	private void updateSolrProject(SRTProjectVO project) {
		Properties props = new Properties();
		props.putAll(attributes);
		SRTProjectIndexer indexer = new SRTProjectIndexer(props);
		indexer.setDBConnection(dbConn);
		indexer.indexItems(project.getProjectId());
	}

	/**
	 * Call out to MilestoneAction and process the Project Milestones.
	 * This process is destructive so tread lightly.
	 * @param project the Project Record to send through Milestone Processing.
	 */
	private void processMilestones(SRTProjectVO project) {
		SRTMilestoneAction sma = new SRTMilestoneAction();
		sma.setAttributes(attributes);
		sma.setDBConnection(dbConn);
		sma.processProject(project);
	}

	/**
	 * Process MasterRecordIds on request and add them as Master Record
	 * Project Xr Records.
	 * @param project
	 */
	private void processMasterRecordXR(SRTProjectVO project) {

		//Flush existing MasterRecordXRs
		flushMasterRecordXRs(project);

		//Save new MasterRecordXRs
		saveMasterRecordXRs(project);
	}

	/**
	 * Flush existing Master Record XR Values.
	 * @param project
	 */
	private void flushMasterRecordXRs(SRTProjectVO project) {

		try(PreparedStatement ps = dbConn.prepareStatement(buildMasterRecordFlushSql())) {
			ps.setString(1, project.getProjectId());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Flushing Master Record Project Xrs", e);
		}
	}

	/**
	 * Build the Deletion Query.
	 * @return
	 */
	private String buildMasterRecordFlushSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("delete from ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_SRT_MASTER_RECORD_PROJECT_XR where project_id = ? ");

		return sql.toString();
	}

	/**
	 * Generate new MasterRecordProjectXR Records for each masterRecordId
	 * on the request.
	 * @param project
	 */
	private void saveMasterRecordXRs(SRTProjectVO project) {
		try(PreparedStatement ps = dbConn.prepareStatement(buildMasterRecordXrInsertSql())) {
			int i;
			UUIDGenerator uuid = new UUIDGenerator();
			for(SRTMasterRecordVO mrv : project.getMasterRecords()) {
				i = 1;
				ps.setString(i++, uuid.getUUID());
				ps.setString(i++, mrv.getMasterRecordId());
				ps.setString(i++, project.getProjectId());
				ps.setInt(i++, mrv.getPartCount());
				ps.setTimestamp(i++, Convert.getCurrentTimestamp());
				ps.addBatch();
			}

			ps.executeBatch();
		} catch (SQLException e) {
			log.error("Error Flushing Master Record Project Xrs", e);
		}
	}

	/**
	 * Build Master Record Project Xr Insert Sql.
	 * @return
	 */
	private String buildMasterRecordXrInsertSql() {
		StringBuilder sql = new StringBuilder(200);
		sql.append("insert into ").append(attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_srt_master_record_project_xr (master_record_project_xr_id, ");
		sql.append("master_record_id, project_id, part_count, create_Dt) values (?,?,?,?,?)");
		return sql.toString();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataTransaction#saveFieldData(com.smt.sitebuilder.data.vo.FormTransactionVO)
	 */
	@Override
	public void saveFieldData(FormTransactionVO data) throws DatabaseException {
		//Not Implemented
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataTransaction#loadTransactions(com.smt.sitebuilder.data.DataContainer)
	 */
	@Override
	public DataContainer loadTransactions(DataContainer dc) throws DatabaseException {
		//Not Implemented
		return null;
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.data.AbstractDataTransaction#saveFile(com.smt.sitebuilder.action.file.transfer.ProfileDocumentAction, com.siliconmtn.util.databean.FilePartDataBean)
	 */
	@Override
	public void saveFile(ProfileDocumentAction pda, FilePartDataBean fpdb) throws ActionException, FileWriterException, InvalidDataException {
		super.saveFile(pda, fpdb);
		writeSRTFileRecord();
	}

	@Override
	public void saveFiles(FormTransactionVO data) {
		super.saveFiles(data);

		//Write SRT Files after processing all files.
		if(!files.isEmpty()) {
			try {
				new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA)).executeBatch(files, true);
			} catch (com.siliconmtn.db.util.DatabaseException e) {
				log.error("Error saving Project Attachments", e);
			}
		}
	}
}