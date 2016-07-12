import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.google.common.io.Files;

import edu.uci.ics.crawler4j.crawler.Page;
import edu.uci.ics.crawler4j.crawler.WebCrawler;
import edu.uci.ics.crawler4j.parser.BinaryParseData;
import edu.uci.ics.crawler4j.parser.HtmlParseData;
import edu.uci.ics.crawler4j.url.WebURL;

public class DermNetNZCrawler extends WebCrawler {

	// Filters
	private static final Pattern standardFilters = Pattern.compile(".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
	private static final Pattern imgFilters = Pattern.compile(".*(\\.(bmp|ico|jpe?g|png|tiff?))$");
	private static final Pattern customFilters = Pattern.compile(".*(\\.(cfm))$");

	private static final Pattern pageFilters = Pattern.compile(".*((.*(index|links|credits|browse|search).*)\\.[A-z]{1,4})$");
	private static final Pattern customPageFilters = Pattern.compile(".*/(site-age-specific)/.*");

	private static final Pattern imagePageFilters = Pattern.compile(".*/common/image\\.php.*");

	// Regexes
	private static final String httpRegex = ".*://";

	// Initialized parameters
	private static File storageFolder;
	private static String[] crawlDomains;
	public static List<String> dermPages;

	// Pages and Depth
	public static String seedPage = "";
	public static List<String> transitionPages = new ArrayList<String>();
	public static List<String> leafPages = new ArrayList<String>();

	// Counters
	private static int itemsChecked, standard, imgFiltered, custom, accepted, wrongPage, wrongDomain, defaulted, img;
	public static int totalImg = 0;
	public static int totalImgLinked = 0;
	private static int PAGES = 10000, PAGES2 = 10000;

	public static void configure(String[] domain, String storageFolderName) {
		crawlDomains = domain;

		storageFolder = new File(storageFolderName);
		if (!storageFolder.exists()) {
			storageFolder.mkdirs();
		}

		// Custom Initializations
		clearCounters();

		seedPage = domain[0].toLowerCase();
	}

	@Override
	public boolean shouldVisit(Page referringPage, WebURL url) {
		String href = url.getURL().toLowerCase();
		// System.out.println("Referring Page :" + referringPage.getWebURL().getURL());

		// Height at 1
		if (url.getParentUrl().equals(seedPage)) {

			itemsChecked++;
			if (standardFilters.matcher(href).matches()) {
				standard++;
				return false;
			}
			if (imgFilters.matcher(href).matches()) {
				imgFiltered++;
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

			if (!href.replaceAll(httpRegex, "").split("/")[0].startsWith(seedPage.replaceAll(httpRegex, "").split("/")[0])) {
				wrongDomain++;
				return false;
			}

			if (customPageFilters.matcher(href).matches()) {
				wrongPage++;
				return false;
			}

			if (PAGES-- > 0) {
				// System.out.println("VALID @ Height 1:\t" + href);

				if (href.split("/").length < 5) {
					wrongPage++;
					return false;
				}

				// Pattern pattern = Pattern.compile("([0-9]+) images");
				// Matcher matcher = pattern.matcher(url.getAnchor());
				// matcher.find();
				//
				// int number = Integer.parseInt(matcher.group(1));
				//
				// if (number > 0) {
				// System.out.println("VALID @ Height 1:\t" + href + " " + number);
				// System.out.println("VALID @ Height 1:\t" + href);
				accepted++;
				transitionPages.add(href);
				return true;
				// } else
				// return false;
			} else
				return false;
		}

		// Height at 2
		else if (transitionPages.contains(url.getParentUrl().toLowerCase())) {

			itemsChecked++;
			if (standardFilters.matcher(href).matches()) {
				// System.out.println("standard:\t " + href);
				standard++;
				return false;
			}
			// if (imgFilters.matcher(href).matches()) {
			// // System.out.println("img:\t " + href);
			// img++;
			// return false;
			// }

			if (customFilters.matcher(href).matches()) {
				// System.out.println("custom:\t " + href);
				custom++;
				return false;
			}

			if (pageFilters.matcher(href).matches()) {
				// System.out.println("page:\t " + href);
				wrongPage++;
				return false;
			}

			if (!href.replaceAll(httpRegex, "").split("/")[0].startsWith(seedPage.replaceAll(httpRegex, "").split("/")[0])) {
				// System.out.println("domain:\t " + href);
				wrongDomain++;
				return false;
			}

			// if (transitionPageFilters.matcher(href).matches()) {
			// // System.out.println("page:\t " + href);
			// wrongPage++;
			// return false;
			// }
			//

			if (imgFilters.matcher(href).matches() && imagePageFilters.matcher(href).matches()) {
				// System.out.println("img:\t " + href);
				img++;
				accepted++;
				// System.out.println("VALID @ Height 2:\t " + href);
				leafPages.add(href);
				return true;
			}

			return false;
		}
		// Height at 3
		else if (leafPages.contains(url.getParentUrl().toLowerCase())) {
			// if (imgFilters.matcher(href).matches() && imgDownloadFilters.matcher(href).matches()) {

			// System.out.println("VALID @ Height 3:\t " + href);
			// img++;
			// return true;
			// }

			if (imgFilters.matcher(href).matches()) {
				// System.out.println("img:\t " + href);
				img++;
				accepted++;
				// System.out.println("VALID @ Height 3:\t " + href);
				leafPages.add(href);
				return true;
			}

			return false;
		}

		System.out.println("Defaulted " + url.getURL() + "\t" + url.getParentUrl());
		return false;

	}

	@Override
	public void visit(Page page) {
		// System.out.println("Visiting: \"" + page.getWebURL().getURL() + "\""); // Visiting "https://meded.ucsd.edu/clinicalimg/skin.htm"
		String url = page.getWebURL().getURL();

		// Height at 0
		if (page.getWebURL().getParentUrl() == null) {
			// System.out.println("Height at 0");
			if (page.getParseData() instanceof HtmlParseData) {
				// Retrieve Textual content
				HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
				String text = htmlParseData.getText();
				String html = htmlParseData.getHtml();
				Set<WebURL> links = htmlParseData.getOutgoingUrls();

				// Saving crawling details
				File file = new File(storageFolder.getPath() + "/Crawling Details.txt");
				try {
					if (!file.exists())
						file.createNewFile();

					FileWriter fileWriter = new FileWriter(file);

					fileWriter.write("itemsChecked = " + itemsChecked + "\r\n");
					fileWriter.write("standard = " + standard + "\r\n");
					fileWriter.write("img = " + imgFiltered + "\r\n");
					fileWriter.write("custom = " + custom + "\r\n");
					fileWriter.write("wrongPage = " + wrongPage + "\r\n");
					fileWriter.write("wrongDomain = " + wrongDomain + "\r\n");
					fileWriter.write("accepted = " + accepted + "\r\n");
					fileWriter.write("defaulted = " + defaulted + "\r\n");
					fileWriter.flush();
					fileWriter.close();

					System.out.println(accepted + " pages marked for crawling.");
					System.out.println("Details written to file.");

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				// Clearing counters for deeper crawling
				clearCounters();
			}
		}

		// Height at 1
		else if (page.getWebURL().getParentUrl().toLowerCase().equals(seedPage)) {
//			System.out.println("Height at 1 " + url);

			HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
			String text = htmlParseData.getText();
			String html = htmlParseData.getHtml();
			Set<WebURL> links = htmlParseData.getOutgoingUrls();

			try {
				String urlSegments[] = page.getWebURL().getURL().split("/");
				String fileName = urlSegments[urlSegments.length - 2] + "_" + urlSegments[urlSegments.length - 1].split("[.]")[0];

				File fileDir = new File(storageFolder.getPath() + "/Text/");
				fileDir.mkdirs();
				File file = new File(fileDir.getPath() + "\\" + fileName + "_text.txt");

				if (!file.exists())
					file.createNewFile();
				FileWriter fileWriter = new FileWriter(file);

				// Getting HTML Doc

				Document doc = Jsoup.parse(html);

				// JSON Writing
				JSONObject json = new JSONObject();

				// Writing Web URL Info
				JSONObject webURLInfo = new JSONObject();

				webURLInfo.put("Anchor", page.getWebURL().getAnchor());
				webURLInfo.put("DocID", page.getWebURL().getDocid());
				webURLInfo.put("Domain", page.getWebURL().getDomain());
				webURLInfo.put("ParentDocID", page.getWebURL().getParentDocid());
				webURLInfo.put("PathURL", page.getWebURL().getParentUrl());
				webURLInfo.put("Path", page.getWebURL().getPath());
				webURLInfo.put("SubDomain", page.getWebURL().getSubDomain());
				webURLInfo.put("Tag", page.getWebURL().getTag());
				webURLInfo.put("URL", page.getWebURL().getURL());
				json.put("WebURLInfo", webURLInfo);

				// Writing Image Info
				JSONObject imgInfo = new JSONObject();
				JSONArray imgArray = new JSONArray();

				Elements images = doc.getElementsByClass("images").select("a");

				for (Element image : images) {
					totalImgLinked++;
					String imgSrc = image.attr("href");
					imgSrc = "http://www.dermnetnz.org" + imgSrc.split("=")[1];
					imgArray.put(imgSrc);
				}

				imgInfo.put("ImageCount", imgArray.length());
				imgInfo.put("Images", imgArray);

				json.put("ImgInfo", imgInfo);

				// Writing Text Info
				JSONObject textInfo = new JSONObject();

				textInfo.put("Content", doc.getElementById("content").text());

				json.put("TextInfo", textInfo);

				fileWriter.write(json.toString(1));
				fileWriter.write("\r\n");

				img = 0;
				fileWriter.flush();
				fileWriter.close();

				logger.info("Stored: {}", url);
			} catch (IOException iox) {
				iox.printStackTrace();
			}
		}

		// Height at 2
		else if (transitionPages.contains(page.getWebURL().getParentUrl().toLowerCase())) {

		}

		// Height at 3
		else if (leafPages.contains(page.getWebURL().getParentUrl().toLowerCase())) {
			// System.out.println("Height at 3");

			if (page.getParseData() instanceof BinaryParseData) {
				String urlSegments[] = page.getWebURL().getPath().split("/");
				String fileName = urlSegments[urlSegments.length - 1];

				File fileDir = new File(storageFolder.getPath() + "\\Images\\");
				fileDir.mkdirs();
				String imageFileName = fileDir.getPath() + "\\" + fileName;

				try {
					Files.write(page.getContentData(), new File(imageFileName));

					totalImg++;
					System.out.println("Downloaded Image # " + totalImg + " " + imageFileName);
					logger.info("Stored: {}", url);
				} catch (IOException iox) {

					System.out.println("ERROR: Downloading Image: " + imageFileName);
					iox.printStackTrace();
					logger.error("Failed to write file: " + imageFileName, iox);
				}
			}
		}
	}

	private static void clearCounters() {
		itemsChecked = 0;
		standard = 0;
		imgFiltered = 0;
		custom = 0;
		wrongPage = 0;
		wrongDomain = 0;
		accepted = 0;
		defaulted = 0;
		img = 0;
	}
}
