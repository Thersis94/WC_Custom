package com.depuysynthes.action;

// JDK 1.7
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;


// SMT Base Libs
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.action.ActionRequest;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;

// WC Libs
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: PatentAction <p/>
 * <b>Description:</b> A SimpleAction that loads DePuy Synthes Patents from 
 *  a table in CustomDB.  The admin side of this imports an Excel file.
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2013<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Dec 4, 2015<p/>
 ****************************************************************************/
public class PatentAction extends SimpleActionAdapter {

	public PatentAction () {
		super();
	}

	public PatentAction (ActionInitVO actionInit) {
		super(actionInit);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#update(com.siliconmtn.http.SMTServletRequest)
	 */
	public void update(ActionRequest req) throws ActionException {
		super.update(req);

		//save the Excel file if one was uploaded
		if (req.getFile("xlsFile") != null)
			importExcelFile(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#list(com.siliconmtn.http.SMTServletRequest)
	 */
	public void list(ActionRequest req) throws ActionException {
		super.retrieve(req);
	}


	/*
	 * (non-Javadoc)
	 * @see com.smt.sitebuilder.action.SBActionAdapter#retrieve(com.siliconmtn.http.SMTServletRequest)
	 */
	public void retrieve(ActionRequest req) throws ActionException {
		//ensure we were given something to search for, otherwise a query is not needed (the search form is displayed)
		if (!req.hasParameter("code")) return;

		StringBuilder sql = new StringBuilder(150);
		sql.append("select top 1 item_txt, desc_txt, code_txt, patents_txt  from ");
		sql.append(getAttribute(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_PATENT where code_txt=? and action_id=? ");
		log.debug(sql + "|" + req.getParameter("code") + "|" + actionInit.getActionId());

		//note this lookup only returns one record, ever.  0 or 1 matching to the searched value.
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, req.getParameter("code"));
			ps.setString(2, actionInit.getActionId());
			ResultSet rs = ps.executeQuery();
			if (rs.next())
				super.putModuleData(new PatentVO(rs));


		} catch (SQLException sqle) {
			log.error("could not load patents", sqle);
		}
	}


	/**
	 * turn excel into list of beans using annotations.  Import the beanList into the database.
	 * @param req
	 * @throws ActionException
	 */
	private void importExcelFile(ActionRequest req) throws ActionException {
		String actionId = StringUtil.checkVal(req.getAttribute(SB_ACTION_ID), req.getParameter(SB_ACTION_ID));
		AnnotationParser parser;
		FilePartDataBean fpdb = req.getFile("xlsFile");
		try {
			parser = new AnnotationParser(PatentVO.class, fpdb.getExtension());
		} catch(InvalidDataException e) {
			throw new ActionException("could not load import file", e);
		}
		try {
			//Gets the xls file from the request object, and passes it to the parser.
			//Parser then returns the list of populated beans
			Map<Class<?>, Collection<Object>> beans = parser.parseFile(fpdb, true);
			
			//Disable the db autocommit for the insert batch
			dbConn.setAutoCommit(false);
			ArrayList<Object> beanList = new ArrayList<>(beans.get(PatentVO.class));
			String companyNm = null;
			for (Object o : beanList) {
				PatentVO vo = (PatentVO) o;
				
				//use the company name off the first record to purge all existing data before we do any inserts
				if (companyNm == null) {
					companyNm = vo.getCompany();
					deleteByCompany(actionId, companyNm);
				}
				
				vo.setActionId(actionId);
				vo.setOrganizationId(req.getParameter("organizationId"));
			}

			 importBeans(beanList);
			
			//commit only after the entire import succeeds
			dbConn.commit();
			
		} catch (InvalidDataException | SQLException e) {
			log.error("could not process patent import", e);
		} finally {
			try {
				//restore autocommit state
				dbConn.setAutoCommit(true);
			} catch (SQLException e) {}
		}
	}
	
	
	/**
	 * deletes all existing records for the given companyNm
	 * @param actionId
	 * @param companyNm
	 */
	private void deleteByCompany(String actionId, String companyNm) throws ActionException {
		String customDb = getAttribute(Constants.CUSTOM_DB_SCHEMA).toString();
		StringBuilder sql = new StringBuilder(100);
		sql.append("delete from ").append(customDb).append("DPY_SYN_PATENT ");
		sql.append("where company_nm=? and action_id=?");
		log.debug(sql);
		
		try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(1, companyNm);
			ps.setString(2, actionId);
			ps.executeUpdate();
			
		} catch (SQLException sqle) {
			log.error("could not delete company records", sqle);
			 throw new ActionException(sqle);
		}
	}
	
	
	 /**
	  * Imports the beans as a set of new records to the event_entry table
	  * @param beanList
	  * @param actionId
	  * @throws ActionException
	  */
	 private void importBeans(ArrayList<Object> beanList) throws ActionException {
		 if (beanList == null || beanList.size() == 0) return;
		 
		String customDb = getAttribute(Constants.CUSTOM_DB_SCHEMA).toString();
		 StringBuilder sql = new StringBuilder(150);
		 sql.append("insert into ").append(customDb).append("DPY_SYN_PATENT ");
		 sql.append("(ACTION_ID, ORGANIZATION_ID, COMPANY_NM, CODE_TXT, ");
		 sql.append("ITEM_TXT, DESC_TXT, PATENTS_TXT, CREATE_DT) ");
		 sql.append("values (?,?,?,?,?,?,?,?)");
		 log.debug(sql);

		 try (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			 for ( Object obj : beanList ) {
				 //Casts the generic object to PatentVO
				 PatentVO vo = (PatentVO) obj;
				 
				 //code is required, skip any without it
				 if (vo.getCode() == null || vo.getCode().length() == 0) continue;
				 
				 ps.setString(1, vo.getActionId());
				 ps.setString(2, vo.getOrganizationId());
				 ps.setString(3, vo.getCompany());
				 ps.setString(4, vo.getCode());
				 ps.setString(5, vo.getItem());
				 ps.setString(6, vo.getDesc());
				 ps.setString(7, StringUtil.replace(vo.getPatents(), "|","; ")); //clean up the tokenized data and store it the way we'll need it for display
				 ps.setTimestamp(8, Convert.getCurrentTimestamp());
				 ps.addBatch();
				 log.debug("added to batch: "+ vo.getCode());
			 }
			 ps.executeBatch();
		 } catch (SQLException e){
			 throw new ActionException("Error inserting records into dpy_syn_patent table.",e);
		 }
	 }
}