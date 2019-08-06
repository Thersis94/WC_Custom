package com.depuysynthes.ifu;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.HashMap;
import java.util.Map;

import com.siliconmtn.db.DBUtil;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.db.util.RecordDuplicator;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <p><b>Title:</b> MigrationTool.java</p>
 * <p><b>Description:</b> This class migrates the IFU database tables from the 
 * depuy instance to the sb instance.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jul 25, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class MigrationTool extends CommandLineUtil {

	private static final String RS_DEPUY_IFU_ID = "depuy_ifu_id";

	protected Connection destDbConn;
	private final String schema;
	private final String[] ifuTables;
	private Map<String, Object> replaceVals = new HashMap<>();

	public MigrationTool(String[] args) {
		super(args);
		loadProperties("scripts/ifu-migration.properties");
		loadDBConnection(props);
		openDestDbConn();
		schema = props.getProperty("customDbSchema");
		ifuTables = props.getProperty("ifuTables").split(",");
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		new MigrationTool(args).run();
	}


	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		dropDestTables();
		createDestTables();
		createDestIndexes();
		copyTableData();
		copySyncRecords();
		updateGroupIds();
		printReport();
		closeDBConnection();
	}


	/**
	 * Print some stats to confirm the data was fully/properly copied
	 */
	private void printReport() {
		for (String table : ifuTables) {
			String sql = StringUtil.join("select count(*) from ", schema, " ", table);

			try (PreparedStatement ps = dbConn.prepareStatement(sql);
					PreparedStatement ps2 = destDbConn.prepareStatement(sql)) {
				ResultSet srcRs = ps.executeQuery();
				ResultSet destRs = ps2.executeQuery();
				srcRs.next();
				destRs.next();
				log.info(String.format("%s row counts: %d = %d", table, srcRs.getInt(1), destRs.getInt(1)));
			} catch (Exception e) {
				log.error("could not count rows in table " + table, e);
			}
		}
	}


	/**
	 * drop the tables from the target DB
	 */
	private void dropDestTables() {
		for (String table : ifuTables) {
			String sql = StringUtil.join("drop table if exists ", schema, table, " cascade");
			log.debug(sql);
			try (PreparedStatement ps = destDbConn.prepareStatement(sql)) {
				ps.execute();
				log.info("dropped table " + table);

			} catch (Exception e) {
				log.error("could not drop table " + table, e);
			}
		}
	}


	/**
	 * create the tables from the target DB
	 */
	private void createDestTables() {
		for (String table : ifuTables) {
			String sql = props.getProperty(table + "_create");
			log.debug(sql);
			try (PreparedStatement ps = destDbConn.prepareStatement(sql)) {
				ps.execute();
				log.info("created table " + table);

			} catch (Exception e) {
				log.error("could not create table " + table, e);
			}
		}
	}


	/**
	 * create indexes on those tables defining one
	 */
	private void createDestIndexes() {
		for (String table : ifuTables) {
			String sql = props.getProperty(table + "_index");
			if (StringUtil.isEmpty(sql)) continue; //no index for this table
			log.debug(sql);
			try (PreparedStatement ps = destDbConn.prepareStatement(sql)) {
				ps.execute();
				log.info("created index on table " + table);

			} catch (Exception e) {
				log.error("could not create index on table " + table, e);
			}
		}
	}


	/**
	 * Copy the table data from the source to dest DB
	 */
	private void copyTableData() {
		RecordDuplicator rd;
		for (String table : ifuTables) {
			String pkIdCol = props.getProperty(table + "_pkId").toLowerCase();
			log.debug(table + " primary key=" + pkIdCol);
			try {
				rd = new RecordDuplicator(dbConn, destDbConn, table, pkIdCol, false);
				rd.setReplaceVals(replaceVals);
				rd.setSchemaNm(schema);
				rd.returnGeneratedKeys(false);
				replaceVals.put(pkIdCol, rd.copyRecords()); //pass the generated keys forward to the next
			} catch (Exception e) {
				log.error("could not copy table data for " + table, e);
			}
		}
	}


	@SuppressWarnings("unchecked")
	protected void updateGroupIds() {
		String sql = StringUtil.join("update ", schema, "depuy_ifu set depuy_ifu_group_id=? where depuy_ifu_group_id=?");
		log.debug(sql);
		try (PreparedStatement ps = destDbConn.prepareStatement(sql)) {
			Map<String, String> keys = (Map<String, String>)replaceVals.get(RS_DEPUY_IFU_ID);
			for (Map.Entry<String, String> entry : keys.entrySet()) {
				ps.setString(1, entry.getValue());
				ps.setString(2, entry.getKey());
				ps.addBatch();
			}
			int[] cnt = ps.executeBatch();
			log.info("updated " + cnt.length + " depuy_ifu rows to re-point ifu_group_ids");
		} catch (Exception sqle) {
			log.error("could not update ifu_group_ids", sqle);
		}
	}


	/**
	 * Copy the IFU sync records
	 */
	private void copySyncRecords() {
		//copy the IFU IDs into the two columns of wc_sync that could reference them
		replaceVals.put("wc_key_id", replaceVals.get(RS_DEPUY_IFU_ID));
		replaceVals.put("wc_orig_key_id", replaceVals.get(RS_DEPUY_IFU_ID));
		replaceVals.put("admin_profile_id", "c0a8023743efd4301176c200e31a1cfe"); //reset all submitters to Mike
		replaceVals.put("disposition_by_id", "c0a8023743efd4301176c200e31a1cfe"); //reset all dispositioned_by to Mike
		replaceVals.put("organization_id",  "DPY_SYN_EMEA");
		try {
			RecordDuplicator rd = new RecordDuplicator(dbConn, destDbConn, "wc_sync", "wc_sync_id", false);
			rd.setWhereSQL("module_type_id='DePuyIFU'");
			rd.setReplaceVals(replaceVals);
			rd.returnGeneratedKeys(false);
			rd.copyRecords();
		} catch (Exception e) {
			log.error("could not copy table data for wc_sync", e);
		}
	}


	/**
	 * Create the secondary dbConn.
	 * Also mark the default dbConn readOnly, so we don't cross streams inadvertently 
	 */
	private void openDestDbConn() {
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass((String) props.get("dest-dbDriver"));
		dbc.setUrl((String) props.get("dest-dbUrl"));
		dbc.setUserName((String) props.get("dest-dbUser"));
		dbc.setPassword((String) props.get("dest-dbPassword"));
		try {
			destDbConn = dbc.getConnection();

			//	mark the source dbConn readOnly, so we don't cross streams inadvertently
			dbConn.setReadOnly(true);
		} catch (Exception e) {
			log.error("could not connect dbConns", e);
		}
	}


	/**
	 * close both DB connections
	 */
	@Override
	protected void closeDBConnection() {
		super.closeDBConnection();
		DBUtil.close(destDbConn);
	}
}