import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import java.util.UUID;
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

public class DermoscopyAtlasCrawler extends WebCrawler {

	// Filters
	private static final Pattern standardFilters = Pattern.compile(".*(\\.(css|js|mid|mp2|mp3|mp4|wav|avi|mov|mpeg|ram|m4v|pdf" + "|rm|smil|wmv|swf|wma|zip|rar|gz))$");
	private static final Pattern imgFilters = Pattern.compile(".*(\\.(bmp|jpe?g|png|tiff?))$");
	private static final Pattern customFilters = Pattern.compile(".*(\\.(cfm))$");

	private static final Pattern pageFilters = Pattern.compile(".*((.*(index|links|credits|browse).*)\\.[A-z]{1,4})$");

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

			accepted++;
			if (PAGES-- > 0) {
				// System.out.println("VALID @ Height 1:\t" + href);
				transitionPages.add(href);
				return true;
			} else
				return false;
		}

		// Height at 2
		else if (transitionPages.contains(url.getParentUrl().toLowerCase())) {
			// System.out.println("\t\t" + url.getAnchor());
			// System.out.println("\t\t" + url.getDepth());
			// System.out.println("\t\t" + url.getDocid());
			// System.out.println("\t\t" + url.getDomain());
			// System.out.println("\t\t" + url.getParentDocid());
			// System.out.println("\t\t" + url.getParentUrl());
			// System.out.println("\t\t" + url.getPath());
			// System.out.println("\t\t" + url.getPriority());
			// System.out.println("\t\t" + url.getSubDomain());
			// System.out.println("\t\t" + url.getTag());
			// System.out.println("\t\t" + url.getURL());

			itemsChecked++;
			if (standardFilters.matcher(href).matches()) {
				// System.out.println("standard:\t " + href);
				standard++;
				return false;
			}
			if (imgFilters.matcher(href).matches()) {
				// System.out.println("img:\t " + href);
				if (PAGES2-- > 0) {
					img++;
					return true;
				} else
					return false;
			}

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

			accepted++;
			// System.out.println("VALID @ Height 2:\t " + href);
			leafPages.add(href);
			return true;
		}
		// Height at 3
		else if (leafPages.contains(url.getParentUrl().toLowerCase())) {
			// DO NOTHING
			// System.out.println("FOUND @ Height 3:\t " + href);
			// if (imgFilters.matcher(href).matches()) {
			// System.out.println("VALID @ Height 3:\t " + href);
			// img++;
			// return true;
			// }
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
			// System.out.println("Height at 1 " + url);

		}

		// Height at 2
		else if (transitionPages.contains(page.getWebURL().getParentUrl().toLowerCase())) {
			// System.out.println("Height at 2");
			if (imgFilters.matcher(url.toLowerCase()).matches() && page.getParseData() instanceof BinaryParseData) {

				if ((page.getContentData().length > (1 * 1024))) {

					// System.out.println("SAVING IMAGE");
					WebURL largeImgUrl = new WebURL();
					String largeImgUrlStr = page.getWebURL().getURL().replace("th", "lg");
					largeImgUrl.setURL(page.getWebURL().getURL().replace("th", "lg"));
					Page largeImgPage = new Page(largeImgUrl);

					String urlSegments[] = largeImgPage.getWebURL().getURL().split("/");
					String fileName = urlSegments[urlSegments.length - 1];

					File fileDir = new File(storageFolder.getPath() + "\\Images\\");
					fileDir.mkdirs();
					String imageFileName = fileDir.getPath() + "\\" + fileName;

					// System.out.println("CHECK " + imageFileName);

					// System.out.println("CHECK " + largeImgPage.getContentData());
					try {
						Utils.saveImage(largeImgUrlStr, imageFileName);
						// Files.write(largeImgPage.getContentData(), new File(imageFileName));

						totalImg++;
						System.out.println("Downloaded Image # " + totalImg + " " + imageFileName);
						logger.info("Stored: {}", url);
					} catch (IOException iox) {

						System.out.println("ERROR: Downloading Image: " + imageFileName);
						iox.printStackTrace();
						logger.error("Failed to write file: " + imageFileName, iox);
					}
				} else {
					System.out.println("ERROR ERROR ERROR ERROR ERROR");
				}

			} else if (page.getParseData() instanceof HtmlParseData) {
				HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
				String text = htmlParseData.getText();
				String html = htmlParseData.getHtml();
				Set<WebURL> links = htmlParseData.getOutgoingUrls();

				try {

					// System.out.println("\t\t" + page.getWebURL().getAnchor());
					// System.out.println("\t\t" + page.getWebURL().getDepth());
					// System.out.println("\t\t" + page.getWebURL().getDocid());
					// System.out.println("\t\t" + page.getWebURL().getDomain());
					// System.out.println("\t\t" + page.getWebURL().getParentDocid());
					// System.out.println("\t\t" + page.getWebURL().getParentUrl());
					// System.out.println("\t\t" + page.getWebURL().getPath());
					// System.out.println("\t\t" + page.getWebURL().getPriority());
					// System.out.println("\t\t" + page.getWebURL().getSubDomain());
					// System.out.println("\t\t" + page.getWebURL().getTag());
					// System.out.println("\t\t" + page.getWebURL().getURL());

					// TODO: Change getPath to getURL
					// System.out.println("2: " + "writing");
					String urlSegments[] = page.getWebURL().getURL().split("/");
					String fileName = urlSegments[urlSegments.length - 1].replaceAll("[^a-zA-Z0-9]", "_");

					File fileDir = new File(storageFolder.getPath() + "/Text/");
					fileDir.mkdirs();
					File file = new File(fileDir.getPath() + "\\" + fileName.split("[.]")[0] + "_text.txt");

					// System.out.println("TEXT: " + fileDir.getPath() + "\\" + fileName.split("[.]")[0] + "_text.txt");

					if (!file.exists())
						file.createNewFile();
					FileWriter fileWriter = new FileWriter(file);

					// System.out.println("File created");

					// Getting HTML Doc

					JSONArray imgArray = new JSONArray();
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

					String imgId = page.getWebURL().getURL().toLowerCase();
					imgId = imgId.split("imageid=")[1].split("&")[0];

					imgInfo.put("ImageCount", 1);
					Element image = doc.getElementById("pointer" + imgId);
					if (image != null) {
						totalImgLinked++;
						String imgSrc = image.attr("style");
						imgSrc = imgSrc.split("'")[1].split("'")[0];
						imgArray.put(imgSrc);
					} else {
						System.out.println(imgId + " NOT FOUND " + "pointer" + imgId);
					}
					imgInfo.put("Images", imgArray);

					json.put("ImgInfo", imgInfo);

					// Writing Text Info
					JSONObject textInfo = new JSONObject();

					Elements paras = doc.select("table").get(2).select("td");
					Element curr;

					// Details
					curr = paras.get(5).clone();

					for (Element formElm : curr.select("form"))
						formElm.remove();
					for (Element aElm : curr.select("a"))
						aElm.remove();
					for (Element brElm : curr.select("br"))
						brElm.remove();

					// System.out.println(curr.html());

					{
						String[] htmlSegments = curr.html().split("</strong>");
						int i = 1;
						for (Element strongElm : curr.select("strong")) {
							textInfo.put(strongElm.text().replaceAll("[^A-z0-9-_ ]", "").trim(), htmlSegments[i].split("<strong>")[0].replaceAll("[^A-z0-9-_ ]", "").trim());
							i++;
						}
					}

					// Description
					curr = paras.get(6);
					textInfo.put("Description", curr.text().trim());

					// Links
					curr = paras.get(7);
					// System.out.println(curr.html());
					{
						JSONArray linksJSON = new JSONArray();
						for (Element linkElm : curr.select("a")) {
							linksJSON.put("http://www.dermoscopyatlas.com/" + linkElm.attr("href"));
						}
						textInfo.put("Links", linksJSON);
					}

					// History
					curr = paras.get(8);
					textInfo.put("History", curr.text().trim());

					json.put("TextInfo", textInfo);

					// System.out.println("Writing to file");
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
		}

		// Height at 3
		else if (leafPages.contains(page.getWebURL().getParentUrl().toLowerCase()))

		{
			// DO NOTHING
			// System.out.println("Height at 3");
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

// NOTES:
//
// Crawling Logic:
//
// crawl(givenPage)
// shouldVisit(givenPage.links)
// foreach link in givenPage.links
// if(shouldVisit(link)
// Pages.add(link)
// visit(givenPage)
// foreach page in Pages
// crawl(page)
//
// System.out.println(page.getWebURL().getAnchor()); // null
// System.out.println(page.getWebURL().getDocid()); // 1
// System.out.println(page.getWebURL().getDomain()); // ucsd.edu
// System.out.println(page.getWebURL().getParentDocid()); // 0
// System.out.println(page.getWebURL().getParentUrl()); // null
// System.out.println(page.getWebURL().getPath()); // /clinicalimg/skin.htm
// System.out.println(page.getWebURL().getSubDomain()); // meded
// System.out.println(page.getWebURL().getTag()); // null
// System.out.println(page.getWebURL().getURL()); // https://meded.ucsd.edu/clinicalimg/skin.htm

// Visiting: "https://meded.ucsd.edu/clinicalimg/skin.htm"
// null
// 1
// ucsd.edu
// 0
// null
/// clinicalimg/skin.htm
// meded
// null
// https://meded.ucsd.edu/clinicalimg/skin.htm
// https://meded.ucsd.edu/clinicalimg/
// 102 pages marked for crawling.
// Details written to file.
//
// __________________________________________________________________________________________________________
//
//
// Visiting: "https://meded.ucsd.edu/clinicalimg/skin_maceration.htm"
// Macerated skin
// 2
// ucsd.edu
// 1
// https://meded.ucsd.edu/clinicalimg/skin.htm
/// clinicalimg/skin_maceration.htm
// meded
// null
// https://meded.ucsd.edu/clinicalimg/skin_maceration.htm
// https://meded.ucsd.edu/clinicalimg/skin_maceration.htm
//
// __________________________________________________________________________________________________________
//
//
// Visiting: "https://meded.ucsd.edu/clinicalimg/skin_stills.htm"
// Still's Disease
// 3
// ucsd.edu
// 1
// https://meded.ucsd.edu/clinicalimg/skin.htm
/// clinicalimg/skin_stills.htm
// meded
// null
// https://meded.ucsd.edu/clinicalimg/skin_stills.htm
// https://meded.ucsd.edu/clinicalimg/skin_stills.htm
