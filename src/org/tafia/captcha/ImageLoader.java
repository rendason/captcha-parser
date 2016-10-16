package org.tafia.captcha;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import javax.imageio.ImageIO;

/**
 * 图片加载器
 * 
 * @author Dason
 * @date 2016年10月8日
 *
 */
public class ImageLoader {

	public static BufferedImage load(String path) throws IOException{
		return load(new File(path));
	}
	
	public static BufferedImage load(File imageFile) throws IOException{
		return ImageIO.read(imageFile);
	}
	
	public static BufferedImage load(URL url) throws IOException{
		return ImageIO.read(url);
	}
	
	
}
