package com.depuysynthes.gfp;

import java.util.List;

/****************************************************************************
 * <b>Title</b>: GFPProgramVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Contains all information for a GFP program, including 
 * all associated workshops and resources
 * <b>Copyright:</b> Copyright (c) 2015
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Eric Damschroder
 * @version 1.0
 * @since July 6, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/

public class GFPProgramVO {

	private String programName;
	private List<GFPWorkshopVO> workshops;
	private List<GFPResourceVO> resources;
}
