package com.ram.persistence;

import com.ram.action.or.RAMCaseManager;
import com.ram.action.or.vo.RAMCaseVO;
import com.siliconmtn.action.ActionRequest;

/****************************************************************************
 * <b>Title:</b> RAMCaseSessionPersist.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> RAM Case Persistance Manager that works with ActionRequest.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 *
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 3, 2017
 ****************************************************************************/
public class RAMCaseSessionPersist extends AbstractPersist<ActionRequest, RAMCaseVO> {

	private ActionRequest req = null;
	public static final String CASE_DATA_KEY = "ramCaseData";
	

	public RAMCaseSessionPersist() {
		super();
	}
	
	/**
	 * Creates the key for the session
	 * @param caseId
	 * @return
	 */
	private String key(String caseId) {
		return RAMCaseManager.STORAGE_SUFFIX + caseId;
	}
	
	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#load()
	 */
	@Override
	public RAMCaseVO load(String caseId) {
		return (RAMCaseVO)req.getSession().getAttribute(key(caseId));
	}


	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#save()
	 */
	@Override
	public RAMCaseVO save(RAMCaseVO cVo) {
		req.getSession().setAttribute(key(cVo.getCaseId()),  cVo);

		return cVo;
	}


	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#flush()
	 */
	@Override
	public void flush(String caseId) {
		req.getSession().removeAttribute(key(caseId));
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
	public void setPersistanceSource(ActionRequest source) {
		req = source;
	}
}