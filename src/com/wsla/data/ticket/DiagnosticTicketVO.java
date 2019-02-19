package com.wsla.data.ticket;

// JDK 1.8.x
import java.sql.ResultSet;
import java.util.Date;

// SMT Base Libs
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.db.orm.Column;
import com.siliconmtn.db.orm.Table;

/****************************************************************************
 * <b>Title</b>: DiagnosticTicketVO.java
 * <b>Project</b>: WC_Custom
 * <b>Description: </b> identifies an instance of the diagnostic being run on a ticket
 * <b>Copyright:</b> Copyright (c) 2018
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author James Camire
 * @version 3.0
 * @since Oct 2, 2018
 * @updates:
 ****************************************************************************/
@Table(name="wsla_diagnostic_xr")
public class DiagnosticTicketVO extends DiagnosticVO {
	
	/**
	 * Outcome codes
	 */
	public enum Outcome {
		PASS, FAIL, NA
	}
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -7402330424028394756L;
	
	// Member variables
	private String diagnosticTicketId;
	private String diagnosticRunId;
	private Outcome outcome;
	private int issueResolvedFlag;
	/**
	 * 
	 */
	public DiagnosticTicketVO() {
		super();
	}

	/**
	 * @param req
	 */
	public DiagnosticTicketVO(ActionRequest req) {
		super(req);
	}

	/**
	 * @param rs
	 */
	public DiagnosticTicketVO(ResultSet rs) {
		super(rs);
	}

	/**
	 * @return the diagnosticTicketId
	 */
	@Column(name="diagnostic_xr_id", isPrimaryKey=true)
	public String getDiagnosticTicketId() {
		return diagnosticTicketId;
	}

	/*
	 * (non-Javadoc)
	 * Added this to overwrite the primary key annotation in the base class
	 * @see com.wsla.data.ticket.DiagnosticVO#getDiagnosticCode()
	 */
	@Override
	@Column(name="diagnostic_cd")
	public String getDiagnosticCode() {
		return super.getDiagnosticCode();
	}
	
	/**
	 * @return the diagnosticRunId
	 */
	@Column(name="diagnostic_run_id")
	public String getDiagnosticRunId() {
		return diagnosticRunId;
	}
	
	/**
	 * @return the outcome
	 */
	@Column(name="outcome_cd")
	public Outcome getOutcome() {
		return outcome;
	}

	/**
	 * @return the issueResolvedFlag
	 */
	@Column(name="resolved_issue_flg")
	public int getIssueResolvedFlag() {
		return issueResolvedFlag;
	}
	
	/**
	 * Overrode this to remove the annotation as update date is not needed in 
	 * the xr
	 */
	@Override
	public Date getUpdateDate() {
		return super.getUpdateDate();
	}

	/**
	 * @param diagnosticTicketId the diagnosticTicketId to set
	 */
	public void setDiagnosticTicketId(String diagnosticTicketId) {
		this.diagnosticTicketId = diagnosticTicketId;
	}

	/**
	 * @param diagnosticRunId the diagnosticRunId to set
	 */
	public void setDiagnosticRunId(String diagnosticRunId) {
		this.diagnosticRunId = diagnosticRunId;
	}
	
	/**
	 * @param outcome the outcome to set
	 */
	public void setOutcome(Outcome outcome) {
		this.outcome = outcome;
	}

	/**
	 * @param issueResolvedFlag the issueResolvedFlag to set
	 */
	public void setIssueResolvedFlag(int issueResolvedFlag) {
		this.issueResolvedFlag = issueResolvedFlag;
	}

}

