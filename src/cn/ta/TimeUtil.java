package cn.ta;

import org.apache.commons.lang3.time.DateUtils;

import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

// 对时间的特别说明
// Instant是与时区无关的时间，推荐用这个
// timestamp也是与时区无关的时间，毫秒
// LocalDateTime是本地时间，与时区有关
// 整个项目中，不推荐再使用别的时间对象
public class TimeUtil {
    public static final ZoneId ZONE_ID = ZoneId.systemDefault();
    private static TimeZone TIME_ZONE = TimeZone.getTimeZone(ZONE_ID);
    Date dt;

    // 获得当前时间
    public static Instant getInstantNow() {
        return Instant.now();
    }

    // 获得当前时间
    public static long getTimeStampNow() {
        return Instant.now().toEpochMilli();
    }

    // 获得本地时间
    public static LocalDateTime getLocalDateTimeNow() {
        return LocalDateTime.now();
    }

    // 转换到本地时间, ts为毫秒
    public static LocalDateTime toDateTime(long ts) {
        return LocalDateTime.ofInstant(Instant.ofEpochMilli(ts), ZONE_ID);
    }

    // 转换到本地时间
    public static LocalDateTime toDateTime(Instant instant) {
        return LocalDateTime.ofInstant(instant, ZONE_ID);
    }

    public static Date ldtToDate(LocalDateTime ldt) {
        return Date.from(ldt.atZone(ZONE_ID).toInstant());
    }

    public static LocalDateTime dateToLdt(Date date) {
        return toDateTime(date.getTime());
    }

    // 转换到毫秒时间
    public static long toTimeStamp(LocalDateTime dt) {
        return dt.atZone(ZONE_ID).toInstant().toEpochMilli();
    }

    // 转换到毫秒时间
    public static long toTimeStamp(Instant instant) {
        return instant.toEpochMilli();
    }

    // 转换到Instant
    public static Instant toInstant(LocalDateTime dt) {
        return dt.atZone(ZONE_ID).toInstant();
    }

    // 转换到Instant, ts为毫秒
    public static Instant toInstant(long ts) {
        return Instant.ofEpochMilli(ts);
    }

    // 是否在同一个月
    public static boolean isSameMonth(long ts1, long ts2) {
        LocalDateTime dt1 = toDateTime(ts1);
        LocalDateTime dt2 = toDateTime(ts2);
        return dt1.getYear() == dt2.getYear() && dt1.getMonthValue() == dt2.getMonthValue();
    }

    public static boolean isSameMonth(LocalDateTime dt1, LocalDateTime dt2) {
        return dt1.getYear() == dt2.getYear() && dt1.getMonthValue() == dt2.getMonthValue();
    }

    public static boolean isSameMonth(Instant ts1, Instant ts2) {
        LocalDateTime dt1 = toDateTime(ts1);
        LocalDateTime dt2 = toDateTime(ts2);
        return dt1.getYear() == dt2.getYear() && dt1.getMonthValue() == dt2.getMonthValue();
    }

    // 是否在同一天
    public static boolean isSameDay(long ts1, long ts2) {
        LocalDateTime dt1 = toDateTime(ts1);
        LocalDateTime dt2 = toDateTime(ts2);
        return dt1.getYear() == dt2.getYear() &&
                dt1.getMonthValue() == dt2.getMonthValue() &&
                dt1.getDayOfMonth() == dt2.getDayOfMonth();
    }

    public static boolean isSameDay(LocalDateTime dt1, LocalDateTime dt2) {
        return dt1.getYear() == dt2.getYear() &&
                dt1.getMonthValue() == dt2.getMonthValue() &&
                dt1.getDayOfMonth() == dt2.getDayOfMonth();
    }

    public static boolean isSameDay(Instant ts1, Instant ts2) {
        LocalDateTime dt1 = toDateTime(ts1);
        LocalDateTime dt2 = toDateTime(ts2);
        return dt1.getYear() == dt2.getYear() &&
                dt1.getMonthValue() == dt2.getMonthValue() &&
                dt1.getDayOfMonth() == dt2.getDayOfMonth();
    }

    public static int getYear(long timestamp) {
        return toDateTime(timestamp).getYear();
    }

