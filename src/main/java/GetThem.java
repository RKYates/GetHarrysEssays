import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;


public class GetThem {
    public static void main(String[] args) {
        OkHttpClient client = new OkHttpClient();

        // GET initial URL, make list of URLs
        try {
            String listUrl = "http://archive.constantcontact.com/fs008/1101380377741/archive/1102228092983.html";

            Document listDoc = Jsoup.connect(listUrl).get();
            List<Element> linkElements = listDoc.select("a[class=\"ArchiveLinks\"]");

            HashMap<String, String> links = new HashMap<>();
            for (Element link : linkElements) {
                String linkUrl = link.absUrl("href");
                String linkTitle = link.text(); // TODO get full title?
                links.put(linkUrl, linkTitle);
            }

            // for each link...
            for (String url : links.keySet()) {

                try {
                    // GET the link
                    Request request = new Request.Builder()
                            .url(url)
                            .build();
                    Response response = client.newCall(request).execute();
                    String body = response.body().string();
                    String[] lines = body.split("\n");

                    // make file name the title of the essay
                    String title = links.get(url).replace("/", ".");
                    String baseFolder = "~/HarryEssays/"; // set to your full path of base folder
                    String folder = getFolder(title);
                    String path = baseFolder + "text/" + folder + title + ".txt";
                    PrintWriter writer = new PrintWriter(path, "UTF-8");

                    // output every line in the essay
                    for (String line : lines) {
                        String lineWithNewlines = line.replace("<br />", "\\n")
                                .replace("\\n ", "\\n")
                                .replace("<div>&nbsp;</div>", "\\n\\n")
                                .replace("<p", "\\n<p")
                                .replace("<div style=\"color: #000000;\">&nbsp;</div>", "\\n\\n")
                                .replace("<span>&nbsp;</span>", "\\n\\n");
                        String plain = Jsoup.parse(lineWithNewlines).text();
                        if (plain.startsWith("By")
                                || plain.startsWith("Findings")
                                || plain.startsWith("FINDINGS")
                                || plain.contains("Harry T. Cook. All rights reserved.")) {
                            plain = "\\n" + plain;
                        } else if (plain.matches("\\d\\d?/\\d\\d?/\\d\\d\\d?\\d?")) {
                            plain = "\\n" + plain + "\\n";
                        } else if (plain.contains("Readers Write")) {
                            plain = plain + "\\n";
                        }

                        plain = plain.replace("\\n", "\n");
                        if (!badLines.contains(plain)) {
                            writer.print(plain);
                        }
                    }

                    writer.close();
                    System.out.println("Done with " + path);

                    // make html file
                    String htmlPath = baseFolder + "html/" + folder + title + ".html";
                    PrintWriter htmlWriter = new PrintWriter(htmlPath, "UTF-8");
                    htmlWriter.write(Jsoup.connect(url).get().html());
                    htmlWriter.close();
                    System.out.println("Done with " + htmlPath);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // lines I don't care to save, just some copyright boilerplate
    public static List<String> badLines = Arrays.asList(
            "written agreement with Constant Contact, neither the Constant Contact software, nor any content that appears on any Constant Contact site,",
            "including but not limited to, web pages, newsletters, or templates may be reproduced, republished, repurposed, or distributed without the",
            "prior written permission of Constant Contact.  For inquiries regarding reproduction or distribution of any Constant Contact material, please",
            "prior written permission of Constant Contact. For inquiries regarding reproduction or distribution of any Constant Contact material, please",
            "contact legal@constantcontact.com.-->"
    );

    /**
     * Gets the folder to put the file in based on the title.
     * @param title title of the essay/file
     * @return folder with trailing slash
     */
    public static String getFolder(String title) {
        String folder = "";
        if (title.contains("Findings")) {
            folder = "Findings/";
        } else if (title.contains("Essay")
                || title.contains("Uterus")) {
            folder = "Essays/";
        } else if (title.contains("Lecture schedule")) {
            folder = "Lecture Schedules/";
        }
        return folder;
    }
}
