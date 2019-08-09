package com.wsla.util.migration;

import java.sql.Connection;
import java.util.List;

import com.siliconmtn.db.orm.ColumnData;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;

/****************************************************************************
 * <p><b>Title:</b> DBProcessorStub.java</p>
 * <p><b>Description:</b> </p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Jun 3, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class DBProcessorStub extends DBProcessor {

	/**
	 * @param conn
	 * @param schema
	 */
	public DBProcessorStub(Connection conn, String schema) {
		super(conn, schema);
		setGenerateExecutedSQL(true);
		log.warn("THIS OBJECT IS NOT TO BE USED FOR ACTUAL WRITES - only for printing SQL !");
	}

	@Override
	protected int executeUpdateSql(String sql, List<ColumnData> vals, Object o)
			throws DatabaseException {
		return 0;
	}
}
