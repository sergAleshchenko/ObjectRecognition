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

package boofcv.examples;

import boofcv.abst.segmentation.ImageSuperpixels;
import boofcv.alg.filter.blur.GBlurImageOps;
import boofcv.alg.filter.derivative.DerivativeType;
import boofcv.alg.filter.derivative.GImageDerivativeOps;
import boofcv.alg.segmentation.ComputeRegionMeanColor;
import boofcv.alg.segmentation.ImageSegmentationOps;
import boofcv.core.image.border.BorderType;
import boofcv.factory.segmentation.FactorySegmentationAlg;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.VisualizeRegions;
import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.webcamcapture.UtilWebcamCapture;
import boofcv.struct.feature.ColorQueue_F32;
import boofcv.struct.image.*;
import com.github.sarxos.webcam.Webcam;
import org.ddogleg.struct.FastQueue;
import org.ddogleg.struct.GrowQueue_I32;

import java.awt.*;
import java.awt.image.BufferedImage;

/**
 * Visualizes the image gradient in a video stream
 *
 * @author Peter Abeles
 */
public class ExampleWebcamGradient {

	public static <T extends ImageBase>
	void performSegmentation( ImageSuperpixels<T> alg , T color )
	{
		// Segmentation often works better after blurring the image.  Reduces high frequency image components which
		// can cause over segmentation
		GBlurImageOps.gaussian(color, color, 0.5, -1, null);

		// Storage for segmented image.  Each pixel will be assigned a label from 0 to N-1, where N is the number
		// of segments in the image
		GrayS32 pixelToSegment = new GrayS32(color.width,color.height);

		// Segmentation magic happens here
		alg.segment(color,pixelToSegment);

		// Displays the results
		visualize(pixelToSegment,color,alg.getTotalSuperpixels());
	}

	/**
	 * Visualizes results three ways.  1) Colorized segmented image where each region is given a random color.
	 * 2) Each pixel is assigned the mean color through out the region. 3) Black pixels represent the border
	 * between regions.
	 */
	public static <T extends ImageBase>
	void visualize(GrayS32 pixelToRegion , T color , int numSegments  )
	{
		// Computes the mean color inside each region
		ImageType<T> type = color.getImageType();
		ComputeRegionMeanColor<T> colorize = FactorySegmentationAlg.regionMeanColor(type);

		FastQueue<float[]> segmentColor = new ColorQueue_F32(type.getNumBands());
		segmentColor.resize(numSegments);

		GrowQueue_I32 regionMemberCount = new GrowQueue_I32();
		regionMemberCount.resize(numSegments);

		ImageSegmentationOps.countRegionPixels(pixelToRegion, numSegments, regionMemberCount.data);
		colorize.process(color,pixelToRegion,regionMemberCount,segmentColor);

		// Draw each region using their average color
		BufferedImage outColor = VisualizeRegions.regionsColor(pixelToRegion,segmentColor,null);
		// Draw each region by assigning it a random color
		BufferedImage outSegments = VisualizeRegions.regions(pixelToRegion, numSegments, null);

		// Make region edges appear red
		BufferedImage outBorder = new BufferedImage(color.width,color.height,BufferedImage.TYPE_INT_RGB);
		ConvertBufferedImage.convertTo(color, outBorder, true);
		VisualizeRegions.regionBorders(pixelToRegion,0xFF0000,outBorder);

		// Show the visualization results
		ListDisplayPanel gui = new ListDisplayPanel();
		gui.addImage(outColor,"Color of Segments");
		gui.addImage(outBorder, "Region Borders");
		gui.addImage(outSegments, "Regions");
		ShowImages.showWindow(gui,"Superpixels", true);
	}


	public static void main(String[] args) {

		// Open a webcam at a resolution close to 640x480
		Webcam webcam = UtilWebcamCapture.openDefault(640,480);

		// Create the panel used to display the image and
		ImagePanel gui = new ImagePanel();
		Dimension viewSize = webcam.getViewSize();
		gui.setPreferredSize(viewSize);

		// Predeclare storage for the gradient
		GrayF32 derivX = new GrayF32((int)viewSize.getWidth(),(int)viewSize.getHeight());
		GrayF32 derivY = new GrayF32((int)viewSize.getWidth(),(int)viewSize.getHeight());

		ShowImages.showWindow(gui,"Gradient",true);

		for(;;) {
			BufferedImage image = webcam.getImage();
			GrayF32 gray = ConvertBufferedImage.convertFrom(image,(GrayF32)null);

			// compute the gradient
			GImageDerivativeOps.gradient(DerivativeType.SOBEL, gray, derivX, derivY, BorderType.EXTENDED);

			// visualize and display
//			BufferedImage visualized = VisualizeImageData.colorizeGradient(derivX, derivY, -1);

			GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);
			GrayU8 binary = new GrayU8(input.width,input.height);


			BufferedImage visualized = VisualizeBinaryData.renderBinary(binary, true, image);

			gui.setBufferedImageSafe(visualized);
		}


	}
}
