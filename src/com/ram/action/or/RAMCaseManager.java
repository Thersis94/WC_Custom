package com.ram.action.or;

import java.sql.Connection;
import java.util.Map;

import org.apache.log4j.Logger;

import com.ram.action.or.vo.RAMCaseItemVO;
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.persistance.AbstractPersist;
import com.ram.persistance.RAMCasePersistanceFactory;
import com.ram.persistance.RAMCasePersistanceFactory.PersistanceType;

/****************************************************************************
 * <b>Title:</b> RAMCaseManager.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Action manages Case Interactions.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 28, 2017
 ****************************************************************************/
public class RAMCaseManager {

	public static final String RAM_CASE_ID = "ramCaseId";
	public static final String RAM_CASE_VO = "ramCaseVO";

	private Logger log;
	private Map<String, Object> attributes;

	private Connection conn;
	public RAMCaseManager() {
		log = Logger.getLogger(getClass());
	}

	public RAMCaseManager(Map<String, Object> attributes, Connection conn) {
		this();
		this.attributes = attributes;
		setConnection(conn);
	}

	/**
	 * @param conn
	 * @throws Exception 
	 */
	public void setConnection(Connection conn) {
		if(conn != null) {
			this.conn = conn;
		} else {
			log.error("Provided Connection is Null.");
		}
	}

	/**
	 * Helper method manages creating a new RAMCaseVO.
	 * @return
	 * @throws Exception 
	 */
	public RAMCaseVO createCase() throws Exception {
		return (RAMCaseVO) buildPI().initialize();
	}

	/**
	 * Helper method manages saving a RAMCaseVO.
	 * @throws Exception 
	 */
	public void saveCase(RAMCaseVO cVo) throws Exception {
		attributes.put(RAM_CASE_VO, cVo);
		buildPI().save();
	}

	/**
	 * Helper method mangages retrieving a RAMCaseVO.
	 * @throws Exception 
	 */
	public RAMCaseVO retreiveCase(String ramCaseId) throws Exception {
		attributes.put(RAM_CASE_ID, ramCaseId);
		return (RAMCaseVO) buildPI().load();
	}

	/**
	 * Helper method manages adding an item to a case
	 * @throws Exception 
	 */
	public void addCaseItem(RAMCaseVO cVo, RAMCaseItemVO item) throws Exception {
		cVo.addItem(item);
		saveCase(cVo);
	}

	/**
	 * Helper method manages removing an item from a CaseVO.
	 * @param cmv
	 * @param item
	 * @throws Exception 
	 */
	public void removeCaseItem(RAMCaseVO cmv, RAMCaseItemVO item) throws Exception {
		cmv.removeItem(item);
		saveCase(cmv);
	}

	/**
	 * Helper method manages updating a RAMCaseVO.
	 * @param cVo
	 * @throws Exception 
	 */
	public void updateCaseInfo(RAMCaseVO cVo) throws Exception {
		saveCase(cVo);
	}

	/**
	 * Helper method retrieves the AbstractPersistManager for persisting RAMCase
	 * Data.
	 * @return
	 * @throws Exception
	 */
	public AbstractPersist<?, ?> buildPI() throws Exception {
		AbstractPersist<?,?> pi = RAMCasePersistanceFactory.loadPersistanceObject(PersistanceType.DB, conn);
		pi.setAttributes(attributes);

		return pi;
	}
}