package org.tafia.captcha;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;

/**
 * 验证码解析调度类
 * 
 * @author Dason
 * @date 2016年10月11日
 *
 */
public class CaptchaParse {

	private CaptchaConfig config;
	
	public CaptchaParse(CaptchaConfig config) {
		this.config = config;
	}
	
	public String parse() {
		String result = "";
		try {
			BufferedImage image = ImageLoader.load(config.source);
			ImageFilter filter = new ImageFilter(image);
			if (config.border != 0) {
				image = filter.clearBorder(config.border).render();
				filter = new ImageFilter(image);
			}
			if (config.italic) {
				image = filter.reitalic().render();
				filter = new ImageFilter(image);
			}
			if (config.threshold == 0) {
				filter.graying().binaryzation();
			} else if (config.threshold > 0){
				filter.graying().binaryzation(config.threshold);
			}
			if (config.noise == 0) {
				filter.clearNoise();
			} else if(config.noise > 0) {
				filter.clearNoise(config.noise);
			}
			if (config.block == 0) {
				filter.clearBlock();
			} else if(config.block > 0){
				filter.clearBlock(config.block);
			}
			if (config.isometry) {
				ImageInciser inciser = new ImageInciser(filter.render());
				image = inciser.fixedIncise(config.chars);
				filter = new ImageFilter(image);
				filter.clearBlock();
			}
			if (config.save) {
				StringBuilder sb = new StringBuilder(config.source);
				int index = sb.lastIndexOf(".");
				if (config.type == null) config.type = sb.substring(index+1);
				sb.insert(index, ".parsed");
				ImageIO.write(filter.render(), config.type, new File(sb.toString()));
			}
			result = ImageParser.origin(filter.render(), config.chinese);
			if (config.semantic) {
				result = ImageParser.semantic(result);
			}
		} catch (IOException e) {
			result =  "无法读取资源"+e;
		}
		return result;
	}
	
	/**
	 * 验证码解析配置
	 * 
	 * @author Dason
	 * @date 2016年10月11日
	 *
	 */
	public static class CaptchaConfig {
		
		private CaptchaConfig() {}
		/**
		 * 资源路径
		 */
		private String source;
		/**
		 * 边框宽度
		 */
		private int border;
		/**
		 * 阈值
		 */
		private int threshold;
		/**
		 * 某像素和周边8个像素最小相同数量
		 */
		private int noise;
		/**
		 * 独立像素块的像素最小数量
		 */
		private int block;
		/**
		 * 字符数
		 */
		private int chars;
		/**
		 * 是否包含中文
		 */
		private boolean chinese;
		/**
		 * 是否是斜体
		 */
		private boolean italic;
		/**
		 * 字符是否等距
		 */
		private boolean isometry;
		/**
		 * 是否语义分析
		 */
		private boolean semantic;
		/**
		 *是否保存处理后的图片 
		 */
		private boolean save;
		/**
		 * 图片格式
		 */
		private String type;
		/**
		 * 验证码解析配置建造者
		 * 
		 * @author Dason
		 * @date 2016年10月11日
		 *
		 */
		public static class Builder {
			
			private CaptchaConfig config = new CaptchaConfig();
			
			{
				threshold(-1);
				noise(-1);
				block(-1);
			}
			/**
			 * 资源路径
			 */
			public Builder source(String source) {
				config.source = source;
				return this;
			}
			/**
			 * 边框宽度
			 */
			public Builder border(int border) {
				config.border = border;
				return this;
			}
			/**
			 * 阈值
			 */
			public Builder threshold(int threshold) {
				config.threshold = threshold;
				return this;
			}
			/**
			 * 某像素和周边8个像素最小相同数量
			 */
			public Builder noise(int noise) {
				config.noise = noise;
				return this;
			}
			/**
			 * 独立像素块的像素最小数量
			 */
			public Builder block(int block) {
				config.block = block;
				return this;
			}
			/**
			 * 字符数
			 */
			public Builder chars(int chars) {
				config.chars = chars;
				return this;
			}
			/**
			 * 包含中文
			 */
			public Builder chinese() {
				config.chinese = true;
				return this;
			}
			/**
			 * 是斜体
			 */
			public Builder italic() {
				config.italic = true;
				return this;
			}
			/**
			 * 字符等距
			 */
			public Builder isometry() {
				config.isometry = true;
				return this;
			}
			/**
			 * 语义分析
			 */
			public Builder semantic() {
				config.semantic = true;
				return this;
			}
			/**
			 * 保存过程图片
			 */
			public Builder save() {
				config.save = true;
				return this;
			}
			/**
			 * 图片格式
			 */
			public Builder type(String type) {
				config.type = type;
				return this;
			}
			
			public CaptchaConfig build() {
				return config;
			}
		}
	}
}
