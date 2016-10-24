package tools;

import java.io.File;
import java.util.ArrayList;
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

public class ProcessImage {

	public static final String INPUT_DIR = "Data/ImgProc/in/";
	public static final String OUTPUT_DIR = "Data/ImgProc/out/";

	public static int GRABCUT_BORDERBASED_ITERATIONS = 3;
	public static int GRABCUT_MASKBASED_ITERATIONS = 3;
	public static int GRABCUT_FINALRUN_ITERATIONS = 1;

	public static void main(String[] args) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);

		File inDir = new File(INPUT_DIR);
		File outDir = new File(OUTPUT_DIR);

		Mat currImgMat;
		Mat resImgMat;

		int lim = 5;

		for (File morphDir : inDir.listFiles()) {
			for (File imgFile : morphDir.listFiles()) {

				currImgMat = Imgcodecs.imread(imgFile.getPath());

				Imgcodecs.imwrite(OUTPUT_DIR + imgFile.getName() + "_00_in.jpg", currImgMat);
				resImgMat = process2(currImgMat, imgFile);
				// Imgcodecs.imwrite(OUTPUT_DIR + imgFile.getName() + "_99_out.jpg", resImgMat);

				lim--;
				if (lim-- <= 0)
					break;
			}
			break;
		}
	}

	private static Mat process(Mat inMat, File currImgFile) {
		Mat resMat = new Mat();
		Mat currMat = new Mat();

		currMat = inMat.clone();

		// I. PRE-PROCESSING

		// Converting color models
		Imgproc.cvtColor(currMat, currMat, Imgproc.COLOR_BGR2HSV_FULL);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_01_HSVConvert.jpg", currMat);

		// Down-sampling via Image Pyramids

		// Bilateral Filtering
		// Imgcodecs.imwrite(OUTPUT_FILEPATH + "1_1B4BF.jpg", dst);
		Imgproc.bilateralFilter(currMat.clone(), currMat, 9, 75, 75);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_02_BiFilter.jpg", currMat);

		// Mean Shift Filtering
		Imgproc.pyrMeanShiftFiltering(currMat, currMat, 15, 50, 5, new TermCriteria(TermCriteria.MAX_ITER | TermCriteria.EPS, 50, 0.001));
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_03_MSFilter.jpg", currMat);

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
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_04_HSVSplit_Hue.jpg", list.get(0));
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_04_HSVSplit_Saturation.jpg", list.get(1));
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_04_HSVSplit_Val.jpg", list.get(2));

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
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_04_Sample.jpg", regionSample);

		int threshold = (int) Math.ceil(mean.get(0, 0)[0]);
		int range = (int) Math.ceil(stdDev.get(0, 0)[0]) + 25;

		// Thresholding
		Imgproc.threshold(currMatRegions, currMatRegions, threshold - range, 255, Imgproc.THRESH_TOZERO);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_05_Threshold1.jpg", currMatRegions);
		Imgproc.threshold(currMatRegions, currMatRegions, threshold + range, 255, Imgproc.THRESH_TOZERO_INV);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_05_Threshold2.jpg", currMatRegions);

		// Creating the mask
		Imgproc.threshold(currMatRegions, currMatRegions, 1, 1, Imgproc.THRESH_BINARY);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_05_Threshold3.jpg", currMatRegions);

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

		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_06_0_Graph.jpg", B);

		Core.compare(mask, matOnes, mask, Core.CMP_EQ);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_06_1_Mask.jpg", mask);

		Mat fg = new Mat(image.size(), CvType.CV_8UC1, new Scalar(0, 0, 0));
		image.copyTo(fg, mask);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_06_3_GrabCut_BorderBased.jpg", mask);

		// IV. SEGMENTATION BY GRABCUT (MASK-BASED)

		image = inMat.clone();
		mask = borderBasedMask.clone();
		bgModel = new Mat();
		fgModel = new Mat();

		B = new Mat();
		alpha = new Scalar(80);

		Core.min(mask.clone(), threshMat, mask);

		Core.multiply(mask.clone(), alpha, B);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_07_0_InitGraph.jpg", B);
		Imgproc.grabCut(image, mask, rect, bgModel, fgModel, GRABCUT_MASKBASED_ITERATIONS, Imgproc.GC_INIT_WITH_MASK);

		Core.multiply(mask, alpha, B);

		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_07_1_Graph.jpg", B);
		Core.compare(mask, matOnes, mask, Core.CMP_EQ);

		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_07_2_Mask.jpg", mask);
		fg = new Mat(image.size(), CvType.CV_8UC1, new Scalar(0, 0, 0));
		inMat.copyTo(fg, mask);

		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_07_3_GrabCut_MaskBased.jpg", mask);

		// V. RESULT

		resMat = fg;
		return resMat;
	}

	private static Mat process2(Mat inMat, File currImgFile) {
		Mat resMat = new Mat();
		Mat currMat = new Mat();

		currMat = inMat.clone();

		// I. PRE-PROCESSING

		Mat tmpMat = new Mat();
		Imgproc.cvtColor(currMat, tmpMat, Imgproc.COLOR_BGR2GRAY);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_10_Gray.jpg", tmpMat);

		// Converting color models
		Imgproc.cvtColor(currMat, currMat, Imgproc.COLOR_BGR2HSV_FULL);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_01_HSVConvert.jpg", currMat);

		// Down-sampling via Image Pyramids

		// Bilateral Filtering
		// Imgcodecs.imwrite(OUTPUT_FILEPATH + "1_1B4BF.jpg", dst);
		Imgproc.bilateralFilter(currMat.clone(), currMat, 9, 75, 75);
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_02_BiFilter.jpg", currMat);

		// Mean Shift Filtering
		Imgproc.pyrMeanShiftFiltering(currMat, currMat, 15, 50, 5, new TermCriteria(TermCriteria.MAX_ITER | TermCriteria.EPS, 50, 0.001));
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_03_MSFilter.jpg", currMat);

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
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_04_HSVSplit_Hue.jpg", list.get(0));
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_04_HSVSplit_Saturation.jpg", list.get(1));
		Imgcodecs.imwrite(OUTPUT_DIR + currImgFile.getName() + "_04_HSVSplit_Val.jpg", list.get(2));

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

		return resMat;
	}
}
