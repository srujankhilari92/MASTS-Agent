package com.varutra.websockets;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

import java.util.logging.Logger;

import com.varutra.websockets.utility.InvalidUtf8Exception;
import com.varutra.websockets.utility.Utf8Util;

public class WebSocketProxyV13 extends WebSocketProxy {

	private static final Logger logger = Logger.getLogger(WebSocketProxyV13.class.getSimpleName());
	
	private static final int PAYLOAD_MAX_FRAME_LENGTH = Integer.MAX_VALUE;

	public WebSocketProxyV13(Socket localSocket, Socket remoteSocket) throws WebSocketException {
		super(localSocket, remoteSocket);
	}

	@Override
	protected WebSocketMessage createWebSocketMessage(InputStream in, byte frameHeader) throws IOException {
		return new WebSocketMessageV13(this, in, frameHeader);
	}

	@Override
	protected WebSocketMessage createWebSocketMessage(WebSocketMessageDTO message) throws WebSocketException {
		return new WebSocketMessageV13(this, message);
	}

	protected class WebSocketMessageV13 extends WebSocketMessage {
		
		private class WebSocketFrameV13 {
		    private final Random randomizer = new Random();
			private ByteBuffer buffer;
			private byte[] mask;
			private boolean isMasked;
			
			private boolean isForwarded;

			private boolean isSealed = false;
			
			private int rsv;
			
			public WebSocketFrameV13() {
				buffer = ByteBuffer.allocate(4096);
				isMasked = false;
				mask = new byte[4];
				isForwarded = false;
				rsv = 0;
			}

			public WebSocketFrameV13(ByteBuffer payload, Direction direction, boolean isFinished, int frameOpcode, int rsv) {
				buffer = ByteBuffer.allocate(payload.limit() + 16);
				this.rsv = rsv;

				int payloadLength = payload.limit();
				if (direction.equals(Direction.OUTGOING)) {
					isMasked = true;

					mask = new byte[4];
					randomizer.nextBytes(mask);
					
					int maskPosition = 0;
					for (int i = 0; i < payloadLength; i++) {
						payload.put(i, (byte) (payload.get(i) ^ mask[maskPosition]));
						maskPosition = (maskPosition + 1) % 4;
					}
				} else {
					isMasked = false;
				}
				
				isForwarded = false;

				byte finishedBits = (byte) (isFinished ? 0x80 : 0x00);
				byte rsvBits = (byte) ((this.rsv & 0x07) << 4);
				byte opcodeBits = (byte) (frameOpcode & 0x0F);
				byte frameHeader = (byte) (finishedBits | rsvBits | opcodeBits);
				buffer.put(frameHeader);
				logger.info("Frame header of newly created WebSocketFrame: " + getByteAsBitString(frameHeader));

				if (payloadLength < PAYLOAD_LENGTH_16) {
					buffer.put((byte) ((isMasked ? 0x80 : 0x00) | (payloadLength & 0x7F)));
				} else if (payloadLength < 65536) {
					buffer.put((byte) ((isMasked ? 0x80 : 0x00) | PAYLOAD_LENGTH_16));
					buffer.putShort((short) payloadLength);
				} else {
					buffer.put((byte) ((isMasked ? 0x80 : 0x00) | PAYLOAD_LENGTH_63));
					buffer.putLong(payloadLength);
				}
				
				if (isMasked) {
					buffer.put(mask);
				}
				
				buffer.put(payload.array());
				
				seal();
			}

			public void put(byte b) throws WebSocketException {
				if (isSealed) {
					throw new WebSocketException("You cannot change a 'sealed' frame.");
				}
				buffer.put(b);
			}

			public void put(byte[] b) throws WebSocketException {
				if (isSealed) {
					throw new WebSocketException("You cannot change a 'sealed' frame.");
				}
				buffer.put(b);
			}

			public void setMasked(boolean isMasked) {
				this.isMasked = isMasked;
			}
			
			public boolean isMasked() {
				return isMasked;
			}

			public void setMask(byte[] read) {
				mask = read;
			}

