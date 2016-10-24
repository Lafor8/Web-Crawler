package tools;
import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.TermCriteria;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class Pipeline {

	public static final String INPUT_DIR = "data/Unique Pod/";
	public static final String OUTPUT_DIR = "output/final/";

	public static int TEST_FILES_COUNT = 71;
	public static int GRABCUT_BORDERBASED_ITERATIONS = 3;
	public static int GRABCUT_MASKBASED_ITERATIONS = 3;
	public static int GRABCUT_FINALRUN_ITERATIONS = 1;

	public Pipeline() {
	}

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		Pipeline pipeline = new Pipeline();

		File inDir = new File(INPUT_DIR);
		File inFiles[] = inDir.listFiles();

		{
			// Clear previous results
			File outDir = new File(OUTPUT_DIR);
//			for (File file : outDir.listFiles())
//				file.delete();
		}

		File currImgFile;
		Mat currImgMat;
		Mat resImgMat;
		for (int i = 52; i < TEST_FILES_COUNT && i < inFiles.length; i++) {
			currImgFile = inFiles[i];

			currImgMat = Imgcodecs.imread(INPUT_DIR + currImgFile.getName());

			Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_00_in.jpg", currImgMat);
			resImgMat = pipeline.process(currImgMat, currImgFile);
			Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_99_out.jpg", resImgMat);
		}
	}

	private Mat process(Mat inMat, File currImgFile) {
		// TODO:
		Mat originalMat = inMat.clone();

		// Down-sampling via Image Pyramids
		// Resizing for reduced computation time
		Imgproc.pyrDown(inMat, inMat);
		Imgproc.pyrDown(inMat, inMat);
		Imgproc.pyrDown(inMat, inMat);

		Mat resMat = new Mat();
		Mat currMat = new Mat();

		currMat = inMat.clone();

		// I. PRE-PROCESSING

		// Converting color models
		Imgproc.cvtColor(currMat, currMat, Imgproc.COLOR_BGR2HSV_FULL);

		// Bilateral Filtering
		// Imgcodecs.imwrite(OUTPUT_FILEPATH + "1_1B4BF.jpg", dst);
		Imgproc.bilateralFilter(currMat.clone(), currMat, 9, 75, 75);

		// Mean Shift Filtering
		Imgproc.pyrMeanShiftFiltering(currMat, currMat, 15, 50, 5, new TermCriteria(TermCriteria.MAX_ITER | TermCriteria.EPS, 50, 0.001));

		// Mean Shift Filtering
		// Imgproc.pyrMeanShiftFiltering(currMat, currMat, 20, 20, 4, new TermCriteria(TermCriteria.MAX_ITER | TermCriteria.EPS, 50, 0.001));
		// Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() +"_032_MSFilter.jpg", currMat);

		// Splitting channels

		List<Mat> list = new ArrayList<>();
		Mat currMatRegions;
		list.add(new Mat());
		list.add(new Mat());
		list.add(new Mat());
		Core.split(currMat, list);

		// Choosing most beneficial channel

		MatOfDouble mean, stdDev;
		mean = new MatOfDouble();
		stdDev = new MatOfDouble();

		int maxStdDev = 0;
		int currStdDev;

		currMatRegions = list.get(0);
		int i;
		for (i = 0; i < 3; i++) {
			Core.meanStdDev(list.get(i), mean, stdDev);
			currStdDev = (int) Math.ceil(stdDev.get(0, 0)[0]);
			if (maxStdDev < currStdDev) {
				currMatRegions = list.get(i);
				maxStdDev = currStdDev;
			}

		}

		// II. SEGMENTATION BY THRESHOLDING

		// Sampling from the center
		Mat regionSample;
		int sampleSize = Math.max(10, (int) (Math.min(inMat.height(), inMat.width()) * .2));
		Rect regionSampleRect = new Rect(inMat.width() / 2 - sampleSize / 2, inMat.height() / 2 - sampleSize / 2, sampleSize, sampleSize);
		regionSample = new Mat(currMatRegions, regionSampleRect);

		Core.meanStdDev(regionSample, mean, stdDev);

		int threshold = (int) Math.ceil(mean.get(0, 0)[0]);
		int range = (int) Math.ceil(stdDev.get(0, 0)[0]) + 25;

		// Thresholding
		Imgproc.threshold(currMatRegions, currMatRegions, threshold - range, 255, Imgproc.THRESH_TOZERO);
		Imgproc.threshold(currMatRegions, currMatRegions, threshold + range, 255, Imgproc.THRESH_TOZERO_INV);

		// Creating the mask
		Imgproc.threshold(currMatRegions, currMatRegions, 1, 1, Imgproc.THRESH_BINARY);

		Mat threshMat = new Mat();
		Core.add(currMatRegions, new Scalar(2), threshMat);

		// III. SEGMENTATION BY GRABCUT (BORDER-BASED)

		// Setting border length
		int borderLength;
		borderLength = Math.max(10, (int) (Math.min(inMat.height(), inMat.width()) * .01));

		// Initializing GrabCut variables
		Mat image, mask, bgModel, fgModel, matOnes;
		Rect rect;

		image = inMat.clone();
		mask = new Mat();
		rect = new Rect(borderLength, borderLength, (int) (image.size().width - borderLength * 2), (int) (image.size().height - borderLength * 2));
		bgModel = new Mat();
		fgModel = new Mat();

		// Segmenting using Border-based GrabCut
		Imgproc.grabCut(image, mask, rect, bgModel, fgModel, GRABCUT_BORDERBASED_ITERATIONS, Imgproc.GC_INIT_WITH_RECT);

		Mat B = new Mat();
		Scalar alpha = new Scalar(80);

		Mat borderBasedMask;
		borderBasedMask = mask.clone();
		Core.multiply(mask, alpha, B);

		// Creating Matrix of ones
		matOnes = new Mat(1, 1, CvType.CV_8U, new Scalar(3));

		Core.compare(mask, matOnes, mask, Core.CMP_EQ);

		Mat fg = new Mat(image.size(), CvType.CV_8UC1, new Scalar(0, 0, 0));
		image.copyTo(fg, mask);

		// IV. SEGMENTATION BY GRABCUT (MASK-BASED)

		image = inMat;
		mask = borderBasedMask.clone();
		bgModel = new Mat();
		fgModel = new Mat();

		B = new Mat();
		alpha = new Scalar(80);

		Core.min(mask.clone(), threshMat, mask);
		Core.multiply(mask.clone(), alpha, B);

		Imgproc.grabCut(image, mask, rect, bgModel, fgModel, GRABCUT_MASKBASED_ITERATIONS, Imgproc.GC_INIT_WITH_MASK);

		// V. SEGMENTATION BY GRABCUT (AS APPLIED TO ORIGINAL IMAGE)
		Imgproc.resize(mask.clone(), mask, originalMat.size(), 0, 0, Imgproc.INTER_NEAREST);

		image = originalMat.clone();
		bgModel = new Mat();
		fgModel = new Mat();

		B = new Mat();
		alpha = new Scalar(80);

		Core.multiply(mask.clone(), alpha, B);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_07_1_Graph.jpg", B);

		Imgproc.grabCut(image, mask, rect, bgModel, fgModel, GRABCUT_FINALRUN_ITERATIONS, Imgproc.GC_INIT_WITH_MASK);

		Core.multiply(mask, alpha, B);
		Core.compare(mask, matOnes, mask, Core.CMP_EQ);

		fg = new Mat(image.size(), CvType.CV_8UC1, new Scalar(0, 0, 0));

		originalMat.copyTo(fg, mask);

		// V. RESULT

		resMat = fg;
		return resMat;
	}
}
// System.out.println(currImgFile.getName());