package com.ram.persistance;

import java.sql.Connection;

import com.ram.action.or.RAMCaseManager;
import com.ram.action.or.vo.RAMCaseVO;
import com.siliconmtn.db.orm.DBProcessor;
import com.siliconmtn.db.util.DatabaseException;
import com.siliconmtn.exception.InvalidDataException;

/****************************************************************************
 * <b>Title:</b> RAMCaseDBPersist.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> RAM Case Persistance Manager that works with ActionRequest.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 3, 2017
 ****************************************************************************/
public class RAMCaseDBPersist extends AbstractPersist<Connection, RAMCaseVO> {

	private DBProcessor dbp;
	public RAMCaseDBPersist() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#load()
	 */
	@Override
	public RAMCaseVO load() {
		String caseId = (String) attributes.get(RAMCaseManager.RAM_CASE_ID);
		RAMCaseVO cVo = initialize();
		cVo.setCaseId(caseId);
		try {
			dbp.getByPrimaryKey(cVo);
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Retrieving CaseVO for: " + caseId, e);
		}

		return cVo;
	}


	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#save()
	 */
	@Override
	public RAMCaseVO save() {
		RAMCaseVO cVo = (RAMCaseVO)attributes.get(RAMCaseManager.RAM_CASE_VO);
		try {
			dbp.save(cVo);
			if(dbp.getGeneratedPKId() != null) {
				cVo.setCaseId(dbp.getGeneratedPKId());
			}
		} catch (InvalidDataException | DatabaseException e) {
			log.error("Error Processing Code", e);
		}
		return cVo;
	}


	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#flush()
	 */
	@Override
	public void flush() {
		//Not necessary for DB.
	}


	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#initialize()
	 */
	@Override
	public RAMCaseVO initialize() {
		return new RAMCaseVO();
	}

	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#setPersistanceSource(java.lang.Object)
	 */
	@Override
	public void setPersistanceSource(Connection source) {
		this.dbp = new DBProcessor(source);
	}
}