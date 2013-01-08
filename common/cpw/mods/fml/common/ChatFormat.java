package cpw.mods.fml.common;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * All supported format values for chat
 */
public enum ChatFormat {
    BLACK('0', 0x000000),
    DARK_BLUE('1', 0x0000AA),
    DARK_GREEN('2', 0x00AA00),
    DARK_AQUA('3', 0x00AAAA),
    DARK_RED('4', 0xAA0000),
    DARK_PURPLE('5', 0xAA00AA),
    GOLD('6', 0xFFAA00),
    GRAY('7', 0xAAAAAA),
    DARK_GRAY('8', 0x555555),
    BLUE('9', 0x5555FF),
    GREEN('a', 0x55FF55),
    AQUA('b', 0x55FFFF),
    RED('c', 0xFF5555),
    LIGHT_PURPLE('d', 0xFF55FF),
    YELLOW('e', 0xFFFF55),
    WHITE('f', 0xFFFFFF),

    /**
     * Represents magical characters that change around randomly
     */
    MAGIC('k'),
    BOLD('l'),
    STRIKETHROUGH('m'),
    UNDERLINE('n'),
    ITALIC('o'),

    /**
     * Resets all previous chat colours or formats.
     */
    RESET('r');

    private static final char FORMAT_CHAR = '\u00A7';
    private static final Pattern STRIP_FORMAT_PATTERN = Pattern.compile("(?i)" + String.valueOf(FORMAT_CHAR) + "[0-9A-FK-OR]");

    private final char charCode;
    private final int hexColour;
    private final String toString;

    private static final Map<Character, ChatFormat> BY_CHAR = new HashMap<Character, ChatFormat>();

    static {
        for (ChatFormat colour : values()) {
            BY_CHAR.put(colour.charCode, colour);
        }
    }

    private ChatFormat(char charCode, int hexColour) {
        this.charCode = charCode;
        this.hexColour = hexColour;
        this.toString = new String(new char[] { FORMAT_CHAR, charCode });
    }

    private ChatFormat(char charCode) {
        this(charCode, -1);
    }

    /**
     * Gets the char value associated with this format code
     * 
     * @return A char value of this format code
     */
    public char getChar() {
        return charCode;
    }

    /**
     * Gets the hexadecimal colour value for this format code
     * 
     * @return The hexadecimal colour value or -1 if this is a format code
     */
    public int getColour() {
        return hexColour;
    }

    /**
     * Checks if this code is a format code as opposed to a colour code.
     */
    public boolean isFormat() {
        return hexColour == -1;
    }

    /**
     * Checks if this code is a colour code as opposed to a format code.
     */
    public boolean isColour() {
        return hexColour != -1;
    }

    @Override
    public String toString() {
        return toString;
    }

    /**
     * Gets the format represented by the specified format character code
     * 
     * @param code - char code to check
     * @return ChatFormat matching this character code or null if invalid code
     */
    public static ChatFormat getByChar(char code) {
        return BY_CHAR.get(code);
    }

    /**
     * Gets the format represented by the specified format character code
     * 
     * @param code - char code to check
     * @return ChatFormat matching this character code or null if invalid code
     */
    public static ChatFormat getByChar(String code) {
        if (code == null || code.length() != 1) {
            return null;
        }

        return BY_CHAR.get(code.charAt(0));
    }

    /**
     * Strips the given message of all format codes
     * 
     * @param input String to strip of formatting
     * @return A copy of the input string, without any formatting
     */
    public static String stripFormat(final String input) {
        if (input == null) {
            return null;
        }

        return STRIP_FORMAT_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Translates a string using an alternate format character into a string that uses the internal ChatFormat.FORMAT_CHAR character. The alternate
     * format character will only be replaced if it is immediately followed by a valid format charCode.
     * 
     * @param altFormatChar - The alternate format character to replace. e.g.: '&'
     * @param textToTranslate - Text containing the alternate format character.
     * @return Text containing proper formatting.
     */
    public static String translateAlternateFormatChar(char altFormatChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altFormatChar && "0123456789AaBbCcDdEeFfKkLlMmNnOoRr".indexOf(b[i + 1]) > -1) {
                b[i] = ChatFormat.FORMAT_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    /**
     * Gets the ChatFormats used at the end of the given input string.
     * 
     * @param input - Input string to retrieve the format from.
     * @return Any remaining ChatFormats to pass onto the next line.
     */
    public static String getLastFormat(String input) {
        String result = "";
        int length = input.length();

        // Search backwards from the end as it is faster
        for (int index = length - 1; index > -1; index--) {
            char section = input.charAt(index);
            if (section == FORMAT_CHAR && index < length - 1) {
                char c = input.charAt(index + 1);
                ChatFormat cf = getByChar(c);

                if (cf != null) {
                    result = cf.toString() + result;

                    // Once we find a color or reset we can stop searching
                    if (cf.isColour() || cf.equals(RESET)) {
                        break;
                    }
                }
            }
        }

        return result;
    }

}