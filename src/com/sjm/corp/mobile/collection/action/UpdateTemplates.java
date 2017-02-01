package com.sjm.corp.mobile.collection.action;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.sjm.corp.mobile.collection.MobileCollectionVO;

/****************************************************************************
 * <b>Title</b>: UpdateTemplates.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Updates the TemplateVO for the SJM Mobile Collection app
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class UpdateTemplates extends CollectionAbstractAction{

	/*
	 * (non-Javadoc)
	 * @see com.sjm.corp.mobile.collection.action.CollectionAbstractAction#update(com.siliconmtn.http.SMTServletRequest, com.sjm.corp.mobile.collection.MobileCollectionVO)
	 */
	public void update(ActionRequest req, MobileCollectionVO vo) {
		List<String> temp = new ArrayList<String>();
		log.debug("Size of themes: " + vo.getTemplates().getThemes().size());
		
		if((parseInt(req.getParameter("themeOnePref")) < 1 || parseInt(req.getParameter("themeTwoPref")) < 1 || parseInt(req.getParameter("themeThreePref")) < 1)){
			req.setParameter("error", "Please input an order of preference");
			req.setParameter("pageNumber", Integer.toString(8));
			return;
		}
		
		if(!checkIfDistinct(req.getParameter("themeOnePref"),req.getParameter("themeTwoPref"),req.getParameter("themeThreePref"))){
			req.setParameter("error", "Please choose different values for your order of preference");
			req.setParameter("pageNumber", Integer.toString(8));
			return;
		}
		//fully initiallize the list to the size that we need.
		for(int i = 0; i < vo.getTemplates().getThemes().size(); i++)
			temp.add(" ");
		
		temp.set(parseInt(req.getParameter("themeOnePref"))-1, vo.getTemplates().getThemes().get(0));
		temp.set(parseInt(req.getParameter("themeTwoPref"))-1, vo.getTemplates().getThemes().get(1));
		temp.set(parseInt(req.getParameter("themeThreePref"))-1, vo.getTemplates().getThemes().get(2));
		vo.getTemplates().setThemes(temp);		
	}
}
