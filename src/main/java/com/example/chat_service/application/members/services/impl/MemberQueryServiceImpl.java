// chat_service/src/main/java/com/example/chat_service/application/members/services/impl/MemberQueryServiceImpl.java

package com.example.chat_service.application.members.services.impl;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.chat_service.application.members.services.MemberQueryServiceInterface;
import com.example.chat_service.domain.members.Member;
import com.example.chat_service.domain.members.exceptions.MemberDomainError;
import com.example.chat_service.domain.members.exceptions.MemberNotFoundError;
import com.example.chat_service.domain.members.repositories.MemberQueryRepository;

/**
 * Application-layer implementation of {@link MemberQueryServiceInterface}.
 *
 * <p>Orchestrates member query (read) operations by coordinating domain entities
 * with infrastructure query repositories. All methods run within a read-only
 * transaction boundary to optimize database access and ensure consistency.</p>
 *
 * <p><strong>Query pattern:</strong> All query methods accept IDs or filters as parameters,
 * delegate to {@link MemberQueryRepository}, apply read-side business logic if needed,
 * and return domain entities or projections. No state mutations occur.</p>
 *
 * <p><strong>CQRS read-side:</strong> This implementation focuses purely on read operations.
 * All queries automatically exclude members where {@code isLeft = true} unless explicitly
 * documented, ensuring only active participants are returned.</p>
 *
 * <p><strong>Design principles:</strong>
 * <ul>
 *   <li>Query methods accept IDs/filters — service delegates to repository, applies read logic</li>
 *   <li>All queries exclude {@code isLeft = true} members by default (active members only)</li>
 *   <li>Read-side projections and DTOs are handled via repository or mapped here</li>
 *   <li>No state mutations — this service is strictly for read operations</li>
 *   <li>All public methods are {@code @Transactional(readOnly = true)} for optimization</li>
 *   <li>Logging at DEBUG level for queries, WARN for not-found scenarios</li>
 * </ul></p>
 */
