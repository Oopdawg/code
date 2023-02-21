package org.javaopencvbook;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Size;
import org.opencv.core.RotatedRect;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.*;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;


public class App 
{
	static{ System.loadLibrary(Core.NATIVE_LIBRARY_NAME); }
	
	final static int CV_CAP_OPENNI = 0;
    final static int CV_CAP_OPENNI_DEPTH_MAP                 = 0; // Depth values in mm (CV_16UC1)
    final static int CV_CAP_OPENNI_POINT_CLOUD_MAP           = 1; // XYZ in meters (CV_32FC3)
    final static int CV_CAP_OPENNI_DISPARITY_MAP             = 2; // Disparity in pixels (CV_8UC1)
    final static int CV_CAP_OPENNI_DISPARITY_MAP_32F         = 3; // Disparity in pixels (CV_32FC1)
    final static int CV_CAP_OPENNI_VALID_DEPTH_MASK          = 4; // CV_8UC1
    final static int CV_CAP_OPENNI_BGR_IMAGE                 = 5;
    final static int CV_CAP_OPENNI_GRAY_IMAGE                = 6;

	static final double MIN_RECT_ASPECT_RATIO = 4.0;
	static final double CAMERA_MOUNT_ANGLE = 0.0;
	static final double ANGLE_TILT_LENIENCY = 10.0;
	//angle on RotatedRect goes from 0 to 90 from top left to top right points
	//the modulo 180 on finding the exclusion range, i.e. thee tilt angle after which
	//we don't want to consider that rectangle as 'straight'
	static final double EXCLUSION_RANGE_MIN = (CAMERA_MOUNT_ANGLE+ ANGLE_TILT_LENIENCY)%180;
	static final double EXCLUSION_RANGE_MAX = (CAMERA_MOUNT_ANGLE-ANGLE_TILT_LENIENCY+180)%180;


	Mat colorImage = new Mat();
	Mat blurImage = new Mat();
	Mat hsvImage = new Mat();
	Mat maskHSV = new Mat();
	Mat maskedImage = new Mat();
	Mat maskedImage8B = new Mat();
	Mat maskedAndContoursImage = new Mat();
	Mat background = Imgcodecs.imread("/Users/denis/Documents/GitHub/code/chapter6/kinect/src/main/resources/images/background.jpg");
	Mat resizedBackground = new Mat();
	Mat workingBackground = new Mat();




	public static void main(String[] args) throws Exception {
		System.out.println(Core.getBuildInformation());

		App app = new App();
		app.run();
	}

