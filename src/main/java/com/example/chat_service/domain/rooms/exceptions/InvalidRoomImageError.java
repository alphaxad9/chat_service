// chat_service/src/main/java/com/example/chat_service/domain/rooms/exceptions/InvalidRoomImageError.java
package com.example.chat_service.domain.rooms.exceptions;
import java.util.UUID;

public class InvalidRoomImageError extends RoomDomainError {
    private final String roomId;
    private final String imageType; // "cover" or "profile"
    private final String providedUrl;
    private final String reason;

    public InvalidRoomImageError(UUID roomId, String imageType, String providedUrl, String reason, String message) {
        super(message != null ? message : reason + ": room_id=" + roomId + ", image_type=" + imageType + ", url='" + providedUrl + "'");
        this.roomId = roomId != null ? roomId.toString() : null;
        this.imageType = imageType;
        this.providedUrl = providedUrl;
        this.reason = reason;
    }
    public InvalidRoomImageError(UUID roomId, String imageType, String providedUrl, String reason) { this(roomId, imageType, providedUrl, reason, null); }
    public InvalidRoomImageError(String imageType, String providedUrl, String reason) { this(null, imageType, providedUrl, reason, null); }
    public String getRoomId() { return roomId; }
    public String getImageType() { return imageType; }
    public String getProvidedUrl() { return providedUrl; }
    public String getReason() { return reason; }
}