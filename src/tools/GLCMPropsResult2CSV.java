package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class GLCMPropsResult2CSV {
	public static void main(String args[]) throws IOException {
		File dir = new File("Data\\outp");

		String vals[], filename, line;
		StringBuilder sb = new StringBuilder();

		sb.append("file,morph");
		// sb.append(",autoc,contr,corrm,corrp,cprom,cshad,dissi,energ,entro,homom,homop,maxpr,sosvh,savgh,svarh,senth,dvarh,denth,inf1h,inf2h,indnc,idmnc");
		sb.append(",Circularity,Solidity,No. of Connected Components,Rows,Columns"+
		",Contrast(Image),Correlation(Image),Energy(Image),Homogeneity(Image)"+
		",Contrast(Lesion),Correlation(Lesion),Energy(Lesion),Homogeneity(Lesion)"+
		",Contrast(Skin),Correlation(Skin),Energy(Skin),Homogeneity(Skin)"+
		",Contrast(Lesion/Image),Correlation(Lesion/Image),Energy(Lesion/Image),Homogeneity(Lesion/Image)"+
		",Contrast(Lesion/Skin),Correlation(Lesion/Skin),Energy(Lesion/Skin),Homogeneity(Lesion/Skin)"+
		",Ratio of Major and Minor Axis Lengths"
);
		sb.append("\n");

		for (File morphDir : dir.listFiles()) {
			for (File file : morphDir.listFiles()) {
				// System.out.println(file.getPath());

				if (!file.getName().split("\\.")[1].equals("csv"))
					continue;

				filename = file.getName().replace(".csv", ".jpg");

				{
					FileInputStream fis = new FileInputStream(file);
					BufferedReader br = new BufferedReader(new InputStreamReader(fis));

					br.readLine();
					line = br.readLine();
					if (line.toLowerCase().contains("nan"))
						continue;
					
					vals = line.split(",");

					sb.append(file.getName());
					sb.append(",");
					sb.append(morphDir.getName());
					for (String str : vals) {
						sb.append(",");
						sb.append(str);
					}

					br.close();
					fis.close();
				}

				sb.append("\n");
			}
		}

		{
			File out = new File("Data\\resp.csv");
			FileWriter fw = new FileWriter(out);

			fw.write(sb.toString());
			fw.close();
		}

		System.out.println(sb.toString());
	}
}
