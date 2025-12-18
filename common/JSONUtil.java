package common;

import java.util.HashMap;
import java.util.Map;

/**
 * Simple JSON utility for serializing and deserializing Message objects.
 * Uses manual JSON parsing to avoid external dependencies.
 */
public class JSONUtil {
    
    /**
     * Convert a Message object to JSON string
     */
    public static String toJSON(Message message) {
        StringBuilder json = new StringBuilder();
        json.append("{");
        
        // Add type
        json.append("\"type\":\"").append(message.getType()).append("\",");
        
        // Add payload
        json.append("\"payload\":{");
        Map<String, Object> payload = message.getPayload();
        int count = 0;
        for (Map.Entry<String, Object> entry : payload.entrySet()) {
            if (count > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append(valueToJSON(entry.getValue()));
            count++;
        }
        json.append("},");
        
        // Add requestId
        json.append("\"requestId\":\"").append(message.getRequestId()).append("\",");
        
        // Add timestamp
        json.append("\"timestamp\":").append(message.getTimestamp());
        
        json.append("}");
        return json.toString();
    }
    
    public static String toJSON(java.util.List<Map<String, Object>> list) {
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < list.size(); i++) {
            if (i > 0) json.append(",");
            json.append(mapToJSON(list.get(i)));
        }
        json.append("]");
        return json.toString();
    }

    public static String mapToJSON(Map<String, Object> map) {
        StringBuilder json = new StringBuilder("{");
        int count = 0;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (count > 0) json.append(",");
            json.append("\"").append(entry.getKey()).append("\":");
            json.append(valueToJSON(entry.getValue()));
            count++;
        }
        json.append("}");
        return json.toString();
    }
    
    /**
     * Convert a value to JSON representation
     */

    private static String valueToJSON(Object value) {
        if (value == null) {
            return "null";
        } else if (value instanceof String) {
            return "\"" + escapeJSONString((String) value) + "\"";
        } else if (value instanceof Number || value instanceof Boolean) {
            return value.toString();
        } else {
            return "\"" + escapeJSONString(value.toString()) + "\"";
        }
    }
    
    /**
     * Escape special characters in JSON strings
     */
    private static String escapeJSONString(String str) {
        return str.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }
    
    /**
     * Parse JSON string to Message object
     */
    public static Message fromJSON(String json) {
        try {
            Message message = new Message();
            
            // Remove outer braces
            json = json.trim();
            if (json.startsWith("{")) json = json.substring(1);
            if (json.endsWith("}")) json = json.substring(0, json.length() - 1);
            
            // Split by commas (simple parser, doesn't handle nested objects)
            String[] parts = splitJSON(json);
            
            for (String part : parts) {
                String[] keyValue = part.split(":", 2);
                if (keyValue.length != 2) continue;
                
                String key = keyValue[0].trim().replace("\"", "");
                String value = keyValue[1].trim();
                
                if (key.equals("type")) {
                    String typeStr = value.replace("\"", "");
                    message.setType(MessageType.valueOf(typeStr));
                } else if (key.equals("payload")) {
                    Map<String, Object> payload = parsePayload(value);
                    message.setPayload(payload);
                } else if (key.equals("requestId")) {
                    message.setRequestId(value.replace("\"", ""));
                } else if (key.equals("timestamp")) {
                    message.setTimestamp(Long.parseLong(value));
                }
            }
            
            return message;
        } catch (Exception e) {
            System.err.println("Error parsing JSON: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
    
    /**
     * Split JSON by commas, respecting nested structures
     */
    private static String[] splitJSON(String json) {
        java.util.List<String> parts = new java.util.ArrayList<>();
        int braceCount = 0;
        int bracketCount = 0;
        boolean inString = false;
        StringBuilder current = new StringBuilder();
        
        for (int i = 0; i < json.length(); i++) {
            char c = json.charAt(i);
            
            if (c == '"' && (i == 0 || json.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            
            if (!inString) {
                if (c == '{') braceCount++;
                if (c == '}') braceCount--;
                if (c == '[') bracketCount++;
                if (c == ']') bracketCount--;
                
                if (c == ',' && braceCount == 0 && bracketCount == 0) {
                    parts.add(current.toString());
                    current = new StringBuilder();
                    continue;
                }
            }
            
            current.append(c);
        }
        
        if (current.length() > 0) {
            parts.add(current.toString());
        }
        
        return parts.toArray(new String[0]);
    }
    
    /**
     * Parse payload object
     */
    private static Map<String, Object> parsePayload(String payloadStr) {
        Map<String, Object> payload = new HashMap<>();
        
        // Remove outer braces
        payloadStr = payloadStr.trim();
        if (payloadStr.startsWith("{")) payloadStr = payloadStr.substring(1);
        if (payloadStr.endsWith("}")) payloadStr = payloadStr.substring(0, payloadStr.length() - 1);
        
        if (payloadStr.trim().isEmpty()) return payload;
        
        String[] parts = splitJSON(payloadStr);
        
        for (String part : parts) {
            String[] keyValue = part.split(":", 2);
            if (keyValue.length != 2) continue;
            
            String key = keyValue[0].trim().replace("\"", "");
            String value = keyValue[1].trim();
            
            // Parse value type
            if (value.equals("null")) {
                payload.put(key, null);
            } else if (value.equals("true") || value.equals("false")) {
                payload.put(key, Boolean.parseBoolean(value));
            } else if (value.startsWith("\"") && value.endsWith("\"")) {
                payload.put(key, value.substring(1, value.length() - 1));
            } else {
                // Try to parse as number
                try {
                    if (value.contains(".")) {
                        payload.put(key, Double.parseDouble(value));
                    } else {
                        payload.put(key, Integer.parseInt(value));
                    }
                } catch (NumberFormatException e) {
                    payload.put(key, value);
                }
            }
        }
        
        return payload;
    }
    
    /**
     * Create error message
     */
    public static Message createErrorMessage(String errorMsg) {
        Message message = new Message(MessageType.ERROR);
        message.addPayload("error", errorMsg);
        return message;
    }
    
    /**
     * Create success message
     */
    public static Message createSuccessMessage(MessageType type) {
        return new Message(type);
    }
}
