package com.wsla.util;

// JDK 1.8
import java.sql.Connection;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Email Reply Parser 1.1
import com.edlio.emailreplyparser.EmailReplyParser;

// SMTBaseLibs
import com.siliconmtn.db.pool.SMTDBConnection;
import com.siliconmtn.io.mail.EmailMessageVO;
import com.siliconmtn.io.mail.pop3.EmailHandlerInterface;
import com.siliconmtn.util.StringUtil;

// WebCrescendo 3.0
import com.smt.sitebuilder.common.constants.Constants;

// WC_Custom
import com.wsla.action.ticket.transaction.TicketCommentTransaction;
import com.wsla.common.WSLAConstants;
import com.wsla.data.ticket.TicketCommentVO;

/********************************************************************
 * <b>Title:</b> CommentEmailHandler.java<br/>
 * <b>Description:</b> Extracts ticket comments from email replies
 *  and adds them to the database.<br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Tim Johnson
 * @version 3.0
 * @since Oct 17, 2018
 *******************************************************************/
public class CommentEmailHandler implements EmailHandlerInterface {
	public CommentEmailHandler() {
		super();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.io.mail.pop3.EmailHandlerInterface#processEmails(java.util.List, java.sql.Connection, java.lang.String)
	 */
	@Override
	public void processEmails(List<EmailMessageVO> emails, Connection dbConn, String schema) throws Exception {
		// Set the required attributes since this is likely out-of-band
		Map<String, Object> attributes = new HashMap<>();
		attributes.put(Constants.CUSTOM_DB_SCHEMA, schema + '.');
		
		// Setup the action for saving the comments
		TicketCommentTransaction tct = new TicketCommentTransaction();
		tct.setDBConnection(new SMTDBConnection(dbConn));
		tct.setAttributes(attributes);
		
		// Parse the comments from the emails and save them
		for (EmailMessageVO email : emails) {
			TicketCommentVO comment = new TicketCommentVO();
			parseReferenceData(comment, email.getHeaders().get("References"));
			
			// No reference headers were found that contained a ticketId
			if (StringUtil.isEmpty(comment.getTicketId())) continue;

			comment.setComment(EmailReplyParser.parseReply(email.getTextBody()));
			comment.setPriorityTicketFlag(0);
			comment.setEndUserFlag(1);
			comment.setActivityType(TicketCommentVO.ActivityType.COMMENT);
			
			tct.addTicketComment(comment, null);
		}
	}

	/**
	 * Parses reference data out of the email's "References" header,
	 * including the TicketId and UserId.
	 * 
	 * @param comment
	 * @param referenceHeader
	 */
	protected void parseReferenceData(TicketCommentVO comment, String referenceHeader) {
		// No valid reference headers to look through
		if (StringUtil.isEmpty(referenceHeader)) return;
		
		// Loop through all "Reference" headers in the email to find the one we initially sent
		List<String> references = Arrays.asList(referenceHeader.split("\\s+"));
		for (String reference : references) {
			if (reference.contains(WSLAConstants.TICKET_EMAIL_REFERENCE_SUFFIX)) {
				comment.setTicketId(getValueFromReference(reference, '<', '|'));
				comment.setUserId(getValueFromReference(reference, '|', '@'));
				break;
			}
		}
	}
	
	/**
	 * Gets a value from the supplied reference, using the specified delimiters.
	 * 
	 * @param reference
	 * @param startDelim
	 * @param endDelim
	 * @return
	 */
	private String getValueFromReference(String reference, char startDelim, char endDelim) {
		int start = reference.indexOf(startDelim) + 1;
		int end = reference.indexOf(endDelim);
		
		return reference.substring(start, end);
	}
}
