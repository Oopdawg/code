package org.javaopencvbook;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.videoio.*;
import org.opencv.imgproc.Imgproc;

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

	Mat colorImage = new Mat();
	Mat brightImage = new Mat();
	Mat yellowColorImage = new Mat();
	Mat mask = new Mat();
	Mat maskedImage = new Mat();
	//Mat depthMap = new Mat();
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
		capture.retrieve( colorImage,  CV_CAP_OPENNI_DISPARITY_MAP);
		colorImage.convertTo(yellowColorImage,CvType.CV_8UC1, 50.00f );



		if(yellowColorImage.rows()>0){
			Imgproc.resize(background, resizedBackground, yellowColorImage.size());
			
			GUI gui = new GUI("OpenCV Kinect Depth Chroma Key", yellowColorImage);
			gui.init();
			while(true){
				capture.grab();

				capture.retrieve(colorImage, CV_CAP_OPENNI_BGR_IMAGE);
				colorImage.convertTo(brightImage,CvType.CV_8UC1, 1.7f ); // increase brightness
				Imgproc.cvtColor(brightImage, yellowColorImage, Imgproc.COLOR_BGR2GRAY);

				// in BGR order
				Scalar lower_color_bounds = new Scalar(gui.minLevels[0].value, gui.minLevels[1].value, gui.minLevels[2].value);
				Scalar upper_color_bounds = new Scalar(gui.maxLevels[0].value, gui.maxLevels[1].value, gui.maxLevels[2].value);

				Core.inRange(brightImage,lower_color_bounds,upper_color_bounds, mask );

				maskedImage.setTo(new Scalar(0,0,0));
				colorImage.copyTo(maskedImage, mask);

				workingBackground.setTo(new Scalar(0,0,0));
				maskedImage.copyTo(workingBackground,maskedImage);
				
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
		else if( GUI.BRIGHT_IMAGE.equals(gui.getOutputMode())){
			gui.updateView(brightImage);
		}
		else if(GUI.YELLOW_IMAGE.equals(gui.getOutputMode())){
			gui.updateView(yellowColorImage);
		}
		else if(GUI.ORIGINAL_IMAGE.equals(gui.getOutputMode())){
			gui.updateView(colorImage);
		}
		else if (GUI.BACKGROUND_STRING.equals(gui.getOutputMode())){
			gui.updateView(resizedBackground);
		}
		else{
			gui.updateView(workingBackground);
		}
	}
}

