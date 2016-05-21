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

package boofcv.examples.segmentation;

import boofcv.abst.feature.detect.line.DetectLineHoughPolar;
import boofcv.alg.filter.binary.BinaryImageOps;
import boofcv.alg.filter.binary.GThresholdImageOps;
import boofcv.factory.feature.detect.line.ConfigHoughPolar;
import boofcv.factory.feature.detect.line.FactoryDetectLineAlgs;
import boofcv.gui.ListDisplayPanel;
import boofcv.gui.binary.VisualizeBinaryData;
import boofcv.gui.feature.ImageLinePanel;
import boofcv.gui.image.ShowImages;
import boofcv.io.UtilIO;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.io.image.UtilImageIO;
import boofcv.struct.image.GrayF32;
import boofcv.struct.image.GrayS16;
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Demonstration of different techniques for automatic thresholding an image to create a binary image.  The binary
 * image can then be used for shape analysis and other applications.  Global methods apply the same threshold
 * to the entire image.  Local methods compute a local threshold around each pixel and can handle uneven
 * lighting, but produce noisy results in regions with uniform lighting.
 *
 * @see boofcv.examples.imageprocessing.ExampleBinaryOps
 *
 * @author Peter Abeles
 */
public class ExampleThresholding {
	static List<Integer> xOfPen = new ArrayList<>();			// Координаты пикселей ручки
	static List<Integer> yOfPen = new ArrayList<>();

	static int xCenter = 0;										// Координаты центрального пикселя ручки
	static int yCenter = 0;

	static int xMaxRemovalPixel = 0;							// Координаты самого удаленного от центра пикселя
	static int yMaxRemovalPixel = 0;

	static int xSum = 0;										// Суммы координат (для поиска центра)
	static int ySum = 0;

	static double maxDistance = 0;								// Расстояние до максимально удаленного пикселя

	static int xBaseCenter;										// Центр картинки
	static int yBaseCenter;

	static final float edgeThreshold = 25;
	static final int maxLines = 10;
	static ListDisplayPanel listPanel = new ListDisplayPanel();



	public static void threshold( String imageName )  {
		BufferedImage image = UtilImageIO.loadImage(imageName);
		// Display multiple images in the same window
		ListDisplayPanel gui = new ListDisplayPanel();

		// convert into a usable format
		GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);
		GrayU8 binary = new GrayU8(input.width,input.height);

//		GThresholdImageOps.threshold(input, binary, ImageStatistics.mean(input), true);
//		gui.addImage(VisualizeBinaryData.renderBinary(binary, false, null),"Global: Mean");

		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeOtsu(input, 0, 255), true);
		gui.addImage(VisualizeBinaryData.renderBinary(binary, false, null),"Global: Otsu");

//		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeEntropy(input, 0, 255), true);
//		gui.addImage(VisualizeBinaryData.renderBinary(binary, false, null),"Global: Entropy");

		gui.addImage(ConvertBufferedImage.convertTo(input,null),"Input Image");


		// Накладываем фильтр, удаляющий лишние белые пиксели
		GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
		filtered = BinaryImageOps.dilate8(filtered, 1, null);

		// Ищем центр
		findCenterOfTheObject(filtered);

		// Ищем самый удаленный от центра пиксель
		findTheMostRemovedPixelFromCenter();

		filtered.set(xCenter, yCenter, 0);
		filtered.set(xMaxRemovalPixel, yMaxRemovalPixel, 0);

		xBaseCenter = filtered.getWidth() / 2;
		yBaseCenter = filtered.getHeight() / 2;


		// Сдвиг изображения в центр картинки
		GrayU8 transferedImage = transferImage(filtered);

		BufferedImage convertedFromTransferedImage = ConvertBufferedImage.convertTo(transferedImage, null);



		UtilImageIO.saveImage(convertedFromTransferedImage, "temp.jpg");

//		GrayU8 newBinary = new GrayU8(newInput.width,newInput.height);




//		AffineTransform rotation = new AffineTransform();
//
//		double hipotenuse = Math.sqrt(Math.pow(xMaxRemovalPixel - xBaseCenter, 2) + Math.pow(yMaxRemovalPixel - yBaseCenter, 2));
//		double angleOfRotation = Math.atan2(xMaxRemovalPixel - xBaseCenter, yMaxRemovalPixel - yBaseCenter);
//		double radians = angleOfRotation * Math.PI / 180;
//		BufferedImage convertedFromFiltered = ConvertBufferedImage.convertTo(filtered, null);
//
//		rotation.rotate(radians, convertedFromFiltered.getWidth() / 2, convertedFromFiltered.getHeight() / 2);
//		AffineTransformOp rotationOperation = new AffineTransformOp(rotation, AffineTransformOp.TYPE_BILINEAR);
//		convertedFromFiltered = rotationOperation.filter(convertedFromFiltered, null);
//
//		// Конвертируем обратно
//		GrayF32 newInput = ConvertBufferedImage.convertFromSingle(convertedFromFiltered, null, GrayF32.class);
//		GrayU8 newBinary = new GrayU8(newInput.width,newInput.height);
//
//		// Отображение полученного изображения
//		GThresholdImageOps.threshold(newInput, newBinary, GThresholdImageOps.computeOtsu(newInput, 0, 255), true);
//		gui.addImage(VisualizeBinaryData.renderBinary(newBinary, false, null),"newFiletered");


