package org.javaopencvbook;

// Imports
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.videoio.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;

public class App
{
		private class Stats {
			double average;
			double variance;
			double standardDeviation;
		}

		public Stats calculateStats(double[] centerArray) {
			Stats stats = new Stats();
			stats.average = Arrays.stream(centerArray).average().orElse(Double.NaN);
			//System.out.println("Average: " + average);

			stats.variance = Arrays.stream(centerArray)
					.map(x -> Math.pow(x - stats.average, 2))
					.average()
					.orElse(Double.NaN);
			stats.standardDeviation = Math.sqrt(stats.variance);
			//System.out.println("Standard deviation: " + standardDeviation);

			return stats;
		}

	public static final int CAMERA_SHIFT_FROM_CENTER = 320;

	static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }

	// Constants
	final static int CV_CAP_OPENNI = 0;
	final static int CV_CAP_OPENNI_DISPARITY_MAP             = 2; // Disparity in pixels (CV_8UC1)
	final static int CV_CAP_OPENNI_BGR_IMAGE                 = 5;

	static final double ANGLE_TILT_LENIENCY = 15.0;
	// angle on RotatedRect goes from 0 to 90 from top left to top right points
	// the modulo 180 on finding the exclusion range, i.e. thee tilt angle after which
	// we don't want to consider that rectangle as 'straight'
	static final double EXCLUSION_RANGE_MIN = (ANGLE_TILT_LENIENCY)%180;
	static final double EXCLUSION_RANGE_MAX = (-ANGLE_TILT_LENIENCY+180)%180;

	static final int CENTER_SAMPLING_SIZE = 20;
	double[] centerArray = new double[CENTER_SAMPLING_SIZE];
	int centerCurrentIndex = -1;

	// information about the pole
	public static double poleCenter = 0;

	// Camera center values calibration
	static final double POLE_ALIGNMENT_CENTER_X = 750;
	static final double POLE_WIDTH_AT_ALIGNMENT = 215;
	static final double POLE_ALIGNMENT_MIN_X = POLE_ALIGNMENT_CENTER_X- POLE_WIDTH_AT_ALIGNMENT *2.5/2; // Cone based width is ˜3 times the width of the pole
	static final double POLE_ALIGNMENT_MAX_X = POLE_ALIGNMENT_CENTER_X+ POLE_WIDTH_AT_ALIGNMENT *2.5/2; // the range to be centered is between, taking 2x ratio

	// Color Specific filtering values
	static final double MIN_RECT_ASPECT_RATIO_Y = 2.0;
	static final double minYellowInitValues[]= {17, 125, 70};
	static final double maxYellowInitValues[] = {44, 255, 255};

	static final double MIN_RECT_ASPECT_RATIO_B = 1.1;
	static final double minBlueInitValues[]= {105, 125, 50};
	static final double maxBlueInitValues[] = {135, 255, 255};

	static final double MIN_RECT_ASPECT_RATIO_R = 1.1;
	static final double minRedInitValues1[]= {165, 125, 110};
	static final double maxRedInitValues1[] = {255, 255, 255};
	static final double minRedInitValues2[]= {0, 125, 110};
	static final double maxRedInitValues2[] = {15, 255, 255};

	// Debug colors for contour boxes
	static final Scalar contourBlue = new Scalar(255, 0, 0);
	static final Scalar contourGreen = new Scalar(0, 255, 0);
	static final Scalar contourRed = new Scalar(0, 0, 255);
	static final Scalar contourViolet = new Scalar(255, 0, 255);
	static final Scalar contourGray = new Scalar(92, 92, 92);

	// Matrices for OpenCv
	Mat colorImage = new Mat();
	Mat blurImage = new Mat();
	Mat hsvImage = new Mat();
	Mat maskHSVYellow = new Mat();
	Mat maskHSVBlue = new Mat();
	Mat maskHSVRed = new Mat();
	Mat maskHSVRed1 = new Mat();
	Mat maskHSVRed2 = new Mat();
	Mat maskedImage = new Mat();
	Mat maskedImage8B = new Mat();
	Mat maskedAndContoursImage = new Mat();
	Mat workingBackground = new Mat();

	GUI gui;

	// App entry point
	public static void main(String[] args) throws Exception {
		System.out.println(Core.getBuildInformation());

		App app = new App();
		app.run();
	}

	// Computer Vision
	public void run() {

		VideoCapture capture = new VideoCapture(CV_CAP_OPENNI);

		if(!capture.grab()){
			System.out.println("Couldn't initialize. Check if Kinect Sensor is connected");
			return;
		}
		capture.retrieve(colorImage,  CV_CAP_OPENNI_DISPARITY_MAP);

		gui = new GUI("OpenCV Kinect Depth Chroma Key", colorImage, minYellowInitValues, maxYellowInitValues);
		gui.init();

		while(true){

			// Rotated array of center
			centerCurrentIndex = (centerCurrentIndex+1)%CENTER_SAMPLING_SIZE;

			// Camera working
			if(!capture.grab()){
				System.out.println("Couldn't grab video camea. Check if Kinect Sensor is connected");
				continue;
			}
			capture.retrieve(colorImage, CV_CAP_OPENNI_BGR_IMAGE);
			if(colorImage.empty()){
				System.out.println("Couldn't retrieve frames. Check if Kinect Sensor is connected");
				continue;
			}



			// Initial blur to filter out some unwanted points
			//Imgproc.medianBlur(colorImage, blurImage, 9);
			Imgproc.GaussianBlur(colorImage, blurImage, new Size(9.0, 9.0), 75, 75);

			// Convert image in HSV to filter easily on hue for yellow
			Imgproc.cvtColor(blurImage, hsvImage, Imgproc.COLOR_BGR2HSV);

			// HSV mask to filter out from interface

/*
			Scalar lower_color_bounds = new Scalar(gui.minLevels[0].value, gui.minLevels[1].value, gui.minLevels[2].value);
			Scalar upper_color_bounds = new Scalar(gui.maxLevels[0].value, gui.maxLevels[1].value, gui.maxLevels[2].value);
			Core.inRange(hsvImage,lower_color_bounds,upper_color_bounds, maskHSVYellow);
*/

			Core.inRange(hsvImage,new Scalar(minYellowInitValues),new Scalar(maxYellowInitValues), maskHSVYellow);
			Core.inRange(hsvImage,new Scalar(minBlueInitValues),new Scalar(maxBlueInitValues), maskHSVBlue);
			Core.inRange(hsvImage,new Scalar(minRedInitValues1),new Scalar(maxRedInitValues1), maskHSVRed1);
			Core.inRange(hsvImage,new Scalar(minRedInitValues2),new Scalar(maxRedInitValues2), maskHSVRed2);
			Core.bitwise_or(maskHSVRed1, maskHSVRed2, maskHSVRed);

			// Blacken pixels not in yellow band
			maskedImage.setTo(new Scalar(0,0,0));
			colorImage.copyTo(maskedImage, maskHSVYellow);

			// Take copy for combined display
			maskedImage.copyTo(workingBackground);

			// Convert to gray image to draw contours
			Imgproc.cvtColor(maskedImage, maskedImage8B, Imgproc.COLOR_BGR2GRAY);

			colorImage.copyTo(maskedImage, maskHSVBlue);
			colorImage.copyTo(maskedImage, maskHSVRed);
			// Take copy for combined display
			maskedImage.copyTo(maskedAndContoursImage);

			// Finding contours
			List<MatOfPoint> contours = new ArrayList<>();
			Mat hierarchy = new Mat();
			Imgproc.findContours(maskedImage8B, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
			MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
			RotatedRect[] rectangle = new RotatedRect[contours.size()];
			List<MatOfPoint> contourList = new ArrayList<>();

			// Going through all the contours to find the rectangles
			for (int i = 0; i < contours.size(); i++) {

				//Enclosed contours as children of overlapping contours are useless
				if(hierarchy.get(0, i)[3] !=-1){
					rectangle[i] = null;
					continue;
				}
				// Getting only the rectangles from the contours
				contoursPoly[i] = new MatOfPoint2f();
				Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
				rectangle[i] = Imgproc.minAreaRect(contoursPoly[i]);
				if (rectangle[i].size.height == 0) {
					rectangle[i] = null;
					continue;
				}
				Scalar contourColor = contourGreen;

				// Be sure that largest side is the height, e.g. filtering out squares
				double aspectRatio = Math.max(rectangle[i].size.width, rectangle[i].size.height) / Math.min(rectangle[i].size.width, rectangle[i].size.height);
				if (aspectRatio > (1/ MIN_RECT_ASPECT_RATIO_Y) && aspectRatio < MIN_RECT_ASPECT_RATIO_Y) {
					contourColor = contourRed;
				}

				// Making a box contour around the rectangles
				Point[] vertices = new Point[4];
				rectangle[i].points(vertices);
				List<MatOfPoint> boxExcluded = new ArrayList<>();
				boxExcluded.add(new MatOfPoint(vertices));


				// Filtering rectangles with too small of an area
				if(rectangle[i].size.area() <= 500) {
					contourColor = contourBlue;
				}


				if (rectangle[i].size.width > rectangle[i].size.height)
				{
					double tmp = rectangle[i].size.width;
					rectangle[i].size.width = rectangle[i].size.height;
					rectangle[i].size.height = tmp;
					rectangle[i].angle += 90.f;
				}

				// Filtering rectangle with too much tilt
				if(EXCLUSION_RANGE_MIN<=EXCLUSION_RANGE_MAX) {
					if (rectangle[i].angle > EXCLUSION_RANGE_MIN && rectangle[i].angle < EXCLUSION_RANGE_MAX) {
						contourColor = contourGray;
					}
				}
				else{
					if (rectangle[i].angle > EXCLUSION_RANGE_MIN || rectangle[i].angle < EXCLUSION_RANGE_MAX) {
						contourColor = contourGray;
					}
				}

				// Drawing contours (debugging purpose)
				Imgproc.drawContours(maskedAndContoursImage, boxExcluded, -1, contourColor,3,0);

				// Removing the rectangles we don't want from the rectangle list
				if(contourColor!=contourGreen){
					rectangle[i] = null;
					continue;
				}

			}

			// Going through the all the rectangles to find the biggest
			double maxWidth = 0;
			int maxWidthIndex = -1;
			for (int i = 0; i < rectangle.length; i++) {
				if(rectangle[i] == null) {
					continue;
				}
				if(rectangle[i].size.width>maxWidth) {
					maxWidthIndex = i;
					maxWidth = rectangle[i].size.width;
				}
			}

			Stats stats = calculateStats(centerArray);

			if(maxWidthIndex>=0){
				centerArray[centerCurrentIndex] = rectangle[maxWidthIndex].center.x;
				// Drawing a line
				Imgproc.line (
						maskedAndContoursImage,                    //Matrix obj of the image
						new Point(centerArray[centerCurrentIndex], 0),        //p1
						new Point(centerArray[centerCurrentIndex], maskedAndContoursImage.height()),       //p2
						new Scalar(50, 50, 240),     //Scalar object for color
						2                        //Thickness of the line
				);
			}

			//debugDisplayCenterArray();


			if((Math.abs(centerArray[centerCurrentIndex]-poleCenter) <= 2*stats.standardDeviation) ||
					(centerArray[centerCurrentIndex] >= stats.average-stats.standardDeviation
							&& centerArray[centerCurrentIndex] <= stats.average+stats.standardDeviation) )	{
				poleCenter = centerArray[centerCurrentIndex];
			}

			debugDisplayStats(stats);
			debugDisplaySelectedCenter((maxWidthIndex>=0)? contourViolet :contourGray);

			// Drawing the biggest rectangle
			if(maxWidthIndex>=0) {
				Point[] vertices = new Point[4];
				rectangle[maxWidthIndex].points(vertices);
				List<MatOfPoint> boxContours = new ArrayList<>();
				boxContours.add(new MatOfPoint(vertices));

				gui.label.setText(String.valueOf(POLE_ALIGNMENT_MIN_X)+" "+String.valueOf(POLE_ALIGNMENT_MAX_X));

				Imgproc.drawContours(maskedAndContoursImage, boxContours, -1, contourViolet, 3, 0);
			}
			renderOutputAccordingToMode(gui);
		}
	}

	private void debugDisplayStats(Stats stats) {
		Imgproc.line (
				maskedAndContoursImage,                    //Matrix obj of the image
				new Point(stats.average, 0),        //p1
				new Point(stats.average, maskedAndContoursImage.height()),       //p2
				new Scalar(128, 0, 0),     //Scalar object for color
				2                        //Thickness of the line
		);
		Imgproc.line (
				maskedAndContoursImage,                    //Matrix obj of the image
				new Point(stats.average- stats.standardDeviation, 0),        //p1
				new Point(stats.average- stats.standardDeviation, maskedAndContoursImage.height()),       //p2
				new Scalar(192, 0, 0),     //Scalar object for color
				1                        //Thickness of the line
		);
		Imgproc.line (
				maskedAndContoursImage,                    //Matrix obj of the image
				new Point(stats.average+ stats.standardDeviation, 0),        //p1
				new Point(stats.average+ stats.standardDeviation, maskedAndContoursImage.height()),       //p2
				new Scalar(192, 0, 0),     //Scalar object for color
				1                        //Thickness of the line
		);
	}

	private void debugDisplaySelectedCenter(Scalar color) {
		Imgproc.line (
				maskedAndContoursImage,                    //Matrix obj of the image
				new Point(poleCenter, 0),        //p1
				new Point(poleCenter, maskedAndContoursImage.height()),       //p2
				color,     //Scalar object for color
				2                           //Thickness of the line
		);
	}

	private void debugDisplayCenterArray() {
		for(int i=0; i<centerArray.length; i++){
			Imgproc.line(
					maskedAndContoursImage,                    //Matrix obj of the image
					new Point(centerArray[i], 0),        //p1
					new Point(centerArray[i], maskedAndContoursImage.height()),       //p2
					new Scalar(50, i*15, 240),     //Scalar object for color
					1                        //Thickness of the line
			);
		}
	}


	private void renderOutputAccordingToMode(GUI gui) {
		if(GUI.RGB_MASK_STRING.equals(gui.getOutputMode())){
			gui.updateView(maskedImage);
		}
		else if( GUI.BLUR_IMAGE.equals(gui.getOutputMode())){
			gui.updateView(blurImage);
		}
		else if(GUI.DENOISE_IMAGE.equals(gui.getOutputMode())){
			gui.updateView(maskedImage8B);
		}
		else if(GUI.ORIGINAL_IMAGE.equals(gui.getOutputMode())){
			gui.updateView(colorImage);
		}
		else if (GUI.MASKED_AND_CONTOURS_IMAGE.equals(gui.getOutputMode())){
			gui.updateView(maskedAndContoursImage);
		}
		else{ // COMBINED_STRING
			gui.updateView(workingBackground);
		}
	}
}

////  SCRATCH ZONE   ///////
// Code and functions calls attempts we park here as we might come back at them

//Imgproc.cvtColor(maskedImage, grayImage, Imgproc.COLOR_BGR2GRAY);
//Imgproc.threshold(grayImage, dnMask, 70, 255, Imgproc.THRESH_BINARY_INV);
//Photo.inpaint(yellowColorImage, dnMask, dnImage, 20, Photo.INPAINT_TELEA);


//Photo.fastNlMeansDenoisingColored(workingBackground, dnImage, 3, 3, 7, 21);
//maskedImage.convertTo(maskedImage8B, CvType.CV_8UC1, 1.0f);
//			blurImage.convertTo(brightImage, CvType.CV_8UC1, 1.7f ); // increase brightness



