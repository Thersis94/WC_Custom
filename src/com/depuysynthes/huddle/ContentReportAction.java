package com.depuysynthes.huddle;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import com.depuysynthes.nexus.NexusCartExcelReport;
import com.depuysynthes.nexus.NexusCartPDFReport;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.Convert;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

public class ContentReportAction extends SBActionAdapter {
	
	public void build(SMTServletRequest req) throws ActionException {
		AbstractSBReportVO report;
		List<SolrDocument> docs = getSolrDocuments(req);
		report.setData(data);
		req.setAttribute(Constants.BINARY_DOCUMENT, report);
		req.setAttribute(Constants.BINARY_DOCUMENT_REDIR, true);
		
	}

	private List<SolrDocument> getSolrDocuments(SMTServletRequest req) {
		// TODO Auto-generated method stub
		return null;
	}
}