//		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeOtsu(input, 0, 255), true);
//		gui.addImage(VisualizeBinaryData.renderBinary(detectedLines, false, null),"WithLines");



		BufferedImage convertedFromFiltered = ConvertBufferedImage.convertTo(filtered, null);
		detectLines(convertedFromFiltered, GrayU8.class, GrayS16.class);


//		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeOtsu(input, 0, 255), true);
//		gui.addImage(VisualizeBinaryData.renderBinary(filtered, false, null),"Filetered");


		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeOtsu(input, 0, 255), true);
		gui.addImage(VisualizeBinaryData.renderBinary(transferedImage, false, null),"Transfered");

		String fileName =  imageName.substring(imageName.lastIndexOf('/')+1);
		ShowImages.showWindow(gui,fileName);
	}


	public static<T extends ImageGray, D extends ImageGray>
	void detectLines( BufferedImage image ,
					  Class<T> imageType ,
					  Class<D> derivType ) {
		// convert the line into a single band image
		T input = ConvertBufferedImage.convertFromSingle(image, null, imageType);

		// Comment/uncomment to try a different type of line detector
		DetectLineHoughPolar<T, D> detector = FactoryDetectLineAlgs.houghPolar(
				new ConfigHoughPolar(3, 30, 2, Math.PI / 180, edgeThreshold, maxLines), imageType, derivType);
//		DetectLineHoughFoot<T,D> detector = FactoryDetectLineAlgs.houghFoot(
//				new ConfigHoughFoot(3, 8, 5, edgeThreshold,maxLines), imageType, derivType);
//		DetectLineHoughFootSubimage<T,D> detector = FactoryDetectLineAlgs.houghFootSub(
//				new ConfigHoughFootSubimage(3, 8, 5, edgeThreshold,maxLines, 2, 2), imageType, derivType);

		List<LineParametric2D_F32> found = detector.detect(input);

		// display the results
		ImageLinePanel imageLinePanel = new ImageLinePanel();
		imageLinePanel.setBackground(image);
		imageLinePanel.setLines(found);
		imageLinePanel.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));

		listPanel.addItem(imageLinePanel, "Found Lines");

	}





	public static GrayU8 transferImage(GrayU8 filtered) {
		int dx = xCenter - xBaseCenter;
		int dy = yCenter - yBaseCenter;

		int[][] startImage = new int[filtered.width][filtered.height];
		int[][] finishImage = new int[filtered.width][filtered.height];

		for (int i = 0; i < filtered.height; i++) {
			for (int j = 0; j < filtered.width; j++) {
				startImage[j][i] = filtered.get(j, i);
			}
		}

		for (int i = 0; i < startImage[0].length; i++) {
			for (int j = 0; j < startImage.length; j++) {
				if (startImage[j][i] == 1)
					finishImage[j - dx][i - dy] = 1;

			}
		}

		for (int i = 0; i < finishImage[0].length; i++) {
			for (int j = 0; j < finishImage.length; j++) {
				filtered.set(j, i, finishImage[j][i]);

			}
		}

		return filtered;
	}



	public static void findTheMostRemovedPixelFromCenter() {

		for (int i = 0; i < xOfPen.size(); i++) {
			double distance = Math.sqrt(Math.pow((xCenter - xOfPen.get(i)), 2) + Math.pow((yCenter - yOfPen.get(i)), 2));

			if (distance >= maxDistance)
				maxDistance = distance;
		}

		for (int i = 0; i < xOfPen.size(); i++) {
			double currentDistance = Math.sqrt(Math.pow((xCenter - xOfPen.get(i)), 2) + Math.pow((yCenter - yOfPen.get(i)), 2));

			if (maxDistance == currentDistance) {
				xMaxRemovalPixel = xOfPen.get(i);
				yMaxRemovalPixel = yOfPen.get(i);
			}
		}
	}


	public static void findCenterOfTheObject(GrayU8 filtered) {

		for (int i = 0; i < filtered.width; i++) {
			for (int j = 0; j < filtered.height; j++) {
				if (filtered.get(i, j) == 1) {
					xOfPen.add(i);
					yOfPen.add(j);
				}
			}
		}

		for (int i = 0; i < xOfPen.size(); i++) {
			xSum += xOfPen.get(i);
			ySum += yOfPen.get(i);
		}

		xCenter = xSum/xOfPen.size();
		yCenter = ySum/yOfPen.size();

	}

	public static void main(String[] args) {

		BufferedImage input = UtilImageIO.loadImage(UtilIO.pathExample("image_01.jpg"));

		detectLines(input, GrayU8.class, GrayS16.class);

		ShowImages.showWindow(listPanel, "Detected Lines", true);


		threshold(UtilIO.pathExample("image_03.jpg"));



	}
}
