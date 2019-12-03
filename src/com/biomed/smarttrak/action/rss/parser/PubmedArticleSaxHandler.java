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
public class PubmedArticleSaxHandler extends DefaultHandler {
	public enum SearchType {
		PUB_DATE("PubDate"), PUB_TYPE("PublicationType"),
		PUBMED_ARTICLE("PubmedArticle"), ABSTRACT_TEXT("AbstractText"),
		PMID("PMID"), TITLE("Title"), ARTICLE_TITLE("ArticleTitle"),
		PUBLICATION_NAME("PublicationName"), YEAR("Year"), MONTH("Month"),
		DAY("Day"), F_DATE("formatDate");
		private String qName;

		SearchType(String qName) {
			this.qName = qName;
		}


		public String getQName() {
			return qName;
		}
	}

	private SearchType type;
	private boolean isDate = false;
	private Map<SearchType, String> data;
	private List<RSSArticleVO> vos;


	/**
	 * 
	 */
	public PubmedArticleSaxHandler() {
		super();
		vos = new ArrayList<>();
		data = new EnumMap<>(SearchType.class);
	}


	/*
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#startElement(java.lang.String,
	 * java.lang.String, java.lang.String, org.xml.sax.Attributes)
	 */
	@Override
	public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {
		super.startElement(uri, localName, qName, attributes);
		for (SearchType s : SearchType.values()) {
			if (s.getQName().equals(qName)) {
				type = s;
				break;
			}
		}

		if (SearchType.PUB_DATE.getQName().equals(qName)) {
			isDate = true;
		}
	}


	/*
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#endElement(java.lang.String,
	 * java.lang.String, java.lang.String)
	 */
	@Override
	public void endElement(String uri, String localName, String qName) throws SAXException {
		super.endElement(uri, localName, qName);
		type = null;
		if (SearchType.PUB_DATE.getQName().equals(qName)) {
			isDate = false;
		}
		if (SearchType.PUBMED_ARTICLE.getQName().equals(qName)) {
			vos.add(buildRSSVO());
			data = new EnumMap<>(SearchType.class);
		}
	}


	/**
	 * @return
	 */
	protected RSSArticleVO buildRSSVO() {
		RSSArticleVO rss = new RSSArticleVO();
		String date = data.get(SearchType.DAY) + "/" + data.get(SearchType.MONTH) + "/" + data.get(SearchType.YEAR);
		Date d = Convert.formatDate("dd/MMM/yyyy", date);
		if (d == null)
			d = Calendar.getInstance().getTime();

		data.put(SearchType.F_DATE, Convert.formatDate(d, Convert.DATE_TIME_DASH_PATTERN_12HR));
		rss.setPublicationName("PubMed");
		rss.setArticleSourceType(ArticleSourceType.PUBMED);
		rss.setArticleGuid(data.get(SearchType.PMID));
		rss.setArticleTxt(StringUtil.checkVal(data.get(SearchType.ABSTRACT_TEXT)).replace("\u00a0"," "));
		rss.setPublishDt(d);
		rss.setTitleTxt(StringUtil.checkVal(data.get(SearchType.ARTICLE_TITLE), data.get(SearchType.TITLE)).replace("\u00a0"," "));
		rss.setPublicationName(data.get(SearchType.TITLE));
		rss.setAttribute1Txt(data.get(SearchType.PUB_TYPE));
		rss.calcDataSize();
		return rss;
	}


	/*
	 * (non-Javadoc)
	 * @see org.xml.sax.helpers.DefaultHandler#characters(char[], int, int)
	 */
	@Override
	public void characters(char[] ch, int start, int length) throws SAXException {
		super.characters(ch, start, length);
		if (type == null) {
			return;
		}
		boolean buildValue = isBuildable();
		String val = new String(ch, start, length);
		if (isDate) {
			data.put(type, val);
		} else if (buildValue) {
			data.put(type, buildValueString(type, val));
		}
	}

	/**
	 * Return if the current Buffer of text should be built into the data map.
	 * @return
	 */
	private boolean isBuildable() {
		boolean isPMID = (!data.containsKey(SearchType.PMID) && SearchType.PMID.equals(type));
		boolean hasDate = !SearchType.DAY.equals(type) && !SearchType.MONTH.equals(type) && !SearchType.YEAR.equals(type) && !SearchType.PMID.equals(type);

		return isPMID || hasDate;
	}

	/**
	 * @param val
	 * @return
	 */
	private String buildValueString(SearchType t, String val) {
		StringBuilder articleTxt = new StringBuilder(2000);
		if (data.containsKey(t)) {
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