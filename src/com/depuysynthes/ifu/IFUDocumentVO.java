package com.depuysynthes.ifu;

import java.util.List;

import com.siliconmtn.http.SMTServletRequest;

/****************************************************************************
 * <b>Title</b>: IFUDocumentInstanceVO.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Contains instance specific information for an IFU document.
 * This includes items such as the language, document alias, and any technique
 * guides pertaining to this instance of the document</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUDocumentVO {
	
	private List<TechniqueGuideVO> tgList;
	
	public IFUDocumentVO() {
		
	}

	public IFUDocumentVO(SMTServletRequest req) {
		
	}

}
