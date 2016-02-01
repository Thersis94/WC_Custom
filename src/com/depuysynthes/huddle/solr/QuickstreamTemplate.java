package com.depuysynthes.huddle.solr;

import com.depuysynthes.huddle.HuddleUtils;
import com.siliconmtn.annotations.SolrField;
import com.siliconmtn.cms.TemplateFieldVO;
import com.siliconmtn.cms.TemplateFieldVOContainer;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.cms.CMSContentVO;

/****************************************************************************
 * <b>Title</b>: QuickstreamTemplate.java<p/>
 * <b>Description: For DS Huddle, which is similar to DSI's template with a few exceptions.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 18, 2016
 ****************************************************************************/
public class QuickstreamTemplate extends com.depuysynthesinst.QuickstreamTemplate {

	private String opco;
	
	public QuickstreamTemplate() {
		//use the default indexType for CMS, not DSI's
		super(HuddleUtils.IndexType.CMS_QUICKSTREAM.toString());
	}

	public void setData(Object o) {
		if (o == null) return;
		super.setData(o);
		CMSContentVO vo = (CMSContentVO) o;
		TemplateFieldVOContainer templateData = vo.getTemplateData();
		
		//some core fields are provided here-in:
		for (TemplateFieldVO field : templateData.getContainerData()) {
			if (field == null || field.getFieldName() == null) continue;

			switch (field.getFieldName()) {
				case "Specialty":
					//lowercase here correlates to the JSP we use in the View "external site.jsp"
					setOpco(StringUtil.checkVal(field.getFieldValue()).toLowerCase()); 
					break;
			}
		}
	}
	
	@Override
	public String getAssetUrl() {
		//if there is nothing in assetUrl, return documentUrl, which is the /docs/ path to the likely XLS file
		if (assetUrl == null || assetUrl.length() == 0) {
			return assetUrl;
		} else {
			return super.getDocumentUrl();
		}
	}

	@SolrField(name=HuddleUtils.SOLR_OPCO_FIELD)
	public String getOpco() {
		return opco;
	}

	public void setOpco(String opco) {
		this.opco = opco;
	}
}