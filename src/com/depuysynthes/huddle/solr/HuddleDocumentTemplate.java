package com.depuysynthes.huddle.solr;

import com.depuysynthes.huddle.HuddleUtils;
import com.depuysynthesinst.DSIDocumentTemplate;
import com.siliconmtn.annotations.SolrField;

/****************************************************************************
 * <b>Title</b>: HuddleDocumentTemplate.java<p/>
 * <b>Description: For DS Huddle, which is similar to DSI's template with a few exceptions.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 18, 2016
 ****************************************************************************/
public class HuddleDocumentTemplate extends DSIDocumentTemplate {

	private String opco;

	public HuddleDocumentTemplate() {
		super();
		attributeSetters.put("Specialty", "setOpco");
	}

	@SolrField(name=HuddleUtils.SOLR_OPCO_FIELD)
	public String getOpco() {
		return opco;
	}

	public void setOpco(String opco) {
		this.opco = opco;
	}
}