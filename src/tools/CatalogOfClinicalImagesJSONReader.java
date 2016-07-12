package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class CatalogOfClinicalImagesJSONReader {
	public static void main(String args[]) throws IOException {
		run();
	}

	public static void run() throws IOException {
		File dir = new File("data_catalogOfClinicalImages");
		File imgDir = new File(dir.getPath() + "\\Images");
		File txtDir = new File(dir.getPath() + "\\Text");

		String directory;
		String name;
		String seedUrl;

		String rawTextInfo;

		String pathUrl;
		String url;
		String anchor;

		List<String> path = new ArrayList<String>();

		String description;
		String diagnosis;

		directory = imgDir.getPath();
		System.out.println(directory);
		name = "Catalog of Clinical Images";
		System.out.println(name);
		seedUrl = "https://meded.ucsd.edu/clinicalimg/skin.htm";
		System.out.println(seedUrl);

		System.out.println();

		for (String fileName : txtDir.list()) {
			JSONObject jsonObj = loadJSONFile(txtDir.getPath(), fileName);
			JSONObject webUrlInfo = (JSONObject) jsonObj.get("WebURLInfo");
			JSONObject imgInfo = (JSONObject) jsonObj.get("ImgInfo");
			JSONObject txtInfo = (JSONObject) jsonObj.get("TextInfo");

			// rawTextInfo = jsonObj.toString();
			// System.out.println(rawTextInfo);

			// String imgFileName = img.toString().split("/")[4].split("\"")[0];

			// pathUrl = "";
			// pathUrl = webUrlInfo.getString("PathURL");
			//
			// url = "";
			// url = webUrlInfo.getString("URL");

			// anchor = "";
			// anchor = webUrlInfo.getString("Anchor").replaceAll("[\\s]+", " ");

			// System.out.println(pathUrl);
			// System.out.println(url);
			// System.out.println(anchor);

			// JSONArray imgList = (JSONArray) imgInfo.get("Images");
			// String img;
			// for (int i = 0; i < imgList.length(); ++i) {
			// img = (String) imgList.get(i);
			//
			// path.add(img);
			// boolean imageExists = new File(imgDir.getPath() + "\\" + img).exists();
			//
			// if (imageExists)
			// path.add(img);
			// else
			// path.add("ERROR");
			//
			// System.out.println(imageExists);
			// }
			// path.clear();

			//
			// diagnosis = "";
			// diagnosis = ((String) txtInfo.get("Diagnosis")).replaceAll("[:.]", "").trim();
			//
			// description = "";
			// String strArr[] = ((String) txtInfo.get("Description")).split(":");
			// if (strArr.length == 2)
			// description = strArr[1].trim();
			// else if (strArr.length == 1)
			// description = strArr[0].trim();
			// else
			// description = "ERROR";

			// System.out.println(diagnosis);
			// System.out.println(description);

			System.out.println();
		}
	}

	public static JSONObject loadJSONFile(String dirName, String fileName) throws IOException {
		FileInputStream fis = new FileInputStream(new File(dirName + "\\" + fileName));

		// Construct BufferedReader from InputStreamReader
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;

		StringBuilder sb = new StringBuilder();

		while ((line = br.readLine()) != null) {
			sb.append(line);
			sb.append(' ');
		}

		br.close();

		JSONObject jsonObj = new JSONObject(sb.toString());
		return jsonObj;
	}
}
