import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.uci.ics.crawler4j.crawler.CrawlConfig;
import edu.uci.ics.crawler4j.crawler.CrawlController;
import edu.uci.ics.crawler4j.fetcher.PageFetcher;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtConfig;
import edu.uci.ics.crawler4j.robotstxt.RobotstxtServer;

public class DermISCrawlerController {
	private static final Logger logger = LoggerFactory.getLogger(MyImageCrawlerController.class);

	public static void main(String[] args) throws Exception {
		String[] myArgs = { "InterData", "1", "data_dermIS" };
		args = myArgs;

		if (args.length < 3) {
			logger.info("Needed parameters: ");
			logger.info("\t rootFolder (it will contain intermediate crawl data)");
			logger.info("\t numberOfCralwers (number of concurrent threads)");
			logger.info("\t storageFolder (a folder for storing downloaded images)");
			return;
		}

		// I. Setting up basic parameters
		String rootFolder = args[0];
		int numberOfCrawlers = Integer.parseInt(args[1]);
		String storageFolder = args[2];

		CrawlConfig config = new CrawlConfig();

		config.setCrawlStorageFolder(rootFolder);
		config.setMaxDownloadSize(10000000);

		// Since images are binary content,
		// we need to set this parameter to true to make sure they are included in the crawl.
		config.setIncludeBinaryContentInCrawling(true);

		String[] crawlDomains = { "http://www.dermis.net/dermisroot/en/list/all/search.htm" };

		// II. Preparing crawler
		PageFetcher pageFetcher = new PageFetcher(config);
		RobotstxtConfig robotstxtConfig = new RobotstxtConfig();
		RobotstxtServer robotstxtServer = new RobotstxtServer(robotstxtConfig, pageFetcher);
		CrawlController controller = new CrawlController(config, pageFetcher, robotstxtServer);

		for (String domain : crawlDomains) {
			controller.addSeed(domain);
		}

		// Running Crawler
		DermISCrawler.configure(crawlDomains, storageFolder);
		controller.start(DermISCrawler.class, numberOfCrawlers);

		// Saving Image download details
		File file = new File(new File(storageFolder).getPath() + "/Crawling Details.txt");
		try {
			if (!file.exists())
				file.createNewFile();

			FileWriter fileWriter = new FileWriter(file, true);

			fileWriter.write("Images Downloaded = " + DermISCrawler.totalImg + "\r\n");
			fileWriter.write("Images Linked = " + DermISCrawler.totalImgLinked + "\r\n");
			fileWriter.flush();
			fileWriter.close();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
