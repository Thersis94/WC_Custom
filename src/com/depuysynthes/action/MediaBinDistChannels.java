package com.depuysynthes.action;

import java.util.HashSet;
import java.util.Set;

/****************************************************************************
 * <b>Title</b>: MediaBinOrgRoles.java<p/>
 * <b>Description: Business rules for which Distribution Channels (EXP file) SMT 
 * is authorized to recieve data from.
 * 
 * This drives the mediabin import, video chapters, DSI & DSHuddle business rules, 
 * and the admintool's 'explorer' window for MediaBin assets.</b> 
 * <p/>
 * <b>Copyright:</b> Copyright (c) 2015<p/>
 * <b>Company:</b> Silicon Mountain Technologies<p/>
 * @author James McKain
 * @version 1.0
 * @since Nov 25, 2015
 ****************************************************************************/
public class MediaBinDistChannels {

	private String orgId = "";

	public MediaBinDistChannels() {
		super();
	}

	public MediaBinDistChannels(String orgId) {
		this();
		setOrgId(orgId);
	}

	/**
	 * enum containing all the constants.  These are driven by DS/Mediabin
	 * and should never change once the org is added to WC.
	 * Changed DSI and Huddle to -1 so they're not ingested (from EXP file), w/o breaking code. -JM- 04.18.19
	 */
	public enum DistChannel {
		INTDS("INTDS.com", 2, "DPY_SYN_EMEA"),
		USDS("USDS.com", 1, "DPY_SYN"),
		DSI("DSI.com", -1, "DPY_SYN_INST"),
		DSHuddle("DSHuddle.com", -1, "DPY_SYN_HUDDLE"),
		SHOWPAD("Showpad EMEA", 3, "DPY_SYN_EMEA");

		private String orgId;
		private int typeCd;
		private String channel;
		DistChannel(String channel, int typeCd, String wcOrgId) {
			this.orgId  = wcOrgId;
			this.typeCd = typeCd;
			this.channel = channel;
		}
		public String getOrgId() { return orgId; }
		public int getTypeCd() { return typeCd; }
		public String getChannel() { return channel; }
	}

	/**
	 * returns a static list compiled from the above enum, of the EXP file dist 
	 * channels for the passed typeCd
	 * @param typeCd
	 * @return
	 */
	public static String[] getDistChannels(int typeCd) {
		Set<String> data = new HashSet<>();
		for (DistChannel dc : DistChannel.values()) {
			if (dc.getTypeCd() != typeCd) continue;
			data.add(dc.getChannel());
		}
		return data.toArray(new String[data.size()]);
	}

	public static DistChannel getByOrgId(String orgId) {
		for (DistChannel dc : DistChannel.values()) {
			if (dc.getOrgId().equals(orgId))
				return dc;
		}
		//default to EMEA, for all their different countries
		return DistChannel.INTDS;
	}

	public String getOpCoNm() {
		return getByOrgId(orgId).getChannel();
	}

	public int getTypeCd() {
		return getByOrgId(orgId).getTypeCd();
	}

	public void setOrgId(String orgId) {
		if (orgId != null) this.orgId = orgId;
	}
}