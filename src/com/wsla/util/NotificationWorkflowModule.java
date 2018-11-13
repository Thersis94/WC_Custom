package com.wsla.util;

import java.sql.Connection;

import com.siliconmtn.workflow.data.WorkflowModuleVO;
import com.siliconmtn.workflow.modules.AbstractWorkflowModule;

/********************************************************************
 * <b>Title:</b> NotificationWorkflowModule.java<br/>
 * <b>Description:</b> Workflow module to send notification emails.<br/>
 * <b>Copyright:</b> Copyright (c) 2018<br/>
 * <b>Company:</b> Silicon Mountain Technologies
 * @author Tim Johnson
 * @version 3.0
 * @since Nov 12, 2018
 *******************************************************************/
public class NotificationWorkflowModule extends AbstractWorkflowModule {

	// Constants for Workflow Config Param Mappings.
	public static final String TICKET_ID = "TICKET_ID";
	public static final String USER_ID = "USER_ID";
	public static final String STATUS_CODE = "STATUS_CODE";

	/**
	 * @param config
	 * @param conn
	 * @throws Exception
	 */
	public NotificationWorkflowModule(WorkflowModuleVO mod, Connection conn, String schema) throws Exception {
		super(mod, conn, schema);
	}

	/* (non-Javadoc)
	 * @see com.ram.workflow.modules.WorkflowModuleIntfc#run()
	 */
	@Override
	protected final void run() throws Exception {
		//attributes, getConnection()
		
		log.debug("Workflow Module Ticket ID: " + mod.getModuleConfig(TICKET_ID));
		log.debug("Workflow Module User ID: " + mod.getModuleConfig(USER_ID));
		log.debug("Workflow Module Status Code: " + mod.getModuleConfig(STATUS_CODE));
	}
}
