// chat_service/src/main/java/com/example/chat_service/application/messages/handlers/MessageQueryHandler.java

package com.example.chat_service.application.messages.handlers;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.example.chat_service.application.messages.handlers.dtos.MessageQueryResponseDTO;
import com.example.chat_service.application.messages.services.MessageCommandServiceInterface;
import com.example.chat_service.application.messages.services.MessageQueryServiceInterface;
import com.example.chat_service.application.members.services.MemberCommandServiceInterface;
import com.example.chat_service.domain.members.MemberAggregate;
import com.example.chat_service.domain.messages.Message;
import com.example.chat_service.external.users.dtos.UserView;
import com.example.chat_service.external.users.dtos.users.services.UserApiClient;

/**
 * Application-layer orchestrator for message query operations.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li>Query messages via {@link MessageQueryServiceInterface}</li>
 *   <li>Enrich message data with external user info via {@link UserApiClient}</li>
 *   <li>Build reply context (ParentPreview) for threaded conversations</li>
 *   <li>Construct {@link MessageQueryResponseDTO} for API responses</li>
 *   <li>Calculate {@code is_mine} flag based on requester_id vs sender_id</li>
 *   <li>Automatically mark all messages as read/seen for the requester when querying a room</li>
 * </ul>
 * </p>
 *
 * <p><strong>DOES NOT:</strong>
 * <ul>
 *   <li>Contain domain business rules (delegated to domain entities)</li>
 *   <li>Directly access database (delegated to Repositories via Services)</li>
 *   <li>Build absolute URLs — that's handled at the controller layer via MediaUrlService</li>
 * </ul>
 * </p>
 *
 * <p><strong>Architecture note:</strong> This handler returns DTOs with RELATIVE image paths
 * (as stored in the domain/database). The controller layer is responsible for converting
 * these to absolute URLs using {@code MediaUrlService.buildMediaUrl(HttpServletRequest, String)}.
 * This mirrors the pattern used in {@code MessageCommandHandler}.</p>
 *
 * <p><strong>Reply preview logic:</strong> When a message is a reply, the handler fetches
 * the parent message to build a {@code ParentPreview} with image-over-text priority:
 * if the parent has an image, only the image is shown; otherwise, the text content is shown.</p>
 *
 * <p><strong>Personalization:</strong> When {@code requester_id} matches the message {@code sender_id},
 * the {@code sender_username} field is set to "You" for personalized UX.</p>
 *
 * <p><strong>Read receipt automation:</strong> When querying messages for a room, the handler:
 * <ol>
 *   <li>Loads the member aggregate and calls {@code markAllRead} to clear unread counts at member level</li>
 *   <li>Iterates through each loaded message and calls {@code markAsSeen} to update individual message state</li>
 * </ol>
 * This ensures that loading messages implies the user has seen them at both aggregate and entity levels.</p>
 */
@Component
public class MessageQueryHandler {

    private static final Logger logger =
            LoggerFactory.getLogger(MessageQueryHandler.class);

    private final MessageQueryServiceInterface messageQueryService;
    private final MessageCommandServiceInterface messageCommandService;
    private final UserApiClient userApiClient;
    private final MemberCommandServiceInterface memberCommandService;

    /**
     * Constructor injection — Spring will auto-wire all dependencies
     * because they're annotated with @Component or @Service.
     *
     * @param messageQueryService handles read-side message queries
     * @param messageCommandService handles write-side message commands (e.g., markAsSeen)
     * @param userApiClient fetches user data from external Auth Service
     * @param memberCommandService handles member command operations (e.g., markAllRead)
     */
    public MessageQueryHandler(
            MessageQueryServiceInterface messageQueryService,
            MessageCommandServiceInterface messageCommandService,
            UserApiClient userApiClient,
            MemberCommandServiceInterface memberCommandService
    ) {
        this.messageQueryService = messageQueryService;
        this.messageCommandService = messageCommandService;
        this.userApiClient = userApiClient;
        this.memberCommandService = memberCommandService;
    }

