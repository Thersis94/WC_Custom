package com.ansmed.sb.report;

//JDK 1.6.0
import java.text.NumberFormat;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

// SB Libs
import com.smt.sitebuilder.action.AbstractSBReportVO;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;

// SB ANS Libs
import com.ansmed.sb.physician.ActualsVO;

/****************************************************************************
 * <p><b>Title</b>:RevenueReport.java<p/>
 * <p><b>Description: </b> 
 * <p><p/>
 * <p><b>Copyright:</b> Copyright (c) 2009<p/>
 * <p><b>Company:</b> Silicon Mountain Technologies<p/>
 * @author Dave Bargerhuff
 * @version 1.0
 * @since Apr 28, 2009
 * <b>Changes: </b>
 ****************************************************************************/
public class RevenueReport extends AbstractSBReportVO {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Collection<RevenueActualVO> data = null;
	private boolean useRawData = false;
	
	/**
	 * 
	 */
	public RevenueReport() {
        super();		
	}
	
	public RevenueReport(ActionRequest req) {
		super();
		this.useRawData = Convert.formatBoolean(req.getParameter("useRawData")).booleanValue();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#generateReport()
	 */
	@Override
	public byte[] generateReport() {
		
		StringBuffer sb = new StringBuffer();
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setGroupingUsed(true);
		nf.setMaximumFractionDigits(0);
		if (data != null && data.size() > 0) {
			log.debug("useRawData boolean value: " + useRawData);
			// if 'raw data' sort
			if (useRawData) {
				sb = createRawDataReport(data);				
			} else {
				sb = createStandardReport(data);
			}
		} else {
			sb.append("<table border=\"1\">");
			// Header line 1
			sb.append("<tr>");
			sb.append("<td colspan=\"7\">&nbsp;</td>");
			sb.append("<td colspan=\"4\">Trials</td>");
			sb.append("<td colspan=\"4\">Perms</td>");
			sb.append("</tr>");
			// Header line 2
			sb.append("<tr>");
			sb.append("<td>TM Name</td>");
			sb.append("<td>Physician First Name</td>");
			sb.append("<td>Physician Last Name</td>");
			sb.append("<td>Title</td>");
			sb.append("<td>Rank</td>");
			sb.append("<td>Period</td>");
			sb.append("<td>Revenue</td>");
			sb.append("<td>Q1</td>");
			sb.append("<td>Q2</td>");
			sb.append("<td>Q3</td>");
			sb.append("<td>Q4</td>");
			sb.append("<td>Q1</td>");
			sb.append("<td>Q2</td>");
			sb.append("<td>Q3</td>");
			sb.append("<td>Q4</td>");
			sb.append("</tr>");
			//Body
			sb.append("<tr>");
			sb.append("<td colspan=\"15\">").append("No revenue summary information found.").append("</td>");
			sb.append("</tr>");
			sb.append("</table");
		}
		
		return sb.toString().getBytes();
	}

	/* (non-Javadoc)
	 * @see com.smt.sitebuilder.action.AbstractSBReportVO#setData(java.lang.Object)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void setData(Object info) {
		if (info instanceof Collection<?>)
			data = (Collection<RevenueActualVO>)info;

	}
	
	/*
	 * For future use...
	 */
	public void setActuals(Map<String,ActualsVO> actuals) {
		
	}
	
	@Override
	public void setFileName(String f) {
		super.setFileName(f);
		if (f.endsWith(".xls")) {
	        setContentType("application/vnd.ms-excel");
	        isHeaderAttachment(Boolean.TRUE);
		} else {
			setContentType("text/html");
		}
	}
	
	/**
	 * Creates report in 'raw data' format.
	 * @param data
	 * @return
	 */
	public StringBuffer createRawDataReport(Collection<RevenueActualVO> data) {
		StringBuffer sb = new StringBuffer();
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setGroupingUsed(true);
		nf.setMaximumFractionDigits(0);

		sb.append("<table border=\"1\">");
		// Header line
		sb.append("<tr style=\"height: 22px;\">");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">TM Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Physician First Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Physician Last Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Title</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Rank</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Period</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Actual Revenue</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Trials Q1</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Trials Q2</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Trials Q3</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Trials Q4</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Perms Q1</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Perms Q2</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Perms Q3</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Perms Q4</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Period</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Forecast Revenue</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Trials Q1</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Trials Q2</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Trials Q3</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Trials Q4</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Perms Q1</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Perms Q2</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Perms Q3</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Perms Q4</td>");
		sb.append("</tr>");
		
		for (Iterator<RevenueActualVO> iter = data.iterator(); iter.hasNext(); ) {
			RevenueActualVO vo = iter.next();
			sb.append("<tr style=\"height: 21px;\">");
			sb.append("<td style=\"vertical-align: top; text-align: center;\">").append(vo.getRepFirstName());
			sb.append("&nbsp;").append(vo.getRepLastName()).append("</td>");
			sb.append("<td style=\"vertical-align: top; text-align: center;\">").append(vo.getSurgeonFirstName()).append("</td>");
			sb.append("<td style=\"vertical-align: top; text-align: center;\">").append(vo.getSurgeonLastName()).append("</td>");
			sb.append("<td style=\"vertical-align: top; text-align: center;\">").append(vo.getTitleNm()).append("</td>");
			sb.append("<td style=\"vertical-align: top; text-align: center;\">").append(vo.getRank()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append("2008 Actual</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(nf.format(vo.getActualsData().getDollars())).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ1Trials()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ2Trials()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ3Trials()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ4Trials()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ1Perms()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ2Perms()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ3Perms()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ4Perms()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append("2009 Forecast</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(nf.format(vo.getForecastDollars())).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getTrialQ1()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getTrialQ2()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getTrialQ3()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getTrialQ4()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getPermQ1()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getPermQ2()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getPermQ3()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getPermQ4()).append("</td>");
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		
		return sb;
	}
	
	/**
	 * Creates report in standard format
	 * @param data
	 * @return
	 */
	public StringBuffer createStandardReport(Collection<RevenueActualVO> data) {
		StringBuffer sb = new StringBuffer();
		NumberFormat nf = NumberFormat.getIntegerInstance();
		nf.setGroupingUsed(true);
		nf.setMaximumFractionDigits(0);

		sb.append("<table border=\"1\">");
		// Header line 1
		sb.append("<tr style=\"height: 22px;\">");
		sb.append("<td colspan=\"7\" style=\"text-align: center; font-weight: bold;\">&nbsp;</td>");
		sb.append("<td colspan=\"4\" style=\"width: 160px; text-align: center; font-weight: bold; border-left-style: solid; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Trials</td>");
		sb.append("<td colspan=\"4\" style=\"width: 160px; text-align: center; font-weight: bold; border-left-style: solid; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Perms</td>");
		sb.append("</tr>");
		// Header line 2
		sb.append("<tr style=\"height: 30px;\">");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">TM Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Physician First Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Physician Last Name</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Title</td>");
		sb.append("<td style=\"text-align: center; font-weight: bold;\">Rank</td>");
		sb.append("<td style=\"width: 120px; text-align: center; font-weight: bold; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Period</td>");
		sb.append("<td style=\"width: 120px; text-align: center; font-weight: bold; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Revenue</td>");
		sb.append("<td style=\"width: 40px; text-align: center; font-weight: bold; border-left-style: solid; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Q1</td>");
		sb.append("<td style=\"width: 40px; text-align: center; font-weight: bold; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Q2</td>");
		sb.append("<td style=\"width: 40px; text-align: center; font-weight: bold; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Q3</td>");
		sb.append("<td style=\"width: 40px; text-align: center; font-weight: bold; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Q4</td>");
		sb.append("<td style=\"width: 40px; text-align: center; font-weight: bold; border-left-style: solid; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Q1</td>");
		sb.append("<td style=\"width: 40px; text-align: center; font-weight: bold; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Q2</td>");
		sb.append("<td style=\"width: 40px; text-align: center; font-weight: bold; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Q3</td>");
		sb.append("<td style=\"width: 40px; text-align: center; font-weight: bold; border-bottom-style: solid; border-width: 1px; border-color: #000000;\">Q4</td>");
		sb.append("</tr>");
		
		for (Iterator<RevenueActualVO> iter = data.iterator(); iter.hasNext(); ) {
			RevenueActualVO vo = iter.next();
			sb.append("<tr style=\"height: 21px;\">");
			sb.append("<td rowspan=\"2\" style=\"vertical-align: top; text-align: center;\">").append(vo.getRepFirstName());
			sb.append("&nbsp;").append(vo.getRepLastName()).append("</td>");
			sb.append("<td rowspan=\"2\" style=\"vertical-align: top; text-align: center;\">").append(vo.getSurgeonFirstName()).append("</td>");
			sb.append("<td rowspan=\"2\" style=\"vertical-align: top; text-align: center;\">").append(vo.getSurgeonLastName()).append("</td>");
			sb.append("<td rowspan=\"2\" style=\"vertical-align: top; text-align: center;\">").append(vo.getTitleNm()).append("</td>");
			sb.append("<td rowspan=\"2\" style=\"vertical-align: top; text-align: center;\">").append(vo.getRank()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append("2008 Actual</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(nf.format(vo.getActualsData().getDollars())).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ1Trials()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ2Trials()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ3Trials()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ4Trials()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ1Perms()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ2Perms()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ3Perms()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #ccccff;\">").append(vo.getActualsData().getQ4Perms()).append("</td>");
			sb.append("</tr>");
			sb.append("<tr style=\"height: 21px;\">");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append("2009 Forecast</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(nf.format(vo.getForecastDollars())).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getTrialQ1()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getTrialQ2()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getTrialQ3()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getTrialQ4()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getPermQ1()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getPermQ2()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getPermQ3()).append("</td>");
			sb.append("<td style=\"text-align: center; background-color: #00ff00;\">").append(vo.getPermQ4()).append("</td>");
			sb.append("</tr>");
		}
		
		sb.append("</table>");
		
		return sb;
	}

}