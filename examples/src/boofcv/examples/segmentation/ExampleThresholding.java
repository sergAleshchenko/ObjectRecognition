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
import boofcv.struct.image.GrayU8;
import boofcv.struct.image.ImageGray;
import georegression.struct.line.LineParametric2D_F32;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;


public class ExampleThresholding {
	private List<Integer> xOfPen = new ArrayList<>();			// Координаты пикселей ручки
	private  List<Integer> yOfPen = new ArrayList<>();

	private  int xCenter = 0;										// Координаты центрального пикселя ручки
	private int yCenter = 0;

	private int xMaxRemovalPixel = 0;							// Координаты самого удаленного от центра пикселя
	private int yMaxRemovalPixel = 0;

	private int xSum = 0;										// Суммы координат (для поиска центра)
	private int ySum = 0;

	private double maxDistance = 0;								// Расстояние до максимально удаленного пикселя

	private int xBaseCenter;										// Центр картинки
	private int yBaseCenter;

	private final float edgeThreshold = 25;
	private final int maxLines = 10;
	private ListDisplayPanel listPanel = new ListDisplayPanel();



	public void threshold( String imageName )  {
		BufferedImage image = UtilImageIO.loadImage(imageName);
		ListDisplayPanel gui = new ListDisplayPanel();

		ResizeImage resizeImage = new ResizeImage();

		image = resizeImage.resizing(image);

		GrayF32 input = ConvertBufferedImage.convertFromSingle(image, null, GrayF32.class);
		GrayU8 binary = new GrayU8(input.width,input.height);

		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeOtsu(input, 0, 255), true);
		gui.addImage(VisualizeBinaryData.renderBinary(binary, false, null),"Global: Otsu");
		gui.addImage(ConvertBufferedImage.convertTo(input,null),"Input Image");


		// Накладываем фильтр, удаляющий лишние белые пиксели
		GrayU8 filtered = BinaryImageOps.erode8(binary, 1, null);
		filtered = BinaryImageOps.dilate8(filtered, 1, null);

		// Ищем центр
		findCenterOfTheObject(filtered);

		// Центр картинки
		xBaseCenter = filtered.getWidth() / 2;
		yBaseCenter = filtered.getHeight() / 2;

		// Сдвиг изображения в центр картинки
		GrayU8 transferedImage = transferImage(filtered);
		findCenterOfTheObject(transferedImage);


		// Поворачивает объект
		// Обновляем координаты ручки после сдвига
		updateObjectCoordinates(transferedImage);
		// Ищем самый удаленный от центра пиксель
		findTheMostRemovedPixelFromCenter();

		BufferedImage convertedFromTransferredImage = ConvertBufferedImage.convertTo(transferedImage, null);
		AffineTransform rotation = new AffineTransform();
		double angleOfRotation;

		if (yMaxRemovalPixel <= yBaseCenter) {
			angleOfRotation = Math.atan2(Math.abs(xMaxRemovalPixel - xBaseCenter), Math.abs(yMaxRemovalPixel - yBaseCenter));
		} else {
			angleOfRotation = Math.PI - Math.atan2(Math.abs(xMaxRemovalPixel - xBaseCenter), Math.abs(yMaxRemovalPixel - yBaseCenter));
		}

		if (xMaxRemovalPixel > xBaseCenter) {
			angleOfRotation *= -1;
		}

		double radians = angleOfRotation;

		rotation.rotate(radians, convertedFromTransferredImage.getWidth() / 2, convertedFromTransferredImage.getHeight() / 2);
		AffineTransformOp rotationOperation = new AffineTransformOp(rotation, AffineTransformOp.TYPE_BILINEAR);
		convertedFromTransferredImage = rotationOperation.filter(convertedFromTransferredImage, null);

		GrayF32 newInput = ConvertBufferedImage.convertFromSingle(convertedFromTransferredImage, null, GrayF32.class);
		GrayU8 newBinary = new GrayU8(newInput.width,newInput.height);

//		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeOtsu(input, 0, 255), true);
//		gui.addImage(VisualizeBinaryData.renderBinary(filtered, false, null),"Filetered");

		GThresholdImageOps.threshold(input, binary, GThresholdImageOps.computeOtsu(input, 0, 255), true);
		gui.addImage(VisualizeBinaryData.renderBinary(transferedImage, false, null),"Transfered");

		// Отображение полученного изображения
		GThresholdImageOps.threshold(newInput, newBinary, GThresholdImageOps.computeOtsu(newInput, 0, 255), true);
		gui.addImage(VisualizeBinaryData.renderBinary(newBinary, false, null),"TransferedAndRotated");


		String fileName =  imageName.substring(imageName.lastIndexOf('/')+1);
		ShowImages.showWindow(gui,fileName);
	}

	public void updateObjectCoordinates(GrayU8 image) {
		xOfPen.clear();
		yOfPen.clear();

		for (int i = 0; i < image.width; i++) {
			for (int j = 0; j < image.height; j++) {
				if (image.get(i, j) == 1) {
					xOfPen.add(i);
					yOfPen.add(j);
				}
			}
		}
	}


	public <T extends ImageGray, D extends ImageGray>
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


	public GrayU8 transferImage(GrayU8 filtered) {
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


	public void findTheMostRemovedPixelFromCenter() {

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



	public void findCenterOfTheObject(GrayU8 filtered) {

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

	public void doExecute(String fileName) {
		threshold(UtilIO.pathExample(fileName));
	}

}
