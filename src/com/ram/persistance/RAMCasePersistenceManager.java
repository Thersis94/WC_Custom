package com.ram.persistance;

import java.sql.Connection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ram.action.or.vo.RAMCaseVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

/****************************************************************************
 * <b>Title:</b> RAMCasePersistenceManager.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Class manages RAM Case Persistance on both Session and DB
 * Levels.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 28, 2017
 ****************************************************************************/
public class RAMCasePersistenceManager {

	public static final String CASE_DATA_KEY = "ramCaseData";
	//Pass Req/DB on attributes.
	private Map<String, Object> attributes;

	//Use Object listeners on the RAMCaseVO to listener for Field changes.
	private Connection conn;
	private Logger log;
	private DBProcessor dbp;

	//What is this?
	private boolean isNewCart;

	//Possibly pass case on constructor.
	public RAMCasePersistenceManager() {
		log = Logger.getLogger(getClass());
	}

	public RAMCasePersistenceManager(Map<String, Object> attributes, Connection conn) {
		this();
		this.attributes = attributes;
		setConnection(conn);
	}

	public RAMCaseVO load(ActionRequest req, String caseId) throws InvalidDataException, DatabaseException {
		RAMCaseVO cVo = null;
		if(req != null && req.getSession().getAttribute(CASE_DATA_KEY) != null) {
			cVo = (RAMCaseVO) req.getSession().getAttribute(CASE_DATA_KEY);
		} else {
			cVo = new RAMCaseVO();
			cVo.setCaseId(caseId);
			dbp.getByPrimaryKey(cVo);
		}

		return cVo;
	}

	/**
	 * Cleanup 
	 * @param req
	 * @param cVo
	 * @throws InvalidDataException
	 * @throws DatabaseException
	 */
	public void save(ActionRequest req, RAMCaseVO cVo) throws InvalidDataException, DatabaseException {
		//Base The Decision off RAMCaseVO Type.
//		if(useDb) {
//			dbp.save(cVo);
//		} else if(req != null) {
//			req.getSession().setAttribute(CASE_DATA_KEY, cVo);
//		}
	}

	public void flush(ActionRequest req) {
		if(req != null) {
			req.getSession().setAttribute(CASE_DATA_KEY, null);
		}
	}

	public void initialize() {
		
	}

	public void setAttributes(Map<String, Object> attributes) {
		this.attributes = attributes;
	}

	public Map<String, Object> getAttributes() {
		return attributes;
	}

	public void setConnection(Connection conn) {
		this.conn = conn;
		dbp = new DBProcessor(conn);
	}

	public Connection getConnection() {
		return conn;
	}
}