    // ─────────────────────────────────────────────────────────────────
    // QUERY MESSAGES BY ROOM (Active Messages Only)
    // ─────────────────────────────────────────────────────────────────

    /**
     * Retrieve all active messages in a specific room, enriched for API response.
     *
     * <p>Flow:
     * <ol>
     *   <li>Load member aggregate for user+room via memberCommandService</li>
     *   <li>Mark all messages as read at member level via markAllRead (clears unread count)</li>
     *   <li>Query active messages by room_id via messageQueryService</li>
     *   <li>For each message, call markAsSeen to update individual message state</li>
     *   <li>For each message, fetch sender username and profile image from Auth Service</li>
     *   <li>If message is a reply, fetch parent message and build ParentPreview</li>
     *   <li>Calculate is_mine by comparing requester_id with message sender_id</li>
     *   <li>Build MessageQueryResponseDTO for each message with enriched data</li>
     *   <li>Return list of DTOs ready for HTTP response (with relative image paths)</li>
     * </ol>
     * </p>
     *
     * <p><strong>Image URL note:</strong> The returned DTOs contain RELATIVE image paths
     * (e.g., {@code /uploads/messages/abc.jpg}). The controller layer should convert
     * these to absolute URLs using {@code MediaUrlService.buildMediaUrl(HttpServletRequest, String)}
     * before sending the HTTP response.</p>
     *
     * <p><strong>Personalization note:</strong> When {@code requester_id} matches a message's
     * {@code sender_id}, that message's {@code sender_username} will be "You" for personalized UX.</p>
     *
     * <p><strong>Reply preview logic:</strong>
     * <ul>
     *   <li>If parent has image: {@code parent_preview.image_url} is set, {@code parent_preview.content} is null</li>
     *   <li>If parent has no image: {@code parent_preview.content} is set, {@code parent_preview.image_url} is null</li>
     * </ul>
     * </p>
     *
     * <p><strong>Read receipt note:</strong> This method automatically marks all messages as read/seen
     * for the requester before returning them. Both member-level (markAllRead) and message-level
     * (markAsSeen) operations are performed. If any read receipt operation fails, the error is
     * logged but the message query continues (fail-soft behavior).</p>
     *
     * @param roomId the room UUID to query messages from
     * @param requesterId the UUID of the user requesting the messages (for is_mine calculation)
     * @return List of MessageQueryResponseDTO ready for HTTP response (with relative image paths)
     */
    public List<MessageQueryResponseDTO> getAllActiveMessagesByRoomId(
            UUID roomId,
            UUID requesterId
    ) {
        logger.info(
                "Querying active messages for room: room_id={}, requester_id={}",
                roomId, requesterId
        );

        // ─────────────────────────────────────────────
        // 0a. Mark all messages as read at MEMBER level
        //    - Load member aggregate, then call markAllRead
        //    - This clears the unread count for the member in this room
        //    - Fail-soft: log errors but continue with message query
        // ─────────────────────────────────────────────
        try {
            MemberAggregate memberAggregate = memberCommandService.loadAggregateByUserAndRoom(requesterId, roomId);
            memberCommandService.markAllRead(memberAggregate.member().id(), requesterId);
            logger.debug(
                    "Successfully marked all messages as read at member level: user_id={}, room_id={}, member_id={}",
                    requesterId, roomId, memberAggregate.member().id()
            );
        } catch (Exception e) {
            logger.warn(
                    "Failed to mark messages as read at member level (non-blocking): user_id={}, room_id={}, error={}",
                    requesterId, roomId, e.getMessage()
            );
        }

        // ─────────────────────────────────────────────
        // 1. Query active messages from service layer
        //    - Returns domain Message entities (active only, excludes deleted)
        // ─────────────────────────────────────────────
        List<Message> messages = messageQueryService.getAllActiveMessagesByRoomId(roomId);
        logger.debug(
                "Retrieved {} active messages for room: room_id={}",
                messages.size(), roomId
        );

        // ─────────────────────────────────────────────
        // 0b. Mark each individual message as SEEN at MESSAGE level
        //    - Iterate through loaded messages and call markAsSeen on each
        //    - This updates the seen_at timestamp and status for each message entity
        //    - Fail-soft: log errors per message but continue processing
        // ─────────────────────────────────────────────
        for (Message message : messages) {
            try {
                messageCommandService.markAsSeen(message.id(), requesterId);
                logger.trace(
                        "Marked individual message as seen: message_id={}, room_id={}, user_id={}",
                        message.id(), roomId, requesterId
                );
            } catch (Exception e) {
                logger.warn(
                        "Failed to mark individual message as seen (non-blocking): message_id={}, room_id={}, user_id={}, error={}",
                        message.id(), roomId, requesterId, e.getMessage()
                );
            }
        }

        // ─────────────────────────────────────────────
        // 2. Enrich each message with external data and build DTOs
        // ─────────────────────────────────────────────
        List<MessageQueryResponseDTO> responses = new ArrayList<>(messages.size());
        
        for (Message message : messages) {
            try {
                // Fetch sender data from Auth Service
                UserView sender = fetchUserView(message.senderId());
                String senderUsername = sender.username();
                String senderProfileImage = sender.profilePicture();

                // Build parent preview if this is a reply
                MessageQueryResponseDTO.ParentPreview parentPreview = null;
                if (message.isReply() && message.parentId() != null) {
                    parentPreview = buildParentPreview(message.parentId());
                }

                // Build response DTO with enriched data
                // - is_mine is auto-calculated by comparing requesterId with message.senderId()
                // - imageUrl contains RELATIVE path (controller converts to absolute)
                MessageQueryResponseDTO response = MessageQueryResponseDTO.fromMessage(
                        message,
                        senderUsername,
                        senderProfileImage,
                        parentPreview,
                        requesterId  // for is_mine calculation and "You" personalization
                );

                responses.add(response);

            } catch (Exception e) {
                // Log but continue processing other messages
                // A single message enrichment failure shouldn't break the entire list
                logger.warn(
                        "Failed to enrich message for response: message_id={}, room_id={}, error={}",
                        message.id(), roomId, e.getMessage()
                );
            }
        }

        logger.info(
                "Successfully built {} message response DTOs for room: room_id={}, requester_id={}",
                responses.size(), roomId, requesterId
        );

        return responses;
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPER METHODS
    // ─────────────────────────────────────────────────────────────────

    /**
     * Fetch full UserView for a user ID from external Auth Service.
     *
     * @param userId the user ID to fetch
     * @return UserView with username, profilePicture, etc.
     */
    private UserView fetchUserView(UUID userId) {
        try {
            return userApiClient.getUserById(userId);
        } catch (Exception e) {
            logger.warn("Failed to fetch UserView for user_id={}: {}", userId, e.getMessage());
            // Fallback: create minimal UserView with just username
            return new UserView(
                    userId,
                    "user_" + userId.toString().substring(0, 8),
                    null, null, null, null
            );
        }
    }

    /**
     * Build ParentPreview for a reply message by fetching the parent message
     * and its creator's username from the Auth Service.
     *
     * <p>Applies image-over-text priority: if parent has image, only image is shown;
     * otherwise, content is shown.</p>
     *
     * @param parentId the UUID of the parent message
     * @return ParentPreview with appropriate content/image based on priority rules, or null if parent not found
     */
    private MessageQueryResponseDTO.ParentPreview buildParentPreview(UUID parentId) {
        try {
            // Load parent message from service layer
            Optional<Message> parentOpt = messageQueryService.getMessageById(parentId);
            
            if (parentOpt.isEmpty()) {
                logger.warn("Parent message not found for reply preview: parent_id={}", parentId);
                return null;
            }
            
            Message parent = parentOpt.get();
            
            // Fetch parent creator's username
            String parentCreatorUsername = fetchUserView(parent.senderId()).username();
            
            // Build preview with image-over-text priority using factory method
            return MessageQueryResponseDTO.ParentPreview.fromMessage(
                    parent,
                    parentCreatorUsername
            );
            
        } catch (Exception e) {
            logger.warn("Failed to build parent preview for parent_id={}: {}", parentId, e.getMessage());
            return null;
        }
    }
}