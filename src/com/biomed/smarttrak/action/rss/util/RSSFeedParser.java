package com.biomed.smarttrak.action.rss.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Characters;
import javax.xml.stream.events.XMLEvent;

import com.biomed.smarttrak.action.rss.vo.RSSArticleVO;
import com.siliconmtn.util.Convert;

/****************************************************************************
 * <b>Title:</b> FeedParser.java <b>Project:</b> SMTBaseLibs <b>Description:</b>
 * TODO <b>Copyright:</b> Copyright (c) 2017 <b>Company:</b> Silicon Mountain
 * Technologies
 * 
 * @author raptor
 * @version 3.0
 * @since May 19, 2017
 ****************************************************************************/
public class RSSFeedParser {
	static final String TITLE = "title";
	static final String DESCRIPTION = "description";
	static final String CHANNEL = "channel";
	static final String LINK = "link";
	static final String ITEM = "item";
	static final String PUB_DATE = "pubDate";
	static final String GUID = "guid";
	final URL url;


	public RSSFeedParser(String feedUrl) {
		try {
			this.url = new URL(feedUrl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}


	public List<RSSArticleVO> readFeed() {
		List<RSSArticleVO> articles = null;
		try {
			boolean isFeedHeader = true;
			// Set header values intial to the empty string
			String description = "";
			String title = "";
			String link = "";
			String pubdate = "";
			String guid = "";

			// First create a new XMLInputFactory
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// Setup a new eventReader
			InputStream in = read();
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			// read the XML document
			while (eventReader.hasNext()) {
				XMLEvent event = eventReader.nextEvent();
				if (event.isStartElement()) {
					String localPart = event.asStartElement().getName()
							.getLocalPart();
					switch (localPart) {
						case ITEM:
							if (isFeedHeader) {
								isFeedHeader = false;
								articles = new ArrayList<>();
							}
							event = eventReader.nextEvent();
							break;
						case TITLE:
							title = getCharacterData(event, eventReader);
							break;
						case DESCRIPTION:
							description = getCharacterData(event, eventReader);
							break;
						case LINK:
							link = getCharacterData(event, eventReader);
							break;
						case GUID:
							guid = getCharacterData(event, eventReader);
							break;
						case PUB_DATE:
							pubdate = getCharacterData(event, eventReader);
							break;
					}
				} else if (event.isEndElement() && event.asEndElement().getName().getLocalPart() == (ITEM)) {
					RSSArticleVO message = new RSSArticleVO();
					message.setArticleTxt(description);
					message.setArticleGuid(guid);
					message.setArticleUrl(link);
					message.setTitleTxt(title);
					message.setPublishDt(Convert.formatDate(pubdate));
					if(articles != null)
						articles.add(message);
					continue;
				}
			}
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
		return articles;
	}


	private String getCharacterData(XMLEvent event, XMLEventReader eventReader) throws XMLStreamException {
		String result = "";
		event = eventReader.nextEvent();
		if (event instanceof Characters) {
			result = event.asCharacters().getData();
		}
		return result;
	}


	private InputStream read() {
		try {
			return url.openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
