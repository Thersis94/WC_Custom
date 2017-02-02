package com.sjm.corp.mobile.collection.action;


import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.sjm.corp.mobile.collection.MobileCollectionVO;

/****************************************************************************
 * <b>Title</b>: updateGoals.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Updates the MarketingUseVO for the SJM Mobile Collection app
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class UpdateMaketingUse extends CollectionAbstractAction {

	/*
	 * (non-Javadoc)
	 * @see com.sjm.corp.mobile.collection.action.CollectionAbstractAction#update(com.siliconmtn.http.SMTServletRequest, com.sjm.corp.mobile.collection.MobileCollectionVO)
	 */
	public void update(ActionRequest req, MobileCollectionVO vo) {
		vo.getMarketing().getUsing().setUsingAppointmentCards(Convert.formatBoolean(req.getParameter("appointmentCards")));
		vo.getMarketing().getUsing().setUsingBrochures(Convert.formatBoolean(req.getParameter("brochures")));
		vo.getMarketing().getUsing().setUsingBusinessCards(Convert.formatBoolean(req.getParameter("businessCards")));
		vo.getMarketing().getUsing().setUsingDirectMail(Convert.formatBoolean(req.getParameter("directMail")));
		vo.getMarketing().getUsing().setUsingFaxReferrals(Convert.formatBoolean(req.getParameter("faxReferrals")));
		vo.getMarketing().getUsing().setUsingFolders(Convert.formatBoolean(req.getParameter("folders")));
		vo.getMarketing().getUsing().setUsingLetterhead(Convert.formatBoolean(req.getParameter("letterhead")));
		vo.getMarketing().getUsing().setUsingLogos(Convert.formatBoolean(req.getParameter("logos")));
		vo.getMarketing().getUsing().setUsingMagazineAds(Convert.formatBoolean(req.getParameter("magazineAds")));
		vo.getMarketing().getUsing().setUsingNewsletters(Convert.formatBoolean(req.getParameter("newsletters")));
		vo.getMarketing().getUsing().setUsingNewspaperAds(Convert.formatBoolean(req.getParameter("newspaperAds")));
		vo.getMarketing().getUsing().setUsingRolodex(Convert.formatBoolean(req.getParameter("rolodex")));
		vo.getMarketing().getUsing().setUsingSocialMedia(Convert.formatBoolean(req.getParameter("socialMedia")));
		vo.getMarketing().getUsing().setUsingWebsites(Convert.formatBoolean(req.getParameter("websites")));
	}

}
