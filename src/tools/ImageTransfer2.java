package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

public class ImageTransfer2 {

	public static void main(String[] args) throws IOException {
		File inFile = new File("Data\\toProc2.csv");
		File outDir = new File("Data\\4ClassUnique");

		FileInputStream fis = new FileInputStream(inFile);
		BufferedReader br = new BufferedReader(new InputStreamReader(fis));

		String line = null;
		String vals[], morph, path, filename;
		File from, to;

		while ((line = br.readLine()) != null) {
			vals = line.split(",");
			morph = vals[0].trim();
			path = vals[1].trim();

			vals = path.split("\\\\");
			filename = vals[vals.length - 1];

			from = new File(path);
			to = new File(outDir.getPath() + "\\" + morph + "\\" + filename);

			to.mkdirs();

			Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
		}

		br.close();
		fis.close();
	}

}
