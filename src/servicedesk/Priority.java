package servicedesk;

/**
 * How urgent a ticket is. Each priority has an SLA (the number of hours
 * within which the ticket must be resolved).
 */
public enum Priority {
    LOW(72),
    MEDIUM(24),
    HIGH(8),
    CRITICAL(4);

    private final int slaHours;

    Priority(int slaHours) {
        this.slaHours = slaHours;
    }

    public int getSlaHours() {
        return slaHours;
    }
}
