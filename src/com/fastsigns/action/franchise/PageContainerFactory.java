package com.fastsigns.action.franchise;

import com.fastsigns.action.franchise.vo.pages.PageContainerVO;
import com.fastsigns.action.franchise.vo.pages.PageContainerVO_en_US;

public class PageContainerFactory {
	
	public static PageContainerVO getInstance(String locale, boolean isMobile) {
		PageContainerVO pcvo = null;
		if(locale == null || locale.equals("")) {
			locale = "en_US";
		}
		String classKey = "com.fastsigns.action.franchise.vo.pages.PageContainerVO_";
		if(isMobile)
			classKey +="m_";
		classKey += locale;
		try {			
			Thread t = Thread.currentThread();
			ClassLoader cl = t.getContextClassLoader();
			Class<?> load = cl.loadClass(classKey);
			pcvo = (PageContainerVO)load.newInstance();

		} catch (Exception e) {
            pcvo = new PageContainerVO_en_US();
        } 
		
		return pcvo;
	}}
