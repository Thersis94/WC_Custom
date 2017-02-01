package com.sjm.corp.mobile.collection.action;

import java.util.ArrayList;
import java.util.List;

import com.siliconmtn.action.ActionRequest;
import com.sjm.corp.mobile.collection.MobileCollectionVO;

/****************************************************************************
 * <b>Title</b>: UpdateThemes.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: </b> Updates the ThemeVO for the SJM Mobile Collection app
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2012<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Josh Wretlind
 * @version 1.0
 * @since Jul 26, 2012<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class UpdateThemes extends CollectionAbstractAction{

	public UpdateThemes(){
		
	}
	/*
	 * (non-Javadoc)
	 * @see com.sjm.corp.mobile.collection.action.CollectionAbstractAction#update(com.siliconmtn.http.SMTServletRequest, com.sjm.corp.mobile.collection.MobileCollectionVO)
	 */
	public void update(ActionRequest req, MobileCollectionVO vo) {
		if(req.getParameter("remove") != null){
			req.setParameter("pageNumber", Integer.toString(8));

			int theme_to_remove = vo.getTemplates().getThemes().indexOf(vo.getThemes().getThemeId().get(parseInt(req.getParameter("theme_selected"))));
			if(theme_to_remove >= 0){
				vo.getTemplates().getThemes().remove(theme_to_remove);
				vo.getTemplates().getThumb().remove(theme_to_remove);
			}
		}
		if(vo.getTemplates().getThemes().size() >= 3){
			unblockNext(req); // we now have all the data to move on to the next page
			return;
		}
		else{
			blockNext(req); // if we allowed moving on at this point, we would throw index out of bounds errors when we go to the next page
		}
		//Adding in all the previously selected theme plus the theme the user
		//just selected into an Object Array(for the checkIfDistinct method)
		List<Object> temp = new ArrayList<Object>(vo.getTemplates().getThemes());
		temp.add(vo.getThemes().getThemeId().get(parseInt(req.getParameter("theme_selected"))));
		Object[] themeArr = temp.toArray();
		
		if(req.getParameter("sub") != null)
			req.setParameter("pageNumber", Integer.toString(8));
		if(req.getParameter("sub") != null && checkIfDistinct(themeArr)){
			vo.getTemplates().getThemes().add(vo.getThemes().getThemeId().get(parseInt(req.getParameter("theme_selected")))); // add the theme to the selected themes list
			vo.getTemplates().getThumb().add(vo.getThemes().getThumbLoc().get(parseInt(req.getParameter("theme_selected")))); //add the thumb location for the selected theme
			if(vo.getTemplates().getThemes().size() >= 3)
				unblockNext(req);
		}
	}
}
