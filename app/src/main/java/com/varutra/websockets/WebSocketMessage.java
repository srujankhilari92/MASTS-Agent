package com.varutra.websockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.sql.Timestamp;

public abstract class WebSocketMessage {

	public enum Direction {
		INCOMING, OUTGOING
	}

	private WebSocketProxy proxy;
	
	private int messageId;
	
	protected ByteBuffer payload;

	protected Direction direction;
	
	protected boolean isFinished;

	protected Timestamp timestamp;
	

	public static final int OPCODE_CONTINUATION = 0x0;
	public static final int OPCODE_TEXT = 0x1;
	public static final int OPCODE_BINARY = 0x2;

	public static final int OPCODE_CLOSE = 0x8;
	public static final int OPCODE_PING = 0x9;
	public static final int OPCODE_PONG = 0xA;

	public static final int STATUS_CODE_OK = 1000;
	
	
	public static final int STATUS_CODE_GOING_AWAY = 1001;
	
	public static final int STATUS_CODE_PROTOCOL_ERROR = 1002;
	
	
	public static final int STATUS_CODE_INVALID_DATA_TYPE = 1003;
	
	
	public static final int STATUS_CODE_INVALID_DATA = 1007;
	
	
	public static final int STATUS_CODE_POLICY_VIOLATION = 1008;
	
	
	public static final int STATUS_CODE_MESSAGE_TOO_LARGE = 1009;
	
	
	public static final int STATUS_CODE_EXTENSION_NEGOTIATION_FAILED = 1010;
	
		public static final int STATUS_CODE_SERVER_ERROR = 1011;

	
	public static final int[] OPCODES = {OPCODE_TEXT, OPCODE_BINARY, OPCODE_CLOSE, OPCODE_PING, OPCODE_PONG };

	protected int opcode = -1;

		protected int closeCode = -1;

	private final WebSocketMessageDTO dto;
	
	public WebSocketMessage(WebSocketProxy proxy, int messageId) {
		this(proxy, messageId, new WebSocketMessageDTO());
	}

	protected WebSocketMessage(WebSocketProxy proxy, int messageId, WebSocketMessageDTO baseDto) {
		this.proxy = proxy;
		this.messageId = messageId;
		this.dto = baseDto;
	}

	public int getMessageId() {
		return messageId;
	}

	public abstract boolean forward(OutputStream out) throws IOException;
	public abstract void readContinuation(InputStream in, byte frameHeader) throws IOException;
	public int getCloseCode() {
		return closeCode;
	}
	
	public final int getOpcode() {
		return opcode;
	}
	
	public final boolean isBinary() {
		return isBinary(opcode);
	}

	public static final boolean isBinary(int opcode) {
		return opcode == OPCODE_BINARY;
	}
	
	public final boolean isText() {
		return isText(opcode);
	}

	public static final boolean isText(int opcode) {
		return opcode == OPCODE_TEXT;
	}

	public final boolean isControl() {
		return isControl(opcode);
	}

	public static final boolean isControl(int opcode) {
		if (opcode >= 0x8 && opcode <= 0xF) {
			return true;
		}
		
		return false;
	}

	public boolean isFinished() {
		return isFinished;
	}

	public String getOpcodeString() {
		return opcode2string(opcode);
	}

	public static String opcode2string(int opcode) {
		switch (opcode) {
		case OPCODE_BINARY:
			return "BINARY";
			
		case OPCODE_CLOSE:
			return "CLOSE";
			
		case OPCODE_CONTINUATION:
			return "CONTINUATION";
			
		case OPCODE_PING:
			return "PING";
			
		case OPCODE_PONG:
			return "PONG";
			
		case OPCODE_TEXT:
			return "TEXT";
			
		default:
			return "UNKNOWN";
		}
	}
	
	protected void appendPayload(byte[] bytes) {		
		if (payload == null) {
			payload = ByteBuffer.allocate(bytes.length);
			payload.put(bytes);
		} else {
			payload = reallocate(payload, payload.capacity() + bytes.length);
			payload.put(bytes);
		}
		
		if (isFinished) {
			payload.flip();
		}
	}
	
	protected ByteBuffer reallocate(ByteBuffer src, int newSize) {
        int srcPos = src.position();
        if (srcPos > 0) {
            src.flip();
        }
        
        ByteBuffer dest = ByteBuffer.allocate(newSize);
        dest.put(src);
        dest.position(srcPos);
        
        return dest;
    }

	/**
	 * Returns date of receiving this message. Might also indicate the timestamp
	 * of the last frame received.
	 * 
	 * @return timestamp of message arrival
	 */
	public Timestamp getTimestamp() {
		return timestamp;
	}
	
	/**
	 * @return number of bytes used in the payload
	 */
	public abstract Integer getPayloadLength();
	
	/**
	 * Returns the 'original' payload as found in the WebSocket frame. Returned
	 * bytes array does not back the messages payload buffer (i.e. it is a
	 * copy).
	 * 
	 * @return bytes of payload
	 */
	public abstract byte[] getPayload();

	/**
	 * Modifies the payload to given byte array. Use
	 * {@link WebSocketMessage#setReadablePayload(String)} for setting payloads
	 * of non-binary messages.
	 * 
	 * @param newPayload
	 * @throws WebSocketException
	 */
	public abstract void setPayload(byte[] newPayload) throws WebSocketException;

	/**
	 * Returns the payload from {@link WebSocketMessage#getPayload()} as
	 * readable string (i.e.: converted to UTF-8).
	 * 
	 * @return readable representation of payload
	 */
	public abstract String getReadablePayload();

	/**
	 * Modifies the payload to given UTF-8 string. Converts that into bytes. 
	 * 
	 * @param newReadablePayload
	 * @throws WebSocketException 
	 */
	public abstract void setReadablePayload(String newReadablePayload) throws WebSocketException;
	
	public abstract Direction getDirection();

	public WebSocketMessageDTO getDTO() {
		dto.channel = proxy.getDTO();
		
		Timestamp ts = getTimestamp();
		dto.setTime(ts);
		
		dto.opcode = getOpcode();
		dto.readableOpcode = getOpcodeString();

		if (isBinary()) {
			dto.payload = getPayload();
		} else {
			dto.payload = getReadablePayload();
			
			if (dto.payload == null) {
				dto.payload = "";
			}
		}
		
		dto.isOutgoing = (getDirection() == Direction.OUTGOING) ? true : false;
		
		dto.payloadLength = getPayloadLength();
		
		return dto;
	}
	
	@Override
	public String toString() {
		return "WebSocketMessage#" + getMessageId();
	}
}
