import java.io.File;
import java.io.IOException;
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
			return true;
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

		if (page.getParseData() instanceof HtmlParseData) {
			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String text = htmlParseData.getText();
			String html = htmlParseData.getHtml();
			Set<WebURL> links = htmlParseData.getOutgoingUrls();
			
			for(WebURL link : links){
				System.out.println(link);
				MyImageCrawlerController.nestedCrawlDomains.add(link.toString());
			}
			// System.out.println("Text length: " + text.length());
			// System.out.println("Html length: " + html.length());
			// System.out.println("Number of outgoing links: " + links.size());
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
		
//		try {
//			Files.write(page.getContentData(), new File(filename));
//			logger.info("Stored: {}", url);
//		} catch (IOException iox) {
//			logger.error("Failed to write file: " + filename, iox);
//		}

		
	}
}
