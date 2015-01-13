/**
 * 
 */
package com.depuysynthes.pa;

import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;
import com.smt.sitebuilder.data.DataContainer;
import com.smt.sitebuilder.data.vo.FormTransactionVO;

/****************************************************************************
 * <b>Title</b>: PatientAmbassadorReportVO.java
 * <p/>
 * <b>Project</b>: WC_Custom
 * <p/>
 * <b>Description: </b> Report VO Used to export Patient Story Records from the
 * PatientAmbassadorStory Data Tool.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015
 * <p/>
 * <b>Company:</b> Silicon Mountain Technologies
 * <p/>
 * 
 * @author raptor
 * @version 1.0
 * @since Jan 12, 2015
 *        <p/>
 *        <b>Changes: </b>
 ****************************************************************************/
public class PatientAmbassadorReportVO extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private DataContainer dc = null;
	private String siteUrl = null;
	/**
	 * 
	 */
	public PatientAmbassadorReportVO() {
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		StringBuffer sb = new StringBuffer();

		// Build the headers
		sb.append("<table border=\"1\">");
		sb.append("<tr><th nowrap>Author Name</th>");
		sb.append("<th nowrap>Email</th>");
		sb.append("<th nowrap>City</th>");
		sb.append("<th nowrap>State</th>");
		sb.append("<th nowrap>ZipCode</th>");
		sb.append("<th nowrap>ImageUrl</th>");
		sb.append("<th nowrap>Joints</th>");
		sb.append("<th nowrap>Hobbies</th>");
		sb.append("<th nowrap>Have a replacement?</th>");
		sb.append("<th nowrap>Life Before?</th>");
		sb.append("<th nowrap>Turning Point</th>");
		sb.append("<th nowrap>Life After</th>");
		sb.append("<th nowrap>Advice for others</th>");
		sb.append("</tr>\r");
		
		//If there are no results then Say so.
		if(dc.getTransactions().size() == 0) {
			sb.append("<tr><td>No Results Found</td></tr>");
		}
		
		//Loop over transactions and print data appropriately.
		for(FormTransactionVO vo : dc.getTransactions().values()) {
			
			//Add Profile Data
			sb.append("<tr><td>").append(vo.getFirstName()).append(" ").append(vo.getLastName()).append("</td>");
			sb.append("<td>").append(vo.getEmailAddress()).append("</td>");
			sb.append("<td>").append(vo.getCity()).append("</td>");
			sb.append("<td>").append(vo.getState()).append("</td>");
			sb.append("<td>").append(vo.getZipCode()).append("</td>");
			
			//Set Image Url
			sb.append("<td>").append(siteUrl).append(vo.getFieldById("c0a80241bbb8c55aae6fabe3fe143767").getResponses().get(0)).append("</td>");
			
			//Set Joints
			sb.append("<td>");
			int i = 0;
			for(String s : vo.getFieldById("c0a80241bba73b0a49493776bd9f999d").getResponses()) {
				if(i > 0) sb.append(", ");
				sb.append(s);
				i++;
			}
			sb.append("</td>");
			i = 0;
			
			//Set Hobbies
			sb.append("<td>");
			for(String s : vo.getFieldById("c0a80241bba4e916f3c24b11c6d6c26f").getResponses()) {
				if(i > 0) sb.append(", ");
				//Skip other, we'll add it later.
				if(!s.equals("OTHER")) {
					sb.append(s);
					i++;
				}
			}
			
			//Add Other Hobby if present.
			if(vo.getFieldById("c0a80241bf9cfab2648d4393cf3bb062").getResponses().size() > 0 && StringUtil.checkVal(vo.getFieldById("c0a80241bf9cfab2648d4393cf3bb062").getResponses().get(0)).length() > 0) {
				if(i > 0) sb.append(", ");
				sb.append(vo.getFieldById("c0a80241bf9cfab2648d4393cf3bb062").getResponses().get(0));
			}
			sb.append("</td>");
			
			//Add Has had Replacement
			sb.append("<td>").append(vo.getFieldById("c0a80241bba861705b540c2e91d3bf6a").getResponses().get(0)).append("</td>");
			
			//Add Life Before
			sb.append("<td>").append(vo.getFieldById("c0a80241bbaa0d063448036ce9a37a9d").getResponses().get(0)).append("</td>");

			//Add Turning Point
			sb.append("<td>").append(vo.getFieldById("c0a80241bbaaa185cd2c542570a03b69").getResponses().get(0)).append("</td>");

			//Add Life After
			sb.append("<td>").append(vo.getFieldById("c0a80241bbab26d391814dedd1b1857d").getResponses().get(0)).append("</td>");

			//Add Advice
			sb.append("<td>").append(vo.getFieldById("c0a80241bbb2d50c11b6f3652f008aa6").getResponses().get(0)).append("</td>");

			//Close out the Transaction Row.
			sb.append("</tr>");
		}

		return sb.toString().getBytes();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.data.report.AbstractReport#setData(java.lang.Object)
	 */
	@Override
	public void setData(Object o) {
		dc = (DataContainer) o;
	}
	
	//Set the SiteUrl for image pathing.
	public void setSiteUrl(String siteUrl) {
		this.siteUrl = siteUrl;
	}

}
