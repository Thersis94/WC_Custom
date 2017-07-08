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

	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#load()
	 */
	@Override
	public RAMCaseVO load() {
		return (RAMCaseVO) req.getSession().getAttribute(RAMCaseManager.RAM_CASE_VO);
	}


	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#save()
	 */
	@Override
	public RAMCaseVO save() {
		RAMCaseVO cVo = (RAMCaseVO) attributes.get(RAMCaseManager.RAM_CASE_VO); 
		if(cVo == null) {
			req.getSession().setAttribute(RAMCaseManager.RAM_CASE_VO, cVo);
		}
		return cVo;
	}


	/* (non-Javadoc)
	 * @see com.ram.persistance.PersistanceIntfc#flush()
	 */
	@Override
	public void flush() {
		req.getSession().setAttribute(RAMCaseManager.RAM_CASE_VO, null);
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