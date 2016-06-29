import java.util.ArrayList;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class DermoscopyAtlasCrawlerOldController {
	// private static final Logger logger =
	// LoggerFactory.getLogger(MyImageCrawlerController.class);
	static ArrayList<String> nestedCrawlDomains = new ArrayList<String>();

	public static void main(String[] args) throws Exception {
		String[] myArgs = { "InterData", "1", "data_dermoscopyAtlas" };
		args = myArgs;

		// if (args.length < 3) {
		// logger.info("Needed parameters: ");
		// logger.info("\t rootFolder (it will contain intermediate crawl data)");
		// logger.info("\t numberOfCralwers (number of concurrent threads)");
		// logger.info("\t storageFolder (a folder for storing downloaded images)");
		// return;
		// }

		String rootFolder = args[0];
		int numberOfCrawlers = Integer.parseInt(args[1]);
		String storageFolder = args[2];

		CrawlConfig config = new CrawlConfig();

		config.setCrawlStorageFolder(rootFolder);
		config.setMaxDownloadSize(10000000);

		//		  Since images are binary content, 
		//we need to set this parameter to true to make sure they are included in the crawl.
		 
		config.setIncludeBinaryContentInCrawling(true);

		String[] crawlDomains = { "http://www.dermoscopyatlas.com/diagindex.cfm" };

		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);
		for (String domain : crawlDomains) {
			controller.addSeed(domain);
		}

		DermoscopyAtlasOldCrawler.configure(crawlDomains, storageFolder);
		controller.start(DermoscopyAtlasOldCrawler.class, numberOfCrawlers);

		pageFetcher = new PageFetcher(config);
		robotstxtConfig = new RobotstxtConfig();
		robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		controller = new CrawlController(config, pageFetcher, robotstxtServer);

		System.out.println("Pages that will be crawled " + nestedCrawlDomains.size());
		for (String domain : nestedCrawlDomains) {
			controller.addSeed(domain);
		}
		
		String[] stockArr = new String[nestedCrawlDomains.size()];
		stockArr = nestedCrawlDomains.toArray(stockArr);

		DermoscopyAtlasOldCrawler.configure(stockArr, storageFolder);
		controller.start(DermoscopyAtlasOldCrawler.class, numberOfCrawlers);
	}
}
