package com.biomed.smarttrak.action.rss.parser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.biomed.smarttrak.action.rss.vo.RSSArticleVO.ArticleSourceType;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> RSSArticleSaxHandler.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Standard RSS Sax Handler.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author raptor
 * @version 3.0
 * @since May 23, 2017
 ****************************************************************************/
public class RSSArticleSaxHandler extends DefaultHandler {

	public enum SearchType {TITLE("title"), DESCRIPTION("description"), LINK("link"), ITEM("item"),PUB_DATE("pubDate"), GUID("guid"), F_DATE("fDate"), CHANNEL("channel");
		private String qName;
		SearchType(String qName) {
			this.qName = qName;
		}

		public String getQName() {
			return qName;
		}
	}

	private List<RSSArticleVO> vos;
	private Map<SearchType, String> data;
	private SearchType type;

	public RSSArticleSaxHandler() {
		super();
		vos = new ArrayList<>();
		data = new EnumMap<>(SearchType.class);
	}

	/* (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String, java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		// Ensure that whenever an item element starts we clear out the data.
		// If the xml file was properly formatted any adding to the vo list will have been handled
		// by the endElement method. Improperly formatted data is discarded here.
		if(SearchType.ITEM.getQName().equals(qName)) data = new EnumMap<>(SearchType.class);
		super.startElement(uri, localName, qName, attributes);
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
		type = null;
		if(SearchType.ITEM.getQName().equals(qName)) {
			vos.add(buildRSSVO());
			data = new EnumMap<>(SearchType.class);
		} else if (SearchType.CHANNEL.getQName().equals(qName)) {
			data = new EnumMap<>(SearchType.class);
		}
	}

	/**
	 * @return
	 */
	protected RSSArticleVO buildRSSVO() {
		RSSArticleVO rss = new RSSArticleVO();
		String date = data.get(SearchType.PUB_DATE);
		Date d = Convert.formatDate(date);
		if(d == null) d = Calendar.getInstance().getTime();

		data.put(SearchType.F_DATE, Convert.formatDate(d, Convert.DATE_TIME_DASH_PATTERN_12HR));
		rss.setArticleSourceType(ArticleSourceType.RSS);
		rss.setArticleGuid(data.get(SearchType.GUID));
		rss.setArticleTxt(StringUtil.checkVal(data.get(SearchType.DESCRIPTION)).replace("\u00a0"," "));
		rss.setPublishDt(d);
		rss.setArticleUrl(data.get(SearchType.LINK));
		if(StringUtil.isEmpty(rss.getArticleGuid())) {
			rss.setArticleGuid(rss.getArticleUrl());
		}
		rss.setTitleTxt(StringUtil.checkVal(data.get(SearchType.TITLE)).replace("\u00a0"," "));
		rss.setPublicationName(data.get(SearchType.TITLE));
		rss.calcDataSize();
		return rss;
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
		data.put(type, buildValueString(type, val));
	}

	/**
	 * @param val
	 * @return
	 */
	private String buildValueString(SearchType t, String val) {
		StringBuilder articleTxt = new StringBuilder(2000);
		if(data.containsKey(t)) {
			articleTxt.append(" ").append(data.get(t));
		}
		articleTxt.append(val);

		return articleTxt.toString();
	}

	/**
	 * @return the vo
	 */
	public List<RSSArticleVO> getVos() {
		return vos;
	}
}