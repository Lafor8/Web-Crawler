package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DermISJSONReader {
	public static final File dir = new File("data_dermIS");
	public static final File imgDir = new File(dir.getPath() + "\\Images");
	public static final File txtDir = new File(dir.getPath() + "\\Text");

	public static void main(String args[]) throws Exception {
		run();
	}

	public static void run() throws Exception {
		DBProcTools.startConnection();

		String directory;
		String name;
		String seedUrl;

		String rawTextInfo;

		String pathUrl;
		String url;
		String anchor;

		String path;

		String description;
		String diagnosis;

		String morphology;

		List<String> collapse = new ArrayList<>();

		int dataSourceID;
		int pathUrlID;
		int pageID;
		int imageID;
		int diagnosisID;
		int morphologyID;

		name = "DermIS";
		seedUrl = "http://www.dermis.net/dermisroot/en/list/all/search.htm";
		directory = imgDir.getPath();
		dataSourceID = DBProcTools.insertUniqueRecord("DataSource", "dataSourceID", new String[] { "name", "seedUrl", "directory", name, seedUrl, directory });
		// System.out.println(dataSourceID);

		List<String> captureTypes = new ArrayList<String>();

		for (String fileName : txtDir.list()) {
			JSONObject jsonObj = loadJSONFile(txtDir.getPath(), fileName);
			JSONObject webUrlInfo = (JSONObject) jsonObj.get("WebURLInfo");
			JSONObject imgInfo = (JSONObject) jsonObj.get("ImgInfo");
			JSONObject txtInfo = (JSONObject) jsonObj.get("TextInfo");

			pathUrl = "";
			pathUrl = webUrlInfo.getString("PathURL");
			pathUrlID = DBProcTools.insertUniqueRecord("PathURL", "pathUrlID", new String[] { "pathUrl", pathUrl });
			// System.out.println(pathUrlID);

			url = "";
			url = webUrlInfo.getString("URL");

			anchor = "";
			// anchor = webUrlInfo.getString("Anchor").replaceAll("[\\s]+", " ");

			description = "";
			StringBuilder sbDesc = new StringBuilder();

			JSONArray descList = (JSONArray) txtInfo.get("Descriptions");

			for (int i = 0; i < descList.length(); ++i) {
				String item = ((String) descList.get(i)).trim();
				if (!collapse.contains(item) && item.length() > 1)
					collapse.add(item);
			}

			if (collapse.size() > 0) {
				sbDesc.append("Descriptions: ");

				for (int i = 0; i < collapse.size(); ++i) {
					if (i != 0)
						sbDesc.append(", ");
					sbDesc.append(collapse.get(i));
				}
				collapse.clear();
			}

			JSONArray locList = (JSONArray) txtInfo.get("Localizations");

			for (int i = 0; i < locList.length(); ++i) {
				String item = ((String) locList.get(i)).trim();

				if (!collapse.contains(item) && item.length() > 1)
					collapse.add(item);
			}
			if (collapse.size() > 0) {
				sbDesc.append(" ; Localizations: ");
				for (int i = 0; i < collapse.size(); ++i) {
					if (i != 0)
						sbDesc.append(", ");
					sbDesc.append(collapse.get(i));
				}
				collapse.clear();
			}

			try {
				txtInfo.get("Sex");
				sbDesc.append(" ; Sex: ");
				sbDesc.append(((String) txtInfo.get("Sex")).trim());
			} catch (JSONException j) {
			}
			try {
				txtInfo.get("Age");
				sbDesc.append(" ; Age: ");
				sbDesc.append(txtInfo.getInt("Age"));
			} catch (JSONException j) {
			}
			try {
				txtInfo.get("Race");
				sbDesc.append(" ; Race:");
				sbDesc.append(((String) txtInfo.get("Race")).trim());
			} catch (JSONException j) {
			}

			description = sbDesc.toString();
			// System.out.println(description);

			rawTextInfo = jsonObj.toString();

			pageID = DBProcTools.insertUniqueRecord("Page", "pageID", new String[] { "url", "anchor", "description", "rawTextInfo", "pathUrlID", "dataSourceID", url, anchor, description, rawTextInfo, String.valueOf(pathUrlID), String.valueOf(dataSourceID) });
			System.out.println(pageID);

			JSONArray imgList = (JSONArray) imgInfo.get("Images");
			String img;
			for (int i = 0; i < imgList.length(); ++i) {
				String imgSegs[] = ((String) imgList.get(i)).split("/");
				img = imgSegs[imgSegs.length - 3] + "_" + imgSegs[imgSegs.length - 1];

				path = "";
				boolean imageExists = new File(imgDir.getPath() + "\\" + img).exists();

				if (imageExists)
					path = img;
				else
					path = "ERROR";

				imageID = DBProcTools.insertUniqueRecord("Image", "imageID", new String[] { "path", "captureType", "pageID", path, "unknown", String.valueOf(pageID) });

				System.out.println(imageID);
			}

			diagnosis = "";
			diagnosis = ((String) txtInfo.get("Diagnosis")).trim().replaceAll("[\n\r]", " ").replaceAll("[\\s]{2,}", " ");
			collapse.add(diagnosis);

			JSONArray diagList = (JSONArray) txtInfo.get("Diagnoses");
			for (int i = 0; i < diagList.length(); ++i) {
				String item = ((String) diagList.get(i)).trim();

				if (!collapse.contains(item) && item.length() > 1)
					collapse.add(item);
			}

			for (String diag : collapse) {
				diagnosis = diag;

				diagnosisID = DBProcTools.insertUniqueRecord("Diagnosis", "diagnosisID", new String[] { "diagnosis", diagnosis });
				// System.out.println(diagnosisID);

				DBProcTools.insertRecord("PageDiagnosisLink", new String[] { "pageID", "diagnosisID", String.valueOf(pageID), String.valueOf(diagnosisID) });

			}
			collapse.clear();

			JSONArray morphList = (JSONArray) txtInfo.get("Morphologies");
			for (int i = 0; i < morphList.length(); ++i) {
				String item = ((String) morphList.get(i)).trim();
				if (!collapse.contains(item) && item.length() > 1)
					collapse.add(item);
			}

			// System.out.println(collapse + " ## " + collapse.size());
			// separate comma values
			List<String> toRemove = new ArrayList<>();
			List<String> toAdd = new ArrayList<>();
			for (String morphEntry : collapse) {
				String morphSegs[] = morphEntry.split(",");

				if (morphSegs.length > 1) {

					// System.out.println(collapse + " ### " + collapse.size() + " ### " + morphSegs.length + " ### " + morphSegs[0]);
					for (int i = 0; i < morphSegs.length; ++i) {
						String diag = morphSegs[i].trim();
						if (!collapse.contains(diag) && !toAdd.contains(diag))
							toAdd.add(diag);
					}
					toRemove.add(morphEntry);
				}
			}

			collapse.removeAll(toRemove);
			collapse.addAll(toAdd);
			// System.out.println(toRemove);
			// System.out.println(toAdd);
			// System.out.println(collapse + " ## " + collapse.size());
			// System.out.println();

			for (String morph : collapse) {
				morphology = morph;

				morphologyID = DBProcTools.insertUniqueRecord("Morphology", "morphologyID", "morphology", "morphologyType", morphology, "unknown");

				DBProcTools.insertRecord("PageMorphologyLink", "pageID", "morphologyID", String.valueOf(pageID), String.valueOf(morphologyID));
			}
			collapse.clear();

			// System.out.println();
		}

		DBProcTools.closeConnection();
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
