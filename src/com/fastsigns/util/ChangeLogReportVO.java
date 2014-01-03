package com.fastsigns.util;

import java.util.Iterator;
import java.util.List;

import com.fastsigns.action.approval.vo.AbstractChangeLogVO;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.action.AbstractSBReportVO;

public class ChangeLogReportVO extends AbstractSBReportVO{
    private List<?> dataSet = null;
    private static final long serialVersionUID = 1l;

    public ChangeLogReportVO(){
		super();
		setContentType("appliation/vnd.ms-excel");
		isHeaderAttachment(Boolean.TRUE);
		setFileName("ChangeLogReport-" + Convert.formatDate(Convert.getCurrentTimestamp()) + ".xls");
	}
    /**
     * Generates a byte [] report in html table form.
     */
	public byte[] generateReport() {
		log.debug("starting generateReport()");
		if(dataSet.size() > 0){
		StringBuffer rpt = new StringBuffer(this.getHeader());
		Iterator<?> iter = dataSet.iterator();
		AbstractChangeLogVO vo = null;
		while (iter.hasNext()) {
			vo = (AbstractChangeLogVO) iter.next();
			rpt.append(this.buildRow(vo));
		}
		
		rpt.append(this.getFooter());
		return rpt.toString().getBytes();
		}
		else return new StringBuffer("<table border='0'>\r<tr>\r<td>No Pending Requests</td>\r</tr>\r</table>").toString().getBytes();
	}
	/**
	 * Returns the headers for the report.
	 * @return
	 */
	private StringBuffer getHeader(){
		StringBuffer hdr = new StringBuffer();
		hdr.append("<table border='1'>\r<tr style='background-color:#ccc;'>");
		hdr.append("<th>Franchise Id</th>");
		hdr.append("<th>Module Name</th>");
		hdr.append("<th>Module Type</th>");
		hdr.append("<th>Submitted By</th>");
		hdr.append("<th>Submitted On</th>");
		hdr.append("<th>Updated On</th>");
		hdr.append("<th>Description</th></tr>\r");
		return hdr;
	}
	/**
	 * Takes in a changelog and returns a Row for the table report.
	 * @param vo
	 * @return
	 */
	private StringBuffer buildRow(AbstractChangeLogVO vo){
		StringBuffer rb = new StringBuffer();
		rb.append("<tr>\r<td>").append(StringUtil.checkVal(vo.getFranchiseId())).append("</td>\r");
		rb.append("<td>").append(StringUtil.checkVal(vo.getModName())).append("</td>\r");
		rb.append("<td>").append(StringUtil.checkVal(vo.getHFriendlyType())).append("</td>\r");
		rb.append("<td>").append(StringUtil.checkVal(vo.getSubmitterName())).append("</td>\r");
		rb.append("<td>").append(Convert.formatDate(vo.getSubmittedDate(), "MM/dd/yyyy HH:mm a z")).append("</td>\r");
		rb.append("<td>").append(Convert.formatDate(vo.getUpdateDate(), "MM/dd/yyyy HH:mm a z")).append("</td>\r");
		if(vo.getDescTxt() != null)
		rb.append("<td>").append(StringUtil.checkVal(vo.getDescTxt())).append("</td>\r</tr>");
		else
			rb.append("<td>").append(StringUtil.checkVal(vo.getModDescTxt())).append("</td>\r</tr>");
		//System.out.println("Building a row");
		return rb;
	}
	/**
	 * Adds any extra information needed below data rows and closes out table.
	 * @return
	 */
	private StringBuffer getFooter(){
		return new StringBuffer("</table>");
	}
	/**
	 * Sets the dataset the report represents.
	 */
	public void setData(Object o) {
    	this.dataSet = (List<?>) o;
	}
	
	public int getCount(){
		return dataSet.size();
	}
	
}
