package servicedesk;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * One support ticket raised by an end user.
 */
public class Ticket {

    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm");

    private final int id;
    private final String title;
    private final String description;
    private final String category;
    private final Priority priority;
    private final LocalDateTime createdAt;

    private Status status;
    private String assignedTo;      // empty string means "not assigned yet"
    private String resolution;      // what fixed the problem
    private LocalDateTime resolvedAt;

    /** Used when a brand new ticket is created. */
    public Ticket(int id, String title, String description, String category, Priority priority) {
        this(id, title, description, category, priority,
                Status.OPEN, "", "", LocalDateTime.now(), null);
    }

    /** Used when a ticket is loaded from the file (or for demo data). */
    public Ticket(int id, String title, String description, String category, Priority priority,
                  Status status, String assignedTo, String resolution,
                  LocalDateTime createdAt, LocalDateTime resolvedAt) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.category = category;
        this.priority = priority;
        this.status = status;
        this.assignedTo = assignedTo;
        this.resolution = resolution;
        this.createdAt = createdAt;
        this.resolvedAt = resolvedAt;
    }

    // ---------- Actions that change the ticket ----------

    public void assignTo(String agent) {
        if (status == Status.CLOSED) {
            throw new IllegalStateException("Ticket #" + id + " is already closed.");
        }
        this.assignedTo = agent;
    }

    public void startWork() {
        if (status != Status.OPEN) {
            throw new IllegalStateException("Only OPEN tickets can be started. Current status: " + status);
        }
        if (assignedTo.isEmpty()) {
            throw new IllegalStateException("Assign the ticket to an agent before starting work.");
        }
        this.status = Status.IN_PROGRESS;
    }

    public void resolve(String resolutionNotes) {
        if (status != Status.IN_PROGRESS) {
            throw new IllegalStateException("Only IN_PROGRESS tickets can be resolved. Current status: " + status);
        }
        this.resolution = resolutionNotes;
        this.resolvedAt = LocalDateTime.now();
        this.status = Status.RESOLVED;
    }

    public void close() {
        if (status != Status.RESOLVED) {
            throw new IllegalStateException("Only RESOLVED tickets can be closed. Current status: " + status);
        }
        this.status = Status.CLOSED;
    }

    // ---------- SLA (Service Level Agreement) ----------

    public LocalDateTime getSlaDeadline() {
        return createdAt.plusHours(priority.getSlaHours());
    }

    /**
     * A ticket breaches its SLA if it was (or is still being) resolved
     * after the deadline.
     */
    public boolean isSlaBreached() {
        LocalDateTime endTime = (resolvedAt != null) ? resolvedAt : LocalDateTime.now();
        return endTime.isAfter(getSlaDeadline());
    }

    /** Hours taken to resolve, or null if the ticket is not resolved yet. */
    public Double getResolutionHours() {
        if (resolvedAt == null) {
            return null;
        }
        return Duration.between(createdAt, resolvedAt).toMinutes() / 60.0;
    }

    // ---------- Saving to / loading from a text file ----------

    /** One ticket = one line, fields separated by the | character. */
    public String toFileLine() {
        return String.join("|",
                String.valueOf(id), clean(title), clean(description), clean(category),
                priority.name(), status.name(), clean(assignedTo), clean(resolution),
                createdAt.toString(), resolvedAt == null ? "" : resolvedAt.toString());
    }

    public static Ticket fromFileLine(String line) {
        String[] p = line.split("\\|", -1);   // -1 keeps empty fields
        return new Ticket(
                Integer.parseInt(p[0]), p[1], p[2], p[3],
                Priority.valueOf(p[4]), Status.valueOf(p[5]), p[6], p[7],
                LocalDateTime.parse(p[8]),
                p[9].isEmpty() ? null : LocalDateTime.parse(p[9]));
    }

    /** The | character is our separator, so it must not appear inside the text. */
    private static String clean(String text) {
        return text.replace("|", "/").replace("\n", " ");
    }

    // ---------- Printing ----------

    /** Short one-line view used in lists. */
    public String toRow() {
        return String.format("#%-4d %-11s %-9s %-10s %s%s",
                id, status, priority, category, title, isSlaBreached() ? "  [SLA BREACHED]" : "");
    }

    /** Full view of one ticket. */
    public String toDetails() {
        StringBuilder sb = new StringBuilder();
        sb.append("------------------------------------------\n");
        sb.append("Ticket #").append(id).append(" - ").append(title).append("\n");
        sb.append("Category    : ").append(category).append("\n");
        sb.append("Priority    : ").append(priority).append(" (SLA ").append(priority.getSlaHours()).append(" hrs)\n");
        sb.append("Status      : ").append(status).append("\n");
        sb.append("Assigned to : ").append(assignedTo.isEmpty() ? "Not assigned" : assignedTo).append("\n");
        sb.append("Created     : ").append(createdAt.format(FORMAT)).append("\n");
        sb.append("SLA deadline: ").append(getSlaDeadline().format(FORMAT))
                .append(isSlaBreached() ? "  (BREACHED)" : "  (within SLA)").append("\n");
        sb.append("Description : ").append(description).append("\n");
        if (resolvedAt != null) {
            sb.append("Resolved    : ").append(resolvedAt.format(FORMAT)).append("\n");
            sb.append("Resolution  : ").append(resolution).append("\n");
        }
        sb.append("------------------------------------------");
        return sb.toString();
    }

    // ---------- Getters ----------

    public int getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public Priority getPriority() { return priority; }
    public Status getStatus() { return status; }
    public String getAssignedTo() { return assignedTo; }
    public String getResolution() { return resolution; }
}
