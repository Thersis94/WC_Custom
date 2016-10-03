package com.depuysynthes.solr.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.search.solr.CMSSolrIndexer;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: ASRRecallVO.java<p/>
 * <b>Description: Used to create solr documents for the ASR Recall  pages in 
 * depuy synthes site. Due to the larger number of pages and the lack of
 * coherent patterns in the naming structure of the files special rules are in 
 * place to prevent incorrect files from being put into solr</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @since Oct 3, 2016
 ****************************************************************************/

public class ASRRecallVO extends SolrDocumentVO {
	
	private final String[] organizations = {"DPY_SYN"};
	
	private Map<String, String> nameCases;
	private List<String> bypass;
	
	public ASRRecallVO() {
		this(CMSSolrIndexer.INDEX_TYPE);
	}

	public ASRRecallVO(String solrIndex) {
		super(solrIndex);
		buildNameCases();
		buildBypass();
	}
	
	
	/**
	 * Create the map of non-standard file names so that
	 * proper titles can be gotten for all documents
	 */
	private void buildNameCases() {
		nameCases = new HashMap<>();
		nameCases.put("asrcanadaenglish.html","ASR Recall for Canada - English");
		nameCases.put("canfr.html","ASR Recall for Canada - French");
		nameCases.put("depuy-asr-recall-usen.html","ASR Recall for United States");
		nameCases.put("aupatient.html","ASR Recall for Australia");
		nameCases.put("malen.html","ASR Recall for Malaysia");
		nameCases.put("nzpatient.html","ASR Recall for New Zealand");
		nameCases.put("southkorea.html","ASR Recall for South Korea");
		nameCases.put("asiapac.html","ASR Recall for Other Asian-Pacific Countries");
		nameCases.put("ukasrrecall.html","ASR Recall for United Kingdom");
		nameCases.put("euren.html","ASR Recall for Other European Countries");
		nameCases.put("mdeafr.html","ASR Recall for Other Countries");
	}
	
	/**
	 * Create a list of files that are present in the directory that
	 * do not need to show up in solr
	 */
	private void buildBypass() {
		bypass = new ArrayList<>();
		bypass.add("cropatient.html");
		bypass.add("koreaengpatient.html");
		bypass.add("russiapatient.html");
		bypass.add("croprofessional.html");
		bypass.add("koreaengpatientinprocess.html");
		bypass.add("russiaprofessional.html");
		bypass.add("czasr.html");
		bypass.add("koreaengprofessional.html");
		bypass.add("asr-hip-replacement-recall.html");
		bypass.add("czpatient.html");
		bypass.add("koreamedia.html");
		bypass.add("singpatient.html");
		bypass.add("ASR-language-updates-2015-11-02.tar");
		bypass.add("czprofessional.html");
		bypass.add("leaving.html");
		bypass.add("singprofessional.html");
		bypass.add("danishpatient.html");
		bypass.add("legal-notice.html");
		bypass.add("asrnorway.html");
		bypass.add("danishprofessional.html");
		bypass.add("malaysiapatient.html");
		bypass.add("asrpoland.html");
		bypass.add("denmark.html");
		bypass.add("malaysiaprofessional.html");
		bypass.add("swepatient.html");
		bypass.add("asrrecallsweden.html");
		bypass.add("sweprofessional.html");
		bypass.add("asrturkey.html");
		bypass.add("finnasrrecall.html");
		bypass.add("thaipatient.html");
		bypass.add("augerprofessional.html");
		bypass.add("finnpatient.html");
		bypass.add("netpatient.html");
		bypass.add("thaiprofessional.html");
		bypass.add("finnprofessional.html");
		bypass.add("netprofessional.html");
		bypass.add("turpatient.html");
		bypass.add("aupatient-preview.html");
		bypass.add("norpatient.html");
		bypass.add("turprofessional.html");
		bypass.add("austriagerpatient.html");
		bypass.add("frpatient.html");
		bypass.add("norprofessional.html");
		bypass.add("austria.html");
		bypass.add("frprofessional.html");
		bypass.add("ukmedia.html");
		bypass.add("nzpatient-preview.html");
		bypass.add("ukpatient.html");
		bypass.add("capatienteng.html");
		bypass.add("gerpatient.html");
		bypass.add("polpatient.html");
		bypass.add("ukprofessional.html");
		bypass.add("capatientfr.html");
		bypass.add("gerprofessional.html");
		bypass.add("polprofessional.html");
		bypass.add("usmedia.html");
		bypass.add("caprofessionaleng.html");
		bypass.add("index.html");
		bypass.add("index.html~");
		bypass.add("index~");
		bypass.add("asrcanadaenglish.html~");
		bypass.add("portpatient.html");
		bypass.add("uspatientsp.html");
		bypass.add("caprofessionalfr.html");
		bypass.add("index.html");
		bypass.add("portprofessional.html");
		bypass.add("usprofessional-depuy-hip-recall.html");
		bypass.add("index.html_ORIG");
		bypass.add("portugal.html");
		bypass.add("usprofessionalsp.html");
		bypass.add("chinapatient.html");
		bypass.add("privacy-policy.html");
		bypass.add("ussp.html");
		bypass.add("chinaprofessional.html");
		bypass.add("indiamedia.html");
		bypass.add("recall-legal-notice.html");
		bypass.add("countries_list.html");
		bypass.add("indiapatient.html");
		bypass.add("recall-privacy-policy.html");
		bypass.add("croatia.html");
		bypass.add("indiaprofessional.html");
		bypass.add("reimbursement.html");
	}
	
	/**
	 * Build a proper solr document vo for the ASR Recall pages
	 */
	@SuppressWarnings("unchecked")
	public void setData(Object o) {
		// If the Object is not of the correct type return
		if (!(o instanceof Entry<?, ?>)) return;
		Entry<String, byte[]> vo = (Entry<String, byte[]>)o;
		// If this is one of the non-live pages return
		if (bypass.contains(vo.getKey())) return;
		
		String fileName = StringUtil.checkVal(vo.getKey());
		super.setDocumentId("ASR_" + fileName.replace(".html", ""));
		super.setFileName((String) vo.getKey());
		super.addSection("Other");
		super.setDocumentUrl("/asrrecall/" + vo.getKey());
		super.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
		for (String s : organizations) {
			super.addOrganization(s);
		}
		if (nameCases.containsKey(vo.getKey())) {
			super.setTitle(nameCases.get(fileName));
		} else {
			super.setTitle("ASR Recall for " + StringUtil.capitalize(fileName.replace("asr", "").replace(".html", "")));
		}
		
	}
}
