package com.sjm.corp.util;

// JDK 1.6.x
import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

// Log4J 1.2.15
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

// IText
import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.List;
import com.lowagie.text.ListItem;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;

// SMT Base Libs
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.db.DatabaseConnection;
import com.siliconmtn.exception.DatabaseException;
import com.siliconmtn.exception.FileException;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.io.FileManager;
import com.siliconmtn.io.FileWriterException;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;

// WC Libs
import com.smt.sitebuilder.action.dealer.DealerLocationVO;
import com.smt.sitebuilder.action.pdf.PDFLabelException;

/****************************************************************************
 * <b>Title</b>: IntroLetterManager.java <p/>
 * <b>Project</b>: SB_ANS_Medical <p/>
 * <b>Description: </b> Put comments here
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2011<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author james
 * @version 1.0
 * @since Apr 26, 2011<p/>
 * <b>Changes: </b>
 ****************************************************************************/
public class IntroLetterManager {
	public static final int PARAGRAPH_SPACING_LARGE = 40;
	public static final int PARAGRAPH_SPACING = 8;
	private static final float PAGE_MARGIN_LEFT = 63;
	private static final float PAGE_MARGIN_RIGHT = 40;
	private static final float PAGE_MARGIN_TOP = 95;
	private static final float PAGE_MARGIN_BOTTOM = 10;
	
	// font info
	private static Font baseFont = null;
	private static Font baseFontBlue = null;
	private static Font baseFontItalics = null;
	public static final String SJM_FONT = "/Library/Fonts/Microsoft/Arial.ttf";
	
	// Member Vars
	private static final Logger log = Logger.getLogger("IntroLetterManager");
	private Properties config = new Properties();
	private String filePath = null;
	private String imagePath = "/Users/james/Temp/SJMPrint/";
	private Map<String, GenericVO> countryVals = new HashMap<String, GenericVO>();
	private Map<Integer, GenericVO> signatures = new HashMap<Integer, GenericVO>();
	private Map<String, Float> spacing = new HashMap<String, Float>();
	private int counter = 1;
	
	/**
	 * @throws DocumentException 
	 * @throws FileException 
	 * 
	 */
	public IntroLetterManager() throws IOException, DocumentException, FileException {
		
		BaseFont bf = BaseFont.createFont(SJM_FONT, BaseFont.IDENTITY_H, true);
		baseFont = new Font(bf);
		baseFont.setSize(8.5f);
		
		baseFontBlue = new Font(baseFont);
		//baseFontBold.setStyle(Font.BOLD);
		baseFontBlue.setColor(Color.BLUE);
		baseFontBlue.setStyle(Font.UNDERLINE);
		
		baseFontItalics = new Font(baseFont);
		baseFontItalics.setStyle(Font.ITALIC);
		log.debug("Font Name: " + bf.getPostscriptFontName());
		
		// Load the config file
		config.load(new FileInputStream(new File("scripts/ans_config.properties")));
		
		// Assign the signatures
		this.assignSignatures();
		
		// Assign the page spacing
		this.assignSpacing();
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		BasicConfigurator.configure();
		log.debug("Starting File Creation: " + Convert.formatDate(new Date(), Convert.DATE_TIME_SLASH_PATTERN));
		IntroLetterManager ilm = new IntroLetterManager();
		
		// loop the types and set the data for each type
		for (int i = 0; i < 3; i++) {
			// Get the list of countries
			ilm.countryVals = new HashMap<String, GenericVO>();
			ilm.assignCountries(i);
			
			// Get the list of clinics
			ArrayList<DealerLocationVO> clinics = ilm.getDealerData();
			log.debug("Number of clinics: " + clinics.size());
			
			// Process the action
			ilm.processRequest(clinics);
		}
		
		log.debug("Completed File Creation: " + Convert.formatDate(new Date(), Convert.DATE_TIME_SLASH_PATTERN));
	}
	
	public void processRequest(ArrayList<DealerLocationVO> clinics ) 
	throws PDFLabelException, FileWriterException, InvalidDataException {
		
		// loop the clinics
		for (int i=0; i < clinics.size(); i++) {
			DealerLocationVO dlr = clinics.get(i);
			if (! countryVals.containsKey(dlr.getCountry())) continue;
			
			byte[] file = this.createPDF(1, dlr);
			FileManager fm = new FileManager();
			String fileName = dlr.getCountry() + "_" + counter + ".pdf";
			fm.writeFiles(file, filePath + dlr.getCountry() + "/", fileName, false, true);
		}
		
		log.debug("Number of PDFs Created: " + counter);
	}
	
	/**
	 * 
	 * @param type
	 * @param dlr
	 * @return
	 * @throws PDFLabelException
	 */
	public byte[] createPDF(int type, DealerLocationVO dlr) throws PDFLabelException {
        // step 1: creation of a document-object
        GenericVO gvo = countryVals.get(dlr.getCountry());
        String letter = (String)gvo.getKey();
		
        // Determine if the individual clinic receives a different language
        // Than the country default
        if (StringUtil.checkVal(dlr.getLocationDesc()).length() > 0) {
        	letter = dlr.getLocationDesc();
        }
        
		float topMargin = spacing.get(letter);
        Document document = new Document(PageSize.A4,PAGE_MARGIN_LEFT, PAGE_MARGIN_RIGHT, topMargin, PAGE_MARGIN_BOTTOM);

        // step 2: we create a writer that listens to the document
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter w = null;
		try {
			w = PdfWriter.getInstance(document, baos);
			
		} catch(Exception e) {
			throw new PDFLabelException("Unable to create new PDF File", e);
		}
		
        // Step 3: We open the document
        document.open();
        
        try {
	        if ("ENGLISH".equalsIgnoreCase(letter)) {
	        	this.addLabel(document, dlr, "en");
	        	this.english(document, dlr.getCountry());
	        } else if ("GERMAN".equalsIgnoreCase(letter)) {
	        	this.addLabel(document, dlr, "de");
	        	this.german(document, dlr.getCountry());
	        } else if ("FRENCH".equalsIgnoreCase(letter)) {
	        	this.addLabel(document, dlr, "fr");
	        	this.french(document, dlr.getCountry());
	        } else if ("SPANISH".equalsIgnoreCase(letter)) {
	        	this.addLabel(document, dlr, "es");
	        	this.spanish(document, dlr.getCountry());
	        } else if ("NORWEGIAN".equalsIgnoreCase(letter)) {
	        	this.addLabel(document, dlr, "no");
	        	this.norwegian(document, dlr.getCountry());
	        } else if ("DUTCH".equalsIgnoreCase(letter)) {
	        	this.addLabel(document, dlr, "nl");
	        	this.dutch(document, dlr.getCountry());
	        } else if ("ITALIAN".equalsIgnoreCase(letter)) {
	        	this.addLabel(document, dlr, "it");
	        	this.italian(document, dlr.getCountry());
	        } else if ("PORTUGUESE".equalsIgnoreCase(letter)) {
	        	this.addLabel(document, dlr, "pt");
	        	this.portugese(document, dlr.getCountry());
	        }
	        
	        counter++;
        } catch (Exception e) {
        	log.error("Unable to write into PDF", e);
        }
        
        if (w.getCurrentPageNumber() > 1)
        	log.debug("num Pages: " + dlr.getCountry() + "_" + counter + "|" + letter + "|"+ w.getCurrentPageNumber());
        
        // step 5: we close the document
        document.close(); 
        
        // Return the PDF File Data
		return baos.toByteArray();
        
	}
	
