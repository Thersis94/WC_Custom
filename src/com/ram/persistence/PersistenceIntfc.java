package com.ram.persistence;

import com.ram.action.or.vo.RAMCaseVO;

/****************************************************************************
 * <b>Title:</b> PersistanceIntfc.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Interface for managing Data through a persistance layer. 
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jul 3, 2017
 ****************************************************************************/
public interface PersistenceIntfc<T extends Object, S extends Object> {

	public S load(String caseId);
	public S save(RAMCaseVO rCase);
	public void  flush(String caseId);
	public S initialize();
	/**
	 * @param source
	 */
	public void setPersistanceSource(T source);
}
