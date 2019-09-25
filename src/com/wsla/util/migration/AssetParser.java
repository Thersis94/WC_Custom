package com.wsla.util.migration;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.google.api.client.util.Charsets;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.MapUtil;
import com.siliconmtn.util.StringUtil;
import com.wsla.data.ticket.ApprovalCode;
import com.wsla.data.ticket.TicketCommentVO;
import com.wsla.data.ticket.TicketDataVO;
import com.wsla.data.ticket.TicketLedgerVO;
import com.wsla.util.migration.vo.AssetPathVO;

/****************************************************************************
 * <p><b>Title:</b> AssetParser.java</p>
 * <p><b>Description:</b> Parses the HTML file containing the paths to the assets (files).
 * Updates the DB ticket_data table with found values.</p>
 * <p> 
 * <p>Copyright: Copyright (c) 2019, All Rights Reserved</p>
 * <p>Company: Silicon Mountain Technologies</p>
 * @author James McKain
 * @version 1.0
 * @since Sep 12, 2019
 * <b>Changes:</b>
 ****************************************************************************/
public class AssetParser extends AbsImporter {

	private static final Pattern skuLinePattern = Pattern.compile("(?i)^([A-Z0-9]+)\\ ([A-Z0-9]+)");
	private static final Pattern alertNoteLinePattern = Pattern.compile("(ALERT!!|NOTE:)\\s(.*)");
	private static final Pattern commentDatePattern = Pattern.compile("[0-9]+[A-Z]{3,}[0-9]+\\s+[0-9]+:[0-9]+");
	private static final Pattern fileLinePattern = Pattern.compile("(?i)^\\s+File Attached:\\s(.*)");

	private Map<String, String> ticketIds = new HashMap<>(30000, 1);

	private List<AssetPathVO> assets = new ArrayList<>(30000);
	private List<TicketLedgerVO> ledgers = new ArrayList<>(10000);
	private List<TicketDataVO> attributes = new ArrayList<>(10000);
	private List<TicketCommentVO> comments = new ArrayList<>(10000);

	public AssetParser() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#run()
	 */
	@Override
	void run() throws Exception {
		File[] files = listFilesMatching(props.getProperty("assetFile"), "(.*)NOTES(.*)");
		loadTicketIds();

		for (File f : files) {
			List<String> fileLines;
			try (Stream<String> stream = Files.lines(Paths.get(f.getAbsolutePath()), Charsets.ISO_8859_1)) {
				fileLines = stream.collect(Collectors.toList());
			}
			log.info(String.format("read %d lines from %s", fileLines.size(), f.getName()));

			parseLines(fileLines);
			sortData();
			save();

			//clear the lists before moving on to the next file
			assets.clear();
			ledgers.clear();
			attributes.clear();
			comments.clear();
		}
	}


	/**
	 * parse the lines of the raw file into something useful.
	 */
	private void parseLines(List<String> fileLines) {
		List<String> ticket= new ArrayList<>(50);

		for (String line : fileLines) {
			if (line.matches("\\s")) continue; //skip empty lines
			if (isSkuLine(line)) assessTicket(ticket);

			//bank this line and keep going.  We can't take action until we know what comes next.
			ticket.add(line);
		}
		assessTicket(ticket);
	}


	/**
	 * Decide if the given block of lines qualifies as a non-empty ticket.  Continue processing if so.
	 * Flush the buffer and return to reading the raw file if not.
	 * @param ticket
	 * @param b
	 * @param c
	 * @param d
	 */
	private void assessTicket(List<String> ticket) {
		if (ticket.size() > 1) //empty tickets have 1 line - ignore them
			parseTicket(ticket);

		//flush the ticket buffer
		ticket.clear();
	}


	/**
	 * Iterate the lines of the ticket and create List<VO> for the ticket.  Add the ticket
	 * to the Map<TicketID, List<VO>> when done.
	 * @param ticket
	 */
	private void parseTicket(List<String> ticket) {
		AssetPathVO vo = null;
		String ticketId = "";
		StringBuilder textBuffer = new StringBuilder(500);
		boolean isFile = false;

		for (String line : ticket) {
			log.debug("inspecting: " + line);

			if (isSkuLine(line)) { //this will always match the first line in the list, and never again
				ticketId = line.split("\\s+")[0].trim();

			} else if (isCommentStart(line)) {
				//if there's a prior VO, bank it before starting a new one
				bankVO(vo, textBuffer, isFile);

				vo = new AssetPathVO();
				vo.setTicketId(ticketId);
				vo.isAlert(line.matches("(?i).*ALERT.*"));
				vo.setCreateDate(Convert.formatDate("ddMMMyy HH:mm", getDate(line)));
				vo.setAttributeCode(getAttributeCode(line));

				//reset the buffer & file indicator
				textBuffer.setLength(0);
				textBuffer.ensureCapacity(500);
				isFile = false;

			} else if (isFileStart(line)) {
				//reset the buffer here - we don't care about any file comments (already loaded)
				textBuffer = new StringBuilder(getFilePath(line));
				isFile = true;

			} else if (!line.trim().isEmpty()) {
				//not the start of anything, just add it to the currently open item and keep going
				if (!isFile && textBuffer.length() > 0) textBuffer.append(' ');
				textBuffer.append(line.trim());
			}
		}
		//after the final line, bank the VO
		bankVO(vo, textBuffer, isFile);
	}


