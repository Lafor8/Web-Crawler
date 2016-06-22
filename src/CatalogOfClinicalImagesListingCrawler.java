import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.io.Files;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class CatalogOfClinicalImagesListingCrawler extends WebCrawler {

	// Filters
	private static final Pattern standardFilters = Pattern.compile(".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
	private static final Pattern imgFilters = Pattern.compile(".*(\\.(bmp|gif|jpe?g|png|tiff?))$");
	private static final Pattern customFilters = Pattern.compile(".*(\\.(cfm))$");

	private static final Pattern pageFilters = Pattern.compile(".*((.*(index|links|credits|browse).*)\\.[A-z]{1,4})$");

	// Regexes
	private static final String httpRegex = ".*://";

	// Initialized parameters
	private static File storageFolder;
	private static String[] crawlDomains;
	public static List<String> dermPages;

	// Counters
	private static int itemsChecked, standard, img, custom, accepted, wrongPage, wrongDomain, defaulted;

	public static void configure(String[] domain, String storageFolderName) {
		crawlDomains = domain;

		storageFolder = new File(storageFolderName);
		if (!storageFolder.exists()) {
			storageFolder.mkdirs();
		}

		// Custom Initializations
		itemsChecked = 0;
		standard = 0;
		img = 0;
		custom = 0;
		wrongPage = 0;
		wrongDomain = 0;
		accepted = 0;
		defaulted = 0;
	}

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();

		itemsChecked++;
		if (standardFilters.matcher(href).matches()) {
			standard++;
			return false;
		}
		if (imgFilters.matcher(href).matches()) {
			img++;
			return false;
		}

		if (customFilters.matcher(href).matches()) {
			custom++;
			return false;
		}

		if (pageFilters.matcher(href).matches()) {
			wrongPage++;
			return false;
		}

		for (String domain : crawlDomains) {
			if (!href.replaceAll(httpRegex, "").split("/")[0].startsWith(domain.replaceAll(httpRegex, "").split("/")[0])) {
				wrongDomain++;
				return false;
			}
		}

		for (String domain : crawlDomains) {
			// System.out.println(domain); // https://meded.ucsd.edu/clinicalimg/skin.htm
			if (href.replaceAll(httpRegex, "").startsWith(domain.replaceAll(httpRegex, ""))) {
				System.out.println("Seeded: " + href);
				accepted++;
				return true;
			} else {
				System.out.println("Not Seeded: " + href);
				return false;
			}
		}

		System.out.println(href);
		defaulted++;
		return false;
	}

	@Override
	public void visit(Page page) {
		System.out.println("Visiting: \"" + page.getWebURL().getURL() + "\""); // Visiting "https://meded.ucsd.edu/clinicalimg/skin.htm"
		String url = page.getWebURL().getURL();

		// System.out.println(page.getWebURL().getAnchor()); // null
		// System.out.println(page.getWebURL().getDocid()); // 1
		// System.out.println(page.getWebURL().getDomain()); // ucsd.edu
		// System.out.println(page.getWebURL().getParentDocid()); // 0
		// System.out.println(page.getWebURL().getParentUrl()); // null
		// System.out.println(page.getWebURL().getPath()); // /clinicalimg/skin.htm
		// System.out.println(page.getWebURL().getSubDomain()); // meded
		// System.out.println(page.getWebURL().getTag()); // null
		// System.out.println(page.getWebURL().getURL()); // https://meded.ucsd.edu/clinicalimg/skin.htm
		// System.out.println(page.getWebURL().getURL().replace("skin.htm", "")); // https://meded.ucsd.edu/clinicalimg/

		if (page.getParseData() instanceof HtmlParseData) {
			System.out.println("Data successfully gathered.");

			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String text = htmlParseData.getText();
			String html = htmlParseData.getHtml();
			Set<WebURL> links = htmlParseData.getOutgoingUrls();

			File file = new File(storageFolder.getPath() + "/scrap.txt");
			try {
				if (!file.exists())
					file.createNewFile();
				FileWriter fileWriter = new FileWriter(file);

				// for (WebURL link : links) {
				// if (skinFilter.matcher(link.toString()).matches()) {

				// System.out.println(link);
				// fileWriter.write(link + "\n");
				// CatalogOfClinicalImagesCrawlerController.nestedCrawlDomains.add(link.toString());
				// }
				// }
				// System.out.println("Text length: " + text.length());
				// System.out.println("Html length: " + html.length());
				// System.out.println("Number of outgoing links: " +
				// links.size());
				// fileWriter.write(html);

				String trimmed = html.substring((html.indexOf("<ul>") + 4), html.indexOf("</ul>")).trim();
				// fileWriter.write(trimmed);

				// Scanner scanner = new Scanner(trimmed);
				// while (scanner.hasNextLine()) {
				// String line = scanner.nextLine();
				//
				//
				// System.out.println(line);
				// System.out.println((line.indexOf("href=\"") + 6)+" "+
				// line.indexOf("\">"));
				// // line = line.substring(line.indexOf("href=\"") + 6,
				// line.indexOf("\">")).trim();
				// // fileWriter.write(line + "\n");
				// }
				// }

				// scanner.close();

				List<String> pages = new ArrayList<>();

				String[] lines = trimmed.split("<li>");
				for (String line : lines) {
					if (line.trim().length() > 0) {
						// System.out.println(line);
						// System.out.println((line.indexOf("href=\"") + 6) + "
						// " + line.indexOf("\">"));
						line = line.substring(line.indexOf("href=\"") + 6, line.indexOf("\">")).trim();

						line = page.getWebURL().getURL().replace("skin.htm", line);
						pages.add(line);

						fileWriter.write(line + "\n");
					}
				}

				CatalogOfClinicalImagesListingCrawler.dermPages = pages;

				fileWriter.flush();
				fileWriter.close();

				System.out.println(pages.size() + " pages marked for crawling.");
				System.out.println("Details written to file.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// We are only interested in processing images which are bigger than 10k
		if (!imgFilters.matcher(url).matches() || !((page.getParseData() instanceof BinaryParseData) || (page.getContentData().length < (10 * 1024)))) {
			return;
		}

		// get a unique name for storing this image
		String extension = url.substring(url.lastIndexOf('.'));
		String hashedName = UUID.randomUUID() + extension;

		// store image
		String filename = storageFolder.getAbsolutePath() + "/" + hashedName;

		// try {
		// Files.write(page.getContentData(), new File(filename));
		// logger.info("Stored: {}", url);
		// } catch (IOException iox) {
		// logger.error("Failed to write file: " + filename, iox);
		// }

	}
}
