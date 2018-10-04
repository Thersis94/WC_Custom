package com.wsla.action.admin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.report.GenericReport;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.WebCrescendoReport;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <p><b>Title:</b> BatchImport.java</p>
 * <p><b>Description:</b> Bulk POJO importer.  Permits data audit between ingest and commit.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2018, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Oct 3, 2018
 * <b>Changes:</b>
 ****************************************************************************/
public abstract class BatchImport extends SBActionAdapter {

	private static final String DEFAULT_FORMAT = "xls";

	protected BatchImport() {
		super();
	}

	protected BatchImport(ActionInitVO actionInit) {
		super(actionInit);
	}


	/**
	 * The POJO/bean representing the data being imported.  e.g. ProductSerialNumberVO.class 
	 * @return
	 */
	protected abstract Class<?> getBatchImportableClass();


	/**
	 * Generate an empty template file the user can populate their data into, then upload.
	 * Excel column headings come from the bean annotations.
	 * @see com.siliconmtn.annotations.Importable
	 * @param req for attaching the WC report
	 * @param fileName with or (preferably) without file extention.  e.g. "product-template"
	 * @throws ActionException
	 */
	public void getBatchTemplate(ActionRequest req, String fileName) 
			throws ActionException {
		log.info("generating batch template");
		AnnotationParser parser;
		try {
			parser = new AnnotationParser(getBatchImportableClass(), DEFAULT_FORMAT);
		} catch (InvalidDataException e) {
			throw new ActionException("could not initialize parser", e);
		}

		AbstractSBReportVO rpt = new WebCrescendoReport(new GenericReport());
		rpt.setFileName(fileName + (fileName.indexOf('.') == -1 ? "."+DEFAULT_FORMAT : ""));
		rpt.setData(parser.getTemplate());
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, Boolean.TRUE);
		req.setAttribute(Constants.BINARY_DOCUMENT, rpt);
	}


	/**
	 * Entry point for event entry import
	 * @param req
	 * @throws ActionException
	 */
	public void processImport(ActionRequest req) throws ActionException {
		log.info("processing batch upload");
		Class<?> impClass = getBatchImportableClass();
		List<FilePartDataBean> fileList = req.getFiles();
		Map<Class<?>, Collection<Object>> beans;
		int importCnt = 0;

		// 99% of the time this loop has one file in it, but from a code standpoint we support multiple
		for (FilePartDataBean fb : fileList ) {
			try {
				AnnotationParser parser = new AnnotationParser(impClass, fb.getExtension());
				beans = parser.parseFile(fb, true);
			} catch(InvalidDataException e) {
				throw new ActionException("could not parse import file", e);
			}
			ArrayList<Object> entries = (ArrayList<Object>) beans.get(impClass);
			log.debug(String.format("Parsed file %s.  Loaded %d %s beans into a collection", fb.getFileName(), 
					entries.size(), impClass.getSimpleName()));

			// Perform work on the collection before saving.
			// These throw ActionException, which will cause the import to hault/fail if the implementation so chooses.
			validateBatchImport(req, entries);
			transposeBatchImport(req, entries);

			// Save the beans through a DBProcessor batch query
			// If this step fails use one or both of the above methods to clean-up your data prior to saving.
			saveBatchImport(req, entries);
			
			importCnt += entries.size();
		}
		
		//set some response data
		putModuleData("success", importCnt, false);
	}


	/**
	 * Stub method for subclasses to validate foreign keys, manditory fields, etc.
	 * Any changes to the list must be done by reference.
	 * @param req downstream classes may transpose other reqParams into the beans.
	 * @param entries the beans extracted from the import file
	 * @throws ActionException
	 */
	protected void validateBatchImport(ActionRequest req, ArrayList<? extends Object> entries)
			throws ActionException {
		if (entries == null || entries.isEmpty())
			throw new ActionException("no data found in uploaded file");
	}


	/**
	 * Stub method for subclasses to remove duplicate records, align w/existing DB entries, etc.
	 * Any changes to the list must be done by reference.
	 * @param req downstream classes may transpose other reqParams into the beans.
	 * @param entries the beans which passed validation
	 * @throws ActionException
	 */
	protected void transposeBatchImport(ActionRequest req, ArrayList<? extends Object> entries)
			throws ActionException {
		//nothing done here by default - all records will get imported as-provided
	}


	/**
	 * Save the data to the database using DBProcessor and Column/Table annotations.
	 * @see com.siliconmtn.db.orm.Column
	 * @see com.siliconmtn.db.orm.Table
	 * @param req downstream classes may transpose other reqParams into the beans.
	 * @param entries the beans to save  to the DB
	 * @throws ActionException
	 */
	protected void saveBatchImport(ActionRequest req, ArrayList<? extends Object> entries)
			throws ActionException {
		log.debug(String.format("saving %d database records", entries.size()));
		DBProcessor db = new DBProcessor(dbConn, getCustomSchema());
		try {
			//DBProcessor already turns off & on AutoCommit as part of this transaction:
			db.executeBatch(entries);

		} catch (DatabaseException de) {
			throw new ActionException("Unable to save batch entries", de);
		}
	}
}