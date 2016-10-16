package org.tafia.captcha;

import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * 图片过滤
 * 
 * @author Dason
 * @date 2016年10月8日
 *
 */
public class ImageFilter {

	/**
	 * 前景色
	 */
	private static final int FOREGROUND_COLOR = 0xFF << 24;
	
	/**
	 * 背景色
	 */
	private static final int BACKGROUND_COLOR = 0xFFFFFFFF;
	
	private BufferedImage image;
	
	public ImageFilter(BufferedImage image) {
		this.image = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		this.image.setData(image.getRaster());
	}
	
	/**
	 * 灰度化
	 * 
	 * @return 当前Filter对象
	 */
	public ImageFilter graying() {
		int width = image.getWidth();
		int height = image.getHeight();
		
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int rgb = image.getRGB(x, y);
				int red = (rgb >> 16) & 0xFF;
				int green = (rgb >> 8) & 0xFF;
				int blue = rgb & 0xFF;
				/*
				 * 使用加权法灰度图像，权重(r,g,b) = (0.299, 0.587, 0.114)
				 * 求出加权平均灰度gray，再另当前元素的red=gay,
				 * green=gray,blue=gray得到灰度化图
				 */
				int gray= (int)(0.299 * red + 0.587 * green + 0.114 * blue) & 0xFF;
				gray |= gray <<= 8;
				gray |= gray <<= 8;
				image.setRGB(x, y, gray);
			}
		}
		return this;
	}
	
	/**
	 * 自动阈值二值化
	 * 
	 * <p>此方法只对灰度化的图片有效</p>
	 * 
	 * @return 当前Filter对象
	 */
	public ImageFilter binaryzation() {
		int threshold = ostu();
		binaryzation(threshold);
		return this;
	}
	
	/**
	 * 二值化
	 * 
	 * <p>此方法只对灰度化的图片有效</p>
	 * 
	 * @return 当前Filter对象
	 */
	public ImageFilter binaryzation(int threshold) {
		int width = image.getWidth();
		int height = image.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int gray = image.getRGB(x, y) & 0xFF;
				if (gray > threshold) {
					image.setRGB(x, y, BACKGROUND_COLOR);
				} else {
					image.setRGB(x, y, FOREGROUND_COLOR);
				}
			}
		}
		rectify();
		return this;
	}
	
	/**
	 * 降低颜色数量
	 * 
	 * @param maxColor 最大的颜色数量
	 * @return
	 */
	public ImageFilter decreaseColor(int maxColor) {
		int bound = 0xFFFFFF / maxColor;
		int width = image.getWidth();
		int height = image.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int rgb = image.getRGB(x, y) & 0xFFFFFF;
				rgb = rgb / bound * bound + bound / 2;
				image.setRGB(x, y, rgb);
			}
		}
		return this;
	}
	
	/**
	 * 最大类间方差法求最佳阈值
	 * 
	 * @return 最佳灰度阈值
	 */
	private int ostu() {
		int width = image.getWidth();
		int height = image.getHeight();
		
		/*
		 * 获取灰度直方图，灰度i的像素数G(i)=histogram[i];
		 */
		float[] histogram = new float[256];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int gray = image.getRGB(x, y) & 0xFF;
				histogram[gray]++;
			}
		}
		/*
		 * 将灰度直方图的像素数转换为像素数占总像素数的比例，
		 * 灰度为i的像素所占比例R(i)=histogram[i]
		 */
		int totalPixel = width * height; //总像素数量
		float avgGrayscale = 0;          //总平均灰度
		for (int i = 0; i < 256; i++) {
			histogram[i] /= totalPixel;
			avgGrayscale += i * histogram[i];
		}
		int threshold = 0;            //最大方差下的阈值
		float maxVariance = 0;        //最大方差
		float foreGrayscaleRatio = 0; //前景灰度占比
		float foreGrayscaleSum = 0;   //前景平均灰度加权总和
		for (int i = 0; i < 256; i++) {
			foreGrayscaleRatio += histogram[i];
			foreGrayscaleSum += i * histogram[i];
			//当前阈值下的前景平均灰度
			float foreAvgGrayscale = foreGrayscaleSum / foreGrayscaleRatio;
			//类间方差公式
			float variance = (foreAvgGrayscale - avgGrayscale) 
								* (foreAvgGrayscale - avgGrayscale)
								* foreGrayscaleRatio
								/ (1-foreGrayscaleRatio);
			if (variance > maxVariance) {
				maxVariance = variance;
				threshold = i;
			}
		}
		return threshold;
	}
	
	
	/**
	 * 8邻接滤波，自动计算精度
	 */
	public ImageFilter clearNoise() {
		int accuracy = getNosieWidth();
		return clearNoise(accuracy);
	}
	
	/**
	 * 除去噪点和干扰线
	 * 
	 * <p>窗口大小3*3</p>
	 * @param round 元素周边最少元素数量，少于此值认为是独立噪点
	 * @param group 最小聚合像素数量，少于此值的独立像素块视为噪点
	 * @return 当前Filter对象
	 */
	public ImageFilter clearNoise(int accuracy) {
		
		int width = image.getWidth();
		int height = image.getHeight();
		int[] window = new int[8]; //存储当前元素周边的8个元素
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				//清除边缘
				if (x == 0 || x == width - 1 || y == 0 || y == height - 1){
					image.setRGB(x, y, BACKGROUND_COLOR);
					continue;
				}
				int current = image.getRGB(x, y);
				if (current == BACKGROUND_COLOR) continue;
				window[0] = image.getRGB(x-1, y-1);
				window[1] = image.getRGB(x, y-1);
				window[2] = image.getRGB(x+1, y-1);
				window[3] = image.getRGB(x-1, y);
				window[4] = image.getRGB(x+1, y);
				window[5] = image.getRGB(x-1, y+1);
				window[6] = image.getRGB(x, y+1);
				window[7] = image.getRGB(x+1, y+1);
				int count = 0;
				for (int i = 0; i < 8; i++) {
					if (window[i] == current) count++; 
				}
				if (count < accuracy){
					image.setRGB(x, y, BACKGROUND_COLOR);
				}
			}
		}
		return this;
	}
	
	private int getNosieWidth() {
		/*
		 * 分别获取前景色和背景色中的最大矩形边长
		 */
		int maxForeSquare = maxSquare(FOREGROUND_COLOR);
		int maxBackSquare = maxSquare(BACKGROUND_COLOR);
		/*
		 * 选择较小的边长为精度，选择反色为噪点填充颜色
		 */
		int accuracy = Math.min(maxForeSquare, maxBackSquare);
		/*
		 * 当较大边长与较小边长之商小于3时，认为精度计算无效，精度降为1
		 */
		if (accuracy == 0 || Math.max(maxForeSquare, maxBackSquare) / accuracy < 3) {
			accuracy = 1;
		}
		if (accuracy > 4) accuracy -= 2; //防止误伤，精度减2
		else accuracy = 1;
		return accuracy;
	}
	
	private int maxSquare(int rgb) {
		int length = 1;
		int width = image.getWidth();
		int height = image.getHeight();
		outer :
		while (true) {
			for (int x = 0; x <= width - length; x++) {
				for (int y = 0; y <= height - length; y++) {
					if (image.getRGB(x, y) != rgb) continue;
					if (isPureColor(x, y, length)) {
						length++;
						continue outer;
					}
				}
			}
			break;
		}
		return length-1;
	}
	/**
	 * 检查以(x,y)为左上角的边长为length边长的正方形是否纯色
	 * 
	 * @param x 横坐标
	 * @param y 纵坐标
	 * @param length 正方形边长
	 * @return 如果是纯色则返回true，否则返回false
	 */
	private boolean isPureColor(int x, int y, int length) {
		int key = image.getRGB(x, y);
		for (int i = 0; i < length; i++) {
			for (int j = 0; j < length; j++) {
				if (image.getRGB(x+i, y+j) != key) return false;
			}
		}
		return true;
	}
	
	/**
	 * 连通域法去孤立连通分量，自动计算孤立下限
	 */
	public ImageFilter clearBlock() {
		int limit = 3;
		return clearBlock(limit);
	}
	
	/**
	 * 连通域法去孤立连通分量
	 * 
	 * @param limit 孤立下限，低于此值的像素块被视为噪点
	 */
	public ImageFilter clearBlock(int limit) {
		int width = image.getWidth();
		int height = image.getHeight();
		int[][] data = new int[width][height];
		int color = 1;
		int count;
		Map<Integer, Integer> map = new HashMap<>();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if ((count = floorFill(data, x, y, color)) != 0) {
					map.put(color, count);
					color++;
				}
			}
		}
		for (Integer c : map.keySet()) {
			int n = map.get(c);
			if (n < limit) removeColor(data, c);
		}
		return this;
		/*for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				System.out.print(data[i][j]);
			}
			System.out.println();
		}*/
		//System.out.println(map);
	}
	
	
	private int floorFill(int[][] data, int i, int j, int color) {
		if (data[i][j] != 0) return 0;
		if (image.getRGB(i, j) != FOREGROUND_COLOR) return 0;
		data[i][j] = color;
		int count = 1;
		if (i > 0) count += floorFill(data, i-1, j, color);
		if (i < data.length - 1) count +=floorFill(data, i+1, j, color);
		if (j > 0) count += floorFill(data, i, j-1, color);
		if (j < data[i].length -  1) count += floorFill(data, i, j+1, color);
		if (i > 0 && j > 0) count += floorFill(data, i-1, j-1, color);
		if (i > 0 && j < data[i].length - 1) count += floorFill(data, i-1, j+1, color);
		if (i < data.length - 1 && j > 0) count += floorFill(data, i+1, j-1, color);
		if (i < data.length - 1 && j < data[i].length - 1) count += floorFill(data, i+1, j+1, color);
		return count;
	}
	
	private void removeColor(int[][] data, int color) {
		for (int i = 0; i < data.length; i++) {
			for (int j = 0; j < data[i].length; j++) {
				if (data[i][j] == color) {
					image.setRGB(i, j, BACKGROUND_COLOR);
				}
			}
		}
	}
	
	/**
	 * 矫正图像
	 * 
	 * <p>如果背景色的像素数量少于前景色的，则图像取反色</p>
	 * @return
	 */
	public ImageFilter rectify(){
		int foreCount = 0;
		int backCount = 0;
		int width = image.getWidth();
		int height = image.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				if (image.getRGB(x, y) == FOREGROUND_COLOR) foreCount++;
				else backCount++;
			}
		}
		if (foreCount > backCount) {
			/*
			 * 图像取反色
			 */
			for (int x = 0; x < width; x++) {
				for (int y = 0; y < height; y++) {
					if (image.getRGB(x, y) == FOREGROUND_COLOR) 
						image.setRGB(x, y, BACKGROUND_COLOR);
					else image.setRGB(x, y, FOREGROUND_COLOR);
				}
			}
		}
		return this;
	}
	
	/**
	 * 色彩筛选
	 * 
	 * <p>保留颜色数量的前ration个颜色，过滤其他颜色</p>
	 * @return
	 */
	public ImageFilter extractColor(float ratio, int accuracy){
		Map<Integer, Integer> map = new HashMap<>();
		int bound = accuracy;
		int width = image.getWidth();
		int height = image.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int rgb = image.getRGB(x, y) & 0xFFFFFF;
				int red = (rgb >> 16) & 0xFF;
				int green = (rgb >> 8) & 0xFF;
				int blue = rgb & 0xFF;
				red = red / bound * bound + bound / 2;
				green = green / bound * bound + bound / 2;
				blue = red / bound * bound + bound / 2;
				rgb |= red << 16 | green << 8 | blue;
				Integer value = map.get(rgb);
				if (value == null) map.put(rgb, 1);
				else map.put(rgb, value+1);
			}
		}
		int n = (int) (map.size() * ratio);
		class Pair implements Comparable<Pair>{
			int rgb;
			int num;
			public Pair(int rgb, int num) {
				this.rgb = rgb;
				this.num = num;
			}
			
			@Override
			public String toString() {
				return "(rgb=#"+Integer.toHexString(rgb)+", num="+num+")";
			}

			@Override
			public int compareTo(Pair o) {
				return o.num - num;
			}
		}
		List<Pair> list = new ArrayList<>(map.size());
		for (Integer key : map.keySet()) {
			list.add(new Pair(key, map.get(key)));
		}
		Collections.sort(list);
		List<Integer> colors = new ArrayList<>(n);
		for (int i = 0; i < n; i++) {
			Pair p = list.get(i);
			colors.add(p.rgb);
		}
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int rgb = image.getRGB(x, y) & 0xFFFFFF;
				int red = (rgb >> 16) & 0xFF;
				int green = (rgb >> 8) & 0xFF;
				int blue = rgb & 0xFF;
				red = red / bound * bound + bound / 2;
				green = green / bound * bound + bound / 2;
				blue = red / bound * bound + bound / 2;
				rgb |= red << 16 | green << 8 | blue;
				if (!colors.contains(rgb)) image.setRGB(x, y, BACKGROUND_COLOR);
			}
		}
		return this;
	}
	
	/**
	 * 色彩筛选
	 * 
	 * <p>保留颜色数量的前n个颜色，其中前n个颜色占比95%，过滤其他颜色</p>
	 * @return
	 */
	public ImageFilter extractColor(){
		return extractColor(0.95f, 10);
	}
	
	/**
	 * 删除背景
	 * 
	 * <p>此方法视出现最多的颜色为背景</p>
	 * @return 当前Filter对象
	 */
	public ImageFilter clearBackground(int bound) {
		Map<Integer, Integer> map = new HashMap<>();
		int width = image.getWidth();
		int height = image.getHeight();
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int rgb = image.getRGB(x, y) & 0xFFFFFF;
				Integer value = map.get(rgb);
				if (value == null) map.put(rgb, 1);
				else map.put(rgb, value+1);
			}
		}
		int maxNum = 0;
		int maxRgb = 0;
		for (Integer key : map.keySet()) {
			int value = map.get(key);
			if (value > maxNum) {
				maxNum = value;
				maxRgb = key;
			}
		}
		int n = 0xFFFFFF / bound;
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int rgb = image.getRGB(x, y) & 0xFFFFFF;
				if (Math.abs(rgb - maxRgb) <= n) image.setRGB(x, y, BACKGROUND_COLOR);
			}
		}
		return this;
	}
	
	/**
	 * 修复斜体
	 * @param offset
	 * @return
	 */
	public ImageFilter reitalic() {
		BufferedImage newImage = new BufferedImage(image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics2D g = (Graphics2D) newImage.getGraphics();
		g.setTransform(AffineTransform.getShearInstance(0.36, 0));
		
		g.drawImage(image, 0, 0, null);
		int off = (int)(image.getHeight()*0.36);
		g.dispose();
		image = newImage.getSubimage(off, 0, image.getWidth()-off, image.getHeight());
		return this;
	}
	
	public ImageFilter clearBorder(int borderWidth) {
		int width = image.getWidth();
		int height = image.getHeight();
		if (borderWidth < 1 || width < 2*borderWidth || height < 2*borderWidth) 
			return this;
		image = image.getSubimage(borderWidth, borderWidth, width-2*borderWidth, height-2*borderWidth);
		return this;
	}
	
	@SuppressWarnings("unused")
	private void showGrayscaleHistogram() {
		/*
		 * 获取灰度直方图，灰度i的像素数G(i)=histogram[i];
		 */
		int width = image.getWidth();
		int height = image.getHeight();
		int[] histogram = new int[256];
		for (int x = 0; x < width; x++) {
			for (int y = 0; y < height; y++) {
				int gray = image.getRGB(x, y) & 0xFF;
				histogram[gray]++;
			}
		}
		int maxValue = histogram[0];
		for (int i = 1; i < width; i++) {
			if (histogram[i] > maxValue) maxValue = histogram[i];
		}
		
		for (int i = 0; i < maxValue; i++) {
			for (int j = 0; j < width; j++) {
				if (histogram[j] > maxValue - i) System.out.print("*");
				else System.out.print(" ");
			}
			System.out.println();
		}
	}
	
	public BufferedImage render() {
		return image;
	}
	
}
