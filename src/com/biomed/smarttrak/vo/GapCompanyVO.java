/**
 *
 */
package com.biomed.smarttrak.vo;

import java.util.Map;

/****************************************************************************
 * <b>Title</b>: GapCompanyVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> VO Manages all the data for a Gap Company Record.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 1.0
 * @since Feb 6, 2017
 ****************************************************************************/
public class GapCompanyVO {

	Map<String, Map<String, RegulationVO>> regulations;
	String companyName;

}
