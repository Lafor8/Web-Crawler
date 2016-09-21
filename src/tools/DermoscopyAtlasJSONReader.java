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
import org.json.JSONObject;

public class DermoscopyAtlasJSONReader {
	public static final File dir = new File("data_dermoscopyAtlas");
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

		// List<String> path = new ArrayList<String>();
		String path;

		String description;
		String diagnosis;

		String morphology;

		int dataSourceID;
		int pathUrlID;
		int pageID;
		int imageID;
		int diagnosisID;
		int morphologyID;

		name = "Dermoscopy Atlas";
		seedUrl = "http://www.dermoscopyatlas.com/diagindex.cfm";
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
			sbDesc.append(((String) txtInfo.get("Description")).trim());
			sbDesc.append(" ; ");
			sbDesc.append(((String) txtInfo.get("History")).trim());
			sbDesc.append(" ; Site: ");
			sbDesc.append(((String) txtInfo.get("Site")).trim());
			sbDesc.append(" ; Age: ");
			sbDesc.append(((String) txtInfo.get("Age")).trim());
			sbDesc.append(" ; Sex:");
			sbDesc.append(((String) txtInfo.get("Sex")).trim());
			description = sbDesc.toString();
			// System.out.println(description);

			rawTextInfo = jsonObj.toString();

			pageID = DBProcTools.insertUniqueRecord("Page", "pageID", new String[] { "url", "anchor", "description", "rawTextInfo", "pathUrlID", "dataSourceID", url, anchor, description, rawTextInfo, String.valueOf(pathUrlID), String.valueOf(dataSourceID) });
			 System.out.println(pageID);

			JSONArray imgList = (JSONArray) imgInfo.get("Images");
			String img;
			for (int i = 0; i < imgList.length(); ++i) {
				String imgSegs[] = ((String) imgList.get(i)).split("/");
				img = imgSegs[imgSegs.length - 1];

				path = "";
				boolean imageExists = new File(imgDir.getPath() + "\\" + img).exists();

				if (imageExists)
					path = img;
				else
					path = "ERROR";

				imageID = DBProcTools.insertUniqueRecord("Image", "imageID", new String[] { "path", "captureType", "pageID", path, "unknown", String.valueOf(pageID) });

				// System.out.println(imageID);
			}

			diagnosis = "";
			diagnosis = ((String) txtInfo.get("Diagnosis")).trim();

			String type = (String) txtInfo.get("Type");
			if (!captureTypes.contains(type))
				captureTypes.add(type);

			diagnosisID = DBProcTools.insertUniqueRecord("Diagnosis", "diagnosisID", new String[] { "diagnosis", diagnosis });
			// System.out.println(diagnosisID);

			DBProcTools.insertRecord("PageDiagnosisLink", new String[] { "pageID", "diagnosisID", String.valueOf(pageID), String.valueOf(diagnosisID) });

			String morphSegs[] = ((String) txtInfo.get("Morphologies")).trim().split("( ){2}");
			for (String morph : morphSegs) {
				morphology = morph;

				morphology = morphology.replace("Noduleblack", "Nodule black");
				morphology = morphology.replace("Nodulepurple", "Nodule purple");
				morphology = morphology.replace("Nodule skincoloured", "Nodule skin coloured");
				morphology = morphology.replace("Noduleyellow", "Nodule yellow");
				morphology = morphology.replace("Pathinflammatory", "Path inflammatory");
				morphology = morphology.replace("Pathpagetoid", "Path pagetoid");
				morphology = morphology.replace("Rednonscaly", "Red nonscaly");
				morphology = morphology.replace("Redscaly", "Red scaly");
				morphology = morphology.replace("Skin colourednonscaly", "Skin coloured nonscaly");
				morphology = morphology.replace("Skin colouredscaly", "Skin coloured scaly");

				morphologyID = DBProcTools.insertUniqueRecord("Morphology", "morphologyID", "morphology", "morphologyType", morphology, "unknown");

				DBProcTools.insertRecord("PageMorphologyLink", "pageID", "morphologyID", String.valueOf(pageID), String.valueOf(morphologyID));
			}

			// System.out.println();
		}

		for (String type : captureTypes)
			System.out.println("Types :" + type);

		// Types :Heine
		// Types :Dermlite Fuid
		// Types :
		// Types :Dermlite
		// Types :Dermlite Polarised
		// Types :Mixed
		// Types :Molemax
		// Types :Fotofinder

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
