package servicedesk;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * A small knowledge base of fixes. Agents search it to solve common issues faster,
 * and can add new articles from tickets they have resolved.
 */
public class KnowledgeBase {

    /** One article: a title and the steps to fix the problem. */
    public static class Article {
        private final String title;
        private final String steps;

        public Article(String title, String steps) {
            this.title = title;
            this.steps = steps;
        }

        public String getTitle() { return title; }
        public String getSteps() { return steps; }
    }

    private final List<Article> articles = new ArrayList<>();
    private final Path file;

    public KnowledgeBase(String fileName) {
        this.file = Paths.get(fileName);
    }

    public void addArticle(String title, String steps) {
        articles.add(new Article(title.replace("|", "/"), steps.replace("|", "/")));
        save();
    }

    public List<Article> search(String keyword) {
        String k = keyword.toLowerCase();
        List<Article> result = new ArrayList<>();
        for (Article a : articles) {
            if (a.getTitle().toLowerCase().contains(k) || a.getSteps().toLowerCase().contains(k)) {
                result.add(a);
            }
        }
        return result;
    }

    public int size() {
        return articles.size();
    }

    private void save() {
        List<String> lines = new ArrayList<>();
        for (Article a : articles) {
            lines.add(a.getTitle() + "|" + a.getSteps());
        }
        try {
            Files.write(file, lines);
        } catch (IOException e) {
            System.out.println("Warning: could not save knowledge base: " + e.getMessage());
        }
    }

    /** Loads the saved file; if there is none, starts with some common articles. */
    public void load() {
        if (Files.exists(file)) {
            try {
                for (String line : Files.readAllLines(file)) {
                    String[] p = line.split("\\|", 2);
                    if (p.length == 2) {
                        articles.add(new Article(p[0], p[1]));
                    }
                }
            } catch (IOException e) {
                System.out.println("Warning: could not read knowledge base: " + e.getMessage());
            }
        } else {
            addArticle("Password reset (Windows / Email)",
                    "1) Open the self-service portal. 2) Click 'Forgot password'. 3) Verify with OTP. 4) Set a new password. "
                            + "5) Sign out of all devices and sign in again with the new password.");
            addArticle("VPN not connecting",
                    "1) Check internet connection. 2) Restart the VPN client. 3) Make sure date and time are correct. "
                            + "4) Re-enter credentials. 5) If error persists, reinstall the VPN client and escalate to the Network team.");
            addArticle("Printer offline",
                    "1) Check printer power and network cable. 2) Restart the Print Spooler service. "
                            + "3) Remove and re-add the printer. 4) Print a test page.");
            addArticle("Outlook not opening / not syncing",
                    "1) Close Outlook completely. 2) Start it in safe mode (outlook /safe). 3) Disable add-ins. "
                            + "4) Recreate the mail profile from Control Panel > Mail.");
            addArticle("Laptop running slow",
                    "1) Restart the laptop. 2) Check Task Manager for high CPU or memory use. "
                            + "3) Disable unneeded startup programs. 4) Free up disk space. 5) Run pending Windows updates.");
        }
    }
}
