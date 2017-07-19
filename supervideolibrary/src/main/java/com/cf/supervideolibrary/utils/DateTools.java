package com.cf.supervideolibrary.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTools {

	/**
	 * 获取指定格式的当前时间
	 * @param timeFormat 时间格式： 例如yyyy-MM-dd HH:mm
	 * @return
	 */
	public static String getTime(String timeFormat) {
		SimpleDateFormat format = new SimpleDateFormat(timeFormat);
		return format.format(new Date());
	}
	/**
	 * 将时间long转成String
	 * @param time  long类型的毫秒值
	 * @param timeFormat timeFormat 时间格式： 例如yyyy-MM-dd HH:mm
	 * @return
	 */
	public static String getTime(long time, String timeFormat) {
		SimpleDateFormat format = new SimpleDateFormat(timeFormat);
		return format.format(new Date(time));
	}
	/**
	 * 将时间String转换成long
	 * @param strTime
	 * @return
	 */
	public static long timeToLong(String strTime) {
		SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		long longTime = 0;
		try {
			longTime = format.parse(strTime).getTime();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return longTime;
	}

	/**
	 * 计算两个##:##格式的时间 段(视频总播放时间--格式##:##String)
	 * 
	 * @param start
	 *            开始的时间
	 * @param end
	 *            结束的时间
	 * @return
	 */
	public static String getTimePeriod(String start, String end) {
		String timeStr = null;
		int startInt = Integer.parseInt(start.split(":")[0]) * 60
				+ Integer.parseInt(start.split(":")[1]);
		int endInt = Integer.parseInt(end.split(":")[0]) * 60
				+ Integer.parseInt(end.split(":")[1]);
		int timeInt = endInt - startInt;
		if (timeInt >= 60) {
			if (timeInt % 60 >= 10) {
				timeStr = timeInt / 60 + ":" + timeInt % 60 + ":00";
			} else {
				timeStr = timeInt / 60 + ":0" + timeInt % 60 + ":00";
			}
		} else {
			timeStr = timeInt + ":00";
		}
		return timeStr;
	}

	/**
	 * 计算两个##:##格式的时间 段(总时间int)---视频进度条使用
	 * 
	 * @param start
	 *            开始的时间
	 * @param end
	 *            结束的时间
	 * @return
	 */
	public static int getTimePeriodInt(String start, String end) {
		int timeInt = 0;
		int startInt = Integer.parseInt(start.split(":")[0]) * 60
				+ Integer.parseInt(start.split(":")[1]);
		int endInt = Integer.parseInt(end.split(":")[0]) * 60
				+ Integer.parseInt(end.split(":")[1]);
		timeInt = (endInt - startInt) * 60;

		return timeInt;
	}

	/**
	 * 将long型的时间转成##:##格式的时间
	 */
	public static String generateTime(long time) {
		int totalSeconds = (int) (time / 1000);
		int seconds = totalSeconds % 60;
		int minutes = (totalSeconds / 60) % 60;
		int hours = totalSeconds / 3600;

		return hours > 0 ? String.format("%02d:%02d:%02d", hours, minutes, seconds) : String.format("%02d:%02d", minutes, seconds);
	}
}