			public byte getMaskAt(int index) {
				return mask[index];
			}

			public int getFreeSpace() {
				if (isSealed) {
					return 0;
				}
				return buffer.capacity() - buffer.position();
			}

			public void reallocateFor(int bytesRead) throws WebSocketException {
				if (isSealed) {
					throw new WebSocketException("You cannot change size of 'sealed' frame's buffer.");
				}
				buffer = reallocate(buffer, buffer.position() + bytesRead);
			}
			
			public boolean isForwarded() {
				return isForwarded;
			}

			public void seal() {
				if (!isSealed) {
					buffer.flip();
					isSealed  = true;
				}
			}

			public byte[] getBuffer() throws WebSocketException {
				if (!isSealed) {
					throw new WebSocketException("You should call seal() on WebSocketFrame first, before getBuffer().");
				}
				byte[] result = new byte[buffer.limit()];
				buffer.get(result);
				return result;
			}

			public void setForwarded(boolean isForwarded) {
				this.isForwarded = isForwarded;
			}

			/**
			 * Valid values are in the interval [1,6].
			 * 
			 * @param rsv
			 */
			public void setRsv(int rsv) {
				this.rsv  = rsv;
			}

		}
		
		private List<WebSocketFrameV13> receivedFrames = new ArrayList<WebSocketFrameV13>();
		private WebSocketFrameV13 currentFrame;

		private int payloadLength;

		private boolean hasChanged;

		private boolean isValidUtf8Payload;

		private static final int PAYLOAD_LENGTH_16 = 126;

		private static final int PAYLOAD_LENGTH_63 = 127;

		public WebSocketMessageV13(WebSocketProxy proxy, InputStream in, byte frameHeader) throws IOException {
			super(proxy, getIncrementedMessageCount());
			
			opcode = (frameHeader & 0x0F);
			
			Calendar calendar = Calendar.getInstance();
			timestamp = new Timestamp(calendar.getTimeInMillis());
			
			readFrame(in, frameHeader);
			direction = receivedFrames.get(0).isMasked() ? Direction.OUTGOING : Direction.INCOMING;
		}

		public WebSocketMessageV13(WebSocketProxy proxy, WebSocketMessageDTO message) throws WebSocketException {
			super(proxy, getIncrementedMessageCount(), message);
			message.id = getMessageId();
			
			Calendar calendar = Calendar.getInstance();
			timestamp = new Timestamp(calendar.getTimeInMillis());
			message.setTime(timestamp);
			
			isFinished = true;
			opcode = message.opcode;
			closeCode = (message.closeCode == null) ? -1 : message.closeCode;
			direction = message.isOutgoing ? Direction.OUTGOING : Direction.INCOMING;
			
			payload = ByteBuffer.allocate(0);
			if (message.payload instanceof byte[]) {
				setPayload((byte[])message.payload);
			} else if (message.payload instanceof String && message.payload != null){
				setReadablePayload((String)message.payload);
			}
		}

		@Override
		public void readContinuation(InputStream in, byte frameHeader) throws IOException {			
			readFrame(in, frameHeader);
		}
		
		private String getByteAsBitString(byte word) {
	      StringBuilder buf = new StringBuilder();
	      for (int i = 0; i < 8; i++) {
	         buf.append((word >> (8 - (i+1)) & 0x0001));
	      }
	      return buf.toString();
	   }

