package com.wsla.util.migration;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;

import org.apache.log4j.Logger;

import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
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

	/*
	 * batch name is unique to each time we run the importer.  It helps us earmark the ones we've 
	 * added/changed during execution so we don't harm exisitng records.
	 * This column is meant to be purged from the DB when all is said and done (late 2019). 
	 */
	protected String batchNm;
	protected boolean purgeTablesFirst = false;
	protected boolean isOpenTktRun;
	protected boolean isClosedTktRun;
	protected String schema; //custom.
	protected DBProcessor db;
	protected UUIDGenerator uuid;
	private Set<String> ticketIds = new HashSet<>();

	AbsImporter() {
		super();
		log = Logger.getLogger(getClass());
		uuid = new UUIDGenerator();
	}

	/**
	 * Passed runtime environment from the CommandLineUtil controller
	 * @param conn
	 * @param props
	 * @param args
	 */
	@SuppressWarnings("unchecked")
	protected void setAttributes(Connection conn, Properties props, String[] args) {
		this.dbConn = conn;
		this.props = props;
		this.args = args;
		purgeTablesFirst = Convert.formatBoolean(props.getProperty("purgeTablesFirst", "false"));
		isOpenTktRun = Convert.formatBoolean(props.getProperty("isOpenTktRun", "false"));
		isClosedTktRun = Convert.formatBoolean(props.getProperty("isClosedTktRun", "true"));
		schema = props.getProperty("customDbSchema", "custom.");
		db = new DBProcessor(dbConn, schema);
		db.setGenerateExecutedSQL(log.isDebugEnabled());

		//see if we should limit qualifying tickets by ID
		StringTokenizer st = new StringTokenizer(StringUtil.checkVal(props.get("ticketIds")), ",");
		while (st.hasMoreTokens())
			ticketIds.add(st.nextToken().trim());
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
	 * provide all of the downstream importers a consistent way of knowing 
	 * which tickets to import, or which match rules to apply.
	 * @param ticketId
	 * @return
	 */
	protected boolean isImportable(String ticketId) {
		if (StringUtil.isEmpty(ticketId)) return false;

		//if we have ids, this one must be on the list
		if (!ticketIds.isEmpty())
			return ticketIds.contains(ticketId);

		//if we don't have IDs, this one must NOT match our exclusion regex
		return !ticketId.matches("(?i)^WSL0(.*)$");
	}


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
	 * overloaded to accept String filename
	 * @param path
	 * @param beanClass
	 * @param sheetNo
	 * @return
	 * @throws Exception
	 */
	protected <T> List<T> readFile(String path, Class<T> beanClass, int sheetNo) throws Exception {
		return readFile(new File(path), beanClass, sheetNo);
	}


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
	protected <T> List<T> readFile(File f, Class<T> beanClass, int sheetNo) throws Exception {
		//read the file into a FilePartDataBean
		if (!f.canRead()) throw new Exception("file not found: " + f.getAbsolutePath());
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
	 * Writes to the db in a batch.  Insert override is defaulted to true
	 * @param data
	 * @return
	 * @throws Exception
	 */
	protected int writeToDB(List<?> data) throws Exception {
		return writeToDB(data, true);
	}

	/**
	 * write the presumed-annotated list of beans to the database using DBProcessor
	 * @param data
	 * @param forceInsert
	 * @throws Exception 
	 */
	protected int writeToDB(List<?> data, boolean forceInsert) throws Exception {
		int[] cnt = db.executeBatch(data, forceInsert);
		log.debug(String.format("saved %d rows to the database", cnt.length));
		return cnt.length;
	}


	/**
	 * Turn the properties file into a Map
	 * @return
	 */
	protected Map<String, Object> getAttributes() {
		Map<String, Object> attrs = new HashMap<>(props.size());
		for (Map.Entry<Object, Object> entry : props.entrySet())
			attrs.put(entry.getKey().toString(), entry.getValue());

		return attrs;
	}


	/**
	 * Analyze the provided path.  If it's a file return it in a single array.  If its a folder
	 * find all files matching the pattern and return them all.
	 * @param property
	 * @param string
	 * @return
	 */
	public File[] listFilesMatching(String configPath, String pattern) {
		File passedFilePtr = new File(configPath);
		if (passedFilePtr.isDirectory()) {
			return passedFilePtr.listFiles((d, name) -> name.matches(pattern));
		} else {
			return new File[] { passedFilePtr };
		}
	}


	/**
	 * Put the thread to sleep.  We do this between writes and reads, to give 
	 * the DB time to commit.
	 * @param i
	 */
	protected void sleepThread(int durationMillis) {
		try {
			Thread.sleep(durationMillis);
		} catch (Exception e ) {
			log.fatal("could not sleep thread", e);
		}
	}
}