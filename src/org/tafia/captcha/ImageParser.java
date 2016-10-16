package org.tafia.captcha;

import java.awt.image.BufferedImage;
import net.sourceforge.pinyin4j.PinyinHelper;
import net.sourceforge.pinyin4j.format.HanyuPinyinCaseType;
import net.sourceforge.pinyin4j.format.HanyuPinyinOutputFormat;
import net.sourceforge.pinyin4j.format.HanyuPinyinToneType;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

/**
 * 字符图片读取、语义分析
 * 
 * @author Dason
 * @date 2016年10月10日
 *
 */
public class ImageParser {

	/**
	 * 符号映射
	 */
	private static String[][] symbolMap = {
			{"加上", "+"},
			{"加", "+"},
			{"减去", "-"},
			{"减", "-"},
			{"乘上", "*"},
			{"乘", "*"},
			{"除去", "/"},
			{"除以", "/"},
			{"等于", ""},
			{"几", ""},
			{"?", ""},
			{"零", "0"},
			{"一", "1"},
			{"二", "2"},
			{"三", "3"},
			{"四", "4"},
			{"五", "5"},
			{"六", "6"},
			{"七", "7"},
			{"八", "8"},
			{"九", "9"},
			{"十", "10"},
			{"壹", "1"},
			{"贰", "2"},
			{"叁", "3"},
			{"肆", "4"},
			{"伍", "5"},
			{"陆", "6"},
			{"柒", "7"},
			{"捌", "8"},
			{"玖", "9"},
			{"拾", "10"},
			{"的拼音", "{$}"},
			{"首字母", "{#}"},
	};
	
	/**
	 * 图片识别原文，包含中文
	 * @param image 字符图片
	 * @return 图片内容
	 */
	public static String origin(BufferedImage image) {
		return origin(image, true);
	}
	
	/**
	 * 图片识别原文
	 * @param image 字符图片
	 * @param chiness 是否包含中文
	 * @return 图片内容
	 */
	public static String origin(BufferedImage image, boolean chiness) {
		Tesseract instance = new Tesseract();
		if (chiness) instance.setLanguage("chi_sim");
		else instance.setLanguage("eng");
		try {
			return instance.doOCR(image).trim().replace(" ", "");
		} catch (TesseractException e) {
			return "内容识别失败";
		}
	}
	
	/**
	 * 语义分析
	 * 
	 * <p>仅支持加减乘除、拼音、拼音首字母</p>
	 * @param content 语句
	 * @return 结果
	 */
	public static String semantic(String content) {
		for (String[] pair : symbolMap) {
			String key = pair[0];
			String value = pair[1];
			if (content.contains(key)) content = content.replace(key, value);
		}
		return compute(content);
	}
	
	/**
	 * 计算表达式
	 * 
	 * @param express 表达式
	 * @return 表达式结果
	 */
	private static String compute(String express) {
		if (express.contains("{$}")) {
			boolean capital = express.contains("{#}");
			int index = express.indexOf("{$}");
			String target = express.substring(0, index);
			HanyuPinyinOutputFormat format = new HanyuPinyinOutputFormat();
			format.setCaseType(HanyuPinyinCaseType.LOWERCASE);
			format.setToneType(HanyuPinyinToneType.WITHOUT_TONE);
			String pinyin = "";
			try {
				for (int i = 0; i < target.length(); i++) {
					char ch = target.charAt(i);
					
						String s = PinyinHelper.toHanyuPinyinStringArray(ch, format)[0];
						if (capital) {
							pinyin += s.charAt(0);
						} else {
							pinyin += s;
						}
				}
			} catch (BadHanyuPinyinOutputFormatCombination e) {
				pinyin = "获取“"+target+"”的拼音失败";
			}
			return pinyin;
		}
		String[] ops = express.split("\\+|-|\\*|\\\\", 3);
		if (ops.length != 2) return "错误的表达式";
		int num1 = Integer.parseInt(ops[0]);
		int num2 = Integer.parseInt(ops[1]);
		String op = express.substring(ops[0].length(), ops[0].length()+1);
		int result = 0;
		if (op.equals("+")) {
			result = num1 + num2;
		} else if (op.equals("-")) {
			result = num1 - num2;
		} else if (op.equals("*")) {
			result = num1 * num2;
		} else if (op.equals("/")) {
			result = num1 / num2;
		} 
		return String.valueOf(result);
	}
	
	
}
