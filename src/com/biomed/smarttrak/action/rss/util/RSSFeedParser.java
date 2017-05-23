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
 * <b>Title:</b> FeedParser.java
 * <b>Project:</b> SMTBaseLibs
 * <b>Description:</b> Class manages Parsing an RSS Xml Feed.
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain
 * Technologies
 * 
 * @author Billy Larsen
 * @version 3.0
 * @since May 19, 2017
 ****************************************************************************/
public class RSSFeedParser {
	private static final String TITLE = "title";
	private static final String DESCRIPTION = "description";
	private static final String LINK = "link";
	private static final String ITEM = "item";
	private static final String PUB_DATE = "pubDate";
	private static final String GUID = "guid";

	// Set header values intial to the empty string
	final URL url;


	public RSSFeedParser(String feedUrl) {
		try {
			this.url = new URL(feedUrl);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e);
		}
	}


	public List<RSSArticleVO> readFeed() {
		List<RSSArticleVO> articles = new ArrayList<>();
		try {

			// First create a new XMLInputFactory
			XMLInputFactory inputFactory = XMLInputFactory.newInstance();
			// Setup a new eventReader
			InputStream in = read();
			XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
			// read the XML document
			parseEvent(eventReader, articles);
		} catch (XMLStreamException e) {
			throw new RuntimeException(e);
		}
		return articles;
	}


	/**
	 * 
	 * @param event
	 * @throws XMLStreamException 
	 */
	private void parseEvent(XMLEventReader eventReader, List<RSSArticleVO> articles) throws XMLStreamException {
		RSSArticleVO message = new RSSArticleVO();
		while (eventReader.hasNext()) {
			XMLEvent event = eventReader.nextEvent();
			if (event.isStartElement()) {
				populateMessage(message, event, eventReader);
			} else if (event.isEndElement() && event.asEndElement().getName().getLocalPart() == (ITEM)) {
				articles.add(message);
				message = new RSSArticleVO();
			}
		}
	}

	/**
	 * Populates the given RSSArticleVO with data from the RSS Feed.
	 * @param message
	 * @param event
	 * @param eventReader
	 * @throws XMLStreamException 
	 */
	private void populateMessage(RSSArticleVO message, XMLEvent event, XMLEventReader eventReader) throws XMLStreamException {
		switch (event.asStartElement().getName().getLocalPart()) {
			case TITLE:
				message.setTitleTxt(getCharacterData(event, eventReader));
				break;
			case DESCRIPTION:
				message.setArticleTxt(getCharacterData(event, eventReader));
				break;
			case LINK:
				message.setArticleUrl(getCharacterData(event, eventReader));
				break;
			case GUID:
				message.setArticleGuid(getCharacterData(event, eventReader));
				break;
			case PUB_DATE:
				message.setPublishDt(Convert.formatDate(getCharacterData(event, eventReader)));
				break;
			case ITEM:
			default:
				break;
		}
	}


	/**
	 * Method extracs Character Data from an XML Field.
	 * @param event
	 * @param eventReader
	 * @return
	 * @throws XMLStreamException
	 */
	private String getCharacterData(XMLEvent event, XMLEventReader eventReader) throws XMLStreamException {
		String result = "";
		event = eventReader.nextEvent();
		if (event instanceof Characters) {
			result = event.asCharacters().getData();
		}
		return result;
	}


	/**
	 * Method opens an InputStream for the url.
	 * @return
	 */
	private InputStream read() {
		try {
			return url.openStream();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
