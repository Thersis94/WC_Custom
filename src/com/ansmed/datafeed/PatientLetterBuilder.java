package com.ansmed.datafeed;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;

import org.apache.log4j.Logger;

import com.ansmed.sb.physician.ClinicVO;
import com.ansmed.sb.physician.SurgeonVO;
import com.siliconmtn.http.parser.StringEncoder;
import com.siliconmtn.security.PhoneVO;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title</b>: PatientLetterBuilder.java<p/>
 * <b>Description: </b> Builds the patient letter for a patient. 
 * <p/>
 * <b>Copyright:</b> (c) 2010<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author David Bargerhuff
 * @version 1.0
 * @since Nov. 15, 2010
 * Change Log:
 * May 2011 - refactored send date format to support language format
 ****************************************************************************/
public class PatientLetterBuilder {
	
	private static Logger log = Logger.getLogger(PatientLetterBuilder.class);
	
	//private StringBuilder sendDate;
	
	private StringBuilder englishLetter = null;
	private StringBuilder spanishLetter = null;
	
	private StringBuilder firstImageHeader = null;
	private StringBuilder secondImageHeader = null;
	
	private StringBuilder xmlEmptyCell = new StringBuilder("<w:tc>\n<w:p>\n</w:p>\n</w:tc>\n");
	private StringBuilder xmlEmptyRow = new StringBuilder("<w:tr>\n<w:tc>\n<w:p>\n</w:p>\n</w:tc>\n<w:tc>\n<w:p>\n</w:p>\n</w:tc>\n</w:tr>\n");
	private StringBuilder xmlLineBreak = new StringBuilder("<w:br w:type=\"text-wrapping\"/>");
	//private StringBuilder xmlPageBreak = new StringBuilder("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>");
	private StringBuilder xmlPageBreak = new StringBuilder("<w:p><w:br w:type=\"page\"/></w:p>");
	
	/**
	 * 
	 */
	public PatientLetterBuilder() {
	}
	