	/**
	 * look for keywords in the string and return the proper attributeCode
	 * These are unaccounted:
	 * 		attr_unitVideo
	 * @param line
	 * @return
	 */
	private String getAttributeCode(String line) {
		if (line.matches("(?i).*ADJUNTAR ([FOTOS|BER]).*")) {
			return "attr_unitImage";
		} else if (line.matches("(?i).*ADJUNTAR POP.*")) {
			return "attr_proofPurchase";
		} else if (line.matches("(?i).*ADJUNTAR AUT.*")) {
			return "attr_serialNumberImage";
		}
		//if it looks like we should have a match, report the concern
		if (line.matches("(?i).*ADJUNTAR.*"))
			log.warn("unmatched attribute: " + line);

		//some comments have files but aren't explicitly one of our attributes.  Default to photo.
		//this gets ignored later if there's no file
		return "attr_unitImage"; 
	}

	/**
	 * cleanup the VO and add it to the list of assets loaded from the file
	 * @param lineItems
	 * @param vo
	 * @param isFile
	 */
	private void bankVO(AssetPathVO vo, StringBuilder textBuffer, boolean isFile) {
		if (vo == null) {
			return;
		} else if (isFile) {
			vo.setFilePath(textBuffer.toString().trim());						
		} else {
			vo.setComment(textBuffer.toString().trim());						
		}
		log.debug(String.format("\r%s", vo.toString()));
		assets.add(vo);
	}


	/**
	 * Sort the assets into ticket attributes and comments
	 */
	private void sortData() {
		TicketDataVO attr;
		TicketCommentVO comment;
		TicketLedgerVO ledger;

		for (AssetPathVO vo : assets) {
			if (!ticketIds.containsKey(vo.getTicketId())) {
				//we found attributes for a ticket not in Cypher - typically this is ignorable but lets start with a warning
				log.warn(String.format("Found asset for non-existent ticket %s - ignoring asset", vo.getTicketId()));
				continue;
			}

			if (!StringUtil.isEmpty(vo.getFilePath())) {
				//its an attribute
				attr = new TicketDataVO();
				attr.setTicketId(vo.getTicketId());
				attr.setCreateDate(LegacyDataImporter.toUTCDate(vo.getCreateDate()));
				attr.setAttributeCode(vo.getAttributeCode());
				attr.setApprovalCode(ApprovalCode.APPROVED);
				attr.setValue(transposeFileUrl(vo.getFilePath()));
				attr.setMetaValue(getFileName(attr.getValue())); //uses url format
				attr.setLedgerEntryId(uuid.getUUID());
				attributes.add(attr);

				//we also need a ledger entry for these
				ledger = new TicketLedgerVO();
				ledger.setLedgerEntryId(attr.getLedgerEntryId());
				ledger.setTicketId(attr.getTicketId());
				ledger.setDispositionBy(SOHeader.LEGACY_USER_ID);
				ledger.setSummary("Usuario Agrega Evidencia");
				ledger.setCreateDate(attr.getCreateDate());
				ledgers.add(ledger);

			} else {
				//its a comment
				comment = new TicketCommentVO();
				comment.setTicketCommentId(uuid.getUUID());
				comment.setTicketId(vo.getTicketId());
				comment.setCreateDate(LegacyDataImporter.toUTCDate(vo.getCreateDate()));
				comment.setPriorityTicketFlag(vo.isAlert() ? 1 : 0);
				comment.setComment(vo.getComment());
				comment.setUserId(SOHeader.LEGACY_USER_ID);
				comments.add(comment);
			}
		}
		log.info(String.format("sorted data into %d comments and %d attributes", comments.size(), attributes.size()));
	}


	/**
	 * @param filePath
	 * @return
	 */
	private String getFileName(String filePath) {
		return filePath.substring(filePath.lastIndexOf('/')+1);
	}

	/**
	 * Turn the old E:\swattach\... disk paths into /binary URLs.  "/binary/file_transfer" is prepended in code.
	 * @param filePath
	 * @return
	 */
	private String transposeFileUrl(String filePath) {
		return filePath.replaceAll("\\\\", "/").replaceAll("E:/swattach/", "/wsla-swattach/").replace(' ','+');
	}


	/* (non-Javadoc)
	 * @see com.wsla.util.migration.AbsImporter#save()
	 */
	@Override
	void save() throws Exception {
		writeToDB(ledgers);
		writeToDB(attributes);
		writeToDB(comments);
	}

	/**
	 * @param line
	 * @return
	 */
	private boolean isSkuLine(String line) {
		return skuLinePattern.matcher(line).matches();
	}

	/**
	 * @param line
	 * @return
	 */
	private boolean isCommentStart(String line) {
		return !StringUtil.isEmpty(getComment(line));
	}

	/**
	 * @param line
	 * @return
	 */
	private String getComment(String line) {
		Matcher m = alertNoteLinePattern.matcher(line); 
		return m.find() ? m.group(2) : "";
	}

	/**
	 * returns ddMMMyy hh:mm
	 * @param line
	 * @return
	 */
	private String getDate(String line) {
		Matcher m = commentDatePattern.matcher(line); 
		return m.find() ? m.group(0) : "";
	}


	/**
	 * @param line
	 * @return
	 */
	private boolean isFileStart(String line) {
		return fileLinePattern.matcher(line).matches();
	}

	/**
	 * @param line
	 * @return
	 */
	private String getFilePath(String line) {
		Matcher m = fileLinePattern.matcher(line); 
		return m.find() ? m.group(1) : "";
	}


	/**
	 * load up the ticketIds from the database to cross-reference the soNumbers
	 */
	private void loadTicketIds() {
		String sql = StringUtil.join("select ticket_no as key, ticket_id as value from ", schema, "wsla_ticket");
		MapUtil.asMap(ticketIds, db.executeSelect(sql, null, new GenericVO()));
		log.info("loaded " + ticketIds.size() + " ticketIds from database");
	}
}