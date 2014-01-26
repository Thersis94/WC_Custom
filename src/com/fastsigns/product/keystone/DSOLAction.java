package com.fastsigns.product.keystone;

import java.io.ByteArrayOutputStream;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

import javax.servlet.http.HttpSession;

import org.apache.commons.codec.binary.Base64;

import com.fastsigns.action.franchise.CenterPageAction;
import com.fastsigns.product.keystone.parser.KeystoneDataParser;
import com.fastsigns.product.keystone.vo.KeystoneProductVO;
import com.fastsigns.security.FastsignsSessVO;
import com.lowagie.text.Document;
import com.lowagie.text.Image;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfWriter;
import com.siliconmtn.action.ActionException;
import com.siliconmtn.action.ActionInitVO;
import com.siliconmtn.commerce.ShoppingCartVO;
import com.siliconmtn.commerce.cart.storage.Storage;
import com.siliconmtn.http.SMTServletRequest;
import com.siliconmtn.io.FileManagerFactoryImpl;
import com.siliconmtn.util.Convert;
import com.siliconmtn.util.databean.FilePartDataBean;
import com.smt.sitebuilder.action.FileLoader;
import com.smt.sitebuilder.action.SBActionAdapter;
import com.smt.sitebuilder.common.ModuleVO;
import com.smt.sitebuilder.common.PageVO;
import com.smt.sitebuilder.common.constants.Constants;

/**
 * 
 * @author smt_user
 *
 */
public class DSOLAction extends SBActionAdapter {
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		FileLoader fl  = null;
		Map<String, Object> attributes = new HashMap<String, Object>();
		attributes.put("fileManagerType", "1");
		
		//String path = "smb://smt_user:smtrules@10.0.20.19/binary/";
		String path = "smb://192.168.2.180/Keystone/staging/binary/dsol/999//billy/";
		