	/**
	 * Builds patient letter.
	 * @param udv
	 * @param lSurgeonVO
	 * @param limit
	 * @param efb
	 * @param format
	 * @return
	 */
	protected String buildPatientLetter(UserDataVO udv, List<SurgeonVO> lSurgeonVO, 
			int limit, ExcelFileBuilder efb, String format, int count) {
		
		StringEncoder se = new StringEncoder();
		
		Iterator<SurgeonVO> iterSurgeonVO = lSurgeonVO.iterator();
		List<StringBuilder> patientClinics = new ArrayList<StringBuilder>();
		SurgeonVO sv = null;
		ClinicVO cv = null;
		PhoneVO phv = null;
		
		StringBuilder firstName = new StringBuilder(StringUtil.capitalizePhrase(scrubXML(se.decodeValue(udv.getFirstName().trim()))));
		StringBuilder fullName = new StringBuilder(firstName);
		String lastName = scrubXML(se.decodeValue(udv.getLastName().trim())).replace(",", "");
		fullName.append(" ").append(StringUtil.capitalizePhrase(lastName));
		String address = StringUtil.capitalizeAddress(scrubXML(se.decodeValue(udv.getAddress().trim())));
		address = StringUtil.replace(address,"#"," ");
		String city = StringUtil.capitalizePhrase(scrubXML(se.decodeValue(udv.getCity().trim())));
		String state = udv.getState();
		
		String zipCode = udv.getZipCode();
		//strip out the "plus 4" portion of the zipcode if it exists.
		int index = zipCode.indexOf("-");
		if (index > -1) {
			zipCode = zipCode.substring(0,index);
		}
		
		// Format the phone number in dashed format.
		//PhoneNumberFormat pnf = new PhoneNumberFormat(udv.getMainPhone(),3);
		//String phone = pnf.getFormattedNumber();
		String phone = udv.getMainPhone();
		log.debug("main phone is: " + phone);
		
		// Build the patient's first name.
		StringBuilder patientGreeting = new StringBuilder(firstName).append(",");
		
		// Build the patient's address.
		//StringBuilder patientName = new StringBuilder("<w:t>").append(fullName).append("</w:t>\n");
		StringBuilder patientName = new StringBuilder("<w:r><w:br/><w:t>").append(fullName).append("</w:t></w:r>\n");
		
		//patientName.append(xmlLineBreak).append("\n");
		
		//StringBuilder patientStreetAddr = new StringBuilder("<w:t>").append(address);
		StringBuilder patientStreetAddr = new StringBuilder("<w:r><w:br/><w:t>").append(address);
				
		if ((udv.getAddress2() != null) && (udv.getAddress2().length() > 0)) {
			patientStreetAddr.append(", ");
			patientStreetAddr.append(StringUtil.replace(scrubXML(StringUtil.capitalizeAddress(udv.getAddress2().trim())), "#"," "));
			//patientStreetAddr.append("</w:t>\n").append(xmlLineBreak).append("\n");
			patientStreetAddr.append("</w:t></w:r>\n");
		} else {
			//patientStreetAddr.append("</w:t>\n").append(xmlLineBreak).append("\n");
			patientStreetAddr.append("</w:t></w:r>\n");
		}
		
		//StringBuilder patientCityStateZip = new StringBuilder("<w:t>");
		StringBuilder patientCityStateZip = new StringBuilder("<w:r><w:br/><w:t>");
		patientCityStateZip.append(city).append(", ");		
		patientCityStateZip.append(state).append("  ");			
		patientCityStateZip.append(zipCode);
		//patientCityStateZip.append("</w:t>\n");
		//patientCityStateZip.append(xmlLineBreak).append("\n");
		patientCityStateZip.append("</w:t></w:r>\n");
		
		StringBuilder patientAddr = new StringBuilder(patientName);
		patientAddr.append(patientStreetAddr).append(patientCityStateZip);

		efb.addRow(fullName.toString(),address, city, state, zipCode, phone);
		
		// Iterate SurgeonVO list to get Clinic.  If profile data is invalid, skip.
		int counter = 1;
		while(iterSurgeonVO.hasNext() && (counter <= limit)) {
			
			sv = iterSurgeonVO.next();
			cv = sv.getClinic();
			
			//iterate Clinic and get first valid phone number
			Iterator<PhoneVO> iterClinic = cv.getPhones().iterator();
			StringBuilder clinicPhone = new StringBuilder();
			int phoneCount = 0;
			while(iterClinic.hasNext() && phoneCount < 1) {
				
				phv = iterClinic.next();
				if (phv.isValidPhone()) {
					phv.setFormatType(2);
					clinicPhone.append(phv.getFormattedNumber());
					phoneCount++;
				}
			}
			
			// Retrieve patient clinics and add to list.
			patientClinics.add(buildClinics(cv,sv,clinicPhone));
			
			counter++;
			
		}
		
		//log.debug(patientGreeting.toString());
		//log.debug(patientAddr.toString());
		
		// Format the table rows for patient clinics list
		StringBuilder clinicBody = new StringBuilder();
		clinicBody = buildRows(patientClinics,limit);
		//log.debug(clinicBody.toString());

		String patientBody = null;
		if (format.equalsIgnoreCase("spanish")) {
			patientBody = new String(spanishLetter);
		} else {
			patientBody = new String(englishLetter);
		}
		
		// replace placeholders with appropriate data
		patientBody = patientBody.replace("#date#", formatSendDate(format));
		patientBody = patientBody.replace("#address#", patientAddr.toString());
		patientBody = patientBody.replace("#patient#", patientGreeting.toString());
		if (count == 1) {
			// this is the first page for this language so include the header image binary data
			patientBody = patientBody.replace("#imageHeader#", firstImageHeader);
		} else {
			// include the image reference only, create a unique image reference id
			patientBody = patientBody.replace("#imageHeader#", secondImageHeader);
			patientBody = patientBody.replace("#sjmLogoId#", ("sjmLogoId_" + count));
		}
		patientBody = patientBody.replace("#clinics#", clinicBody.toString());

		/*		
		if (lastPage) {
			// add the final section properties to set the page margins
			patientBody = patientBody.replace("#lastPage#", clinicBody.toString());
		} else {
			// 
			patientBody = patientBody.replace("#lastPage#", "");
		}
		*/
			
		return patientBody;
		
	}
	
