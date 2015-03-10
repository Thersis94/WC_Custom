package com.depuysynthes.ifu;

import java.util.Map;

/****************************************************************************
 * <b>Title</b>: IFUContainer.java <p/>
 * <b>Project</b>: WebCrescendo <p/>
 * <b>Description: Top level container vo for the IFU documents.  This is designed
 * to hold all instances of the the document as well as related metadata that
 * is shared amongst all documents.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Eric Damschroder
 * @version 1.0
 * @since March 10, 2015<p/>
 * <b>Changes: </b>
 ****************************************************************************/

public class IFUContainer {
	
	private Map<String, IFUDocumentVO> ifuDocuments;
	
	public IFUContainer() {
		
	}

}
