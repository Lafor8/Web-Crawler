import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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
	private static final Pattern filters = Pattern.compile(".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
	private static final Pattern skinFilter = Pattern.compile(".*(skin).*");

	private static final Pattern imgPatterns = Pattern.compile(".*(\\.(bmp|gif|jpe?g|png|tiff?))$");

	private static File storageFolder;
	private static String[] crawlDomains;

	public static void configure(String[] domain, String storageFolderName) {
		crawlDomains = domain;

		storageFolder = new File(storageFolderName);
		if (!storageFolder.exists()) {
			storageFolder.mkdirs();
		}
	}

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();
		if (filters.matcher(href).matches()) {
			return false;
		}

		if (imgPatterns.matcher(href).matches()) {
			return false;
		}

		for (String domain : crawlDomains) {
			if (href.startsWith(domain)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();

		System.out.println(page.getWebURL().getAnchor());
		System.out.println(page.getWebURL().getDocid());
		System.out.println(page.getWebURL().getDomain());
		System.out.println(page.getWebURL().getParentDocid());
		System.out.println(page.getWebURL().getParentUrl());
		System.out.println(page.getWebURL().getPath());
		System.out.println(page.getWebURL().getSubDomain());
		System.out.println(page.getWebURL().getTag());
		System.out.println(page.getWebURL().getURL());
		System.out.println(page.getWebURL().getURL().replace("skin.htm", ""));

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
				// System.out.println("Number of outgoing links: " + links.size());
				// fileWriter.write(html);

				String trimmed = html.substring((html.indexOf("<ul>") + 4), html.indexOf("</ul>")).trim();
				fileWriter.write(trimmed);

				// Scanner scanner = new Scanner(trimmed);
				// while (scanner.hasNextLine()) {
				// String line = scanner.nextLine();
				//
				//
				// System.out.println(line);
				// System.out.println((line.indexOf("href=\"") + 6)+" "+ line.indexOf("\">"));
				// // line = line.substring(line.indexOf("href=\"") + 6, line.indexOf("\">")).trim();
				// // fileWriter.write(line + "\n");
				// }
				// }

				// scanner.close();

				String[] lines = trimmed.split("<li>");
				for (String line : lines) {
					if (line.trim().length() > 0) {
						System.out.println(line);
						System.out.println((line.indexOf("href=\"") + 6) + " " + line.indexOf("\">"));
						line = line.substring(line.indexOf("href=\"") + 6, line.indexOf("\">")).trim();
						fileWriter.write(line + "\n");
					}
				}

				fileWriter.flush();
				fileWriter.close();

				System.out.println("Details written to file.");
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		// We are only interested in processing images which are bigger than 10k
		if (!imgPatterns.matcher(url).matches() || !((page.getParseData() instanceof BinaryParseData) || (page.getContentData().length < (10 * 1024)))) {
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
