package org.tafia.captcha;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * 图片切割
 * 
 * @author Dason
 * @date 2016年10月8日
 *
 */
public class ImageInciser {
	
	/**
	 * 前景色
	 */
	private static final int FOREGROUND_COLOR = 0xFF << 24;
	
	/**
	 * 背景色
	 */
	private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
	
	
	private BufferedImage image;

	/**
	 * 创建一个image的切割器
	 * 
	 * @param image 被切割的图片
	 */
	public ImageInciser(BufferedImage image) {
		this.image = image;
	}
	
	/**
	 * 将图片平均切割为num个小图片
	 * 
	 * <p>字符间使用5pix宽的像素填充</p>
	 * @param num 切割的数量
	 */
	public BufferedImage fixedIncise(int num) {
		image = trim(0, 0, image.getWidth(), image.getHeight());
		int width = image.getWidth();
		int height = image.getHeight();
		if (width < num) return image;
		BufferedImage newImage = new BufferedImage(width+num*5+5, height, BufferedImage.TYPE_INT_RGB);
		int step = width / num;
		for (int i = 0, l = 0; i < width; i++,l++) {
			if (i % step == 0) {
				for (int r = 0; r < 5; r++) {
					for (int s = 0; s < height; s++) {
						newImage.setRGB(l+r, s, BACKGROUND_COLOR);
					}
				}
				l += 5;
			}
			for (int j = 0; j < height; j++) {
				newImage.setRGB(l, j, image.getRGB(i, j));
			}
		}
		image = newImage;
		return image;
	}
	
	/**
	 * 扫线法切割图片
	 * 
	 * @return 切割后的图片集合，集合的大小取决于原图的干扰程度
	 * 当集合大小为1时，则图片无法切割为更小的图片
	 */
	public List<BufferedImage> blankInterval() {
		int width = image.getWidth();
		int height = image.getHeight();
		//计算水平灰度直方图
		int[] projection = new int[width];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (image.getRGB(x, y) == FOREGROUND_COLOR) projection[x]++;
			}
		}
		List<BufferedImage> list = new ArrayList<>();
		int i = 0;
		while (i < width) {
			while (i < width && projection[i] == 0) i++;
			if (i == width) break;
			int left = i;
			while (i < width && projection[i] != 0) i++;
			int right = i;
			//宽度或高度小于11像素的截取的图片会被忽略
			BufferedImage subImage = trim(left, right, right-left, height);
			if (subImage.getWidth() < 11 || subImage.getHeight() < 11) continue;
			list.add(subImage);
		}
		return list;
		
	}
	
	public BufferedImage trim(int x, int y, int width, int height) {
		int left = x;
		leftLabel :
		for (int i = x; i < x + width; i++) {
			for (int j = y; j < y + height; j++) {
				if (image.getRGB(i, j) == FOREGROUND_COLOR) break leftLabel;
			}
			left++;
		}
		int right = x + width - 1;
		rightLabel :
		for (int i = x + width - 1; i > left; i--) {
			for (int j = y; j < y + height; j++) {
				if (image.getRGB(i, j) == FOREGROUND_COLOR) break rightLabel;
			}
			right--;
		}
		int top = y;
		topLabel :
		for (int i = y; i < y + height; i++) {
			for (int j = x; j < x + width; j++) {
				if (image.getRGB(j, i) == FOREGROUND_COLOR) break topLabel;
			}
			top++;
		}
		int bottom = y + height - 1;
		bottomLabel :
		for (int i = y + height - 1; i > top; i--) {
			for (int j = x; j < x + width; j++) {
				if (image.getRGB(j, i) == FOREGROUND_COLOR) break bottomLabel;
			}
			bottom--;
		}
		if (left == x + width) left--;
		if (top == y + height) top--;
		return image.getSubimage(left, top, right-left+1, bottom-top+1);
	}
	
	
}
