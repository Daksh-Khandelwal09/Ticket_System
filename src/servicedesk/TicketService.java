package servicedesk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * All the business logic: create, find, update, search, report and save tickets.
 * (Main only talks to the user; this class does the real work.)
 */
public class TicketService {

    private final List<Ticket> tickets = new ArrayList<>();
    private final Path file;
    private int nextId = 1001;

    public TicketService(String fileName) {
        this.file = Paths.get(fileName);
    }

    // ---------- Create / find ----------

    public Ticket createTicket(String title, String description, String category, Priority priority) {
        Ticket ticket = new Ticket(nextId++, title, description, category, priority);
        tickets.add(ticket);
        save();
        return ticket;
    }

    public Ticket findById(int id) {
        for (Ticket t : tickets) {
            if (t.getId() == id) {
                return t;
            }
        }
        throw new IllegalArgumentException("No ticket found with id " + id);
    }

    // ---------- Workflow actions ----------

    public void assign(int id, String agent) {
        findById(id).assignTo(agent);
        save();
    }

    public void startWork(int id) {
        findById(id).startWork();
        save();
    }

    public void resolve(int id, String resolutionNotes) {
        findById(id).resolve(resolutionNotes);
        save();
    }

    public void close(int id) {
        findById(id).close();
        save();
    }

    // ---------- Lists and search ----------

    public List<Ticket> getAll() {
        return new ArrayList<>(tickets);
    }

    public List<Ticket> searchByKeyword(String keyword) {
        String k = keyword.toLowerCase();
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            boolean match = t.getTitle().toLowerCase().contains(k)
                    || t.getDescription().toLowerCase().contains(k)
                    || t.getCategory().toLowerCase().contains(k);
            if (match) {
                result.add(t);
            }
        }
        return result;
    }

    /** Tickets that have missed (or are missing) their SLA deadline. */
    public List<Ticket> getSlaBreached() {
        List<Ticket> result = new ArrayList<>();
        for (Ticket t : tickets) {
            if (t.isSlaBreached()) {
                result.add(t);
            }
        }
        return result;
    }

    // ---------- Dashboard ----------

    public String buildDashboard() {
        int open = 0, inProgress = 0, resolved = 0, closed = 0;
        double totalHours = 0;
        int resolvedCount = 0;

        for (Ticket t : tickets) {
            switch (t.getStatus()) {
                case OPEN: open++; break;
                case IN_PROGRESS: inProgress++; break;
                case RESOLVED: resolved++; break;
                case CLOSED: closed++; break;
            }
            Double hours = t.getResolutionHours();
            if (hours != null) {
                totalHours += hours;
                resolvedCount++;
            }
        }

        int total = tickets.size();
        int breached = getSlaBreached().size();
        double slaPercent = (total == 0) ? 100.0 : (total - breached) * 100.0 / total;

        StringBuilder sb = new StringBuilder();
        sb.append("========== DASHBOARD ==========\n");
        sb.append("Total tickets   : ").append(total).append("\n");
        sb.append("Open            : ").append(open).append("\n");
        sb.append("In progress     : ").append(inProgress).append("\n");
        sb.append("Resolved        : ").append(resolved).append("\n");
        sb.append("Closed          : ").append(closed).append("\n");
        sb.append("SLA breached    : ").append(breached).append("\n");
        sb.append(String.format("SLA compliance  : %.1f%%%n", slaPercent));
        if (resolvedCount > 0) {
            sb.append(String.format("Avg. resolution : %.1f hours%n", totalHours / resolvedCount));
        } else {
            sb.append("Avg. resolution : n/a (nothing resolved yet)\n");
        }
        sb.append("===============================");
        return sb.toString();
    }

    // ---------- Saving and loading ----------

    public void save() {
        List<String> lines = new ArrayList<>();
        for (Ticket t : tickets) {
            lines.add(t.toFileLine());
        }
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            System.out.println("Warning: could not save tickets: " + e.getMessage());
        }
    }

    /** Returns true if a saved file existed and was loaded. */
    public boolean load() {
        if (!Files.exists(file)) {
            return false;
        }
        try {
            for (String line : Files.readAllLines(file)) {
                if (line.isBlank()) {
                    continue;
                }
                Ticket t = Ticket.fromFileLine(line);
                tickets.add(t);
                if (t.getId() >= nextId) {
                    nextId = t.getId() + 1;
                }
            }
        } catch (IOException | RuntimeException e) {
            System.out.println("Warning: could not read saved tickets: " + e.getMessage());
        }
        return true;
    }

    // ---------- Demo data ----------

    /** A few sample tickets so the project looks alive on the first run. */
    public void loadDemoData() {
        LocalDateTime now = LocalDateTime.now();

        add(new Ticket(nextId++, "Cannot login to email", "Outlook keeps asking for the password again and again.",
                "Email", Priority.HIGH, Status.OPEN, "", "", now.minusHours(10), null));

        add(new Ticket(nextId++, "VPN not connecting from home", "VPN client shows error 800 after entering credentials.",
                "Network", Priority.CRITICAL, Status.IN_PROGRESS, "Rahul", "", now.minusHours(2), null));

        add(new Ticket(nextId++, "Printer offline on 2nd floor", "Printer shows offline for all users on the floor.",
                "Hardware", Priority.MEDIUM, Status.RESOLVED, "Priya",
                "Restarted the print spooler service and re-added the printer.",
                now.minusHours(30), now.minusHours(26)));

        add(new Ticket(nextId++, "Need software installed", "Please install Notepad++ on my laptop.",
                "Software", Priority.LOW, Status.CLOSED, "Rahul",
                "Installed Notepad++ using the approved software portal.",
                now.minusHours(50), now.minusHours(48)));

        save();
    }

    private void add(Ticket t) {
        tickets.add(t);
    }
}