	/**
	 * Formats info about each clinic as an HTML table and appends to body.
	 * @param cvo
	 * @param svo
	 * @param cPhone
	 * @return
	 */
	protected StringBuilder buildClinics(ClinicVO cvo, SurgeonVO svo, StringBuilder cPhone) {
		
		StringBuilder body = new StringBuilder();
		boolean hasClinicName = false;
		
		if ((cvo.getClinicName() != null) && (cvo.getClinicName().trim().length() > 0)) {
			hasClinicName = true;
		} else {
			hasClinicName = false;
		}
		
		// Start the table cell
		//body.append("<w:tc>\n<w:p>\n<w:r>\n");
		body.append("<w:tc>\n<w:p>\n<w:pPr>\n<w:pStyle w:val=\"tableBody\"/>\n</w:pPr>\n<w:r>\n");
		
		if (hasClinicName) {
			body.append("<w:t>").append(scrubXML((cvo.getClinicName().trim())));
			body.append("</w:t>\n");
			body.append(xmlLineBreak).append("\n");
		}
		
		body.append("<w:t>").append(scrubXML(svo.getFirstName().trim())).append(" ");
		body.append(scrubXML(svo.getLastName().trim()));
		if ((svo.getTitle() != null) && (svo.getTitle().trim().length() > 0)) {
			body.append(", ").append(scrubXML(svo.getTitle().trim())).append("</w:t>\n");
			body.append(xmlLineBreak).append("\n");
		} else {
			body.append("</w:t>\n").append(xmlLineBreak).append("\n");
		}
		
		body.append("<w:t>").append(scrubXML(cvo.getAddress().trim()));
		if ((cvo.getAddress2() != null) && (cvo.getAddress2().trim().length() > 0)) {
			body.append(", ").append(scrubXML(cvo.getAddress2().trim()));
		}
		if ((cvo.getAddress3() != null) && (cvo.getAddress3().trim().length() > 0)) {
			body.append(", ").append(scrubXML(cvo.getAddress3().trim()));
		}
		body.append("</w:t>\n").append(xmlLineBreak).append("\n");
		
		body.append("<w:t>");
		if ((cvo.getCity() != null) && cvo.getCity().length() > 0) {
			body.append(scrubXML(cvo.getCity().trim())).append(", ");
		} else {
			body.append("No city, ");
		}
		
		if ((cvo.getState() != null) && cvo.getState().length() > 0) {
			body.append(cvo.getState()).append(" ");
		} else {
			body.append("No state ");
		}
		
		if ((cvo.getZipCode() != null) && cvo.getZipCode().length() > 0) {
			body.append(cvo.getZipCode()).append(" ").append("</w:t>\n");
		} else {
			body.append("No zipcode.").append("</w:t>\n");
		}
		body.append(xmlLineBreak).append("\n");
		
		// Valid phone number check.
		if (cPhone != null && cPhone.length() > 0) {
			body.append("<w:t>").append(cPhone).append("</w:t>\n");
		} else {
			body.append("<w:t>No phone number listed.</w:t>\n");
		}
		body.append(xmlLineBreak).append("\n");
		
		body.append("<w:t>").append(svo.getDistance()).append(" miles</w:t>\n");
		
		// If there wasn't a clinic name, then end the inner table with blank.
		if (!hasClinicName) {
			body.append("<w:t></w:t>\n");
		}

		// Close the table cell
		body.append("</w:r>\n</w:p>\n</w:tc>\n");
		
		return body;
	}
	
	/**
	 * Builds the XML table rows for patient clinic list.
	 * @param clinics
	 * @param max
	 * @return
	 */
	protected StringBuilder buildRows(List<StringBuilder> clinics, int max) {
	
		int size = clinics.size();
		if(size == 0) return new StringBuilder();
		if(size > max) size = max;
		log.debug("Clinic size: " + size);
		//rpp = rows per page
		int rpp = 5;
	
		// Number of pages to be built.
		int pageCount = 1;
		if (size > 10) {
			pageCount = pageCount + ((size - 10) / 12);
			if ((size - 10) % 12 > 0) {
				pageCount++;
			}
		}
		log.debug("pageCount: " + pageCount);
		
		// Starting/Ending cell number
		int startCell = 0;
		int nextCell = 0;
	
		StringBuilder rows = new StringBuilder();
		
		// Iterate through the number of pages to be built.
		for (int i = 0; i < pageCount; i++) {
		
			//log.debug("Building clinic page: " + (i + 1));
			
			// 1st page has 5 clinic rows, subsequent pages have 6 rows.
			if (i == 0) {
				startCell = 1;
			} else {
				startCell = (i * 12) - 1;
				rpp = 6;
			}
			
			// Iterate the rows to build the page.
			for (int j = 1; j <= rpp; j++) {
				
				// startCell is the left-hand column position in the row
				//log.debug("startCell = " + startCell);
				if (startCell <= size) {
					rows.append("<w:tr>\n").append(clinics.get(startCell - 1));
					
					// nextCell is the right-hand column position in the row
					nextCell = startCell + rpp;
					//log.debug("nextCell = " + nextCell);
					if (nextCell <= size) {
						rows.append(clinics.get(nextCell - 1));
					} else {
						rows.append(xmlEmptyCell).append("\n");
					}
					
					rows.append("</w:tr>\n");
					
					// Add a spacer between rows.
					if (j < rpp && rpp != 1) {
						rows.append(xmlEmptyRow).append("\n");
					}
					
				} else {
					rows.append(xmlEmptyRow).append("\n");
				}
				
				startCell++;
			}
			
			// If not last page, append a page break.
			if ((i + 1) < pageCount) {
				rows.append(xmlPageBreak);
			}
		}
					
		return rows;
	}
	
