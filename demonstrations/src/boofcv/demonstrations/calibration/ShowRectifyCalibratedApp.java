/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.demonstrations.calibration;

import boofcv.alg.distort.DistortImageOps;
import boofcv.alg.distort.ImageDistort;
import boofcv.alg.geo.PerspectiveOps;
import boofcv.alg.geo.RectifyImageOps;
import boofcv.alg.geo.rectify.RectifyCalibrated;
import boofcv.alg.misc.GImageMiscOps;
import boofcv.core.image.border.BorderType;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.SelectAlgorithmAndInputPanel;
import boofcv.gui.image.ShowImages;
import boofcv.gui.stereo.RectifiedPairPanel;
import boofcv.io.PathLabel;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.calib.StereoParameters;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.ImageType;
import boofcv.struct.image.Planar;
import georegression.struct.se.Se3_F64;
import org.ejml.data.DenseMatrix64F;

import javax.swing.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;

/**
 * Given a set of stereo images and their intrinsic parameters, display the rectified images and a horizontal line
 * where the user clicks.  The different rectified images show how the view can be optimized for different purposes.
 *
 * @author Peter Abeles
 */
public class ShowRectifyCalibratedApp extends SelectAlgorithmAndInputPanel {

	ListDisplayPanel gui = new ListDisplayPanel();

	StereoParameters param;

	// distorted input images
	Planar<GrayF32> distLeft;
	Planar<GrayF32> distRight;

	// storage for undistorted and rectified images
	Planar<GrayF32> rectLeft;
	Planar<GrayF32> rectRight;

	boolean hasProcessed = false;

	public ShowRectifyCalibratedApp() {
		super(0);

		setMainGUI(gui);
	}

	public void configure( final BufferedImage origLeft , final BufferedImage origRight , StereoParameters param )
	{
		this.param = param;

		// distorted images
		distLeft = ConvertBufferedImage.convertFromMulti(origLeft, null, true, GrayF32.class);
		distRight = ConvertBufferedImage.convertFromMulti(origRight, null, true, GrayF32.class);

		// storage for undistorted + rectified images
		rectLeft = new Planar<GrayF32>(GrayF32.class,
				distLeft.getWidth(),distLeft.getHeight(),distLeft.getNumBands());
		rectRight = new Planar<GrayF32>(GrayF32.class,
				distRight.getWidth(),distRight.getHeight(),distRight.getNumBands());

		// Compute rectification
		RectifyCalibrated rectifyAlg = RectifyImageOps.createCalibrated();
		Se3_F64 leftToRight = param.getRightToLeft().invert(null);

		// original camera calibration matrices
		DenseMatrix64F K1 = PerspectiveOps.calibrationMatrix(param.getLeft(), null);
		DenseMatrix64F K2 = PerspectiveOps.calibrationMatrix(param.getRight(), null);

		rectifyAlg.process(K1,new Se3_F64(),K2,leftToRight);

		// rectification matrix for each image
		DenseMatrix64F rect1 = rectifyAlg.getRect1();
		DenseMatrix64F rect2 = rectifyAlg.getRect2();
		DenseMatrix64F rectK = rectifyAlg.getCalibrationMatrix();

		// show results and draw a horizontal line where the user clicks to see rectification easier
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.reset();
				gui.addItem(new RectifiedPairPanel(true, origLeft, origRight), "Original");
			}
		});

		// add different types of adjustments
		addRectified("No Adjustment", rect1,rect2);
		RectifyImageOps.allInsideLeft(param.left, rect1, rect2, rectK);
		addRectified("All Inside", rect1,rect2);
		RectifyImageOps.fullViewLeft(param.left, rect1, rect2, rectK);
		addRectified("Full View", rect1,rect2);

		hasProcessed = true;
	}

	private void addRectified( final String name , final DenseMatrix64F rect1 , final DenseMatrix64F rect2 ) {
		// Will rectify the image
		ImageType<GrayF32> imageType = ImageType.single(GrayF32.class);
		ImageDistort<GrayF32,GrayF32> imageDistortLeft =
				RectifyImageOps.rectifyImage(param.getLeft(), rect1, BorderType.ZERO, imageType);
		ImageDistort<GrayF32,GrayF32> imageDistortRight =
				RectifyImageOps.rectifyImage(param.getRight(), rect2, BorderType.ZERO, imageType);

		// Fill the image with all black
		GImageMiscOps.fill(rectLeft, 0);
		GImageMiscOps.fill(rectRight,0);

		// Render the rectified image
		DistortImageOps.distortPL(distLeft, rectLeft, imageDistortLeft);
		DistortImageOps.distortPL(distRight, rectRight, imageDistortRight);

		// convert for output
		final BufferedImage outLeft = ConvertBufferedImage.convertTo(rectLeft, null,true);
		final BufferedImage outRight = ConvertBufferedImage.convertTo(rectRight, null, true);

		// Add this rectified image
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				gui.addItem(new RectifiedPairPanel(true, outLeft, outRight), name);
			}});
	}

	@Override
	public void refreshAll(Object[] cookies) {}

	@Override
	public void setActiveAlgorithm(int indexFamily, String name, Object cookie) {}

	@Override
	public void changeInput(String name, int index) {
		PathLabel refs = inputRefs.get(index);

		StereoParameters param = UtilIO.loadXML(media.openFile(refs.getPath(0)));
		BufferedImage origLeft = media.openImage(refs.getPath(1));
		BufferedImage origRight = media.openImage(refs.getPath(2));

		configure(origLeft,origRight,param);
	}

	@Override
	public void loadConfigurationFile(String fileName) {}

	@Override
	public boolean getHasProcessedImage() {
		return hasProcessed;
	}

	public static void main( String args[] ) {
		ShowRectifyCalibratedApp app = new ShowRectifyCalibratedApp();

		// camera config, image left, image right
		String dir = UtilIO.pathExample("calibration/stereo/Bumblebee2_Chess/");

		java.util.List<PathLabel> inputs = new ArrayList<PathLabel>();
		inputs.add(new PathLabel("BumbleBee",dir + "stereo.xml",dir + "left05.jpg",dir + "right05.jpg"));

		app.setInputList(inputs);

		// wait for it to process one image so that the size isn't all screwed up
		while( !app.getHasProcessedImage() ) {
			Thread.yield();
		}
		ShowImages.showWindow(app, "Calibrated Camera Rectification", true);

		System.out.println("Done");
	}
}