		private void readFrame(InputStream in, byte frameHeader) throws IOException {
			isFinished = (frameHeader >> 7 & 0x1) == 1;
			
			currentFrame = new WebSocketFrameV13();
			currentFrame.put(frameHeader);
			
			currentFrame.setRsv((frameHeader >> 4 & 0x7));

			byte payloadByte = read(in);
			
			currentFrame.setMasked((payloadByte >> 7 & 0x1) == 1);
			
			payloadLength = determinePayloadLength(in, payloadByte);
			logger.info("length of current frame payload is: " + payloadLength + "; first two bytes: " + getByteAsBitString(frameHeader) + " " + getByteAsBitString(payloadByte));

			if (currentFrame.isMasked()) {
				currentFrame.setMask(read(in, 4));
			}

			byte[] payload = read(in, payloadLength);

			if (currentFrame.isMasked()) {
				int currentMaskByteIndex = 0;
				for (int i = 0; i < payload.length; i++) {
					payload[i] = (byte) (payload[i] ^ currentFrame.getMaskAt(currentMaskByteIndex));
					currentMaskByteIndex = (currentMaskByteIndex + 1) % 4;
				}
			}
			
			if (isText(opcode)) {
				logger.info("got text frame payload");
			} else if (isBinary(opcode)) {
				logger.info("got binary frame payload");				
			} else {
				if (opcode == OPCODE_CLOSE) {
					if (payload.length > 1) {
						closeCode = ((payload[0] & 0xFF) << 8) | (payload[1] & 0xFF);
						logger.info("close code is: " + closeCode);
						
						payload = getReadableCloseFramePayload(payload, closeCode);
					}
					
					if (payload.length > 0) {
						try {
							logger.info("got control-payload: " + Utf8Util.encodePayloadToUtf8(payload));
						} catch (InvalidUtf8Exception e) {
						}
					}
				}
			}
			
			appendPayload(payload);
			
			currentFrame.seal();
			receivedFrames.add(currentFrame);
		}

		private int determinePayloadLength(InputStream in, byte payloadByte) throws IOException {
			int length = (payloadByte & 0x7F);

			if (length < PAYLOAD_LENGTH_16) {
			} else {
				int bytesToRetrieve = 0;

				if (length == PAYLOAD_LENGTH_16) {
					bytesToRetrieve = 2;
				} else if (length == PAYLOAD_LENGTH_63) {
					bytesToRetrieve = 8;
				}

				byte[] extendedPayloadLength = read(in, bytesToRetrieve);

				length = 0;
				for (int i = 0; i < bytesToRetrieve; i++) {
					byte extendedPayload = extendedPayloadLength[i];
					
					length = (length << 8) | (extendedPayload & 0xFF);
				}
			}
			
			return length;
		}

		private byte[] getReadableCloseFramePayload(byte[] payload, int statusCode) {
			byte[] closeCode = Integer.toString(statusCode).getBytes();
			
			byte[] newPayload = new byte[payload.length + (closeCode.length - 2)];
			
			try {
				System.arraycopy(closeCode, 0, newPayload, 0, closeCode.length);
			} catch (IndexOutOfBoundsException e) {
				logger.info(e.getMessage());
			}
			
			try {
				System.arraycopy(payload, 2, newPayload, closeCode.length, payload.length - 2);
			} catch (IndexOutOfBoundsException e) {
				logger.info(e.getMessage());
			}
			
			return newPayload;
		}

		private ByteBuffer getTransmittableCloseFramePayload(ByteBuffer payload) throws NumberFormatException, WebSocketException {
			String closeCodePayload;
			try {
				closeCodePayload = Utf8Util.encodePayloadToUtf8(payload.array(), 0, 4);
			} catch (InvalidUtf8Exception e) {
				throw new WebSocketException(e.getMessage(), e);
			}
			
			int newCloseCode = Integer.parseInt(closeCodePayload);
			
			byte[] newCloseCodeByte = new byte[2];
			newCloseCodeByte[0] = (byte) ((newCloseCode >> 8) & 0xFF);
			newCloseCodeByte[1] = (byte) ((newCloseCode) & 0xFF);
			
			ByteBuffer newPayload = ByteBuffer.allocate(payload.limit() - 2);
			if (payload.limit() > 4) {
			}
			newPayload.put(newCloseCodeByte, 0, 2);
			
			return newPayload;
		}

		private byte read(InputStream in) throws IOException {
			byte[] buffer = read(in, 1);
			return buffer[0];
		}

		private byte[] read(InputStream in, int length) throws IOException {
			byte[] buffer = new byte[length];
			
			int bytesRead = 0;
			do {
				bytesRead += in.read(buffer, bytesRead, length - bytesRead);
			} while (length != bytesRead);
			
			if (currentFrame.getFreeSpace() < bytesRead) {
				currentFrame.reallocateFor(bytesRead);
			}

			currentFrame.put(buffer);
			
			return buffer;
		}