    public static int getMonthOfYear(long timestamp) {
        return toDateTime(timestamp).getMonthValue();
    }

    public static int getDayOfMonth(long timestamp) {
        return toDateTime(timestamp).getDayOfMonth();
    }

    public static int getHour(long timestamp) {
        return toDateTime(timestamp).getHour();
    }

    public static int getMinute(long timestamp) {
        return toDateTime(timestamp).getMinute();
    }

    public static int getSecond(long timestamp) {
        return toDateTime(timestamp).getSecond();
    }

    public static int getDayOfYear(long timestamp) {
        return toDateTime(timestamp).getDayOfYear();
    }

    public static int getDayOfWeek(long timestamp) {
        return toDateTime(timestamp).getDayOfWeek().getValue();
    }

    public static boolean isMonday(long timestamp) {
        return toDateTime(timestamp).getDayOfWeek() == DayOfWeek.MONDAY;
    }

    private static DateTimeFormatter mDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static DateTimeFormatter mDateFormatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH");
    private static DateTimeFormatter mDateFormatter3 = DateTimeFormatter.ofPattern("yyyy-MM-dd-HH-mm-ss");
    private static DateTimeFormatter mDateFormatter4 = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static LocalDateTime stringToDateTime(String s) {
        if(s.matches("^\\d+-\\d+-\\d+ \\d+:\\d+:\\d+$")) {
            String[] parts = s.split("[\\s\\:\\-]+");
            assert(parts.length == 6);
            return LocalDateTime.of(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    Integer.parseInt(parts[3]),
                    Integer.parseInt(parts[4]),
                    Integer.parseInt(parts[5])
            );
        }
        else if(s.matches("^\\d+-\\d+-\\d+$")) {
            String[] parts = s.split("[\\s\\:\\-]+");
            assert(parts.length == 3);
            return LocalDateTime.of(
                    Integer.parseInt(parts[0]),
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]), 0, 0, 0
            );
        }
        else {
            assert(false);
            return LocalDateTime.now();
        }
    }

    public static long stringToTimeStamp(String s) {
        LocalDateTime dt = stringToDateTime(s);
        return toTimeStamp(dt);
    }

    public static Instant stringToTimeInstant(String s) {
        LocalDateTime dt = stringToDateTime(s);
        return toInstant(dt);
    }

    public static String dateTimeToString(LocalDateTime dt) {
        return dt.format(mDateFormatter);
    }

    public static String dateTimeToString2(LocalDateTime dt) {
        return dt.format(mDateFormatter2);
    }

    public static String dateTimeToString3(LocalDateTime dt) {
        return dt.format(mDateFormatter3);
    }

    public static String dateTimeToString4(LocalDateTime dt) {
        return dt.format(mDateFormatter4);
    }

    public static String timeStampToString(long ts) {
        LocalDateTime dt = toDateTime(ts);
        return dt.format(mDateFormatter);
    }

    public static String instantToString(Instant instant) {
        LocalDateTime dt = toDateTime(instant);
        return dt.format(mDateFormatter);
    }

    // 获得下一个特定小时的时间
    public static Instant getNextInstantForHour(Instant t, int hour) {
        long ts = getNextTimeStamp(toTimeStamp(t), hour);
        return toInstant(ts);
    }

    // 获得下一个特定小时的时间
    public static long getNextTimeStampForHour(long ts, int hour) {
        return getNextTimeStamp(ts, hour);
    }

    /**
     * 天数+1
     * @param timestamp
     * @param hour
     * @return
     */
    public static long getNextTimeStamp(long timestamp, int hour) {
        Calendar c = Calendar.getInstance(TimeUtil.TIME_ZONE);
        c.setTimeInMillis(timestamp);
        c = DateUtils.truncate(c, Calendar.HOUR_OF_DAY);
        if (c.get(Calendar.HOUR_OF_DAY) >= hour) {
            c.set(Calendar.HOUR_OF_DAY, hour);
            c.add(Calendar.DATE, 1);
        } else {
            c.set(Calendar.HOUR_OF_DAY, hour);
        }
        return c.getTimeInMillis();
    }

    // 时间截断到天
    public static long truncateTimeStamp(long timestamp) {
        Calendar c = Calendar.getInstance(TimeUtil.TIME_ZONE);
        c.setTimeInMillis(timestamp);
        c = DateUtils.truncate(c, Calendar.DAY_OF_MONTH);
        return c.getTimeInMillis();
    }

    public static long getSameDayBeginTime(long ts, int targetHour) {
        return getSameDayEndTime(ts, targetHour) - 24 * 3600 * 1000;
    }

    public static Instant getSameDayBeginTime(Instant instant, int targetHour) {
        return getSameDayEndTime(instant, targetHour).plusSeconds(-24 * 3600);
    }

    public static LocalDateTime getSameDayBeginTime(LocalDateTime dt, int targetHour) {
        return getSameDayEndTime(dt, targetHour).plusDays(-1);
    }

    public static long getSameDayEndTime(long ts, int targetHour) {
        LocalDateTime dt = toDateTime(ts);
        dt = getSameDayEndTime(dt, targetHour);
        return toTimeStamp(dt);
    }

    public static Instant getSameDayEndTime(Instant instant, int targetHour) {
        LocalDateTime dt = toDateTime(instant);
        dt = getSameDayEndTime(dt, targetHour);
        return toInstant(dt);
    }

    public static LocalDateTime getSameDayEndTime(LocalDateTime dt, int targetHour) {
        if(dt.getHour() >= targetHour) {
            dt = LocalDateTime.of(dt.getYear(), dt.getMonth(), dt.getDayOfMonth(), targetHour, 0, 0);
            dt = dt.plusDays(1);
            return dt;
        }
        else {
            dt = LocalDateTime.of(dt.getYear(), dt.getMonth(), dt.getDayOfMonth(), targetHour, 0, 0);
            return dt;
        }
    }

    public static long getSameWeekBeginTime(long ts, int targetWday) {
        return getSameWeekEndTime(ts, targetWday) - 7 * 24 * 3600 * 1000;
    }

    public static Instant getSameWeekBeginTime(Instant instant, int targetWday) {
        return getSameWeekEndTime(instant, targetWday).plusSeconds(-7 * 24 * 3600);
    }

    public static LocalDateTime getSameWeekBeginTime(LocalDateTime dt, int targetWday) {
        return getSameWeekEndTime(dt, targetWday).plusDays(-7);
    }

    public static long getSameWeekEndTime(long ts, int targetWday) {
        LocalDateTime dt = toDateTime(ts);
        dt = getSameWeekEndTime(dt, targetWday);
        return toTimeStamp(dt);
    }

    public static Instant getSameWeekEndTime(Instant instant, int targetWday) {
        LocalDateTime dt = toDateTime(instant);
        dt = getSameWeekEndTime(dt, targetWday);
        return toInstant(dt);
    }

    public static LocalDateTime getSameWeekEndTime(LocalDateTime dt, int targetWday) {
        int wday = dt.getDayOfWeek().getValue();
        if(dt.getDayOfWeek().getValue() >= targetWday) {
            dt = LocalDateTime.of(dt.getYear(), dt.getMonth(), dt.getDayOfMonth(), 0, 0, 0);
            dt = dt.plusDays(7 - wday + targetWday);
            return dt;
        }
        else {
            dt = LocalDateTime.of(dt.getYear(), dt.getMonth(), dt.getDayOfMonth(), 0, 0, 0);
            dt = dt.plusDays(wday + targetWday);
            return dt;
        }
    }

    /*
    public static long getRawOffset() {
        return TimeUtil.TIME_ZONE.getRawOffset();
    }

    public static long getMillisDiff() {
        return TimeUtil.TIME_ZONE.getRawOffset() - TimeZone.getDefault().getRawOffset();
    }

    public static int getHourDiff(long start, long end) {
        return (int) ((end - start) / 3600000L);
    }

    public static long getDayDiff(long start, long end) {
        return getDayDiff(start, end, TimeUtil.TIME_ZONE);
    }

    private static long getDayDiff(long start, long end, TimeZone timeZone) {
        long d1 = (start + timeZone.getRawOffset()) / 86400000L;
        long d2 = (end + timeZone.getRawOffset()) / 86400000L;
        return (int) (d2 - d1);
    }
     */

    /*
    public static int getHourOfDay(long timestamp) {
        Calendar c = Calendar.getInstance(TimeUtil.TIME_ZONE);
        c.setTimeInMillis(timestamp);
        return c.get(Calendar.HOUR_OF_DAY);
    }

    public static int getDayOfWeek(long timestamp) {
        Calendar c = Calendar.getInstance(TimeUtil.TIME_ZONE);
        c.setTimeInMillis(timestamp);
        return c.get(Calendar.DAY_OF_WEEK);
    }

    public static int getDayOfWeekNormal(long timestamp) {
        Calendar c = Calendar.getInstance(TimeUtil.TIME_ZONE);
        c.setTimeInMillis(timestamp);
        int day = c.get(7);
        if (day == 1) {
            day = 7;
        } else {
            --day;
        }
        return day;
    }
     */

    /*
    public static int getSecond(long timestamp) {
        Calendar c = Calendar.getInstance(TimeUtil.TIME_ZONE);
        c.setTimeInMillis(timestamp);
        return c.get(Calendar.SECOND);
    }

    public static int getMinute(long timestamp) {
        Calendar c = Calendar.getInstance(TimeUtil.TIME_ZONE);
        c.setTimeInMillis(timestamp);
        return c.get(Calendar.MINUTE);
    }

    public static int getMonthOfYear(long timestamp) {
        Calendar cal = Calendar.getInstance(TimeUtil.TIME_ZONE);
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.MONTH) + 1;
    }

    public static int getDayOfMonth(long timestamp) {
        Calendar cal = Calendar.getInstance(TimeUtil.TIME_ZONE);
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.DAY_OF_MONTH);
    }

    public static int getYear(long timestamp) {
        Calendar cal = Calendar.getInstance(TimeUtil.TIME_ZONE);
        cal.setTimeInMillis(timestamp);
        return cal.get(Calendar.YEAR);
    }

    public static final Pattern patTime = Pattern.compile("^([0-9]+).([0-9]+).([0-9]+).([0-9]+).([0-9]+)$");

    public static final Date setTime(int year, int month, int day, int hourOfDay, int minute, int second) {
        Calendar c = Calendar.getInstance();
        if (year > 0) {
            c.set(Calendar.YEAR, year);
        }
        if (month > 0) {
            c.set(Calendar.MONTH, month - 1);
        }
        if (day > 0) {
            c.set(Calendar.DAY_OF_MONTH, day);
        }
        if (hourOfDay > -1) {
            c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        }
        if (minute > -1) {
            c.set(Calendar.MINUTE, minute);
        }
        if (second > -1) {
            c.set(Calendar.SECOND, second);
        }
        return c.getTime();
    }
    */

    /**
     * 时间转换
     * yyyy.MM.dd.HH.mm
     * @param strTime
     * @return
     */
    /*
    public static final Date parseTime(final String strTime) {
        // 现在创建 matcher 对象
        Matcher m = patTime.matcher(strTime);
        if (m.find()) {
            int year =  Integer.parseInt(m.group(1));
            int month = Integer.parseInt(m.group(2));
            int day = Integer.parseInt(m.group(3));
            int hour = Integer.parseInt(m.group(4));
            int min = Integer.parseInt(m.group(5));
            return setTime(year, month, day, hour, min, 0);
        }
        return setTime(1970, 1, 1, 0, 0, 0);
    }
     */

    /**
     * 说明：将时间转换成字符串
     * 参数：日期时间(整数)
     * 返回：字符串，格式如(年-月-日 小时:分:秒)
     * @param date
     * @return
     */
    /*
    public static String dateTimeToString(Date date) {
        int year = getYear(date.getTime());
        int month = getMonthOfYear(date.getTime());
        int dayOfMonth = getDayOfMonth(date.getTime());
        int hour = getHourOfDay(date.getTime());
        int minute = getMinute(date.getTime());
        int second = getSecond(date.getTime());
        return year + "-" + month + "-" + dayOfMonth + " " + hour + ":" + minute + ":" + second;
    }
     */
}
