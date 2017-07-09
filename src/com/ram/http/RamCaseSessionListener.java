package com.ram.http;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Enumeration;
import java.util.Map;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.sql.DataSource;

import org.apache.log4j.Logger;

import com.ram.action.or.RAMCaseManager;
import com.ram.action.or.vo.RAMCaseVO;
import com.ram.persistence.AbstractPersist;
import com.ram.persistence.RAMCasePersistenceFactory;
import com.ram.persistence.RAMCasePersistenceFactory.PersistenceType;
import com.siliconmtn.common.constants.GlobalConfig;
import com.siliconmtn.util.StringUtil;

/****************************************************************************
 * <b>Title:</b> RamCaseSessionListener.java
 * <b>Project:</b> WC_Custom
 * <b>Description:</b> SessionListener for the RAMCase Framework that attempts
 * to persist a RAMCaseVO to the Database if the Session is terminated.
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
	public void sessionCreated(HttpSessionEvent sessionEvent) {
		//Not implemented.
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
			Map<String, Object> attributes = (Map<String, Object>) sc.getAttribute(GlobalConfig.KEY_ALL_CONFIG);
			Enumeration<String> e = session.getAttributeNames();
			
			try(Connection conn = getDBConnection()) {
				AbstractPersist<?,?> ap = RAMCasePersistenceFactory.loadPersistenceObject(PersistenceType.DB, conn, attributes);
				while(e.hasMoreElements()) {
					String key = (String) e.nextElement();
					if (StringUtil.isEmpty(key)) continue;
					
					if (key.startsWith(RAMCaseManager.STORAGE_SUFFIX)) {
						ap.save((RAMCaseVO)session.getAttribute(key));
					}
				}
			} catch (Exception ex) {
				log.error("Error Processing Code", ex);
			}
		}
	}

	//getConnection
	private Connection getDBConnection() throws NamingException, SQLException {
		// Get the datasource for the Init Name
        Context initContext = new InitialContext();
        Context ctx  = (Context)initContext.lookup(CTX_COMP_ENV);
		DataSource ds = (DataSource) ctx.lookup("dbcon");
		return ds.getConnection();
	}
}
