package com.varutra.websockets;

public class WebSocketChannelDTO implements Comparable<WebSocketChannelDTO> {
	
	public Long id;
	
	public String host;
	
	public Integer port;
	
	public String url;
	
	public Long startTimestamp;
	
	public Long endTimestamp;
	
	public long historyId;

	public WebSocketChannelDTO() {
		
	}
	
	public WebSocketChannelDTO(String host) {
		this.host = host;
	}

	public boolean isConnected() {
		if (startTimestamp != null && endTimestamp == null) {
			return true;
		}
		return false;
	}
	
	 
	public String getContextUrl() {
		if (url == null) {
			return null;
		}
		
		if (url.indexOf("?") != -1) {		
    		url = url.substring(0, url.indexOf("?"));
		}
		
		return url.replaceFirst("/$", "");
	}

	@Override
	public String toString() {
		if (port != null && id != null) {
			return host + ":" + port + " (#" + id + ")";
		}
		return host;
	}

	@Override
	public boolean equals(Object object) {
		if (this == object) {
			return true;
		}
		if (object == null) {
			return false;
		}
		if (getClass() != object.getClass()) {
			return false;
		}
		WebSocketChannelDTO other = (WebSocketChannelDTO) object;
		if (id == null) {
			if (other.id != null) {
				return false;
			}
		} else if (!id.equals(other.id)) {
			return false;
		}
		return true;
	}

	@Override
	public int hashCode() {
		return 31 + ((id == null) ? 0 : id.hashCode());
	}

	@Override
	public int compareTo(WebSocketChannelDTO other) {
		int result = host.compareTo(other.host);

		if (result == 0) {
			result = port.compareTo(other.port);
			if (result == 0) {
				return id.compareTo(other.id);
			}
		}

		return result;
	}

	public String getFullUri() {
    	StringBuilder regex = new StringBuilder();
    	if (url.matches(".*[^:/]/.*")) {
    		String wsUri = url.replaceFirst("([^:/])/", "$1:" + port + "/");
    		
    		wsUri = wsUri.replaceFirst("http(s?://)", "ws$1");
    		regex.append(wsUri);
    	} else {
    		if (port == 80) {
    			regex.append("ws://");
    		} else {
        		regex.append("wss://");
    		}
    		regex.append(host);
    		regex.append(":");
    		regex.append(port);
    	}
		return regex.toString();
	}
}