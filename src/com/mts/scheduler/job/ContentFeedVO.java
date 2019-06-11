package com.mts.scheduler.job;

// JDK 1.8.x
import java.sql.ResultSet;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;

/****************************************************************************
 * <b>Title</b>: ContentFeedVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Document VO replacement for the XML feed. Supports a 
 * different set of fields than the MTSDocumentVO
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jun 11, 2019
 * @updates:
 ****************************************************************************/

public class ContentFeedVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2919886766209555392L;

	/**
	 * 
	 */
	public ContentFeedVO() {
		super();
	}

	/**
	 * @param req
	 */
	public ContentFeedVO(ActionRequest req) {
		super(req);
		
	}

	/**
	 * @param rs
	 */
	public ContentFeedVO(ResultSet rs) {
		super(rs);
	}

}

