package net.nightvision.plugin.utils;

public class LabelUtils {

    /**
     * Inserts "<br>" into the input string every `everyNSteps` characters.
     * If the string length is an exact multiple of everyNSteps, a trailing "<br>"
     * will still be appended after the last chunk.
     *
     * @param msg          The original text.
     * @param everyNSteps  How many characters between each "<br>".
     * @return             A new string with "<br>" inserted every everyNSteps chars.
     */
    public static String addHtmlBreakLinesToLabel(String msg, int everyNSteps) {
        if (msg == null || msg.isEmpty() || everyNSteps <= 0) {
            // Nothing to split or invalid step size → just return original
            return msg;
        }

        String br = "<br>";
        StringBuilder sb = new StringBuilder(msg.length() + br.length() * ((msg.length() / everyNSteps) + 1));

        int len = msg.length();
        for (int pos = 0; pos < len; pos += everyNSteps) {
            int end = Math.min(pos + everyNSteps, len);
            sb.append(msg, pos, end);
            sb.append(br);
        }

        return sb.toString();
    }
}

