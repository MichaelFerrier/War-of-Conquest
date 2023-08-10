//
// War of Conquest Server
// Copyright (c) 2002-2023 Michael Ferrier, IronZog LLC
//
// This program is free software: you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation, either version 3 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program.  If not, see <http://www.gnu.org/licenses/>.
//
//

package WOCServer;

import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import javax.imageio.ImageIO;
import ij.*;
import ij.process.*;
import ij.plugin.filter.*;
import ij.plugin.frame.*;
import ij.gui.*;
import java.awt.*;
import java.awt.color.*;
import WOCServer.*;

public class ImageUtils
{
	public static void ScaleImage(ImagePlus _imp, int _new_width, int _new_height)
	{
		// Scale the image
		ImageProcessor ip = _imp.getProcessor();
		ip.setInterpolationMethod(ImageProcessor.BICUBIC);
		ImageProcessor ip_scaled = ip.resize(_new_width, _new_height); // of the same type as the original
		_imp.setProcessor(_imp.getTitle(), ip_scaled); // UPDATE the original ImagePlus
	}

	public static void ThresholdSelection(ImagePlus _imp, boolean _include, int _minRed, int _maxRed, int _minGreen, int _maxGreen, int _minBlue, int _maxBlue)
	{
		byte[] rSource, gSource, bSource;

		if (_imp.getType()  !=ImagePlus.COLOR_RGB)
		{
			Output.PrintToScreen("ThresholdSelection(): Image type must be RGB");
			return;
		}

		ImageProcessor ip = _imp.getProcessor();

		int width = _imp.getWidth();
		int height = _imp.getHeight();
		int numPixels = width*height;

		rSource = new byte[numPixels];
		gSource = new byte[numPixels];
		bSource = new byte[numPixels];

		ImageProcessor mask = new ByteProcessor(width, height);
		_imp.setProperty("Mask", mask);

		// Get rgb from image.
		ColorProcessor cp = (ColorProcessor)ip;
		cp.getRGB(rSource,gSource,bSource);

		ImageProcessor fillMaskIP = (ImageProcessor)_imp.getProperty("Mask");
		if (fillMaskIP==null)
		{
			Output.PrintToScreen("ThresholdSelection(): Image does not have mask property");
			return;
		}

		// Fill the image's mask
		byte[] fillMask = (byte[])fillMaskIP.getPixels();
		byte fill = (byte)255;
		byte keep = (byte)0;
		for (int j = 0; j < numPixels; j++){
			int red = rSource[j]&0xff;
			int green = gSource[j]&0xff;
			int blue = bSource[j]&0xff;
			if (((red < _minRed)||(red > _maxRed)) || ((green < _minGreen)||(green > _maxGreen)) || ((blue < _minBlue)||(blue > _maxBlue)))
				fillMask[j] = keep;
			else
				fillMask[j] = fill;
		}

		// Store original pixels, to later reset.
		storeOriginal(_imp);

		// Make all pixels within mask, white.
		int[] pixels = (int[])ip.getPixels();
		int fcolor = _include?0xffffffff:0xff000000;
		int bcolor = _include?0xff000000:0xffffffff;
		for (int i=0; i<numPixels; i++) {
			if (fillMask[i]!=0)
				pixels[i] = fcolor;
			else
				pixels[i]= bcolor;
		}

		// Use threshold to make selection out of only the white pixels.
		ImageProcessor ip8 = _imp.getProcessor().convertToByte(false);
		ip8.setThreshold(255, 255, ImageProcessor.NO_LUT_UPDATE);
		Roi roi = (new ThresholdToSelection()).convert(ip8);
		//Output.PrintToScreen("roi: " + roi);

		// Set the image's selection.
		_imp.setRoi(roi);

		// Reset the image's pixel data.
		reset(_imp);
	}

	public static void storeOriginal(ImagePlus _imp)
	{
		ImageProcessor ip = _imp.getProcessor();
		ImagePlus originalImage = _imp.createImagePlus();
		originalImage.setTitle(_imp.getTitle()+" (Original)");
		originalImage.setProcessor(ip.duplicate());
		_imp.setProperty("OriginalImage", originalImage);
	}

