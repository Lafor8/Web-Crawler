package tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;

public class GLCMResult2CSV {
	public static void main(String args[]) throws IOException {
		File dir = new File("Data\\out");

		String vals[], filename, line;
		StringBuilder sb = new StringBuilder();

		sb.append("file,morph");
		// sb.append(",autoc,contr,corrm,corrp,cprom,cshad,dissi,energ,entro,homom,homop,maxpr,sosvh,savgh,svarh,senth,dvarh,denth,inf1h,inf2h,indnc,idmnc");
		sb.append(
				",Autocorrelation,Contrast,Correlation1,Correlation2,Cluster Prominence,Cluster Shade,Dissimilarity,Energy,Entropy,Homogeneity1,Homogeneity2,Maximum probability,Sum of sqaures,Sum average,Sum variance,Sum entropy,Difference variance,Difference entropy,Information measure of correlation1,Informaiton measure of correlation2,Inverse difference normalized,Inverse difference moment normalized");
		sb.append("\n");

		for (File morphDir : dir.listFiles()) {
			for (File file : morphDir.listFiles()) {
				// System.out.println(file.getPath());

				filename = file.getName().replace(".csv", ".jpg");

				sb.append(file.getName());
				sb.append(",");
				sb.append(morphDir.getName());
				{
					FileInputStream fis = new FileInputStream(file);
					BufferedReader br = new BufferedReader(new InputStreamReader(fis));

					br.readLine();
					line = br.readLine();
					vals = line.split(",");
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
			File out = new File("Data\\res.csv");
			FileWriter fw = new FileWriter(out);

			fw.write(sb.toString());
			fw.close();
		}

		System.out.println(sb.toString());
	}
}
