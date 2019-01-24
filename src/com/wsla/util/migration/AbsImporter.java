package com.wsla.util.migration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationExcelParser;

/****************************************************************************
 * <p><b>Title:</b> AbstractImporter.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jan 9, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public abstract class AbsImporter {

	protected static Logger log;
	protected Connection dbConn = null;
	protected Properties props = null;
	protected String[] args = null;

	protected static final int SHEET_1 = 0; //XLS sheet numbers - start at 0
	protected static final int SHEET_2 = 1;
	protected static final int SHEET_3 = 2;
	protected static final int SHEET_4 = 3;
	protected static final int SHEET_5 = 4;

	protected boolean purgeTablesFirst = false;
	protected String schema; //custom.

	AbsImporter() {
		super();
		log = Logger.getLogger(getClass());
	}

	/**
	 * Passed runtime environment from the CommandLineUtil controller
	 * @param conn
	 * @param props
	 * @param args
	 */
	final void setAttributes(Connection conn, Properties props, String[] args) {
		this.dbConn = conn;
		this.props = props;
		this.args = args;
		purgeTablesFirst = Convert.formatBoolean(props.getProperty("purgeTablesFirst", "false"));
		schema = props.getProperty("customDbSchema", "custom.");
	}


	/**
	 * delete from DB based on the given query - only runs if purgeTablesFirst=true
	 * @param sql
	 * @throws Exception
	 */
	final void delete(String sql) throws Exception {
		if (!purgeTablesFirst) return;

		log.debug(sql);
		try (PreparedStatement ps = dbConn.prepareStatement(sql)) {
			int cnt = ps.executeUpdate();
			log.info(String.format("query deleted %d rows", cnt));
		} catch (Exception e) {
			throw e;
		}
	}


	/**
	 * saves the records to the DB - designed to be implemented by a subclass and 
	 * could vary substantially from one to another
	 * @throws Exception
	 */
	abstract void save() throws Exception;


	/**
	 * Execution controller
	 * Often these steps:
	 * 1) Load your data via readFile
	 * 2) Mutate/transpose accordingly into WSLA/annotated VOs. (local methods)
	 * 3) Call delete w/query reflective of the data you're about to add (purge & replace to avoid duplicates)
	 *  4) Call save method
	 * @throws Exception
	 */
	abstract void run() throws Exception;




	/**
	 * Provide a list of VOs read from the requested Excel file.
	 * Read contents of <file> at <sheetNo> into a List<beanClass>.
	 * @param path
	 * @param beanClass
	 * @param sheetNo
	 * @return List
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	protected <T> List<T> readFile(String path, Class<T> beanClass, int sheetNo) throws Exception {
		//read the file into a FilePartDataBean
		File f = new File(path);
		if (!f.canRead()) throw new Exception("file not found: " + path);
		FilePartDataBean fpdb = new FilePartDataBean(f);

		Map<Class<?>, Collection<Object>> beans;
		try {
			//note we're the concrete class for Excel files, not the interface.  This is so we can specify sheetNo on some of the files
			//for the sake of legacy migration every file being imported is Excel.
			AnnotationExcelParser parser = new AnnotationExcelParser(Arrays.asList(beanClass));
			parser.setSheetNo(sheetNo);
			beans = parser.parseData(fpdb.getFileData(), true);

		} catch(InvalidDataException e) {
			throw new Exception("could not parse file", e);
		}
		ArrayList<Object> entries = (ArrayList<Object>) beans.get(beanClass);
		log.info(String.format("Parsed file %s.  Loaded %d %s beans into a collection", fpdb.getFileName(), 
				entries.size(), beanClass.getSimpleName()));

		return (List<T>) entries;
	}


	/**
	 * write the presumed-annotated list of beans to the database using DBProcessor
	 * @param products
	 * @throws Exception 
	 */
	protected void writeToDB(List<?> data) throws Exception {
		DBProcessor db = new DBProcessor(dbConn, schema);
		try {
			int[] cnt = db.executeBatch(data, true);
			log.info(String.format("saved %d rows to the database", cnt.length));
		} catch (Exception e) {
			throw e;
		}
	}
}