	public static void reset(ImagePlus _imp)
	{
		int numPixels = _imp.getWidth() * _imp.getHeight();
		ImageProcessor ip = _imp.getProcessor();
		ImagePlus originalImage = (ImagePlus)_imp.getProperty("OriginalImage");
		if ((originalImage!=null) && (originalImage.getBitDepth()==24)) {
			int[] restore = (int[])originalImage.getProcessor().getPixels();
			int[] pixels = (int[])ip.getPixels();
			for (int i=0; i<numPixels; i++)
				pixels[i] = restore[i];
		}
	}

	public static Vector2 FindColorIndex(ImagePlus _imp, int _color_index, int _start_x)
	{
		ImageProcessor ip = _imp.getProcessor();
		for (int x=_start_x; x < _imp.getWidth(); x++)
		{
			for (int y=0; y < _imp.getHeight(); y++)
			{
				if (ip.get(x,y) == _color_index) {
					return new Vector2(x, y);
				}
			}
		}
		return null;
	}

	public static void MultiplyComposite(ImagePlus _source, ImagePlus _dest, int _x, int _y)
	{
		ImageProcessor source_ip = _source.getProcessor();
		ImageProcessor dest_ip = _dest.getProcessor();

		int sp, dp, sr, sg, sb, dr, dg, db;

		int x1 = Math.min(_source.getWidth(), _dest.getWidth() - _x);
		int y1 = Math.min(_source.getHeight(), _dest.getHeight() - _y);

		for (int y = 0; y < y1; y++)
		{
			for (int x = 0; x < x1; x++)
			{
				sp = source_ip.get(x, y);
				sr = (sp & 0x00FF0000) >> 16;
				sg = (sp & 0x0000FF00) >> 8;
				sb = (sp & 0x000000FF);
				dp = dest_ip.get(x + _x, y + _y);
				dr = (dp & 0x00FF0000) >> 16;
				dg = (dp & 0x0000FF00) >> 8;
				db = (dp & 0x000000FF);
				dest_ip.putPixel(x + _x, y + _y, (((int)(((float)sr / 255f) * dr)) << 16) | (((int)(((float)sg / 255f) * dg)) << 8) | ((int)(((float)sb / 255f) * db)));
			}
		}
	}

	public static void SelectRectangle(ImagePlus _imp, float _x, float _y, float _w, float _h)
	{
		_imp.setRoi(new Roi((int)_x, (int)_y, (int)_w, (int)_h));
	}

	public static void SelectCircle(ImagePlus _imp, Vector2 _origin, float _radius)
	{
		_imp.setRoi(new OvalRoi((int)(_origin.x - _radius), (int)(_origin.y - _radius), (int)(2 * _radius), (int)(2 * _radius)));
	}

	public static void SelectLine(ImagePlus _imp, Vector2 _v0, Vector2 _v1)
	{
		float x_array[] = new float[2];
		float y_array[] = new float[2];
		x_array[0] = _v0.x;
		x_array[1] = _v1.x;
		y_array[0] = _v0.y;
		y_array[1] = _v1.y;
		_imp.setRoi(new PolygonRoi(x_array, y_array, 2, Roi.POLYLINE));
	}

	public static void SelectTriangle(ImagePlus _imp, Vector2 _v0, Vector2 _v1, Vector2 _v2)
	{
		float x_array[] = new float[3];
		float y_array[] = new float[3];
		x_array[0] = _v0.x;
		x_array[1] = _v1.x;
		x_array[2] = _v2.x;
		y_array[0] = _v0.y;
		y_array[1] = _v1.y;
		y_array[2] = _v2.y;
		_imp.setRoi(new PolygonRoi(x_array, y_array, 3, Roi.POLYGON));
	}

	public static class Vector2
	{
		float x, y;

		public Vector2()
		{
			x = 0;
			y = 0;
		}

		public Vector2(float _x, float _y)
		{
			x = _x;
			y = _y;
		}

		public static Vector2 AngleToVector(float _angle, float _dist)
		{
			Vector2 result = new Vector2();
			result.x = (float)Math.cos(_angle *  Math.PI/180f) * _dist;
			result.y = (float)Math.sin(_angle *  Math.PI/180f) * _dist;
			return result;
		}

		public static Vector2 Add(Vector2 _v0, Vector2 _v1)
		{
			Vector2 result = new Vector2();
			result.x = _v0.x + _v1.x;
			result.y = _v0.y + _v1.y;
			return result;
		}

		public static Vector2 Multiply(Vector2 _v0, float _multiplier)
		{
			Vector2 result = new Vector2();
			result.x = _v0.x * _multiplier;
			result.y = _v0.y * _multiplier;
			return result;
		}
	}
}
