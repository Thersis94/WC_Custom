package com.depuysynthes.huddle;

import java.io.File;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

import org.im4java.core.ConvertCmd;
import org.im4java.core.IMOperation;

import com.depuysynthes.action.MediaBinAdminAction;
import com.depuysynthes.action.MediaBinDistChannels;
import com.siliconmtn.data.GenericVO;
import com.siliconmtn.util.CommandLineUtil;
import com.siliconmtn.util.StringUtil;
import com.smt.sitebuilder.common.constants.Constants;

/****************************************************************************
 * <b>Title</b>: ThumbnailGenerator.java<p/>
 * <b>Description: Generates a thumbnail image for MediaBin PDF assets that don't have
 * an image already accessible on the file system.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 28, 2015
 ****************************************************************************/
public class ThumbnailGenerator extends CommandLineUtil {

	StringBuilder email = new StringBuilder(500);

	/**
	 * @param args
	 */
	public ThumbnailGenerator(String[] args) {
		super(args);
		loadProperties("scripts/HuddleThumbnails.properties");
		loadDBConnection(props);
	}

	public static void main (String[] args) {
		ThumbnailGenerator tg = new ThumbnailGenerator(args);
		tg.run();
	}

	/* (non-Javadoc)
	 * @see com.siliconmtn.util.CommandLineUtil#run()
	 */
	@Override
	public void run() {
		//query the database for a list of assets that should have thumbnail images
		//this Set of Strings represents the trimmed file name of the image
		Set<GenericVO> records = getMediabinAssetList();
		
		//this could be turned into a Map<basePath, Set<records>> to support multiple Dist. Channels.
		String basePath = props.getProperty("huddleBaseImgPath");
		//cleanup folder separators for this machine's operating system
		basePath = basePath.replace("\\", "/").replace("/", File.separator);

		//iterate the list, removing those which already have an image present
		records = parseRecords(basePath, records);

		//for each file still on the list, we need to create a thumbnail image
		if (records.size() > 0)
			createImages(basePath, props.getProperty("mediabinDir"), records);

		//send a notification email
		try {
			sendEmail(email, null);
		} catch (Exception e) {
			log.error("could not send email", e);
		}
	}


	/**
	 * returns a list of MediaBin assets for the tagged Dist. Channels.
	 * The code is structured so that we can easily add DSI and other Channels in the future.
	 * @return
	 */
	private Set<GenericVO> getMediabinAssetList() {
		Set<GenericVO> records = new HashSet<>();
		Calendar huddleCal = Calendar.getInstance();
		boolean loadHuddle = (props.getProperty("dsHuddle") != null);

		StringBuilder sql = new StringBuilder(200);
		sql.append("select dpy_syn_mediabin_id, asset_nm from ");
		sql.append(props.getProperty(Constants.CUSTOM_DB_SCHEMA));
		sql.append("DPY_SYN_MEDIABIN where ");
		sql.append("lower(asset_type) in (?"); //loop all pdf types
		for (int x=MediaBinAdminAction.PDF_ASSETS.length; x > 0; x--) sql.append(",?");
		sql.append(") ");

		if (loadHuddle) {
			sql.append("and (opco_nm like ? and modified_dt > ?) ");
			huddleCal.add(Calendar.DAY_OF_YEAR, Integer.parseInt(props.getProperty("dsHuddle")));
			log.debug(huddleCal.getTime());
		}
		log.debug(sql);

		int x=1;
		try  (PreparedStatement ps = dbConn.prepareStatement(sql.toString())) {
			ps.setString(x++, "pdf");
			for (String at: MediaBinAdminAction.PDF_ASSETS) ps.setString(x++, at);
			if (loadHuddle) {
				ps.setString(x++, "%" + MediaBinDistChannels.DistChannel.DSHuddle.getChannel() + "%");
				ps.setDate(x++, new java.sql.Date(huddleCal.getTimeInMillis()));
			}
			ResultSet rs = ps.executeQuery();
			while (rs.next())
				records.add(new GenericVO(rs.getString(1), rs.getString(2)));

		} catch (SQLException sqle) {
			log.error("could not load records", sqle);
		}

		logMsg("found " + records.size() + " mediabin records to process");
		return records;
	}


	/**
	 * appends to the admin email as well as printing to log4J.
	 * @param msg
	 */
	protected void logMsg(String msg) {
		email.append("<br/>").append(msg);
		log.info(msg);
	}

	
	/**
	 * strip offending chars from the file names to get the image names
	 * @param rawName
	 * @return
	 */
	private String getImageName(String rawName) {
		rawName = StringUtil.replace(rawName, "/", "");
		rawName = StringUtil.replace(rawName, "-", "");
		return rawName + ".jpg";
	}

	private String getImageFolder(String rawName) {
		rawName = StringUtil.replace(rawName, "/", "");
		rawName = StringUtil.replace(rawName, "-", "");
		return rawName.substring(0,3);
	}



	/**
	 * iterate each asset in the list and test if a thumbnail is already present
	 * on the server.  Only thos MISSING images are returned on the new Set.
	 * @param basePath
	 * @param records
	 */
	private Set<GenericVO>  parseRecords(String baseImgPath, Set<GenericVO> records) {
		Set<GenericVO> newRecords = new HashSet<>(records.size());

		for (GenericVO vo : records) {
			String fn = StringUtil.checkVal(vo.getKey()); //fileName is based on Tracking Number
			fn = getImageFolder(fn) + File.separator + getImageName(fn);
			File f = new File(baseImgPath + fn);
			log.debug("testing for file: " + baseImgPath + fn);
			if (!f.exists()) newRecords.add(vo);
		}

		logMsg(newRecords.size() + " images need to be created");
		return newRecords;
	}



	/**
	 * iterates the list of records passed, creating a thumbnail image for each one
	 * This method proxies an OS-level API call to ImageMagick, which does the 
	 * complex heavy lifting for us (opening the PDf, reading the PDF's first page, 
	 * creating a thumbnail of it).
	 * @param baseImgPath
	 * @param mediabinPath
	 * @param records
	 */
	private void createImages(String baseImgPath, String mediabinPath, Set<GenericVO> records) {
		Set<String> failures = new HashSet<>();
		int successCnt =0;

		for (GenericVO vo : records) {
			String sourceFilePath = mediabinPath + vo.getValue();
			String destFilePath = baseImgPath + getImageFolder(""+vo.getKey()) + File.separator + getImageName(""+vo.getKey());
			try {
				//make sure the parent directories exist
				new File(baseImgPath + getImageFolder(""+vo.getKey())).mkdirs();

				// create the operation, add images and operators/options
				IMOperation op = new IMOperation();
				op.thumbnail(300);
				op.background("white");
				op.alpha("remove");
				op.addImage(sourceFilePath + "[0]");
				op.addImage(destFilePath);
				log.debug(op.toString());

				// execute the operation
				new ConvertCmd().run(op);
				++successCnt;
			} catch (Exception e) {
				failures.add("could not create image for " + vo.getKey() + " from " +  vo.getValue());
				log.error("could not create thumbnail", e);
			}
		}

		logMsg(successCnt + " images were created");
		for (String s : failures) logMsg(s); //and any failures to the report
	}
}