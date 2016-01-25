package com.depuysynthes.huddle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.exception.InvalidDataException;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.util.StringUtil;
import com.siliconmtn.util.UUIDGenerator;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.siliconmtn.util.parser.AnnotationParser;
import com.smt.sitebuilder.action.SimpleActionAdapter;
import com.smt.sitebuilder.search.SearchDocumentHandler;
import com.smt.sitebuilder.security.SecurityController;
import com.smt.sitebuilder.util.solr.SolrActionUtil;
import com.smt.sitebuilder.util.solr.SolrDocumentVO;

/****************************************************************************
 * <b>Title</b>: ProductContactsAction.java<p/>
 * <b>Description: Wraps a Solr call around a file upload.  There is no public-face to this action; 
 * once the data is in Solr it is joined by Product Name from the HuddleProductAction.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2016<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Jan 15, 2016
 ****************************************************************************/
public class ProductContactsAction extends SimpleActionAdapter {

	public ProductContactsAction() {
		super();
	}

	public ProductContactsAction(ActionInitVO arg0) {
		super(arg0);
	}

	@Override
	public void list(SMTServletRequest req) throws ActionException {
		super.retrieve(req);
	}


	@Override
	public void update(SMTServletRequest req) throws ActionException {
		super.update(req);
		
		if (req.getFile("xlsFile") != null)
			processUpload(req);
	}
	
	
	/**
	 * processes the file upload and imports each row as a new event to add to the 
	 * desired event calendar. 
	 * @param req
	 * @throws ActionException
	 */
	private void processUpload(SMTServletRequest req) throws ActionException {
		AnnotationParser parser;
		FilePartDataBean fpdb = req.getFile("xlsFile");
		try {
			parser = new AnnotationParser(ProductContactVO.class, fpdb.getExtension());
		} catch(InvalidDataException e) {
			throw new ActionException("could not load import file", e);
		}
		try {
			//Gets the xls file from the request object, and passes it to the parser.
			//Parser then returns the list of populated beans
			Map<Class<?>, Collection<Object>> beans = parser.parseFile(fpdb, true);
			Collection<Object> beanList = new ArrayList<>(beans.get(ProductContactVO.class));
			Collection<SolrDocumentVO> contacts = new ArrayList<>(beanList.size());
			
			String lastProduct = "";
			UUIDGenerator uuid = new UUIDGenerator();
			for (Object o : beanList) {
				//set the eventTypeId for each
				ProductContactVO vo = (ProductContactVO) o;
				
				//weed out empty rows in the Excel file
				if (vo.getName() == null) continue;
				
				vo.setDocumentId(uuid.getUUID());
				vo.addRole(SecurityController.PUBLIC_ROLE_LEVEL);
				vo.addOrganization(req.getParameter("organizationId"));
				if (StringUtil.checkVal(vo.getProduct()).length() == 0) {
					vo.setProduct(lastProduct);
				} else {
					//save this for the next record, which may not have a value (implied the same)
					lastProduct = vo.getProduct();
				}
				contacts.add(vo);
			}
			
			//push the new assets to Solr
			pushToSolr(contacts);
			
		} catch (InvalidDataException e) {
			log.error("could not process DSI calendar import", e);
		}
	}
	
	
	/**
	 * pushes the assembled List of VOs over to Solr.  Purges all existing records first.
	 * @param beanList
	 * @throws ActionException
	 */
	private void pushToSolr(Collection<SolrDocumentVO> beanList) throws ActionException {
		SolrActionUtil util = new SolrActionUtil(getAttributes());
		util.removeByQuery(SearchDocumentHandler.INDEX_TYPE, HuddleUtils.IndexType.HUDDLE_PRODUCT_CONTACT.toString());
		util.addDocuments(beanList);
		util.commitSolr(false, true);
	}
}