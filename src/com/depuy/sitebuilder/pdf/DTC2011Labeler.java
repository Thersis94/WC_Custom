package com.depuy.sitebuilder.pdf;

// JDK 1.6.x
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;

// iText 2.1.x
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfImportedPage;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

// Site Builder Imports
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.pdf.AbstractLabelPDF;
import com.smt.sitebuilder.action.pdf.PDFLabelException;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>:DePuyKneeLabeler.java<p/>
 * <b>Description: </b> Takes the DePuy knee PDF cover letter and adds 
 * the user name and address to the document
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2008<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James Camire
 * @version 2.0
 * @since Sep 29, 2008
 * <b>Changes: </b>
 ****************************************************************************/
public class DTC2011Labeler extends AbstractLabelPDF {

	private static final int PARAGRAPH_MARGIN = 15;
	private static final float PAGE_MARGIN_LEFT = 235;
	private static final float PAGE2_MARGIN_LEFT = 35;
	private static final float PAGE_MARGIN_TOP = 80;
	private static final float PAGE_MARGIN_BOTTOM = 0;
	private static final float PAGE_MARGIN_RIGHT = 40;
	
	public static final String DEPUY_FONT = "font/fruitiger/FrutigerLTStd-Light.otf";
	
	protected static final String KNEE_SEARCH = "5";
	protected static final String HIP_SEARCH = "4";
	private static Font baseFont = null;
	private static Font baseFontBold = null;
	private static Font largeTitleFont = null;
	private static Font medTitleFont = null;
	
