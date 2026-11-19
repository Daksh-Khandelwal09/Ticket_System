package servicedesk;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Scanner;

/**
 * The menu the user sees. It only reads input and prints output;
 * the real work is done by TicketService and KnowledgeBase.
 */
public class Main {

    private static final Scanner in = new Scanner(System.in);
    private static final TicketService service = new TicketService("tickets.txt");
    private static final KnowledgeBase kb = new KnowledgeBase("knowledge_base.txt");

    public static void main(String[] args) {
        System.out.println("=== Service Desk Ticketing System ===");

        boolean hadSavedData = service.load();
        kb.load();

        if (!hadSavedData) {
            String answer = ask("No saved tickets found. Load sample tickets? (y/n)");
            if (answer.equalsIgnoreCase("y")) {
                service.loadDemoData();
                System.out.println("Sample tickets loaded.");
            }
        }

        boolean running = true;
        while (running) {
            printMenu();
            try {
                int choice = askInt("Choose an option");
                running = handle(choice);
            } catch (IllegalArgumentException | IllegalStateException e) {
                // Our own checks (wrong id, wrong status...) end up here.
                System.out.println("Error: " + e.getMessage());
            } catch (NoSuchElementException e) {
                // Input ended (for example when the program is fed from a file).
                running = false;
            }
        }
        System.out.println("Goodbye! All tickets are saved.");
    }

    private static void printMenu() {
        System.out.println();
        System.out.println("1.  Create ticket");
        System.out.println("2.  View all tickets");
        System.out.println("3.  View ticket details");
        System.out.println("4.  Assign ticket to an agent");
        System.out.println("5.  Start work on ticket");
        System.out.println("6.  Resolve ticket");
        System.out.println("7.  Close ticket");
        System.out.println("8.  Search tickets");
        System.out.println("9.  Show SLA-breached tickets");
        System.out.println("10. Dashboard");
        System.out.println("11. Search knowledge base");
        System.out.println("12. Add knowledge base article");
        System.out.println("0.  Exit");
    }

    /** Runs the chosen option. Returns false when the user wants to exit. */
    private static boolean handle(int choice) {
        switch (choice) {
            case 1:
                createTicket();
                break;
            case 2:
                printList(service.getAll());
                break;
            case 3:
                System.out.println(service.findById(askInt("Ticket id")).toDetails());
                break;
            case 4:
                int assignId = askInt("Ticket id");
                service.assign(assignId, ask("Agent name"));
                System.out.println("Ticket assigned.");
                break;
            case 5:
                service.startWork(askInt("Ticket id"));
                System.out.println("Ticket is now IN_PROGRESS.");
                break;
            case 6:
                resolveTicket();
                break;
            case 7:
                service.close(askInt("Ticket id"));
                System.out.println("Ticket closed.");
                break;
            case 8:
                printList(service.searchByKeyword(ask("Keyword")));
                break;
            case 9:
                printList(service.getSlaBreached());
                break;
            case 10:
                System.out.println(service.buildDashboard());
                break;
            case 11:
                searchKnowledgeBase();
                break;
            case 12:
                kb.addArticle(ask("Article title"), ask("Fix steps"));
                System.out.println("Article added. Knowledge base now has " + kb.size() + " articles.");
                break;
            case 0:
                return false;
            default:
                System.out.println("Please choose a number from the menu.");
        }
        return true;
    }

    private static void createTicket() {
        String title = ask("Title");
        String description = ask("Description");
        String category = ask("Category (Email / Network / Hardware / Software / Other)");
        Priority priority = askPriority();

        Ticket t = service.createTicket(title, description, category, priority);
        System.out.println("Ticket #" + t.getId() + " created. SLA: " + priority.getSlaHours() + " hours.");
    }

    private static void resolveTicket() {
        int id = askInt("Ticket id");
        String notes = ask("How was it fixed?");
        service.resolve(id, notes);
        System.out.println("Ticket resolved.");

        String save = ask("Save this fix as a knowledge base article? (y/n)");
        if (save.equalsIgnoreCase("y")) {
            Ticket t = service.findById(id);
            kb.addArticle(t.getTitle(), notes);
            System.out.println("Added to the knowledge base.");
        }
    }

    private static void searchKnowledgeBase() {
        List<KnowledgeBase.Article> found = kb.search(ask("Keyword"));
        if (found.isEmpty()) {
            System.out.println("No articles found.");
            return;
        }
        for (KnowledgeBase.Article a : found) {
            System.out.println("* " + a.getTitle());
            System.out.println("  " + a.getSteps());
        }
    }

    private static void printList(List<Ticket> list) {
        if (list.isEmpty()) {
            System.out.println("No tickets to show.");
            return;
        }
        for (Ticket t : list) {
            System.out.println(t.toRow());
        }
    }

    // ---------- Small helpers for reading input safely ----------

    private static String ask(String question) {
        System.out.print(question + ": ");
        return in.nextLine().trim();
    }

    private static int askInt(String question) {
        while (true) {
            String text = ask(question);
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Please enter a number.");
            }
        }
    }

    private static Priority askPriority() {
        while (true) {
            String text = ask("Priority (LOW / MEDIUM / HIGH / CRITICAL)").toUpperCase();
            try {
                return Priority.valueOf(text);
            } catch (IllegalArgumentException e) {
                System.out.println("Please type one of: LOW, MEDIUM, HIGH, CRITICAL.");
            }
        }
    }
}
