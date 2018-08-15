package com.depuysynthes.srt.vo;

import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.util.EnumUtil;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> SRTSolrUIVO.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> Stores Solr UI Config for building an advanced search
 * on the front end.
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 26, 2018
 ****************************************************************************/
@Table(name="DPY_SYN_SRT_SOLR_UI_CONFIG")
public class SRTSolrUIVO extends BeanDataVO {

	/**
	 *
	 */
	private static final long serialVersionUID = 1L;

	public enum FieldType {TEXT, SELECT, DATE}
	public enum OperationType {AND, OR, EQUALS, BETWEEN}
	public enum SearchType {PROJECT, MASTER_RECORD}

	private String solrUIConfigId;
	private SearchType searchType;
	private String solrFieldId;
	private String labelTxt;
	private FieldType fieldType;
	private String optionsSrc;
	private String opCoId;
	private int isRange;
	private List<OperationType> operations;
	private Date createDt;
	private Date updateDt;

	public SRTSolrUIVO() {
		super();
		operations = new ArrayList<>();
	}

	public SRTSolrUIVO(ResultSet rs) {
		super(rs);
	}

	public SRTSolrUIVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @return the solrUIConfigId
	 */
	@Column(name="SOLR_UI_CONFIG_ID", isPrimaryKey=true)
	public String getSolrUIConfigId() {
		return solrUIConfigId;
	}

	/**
	 * @return the searchType
	 */
	@Column(name="SEARCH_TYPE")
	public SearchType getSearchType() {
		return searchType;
	}

	/**
	 * @return the solrFieldId
	 */
	@Column(name="SOLR_FIELD_ID")
	public String getSolrFieldId() {
		return solrFieldId;
	}

	/**
	 * @return the labelTxt
	 */
	@Column(name="LABEL_TXT")
	public String getLabelTxt() {
		return labelTxt;
	}

	/**
	 * @return the fieldType
	 */
	@Column(name="FIELD_TYPE")
	public FieldType getFieldType() {
		return fieldType;
	}

	/**
	 * @return the optionsSrc
	 */
	@Column(name="OPTIONS_SRC")
	public String getOptionsSrc() {
		return optionsSrc;
	}

	/**
	 * @return the isRange
	 */
	@Column(name="IS_RANGE")
	public int isRange() {
		return isRange;
	}

	/**
	 * @return the createDt
	 */
	@Column(name="CREATE_DT", isAutoGen=true, isInsertOnly=true)
	public Date getCreateDt() {
		return createDt;
	}

	/**
	 * @return the updateDt
	 */
	@Column(name="UPDATE_DT", isAutoGen=true, isUpdateOnly=true)
	public Date getUpdateDt() {
		return updateDt;
	}

	public String getOpCoId() {
		return opCoId;
	}

	/**
	 * @return the operations
	 */
	public List<OperationType> getOperations() {
		return operations;
	}

	/**
	 * @return String comma delimited representation of the operations List.
	 */
	@Column(name="OPERATIONS")
	public String getOperationsTxt() {
		return operations.stream().map (OperationType::name).collect (Collectors.joining (","));
	}

	/**
	 * @param solrUIConfigId the solrUIConfigId to set.
	 */
	public void setSolrUIConfigId(String solrUIConfigId) {
		this.solrUIConfigId = solrUIConfigId;
	}

	/**
	 * @param searchType the searchType to set.
	 */
	public void setSearchType(SearchType searchType) {
		this.searchType = searchType;
	}

	/**
	 * @param solrFieldId the solrFieldId to set.
	 */
	public void setSolrFieldId(String solrFieldId) {
		this.solrFieldId = solrFieldId;
	}

	/**
	 * @param labelTxt the labelTxt to set.
	 */
	public void setLabelTxt(String labelTxt) {
		this.labelTxt = labelTxt;
	}

	/**
	 * @param fieldType the fieldType to set.
	 */
	public void setFieldType(FieldType fieldType) {
		this.fieldType = fieldType;
	}

	/**
	 * @param optionsSrc the optionsSrc to set.
	 */
	public void setOptionsSrc(String optionsSrc) {
		this.optionsSrc = optionsSrc;
	}

	/**
	 * @param isRange the isRange to set.
	 */
	public void setRange(int isRange) {
		this.isRange = isRange;
	}

	/**
	 * @param operations the operations to set.
	 */
	public void setOperations(List<OperationType> operations) {
		this.operations = operations;
	}

	/**
	 * @param operationsTxt
	 */
	public void setOperationsTxt(String operationsTxt) {
		this.operations = Arrays.asList(StringUtil.checkVal(operationsTxt)
							.split(","))
							.stream()
							.map(o -> EnumUtil.safeValueOf(OperationType.class, o, null))
							.filter(Objects::nonNull)
							.collect(Collectors.toList());
	}

	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}

	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	/**
	 * @param opCoId
	 * @return
	 */
	public void setOpCoId(String opCoId) {
		this.opCoId = opCoId;
	}
}