package com.biomed.smarttrak.action.rss.parser;

import java.util.LinkedList;
import java.util.Queue;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.biomed.smarttrak.action.rss.util.PubMedSearchResultVO;

/****************************************************************************
 * <b>Title</b>: PubmedListSaxHandler.java
 * <b>Project</b>: WC_Custom
 * <b>Description</b>: Sax Handler for processing Pubmed Article List.
 * <b>Copyright</b>: Copyright (c) 2017
 * <b>Company</b>: Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 1.0
 * @since Apr 22, 2017
 ****************************************************************************/
public class PubmedListSaxHandler extends DefaultHandler {

	public enum SearchType {
		E_SEARCH_RES("eSearchResult"), 
		COUNT("Count"), 
		RET_MAX("RetMax"), 
		RET_START("RetStart"), 
		QUERY_KEY("QueryKey"), 
		WEB_ENV("WebEnv"), 
		ID("Id");

		private String qName;
		SearchType(String qName) { this.qName = qName; }
		public String getQName() { return qName; }
	}

	private SearchType type;
	private PubMedSearchResultVO vo;
	private Queue<String> types;

	public PubmedListSaxHandler() {
		super();
		vo = new PubMedSearchResultVO();
		types = new LinkedList<>();
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);

		//Add Current Element to Queue.  Tracks depth of 
		types.add(qName);
		for(SearchType s : SearchType.values()) {
			if(s.getQName().equals(qName)) {
				type = s;
				break;
			}
		}
	}


	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String, java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		types.poll();
		type = null;
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);
		if(type == null) {
			return;
		}
		String val = new String(ch, start, length);
		switch(type) {
			case COUNT:
				if(types.size() == 2) {
					vo.setCount(Integer.parseInt(val));
				}
				break;
			case ID:
				vo.addId(val);
				break;
			case QUERY_KEY:
				vo.setQueryKey(Integer.parseInt(val));
				break;
			case RET_MAX:
				vo.setRetMax(Integer.parseInt(val));
				break;
			case RET_START:
				vo.setRetStart(Integer.parseInt(val));
				break;
			case WEB_ENV:
				vo.setWebEnv(val);
				break;
			case E_SEARCH_RES:
			default:
				break;
		}
	}

	/**
	 * @return the vo
	 */
	public PubMedSearchResultVO getVo() {
		return vo;
	}
}