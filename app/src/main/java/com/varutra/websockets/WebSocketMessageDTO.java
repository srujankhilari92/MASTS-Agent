package com.varutra.websockets;

import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

import com.varutra.websockets.utility.InvalidUtf8Exception;
import com.varutra.websockets.utility.Utf8Util;


public class WebSocketMessageDTO {
	
	public WebSocketChannelDTO channel;

	public Integer id;

	public Long timestamp;

	public String dateTime;

	public Integer opcode;

	public String readableOpcode;

	public Object payload;

	public Integer closeCode;

	public Boolean isOutgoing;

	public Integer payloadLength;

	public volatile Object tempUserObj;
	
	protected static final DateFormat dateFormatter;
	
	static {
		dateFormatter = DateFormat.getDateTimeInstance(
				SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM);
	}
	
	public WebSocketMessageDTO(WebSocketChannelDTO channel) {
		this.channel = channel; 
	}

	
	public WebSocketMessageDTO() {
		this(new WebSocketChannelDTO());
	}
	public void setTime(Timestamp ts) {
		timestamp = ts.getTime() + (ts.getNanos() / 1000000000);
		
		synchronized (dateFormatter) {
			dateTime = dateFormatter.format(ts);
		}
		
		String nanos = Integer.toString(ts.getNanos()).replaceAll("0+$", "");
		if (nanos.length() == 0) {
			nanos = "0";
		}
		
		dateTime = dateTime.replaceFirst("([0-9]+:[0-9]+:[0-9]+)", "$1." + nanos);
	}

	@Override
	public String toString() {
		if (channel.id != null && id != null) {
			return "#" + channel.id + "." + id;
		}
		return "";
	}

	public void copyInto(WebSocketMessageDTO other) {
		other.channel = this.channel;
		other.closeCode = this.closeCode;
		other.dateTime = this.dateTime;
		other.isOutgoing = this.isOutgoing;
		other.id = this.id;
		other.opcode = this.opcode;
		other.payload = this.payload;
		other.payloadLength = this.payloadLength;
		other.readableOpcode = this.readableOpcode;
		other.tempUserObj = this.tempUserObj;
		other.timestamp = this.timestamp;
	}
	
	public String getReadablePayload() throws InvalidUtf8Exception {
		if (payload instanceof String) {
			return (String) payload;
		} else if (payload instanceof byte[]){
			return Utf8Util.encodePayloadToUtf8((byte[]) payload);
		} else {
			return "";
		}
	}
}