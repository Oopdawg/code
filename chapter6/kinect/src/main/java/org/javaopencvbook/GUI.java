package org.javaopencvbook;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;

import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.javaopencvbook.utils.ImageProcessor;
import org.opencv.core.Mat;

public class GUI {
	public static final String BRIGHT_IMAGE = "Bright Image";
	public static final String ORIGINAL_IMAGE = "Original Image";
	public static final String BACKGROUND_STRING = "Background";
	public static final String DENOISE_IMAGE = "Denoise Image";
	public static final String COMBINED_STRING = "Combined";
	public static final String RGB_MASK_STRING = "Masked RGB";
	private JLabel imageView;
	private String windowName;
	private String outputMode = COMBINED_STRING;
	private Mat image, originalImage;

	public class Level {
		public int value;
		Level(){
			value = 0;
		}

		Level(int initialValue) {
			value = initialValue;
		}
	}
	// Levels in BGR order
	Level[] minLevels = new Level[3];
	Level[] maxLevels = new Level[3];

	//int minInitValues[] = {18, 130, 50};
	//int maxInitValues[] = {32, 255, 255};

	int minInitValues[] = {15, 155, 100};
	int maxInitValues[] = {38, 255, 255};

	private final ImageProcessor imageProcessor = new ImageProcessor();
	

	public GUI(String windowName, Mat newImage) {
		super();
		this.windowName = windowName;
		this.image = newImage;
		this.originalImage = newImage.clone();

		for(int i=0;i<=2;i++) {
			minLevels[i] = new Level(minInitValues[i]);
			maxLevels[i] = new Level(maxInitValues[i]);
		}
	}

	public void init() {
		setSystemLookAndFeel();
		initGUI();
	}
	
	public String getOutputMode() {
		return outputMode;
	}


	private void initGUI() {
		JFrame frame = createJFrame(windowName);

		updateView(image);

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

	}


	private void setupSlider(JFrame frame, String colorName, Level level) {
		JLabel sliderLabel = new JLabel(colorName+ " filter", JLabel.CENTER);
		sliderLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

		int minimum = 0;
		int maximum = 255;

		JSlider levelSlider = new JSlider(JSlider.HORIZONTAL,
				minimum, maximum, level.value);

		levelSlider.setMajorTickSpacing(15);
		levelSlider.setMinorTickSpacing(1);
		levelSlider.setPaintTicks(true);
		levelSlider.setPaintLabels(true);
		levelSlider.addChangeListener(new ChangeListener() {

			public void stateChanged(ChangeEvent e) {
				JSlider source = (JSlider)e.getSource();
				level.value = (int)source.getValue();
				//Mat output = imageProcessor.blur(image, level);
				//updateView(output);
			}
		});

		frame.add(sliderLabel);
		frame.add(levelSlider);
	}


	private JFrame createJFrame(String windowName) {
		JFrame frame = new JFrame(windowName);

		BoxLayout bl = new BoxLayout(frame.getContentPane(), BoxLayout.PAGE_AXIS);
		frame.setLayout(bl);

		// BGR order
		setupSlider(frame, "Min Blue", minLevels[0]);
		setupSlider(frame, "Max Blue", maxLevels[0]);
		setupSlider(frame, "Min Green", minLevels[1]);
		setupSlider(frame, "Max Green", maxLevels[1]);
		setupSlider(frame, "Min Red", minLevels[2]);
		setupSlider(frame, "Max Red", maxLevels[2]);

		setupRadio(frame);
		setupImage(frame);

		frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
		return frame;
	}

	private void setupRadio(JFrame frame) {
		JRadioButton disparityMapButton = new JRadioButton(BRIGHT_IMAGE);
		disparityMapButton.setMnemonic(KeyEvent.VK_D);
		disparityMapButton.setActionCommand(BRIGHT_IMAGE);
		disparityMapButton.setSelected(false);
		
		JRadioButton disparityThresholdButton = new JRadioButton(DENOISE_IMAGE);
		disparityThresholdButton.setMnemonic(KeyEvent.VK_T);
		disparityThresholdButton.setActionCommand(DENOISE_IMAGE);
		disparityThresholdButton.setSelected(false);

		JRadioButton rgbButton = new JRadioButton(ORIGINAL_IMAGE);
		rgbButton.setMnemonic(KeyEvent.VK_R);
		rgbButton.setActionCommand(ORIGINAL_IMAGE);
		rgbButton.setSelected(false);
		
		
		JRadioButton backgroundButton = new JRadioButton(BACKGROUND_STRING);
		backgroundButton.setMnemonic(KeyEvent.VK_B);
		backgroundButton.setActionCommand(BACKGROUND_STRING);
		backgroundButton.setSelected(false);
		
		JRadioButton combinedButton = new JRadioButton(COMBINED_STRING);
		combinedButton.setMnemonic(KeyEvent.VK_S);
		combinedButton.setActionCommand(COMBINED_STRING);
		combinedButton.setSelected(true);
		
		
		JRadioButton rgbMaskButton = new JRadioButton(RGB_MASK_STRING);
		rgbMaskButton.setMnemonic(KeyEvent.VK_M);
		rgbMaskButton.setActionCommand(RGB_MASK_STRING);
		rgbMaskButton.setSelected(false);
		
		
		ButtonGroup group = new ButtonGroup();
		group.add(combinedButton);
		group.add(rgbMaskButton);
		group.add(disparityThresholdButton);
		group.add(disparityMapButton);
		group.add(rgbButton);
		group.add(backgroundButton);
		group.add(combinedButton);
		

		ActionListener operationChangeListener = new ActionListener() {

			public void actionPerformed(ActionEvent event) {
				outputMode  = event.getActionCommand();
			}
		};

		rgbMaskButton.addActionListener(operationChangeListener);
		rgbButton.addActionListener(operationChangeListener);
		combinedButton.addActionListener(operationChangeListener);
		disparityThresholdButton.addActionListener(operationChangeListener);
		backgroundButton.addActionListener(operationChangeListener);
		disparityMapButton.addActionListener(operationChangeListener);

		
		JPanel radioOperationPanel = new JPanel();

		JLabel outputLabel = new JLabel("Output:", JLabel.RIGHT);

		radioOperationPanel.add(outputLabel);
		radioOperationPanel.add(combinedButton);
		radioOperationPanel.add(rgbMaskButton);
		radioOperationPanel.add(disparityMapButton);
		radioOperationPanel.add(disparityThresholdButton);
		radioOperationPanel.add(rgbButton);
		radioOperationPanel.add(backgroundButton);
		
		

		frame.add(radioOperationPanel);
		

		
	}

	private void setupImage(JFrame frame) {
		imageView = new JLabel();
		imageView.setHorizontalAlignment(SwingConstants.CENTER);
		
		final JScrollPane imageScrollPane = new JScrollPane(imageView);
		imageScrollPane.setPreferredSize(new Dimension(680, 510));
		
		frame.add(imageScrollPane);
	}



	private void setSystemLookAndFeel() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
	}

	public void updateView(Mat newMat) {
		Image outputImage = imageProcessor.toBufferedImage(newMat);
		imageView.setIcon(new ImageIcon(outputImage));
	}

}
