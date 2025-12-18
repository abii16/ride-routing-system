package common;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Standard message format for all communications in the distributed system.
 * Uses JSON serialization for language-agnostic communication.
 */
public class Message {
    private MessageType type;
    private Map<String, Object> payload;
    private String requestId;
    private long timestamp;
    
    // Constructors
    public Message() {
        this.payload = new HashMap<>();
        this.requestId = UUID.randomUUID().toString();
        this.timestamp = System.currentTimeMillis();
    }
    
    public Message(MessageType type) {
        this();
        this.type = type;
    }
    
    public Message(MessageType type, Map<String, Object> payload) {
        this(type);
        this.payload = payload;
    }
    
    // Getters and Setters
    public MessageType getType() {
        return type;
    }
    
    public void setType(MessageType type) {
        this.type = type;
    }
    
    public Map<String, Object> getPayload() {
        return payload;
    }
    
    public void setPayload(Map<String, Object> payload) {
        this.payload = payload;
    }
    
    public String getRequestId() {
        return requestId;
    }
    
    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }
    
    // Helper methods
    public void addPayload(String key, Object value) {
        this.payload.put(key, value);
    }
    
    public Object getPayloadValue(String key) {
        return this.payload.get(key);
    }
    
    public String getPayloadString(String key) {
        Object value = this.payload.get(key);
        return value != null ? value.toString() : null;
    }
    
    public Integer getPayloadInt(String key) {
        Object value = this.payload.get(key);
        if (value instanceof Integer) {
            return (Integer) value;
        } else if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public Double getPayloadDouble(String key) {
        Object value = this.payload.get(key);
        if (value instanceof Double) {
            return (Double) value;
        } else if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }
    
    public Boolean getPayloadBoolean(String key) {
        Object value = this.payload.get(key);
        if (value instanceof Boolean) {
            return (Boolean) value;
        } else if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return null;
    }
    
    @Override
    public String toString() {
        return "Message{" +
                "type=" + type +
                ", payload=" + payload +
                ", requestId='" + requestId + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
