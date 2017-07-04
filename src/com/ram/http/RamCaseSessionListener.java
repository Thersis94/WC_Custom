package com.ram.http;

import java.sql.Connection;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.ram.action.or.vo.RAMCaseVO;
import com.ram.persistance.RAMCasePersistanceManager;
import com.siliconmtn.common.constants.GlobalConfig;

/****************************************************************************
 * <b>Title:</b> RamCaseSessionListener.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> TODO
 * <b>Copyright:</b> Copyright (c) 2017
 * <b>Company:</b> Silicon Mountain Technologies
 * 
 * @author Billy Larsen
 * @version 3.3.1
 * @since Jun 28, 2017
 ****************************************************************************/
public class RamCaseSessionListener implements HttpSessionListener {
	public static final String CTX_COMP_ENV = "java:/comp/env";
	protected static Logger log;

	public RamCaseSessionListener() {
		super();
		log = Logger.getLogger(getClass());
	}

	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSessionListener#sessionCreated(javax.servlet.http.HttpSessionEvent)
	 */
	@Override
	public void sessionCreated(HttpSessionEvent arg0) {
	}


	/* (non-Javadoc)
	 * @see javax.servlet.http.HttpSessionListener#sessionDestroyed(javax.servlet.http.HttpSessionEvent)
	 */
	@SuppressWarnings("unchecked")
	@Override
	public void sessionDestroyed(HttpSessionEvent sessionEvent) {
		synchronized(this) {
			HttpSession session = sessionEvent.getSession();
			ServletContext sc = session.getServletContext();
			Map<Object, Object> attributes = (Map<Object, Object>) sc.getAttribute(GlobalConfig.KEY_ALL_CONFIG);

			RAMCaseVO cVo = (RAMCaseVO) session.getAttribute(RAMCasePersistanceManager.CASE_DATA_KEY);

			DataSource ds = sc.setAttribute(GlobalConfig.KEY_DB_CONN, ds);
		}
	}

	//getConnection
	private Connection getDBConnection() {
		// Get the datasource for the Init Name
        Context initContext = new InitialContext();
        Context ctx  = (Context)initContext.lookup(CTX_COMP_ENV);
		DataSource ds = (DataSource) ctx.lookup(dbName);
		return ds.getConnection();
	}
}
