package com.depuysynthes.nexus;

// JDK 1.8.x
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// SMT BAse Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.barcode.BarcodeItemVO;
import com.siliconmtn.barcode.BarcodeManager;
import com.siliconmtn.barcode.BarcodeOEM;
import com.siliconmtn.commerce.catalog.ProductVO;
import com.siliconmtn.http.SMTServletRequest;

import com.siliconmtn.util.StringUtil;
// WC Libs
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.action.search.SolrActionIndexVO;
import com.smt.sitebuilder.action.search.SolrActionVO;
import com.smt.sitebuilder.action.search.SolrFieldVO;
import com.smt.sitebuilder.action.search.SolrQueryProcessor;
import com.smt.sitebuilder.action.search.SolrFieldVO.BooleanType;
import com.smt.sitebuilder.action.search.SolrResponseVO;
import com.smt.sitebuilder.search.SearchDocumentHandler;

/****************************************************************************
 * <b>Title</b>: BarcodeLookupAction.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Manages the lookup of DePuy products based upon a provided
 * barcode.  Barcodes may be 1d or 2d and they are generally in a HIBC or GTIN Format
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since May 6, 2015<p/>
 * @updates:
 ****************************************************************************/
public class BarcodeLookupAction extends SBActionAdapter {
	
	/**
	 * Creates a list of GTIN and HIBC codes for the barcode lookup
	 */
	protected List<BarcodeOEM> oems = new ArrayList<BarcodeOEM>() {
		private static final long serialVersionUID = 1L; {
			add(new BarcodeOEM("codman", "Codman", "0886704", "H202"));
			add(new BarcodeOEM("mitek", "Mitek", "0886705", "H207"));
			add(new BarcodeOEM("depuy", "DePuy", "038135", "H441"));
			add(new BarcodeOEM("depuy_ireland", "DePuy Ireland", "038135", "H205"));
			add(new BarcodeOEM("depuy_france", "France Ireland", "038135", "E181"));
			add(new BarcodeOEM("depuy_intl", "DePuy International", "038135", "E085"));
			add(new BarcodeOEM("depuy_intl", "DePuy International", "038135", "E513"));
			add(new BarcodeOEM("depuy_cmw", "DePuy CMW", "038135", "E121"));
			add(new BarcodeOEM("joint_recon", "Joint Recon", "0603295", "H441"));
			add(new BarcodeOEM("depuy_spine", "Spine", "0705034", "H761"));
			add(new BarcodeOEM("synthes_us", "Synthes US", "0886982", "E085"));
			add(new BarcodeOEM("synthes_eu", "Synthes EU", "7612334", "E085"));
			add(new BarcodeOEM("synthes_eu", "Synthes EU", "7612335", "E085"));
			add(new BarcodeOEM("jnj_med", "JNJ Medical", "", "E555"));
			add(new BarcodeOEM("medos", "Medos International", "", "H200"));
		}
	};

	/**
	 * 
	 */
	public BarcodeLookupAction() {
		
	}

	/**
	 * @param actionInit
	 */
	public BarcodeLookupAction(ActionInitVO actionInit) {
		super(actionInit);
		
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(SMTServletRequest req) throws ActionException {
		// Get the barcode data
		Set<String> scans = new HashSet<String>();
		scans.add(req.getParameter("barcode"));
		scans.add(req.getParameter("barcode2"));
		
		ProductVO product = null;
		String errorMsg = null;
		try {
			// Parse the barcodes
			BarcodeManager bcm = new BarcodeManager(oems);
			BarcodeItemVO barcode = bcm.parseBarCode(scans);
			log.info("barcode: " + barcode);
			
			// Call the SOLR Query to populate
			if (barcode == null) throw new Exception("Invalid Barcode Recieved");
			
			product = this.retrieveProduct(barcode);
		} catch(Exception e) {
			errorMsg = e.getLocalizedMessage();
		}
		
		// Add the data to the collection for return to the view
		this.putModuleData(product, 1, false, errorMsg, errorMsg == null ? false: true);
	}
	
	/**
	 * Retrieves the product information for the provided barcode
	 * @param barcode
	 * @return
	 */
	protected ProductVO retrieveProduct(BarcodeItemVO barcode) {
		SolrQueryProcessor sqp = new SolrQueryProcessor(attributes, "DePuy_NeXus");
		SolrActionVO qData = new SolrActionVO();
		qData.setNumberResponses(1);
		qData.setStartLocation(0);
		qData.setOrganizationId("DPY_SYN_NEXUS");
		qData.setRoleLevel(0);
		qData.addIndexType(new SolrActionIndexVO("", NexusProductVO.solrIndex));
		log.debug(barcode.getProductId());
		SolrFieldVO field = new SolrFieldVO(SolrFieldVO.FieldType.FILTER, SearchDocumentHandler.DOCUMENT_ID, barcode.getProductId(), BooleanType.AND);
		qData.addSolrField(field);
		ProductVO prod = new ProductVO();
		SolrResponseVO resp = sqp.processQuery(qData);
		if (resp.getResultDocuments().size() == 0) {
			prod .setProductId("No Product Found with Supplied Barcode.");
			return prod;
		}
		prod.setProductId(StringUtil.checkVal(resp.getResultDocuments().get(0).get("documentId"), "No valid barcode found in system."));
		return prod;
	}

}
