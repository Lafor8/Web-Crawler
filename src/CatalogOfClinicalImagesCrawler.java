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

public class CatalogOfClinicalImagesCrawler extends WebCrawler {

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

	// Pages and Depth
	public static String seedPage = "";
	public static List<String> transitionPages = new ArrayList<String>();
	public static List<String> leafPages = new ArrayList<String>();

	// Counters
	private static int itemsChecked, standard, imgFiltered, custom, accepted, wrongPage, wrongDomain, defaulted, img;
	public static int totalImg = 0;
	public static int totalImgLinked = 0;
	private static int PAGES = 2;

	public static void configure(String[] domain, String storageFolderName) {
		crawlDomains = domain;

		storageFolder = new File(storageFolderName);
		if (!storageFolder.exists()) {
			storageFolder.mkdirs();
		}

		// Custom Initializations
		clearCounters();

		seedPage = domain[0];
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
			// if (PAGES-- > 0) {
			leafPages.add(href);
			return true;
			// }
		}

		// Height at 2
		else if (leafPages.contains(url.getParentUrl())) {
			if (imgFilters.matcher(href).matches()) {
				img++;
				return true;
			}
		}

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
		else if (page.getWebURL().getParentUrl().equals(seedPage)) {
			// System.out.println("Height at 1");
			if (page.getParseData() instanceof HtmlParseData) {
				HtmlParseData htmlParseData = (HtmlParseData) page.getParseData();
				String text = htmlParseData.getText();
				String html = htmlParseData.getHtml();
				Set<WebURL> links = htmlParseData.getOutgoingUrls();

				try {

					// System.out.println("2: " + "writing");
					String urlSegments[] = page.getWebURL().getPath().split("/");
					String fileName = urlSegments[urlSegments.length - 1];

					File fileDir = new File(storageFolder.getPath() + "/Text/");
					fileDir.mkdirs();
					File file = new File(fileDir.getPath() +"/" + fileName.split("[.]")[0] + "_text.txt");

					if (!file.exists())
						file.createNewFile();
					FileWriter fileWriter = new FileWriter(file);

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

					imgInfo.put("ImageCount", img);
					Elements images = doc.select("img[src~=(?i)\\.(jpe?g)]");
					for (Element image : images) {
						totalImgLinked++;
						imgArray.put(image.attr("src"));
					}
					imgInfo.put("Images", imgArray);

					json.put("ImgInfo", imgInfo);

					// Writing Text Info
					JSONObject textInfo = new JSONObject();

					Elements paras = doc.select("p");
					for (Element para : paras) {
						Elements bolded = para.getElementsByTag("b");
						if (bolded.size() > 0) {
							textInfo.put("Diagnosis", bolded.get(0).text());
							textInfo.put("Description", para.text());
						}
					}

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
		}

		// Height at 2
		else if (leafPages.contains(page.getWebURL().getParentUrl())) {
			// System.out.println("Height at 2");
			if (imgFilters.matcher(url.toLowerCase()).matches() && (page.getContentData().length > (10 * 1024))) {

				String urlSegments[] = page.getWebURL().getPath().split("/");
				String fileName = urlSegments[urlSegments.length - 1];

				File fileDir = new File(storageFolder.getPath() + "/Images/");
				fileDir.mkdirs();
				String imageFileName = fileDir.getPath() + "/" + fileName;

				try {
					Files.write(page.getContentData(), new File(imageFileName));
					totalImg++;
					System.out.println("Downloaded Image # " + totalImg);
					logger.info("Stored: {}", url);
				} catch (IOException iox) {
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
