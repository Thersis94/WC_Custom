package com.depuysynthes.solr.data;

import java.util.Map.Entry;

import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.solr.FileSolrIndexer;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: PatientsCaregiversVO.java<p/>
 * <b>Description: Used to create solr documents for the Patients and Caregivers
 * pages in depuy synthes site</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @since Oct 3, 2016
 ****************************************************************************/

public class PatientsCaregiversVO extends SolrDocumentVO {
	private final String[] organizations = {"DPY_SYN"};
	
	public PatientsCaregiversVO() {
		this(FileSolrIndexer.INDEX_TYPE);
	}

	public PatientsCaregiversVO(String solrIndex) {
		super(solrIndex);
	}
	
	
	/**
	 * Build a proper solr document vo for the Patients and Carefivers pages
	 */
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		// If the Object is not of the correct type return
		if (!(o instanceof Entry<?, ?>)) return;
		Entry<String, byte[]> vo = (Entry<String, byte[]>)o;
		
		String fileName = StringUtil.checkVal(vo.getKey());
		super.setDocumentId("PATIENTS_" + fileName.replace(".html", ""));
		super.setFileName((String) vo.getKey());
		super.addSection("Other");
		super.setDocumentUrl("/patients/" + ((String)vo.getKey()).replace(".html", ""));
		super.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		for (String s : organizations) {
			super.addOrganization(s);
		}
		super.setTitle("Patients and Caregivers for " + StringUtil.capitalize(fileName.replace("-", " ").replace(".html", "")));
		super.addAttribute("pcg", "true");
	}

}
