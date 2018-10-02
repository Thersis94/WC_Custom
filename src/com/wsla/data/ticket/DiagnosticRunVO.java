package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: DiagnosticRunVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> Data object for the diagnostic run table
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 2, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_diagnostic_run")
public class DiagnosticRunVO extends BeanDataVO {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5505851256156607271L;
	
	// Member Variables
	private String diagnosticRunId;
	private String ticketAssignmentID;
	private String ticketId;
	private String dispositionedBy;
	private String comments;
	private Date createDate;
	private Date updateDate;
	
	// Bean Sub-elements
	private List<DiagnosticTicketVO> diagnostics = new ArrayList<>(16);
	
	/**
	 * 
	 */
	public DiagnosticRunVO() {
		super();
	}

	/**
	 * @param req
	 */
	public DiagnosticRunVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public DiagnosticRunVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the diagnosticRunId
	 */
	@Column(name="diagnostic_run_id", isPrimaryKey=true)
	public String getDiagnosticRunId() {
		return diagnosticRunId;
	}

	/**
	 * @return the ticketAssignmentID
	 */
	@Column(name="ticket_assig_id")
	public String getTicketAssignmentID() {
		return ticketAssignmentID;
	}

	/**
	 * @return the ticketId
	 */
	@Column(name="ticket_id")
	public String getTicketId() {
		return ticketId;
	}

	/**
	 * @return the dispositionedBy
	 */
	@Column(name="dispositioned_by_id")
	public String getDispositionedBy() {
		return dispositionedBy;
	}

	/**
	 * @return the comments
	 */
	@Column(name="diag_comment_txt")
	public String getComments() {
		return comments;
	}

	/**
	 * @return the createDate
	 */
	@Column(name="create_dt", isInsertOnly=true, isAutoGen=true)
	public Date getCreateDate() {
		return createDate;
	}

	/**
	 * @return the updateDate
	 */
	@Column(name="update_dt", isUpdateOnly=true, isAutoGen=true)
	public Date getUpdateDate() {
		return updateDate;
	}

	/**
	 * @return the diagnostics
	 */
	public List<DiagnosticTicketVO> getDiagnostics() {
		return diagnostics;
	}

	/**
	 * @param diagnosticRunId the diagnosticRunId to set
	 */
	public void setDiagnosticRunId(String diagnosticRunId) {
		this.diagnosticRunId = diagnosticRunId;
	}

	/**
	 * @param ticketAssignmentID the ticketAssignmentID to set
	 */
	public void setTicketAssignmentID(String ticketAssignmentID) {
		this.ticketAssignmentID = ticketAssignmentID;
	}

	/**
	 * @param ticketId the ticketId to set
	 */
	public void setTicketId(String ticketId) {
		this.ticketId = ticketId;
	}

	/**
	 * @param dispositionedBy the dispositionedBy to set
	 */
	public void setDispositionedBy(String dispositionedBy) {
		this.dispositionedBy = dispositionedBy;
	}

	/**
	 * @param comments the comments to set
	 */
	public void setComments(String comments) {
		this.comments = comments;
	}

	/**
	 * @param createDate the createDate to set
	 */
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}

	/**
	 * @param updateDate the updateDate to set
	 */
	public void setUpdateDate(Date updateDate) {
		this.updateDate = updateDate;
	}

	/**
	 * @param diagnostics the diagnostics to set
	 */
	public void setDiagnostics(List<DiagnosticTicketVO> diagnostics) {
		this.diagnostics = diagnostics;
	}

	/**
	 * Adds a single diagnostic to the list
	 * @param diagnostic
	 */
	@BeanSubElement
	public void addDiagnostic(DiagnosticTicketVO diagnostic) {
		if (diagnostic != null)
			diagnostics.add(diagnostic);
	}
}

