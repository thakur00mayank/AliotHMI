package io.datonis.aliothmi;

/**
 * Created by mayank on 7/5/17.
 */

public class Util {
    /**
     * Give the the time diff in formated string.
     * @param timeDiffInSec
     * @param formatter 1-days, 2-hours, 4-minutes, 8-seconds. Add the values which you want and pass it.
     * @return String with the formatted output.
     */
    public static String getTimeDiffStr(long timeDiffInSec, int formatter) {
        long diff[] = new long[] { 0, 0, 0, 0 };
        diff[3] = (timeDiffInSec >= 60 ? timeDiffInSec % 60 : timeDiffInSec);
        diff[2] = (timeDiffInSec = (timeDiffInSec / 60)) >= 60 ? timeDiffInSec % 60 : timeDiffInSec;
        diff[1] = (timeDiffInSec = (timeDiffInSec / 60)) >= 24 ? timeDiffInSec % 24 : timeDiffInSec;
        diff[0] = (timeDiffInSec = (timeDiffInSec / 24));
        String timeDiffStr = "";
        if(diff[0] > 0 && (formatter & 1) == 1) {
            timeDiffStr += diff[0] + ((diff[0] > 1) ? "Days " : "Day ");
        }
        if(diff[1] > 0 && ((formatter >> 1) & 1) == 1) {
            timeDiffStr += diff[1] + ((diff[1] > 1) ? "h " : "h ");
        }
        if(diff[2] > 0 && ((formatter >> 2) & 1) == 1) {
            timeDiffStr += diff[2] + ((diff[2] > 1) ? "m " : "m ");
        }
        if(diff[3] > 0 && ((formatter >> 3) & 1) == 1) {
            timeDiffStr += diff[3] + ((diff[3] > 1) ? "s " : "s ");
        }
        if (timeDiffStr.isEmpty()) {
            timeDiffStr = "a moment ";
        }
        timeDiffStr += "ago";
        return timeDiffStr;
    }
}