		try {
    		fl = new FileLoader(attributes);
        	fl.setFileName("james-test.txt");
        	fl.setPath(path);
        	fl.setRename(true);
    		fl.setOverWrite(true);
        	fl.setData("This is a test file from my windows box".getBytes());
        	fl.writeFiles();
        	
        	//System.out.print("write complete");
    	} catch (Exception e) {
    		e.printStackTrace();
    	}

	}

	public DSOLAction() {
		
	}
	
	public DSOLAction(ActionInitVO avo){
		super(avo);
	}
	
	//TODO needs code cleanup and review
	public void build(SMTServletRequest req) throws ActionException {
		log.info("DSOL Build Method");
		KeystoneProductVO vo = new KeystoneProductVO(req);
		req.setValidateInput(false);
			/*
			 * Write the image data now so we don't have to carry around so much
			 * data on the request and risk blowing up a browser.   
			 */
			try {
				if(req.hasParameter("highResData")){
					String svg = req.getParameter("svgData");
					Map<String, Object> attr = vo.getProdAttributes();
					attr.put("jsonData", req.getParameter("jsonData"));
					if(req.hasParameter("materialName"))
						attr.put("materialName", req.getParameter("materialName"));
					
					String hrd = req.getParameter("highResData");
					String lrd = req.getParameter("thumbnailData");
					
					//Trim the pre-amble off the data string.
					int start = hrd.indexOf(",");  
					hrd = hrd.substring(start + 1);
					start = lrd.indexOf(",");  
					lrd = lrd.substring(start + 1);
					//Works but uses sun libraries = BAD!
					//BASE64Decoder decoder = new BASE64Decoder();
					//imgData = decoder.decodeBuffer(data);
					byte [] bHrd = Base64.decodeBase64(hrd.getBytes());
					byte [] bLrd = Base64.decodeBase64(lrd.getBytes());
					byte [] bPdf = getPdfData(bHrd);
					byte [] bSvg = URLDecoder.decode(svg, "UTF-8").getBytes();
					//Generate random folders
					String ran1 = getDirectoryPath();
					String ran2 = getDirectoryPath();
					
					String pdf = writeDsolFile(bPdf, UUID.randomUUID() + ".pdf", attributes, ran1, ran2);
					svg = writeDsolFile(bSvg, UUID.randomUUID() + ".svg", attributes, ran1, ran2);
					hrd = writeDsolFile(bHrd, UUID.randomUUID() + ".jpeg", attributes, ran1, ran2);
					log.debug("hrd = " + hrd);
					lrd = writeDsolFile(bLrd, UUID.randomUUID() + ".jpeg", attributes, ran1, ran2);
					log.debug("lrd = " + lrd);
					
					
					
					if(pdf != null && pdf.length() > 0) {
						vo.addProdAttribute("pdfPath", pdf);
						vo.addProdAttribute("pdfSize", bPdf.length);
						attr.put("pdfPath", pdf);
						req.setParameter("pdfPath", pdf);
					}
					
					if(svg != null && svg.length() > 0) {
						vo.addProdAttribute("svgData", svg);
						vo.addProdAttribute("svgSize", bSvg.length);
						attr.put("svgData", svg);
						req.setParameter("svgData", svg);
					}
					
					if(hrd != null && hrd.length() > 0) {
						vo.addProdAttribute("highResPath", hrd);
						vo.addProdAttribute("hrdDataSize", bHrd.length);
						req.setParameter("highResData", hrd);
					}
					
					if(lrd != null && lrd.length() > 0) {
						vo.addProdAttribute("lowResPath", lrd);
						vo.addProdAttribute("lrdDataSize", bLrd.length);
						req.setParameter("thumbnailData", lrd);
					}
					attr.put("highResData", hrd);
					attr.put("thumbnailData", lrd);
					//String lowResPath = DSOLAction.writeBase64File((String) prod.getProdAttributes().get("thumbnailData"), "thumbnailData.png", attributes, ran1, ran2);
					log.debug("Done Writing files");
				}
			} catch (Exception e) {
				log.debug(e);
			}
		
		req.getSession().setAttribute("DSOLVO", vo);
		
		/*
		 * TODO
		 * Save data somewhere until we checkout.
		 */
//		try {
//			DSOLAction.writeBase64File(new String(Base64.decode(req.getParameter("highResData"))), "highResImage.png", attributes);
//			DSOLAction.writeBase64File(new String(Base64.decode(req.getParameter("thumbnailData"))), "thumbnailImage.png", attributes);
//			DSOLAction.writeBase64File(req.getParameter("svgData"), "svgData.svg", attributes);
//			DSOLAction.writeBase64File(req.getParameter("jsonData"), "jsonData.txt", attributes);
//		} catch (Base64DecodingException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		PageVO page = (PageVO) req.getAttribute(Constants.PAGE_DATA);
		StringBuilder url = new StringBuilder(page.getRequestURI());
		url.append("?pmid=").append(req.getParameter("pmid"));
		url.append("&ecommerce_size_id=").append(req.getParameter("ecommerce_size_id"));
		url.append("&display=").append(req.getParameter("display"));
		url.append("&catalog=").append(req.getParameter("catalog"));
		url.append("&category=").append(req.getParameter("category"));
		url.append("&itemId=").append(req.getParameter("itemId"));
		url.append("&showDetail=true");
		if(req.hasParameter("materialName"))
			url.append("&materialName=").append(req.getParameter("materialName"));
		else if(req.getSession().getAttribute("DSOLVO") != null)
			url.append("&materialName=").append(((KeystoneProductVO)req.getSession().getAttribute("DSOLVO")).getProdAttributes().get("materialName"));
		log.debug("redirUrl: " + url);
		super.sendRedirect(url.toString(), null, req);
	}
	
	private String getDirectoryPath() {
		Random r = new Random();
		String i = r.nextInt(1000) + "";
		while(i.length() != 3){
			i = "0" + i;
		}
		i +="/";
		return i;
	}
	
	public void retrieve(SMTServletRequest req) throws ActionException {
		log.info("Entered DSOL Retrieve");
		ModuleVO mod = (ModuleVO) getAttribute(Constants.MODULE_DATA);
		HttpSession ses = req.getSession();
		KeystoneProductVO data = null;
		
		if (req.hasParameter("flush"))
			ses.setAttribute("DSOLVO", null);
		
		if (!req.hasParameter("sfs")) //if we're not starting from scratch, retrieve the exists object from session
			data = (KeystoneProductVO) ses.getAttribute("DSOLVO");
		
		/*
		 * If we are not re-editing a cart item, load Data from keystone.  
		 * Otherwise use data from cart.
		 */
		if (!req.hasParameter("itemId") && !req.hasParameter("sfs")) {
			this.loadDataFromKeystone(req, data, mod);
		}
		
		if (req.hasParameter("itemId") && !req.hasParameter("flush") && !req.hasParameter("showDetail")) {
			ShoppingCartAction sca = new ShoppingCartAction();
			sca.setDBConnection(dbConn);
			sca.setAttributes(attributes);
			Storage s = sca.loadCartStorage(req);
			ShoppingCartVO cart = s.load();
			data = (KeystoneProductVO) cart.getItems().get(req.getParameter("itemId")).getProduct();
			req.setParameter("dimensions", data.getSizes().get(0).getHeight() + " x " + data.getSizes().get(0).getWidth());
		}
		
		//if we did not load or build a data object, initialize a new one
		if (data == null) data = new KeystoneProductVO();
		
		if(!data.getProdAttributes().containsKey("sfs"))
			data.addProdAttribute("sfs", req.getParameter("sfs"));
		
		if (req.hasParameter("dsolProductId")) {
			data.setProductId(req.getParameter("dsolProductId"));
			data.setProductName(req.getParameter("dsolProdName"));
		}
		
		if (req.hasParameter("dimensions")) {
			try {
				String[] s = req.getParameter("dimensions").split(" x ");
				data.addProdAttribute("widthPixels", Integer.parseInt(s[0]) * 72);
				data.addProdAttribute("heightPixels", Integer.parseInt(s[1]) * 72);
			} catch (Exception e) {
				//possible Null, Arithmetic, or IndexOutOfBounds
				log.error(e);
			} 
		}
		
		if (req.hasParameter("dsolProdDesc"))
			data.setDescription(req.getParameter("dsolProdDesc"));
		
		if (req.hasParameter("materialName"))
			data.addProdAttribute("materialName", req.getParameter("materialName"));
		
		ses.setAttribute("DSOLVO", data);
		setAttribute(Constants.MODULE_DATA, mod);

	}
	
	
	/**
	 * this method conditionally makes two calls to Keystone to load data related to this DSOL request.
	 * @param req
	 * @param data
	 * @param mod
	 */
	private void loadDataFromKeystone(SMTServletRequest req, KeystoneProductVO data, ModuleVO mod) {
		FastsignsSessVO fsvo = (FastsignsSessVO) req.getSession().getAttribute(KeystoneProxy.FRAN_SESS_VO);

		//TODO this call is uncached, perhaps blend with StartFromScratchAction, or use data from ProductDetailsAction?
		KeystoneProxy proxy = new KeystoneProxy(attributes);
		proxy.setSessionCookie(req.getCookie(Constants.JSESSIONID));
		proxy.addPostData("ecommerce_size_id", req.getParameter("ecommerce_size_id"));
		proxy.addPostData("franchise_id", fsvo.getFranchise(CenterPageAction.getFranchiseId(req)).getFranchiseId());
		
		if (!Convert.formatBoolean(req.getParameter("showDetail"))) {
			//this call adds the parsed response directly to our ModuleVO mod
			sendTemplatesRequest(req, mod, proxy);
			
		} else {
			setMaterialsRequest(req, mod, proxy);
		}
	}
	
	
	/**
	 * sends the call for materials off to the proxy, expects a parsed response back
	 * See DSOLMaterialsParser
	 * @param req
	 * @param mod
	 * @param proxy
	 */
	private void setMaterialsRequest(SMTServletRequest req, ModuleVO mod, KeystoneProxy proxy) {
		proxy.setModule("productsSizes");
		proxy.setAction("getProductsByEcommSizeId");
		proxy.setParserType(KeystoneDataParser.DataParserType.DSOLMaterials);
		
		try {
			//transform the response into something meaningful to WC
			Object obj = proxy.getData().getActionData();
			req.setAttribute("materials", obj);  //obj is actually a Map<String, String>
			mod.setActionData(obj);
			
		} catch (Exception ide) {
			log.error(ide);
			mod.setError(ide);
			mod.setErrorMessage("Unable to load DSOL Materials");
		}
	}
	
	
	/**
	 * sends a call for templates off to the proxy.  Expects a parsed response back.
	 * See DSOLTemplatesParser
	 * @param req
	 * @param mod
	 * @param proxy
	 */
	private void sendTemplatesRequest(SMTServletRequest req, ModuleVO mod, KeystoneProxy proxy) {
		proxy.setModule("dsolFiles");
		proxy.setAction("getTemplateData");
		proxy.setParserType(KeystoneDataParser.DataParserType.DSOLTemplates);
		if (req.hasParameter("dsolItemId")) {
			proxy.addPostData("dsolItemId", req.getParameter("dsolItemId"));
		} else if (req.hasParameter("dsolProductId")) {
			proxy.addPostData("dsolItemId", req.getParameter("dsolProductId"));
		}

		try {
			//tell the proxy to go get our data
			mod.setActionData(proxy.getData().getActionData());
		} catch (Exception ide) {
			log.error(ide);
			mod.setError(ide);
			mod.setErrorMessage("Unable to load DSOL Templates");
		}
	}
	
	
	/**
	 * turns the customized DSOL image into a PDF file.
	 * @param data
	 * @return
	 */
	public static byte [] getPdfData(byte [] data) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try {
			Image img = Image.getInstance(data);
			img.setAbsolutePosition(0, 0);
			Document hrdoc = new Document(new Rectangle(0, 0, img.getWidth(), img.getHeight()));
			PdfWriter.getInstance(hrdoc, baos);
			hrdoc.open();
			hrdoc.add(img);
	        hrdoc.close();
		} catch (Exception e) {
			log.error("Could not write pdf file.", e);
		}
		
		return baos.toByteArray();
	
	}

	/**
	 * writes the customized image to the file system
	 * @param data
	 * @param name
	 * @param attributes
	 * @param ran1
	 * @param ran2
	 * @return
	 */
	public String writeDsolFile(byte [] data, String name, Map<String, Object> attributes, String ran1, String ran2){
		attributes.put(FileManagerFactoryImpl.CONFIG_FILE_MANAGER_TYPE, attributes.get("dsolFileManagerType"));
		log.debug("Creating FileLoader of type: " + attributes.get(FileManagerFactoryImpl.CONFIG_FILE_MANAGER_TYPE));
		
		FilePartDataBean fpdb = new FilePartDataBean();
		fpdb.setCanonicalPath((String) attributes.get("keystoneDsolTemplateFilePath") + ran1 + ran2);
		log.debug("path=" + fpdb.getCanonicalPath());
		
		String name2 = "";
		fpdb.setFileName(name);
		fpdb.setFileData(data);
		
		FileLoader fl  = null;
		try {
			fl = new FileLoader(attributes);
			fl.setFileName(fpdb.getFileName());
			fl.setPath(fpdb.getCanonicalPath());
			fl.setRename(true);
			fl.setOverWrite(false);
			fl.setData(fpdb.getFileData());
			name2 = ran1 + ran2 + fl.writeFiles();
			log.debug(name2);
		} catch (Exception e) {
			log.error("There was a problem writing the file: ", e);
			return "";
		}
		return name2;
	}
}
