package com.depuysynthes.srt.vo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/****************************************************************************
 * <b>Title:</b> ProjectExportReportVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Formats necessary data for the SRTProjectExportReportVO
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Apr 10, 2018
 ****************************************************************************/
public class ProjectExportReportVO {

	//Holds Column Name and Index for easier refactoring in future.
		private enum HeaderEnum {
			//PROJECT Values
			//PROJECT_ID("Project Id"),
			CO_PROJECT_ID("OPCo Project Id"),
			OP_CO_ID("OpCo"),
			PROJECT_NM("Project Name"),
			PROJECT_DESC("Project Desc."),
			PROJECT_TYPE("Project Type"),
			PRIORITY("Priority"),
			HOSPITAL_PO("Hospital PO"),
			SPECIAL_INSTRUC("Special Instructions"),
			PROJECT_STAT("Project Status"),
			ACTUALROI("Actual ROI"),
			SRT_CONTACT("SRT Contact"),
			ENGINEER("Engineer"),
			SEC_ENGINEER("Secondary Engineer"),
			DESIGNER("Designer"),
			SEC_DESGINER("Secondary Designer"),
			QA("Quality Engineer"),
			SEC_QA("Secondary Quality Engineer"),
			FROM_SCRATCH("From Scratch"),
			FUNCT_CHECK("Functional Check Order No"),
			MAKE_FROM("Make From Order No"),
			BUYER("Buyer"),
			SEC_BUYER("Secondary Buyer"),
			MFG_PO("Manufacturing PO To Vendor"),
			SUPPLIER_ID("Supplier/Vendor"),
			PROEJCT_HOLD("Project On Hold"),
			PROJECT_CANCELLED("Project Cancelled"),
			WAREHOUSE_TRACKING("Warehouse Tracking No"),
			MFG_DT_CHANGE_REASON("Manufacturing Date Change Reason"),
			WAREHOUSE_SALES_NO("Warehouse Sales No"),

			//REQUEST Values
			REQUEST_ID("Request Id"),
			HOSPITAL_NM("Hospital Name"),
			SURGEON_NM("Surgeon Name"),
			SALES_CONSULT_NM("Sales Consultant Name"),
			AREA("Area Name"),
			REGION("Region Name"),
			DESCRIPTION("Request Description"),
			TERRITORY_ID("Territory Id"),
			ESTIMATED_ROI("Estimated ROI"),
			REQ_QTY_NO("Requested Qty No"),
			REQ_REASON("Request Reason"),
			REQ_REASON_TXT("Requestor Reason"),
			CHARGE_TO("Charge To"),
			REQUEST_DT("Request Date"),

			//REQUEST ADDR Values
			ADDRESS_1("Address 1"),
			ADDRESS_2("Address 2"),
			CITY("City"),
			STATE("State"),
			ZIP("Zip"),

			//MASTER Record Values
			MASTER_RECORD_ID("Master Record Id"),
			PART_NO("Part No"),
			TITLE_TXT("Master Record Title Text"),
			QUALITY_SYSTEM("Quality System"),
			PROD_TYPE("Product Type"),
			COMPLEXITY("Complexity"),
			PROD_CAT("Product Category"),
			MAKE_FROM_PART_NO("Make From Part No"),
			PROD_FAMILY("Product Family"),
			PART_COUNT("Part Count"),
			TOTAL_BUILD("Total Built"),
			OBSOLETE("Is Obsolete"),
			OBSOLETE_REASON("Obsolete Reason");

			private String colName;
			private HeaderEnum(String colName) {
				this.colName = colName;
			}

			/**
			 * @return
			 */
			public String getColName() {
				return colName;
			}
		}

	private Map<String, SRTProjectVO> projects;
	private Map<Integer, String> headers;

	public ProjectExportReportVO(Map<String, SRTProjectVO> projects, List<String> mrAttrs, List<SRTProjectMilestoneVO> milestones) {
		this.projects = projects;
		this.buildHeaderMap(mrAttrs, milestones);
	}

	/**
	 * Build the report header Map.
	 * @param masterRecordAttributes
	 * @param milestones
	 */
	private void buildHeaderMap(List<String> mrAttrs, List<SRTProjectMilestoneVO> milestones) {
		headers = new LinkedHashMap<>();
		int i = 0;
		for(HeaderEnum h : HeaderEnum.values()) {
			headers.put(i++, h.getColName());
		}

		for(String attr : mrAttrs) {
			headers.put(i++, attr);
		}

		for(SRTProjectMilestoneVO m : milestones) {
			headers.put(i++, m.getMilestoneNm());
		}
	}

	/**
	 * Return the list of Projects
	 * @return
	 */
	public Map<String, SRTProjectVO> getProjects() {
		return projects;
	}

	/**
	 * Return the Map of Headers.
	 * @return
	 */
	public Map<Integer, String> getHeaders() {
		return headers;
	}
}