		@Override
		public boolean forward(OutputStream out) throws IOException {
			if (out == null) {
				return false;
			}
			
			logger.info("forward message#" + getMessageId());
			
			if (hasChanged) {
				if (opcode == OPCODE_CLOSE) {
					payload = getTransmittableCloseFramePayload(payload);
				}
				
				int writtenBytes = 0;
				int frameLength = Math.min(PAYLOAD_MAX_FRAME_LENGTH, payload.limit());
				int frameOpcode = opcode;
				boolean isLastFrame;
				
				do {
					ByteBuffer tempBuffer = ByteBuffer.allocate(frameLength);
					tempBuffer.hasArray();
					payload.get(tempBuffer.array(), 0, frameLength);
					
					writtenBytes = frameLength + writtenBytes;
					frameLength = Math.min(PAYLOAD_MAX_FRAME_LENGTH, payload.limit() - writtenBytes);
					
					isLastFrame = (frameLength <= 0); 
				
					// TODO: use RSV from first original frame?
					WebSocketFrameV13 frame = new WebSocketFrameV13(tempBuffer, getDirection(), isLastFrame, frameOpcode, 0);
					logger.info("forward modified frame");
					forwardFrame(frame, out);
					frameOpcode = OPCODE_CONTINUATION;
					
				} while (!isLastFrame);
			} else {
				for (WebSocketFrameV13 frame : receivedFrames) {
					if (!frame.isForwarded()) {
						logger.info("forward frame");
						forwardFrame(frame, out);
					}
				}
			}
			
			return true;
		}

		/**
		 * Helper method to forward frames.
		 * 
		 * @param frame
		 * @param out
		 */
		private void forwardFrame(WebSocketFrameV13 frame, OutputStream out) throws IOException {
			synchronized (out) {
				out.write(frame.getBuffer());
				out.flush();
			}
			
			frame.setForwarded(true);
		}
		
		@Override
		public byte[] getPayload() {
			if (!isFinished) {
				return new byte[0];
			}
			payload.rewind();
			byte[] bytes = new byte[payload.limit()];
			payload.get(bytes);
			return bytes;
		}

		@Override
		public void setPayload(byte[] newPayload) throws WebSocketException {
			if (!isFinished()) {
				throw new WebSocketException("Only allowed to set payload of finished message!");
			}
			
			if (!Arrays.equals(newPayload, getPayload())) {
				hasChanged = true;
				payload = ByteBuffer.wrap(newPayload);
			}
		}

		@Override
		public Integer getPayloadLength() {
			int length = payload.limit();
			
			if (opcode == OPCODE_CLOSE) {
				length = Math.max(0, length - 2);
			}
			
			return length;
		}

		@Override
		public String getReadablePayload() {
			try {
				isValidUtf8Payload = true;
				return Utf8Util.encodePayloadToUtf8(payload.array(), 0, payload.limit());
			} catch (InvalidUtf8Exception e) {
				isValidUtf8Payload  = false;
				return "<invalid UTF-8>";
			}
		}

		@Override
		public void setReadablePayload(String newReadablePayload) throws WebSocketException {
			if (!isFinished()) {
				throw new WebSocketException("Only allowed to set payload of finished message!");
			}
			
			String readablePayload = getReadablePayload();
			byte[] newBytesPayload = Utf8Util.decodePayloadFromUtf8(newReadablePayload);
			if (isValidUtf8Payload && !Arrays.equals(newBytesPayload, Utf8Util.decodePayloadFromUtf8(readablePayload))) {
				hasChanged = true;
				payload = ByteBuffer.wrap(newBytesPayload);
			}
		}

		@Override
		public Direction getDirection() {
			return direction;
		}
		
		@Override
		public WebSocketMessageDTO getDTO() {
			WebSocketMessageDTO message = super.getDTO();
			
			message.channel.id = getChannelId();
			message.id = getMessageId();
			
			return message;
		}
	}
}