	/**
	 * adds the address label and the date
	 * @param doc
	 * @param dlr
	 * @throws DocumentException
	 */
	public void addLabel(Document doc, DealerLocationVO dlr, String lng) throws DocumentException {
		String txt = StringEscapeUtils.unescapeHtml(dlr.getLocationName()) + "\n";
		String addr = StringEscapeUtils.unescapeHtml(StringUtil.checkVal(dlr.getAddress()));
		String addr2 = StringEscapeUtils.unescapeHtml(StringUtil.checkVal(dlr.getAddress2()));
		String zip = StringEscapeUtils.unescapeHtml(StringUtil.checkVal(dlr.getZipCode()));
		String city = StringEscapeUtils.unescapeHtml(StringUtil.checkVal(dlr.getCity()));
		
		txt += this.getDivision(lng) + "\n";
		if (addr.length() > 0) txt += addr + "\n";
		if (addr2.length() > 0) txt += addr2 + "\n";
		if (StringUtil.checkVal(dlr.getState()).length() > 0) txt += dlr.getState() + "\n";
		if (zip.length() > 0) txt += zip + ", ";
		if (city.length() > 0) txt +=  city + "\n";
		
		Paragraph par = new Paragraph(new Chunk("A-Post"));
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
		par = new Paragraph(new Chunk(txt, baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		doc.add(par);
		
		// add the date
		String c = dlr.getCountry().toUpperCase();
		//DateFormat df = DateFormat.getDateInstance(DateFormat.LONG, new Locale(lng, c));
		Format formatter = new SimpleDateFormat("MMMM yyyy", new Locale(lng, c));
		//txt =  df.format(Convert.formatDate("8/26/2011"));
		txt = formatter.format(new Date());
		//par = new Paragraph(new Chunk(txt, baseFont));
		par = new Paragraph(new Chunk(StringUtil.capitalize(txt), baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		par.setIndentationLeft(351);
		par.setSpacingAfter(PARAGRAPH_SPACING + 40);
		doc.add(par);
	}
	
	/**
	 * 
	 * @param lng
	 * @return
	 */
	public String getDivision(String lng) {
		log.debug("************** Language: " + lng);
		String val = "Division";
		
		if ("fr".equalsIgnoreCase(lng)) val = "Cardiologie";
		else if ("de".equalsIgnoreCase(lng)) val = "Kardiologie";
		else if ("it".equalsIgnoreCase(lng)) val = "Cardiologia";
		
		return val;
	}

	/**
	 * Loads the list of dealers
	 * @return
	 */
	public ArrayList<DealerLocationVO> getDealerData() {
		StringBuilder c = new StringBuilder();
		Set<String> set = countryVals.keySet();
		int i = 0;
		for (Iterator<String> iter = set.iterator(); iter.hasNext(); i++) {
			if (i > 0) c.append(",");
			c.append("'").append(iter.next()).append("'");
		}
		
		String s = "select b.* from dealer a inner join dealer_location b ";
		s += "on a.dealer_id = b.dealer_id where organization_id = 'SJM_CORP_LOC' ";
		s += "and parent_id is null and country_cd in ('CH')";
		s += "order by country_cd, location_nm";
		log.debug("Dealer SQL: " + s);
		
		ArrayList<DealerLocationVO> data = new ArrayList<DealerLocationVO>();
		Connection conn = null;
		try {
			conn = this.getConnection();
			PreparedStatement ps = conn.prepareStatement(s);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				data.add(new DealerLocationVO(rs));
			}
		} catch(Exception e) {
			log.error("Unable to retrieve dealers", e);
		} finally {
			try {
				conn.close();
			} catch (Exception e) {}
		}
		
		return data;
	}
	
	/**
	 * 
	 * @return
	 * @throws DatabaseException
	 * @throws InvalidDataException
	 */
	public Connection getConnection() throws DatabaseException, InvalidDataException {
		DatabaseConnection dbc = new DatabaseConnection();
		dbc.setDriverClass(config.getProperty("dbDriver"));
		dbc.setUrl(config.getProperty("dbUrl"));
		dbc.setUserName(config.getProperty("dbUser"));
		dbc.setPassword(config.getProperty("dbPassword"));
		return dbc.getConnection();
	}
	
	
	public void spanish(Document doc, String country) throws DocumentException {
		Paragraph par = new Paragraph(new Chunk("Estimado Sr./Sra.:", baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		StringBuilder s = new StringBuilder();
		s.append("Como valioso cliente de St. Jude Medical, su clínica se ha incluido en nuestro ");
		s.append("localizador internacional de clínicas online (International Clinic Locator).  ");
		s.append("Se trata de un programa destinado a personas que han recibido tratamiento con ");
		s.append("productos de St. Jude Medical.  El localizador constituye un recurso práctico ");
		s.append("para pacientes que requieren un seguimiento de su marcapasos o desfibrilador ");
		s.append("cuando viajan a otros países.  No se trata de una iniciativa de marketing ni ");
		s.append("de promoción de su centro.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Rogamos que revise y actualice la información de contacto de su clínica para ");
		s.append("garantizar que el localizador refleja dicha información con la mayor precisión ");
		s.append("posible.  También podrá incluir información de contacto adicional, como el ");
		s.append("número de fax y la página web de la clínica.  Si su clínica no desea estar ");
		s.append("incluida en el programa, puede salir de él en cualquier momento.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("En este momento, el localizador internacional de clínicas contiene la siguiente información:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		List ol = new List(false, 15);
		ol.setIndentationLeft(25);
		ol.setListSymbol("•");
		ol.add(new ListItem(15, "Nombre de la clínica", baseFont));
		ol.add(new ListItem(15, "Dirección de la clínica (calle, ciudad, provincia/país, código postal)", baseFont));
		ol.add(new ListItem(15, "Número de teléfono de la clínica", baseFont));
		ol.add(new ListItem(15, "Indicación para asistencia de seguimiento de marcapasos/desfibriladores de St. Jude Medical", baseFont));
		doc.add(ol);
		
		par = new Paragraph(new Chunk("Para acceder a la información de su clínica, escriba la siguiente dirección en su explorador:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("www.sjmcliniclocator.com/clinic", baseFontBlue));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("En ella podrá buscar su clínica para revisar la información de contacto.   Una vez creada una ");
		s.append("cuenta de acceso, podrá actualizar la información de su clínica según sea necesario.  Le ");
		s.append("animamos a crear una cuenta de acceso para que podamos comunicarnos con su clínica sobre el programa. ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Tras la aprobación de un administrador de SJM, la información de contacto actualizada de su ");
		s.append("clínica estará disponible a través de la página de búsqueda para pacientes en ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.add(new Chunk("www.sjmcliniclocator.com", baseFontBlue));
		par.add(new Chunk(".", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Como participante del localizador internacional de clínicas de St. Jude Medical, acepta y acuerda ");
		s.append("que su clínica sea incluida en nuestra página web como herramienta para pacientes y que St. ");
		s.append("Jude Medical no utiliza, regula ni controla la atención prestada por su clínica al paciente, ");
		s.append("incluso en relación con los productos de St. Jude Medical, y que tampoco damos a entender ");
		s.append("que su centro tenga más capacidad que otras clínicas para administrar tratamiento a los pacientes.  ");
		s.append("St. Jude Medical no se hace responsable de los tratamientos administrados a los pacientes como ");
		s.append("consecuencia de haberse puesto en contacto con su clínica a través de nuestra página web, y su ");
		s.append("clínica indemnizará y eximirá a St. Jude Medical de cualquier responsabilidad derivada de las ");
		s.append("reclamaciones relacionadas con el tratamiento administrado a dichos pacientes. ");
		par = new Paragraph(new Chunk(s.toString(), baseFontItalics));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("No dude en enviarnos cualquier pregunta o comentario sobre el localizador internacional de clínicas a cliniclocator@sjm.com.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Atentamente:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		 
		this.displaySignature(doc, country, "SPANISH");
	}
	
	
	public void german(Document doc, String country) throws DocumentException {
		Paragraph par = new Paragraph(new Chunk("Sehr geehrte Damen und Herren,", baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		StringBuilder s = new StringBuilder();
		s.append("die Reiseaktivitäten von Patienten mit Schrittmachern und ICD nimmt stetig zu. ");
		s.append("Für die Reiseplanung benötigen diese Patienten Informationen, wo sie im Fall ");
		s.append("der Fälle eine Nachsorge ihrer St. Jude Medical Implantate durchführen lassen ");
		s.append("können. Häufig erhalten wir Anfragen bezüglich. Nachsorgezentren im In- und Ausland. ");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Als Serviceangebot an Ihre Patienten haben wir ein internationales Online-Klinikverzeichnis ");
		s.append("erstellt. Ihre Klinik/Praxis wurde als lokales Nachsorgezentrum in dieses Online-Klinikverzeichnis ");
		s.append("aufgenommen.  Dieses Verzeichnis ist als Informationsangebot für Patienten konzipiert und dient ");
		s.append("nicht als Marketing- oder Werbemaßnahme für uns oder die genannten Zentren.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Wir würden Sie bitten die Kontaktinformationen Ihrer Klinik/Praxis zu überprüfen und wenn ");
		s.append("nötig zu aktualisieren, damit Ihre Angaben im Verzeichnis korrekt sind. Weiterhin haben Sie ");
		s.append("dadurch auch die Möglichkeit, zusätzliche Informationen zu Ihrer Klinik/Praxis wie Faxnummer ");
		s.append("und Website hinzuzufügen. Wenn Sie nicht aufgenommen werden möchten, können Sie ");
		s.append("selbstverständlich einer Aufnahme in das Verzeichnis widersprechen. ");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Zur Zeit enthält das internationale Online-Klinikverzeichnis folgende Informationen:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		List ol = new List(false, 12);
		ol.setIndentationLeft(25);
		ol.setListSymbol("•");
		ol.add(new ListItem(12, "Name der Klinik", baseFont));
		ol.add(new ListItem(12, "Anschrift der Klinik (Straße, Postleitzahl, Ort, Land)", baseFont));
		ol.add(new ListItem(12, "Telefonnummer der Klinik", baseFont));
		ol.add(new ListItem(12, "Möglichkeiten der Nachsorge von St. Jude Medical Herzschrittmachern/Defibrillatoren", baseFont));
		doc.add(ol);
		
		par = new Paragraph(new Chunk("Sie können die Daten Ihrer Klinik aufrufen, indem Sie die folgende URL in Ihren Browser eingeben:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("www.sjmcliniclocator.com/clinic", baseFontBlue));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Dort haben Sie die Möglichkeit sich zu registrieren und ein Konto zu erstellen. ");
		s.append("Nach der Anmeldung können Sie Ihre Kontaktinformationen überprüfen, ändern und ");
		s.append("gegebenenfalls ergänzen. Über Ihr Konto können Sie mit uns in Kontakt treten und ");
		s.append("erhalten von uns aktuelle Informationen über dieses Online-Klinikverzeichnis.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Bitte wenden Sie sich mit Fragen oder Anmerkungen zum internationalen Klinikverzeichnis an cliniclocator@sjm.com.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Mit freundlichen Grüßen,", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		 
		this.displaySignature(doc, country, "GERMAN");
		
		s = new StringBuilder();
		s.append("Bitte beachten Sie folgenden Hinweis:\n");
		s.append("Als Teilnehmer des internationalen Klinikverzeichnisses von St. Jude Medical nehmen ");
		s.append("Sie zur Kenntnis und sind Sie damit einverstanden, dass Ihre Klinik zur Annehmlichkeit ");
		s.append("der Patienten auf der Website aufgeführt wird und dass St. Jude Medical Ihre ");
		s.append("Patientenversorgung nicht regelt oder kontrolliert, auch nicht im Zusammenhang mit ");
		s.append("St. Jude Medical Produkten. Des Weiteren behaupten wir nicht, dass Ihre Klinik in ");
		s.append("Bezug auf die Patientenbetreuung kompetenter sei als andere Kliniken. St. Jude Medical ");
		s.append("haftet nicht für Nachsorgen, die durchgeführt wurden, nachdem Patienten Sie aufgrund Ihres ");
		s.append("Eintrags auf unserer Website kontaktiert hatten. Sie halten St. Jude Medical schadlos ");
		s.append("von allen Ansprüchen hinsichtlich der Nachsorge solcher Patienten.");
		par = new Paragraph(new Chunk(s.toString(), baseFontItalics));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
	}
	
	public void french(Document doc, String country) throws DocumentException {
		Paragraph par = new Paragraph(new Chunk("Chère Madame, Cher Monsieur,", baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		StringBuilder s = new StringBuilder();
		s.append("St. Jude Medical va faire figurer sur son site en ligne « International Clinic Locator ");
		s.append("»  les coordonnées des centres les plus importants qui utilisent nos dispositifs, dont ");
		s.append("votre établissement fait partie.  Ce programme est conçu pour les personnes soignées avec ");
		s.append("des dispositifs cardiaques St. Jude Medical.  Ce site permet aux patients d'identifier en ");
		s.append("toute commodité les centres dans lesquels ils peuvent faire effectuer, si le besoin se présentait, ");
		s.append("le contrôle de leur défibrillateur ou stimulateur lors de leurs séjours à l'étranger. Il ne s'agit ");
		s.append("en aucune façon d'une initiative de  marketing ou de promotion de votre établissement.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Nous vous serions reconnaissants de bien vouloir nous accorder un peu de votre temps pour ");
		s.append("contrôler et mettre à jour les coordonnées de votre centre sur notre site afin d'en garantir ");
		s.append("l'exactitude. Vous pouvez aussi, si vous le souhaitez, insérer d'autres informations concernant ");
		s.append("votre établissement, telles que le numéro de fax et l'adresse Internet. Si vous ne tenez pas à être inclus ");
		s.append("dans le programme, vous pouvez bien sûr demander le retrait de vos coordonnées");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Pour le moment, les informations que contient l'« International Clinic Locator » sont les suivantes :", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		List ol = new List(false, 12);
		ol.setIndentationLeft(25);
		ol.setListSymbol("•");
		ol.add(new ListItem(12, "Nom du centre", baseFont));
		ol.add(new ListItem(12, "Adresse du centre (rue, ville, code postal, pays)", baseFont));
		ol.add(new ListItem(12, "Numéro de téléphone du centre", baseFont));
		ol.add(new ListItem(12, "Indications pour l'exécution du suivi d'un défibrillateur/stimulateur St. Jude Medical", baseFont));
		doc.add(ol);
		
		par = new Paragraph(new Chunk("Pour accéder aux données associées à votre centre, veuillez taper l'adresse URL suivante dans votre navigateur :", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("www.sjmcliniclocator.com/clinic", baseFontBlue));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Elle vous permettra de trouver le nom de votre établissement et de contrôler ses ");
		s.append("coordonnées. Il vous sera demandé de créer un compte d'accès pour pouvoir les ");
		s.append("actualiser. Nous vous encourageons à créer ce compte pour que nous puissions communiquer avec vous à propos du programme.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Dès l'approbation d'un administrateur SJM, vos données actualisées seront insérées sur le site de recherche réservé aux patients à l'adresse ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.add(new Chunk("www.sjmcliniclocator.com", baseFontBlue));
		par.add(new Chunk(".", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("En qualité de participant à l'« international Clinic Locator » de St. Jude Medical, ");
		s.append("vous reconnaissez et acceptez que les coordonnées de votre centre soient insérées dans ");
		s.append("la liste publiée sur notre site Internet pour la commodité des patients et que St. Jude ");
		s.append("Medical n'utilise, ne réglemente, ne contrôle les soins que vous assurez à vos patients, ");
		s.append("notamment en ce qui concerne les dispositifs St. Jude Medical, ni n'indique que votre ");
		s.append("centre est plus qualifié qu'un autre pour fournir un traitement aux patients. St. Jude ");
		s.append("Medical ne sera pas tenu responsable des soins éventuels fournis aux patients qui vous ");
		s.append("contacteront après avoir consulté votre fiche sur notre site et, de votre côté, vous vous ");
		s.append("engagez à indemniser et exonérer St. Jude Medical de toute responsabilité pour tout dommage relatif au traitement de ces patients.");
		par = new Paragraph(new Chunk(s.toString(), baseFontItalics));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("N'hésitez pas à adresser vos questions ou commentaires éventuels sur le site « International Travel Locator » à cliniclocator@sjm.com.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Vous remerciant de l'intérêt que vous porterez à cette initiative, nous vous prions d'agréer, Chère Madame, Cher Monsieur, nos salutations distinguées.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		this.displaySignature(doc, country, "FRENCH");
	}
	
	public void english(Document doc, String country) throws DocumentException {
		Paragraph par = new Paragraph(new Chunk("Dear Sir/Madam:", baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		StringBuilder s = new StringBuilder();
		s.append("As a valued customer of St. Jude Medical, your clinic has been included ");
		s.append("in our online International Clinic Locator.  This program is intended for ");
		s.append("people who have been treated with St. Jude Medical cardiac products.  ");
		s.append("The locator serves as a convenient resource for patients who may require follow up ");
		s.append("care for their pacemaker or defibrillator when traveling internationally.  ");
		s.append("It is not meant to be a marketing initiative or an endorsement of your facility.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("We ask that you take the time to review and update your clinic’s contact information ");
		s.append("to ensure that the locator reflects your most accurate information.  You will also have ");
		s.append("the opportunity to include additional contact information about your clinic, such as fax ");
		s.append("number and clinic website.  You are free to opt-out of the program if you do not wish to be included.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("At present, the information contained in the International Clinic Locator includes:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		List ol = new List(false, 15);
		ol.setIndentationLeft(25);
		ol.setListSymbol("•");
		ol.add(new ListItem(15, "Clinic name", baseFont));
		ol.add(new ListItem(15, "Clinic address (street, city, country/province, postal code)", baseFont));
		ol.add(new ListItem(15, "Clinic phone number", baseFont));
		ol.add(new ListItem(15, "Indication for St. Jude Medical pacemaker/defibrillator follow up care ", baseFont));
		doc.add(ol);
		
		par = new Paragraph(new Chunk("To access your clinic information, please type the following URL into your browser:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("www.sjmcliniclocator.com/clinic", baseFontBlue));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("You will then be able to search for your clinic to review your contact information.  ");
		s.append("Once you create a login account, you may update your clinic information as needed.  ");
		s.append("We encourage you to create a login account so that we can communicate with your clinic about the program.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Upon approval by an SJM administrator, your updated contact ");
		s.append("information will be available via the patient search site at ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.add(new Chunk("www.sjmcliniclocator.com", baseFontBlue));
		par.add(new Chunk(".", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("As a participant of the St. Jude Medical International Clinic Locator, ");
		s.append("you acknowledge and agree that your clinic will be listed on the Web site ");
		s.append("as a convenience to patients and that St. Jude Medical does not employ, ");
		s.append("regulate or control your  patient care, including in relation to St. Jude Medical ");
		s.append("Products, nor do we represent that you are more skilled to provide treatment to ");
		s.append("patients than any other clinic.  St. Jude Medical will not be liable for any treatment ");
		s.append("provided to patients as a result of them contacting you from your listing on our Web site, ");
		s.append("and you will indemnify and hold St. Jude Medical harmless from any and all claims relating to treatment of such patients.");
		par = new Paragraph(new Chunk(s.toString(), baseFontItalics));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Feel free to forward questions and comments about the International Travel Locator to cliniclocator@sjm.com.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Sincerely,", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		 
		this.displaySignature(doc, country, "ENGLISH");
	}
	
	public void italian(Document doc, String country) throws DocumentException {
		Paragraph par = new Paragraph(new Chunk("Gentili dottori,", baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		StringBuilder s = new StringBuilder();
		s.append("da stimati clienti St. Jude Medical quali Voi siete, la Vostra clinica è stata ");
		s.append("inserita nel nostro sito Internet International Clinic Locator. Tale sito è una ");
		s.append("risorsa per tutti quei pazienti ai quali è stato impiantato un dispositivo St. ");
		s.append("Jude Medical e che potrebbero avere bisogno di eseguire un follow up per il loro ");
		s.append("dispositivo quando si recano all’estero. Questa iniziativa non è da intendere ");
		s.append("come un'azione di marketing nè come una promozione della vostra struttura, l’obiettivo ");
		s.append("invece che il sito International Clinic Locator si propone è unicamente quello ");
		s.append("di offrire ai pazienti un pratico sistema per accedere ai dati delle cliniche. ");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Vi chiederemmo gentilmente di controllare ed aggiornare le informazioni relative ai ");
		s.append("contatti della vostra clinica onde garantirne la massima accuratezza.  Avrete inoltre ");
		s.append("l’opportunità di inserire ulteriori informazioni riguardo i contatti della ");
		s.append("vostra clinica quali per esempio il numero di fax e l’indirizzo Internet.  ");
		s.append("Qualora non desideriate essere inclusi nel programma, siete liberi di rinunciare a tale opportunità. ");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Attualmente le informazioni contenute nel sito International Clinic Locator sono le seguenti:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		List ol = new List(false, 15);
		ol.setIndentationLeft(25);
		ol.setListSymbol("•");
		ol.add(new ListItem(15, "Nome della clinica", baseFont));
		ol.add(new ListItem(15, "Indirizzo della clinica (via, città, stato, codice postale)", baseFont));
		ol.add(new ListItem(15, "Numero di telefono della clinica", baseFont));
		ol.add(new ListItem(15, "Indicazioni per l’esecuzione del follow-up per un pacemaker e/o un defibrillatore St. Jude Medical", baseFont));
		doc.add(ol);
		
		par = new Paragraph(new Chunk("Per visualizzare ed accedere ai dati della vostra clinica, potete visitare il seguente link.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("www.sjmcliniclocator.com/clinic", baseFontBlue));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Successivamente all'interno del sito potrete cercare la Vostra clinica in modo da poter ");
		s.append("verificare se le informazioni riguardo ai Vostri contatti siano corrette. Creando un ");
		s.append("account di accesso (Login) potrete aggiornare le informazioni riguardanti la Vostra ");
		s.append("clinica come riterrete necessario. Si incoraggia a creare un account in modo da ");
		s.append("consentirci di comunicare con la vostra clinica attraverso il programma.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Contestualmente all'approvazione da parte di un amministratore SJM del sito, le informazioni ");
		s.append("aggiornate dei Vostri contatti saranno disponibili per i pazienti attraverso il sito di ricerca all'indirizzo ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.add(new Chunk("www.sjmcliniclocator.com", baseFontBlue));
		par.add(new Chunk(".", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("In qualità di partecipante al programma International Clinic Locator di St.Jude Medical, riconoscete ");
		s.append("ed accettate che la Vostra clinica venga inserita nell'elenco pubblicato sul sito web per offrire un ");
		s.append("pratico servizio ai pazienti e che St. Jude Medical non impiega, regola o controlla l'assistenza da ");
		s.append("Voi prestata ai Vostri pazienti inclusa l'assistenza relativa ai prodotti St. Jude Medical , e che ");
		s.append("non si dichiara che la Vostra clinica possiede maggiori abilità professionali nel fornire il ");
		s.append("trattamento di follow up ai pazienti rispetto ad altre cliniche.  St. Jude Medical non è responsabile ");
		s.append("per il trattamento fornito ai pazienti che Vi contattano tramite l'elenco presente sul sito e da parte ");
		s.append("Vostra vi impegnate ad indennizzare e a tenere St. Jude Medical al riparo da qualsiasi ");
		s.append("rivendicazione per danni correlati al trattamento di tali pazienti.");
		par = new Paragraph(new Chunk(s.toString(), baseFontItalics));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Eventuali domande e/o commenti sul locator possono essere inviate all'indirizzo cliniclocator@sjm.com.  Da parte nostra sarà esercitato il massimo impegno nel seguirvi con la massima tempestività.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Cordiali saluti,", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		 
		this.displaySignature(doc, country,"ITALIAN");
	}
	
	public void norwegian(Document doc, String country) throws DocumentException {
		Paragraph par = new Paragraph(new Chunk("Kjære kunde!", baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		StringBuilder s = new StringBuilder();
		s.append("Som verdifull kunde hos St. Jude Medical er din klinikk nå inkludert i vår elektroniske ");
		s.append("internasjonale klinikklokator.  Dette programmet er beregnet for personer som er blitt ");
		s.append("behandlet med hjerteprodukter fra St. Jude Medical.  Lokatoren fungerer som en praktisk ");
		s.append("ressurs for pasienter som kan ha behov for oppfølgingstjenester for sine pacemakere eller ");
		s.append("defibrillatorer når de reiser internasjonalt.  Den er ikke ment som et ");
		s.append("markedsføringsinitiativ eller et utvidet tilbud fra din klinikk. ");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Vi ber deg ta deg tid til å gjennomgå og oppdatere din klinikks kontaktinformasjon for å ");
		s.append("sikre at lokatoren gjengir informasjonen helt nøyaktig.  Du vil også ha anledning til å ");
		s.append("inkludere ekstra kontaktinformasjon om din klinikk, som f.eks. faksnummer og klinikkens ");
		s.append("nettsted.  Du har mulighet til å velge deg ut av programmet, dersom du ikke ønsker å være inkludert.  ");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Skriv inn følgende URL i nettleseren din for å få tilgang til din klinikks informasjon:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		List ol = new List(false, 15);
		ol.setIndentationLeft(25);
		ol.setListSymbol("•");
		ol.add(new ListItem(15, "Klinikkens navn", baseFont));
		ol.add(new ListItem(15, "Klinikkens adresse (gate, by, land/provins, postnummer)", baseFont));
		ol.add(new ListItem(15, "Klinikkens telefonnummer", baseFont));
		ol.add(new ListItem(15, "Angivelse for St. Jude Medical oppfølgingstjeneste for pacemaker/defibrillator", baseFont));
		doc.add(ol);
		
		par = new Paragraph(new Chunk("Skriv inn følgende URL i nettleseren din for å få tilgang til din klinikks informasjon:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("www.sjmcliniclocator.com/clinic", baseFontBlue));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Da kan du søke etter din klinikk for å gjennomgå din kontaktinformasjon.   Når du har opprettet ");
		s.append("en påloggingskonto, kan du oppdatere din klinikks informasjon etter behov.  Vi oppfordrer deg til ");
		s.append("å opprette en påloggingskonto, slik at vi kan kommunisere med din klinikk om programmet.  ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Etter godkjenning av en SJM-administrator vil din oppdaterte ");
		s.append("kontaktinformasjon være tilgjengelig via pasientsøkerstedet på ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.add(new Chunk("www.sjmcliniclocator.com", baseFontBlue));
		par.add(new Chunk(".", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Som medlem av St. Jude Medicals internasjonale klinikklokator aksepterer og godtar ");
		s.append("du at din klinikk blir lagt ut på nettet som en service til pasienter, at St. ");
		s.append("Jude Medical ikke anvender, regulerer eller kontrollerer din pasientbehandling, ");
		s.append("inkludert i sammenheng med St. Jude Medical produkter, og at vi heller ikke ");
		s.append("fremhever at du er mer kompetent enn andre klinikker til å gi behandling til pasienter.  ");
		s.append("St. Jude Medical er ikke ansvarlig for noen behandling som blir gitt til pasienter ");
		s.append("som et resultat av at de har kontaktet din klinikk fordi den er lagt ut på vårt nettsted, ");
		s.append("og du kan ikke kreve erstatning fra eller holde St. Jude Medical ansvarlig for noen eller ");
		s.append("alle krav i forbindelse med behandling av slike pasienter.  ");
		par = new Paragraph(new Chunk(s.toString(), baseFontItalics));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Hvis du har spørsmål eller kommentarer angående den internasjonale reiselokatoren, er det bare å sende en e-post til cliniclocator@sjm.com.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Vennlig hilsen", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		 
		this.displaySignature(doc, country, "NORWEGIAN");
	}
	
	
	public void dutch(Document doc, String country) throws DocumentException {
		Paragraph par = new Paragraph(new Chunk("Geachte mijnheer/mevrouw,", baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		StringBuilder s = new StringBuilder();
		s.append("als gewaardeerde klant van St. Jude Medical is uw kliniek opgenomen in onze online ");
		s.append("International Clinic Locator,  een programma dat bedoeld is voor mensen die behandeld ");
		s.append("zijn met cardioproducten van St. Jude Medical.  Deze locator dient als handig ");
		s.append("hulpmiddel voor patiënten die nazorg nodig hebben voor hun pacemaker of defibrillator ");
		s.append("als ze in het buitenland zijn.  Hij is niet bedoeld als marketinginitiatief of voor certificering van uw faciliteit.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Wij vragen u om even tijd te maken om de contactinformatie van uw kliniek te controleren en te ");
		s.append("actualiseren, om er zeker van te zijn dat de locator exacte gegevens bevat.  Ook heeft u de ");
		s.append("gelegenheid om aanvullende contactinformatie, zoals een faxnummer en website, voor uw kliniek ");
		s.append("toe te voegen.  U bent vrij om af te zien van deelname aan het programma als u niet hierin wenst te worden opgenomen.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Op dit moment bestaat de informatie in de International Clinic Locator uit het volgende:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		List ol = new List(false, 15);
		ol.setIndentationLeft(25);
		ol.setListSymbol("•");
		ol.add(new ListItem(15, "Naam van de kliniek", baseFont));
		ol.add(new ListItem(15, "Adres van de kliniek (weg, plaats/provincie, postcode)", baseFont));
		ol.add(new ListItem(15, "Telefoonnummer van de kliniek", baseFont));
		ol.add(new ListItem(15, "Indicatie voor nazorg voor St. Jude Medical pacemakers/defibrillators", baseFont));
		doc.add(ol);
		
		par = new Paragraph(new Chunk("Om toegang te krijgen tot de informatie over uw kliniek gelieve de volgende URL in uw browser te typen:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("www.sjmcliniclocator.com/clinic", baseFontBlue));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Vervolgens kunt u uw kliniek opzoeken om uw contactinformatie te controleren.   Nadat u een login account ");
		s.append("heeft aangemaakt, kunt u de gegevens van uw kliniek naar wens actualiseren.  We moedigen u aan een login ");
		s.append("account aan te maken, zodat we met uw kliniek kunnen communiceren omtrent het programma.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Na goedkeuring door een SJM beheerder, komt uw geactualiseerde contactinformatie beschikbaar via de zoekwebsite voor patiënten ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.add(new Chunk("www.sjmcliniclocator.com", baseFontBlue));
		par.add(new Chunk(".", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Als deelnemer aan de St. Jude Medical International Clinic Locator bevestigt en accepteert u dat uw kliniek op de ");
		s.append("website wordt genoemd als faciliteit voor patiënten, dat St. Jude Medical uw patiëntenzorg niet gebruikt, ");
		s.append("reguleert of controleert, ook niet voor wat betreft St. Jude Medical producten, noch geven wij de indruk ");
		s.append("dat u ervarener bent in het verlenen van patiëntbehandeling dan enige andere kliniek.  St. Jude Medical ");
		s.append("kan niet aansprakelijk worden gesteld voor enige behandeling die aan patiënten geleverd wordt als gevolg ");
		s.append("van het feit dat zij contact met u hebben opgenomen vanwege de vermelding van uw kliniek op onze website, ");
		s.append("en u zult St. Jude Medical vrijwaren en vrijpleiten voor alle claims met betrekking tot de behandeling van dergelijke patiënten.");
		par = new Paragraph(new Chunk(s.toString(), baseFontItalics));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Aarzel niet om vragen over de International Travel Locator te stellen en opmerkingen te sturen aan cliniclocator@sjm.com.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Met vriendelijke groet,", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		 
		this.displaySignature(doc, country, "DUTCH");
	}
	
	public void portugese(Document doc, String country) throws DocumentException {
		Paragraph par = new Paragraph(new Chunk("Exmo(a). Senhor(a) Dr.(a):", baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		StringBuilder s = new StringBuilder();
		s.append("Na qualidade de cliente da St. Jude Medical, o seu Hospital foi incluído no nosso Localizador ");
		s.append("Internacional de Hospitais online.  Este programa destina-se a pacientes que tenham sido ");
		s.append("implantados com dispositivos cardíacos da St. Jude Medical.  O localizador funciona como ");
		s.append("um cómodo recurso para pacientes que durante as suas viagens pelo estrangeiro venham a ");
		s.append("necessitar de cuidados de acompanhamento para o respectivo pacemaker ou desfibrilhador.  ");
		s.append("Este programa não foi concebido para ser uma iniciativa de marketing ou uma forma de recomendação da sua instituição.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Solicitamos-lhe que dedique algum do seu tempo à revisão e actualização das informações de contacto ");
		s.append("do seu Hospital de forma a garantir que o localizador apresenta as informações correctas.  Terá ");
		s.append("igualmente a possibilidade de incluir informações de contacto adicionais relativas ao seu Hospital, ");
		s.append("tais como o número de fax e o Web site do Hospital.  Poderá optar por não participar ");
		s.append("no programa, caso não deseje ser incluído(a) no mesmo.");
		
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Actualmente, as informações apresentadas no Localizador Internacional de Hospitais incluem", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		List ol = new List(false, 15);
		ol.setIndentationLeft(25);
		ol.setListSymbol("•");
		ol.add(new ListItem(15, "Nome do Hospital", baseFont));
		ol.add(new ListItem(15, "Endereço do Hospital (rua, cidade, país/região, código postal)", baseFont));
		ol.add(new ListItem(15, "Número de telefone do Hospital", baseFont));
		ol.add(new ListItem(15, "Indicação sobre possibilidade de follow-up de pacemaker/desfibrilhador da St. Jude Medical", baseFont));
		doc.add(ol);
		
		par = new Paragraph(new Chunk("Para aceder às informações do seu Hospital, introduza o URL seguinte no seu browser:", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		par.setSpacingBefore(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("www.sjmcliniclocator.com/clinic", baseFontBlue));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Poderá depois procurar o seu Hospital para rever as respectivas informações de contacto.   Depois de ");
		s.append("criar uma conta de acesso, poderá actualizar as informações do seu Hospital consoante o necessário.  ");
		s.append("Encorajámo-lo(a) a criar uma conta de acesso para que possamos entrar em contacto com o seu Hospital e ");
		s.append("fornecer informações relativas ao programa.");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Após aprovação por parte de um administrador da SJM, as suas informações de contacto actualizadas ");
		s.append("serão disponibilizadas na página de pesquisa do paciente, no endereço ");
		par = new Paragraph(new Chunk(s.toString(), baseFont));
		par.add(new Chunk("www.sjmcliniclocator.com", baseFontBlue));
		par.add(new Chunk(".", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		s = new StringBuilder();
		s.append("Na qualidade de participante no programa Localizador Internacional de Hospitais da St. Jude ");
		s.append("Medical, aceita e concorda que o seu Hospital seja apresentado no Web site como uma vantagem ");
		s.append("para os pacientes e que a St. Jude Medical não utiliza, regula ou controla os cuidados ");
		s.append("prestados aos pacientes, incluindo os cuidados relativos aos produtos da St. Jude Medical, ");
		s.append("nem que tal apresentação constitui uma indicação de que o seu Hospital está mais habilitado ");
		s.append("a prestar cuidados aos pacientes do que qualquer outro Hospital.  A St. Jude Medical não pode ");
		s.append("ser responsabilizada pelos tratamentos prestados aos pacientes, resultantes do contacto por ");
		s.append("eles estabelecido devido à apresentação das informações de contacto do seu Hospital no nosso ");
		s.append("Web site, e o participante concorda em indemnizar e isentar de responsabilidade a St. Jude Medical ");
		s.append("relativamente a quaisquer queixas relacionadas com o tratamento de tais pacientes.");
		par = new Paragraph(new Chunk(s.toString(), baseFontItalics));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Caso tenha dúvidas ou comentários a fazer relativos ao programa Localizador Internacional de Hospitais, poderá enviá-los para o endereço de correio electrónico cliniclocator@sjm.com.", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		
		par = new Paragraph(new Chunk("Atentamente,", baseFont));
		par.setAlignment(Element.ALIGN_JUSTIFIED);
		par.setSpacingAfter(PARAGRAPH_SPACING);
		doc.add(par);
		 
		this.displaySignature(doc, country, "PORTUGESE");
	}
	
	/**
	 * 
	 * @param doc
	 * @throws DocumentException
	 */
	public void displaySignature(Document doc, String country, String lang) 
	throws DocumentException {
		Integer type = (Integer) countryVals.get(country).getValue();
		try {
			String key = (String)signatures.get(type).getKey();
			if ("CA".equalsIgnoreCase(country) && "FRENCH".equalsIgnoreCase(lang))
				key = "sig_20.png";
			
			Image image = Image.getInstance(imagePath + key);
			doc.add(image);
		} catch (Exception e) {
			log.error("Unable to retrieve signature", e);
		}
		
		Paragraph par = new Paragraph(new Chunk((String) signatures.get(type).getValue(), baseFont));
		par.setAlignment(Element.ALIGN_LEFT);
		doc.add(par);
		
	}

	/**
	 * Assigns the letter to the country and identifies the signature for each
	 */
	public void assignCountries(int type) {
		
		switch (type) {
			case 0:
				// Countries having their letters printed and mailed by SMT
				filePath = "/Users/james/Temp/SJMPrint/sjm/SMT_Mail/MAIL_TO_CLINIC/";
				countryVals.put("FR", new GenericVO("FRENCH", Integer.valueOf(2)));
				countryVals.put("DE", new GenericVO("GERMAN", Integer.valueOf(2)));
				countryVals.put("RS", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("ZA", new GenericVO("ENGLISH", Integer.valueOf(3)));
				countryVals.put("ES", new GenericVO("SPANISH", Integer.valueOf(2)));
				countryVals.put("GB", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("EE", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("CY", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("GR", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("MT", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("YE", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("BH", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("EG", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("IQ", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("JO", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("KW", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("LB", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("LY", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("OM", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("PS", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("QA", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("SA", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("AE", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("BY", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("BA", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("BG", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("HR", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("KZ", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("LV", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("MK", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("ME", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("RO", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("RU", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("SK", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("SI", new GenericVO("ENGLISH", Integer.valueOf(2))); 
				countryVals.put("UA", new GenericVO("ENGLISH", Integer.valueOf(2)));
				countryVals.put("GP", new GenericVO("FRENCH", Integer.valueOf(2))); 
				countryVals.put("RE", new GenericVO("FRENCH", Integer.valueOf(2))); 
				countryVals.put("MQ", new GenericVO("FRENCH", Integer.valueOf(2)));
				countryVals.put("NC", new GenericVO("FRENCH", Integer.valueOf(2))); 
				break;
				
			case 1:
				// Countries having their letters printed by SMT and mailed to country
				filePath = "/Users/james/Temp/SJMPrint/sjm/SMT_Mail/SEND_TO_COUNTRY/";
				countryVals.put("AT", new GenericVO("GERMAN", Integer.valueOf(2)));
				countryVals.put("IL", new GenericVO("ENGLISH", Integer.valueOf(1)));
				countryVals.put("IT", new GenericVO("ITALIAN", Integer.valueOf(2)));
				countryVals.put("FI", new GenericVO("ENGLISH", Integer.valueOf(4))); 
				break;
			
			case 2:
				// Other countries
				filePath = "/Users/james/Temp/SJMPrint/sjm/SJM_Mail/";
				countryVals.put("BE", new GenericVO("FRENCH", Integer.valueOf(13))); 
				countryVals.put("CA", new GenericVO("ENGLISH", Integer.valueOf(14))); 
				countryVals.put("CH", new GenericVO("GERMAN", Integer.valueOf(16))); 
				countryVals.put("DK", new GenericVO("ENGLISH", Integer.valueOf(7))); 
				countryVals.put("HU", new GenericVO("ENGLISH", Integer.valueOf(15))); 
				countryVals.put("IS", new GenericVO("ENGLISH", Integer.valueOf(6))); 
				countryVals.put("LT", new GenericVO("ENGLISH", Integer.valueOf(17))); 
				countryVals.put("LU", new GenericVO("FRENCH", Integer.valueOf(13))); 
				countryVals.put("NL", new GenericVO("DUTCH", Integer.valueOf(11))); 
				countryVals.put("NO", new GenericVO("NORWEGIAN", Integer.valueOf(10))); 
				countryVals.put("PT", new GenericVO("PORTUGUESE", Integer.valueOf(5))); 
				countryVals.put("SE", new GenericVO("ENGLISH", Integer.valueOf(12))); 
				break;
		}
		
	}
	
	/**
	 * 
	 */
	public void assignSignatures() {
		signatures.put(Integer.valueOf(1), new GenericVO("sig_1.png","Joris Dewals \nMarketing Director CRM/AF – Emerging Markets and Distributors"));
		signatures.put(Integer.valueOf(2), new GenericVO("sig_2.png","Per Persson \nSr. Marketing Director AF-CRM EMEAC"));
		signatures.put(Integer.valueOf(3), new GenericVO("sig_3.png","Patrick Godard \nChief Executive Officer\nAmayeza Abantu (Pty) Ltd"));
		signatures.put(Integer.valueOf(4), new GenericVO("sig_4.png","Samuel Sipinen  \nArea Sales Manager Cardiac Rhythm Management"));
		signatures.put(Integer.valueOf(5), new GenericVO("sig_5.png","João Matos Alves  \nGeneral Manager"));
		signatures.put(Integer.valueOf(6), new GenericVO("sig_6.png","Peter Jagd Nielsen  \nSales Manager Iceland "));
		signatures.put(Integer.valueOf(7), new GenericVO("sig_7.png","Pøul Tølbøll  \nNational Sales Manager, AF/CRM"));
		signatures.put(Integer.valueOf(10), new GenericVO("sig_10.png","Tor Håkon Grønli  \nKey Account Manager Cardiac Rhythm Management"));
		signatures.put(Integer.valueOf(11), new GenericVO("sig_11.png","Wim Boute  \nMarketing Manager, CRM/AF"));
		signatures.put(Integer.valueOf(12), new GenericVO("sig_12.png","Steinar Holmstrom  \nCountry Sales Manager Cardiac Rhythm Management Division"));
		signatures.put(Integer.valueOf(13), new GenericVO("sig_19.png","Yves Dewilde                                                                                         Stephane Sacre\nDivisional Manager, CRM/AF                                                                Marketing Manager"));
		signatures.put(Integer.valueOf(14), new GenericVO("sig_17.png","Jeff Mamer  \nMarketing Director, CRM/AF\nSt. Jude Medical Canada"));
		signatures.put(Integer.valueOf(15), new GenericVO("sig_17.png","Károly Miklós  \nSales Manager Hungary"));
		signatures.put(Integer.valueOf(16), new GenericVO("sig_17.png","Manuel Heuer                                                                              Andino Hartkopf\nCountry Manager Switzerland                                                     Division Manager AF/CRM Switzerland"));
		signatures.put(Integer.valueOf(17), new GenericVO("sig_17.png","Giedrius Semetas  \nRegional sales manager"));
	}
	
	/**
	 * 
	 */
	public void assignSpacing() {
		spacing.put("ENGLISH", PAGE_MARGIN_TOP + 40);
		spacing.put("DUTCH", PAGE_MARGIN_TOP + 21);
		spacing.put("PORTUGUESE", PAGE_MARGIN_TOP + 5);
		spacing.put("GERMAN", PAGE_MARGIN_TOP + 23);
		spacing.put("FRENCH", PAGE_MARGIN_TOP + 4);
		spacing.put("ITALIAN", PAGE_MARGIN_TOP);
		spacing.put("SPANISH", PAGE_MARGIN_TOP + 13);
		spacing.put("NORWEGIAN", PAGE_MARGIN_TOP + 30);
	}
}
