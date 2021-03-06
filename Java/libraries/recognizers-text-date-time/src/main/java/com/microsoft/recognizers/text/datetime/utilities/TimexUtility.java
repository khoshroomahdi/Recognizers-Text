package com.microsoft.recognizers.text.datetime.utilities;

import com.google.common.collect.ImmutableMap;
import com.microsoft.recognizers.text.datetime.Constants;
import com.microsoft.recognizers.text.datetime.DatePeriodTimexType;
import com.microsoft.recognizers.text.utilities.StringUtility;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TimexUtility {
    private static final HashMap<DatePeriodTimexType, String> DatePeriodTimexTypeToTimexSuffix = new HashMap<DatePeriodTimexType, String>() {
        {
            put(DatePeriodTimexType.ByDay, Constants.TimexDay);
            put(DatePeriodTimexType.ByWeek, Constants.TimexWeek);
            put(DatePeriodTimexType.ByMonth, Constants.TimexMonth);
            put(DatePeriodTimexType.ByYear, Constants.TimexYear);
        }
    };

    public static String generateCompoundDurationTimex(Map<String, String> unitToTimexComponents, ImmutableMap<String, Long> unitValueMap) {
        List<String> unitList = new ArrayList<>(unitToTimexComponents.keySet());
        unitList.sort((x, y) -> unitValueMap.get(x) < unitValueMap.get(y) ? 1 : -1);
        boolean isTimeDurationAlreadyExist = false;
        StringBuilder timexBuilder = new StringBuilder(Constants.GeneralPeriodPrefix);

        for (String unitKey : unitList) {
            String timexComponent = unitToTimexComponents.get(unitKey);

            // The Time Duration component occurs first time
            if (!isTimeDurationAlreadyExist && isTimeDurationTimex(timexComponent)) {
                timexBuilder.append(Constants.TimeTimexPrefix);
                timexBuilder.append(getDurationTimexWithoutPrefix(timexComponent));
                isTimeDurationAlreadyExist = true;
            } else {
                timexBuilder.append(getDurationTimexWithoutPrefix(timexComponent));
            }
        }
        return timexBuilder.toString();
    }

    private static boolean isTimeDurationTimex(String timex) {
        return timex.startsWith(Constants.GeneralPeriodPrefix + Constants.TimeTimexPrefix);
    }

    private static String getDurationTimexWithoutPrefix(String timex) {
        // Remove "PT" prefix for TimeDuration, Remove "P" prefix for DateDuration
        return timex.substring(isTimeDurationTimex(timex) ? 2 : 1);
    }

    public static String generateDatePeriodTimex(LocalDateTime begin, LocalDateTime end, DatePeriodTimexType timexType) {

        return generateDatePeriodTimex(begin, end, timexType, null, null);
    }

    public static String generateDatePeriodTimex(LocalDateTime begin, LocalDateTime end, DatePeriodTimexType timexType,
                                                 LocalDateTime alternativeBegin, LocalDateTime alternativeEnd) {

        Boolean equalDurationLength;
        if (alternativeBegin == null || alternativeEnd == null) {
            equalDurationLength = true;
        } else {
            equalDurationLength = Duration.between(begin, end).equals(Duration.between(alternativeBegin, alternativeEnd));
        }

        String unitCount = "XX";

        if (equalDurationLength) {
            switch (timexType) {
                case ByDay:
                    unitCount = Long.toString(ChronoUnit.DAYS.between(begin, end));
                    break;
                case ByWeek:
                    unitCount = Long.toString(ChronoUnit.WEEKS.between(begin, end));
                    break;
                case ByMonth:
                    unitCount = Long.toString(ChronoUnit.MONTHS.between(begin, end));
                    break;
                default:
                    unitCount = new BigDecimal((end.getYear() - begin.getYear()) + (end.getMonthValue() - begin.getMonthValue()) / 12.0).stripTrailingZeros().toString();
            }
        }

        String datePeriodTimex = "P" + unitCount + DatePeriodTimexTypeToTimexSuffix.get(timexType);
        return "(" + DateTimeFormatUtil.luisDate(begin, alternativeBegin) + "," + DateTimeFormatUtil.luisDate(end, alternativeEnd) + "," + datePeriodTimex + ")";
    }

    public static String generateWeekTimex() {
        return generateWeekTimex(null);
    }

    public static String generateWeekTimex(LocalDateTime monday) {

        if (monday == null) {
            return Constants.TimexFuzzyYear + Constants.DateTimexConnector + Constants.TimexFuzzyWeek;
        } else {
            return DateTimeFormatUtil.toIsoWeekTimex(monday);
        }
    }

    public static String generateWeekTimex(int weekNum) {
        return "W" + String.format("%02d", weekNum);
    }

    public static String generateWeekendTimex() {
        return generateWeekendTimex(null);
    }

    public static String generateWeekendTimex(LocalDateTime date) {
        if (date == null) {
            return Constants.TimexFuzzyYear + Constants.DateTimexConnector + Constants.TimexFuzzyWeek + Constants.DateTimexConnector + Constants.TimexWeekend;
        } else {
            return DateTimeFormatUtil.toIsoWeekTimex(date) + Constants.DateTimexConnector + Constants.TimexWeekend;
        }
    }

    public static String generateMonthTimex() {
        return generateMonthTimex(null);
    }

    public static String generateMonthTimex(LocalDateTime date) {
        if (date == null) {
            return Constants.TimexFuzzyYear + Constants.DateTimexConnector + Constants.TimexFuzzyMonth;
        } else {
            return String.format("%04d-%02d", date.getYear(), date.getMonthValue());
        }
    }

    public static String generateYearTimex() {
        return Constants.TimexFuzzyYear;
    }

    public static String generateYearTimex(LocalDateTime date) {
        return String.format("%04d", date.getYear());
    }

    public static String generateDurationTimex(double number, String unitStr, boolean isLessThanDay) {
        if (!Constants.TimexBusinessDay.equals(unitStr)) {
            if (Constants.DECADE_UNIT.equals(unitStr)) {
                number = number * 10;
                unitStr = Constants.TimexYear;

            } else {
                unitStr = unitStr.substring(0, 1);
            }
        }

        return String.format("%s%s%s%s",
                Constants.GeneralPeriodPrefix,
                isLessThanDay ? Constants.TimeTimexPrefix : "",
                StringUtility.format(number),
                unitStr);
    }

    public static DatePeriodTimexType getDatePeriodTimexType(String durationTimex) {
        DatePeriodTimexType result;

        String minimumUnit = durationTimex.substring(durationTimex.length() - 1);

        switch (minimumUnit) {
            case Constants.TimexYear:
                result = DatePeriodTimexType.ByYear;
                break;
            case Constants.TimexMonth:
                result = DatePeriodTimexType.ByMonth;
                break;
            case Constants.TimexWeek:
                result = DatePeriodTimexType.ByWeek;
                break;
            default:
                result = DatePeriodTimexType.ByDay;
                break;
        }

        return result;
    }

    public static LocalDateTime offsetDateObject(LocalDateTime date, int offset, DatePeriodTimexType timexType) {
        LocalDateTime result;

        switch (timexType) {
            case ByYear:
                result = date.plusYears(offset);
                break;
            case ByMonth:
                result = date.plusMonths(offset);
                break;
            case ByWeek:
                result = date.plusDays(7 * offset);
                break;
            case ByDay:
                result = date.plusDays(offset);
                break;
            default:
                result = date;
                break;
        }

        return result;
    }

    public static TimeOfDayResolutionResult parseTimeOfDay(String tod) {
        switch (tod) {
            case Constants.EarlyMorning:
                return new TimeOfDayResolutionResult(Constants.EarlyMorning, 4, 8, 0);
            case Constants.Morning:
                return new TimeOfDayResolutionResult(Constants.Morning, 8, 12, 0);
            case Constants.Afternoon:
                return new TimeOfDayResolutionResult(Constants.Afternoon, 12, 16, 0);
            case Constants.Evening:
                return new TimeOfDayResolutionResult(Constants.Evening, 16, 20, 0);
            case Constants.Daytime:
                return new TimeOfDayResolutionResult(Constants.Daytime, 8, 18, 0);
            case Constants.BusinessHour:
                return new TimeOfDayResolutionResult(Constants.BusinessHour, 8, 18, 0);
            case Constants.Night:
                return new TimeOfDayResolutionResult(Constants.Night, 20, 23, 59);
            default:
                return new TimeOfDayResolutionResult();
        }
    }

    public static String combineDateAndTimeTimex(String dateTimex, String timeTimex) {
        return dateTimex + timeTimex;
    }

    public static String generateWeekOfYearTimex(int year, int weekNum) {
        String weekTimex = generateWeekTimex(weekNum);
        String yearTimex = DateTimeFormatUtil.luisDate(year);

        return yearTimex + "-" + weekTimex;
    }

    public static String generateWeekOfMonthTimex(int year, int month, int weekNum) {
        String weekTimex = generateWeekTimex(weekNum);
        String monthTimex = DateTimeFormatUtil.luisDate(year, month);

        return monthTimex + "-" + weekTimex;
    }

    public static String generateDateTimePeriodTimex(String beginTimex, String endTimex, String durationTimex) {
        return "(" + beginTimex + "," + endTimex + "," + durationTimex + ")";
    }

    public static RangeTimexComponents getRangeTimexComponents(String rangeTimex) {
        rangeTimex = rangeTimex.replace("(", "").replace(")", "");
        String[] components = rangeTimex.split(",");
        RangeTimexComponents result = new RangeTimexComponents();

        if (components.length == 3) {
            result.beginTimex = components[0];
            result.endTimex = components[1];
            result.durationTimex = components[2];
            result.isValid = true;
        }

        return result;
    }

    public static boolean isRangeTimex(String timex) {
        return !StringUtility.isNullOrEmpty(timex) && timex.startsWith("(");
    }

    public static String setTimexWithContext(String timex, DateContext context) {
        return timex.replace(Constants.TimexFuzzyYear, String.format("%04d", context.getYear()));
    }
}
