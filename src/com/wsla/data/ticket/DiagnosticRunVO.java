package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.data.parser.BeanDataVO;
import com.siliconmtn.db.orm.BeanSubElement;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;
import com.siliconmtn.security.UserDataVO;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;

// WC Libs
import com.smt.sitebuilder.common.constants.Constants;
import com.wsla.data.ticket.DiagnosticTicketVO.Outcome;

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
	private String ticketAssignmentId;
	private String ticketId;
	private String dispositionedBy;
	private String diagComments;
	private Date createDate;
	private Date updateDate;
	
	// Bean Sub-elements
	private List<DiagnosticTicketVO> diagnostics = new ArrayList<>(16);
	private UserVO dispositioned;
	
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
		assignDiagnostics(req);
	}

	/**
	 * @param rs
	 */
	public DiagnosticRunVO(ResultSet rs) {
		super(rs);
	}
	
	/**
	 * Since the diagnostics are in a grid of radio and checkboxes, we need to process 
	 * the data a little bit differently from the request
	 * @param req
	 */
	protected void assignDiagnostics(ActionRequest req) {
		// Assign the user who is submitting the request
		UserDataVO userData = ((UserDataVO) req.getSession().getAttribute(Constants.USER_DATA));
		dispositionedBy = ((UserVO)userData.getUserExtendedInfo()).getUserId();
		
		// Since the runs will be insert only, assign the uuid so it can be 
		// assigned to the dt 
		this.setDiagnosticRunId(new UUIDGenerator().getUUID());
		
		// Loop the request and assign the diagnostics
		for (String name : Collections.list(req.getParameterNames())) {
			if (StringUtil.checkVal(name).startsWith("diag_") && ! StringUtil.checkVal(name).endsWith("_resolved")) {
				DiagnosticTicketVO dtv = new DiagnosticTicketVO();
				dtv.setDiagnosticTicketId(new UUIDGenerator().getUUID());
				dtv.setDiagnosticRunId(diagnosticRunId);
				dtv.setDiagnosticCode(name.substring(5));
				dtv.setOutcome(Outcome.valueOf(req.getStringParameter(name, "NA")));
				dtv.setIssueResolvedFlag(req.getIntegerParameter(name + "_resolved", 0));
				this.addDiagnostic(dtv);
			}
		}
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
	public String getTicketAssignmentId() {
		return ticketAssignmentId;
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
	public String getDiagComments() {
		return diagComments;
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
	 * @return the dispositioned
	 */
	public UserVO getDispositioned() {
		return dispositioned;
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
	public void setTicketAssignmentId(String ticketAssignmentId) {
		this.ticketAssignmentId = ticketAssignmentId;
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
	public void setDiagComments(String diagComments) {
		this.diagComments = diagComments;
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
	
	/**
	 * @param dispositioned the dispositioned to set
	 */
	@BeanSubElement
	public void setDispositioned(UserVO dispositioned) {
		this.dispositioned = dispositioned;
	}
}