@Service
@Transactional(readOnly = true)
public class MemberQueryServiceImpl implements MemberQueryServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(MemberQueryServiceImpl.class);

    private final MemberQueryRepository memberQueryRepository;

    public MemberQueryServiceImpl(MemberQueryRepository memberQueryRepository) {
        this.memberQueryRepository = memberQueryRepository;
    }

    // ── Single Entity Queries (Active Members Only) ──────────────────

    @Override
    public Optional<Member> getMemberById(UUID memberId) {
        try {
            Optional<Member> result = memberQueryRepository.findById(memberId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Retrieved member by ID: member_id={}, user_id={}, room_id={}, status={}",
                    result.get().id(),
                    result.get().userId(),
                    result.get().roomId(),
                    result.get().status()
                );
            } else {
                logger.debug("No active member found by ID: member_id={}", memberId);
            }
            
            return result;

        } catch (MemberNotFoundError e) {
            logger.debug("Member not found by ID: member_id={}", memberId);
            return Optional.empty();

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving member by ID (member_id={}): {}",
                memberId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error retrieving member by ID: member_id={}", memberId, e);
            throw e;
        }
    }

    @Override
    public Optional<Member> getMemberByUserIdAndRoomId(UUID userId, UUID roomId) {
        try {
            Optional<Member> result = memberQueryRepository.findByUserIdAndRoomId(userId, roomId);
            
            if (result.isPresent()) {
                logger.debug(
                    "Retrieved member by user+room: user_id={}, room_id={}, member_id={}, status={}",
                    userId,
                    roomId,
                    result.get().id(),
                    result.get().status()
                );
            } else {
                logger.debug(
                    "No active member found by user+room: user_id={}, room_id={}",
                    userId,
                    roomId
                );
            }
            
            return result;

        } catch (MemberNotFoundError e) {
            logger.debug(
                "Member not found by user+room: user_id={}, room_id={}",
                userId,
                roomId
            );
            return Optional.empty();

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving member by user+room (user_id={}, room_id={}): {}",
                userId,
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving member by user+room: user_id={}, room_id={}",
                userId,
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public boolean isUserActiveMember(UUID userId, UUID roomId) {
        try {
            boolean isActive = memberQueryRepository.isActiveMember(userId, roomId);
            logger.debug(
                "Active membership check: user_id={}, room_id={}, is_active={}",
                userId,
                roomId,
                isActive
            );
            return isActive;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error checking active membership (user_id={}, room_id={}): {}",
                userId,
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking active membership: user_id={}, room_id={}",
                userId,
                roomId,
                e
            );
            throw e;
        }
    }

    // ── Bulk Queries by Room (Active Members Only) ───────────────────

    @Override
    public List<Member> getAllActiveMembersByRoomId(UUID roomId) {
        try {
            List<Member> members = memberQueryRepository.findAllActiveByRoomId(roomId);
            logger.debug(
                "Retrieved {} active members for room: room_id={}",
                members.size(),
                roomId
            );
            return members;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving active members by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error retrieving active members by room: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public List<Member> getActiveAdminsByRoomId(UUID roomId) {
        try {
            List<Member> admins = memberQueryRepository.findActiveAdminsByRoomId(roomId);
            logger.debug(
                "Retrieved {} active admin members for room: room_id={}",
                admins.size(),
                roomId
            );
            return admins;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving active admins by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error retrieving active admins by room: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public List<Member> getActiveUsersByRoomId(UUID roomId) {
        try {
            List<Member> users = memberQueryRepository.findActiveUsersByRoomId(roomId);
            logger.debug(
                "Retrieved {} active user members for room: room_id={}",
                users.size(),
                roomId
            );
            return users;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving active users by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error retrieving active users by room: room_id={}", roomId, e);
            throw e;
        }
    }

    @Override
    public long countActiveMembersByRoomId(UUID roomId) {
        try {
            long count = memberQueryRepository.countActiveByRoomId(roomId);
            logger.debug("Counted {} active members for room: room_id={}", count, roomId);
            return count;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error counting active members by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error counting active members by room: room_id={}", roomId, e);
            throw e;
        }
    }

    // ── Bulk Queries by User (Active Memberships Only) ───────────────

    @Override
    public List<Member> getAllActiveMembershipsByUserId(UUID userId) {
        try {
            List<Member> memberships = memberQueryRepository.findAllActiveByUserId(userId);
            logger.debug(
                "Retrieved {} active memberships for user: user_id={}",
                memberships.size(),
                userId
            );
            return memberships;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving active memberships by user (user_id={}): {}",
                userId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error retrieving active memberships by user: user_id={}", userId, e);
            throw e;
        }
    }

    @Override
    public List<Member> getActiveAdminMembershipsByUserId(UUID userId) {
        try {
            List<Member> adminMemberships = memberQueryRepository.findActiveAdminMembershipsByUserId(userId);
            logger.debug(
                "Retrieved {} active admin memberships for user: user_id={}",
                adminMemberships.size(),
                userId
            );
            return adminMemberships;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving active admin memberships by user (user_id={}): {}",
                userId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving active admin memberships by user: user_id={}",
                userId,
                e
            );
            throw e;
        }
    }

    @Override
    public long countActiveMembershipsByUserId(UUID userId) {
        try {
            long count = memberQueryRepository.countActiveByUserId(userId);
            logger.debug("Counted {} active memberships for user: user_id={}", count, userId);
            return count;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error counting active memberships by user (user_id={}): {}",
                userId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error("Unexpected error counting active memberships by user: user_id={}", userId, e);
            throw e;
        }
    }

    // ── Batch Lookup Queries (Active Members Only) ───────────────────

    @Override
    public List<Member> getActiveMembersByUserIdsInRoom(Collection<UUID> userIds, UUID roomId) {
        try {
            List<Member> members = memberQueryRepository.findActiveByUserIdsInRoom(userIds, roomId);
            logger.debug(
                "Bulk retrieved {} active members for {} users in room: room_id={}",
                members.size(),
                userIds.size(),
                roomId
            );
            return members;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error bulk retrieving members by user IDs in room (room_id={}, user_count={}): {}",
                roomId,
                userIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk retrieving members by user IDs in room: room_id={}, user_count={}",
                roomId,
                userIds.size(),
                e
            );
            throw e;
        }
    }

    @Override
    public List<Member> getActiveMembersByIds(Collection<UUID> memberIds) {
        try {
            List<Member> members = memberQueryRepository.findActiveByIds(memberIds);
            logger.debug(
                "Bulk retrieved {} active members for {} requested IDs",
                members.size(),
                memberIds.size()
            );
            return members;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error bulk retrieving members by IDs (requested_count={}): {}",
                memberIds.size(),
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error bulk retrieving members by IDs: requested_count={}",
                memberIds.size(),
                e
            );
            throw e;
        }
    }

    // ── Projection Queries (Lightweight Read Models) ─────────────────

    @Override
    public List<MemberSummary> getActiveMemberSummariesByRoomId(UUID roomId) {
        try {
            List<MemberQueryRepository.MemberSummary> repoSummaries = 
                memberQueryRepository.findActiveSummariesByRoomId(roomId);
            
            // Map repository projection to application-layer projection
            List<MemberSummary> summaries = repoSummaries.stream()
                .map(repo -> new MemberSummary(
                    repo.memberId(),
                    repo.userId(),
                    repo.roomId(),
                    repo.status(),
                    repo.joinedAt()
                ))
                .toList();
            
            logger.debug(
                "Retrieved {} active member summaries for room: room_id={}",
                summaries.size(),
                roomId
            );
            return summaries;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving member summaries by room (room_id={}): {}",
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving member summaries by room: room_id={}",
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public List<MemberSummary> getActiveMemberSummariesByUserId(UUID userId) {
        try {
            List<MemberQueryRepository.MemberSummary> repoSummaries = 
                memberQueryRepository.findActiveSummariesByUserId(userId);
            
            // Map repository projection to application-layer projection
            List<MemberSummary> summaries = repoSummaries.stream()
                .map(repo -> new MemberSummary(
                    repo.memberId(),
                    repo.userId(),
                    repo.roomId(),
                    repo.status(),
                    repo.joinedAt()
                ))
                .toList();
            
            logger.debug(
                "Retrieved {} active member summaries for user: user_id={}",
                summaries.size(),
                userId
            );
            return summaries;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving member summaries by user (user_id={}): {}",
                userId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving member summaries by user: user_id={}",
                userId,
                e
            );
            throw e;
        }
    }

    // ── Read-Side Utility Queries ────────────────────────────────────

    @Override
    public boolean activeMemberExists(UUID memberId) {
        try {
            boolean exists = memberQueryRepository.findById(memberId).isPresent();
            logger.debug("Active member existence check: member_id={}, exists={}", memberId, exists);
            return exists;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error checking active member existence (member_id={}): {}",
                memberId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking active member existence: member_id={}",
                memberId,
                e
            );
            throw e;
        }
    }

    @Override
    public boolean activeMemberExistsByUserAndRoom(UUID userId, UUID roomId) {
        try {
            boolean exists = memberQueryRepository.findByUserIdAndRoomId(userId, roomId).isPresent();
            logger.debug(
                "Active member existence check by user+room: user_id={}, room_id={}, exists={}",
                userId,
                roomId,
                exists
            );
            return exists;

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error checking active member existence by user+room (user_id={}, room_id={}): {}",
                userId,
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error checking active member existence by user+room: user_id={}, room_id={}",
                userId,
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public Member.Status getMemberStatusInRoom(UUID userId, UUID roomId) {
        try {
            Optional<Member> member = memberQueryRepository.findByUserIdAndRoomId(userId, roomId);
            
            if (member.isPresent()) {
                Member.Status status = member.get().status();
                logger.debug(
                    "Retrieved member status: user_id={}, room_id={}, status={}",
                    userId,
                    roomId,
                    status
                );
                return status;
            } else {
                logger.debug(
                    "No active member found for status check: user_id={}, room_id={}",
                    userId,
                    roomId
                );
                return null;
            }

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving member status (user_id={}, room_id={}): {}",
                userId,
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving member status: user_id={}, room_id={}",
                userId,
                roomId,
                e
            );
            throw e;
        }
    }

    @Override
    public int getUnreadMessageCount(UUID userId, UUID roomId) {
        try {
            Optional<Member> member = memberQueryRepository.findByUserIdAndRoomId(userId, roomId);
            
            if (member.isPresent()) {
                int unreadCount = member.get().unreadMessages();
                logger.debug(
                    "Retrieved unread count: user_id={}, room_id={}, unread={}",
                    userId,
                    roomId,
                    unreadCount
                );
                return unreadCount;
            } else {
                logger.debug(
                    "No active member found for unread count: user_id={}, room_id={}, returning 0",
                    userId,
                    roomId
                );
                return 0;
            }

        } catch (MemberDomainError e) {
            logger.warn(
                "Domain error retrieving unread count (user_id={}, room_id={}): {}",
                userId,
                roomId,
                e.getMessage()
            );
            throw e;

        } catch (Exception e) {
            logger.error(
                "Unexpected error retrieving unread count: user_id={}, room_id={}",
                userId,
                roomId,
                e
            );
            throw e;
        }
    }
}