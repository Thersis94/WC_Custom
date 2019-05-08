package com.wsla.action.report;

// JDK 1.8.x
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.DBUtil;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.wsla.data.ticket.FailureReportVO;

/****************************************************************************
 * <b>Title</b>: FailureRateReport.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Pulls the data for the failure Reports
 * <b>Copyright:</b> Copyright (c) 2019
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Jan 31, 2019
 * @updates:
 ****************************************************************************/

public class RegionReport extends SBActionAdapter {
	/**
	 * Key to use for the report type
	 */
	public static final String AJAX_KEY = "region";
	
	/**
	 * 
	 */
	public RegionReport() {
		super();
	}

	/**
	 * @param actionInit
	 */
	public RegionReport(ActionInitVO actionInit) {
		super(actionInit);
	}

	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.action.ActionRequest)
	 */
	@Override
	public void retrieve(ActionRequest req) throws ActionException {
		log.info("region report running");
		if (! req.hasParameter("json")) return;
		
		Date startDate = req.getDateParameter("startDate");
		Date endDate = req.getDateParameter("endDate");
		String[] oemId = req.getParameterValues("oemId");
		oemId = oemId[0].split(",");
	}

}

