import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import com.google.common.io.Files;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class CatalogOfClinicalImagesPageCrawler extends WebCrawler {
	private static final Pattern filters = Pattern.compile(".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
	private static final String httpRegex = ".*://";

	private static final Pattern imgPatterns = Pattern.compile(".*(\\.(bmp|jpe?g|png|tiff?))$");

	private static File storageFolder;
	private static List<String> crawlDomains;

	public static void configure(List<String> domain, String storageFolderName) {
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
			return true;
		}
		//
		// if (href.contains("skin_actinic_keratosis")) {
		// System.out.println("A: " + href);
		// System.out.println("A: " + href.replaceAll(httpRegex, ""));
		// }
		for (String domain : crawlDomains) {
			// if (domain.contains("skin_actinic_keratosis")) {
			// System.out.println("A: " + href.replaceAll(httpRegex, ""));
			// System.out.println("B: " + domain.replaceAll(httpRegex, ""));
			// System.out.println(href.replaceAll(httpRegex, "").equals(domain.replaceAll(httpRegex, "")));
			// }
			if (href.replaceAll(httpRegex, "").equals(domain.replaceAll(httpRegex, ""))) {
				System.out.println("B match: " + domain.replaceAll(httpRegex, ""));
				return true;
			}
		}
		return false;
	}

	@Override
	public void visit(Page page) {
		String url = page.getWebURL().getURL();

		String html = null;
		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String text = htmlParseData.getText();
			// String html = htmlParseData.getHtml();
			html = htmlParseData.getHtml();
			Set<WebURL> links = htmlParseData.getOutgoingUrls();

			// for (WebURL link : links) {
			// System.out.println(link);
			// MyImageCrawlerController.nestedCrawlDomains.add(link.toString());
			// }
			// System.out.println("Text length: " + text.length());
			// System.out.println("Html length: " + html.length());
			// System.out.println("Number of outgoing links: " + links.size());
		}

		// We are only interested in processing images which are bigger than 10k
		// if (!imgPatterns.matcher(url).matches() || !((page.getParseData() instanceof BinaryParseData) || (page.getContentData().length < (10 * 1024)))) {
		// return;
		// }

		// System.out.println("0: " + url);
		// System.out.println("0: " + url.toLowerCase());
		// System.out.println("0: " + imgPatterns.matcher(url.toLowerCase()).matches());

		if (imgPatterns.matcher(url.toLowerCase()).matches()) {

			// get a unique name for storing this image
			// String extension = url.substring(url.lastIndexOf('.'));
			// String hashedName = UUID.randomUUID() + extension;

			String fileName = page.getWebURL().getPath().split("/")[2];

			// System.out.println("1: " + fileName);

			// store image
			String imageFileName = storageFolder.getAbsolutePath() + "/" + fileName;

			// System.out.println("1: " + imageFileName);

			try {

				// System.out.println("1: " + "in try");
				File file = new File(storageFolder.getPath() + "/" + fileName.split("[.]")[0] + ".txt");

				// System.out.println("1: " + file.getPath());
				if (!file.exists())
					file.createNewFile();
				FileWriter fileWriter = new FileWriter(file);

				fileWriter.write(page.getWebURL().getAnchor() + "\r\n");
				fileWriter.write(page.getWebURL().getDocid() + "\r\n");
				fileWriter.write(page.getWebURL().getDomain() + "\r\n");
				fileWriter.write(page.getWebURL().getParentDocid() + "\r\n");
				fileWriter.write(page.getWebURL().getParentUrl() + "\r\n");
				fileWriter.write(page.getWebURL().getPath() + "\r\n");
				fileWriter.write(page.getWebURL().getSubDomain() + "\r\n");
				fileWriter.write(page.getWebURL().getTag() + "\r\n");
				fileWriter.write(page.getWebURL().getURL() + "\r\n");
				fileWriter.write("\r\n");

				fileWriter.flush();
				fileWriter.close();

				// System.out.println("1: " + "writing binary data");
				Files.write(page.getContentData(), new File(imageFileName));

				// System.out.println("1: " + "succeeded");

				logger.info("Stored: {}", url);
			} catch (IOException iox) {
				iox.printStackTrace();
				logger.error("Failed to write file: " + imageFileName, iox);
			}

		} else {

			// System.out.println("2: " + "processing text");
			String text = html;
			String[] paras = text.split("<p>");

			try {

				// System.out.println("2: " + "writing");
				String fileName = page.getWebURL().getPath().split("/")[2];

				File file = new File(storageFolder.getPath() + "/" + fileName.split("[.]")[0] + "_text.txt");

				if (!file.exists())
					file.createNewFile();
				FileWriter fileWriter = new FileWriter(file);

				fileWriter.write(page.getWebURL().getAnchor() + "\r\n");
				fileWriter.write(page.getWebURL().getDocid() + "\r\n");
				fileWriter.write(page.getWebURL().getDomain() + "\r\n");
				fileWriter.write(page.getWebURL().getParentDocid() + "\r\n");
				fileWriter.write(page.getWebURL().getParentUrl() + "\r\n");
				fileWriter.write(page.getWebURL().getPath() + "\r\n");
				fileWriter.write(page.getWebURL().getSubDomain() + "\r\n");
				fileWriter.write(page.getWebURL().getTag() + "\r\n");
				fileWriter.write(page.getWebURL().getURL() + "\r\n");
				fileWriter.write("\r\n");

				for (String para : paras) {
					if (para.contains("<b>")) {
						fileWriter.write(para.split("</p>")[0]);
						fileWriter.write("\r\n");
					}
				}

				fileWriter.flush();
				fileWriter.close();
				// System.out.println("2: " + "success");

				logger.info("Stored: {}", url);
			} catch (IOException iox) {
				iox.printStackTrace();
			}
		}

	}
}
