package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ImageTransfer {

	public static void main(String[] args) throws IOException {
		File dir = new File("data_dermoscopyAtlas");
		File imgDir = new File(dir.getPath() + "\\Images");
		File txtDir = new File(dir.getPath() + "\\Text");

		List<String> list = new ArrayList<>();
		List<String> notFound = new ArrayList<>();

		// System.out.println(dir.getPath());
		// System.out.println(dir.getPath() + "\\Images");
		for (String fileName : imgDir.list()) {
			// System.out.println(fileName);
			list.add(fileName);
		}

		for (String fileName : txtDir.list()) {

			FileInputStream fis = new FileInputStream(new File(txtDir.getPath() + "\\" + fileName));

			// Construct BufferedReader from InputStreamReader
			BufferedReader br = new BufferedReader(new InputStreamReader(fis));

			String line = null;

			StringBuilder sb = new StringBuilder();

			while ((line = br.readLine()) != null) {
				sb.append(line);
				sb.append(' ');
			}

			JSONObject jsonObj = new JSONObject(sb.toString());
			JSONObject imgInfo = (JSONObject) jsonObj.get("ImgInfo");
			JSONArray img = (JSONArray) imgInfo.get("Images");

			String imgFileName = img.toString().split("/")[4].split("\"")[0];


			System.out.println();
			
			if (!list.contains(imgFileName))
				notFound.add(imgFileName);

			br.close();
		}
		
		System.out.println(notFound.size());

		for (String str : notFound) {
			System.out.println(str);
		}
	}

}