	public void run() {


		VideoCapture capture = new VideoCapture(CV_CAP_OPENNI);
		capture.grab();
		capture.retrieve(colorImage,  CV_CAP_OPENNI_DISPARITY_MAP);
		//colorImage.convertTo(yellowColorImage,CvType.CV_8UC1, 50.00f );

		Scalar contourBlue = new Scalar(255, 0, 0);
		Scalar contourGreen = new Scalar(0, 255, 0);
		Scalar contourRed = new Scalar(0, 0, 255);
		Scalar contourViolet = new Scalar(255, 0, 255);
		Scalar contourGray = new Scalar(92, 92, 92);


		if(colorImage.rows()>0){
			Imgproc.resize(background, resizedBackground, colorImage.size());

			GUI gui = new GUI("OpenCV Kinect Depth Chroma Key", colorImage);
			gui.init();
			while(true){
				capture.grab();

				capture.retrieve(colorImage, CV_CAP_OPENNI_BGR_IMAGE);
				//colorImage.convertTo(brightImage,CvType.CV_8UC1, 1.8f ); // increase brightness

				Imgproc.GaussianBlur(colorImage, blurImage, new Size(9.0, 9.0), 75, 75);

				// Convert image in HSV to filter easily on hue for yellow
				Imgproc.cvtColor(blurImage, hsvImage, Imgproc.COLOR_BGR2HSV);

				Scalar lower_color_bounds = new Scalar(gui.minLevels[0].value, gui.minLevels[1].value, gui.minLevels[2].value);
				Scalar upper_color_bounds = new Scalar(gui.maxLevels[0].value, gui.maxLevels[1].value, gui.maxLevels[2].value);
				Core.inRange(hsvImage,lower_color_bounds,upper_color_bounds, maskHSV);

				// Blacken pixels not in yellow band
				maskedImage.setTo(new Scalar(0,0,0));
				colorImage.copyTo(maskedImage, maskHSV);

				// Take copy for combined display
				maskedImage.copyTo(workingBackground);

				//Imgproc.cvtColor(maskedImage, grayImage, Imgproc.COLOR_BGR2GRAY);
				//Imgproc.threshold(grayImage, dnMask, 70, 255, Imgproc.THRESH_BINARY_INV);
				//Photo.inpaint(yellowColorImage, dnMask, dnImage, 20, Photo.INPAINT_TELEA);


				//Photo.fastNlMeansDenoisingColored(workingBackground, dnImage, 3, 3, 7, 21);

				Imgproc.cvtColor(maskedImage, maskedImage8B, Imgproc.COLOR_BGR2GRAY);
				//maskedImage.convertTo(maskedImage8B, CvType.CV_8UC1, 1.0f);

				// Take copy for combined display
				maskedImage.copyTo(maskedAndContoursImage);

				List<MatOfPoint> contours = new ArrayList<>();
				Mat hierarchy = new Mat();
				Imgproc.findContours(maskedImage8B, contours, hierarchy, Imgproc.RETR_TREE, Imgproc.CHAIN_APPROX_SIMPLE);
				MatOfPoint2f[] contoursPoly = new MatOfPoint2f[contours.size()];
				RotatedRect[] rectangle = new RotatedRect[contours.size()];
				List<MatOfPoint> contourList = new ArrayList<>();
		    	for (int i = 0; i < contours.size(); i++) {

					// Enclosed contours as children of overlapping contours are useless
					if(hierarchy.get(0, i)[3] !=-1){
						continue;
					}
					contoursPoly[i] = new MatOfPoint2f();
					Imgproc.approxPolyDP(new MatOfPoint2f(contours.get(i).toArray()), contoursPoly[i], 3, true);
					rectangle[i] = Imgproc.minAreaRect(contoursPoly[i]);
					if (rectangle[i].size.height == 0) {
						continue;
					}
					// Be sure that largest side is the height

					double aspectRatio = Math.max(rectangle[i].size.width, rectangle[i].size.height) / Math.min(rectangle[i].size.width, rectangle[i].size.height);

					Scalar contourColor = contourGreen;
					if (aspectRatio > (1/ MIN_RECT_ASPECT_RATIO) && aspectRatio < MIN_RECT_ASPECT_RATIO) {
						contourColor = contourRed;
					}


					Point[] vertices = new Point[4];
					rectangle[i].points(vertices);
					List<MatOfPoint> boxContours = new ArrayList<>();
					boxContours.add(new MatOfPoint(vertices));

					if(rectangle[i].size.area() <= 300) {
						contourColor = contourBlue;
					}

					System.out.print(EXCLUSION_RANGE_MIN);
					System.out.print(" ");
					System.out.print(EXCLUSION_RANGE_MAX);
					System.out.print(" ");
					System.out.println( rectangle[i].angle);


					if (rectangle[i].size.width > rectangle[i].size.height)
					{
						double tmp = rectangle[i].size.width;
						rectangle[i].size.width = rectangle[i].size.height;
						rectangle[i].size.height = tmp;
						rectangle[i].angle += 90.f;
					}

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


					Imgproc.drawContours(maskedAndContoursImage, boxContours, -1, contourColor,3,0);
					if(contourColor!=contourGreen){
						rectangle[i] = null;
					}

				}

				int biggestRect = -1;
				double biggestArea = 0;
				for (int i = 0; i < rectangle.length; i++) {
					if(rectangle[i] == null) {
						continue;
					}
					if(rectangle[i].size.area()>biggestArea) {
						biggestRect = i;
						biggestArea = rectangle[i].size.area();
					}
				}

				if(biggestRect>=0) {
					Point[] vertices = new Point[4];
					rectangle[biggestRect].points(vertices);
					List<MatOfPoint> boxContours = new ArrayList<>();
					boxContours.add(new MatOfPoint(vertices));

					Imgproc.drawContours(maskedAndContoursImage, boxContours, -1, contourViolet, 3, 0);
				}
				renderOutputAccordingToMode(gui);
			}
		}
		else{
			System.out.println("Couldn't retrieve frames. Check if Kinect Sensor is connected and if opencv was compiled with OpenNI support");
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