	/**
	 * replaces XML reserved characters with their HTML-equiv values.
	 * @param val
	 * @return
	 */
	protected String scrubXML(String val) {
		val = StringUtil.checkVal(val);
		val = StringUtil.replace(val, "&", "&amp;");
		val = StringUtil.replace(val, "'", "&apos;");
		val = StringUtil.replace(val, "’", "&apos;");
		val = StringUtil.replace(val, "´", "&apos;");
		val = StringUtil.replace(val, "\"", "&quot;");
		val = StringUtil.replace(val, ">", "&gt;");
		val = StringUtil.replace(val, "<", "&lt;");
		val = StringUtil.replace(val, "ï", "&#239;");
		val = StringUtil.replace(val, "Ï", "&#207;");
		val = StringUtil.replace(val, "¿", "&#191;");
		val = StringUtil.replace(val, "¼", "&#188;");
		val = StringUtil.replace(val, "½", "&#189;");
		val = StringUtil.replace(val, "ã", "&#227;");
		val = StringUtil.replace(val, "©", "&#169;");
		val = StringUtil.replace(val, "£", "&#163;");
		val = StringUtil.replace(val, "‰", "");
		val = StringUtil.replace(val, "¡", "");
		val = StringUtil.replace(val, "¨","");
		val = StringUtil.replace(val, "º", "&#176;");
		val = StringUtil.replace(val, "°", "&#186;");
		val = StringUtil.replace(val, "â", "&#226;");
		val = StringUtil.replace(val, "Â", "&#194;");
		val = StringUtil.replace(val, "³", "&#179;");
		val = StringUtil.replace(val, "§", "&#167;");
		val = StringUtil.replace(val, "á", "&#225;");
		val = StringUtil.replace(val, "é", "&#233;");
		val = StringUtil.replace(val, "í", "&#237;");
		val = StringUtil.replace(val, "ó", "&#243;");
		val = StringUtil.replace(val, "ú", "&#250;");
		return val;
	}
	
	/**
	 * Formats sendDate based on language format
	 * @param format
	 * @return
	 */
	private String formatSendDate(String format) {
		StringBuilder sendDate = new StringBuilder();
		Calendar cal = GregorianCalendar.getInstance();
		if (format.equalsIgnoreCase("spanish")) {
			sendDate.append(cal.get(Calendar.DAY_OF_MONTH));
			sendDate.append(" de ");
			sendDate.append(cal.getDisplayName(Calendar.MONTH,2,new Locale("ES")));
			sendDate.append(" de ");
			sendDate.append(cal.get(Calendar.YEAR));
		} else {
			sendDate.append(cal.getDisplayName(Calendar.MONTH,2,Locale.US));
			sendDate.append(" ").append(cal.get(Calendar.DAY_OF_MONTH));
			sendDate.append(", ").append(cal.get(Calendar.YEAR));
		}
		return sendDate.toString();
	}
	
	/**
	 * @return the englishLetter
	 */
	public StringBuilder getEnglishLetter() {
		return englishLetter;
	}

	/**
	 * @param englishLetter the englishLetter to set
	 */
	public void setEnglishLetter(StringBuilder englishLetter) {
		this.englishLetter = englishLetter;
	}

	/**
	 * @return the spanishLetter
	 */
	public StringBuilder getSpanishLetter() {
		return spanishLetter;
	}

	/**
	 * @param spanishLetter the spanishLetter to set
	 */
	public void setSpanishLetter(StringBuilder spanishLetter) {
		this.spanishLetter = spanishLetter;
	}
	
	/**
	 * @return the firstImageHeader
	 */
	public StringBuilder getFirstImageHeader() {
		return firstImageHeader;
	}

	/**
	 * @param firstImageHeader  the firstImageHeader to set
	 */
	public void setFirstImageHeader(StringBuilder firstImageHeader) {
		this.firstImageHeader = firstImageHeader;
	}
	
		/**
	 * @return the secondImageHeader
	 */
	public StringBuilder getSecondImageHeader() {
		return secondImageHeader;
	}

	/**
	 * @param secondImageHeader the secondImageHeader to set
	 */
	public void setSecondImageHeader(StringBuilder secondImageHeader) {
		this.secondImageHeader = secondImageHeader;
	}

}
