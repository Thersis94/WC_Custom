package com.depuysynthes.srt.data;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.depuysynthes.srt.SRTMasterRecordAction;
import com.depuysynthes.srt.util.SRTUtil;
import com.depuysynthes.srt.vo.SRTFileVO;
import com.depuysynthes.srt.vo.SRTMasterRecordVO;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionRequest;
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
import com.smt.sitebuilder.data.AbstractDataProcessor;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.vo.FormFieldVO;
import com.smt.sitebuilder.data.vo.FormTransactionVO;

/****************************************************************************
 * <b>Title:</b> RequestDataTransactionHandler.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Data Transaction Handler for SRT Master Records. 
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Feb 28, 2018
 ****************************************************************************/
public class MasterRecordDataProcessor extends AbstractDataProcessor {

	public enum MasterRecordField {
		PART_NO("partNo"), PROD_DESC("titleTxt"), QUALITY_SYS("qualitySystemId"),
		PROD_TYPE("prodTypeId"), COMPLEXITY("complexityId"),PROD_CATEGORY("prodCatId"),
		MAKE_FROM_PART_NO("makeFromPartNos"), PROD_FAMILY("prodFamilyId"),
		TOTAL_BUILT("totalBuilt"),
		OBSOLETE_FLG("obsoleteFlg"), OBSOLETE_REASON("obsoleteReason");
		private String reqParam;
		private MasterRecordField(String reqParam) {
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
	public MasterRecordDataProcessor(SMTDBConnection conn, Map<String, Object> attributes, ActionRequest req) {
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
		log.debug("Saving SRT Master Record");

		// Set the form fields that should not be saved as attributes, onto the request, with appropriate parameter names.

		/*
		 * If we are submitting over ajax, assume params have been set
		 * correctly already for VO to populate.
		 */
		if(!req.hasParameter("amid")) {
			Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
			while (iter.hasNext()) {
				Map.Entry<String, FormFieldVO> entry = iter.next();
				MasterRecordField param = EnumUtil.safeValueOf(MasterRecordField.class, entry.getValue().getSlugTxt());
				if (param != null) {
					req.setParameter(param.getReqParam(), StringUtil.checkVal(entry.getValue().getResponseText()).trim());
					iter.remove();
				}
			}
		}

		// Build the Master Record VO from request.
		SRTMasterRecordVO mrv = new SRTMasterRecordVO(req);

		//Load Attribute from Db.
		List<String> attrs = loadAttributesList();

		//Process Attributes off the DataMap and store on MasterRecordVO.
		matchAttrs(attrs, mrv, data);

		saveMasterRecordData(mrv);

		data.setFormSubmittalId(mrv.getMasterRecordId());

		log.debug("Process Master Record");
	}

	/*
	 * Controls saving an SRTMasterRecordVO.  Can be called externally as
	 * a single point of execution for Saving.
	 */
	public void saveMasterRecordData(SRTMasterRecordVO mrv) throws DatabaseException {
		DBProcessor dbp = new DBProcessor(dbConn, (String)attributes.get(Constants.CUSTOM_DB_SCHEMA));
		try {
			dbp.save(mrv);
			req.setParameter(SRTMasterRecordAction.SRT_MASTER_RECORD_ID, mrv.getMasterRecordId());

			//Process Master Record Attributes
			processAttributes(mrv);

			//Put Master Record back on request at retrievable location.
			req.setAttribute(SRTMasterRecordAction.MASTER_RECORD_DATA, mrv);
		} catch(Exception e) {
			throw new DatabaseException(e);
		}
	}

	/**
	 * Build MR ATTR XR Records and save them.
	 * @param mrv
	 * @param data
	 */
	private void processAttributes(SRTMasterRecordVO mrv) {
		flushAttrs(mrv);

		saveAttrs(mrv);
	}

	/**
	 * Match MR Attribute Records and save them on the MasterRecordVO
	 * @param attrs
	 * @param mrv
	 * @param data
	 */
	private void matchAttrs(List<String> attrs, SRTMasterRecordVO mrv, FormTransactionVO data) {
		Iterator<Map.Entry<String, FormFieldVO>> iter = data.getCustomData().entrySet().iterator();
		while (iter.hasNext()) {
			Map.Entry<String, FormFieldVO> entry = iter.next();
			if (attrs.contains(entry.getValue().getSlugTxt())) {
				mrv.addAttribute(entry.getValue().getSlugTxt(), StringUtil.checkVal(entry.getValue().getResponseText()).trim());
				iter.remove();
			}
		}
	}

	/**
	 * Flush Existing Master Record Attributes from db.
	 * @param mrv
	 */
	private void flushAttrs(SRTMasterRecordVO mrv) {
		try(PreparedStatement ps = dbConn.prepareStatement(buildAttrFlushQuery())) {
			ps.setString(1, mrv.getMasterRecordId());
			ps.executeUpdate();
		} catch (SQLException e) {
			log.error("Error Processing Code", e);
		}
	}

	/**
	 * Build Master Record Attr Deletion Query.
	 * @return
	 */
	private String buildAttrFlushQuery() {
		StringBuilder sql = new StringBuilder(150);
		sql.append(DBUtil.DELETE_CLAUSE).append(DBUtil.FROM_CLAUSE);
		sql.append((String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("dpy_syn_srt_mr_attr_xr where master_record_id = ?");
		return sql.toString();
	}

	/**
	 * Insert new Master Record Attributes into db.
	 * @param mrv
	 */
	private void saveAttrs(SRTMasterRecordVO mrv) {
		UUIDGenerator uuid = new UUIDGenerator();
		try(PreparedStatement ps = dbConn.prepareStatement(buildAttrInsertQuery())) {
			for(Entry<String, String> a: mrv.getAttributes().entrySet()) {
				ps.setString(1, uuid.getUUID());
				ps.setString(2, a.getKey());
				ps.setString(3, a.getValue());
				ps.setString(4, mrv.getMasterRecordId());
				ps.setTimestamp(5, Convert.getCurrentTimestamp());
				ps.addBatch();
			}

			ps.executeBatch();
		} catch (SQLException e) {
			log.error("Error Inserting new Attribute Records", e);
		}
	}

	/**
	 * Build Master Record Attr Insert Query.
	 * @return
	 */
	private String buildAttrInsertQuery() {
		StringBuilder sql = new StringBuilder(250);
		sql.append(DBUtil.INSERT_CLAUSE);
		sql.append((String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_SRT_MR_ATTR_XR (ATTR_XR_ID, ATTR_ID, VALUE_TXT, ");
		sql.append("MASTER_RECORD_ID, CREATE_DT) values (?,?,?,?,?);");
		return sql.toString();
	}

	/**
	 * Retrieve Attributes that a Master Record can have for this OP_CO.
	 * @return
	 */
	private List<String> loadAttributesList() {
		List<String> attrs = new ArrayList<>();
		try(PreparedStatement ps = dbConn.prepareStatement(buildMRAttrSql())) {
			ps.setString(1, SRTUtil.getOpCO(req));
			ResultSet rs = ps.executeQuery();

			while(rs.next()) {
				attrs.add(rs.getString("ATTR_ID"));
			}
		} catch (SQLException e) {
			log.error("Error retrieving Master Record Attributes ", e);
		}

		return attrs;
	}

	/**
	 * Build the MR_ATTR Retrieval Sql.
	 * @return
	 */
	private String buildMRAttrSql() {
		StringBuilder sql = new StringBuilder(150);
		sql.append("select ATTR_ID ");
		sql.append(DBUtil.FROM_CLAUSE);
		sql.append((String) attributes.get(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_SRT_MR_ATTR_OP_CO_XR ");
		sql.append(DBUtil.WHERE_CLAUSE).append(" OP_CO_ID = ? ");

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
				log.error("Error Processing Code", e);
			}
		}
	}
}