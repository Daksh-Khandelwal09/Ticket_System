package servicedesk;

/**
 * Life cycle of a ticket:
 * OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED
 */
public enum Status {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
    CLOSED
}
