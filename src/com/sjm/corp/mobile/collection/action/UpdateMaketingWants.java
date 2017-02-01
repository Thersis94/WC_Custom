package com.sjm.corp.mobile.collection.action;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.sjm.corp.mobile.collection.MobileCollectionVO;

/****************************************************************************
 * <b>Title</b>: UpdateMarketingWants.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Updates the MarketingWants for the SJM Mobile Collection app
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class UpdateMaketingWants extends CollectionAbstractAction {

	/*
	 * (non-Javadoc)
	 * @see com.sjm.corp.mobile.collection.action.CollectionAbstractAction#update(com.siliconmtn.http.SMTServletRequest, com.sjm.corp.mobile.collection.MobileCollectionVO)
	 */
	public void update(ActionRequest req, MobileCollectionVO vo) {
		vo.getMarketing().getWants().setWantAppointmentCards(Convert.formatBoolean(req.getParameter("appointmentCards")));
		vo.getMarketing().getWants().setWantBrochures(Convert.formatBoolean(req.getParameter("brochures")));
		vo.getMarketing().getWants().setWantBusinessCards(Convert.formatBoolean(req.getParameter("businessCards")));
		vo.getMarketing().getWants().setWantFaxReferrals(Convert.formatBoolean(req.getParameter("faxReferrals")));
		vo.getMarketing().getWants().setWantFolders(Convert.formatBoolean(req.getParameter("folders")));
		vo.getMarketing().getWants().setWantLetterhead(Convert.formatBoolean(req.getParameter("letterhead")));
		vo.getMarketing().getWants().setWantLogo(Convert.formatBoolean(req.getParameter("logos")));
		vo.getMarketing().getWants().setWantMagazineAds(Convert.formatBoolean(req.getParameter("magazineAds")));
		vo.getMarketing().getWants().setWantNewsletters(Convert.formatBoolean(req.getParameter("newsletters")));
		vo.getMarketing().getWants().setWantNewspaperAds(Convert.formatBoolean(req.getParameter("newspaperAds")));
		vo.getMarketing().getWants().setWantRolodex(Convert.formatBoolean(req.getParameter("rolodex")));
		vo.getMarketing().getWants().setWantSocialMedia(Convert.formatBoolean(req.getParameter("socialMedia")));
		vo.getMarketing().getWants().setWantWebsite(Convert.formatBoolean(req.getParameter("website")));
		vo.getMarketing().getWants().setWantPostcards(Convert.formatBoolean(req.getParameter("postcards")));
		vo.getMarketing().getWants().setWantEnvelopes(Convert.formatBoolean(req.getParameter("envelopes")));
	}
}
