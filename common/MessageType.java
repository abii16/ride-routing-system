package common;

/**
 * Enumeration of all message types in the distributed ride-sharing system.
 * These are used for communication between clients and services.
 */
public enum MessageType {
    // Authentication & Registration
    REGISTER_PASSENGER,
    REGISTER_DRIVER,
    LOGIN,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    
    // Location Updates
    UPDATE_LOCATION,
    LOCATION_UPDATED,
    
    // Ride Management (Passenger)
    RIDE_REQUEST,
    RIDE_ASSIGNMENT,
    RIDE_ACCEPTED,
    RIDE_REJECTED,
    RIDE_STARTED,
    RIDE_COMPLETED,
   RIDE_CANCELLED,
    
    // Driver Management
    UPDATE_AVAILABILITY,
    AVAILABILITY_UPDATED,
    GET_AVAILABLE_DRIVERS,
    AVAILABLE_DRIVERS_LIST,
    ASSIGN_DRIVER,
    DRIVER_ASSIGNED,
    
    // Database Operations
    DB_CREATE_RIDE,
    DB_UPDATE_RIDE,
    DB_GET_RIDE,
    DB_INSERT_PASSENGER,
    DB_INSERT_DRIVER,
    DB_UPDATE_DRIVER_LOCATION,
    DB_UPDATE_PASSENGER_LOCATION,
    DB_VALIDATE_LOGIN,
    DB_GET_ACTIVE_RIDES,
    DB_RESPONSE,
    
    // System Messages
    HEARTBEAT,
    DISCONNECT,
    ERROR,
    NO_DRIVERS_AVAILABLE,
    DRIVER_DISCONNECTED,
    SERVER_SHUTDOWN,
    
    // Web/API Messages
    GET_LOCATIONS,
    LOCATIONS_DATA,
    
    // Synchronization
    SYNC_REQUEST,
    SYNC_DATA_RESPONSE
}