	/**
	 * @throws IOException 
	 * @throws DocumentException 
	 * 
	 */
	public void setFonts(String path) {
		try {
			log.debug("Getting fonts: " + path + DEPUY_FONT);
			BaseFont bf = BaseFont.createFont(path + DEPUY_FONT, BaseFont.WINANSI, BaseFont.EMBEDDED);
			baseFont = new Font(bf);
			baseFont.setSize(11);
			
			// Bold fonts
			baseFontBold = new Font(baseFont);
			baseFontBold.setStyle(Font.BOLD);
			
			// large red fonts
			largeTitleFont = new Font(baseFont);
			largeTitleFont.setStyle(Font.BOLD);
			largeTitleFont.setColor(new Color(218,59, 65));
			largeTitleFont.setSize(16);
			largeTitleFont.setStyle(Font.ITALIC);
			
			// Medium Fonts
			medTitleFont = new Font(baseFont);
			medTitleFont.setStyle(Font.BOLD);
			medTitleFont.setSize(12);
			medTitleFont.setStyle(Font.ITALIC);
		} catch (Exception e) {
			log.error("Error setting up fonts", e);
		}
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.pdf.AbstractLabelPDF#labelPDFFile(java.util.Map, java.lang.String)
	 */
	@Override
	public byte[] labelPDFFile(Map<String, Object> params, String path)
	throws PDFLabelException {
		// Set the fonts
		this.setFonts(path);
		
		// Get the joint information
		String type = StringUtil.checkVal(params.get("JOINT"), "knee").toLowerCase();
		log.debug("Type: " + type);
		
		// Get the user information
		UserDataVO user = (UserDataVO) params.get(Constants.USER_DATA);
		if (user == null) user = new UserDataVO();
		
		// Retrieve the cover letter
		String file = this.getFileName(path, type);
		PdfReader reader = this.getReader(file);		
		
        // we retrieve the size of the first page
        Rectangle psize = reader.getPageSize(1);
        
        // step 1: creation of a document-object
        Document document = new Document(psize,0,PAGE_MARGIN_RIGHT,PAGE_MARGIN_TOP,PAGE_MARGIN_BOTTOM);
        
        // step 2: we create a writer that listens to the document
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = this.getWriter(document, baos);
		
        // step 3: we open the document
        document.open();

        try {
        	this.addUserLabel(document, user);
			this.addHeaderLabel(document, user);
			
			if ("hip".equalsIgnoreCase(type))
				this.addHipPage1(document);
			else
				this.addKneePage1(document);
		} catch (DocumentException e1) {
			log.debug("adding text labels");
		}
        
        // Import the pages for editing
        PdfImportedPage page1 = writer.getImportedPage(reader, 1);
        PdfImportedPage page2 = null;
        if (reader.getNumberOfPages() == 2) {
        	page2 = writer.getImportedPage(reader, 2);
        }
        
        // Add the first page to the new document
        PdfContentByte cb = writer.getDirectContent();
        cb.addTemplate(page1, 1f, 0, 0, 1f, 0, 0);
        cb.endText();
        
        // Add a new page to the document
        document.newPage();
        
        try {
        	if ("hip".equalsIgnoreCase(type))
        		this.addHipPage2(document);
        	else 
        		this.addKneePage2(document);
		} catch (DocumentException e1) {
			log.error("Unable to add text to document", e1);
		}
		
        // Add the 2nd page
        if (reader.getNumberOfPages() == 2)	cb.addTemplate(page2, 1f, 0, 0, 1f, 0, 0);
        
        // step 5: we close the document
        document.close();
        
		return baos.toByteArray();
	}
	
	/**
	 * 
	 * @param document
	 * @param baos
	 * @return
	 * @throws PDFLabelException
	 */
	protected PdfWriter getWriter(Document document, ByteArrayOutputStream baos) 
	throws PDFLabelException {
        PdfWriter writer = null;
		try {
			writer = PdfWriter.getInstance(document, baos);
		} catch (DocumentException e) {
			log.debug("Error", e);
			throw new PDFLabelException("Error getting instance", e);
		}
		
		return writer;
	}
	
	/**
	 * 
	 * @param file
	 * @return
	 * @throws PDFLabelException
	 */
	public PdfReader getReader(String file) throws PDFLabelException {
		PdfReader reader = null;
		
		try {
			reader = new PdfReader(file);
		} catch (IOException e) {
			log.debug("Error", e);
			throw new PDFLabelException("Error getting instance", e);
		}
		
		return reader;
	}
	
	/**
	 * 
	 * @param path
	 * @return
	 */
	protected String getFileName(String path, String type) {
		String joint = type.toLowerCase();
		String file = path + "oadtc_2011q2_" + joint + ".pdf";
		return file;
	}
	
	/**
	 * 
	 * @param doc
	 * @param left
	 * @throws DocumentException
	 */
	protected void addHeaderLabel(Document doc, UserDataVO user) throws DocumentException {
		StringBuffer address = new StringBuffer("Dear ");
		if (StringUtil.checkVal(user.getPrefixName()).length() > 0)
			address.append(user.getPrefixName()).append(" ");
		address.append(StringUtil.checkVal(user.getFirstName(), "Patient"));
		address.append(" ").append(StringUtil.checkVal(user.getLastName()));
		address.append(",");
		
		Paragraph par = new Paragraph(new Chunk(address.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(55);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
	}

	/**
	 * 
	 * @param doc
	 * @param user
	 * @throws DocumentException
	 */
	protected void addUserLabel(Document doc, UserDataVO user) throws DocumentException {
		StringBuffer address = new StringBuffer();
		
    	// Make sure we have user info before displaying the header
    	if (StringUtil.checkVal(user.getLastName()).length() > 0) {
    		if (StringUtil.checkVal(user.getPrefixName()).length() > 0)
				address.append(user.getPrefixName()).append(" ");
			address.append(StringUtil.checkVal(user.getFirstName())).append(" ");
			address.append(StringUtil.checkVal(user.getLastName()));
			address.append("\nDePuy-10/05-Patient\n");
			address.append(StringUtil.checkVal(user.getAddress())).append("\n");
			address.append(StringUtil.checkVal(user.getCity())).append(", ");
			address.append(StringUtil.checkVal(user.getState()));
			address.append("\n").append(StringUtil.checkVal(user.getZipCode()));
    	}
    	
		Paragraph par = new Paragraph(new Chunk(address.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
	}
	
	/**
	 * 
	 * @param doc
	 * @throws DocumentException
	 */
	protected void addHipPage1(Document doc) throws DocumentException {
		StringBuilder s = new StringBuilder();
		s.append("Rebecca works as an OR nurse, where shifts can be as long as ");
		s.append("ten or twelve hours on a regular basis. It's hard work, but ");
		s.append("Rebecca enjoys helping people. Her hips began to bother her ");
		s.append("in her forties, but for years, she didn't complain. When she ");
		s.append("finally had to take action because the pain was overwhelming, ");
		s.append("she consulted doctors and surgeons in the hospital, and decided ");
		s.append("on her first (and later her other) hip replacement from DePuy ");
		s.append("Orthopaedics. Now she's back to moving freely, without pain. ");
		s.append("Every day she tests her hips Ñ and her DePuy Orthopaedics hips ");
		s.append("are helping her stand up to the tests of her life.");
		
		Paragraph par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("You have taken the first step toward changing your life by learning ");
		s.append("more about total hip replacements from DePuy Orthopaedics, Inc. Now ");
		s.append("it's time to take the next step. Included in this package, you will ");
		s.append("find information about hip replacement surgery along with a list of ");
		s.append("surgeons in your area that use DePuy Orthopaedics' products. You will ");
		s.append("also find a discussion guide for your meeting with an orthopaedic surgeon.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Like Rebecca, you may find that hip replacement surgery is right for ");
		s.append("you. Ask your doctor or one of the orthopaedic specialists on the ");
		s.append("included doctor locator, about the possibility of hip replacement.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
	}
	
	/**
	 * 
	 * @param doc
	 * @throws DocumentException
	 */
	protected void addKneePage1(Document doc) throws DocumentException {
		StringBuilder s = new StringBuilder();
		s.append("Lesa owns a dairy farm, but she has also had two DePuy Orthopaedics ");
		s.append("knee replacements because of osteoarthritis. She knows what it's ");
		s.append("like to miss out on life because of joint pain. Working 15 hours a ");
		s.append("day on the farm, she didn't think she could afford to take time off ");
		s.append("to have her knees checked by her doctor. She waited until the pain ");
		s.append("was so bad that she had to take pain pills to get through the day. ");
		s.append("When she finally had her first (and later her other) knee replaced, ");
		s.append("she wished she hadn't waited so long. It allowed her to get back to ");
		s.append("running the farm Ñ something she loves. Every day she tests her ");
		s.append("knees Ñ and her DePuy Orthopaedics knees are helping her stand up to the tests of her life.");
		
		Paragraph par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("You have taken the first step toward changing your life by learning ");
		s.append("more about total knee replacements from DePuy Orthopaedics, Inc. Now ");
		s.append("it's time to take the next step. Included in this package, you will ");
		s.append("find information about knee replacement surgery along with a list of ");
		s.append("surgeons in your area that use DePuy Orthopaedics' products. You will ");
		s.append("also find a discussion guide for your meeting with an orthopaedic surgeon.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Like Lesa, you may find that knee replacement surgery is right for ");
		s.append("you. Ask your doctor or one of the orthopaedic specialists on the ");
		s.append("included doctor locator, about the possibility of knee replacement.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE_MARGIN_LEFT);
		doc.add(par);
	}
	
	/**
	 * 
	 * @param doc
	 * @throws DocumentException
	 */
	protected void addHipPage2(Document doc) throws DocumentException {
		StringBuilder s = new StringBuilder();
		s.append("Learn more about PINNACLE");
		Paragraph par = new Paragraph(new Chunk(s.toString(), largeTitleFont));
		
		Font cFont = new Font(largeTitleFont);
		cFont.setStyle(Font.NORMAL);
		cFont.setSize(8);
		Chunk c = new Chunk("¨ ", cFont);
		c.setTextRise(5);
		par.add(c);
		par.add(new Chunk("Hips", largeTitleFont));
		
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("When traditional treatment options are no longer effective, ");
		s.append("hip replacement surgery may be an appropriate solution. PINNACLE ");
		s.append("Hips have a heritage of 11 years. DePuy Orthopaedics is working ");
		s.append("hard to bring more patients and surgeons together to help patients ");
		s.append("regain their freedom to move.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Talk to an orthopaedic surgeon today about PINNACLE Hips");
		par = new Paragraph(new Chunk(s.toString(), largeTitleFont));
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("When it comes to choosing a hip replacement, an orthopaedic surgeon ");
		s.append("will look for the one that helps you get back to the activities you ");
		s.append("used to enjoy, but had to give up due to joint pain. PINNACLE Hips ");
		s.append("are available in a wide range of sizes and materials. Your surgeon ");
		s.append("will choose the best option for you based on your age, weight, ");
		s.append("activity level, and other specific needs.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Important safety information");
		par = new Paragraph(new Chunk(s.toString(), largeTitleFont));
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("The performance of a hip replacement depends on age, weight, ");
		s.append("activity level and other factors. There are potential risks and ");
		s.append("recovery takes time. People with conditions limiting rehabilitation ");
		s.append("should not have this surgery. Only an orthopaedic surgeon can ");
		s.append("tell if hip replacement is right for you.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Call and arrange an appointment with an orthopaedic surgeon ");
		s.append("and for other Real Life Tested stories, visit ");
		c = new Chunk(s.toString(), baseFont);
		par = new Paragraph(c);
		c = new Chunk("www.RealLifeTested.com", baseFontBold);
		par.add(c);
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		/*
		s = new StringBuilder();
		s.append("*SIGMA¨ Knees are an evolution of the P.F.C.¨ and P.F.C. SIGMA Knee systems");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(182);
		par.setSpacingAfter(0);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		*/
	}
	
	/**
	 * 
	 * @param doc
	 * @throws DocumentException
	 */
	protected void addKneePage2(Document doc) throws DocumentException {
		StringBuilder s = new StringBuilder();
		s.append("Learn more about SIGMA");
		Paragraph par = new Paragraph(new Chunk(s.toString(), largeTitleFont));
		
		Font cFont = new Font(largeTitleFont);
		cFont.setStyle(Font.NORMAL);
		cFont.setSize(8);
		Chunk c = new Chunk("¨ ", cFont);
		c.setTextRise(5);
		par.add(c);
		par.add(new Chunk("Knees", largeTitleFont));
		
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("When traditional treatment options are no longer effective, ");
		s.append("knee replacement surgery may be an appropriate solution. P.F.C."); //¨ SIGMA ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		
		c = new Chunk("¨ ", baseFont);
		c.setTextRise(5);
		par.add(c);
		
		s = new StringBuilder();
		s.append(" SIGMA Knee replacements: 25 years of heritage.  DePuy Orthopaedics ");
		s.append("is working hard to bring more patients and surgeons together to ");
		s.append("help patients regain their freedom to move.");
		par.add(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Talk to an orthopaedic surgeon today about SIGMA Knees");
		par = new Paragraph(new Chunk(s.toString(), largeTitleFont));
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("When it comes to choosing a knee replacement, an orthopaedic surgeon ");
		s.append("will look for the one that helps you get back to the activities you ");
		s.append("used to enjoy, but had to give up due to joint pain. SIGMA Knees ");
		s.append("are available in a wide range of sizes and shapes. Your surgeon ");
		s.append("will choose the best option for you based on your age, weight, ");
		s.append("activity level, and other specific needs.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Important safety information");
		par = new Paragraph(new Chunk(s.toString(), largeTitleFont));
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("The performance of a knee replacement depends on age, weight, ");
		s.append("activity level and other factors. There are potential risks and ");
		s.append("recovery takes time. People with conditions limiting rehabilitation ");
		s.append("should not have this surgery. Only an orthopaedic surgeon can ");
		s.append("tell if knee replacement is right for you.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Call and arrange an appointment with an orthopaedic surgeon ");
		s.append("and for other Real Life Tested stories, visit ");
		c = new Chunk(s.toString(), baseFont);
		par = new Paragraph(c);
		c = new Chunk("www.RealLifeTested.com", baseFontBold);
		par.add(c);
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_MARGIN);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		
		/*
		s = new StringBuilder();
		s.append("*SIGMA¨ Knees are an evolution of the P.F.C.¨ and P.F.C. SIGMA Knee systems");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(182);
		par.setSpacingAfter(0);
		par.setIndentationLeft(PAGE2_MARGIN_LEFT);
		doc.add(par);
		*/
	}
}
