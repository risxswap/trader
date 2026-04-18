package cc.riskswap.trader.collector.common.util;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

public class DateUtil {

    public static final DateTimeFormatter FUTURES_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ssXXX");

    public static final String DT_FORMAT_WITH_MS = "yyyy-MM-dd HH:mm:ss.SSS";
	public static final String DT_FORMAT_WITH_MS_INT = "yyyyMMddHHmmssSSS";
	public static final String DT_FORMAT = "yyyy-MM-dd HH:mm:ss";
	public static final String DT_FORMAT_INT = "yyyyMMddHHmmss";

	public static final String T_FORMAT_WITH_MS_INT = "HHmmssSSS";
	public static final String T_FORMAT_WITH_MS = "HH:mm:ss.SSS";
	public static final String T_FORMAT_INT = "HHmmss";
	public static final String T_FORMAT = "HH:mm:ss";
	public static final String D_FORMAT_INT = "yyyyMMdd";
	public static final String D_FORMAT = "yyyy-MM-dd";

	public static final DateTimeFormatter DT_FORMAT_WITH_MS_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_WITH_MS);
	public static final DateTimeFormatter DT_FORMAT_WITH_MS_INT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_WITH_MS_INT);
	public static final DateTimeFormatter DT_FORMAT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT);
	public static final DateTimeFormatter DT_FORMAT_INT_FORMATTER = DateTimeFormatter.ofPattern(DT_FORMAT_INT);

	public static final DateTimeFormatter T_FORMAT_WITH_MS_INT_FORMATTER = DateTimeFormatter.ofPattern(T_FORMAT_WITH_MS_INT);
	public static final DateTimeFormatter T_FORMAT_WITH_MS_FORMATTER = DateTimeFormatter.ofPattern(T_FORMAT_WITH_MS);
	public static final DateTimeFormatter T_FORMAT_INT_FORMATTER = DateTimeFormatter.ofPattern(T_FORMAT_INT);
	public static final DateTimeFormatter T_FORMAT_FORMATTER = DateTimeFormatter.ofPattern(T_FORMAT);
	public static final DateTimeFormatter D_FORMAT_INT_FORMATTER = DateTimeFormatter.ofPattern(D_FORMAT_INT);
	public static final DateTimeFormatter D_FORMAT_FORMATTER = DateTimeFormatter.ofPattern(D_FORMAT);

	/**
	 * 解析期货日期时间字符串为Instant
	 *
	 * @param dateTimeStr 日期时间字符串，格式为"yyyy-MM-dd HH:mm:ssXXX"
	 * @return 解析后的Instant对象
	 */
	public static Instant parseFuturesDateTime(String dateTimeStr) {
		return ZonedDateTime.parse(dateTimeStr, FUTURES_DATE_FORMAT).toInstant();
	}

	/**
	 * 解析期货日期时间字符串为LocalDateTime
	 *
	 * @param dateTimeStr 日期时间字符串，格式为"yyyy-MM-dd HH:mm:ssXXX"
	 * @return 解析后的LocalDateTime对象
	 */
	public static LocalDateTime parseToLocalDateTime(String dateTimeStr) {
		return ZonedDateTime.parse(dateTimeStr, FUTURES_DATE_FORMAT).toLocalDateTime();
	}

	/**
	 * 解析期货日期时间字符串为 OffsetDateTime（保留时区偏移）
	 *
	 * @param dateTimeStr 日期时间字符串，格式为"yyyy-MM-dd HH:mm:ssXXX"
	 * @return 解析后的 OffsetDateTime 对象
	 */
	public static OffsetDateTime parseToOffsetDateTime(String dateTimeStr) {
		return ZonedDateTime.parse(dateTimeStr, FUTURES_DATE_FORMAT).toOffsetDateTime();
	}

	public static OffsetDateTime parseToOffsetDateTime(String dateStr, DateTimeFormatter formatter) {
		return ZonedDateTime.parse(dateStr, formatter).toOffsetDateTime();
	}
	
	/**
	 * 使用LocalDate解析日期字符串，然后转换为OffsetDateTime
	 * 适用于只有日期没有时间的字符串，如"yyyyMMdd"格式
	 *
	 * @param dateStr 日期字符串
	 * @param formatter 日期格式
	 * @return 解析后的OffsetDateTime对象，时间部分为当天的开始时间
	 */
	public static OffsetDateTime parseLocalDateToOffsetDateTime(String dateStr, DateTimeFormatter formatter) {
		LocalDate localDate = LocalDate.parse(dateStr, formatter);
		return localDate.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
	}

	public static String format(LocalDate date, DateTimeFormatter formatter) {
		return date.format(formatter);
	}
	
	/**
	 * Convert LocalDate to OffsetDateTime
	 * Sets the time to start of day (00:00:00) in the system default time zone
	 *
	 * @param date LocalDate to convert
	 * @return OffsetDateTime representing the start of the specified date
	 */
	public static OffsetDateTime toOffsetDateTime(LocalDate date) {
		return date.atStartOfDay().atZone(ZoneId.systemDefault()).toOffsetDateTime();
	}
}
