package org.tafia.captcha;

import java.util.Scanner;

import org.tafia.captcha.CaptchaParse.CaptchaConfig;

/**
 * 验证码解析启动类
 * 
 * <p>解析参数</p>
 * 
 * @author Dason
 * @date 2016年10月11日
 *
 */
public class Bootstrap {

	public static void main(String[] args) {
		System.out.println(instruction());
		Scanner scan = new Scanner(System.in);
		System.out.print("custom:");
		String input = scan.nextLine();
		scan.close();
		String[] params = input.split(" |=");
		CaptchaConfig.Builder builder = new CaptchaConfig.Builder();
		for (int i = 0; i < params.length - 1; i += 2) {
			String key = params[i];
			String value = params[i+1];
			if ("source".equals(key)) {
				builder.source(value);
			} else if ("border".equals(key)) {
				builder.border(Integer.valueOf(value));
			} else if ("threshold".equals(key)) {
				builder.threshold(Integer.valueOf(value));
			} else if ("noise".equals(key)) {
				builder.noise(Integer.valueOf(value));
			} else if ("block".equals(key)) {
				builder.block(Integer.valueOf(value));
			} else if ("chars".equals(key)) {
				builder.chars(Integer.valueOf(value));
			} else if ("chinese".equals(key)) {
				if (Boolean.valueOf(value)) builder.chinese();
			} else if ("italic".equals(key)) {
				if (Boolean.valueOf(value)) builder.italic();
			} else if ("isometry".equals(key)) {
				if (Boolean.valueOf(value)) builder.isometry();
			} else if ("semantic".equals(key)) {
				if (Boolean.valueOf(value)) builder.semantic();
			} else if ("save".equals(key)) {
				if (Boolean.valueOf(value)) builder.save();
			} else if ("type".equals(key)) {
				builder.type(value);
			}
		}
		CaptchaConfig config = builder.build();
		CaptchaParse parser = new CaptchaParse(config);
		System.out.println("process:parsing...");
		System.out.println("result:"+parser.parse());
	}
	
	/**
	 * 使用说明
	 * 
	 * @return 字符串
	 */
	private static String instruction() {
		return String.format("default:source=%s border=%d threshold=%d "
				+ "noise=%d block=%d chars=%d chinese=%b italic=%b "
				+ "isometry=%b semantic=%b save=%b type=%s",
				"partition:\\pathname", 0, -1, -1, -1, 0, 
				false, false, false, false, false, "filetype");
	}
}
