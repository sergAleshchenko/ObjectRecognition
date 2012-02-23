/*
 * Copyright (c) 2011-2012, Peter Abeles. All Rights Reserved.
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

package boofcv.alg.distort;

import boofcv.alg.interpolate.InterpolatePixel;
import boofcv.core.image.border.ImageBorder;
import boofcv.struct.distort.PixelTransform_F32;
import boofcv.struct.image.ImageSingleBand;
import georegression.struct.point.Point2D_F32;

/**
 * Except for very simple functions, computing the per pixel distortion is an expensive operation.
 * To overcome this problem the distortion is computed once and cached.  Then when the image is distorted
 * again the save results are simply recalled and not computed again.
 *
 * @author Peter Abeles
 */
public abstract class ImageDistortCache<T extends ImageSingleBand> implements ImageDistort<T> {

	// size of output image
	private int width=-1,height=-1;
	private Point2D_F32 map[];
	// sub pixel interpolation
	private InterpolatePixel<T> interp;
	// handle the image border
	private ImageBorder<T> border;

	// crop boundary
	private int x0,y0,x1,y1;

	protected T srcImg;
	protected T dstImg;

	/**
	 * Specifies configuration parameters
	 *
	 * @param interp Interpolation algorithm
	 * @param border How borders are handled
	 */
	public ImageDistortCache(InterpolatePixel<T> interp,
							 ImageBorder<T> border) {
		this.interp = interp;
		this.border = border;
	}

	@Override
	public void setModel(PixelTransform_F32 dstToSrc) {
		int index = 0;
		for( int y = 0; y < height; y++ ) {
			for( int x = 0; x < width; x++ ) {
				dstToSrc.compute(x,y);
				map[index++].set(dstToSrc.distX,dstToSrc.distY);
			}
		}
	}

	@Override
	public void apply(T srcImg, T dstImg) {
		init(srcImg, dstImg);

		x0 = 0;y0 = 0;x1 = dstImg.width;y1 = dstImg.height;

		if( border != null )
			applyBorder();
		else
			applyNoBorder();
	}

	@Override
	public void apply(T srcImg, T dstImg, int dstX0, int dstY0, int dstX1, int dstY1) {
		init(srcImg, dstImg);

		x0 = dstX0;y0 = dstY0;x1 = dstX1;y1 = dstY1;

		if( border != null )
			applyBorder();
		else
			applyNoBorder();
	}

	private void init(T srcImg, T dstImg) {
		if( width == -1 ) {
			width = dstImg.width;
			height = dstImg.height;
			map = new Point2D_F32[width*height];
			for( int i = 0; i < map.length; i++ ) {
				map[i] = new Point2D_F32();
			}
		} else if( dstImg.width != width || dstImg.height != height )
			throw new IllegalArgumentException("Unexpected dstImg dimension");

		this.srcImg = srcImg;
		this.dstImg = dstImg;
		interp.setImage(srcImg);
	}

	public void applyBorder() {

		border.setImage(srcImg);

		final float minInterpX = interp.getUnsafeBorderX();
		final float minInterpY = interp.getUnsafeBorderY();
		final float maxInterpX = srcImg.getWidth()-interp.getUnsafeBorderX();
		final float maxInterpY = srcImg.getHeight()-interp.getUnsafeBorderY();

		final float widthF = srcImg.getWidth();
		final float heightF = srcImg.getHeight();

		for( int y = y0; y < y1; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			for( int x = x0; x < x1; x++ , indexDst++ ) {
				Point2D_F32 s = map[indexDst];

				if( s.x < minInterpX || s.x >= maxInterpX || s.y < minInterpY || s.y >= maxInterpY ) {
					if( s.x < 0f || s.x >= widthF || s.y < 0f || s.y >= heightF )
						assign(indexDst,(float)border.getGeneral((int)s.x,(int)s.y));
					else
						assign(indexDst,interp.get(s.x, s.y));
				} else {
					assign(indexDst,interp.get_unsafe(s.x, s.y));
				}
			}
		}
	}

	public void applyNoBorder() {
		final float minInterpX = interp.getUnsafeBorderX();
		final float minInterpY = interp.getUnsafeBorderY();
		final float maxInterpX = srcImg.getWidth()-interp.getUnsafeBorderX();
		final float maxInterpY = srcImg.getHeight()-interp.getUnsafeBorderY();

		final float widthF = srcImg.getWidth();
		final float heightF = srcImg.getHeight();

		for( int y = y0; y < y1; y++ ) {
			int indexDst = dstImg.startIndex + dstImg.stride*y + x0;
			for( int x = x0; x < x1; x++ , indexDst++ ) {
				Point2D_F32 s = map[indexDst];

				if( s.x < minInterpX || s.x >= maxInterpX || s.y < minInterpY || s.y >= maxInterpY ) {
					if( s.x >= 0f && s.x < widthF && s.y >= 0f && s.y < heightF )
						assign(indexDst,interp.get(s.x, s.y));
				} else {
					assign(indexDst,interp.get_unsafe(s.x, s.y));
				}
			}
		}
	}

	protected abstract void assign( int indexDst , float value );
}