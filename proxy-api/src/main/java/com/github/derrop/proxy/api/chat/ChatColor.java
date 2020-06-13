/*
 * MIT License
 *
 * Copyright (c) derrop and derklaro
 * Copyright (c) contributors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.github.derrop.proxy.api.chat;

import lombok.Getter;
import org.apache.commons.lang3.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Simplistic enumeration of all supported color values for chat.
 */
public enum ChatColor {

    /**
     * Represents black.
     */
    BLACK('0', "black"),
    /**
     * Represents dark blue.
     */
    DARK_BLUE('1', "dark_blue"),
    /**
     * Represents dark green.
     */
    DARK_GREEN('2', "dark_green"),
    /**
     * Represents dark blue (aqua).
     */
    DARK_AQUA('3', "dark_aqua"),
    /**
     * Represents dark red.
     */
    DARK_RED('4', "dark_red"),
    /**
     * Represents dark purple.
     */
    DARK_PURPLE('5', "dark_purple"),
    /**
     * Represents gold.
     */
    GOLD('6', "gold"),
    /**
     * Represents gray.
     */
    GRAY('7', "gray"),
    /**
     * Represents dark gray.
     */
    DARK_GRAY('8', "dark_gray"),
    /**
     * Represents blue.
     */
    BLUE('9', "blue"),
    /**
     * Represents green.
     */
    GREEN('a', "green"),
    /**
     * Represents aqua.
     */
    AQUA('b', "aqua"),
    /**
     * Represents red.
     */
    RED('c', "red"),
    /**
     * Represents light purple.
     */
    LIGHT_PURPLE('d', "light_purple"),
    /**
     * Represents yellow.
     */
    YELLOW('e', "yellow"),
    /**
     * Represents white.
     */
    WHITE('f', "white"),
    /**
     * Represents magical characters that change around randomly.
     */
    MAGIC('k', "obfuscated", true),
    /**
     * Makes the text bold.
     */
    BOLD('l', "bold", true),
    /**
     * Makes a line appear through the text.
     */
    STRIKETHROUGH('m', "strikethrough", true),
    /**
     * Makes the text appear underlined.
     */
    UNDERLINE('n', "underline", true),
    /**
     * Makes the text italic.
     */
    ITALIC('o', "italic", true),
    /**
     * Resets all previous chat colors or formats.
     */
    RESET('r', "reset", true);
    /**
     * The special character which prefixes all chat colour codes. Use this if
     * you need to dynamically convert colour codes from your custom format.
     */
    public static final char COLOR_CHAR = '\u00A7';
    public static final String ALL_CODES = "0123456789AaBbCcDdEeFfKkLlMmNnOoRr";
    /**
     * Pattern to remove all colour codes.
     */
    public static final Pattern STRIP_COLOR_PATTERN = Pattern.compile("(?i)" + COLOR_CHAR + "[0-9A-FK-OR]");
    /**
     * Colour instances keyed by their active character.
     */
    private static final Map<Character, ChatColor> BY_CHAR = new HashMap<Character, ChatColor>();
    /**
     * The code appended to {@link #COLOR_CHAR} to make usable colour.
     */
    private final char code;
    /**
     * This colour's colour char prefixed by the {@link #COLOR_CHAR}.
     */
    private final String toString;
    @Getter
    private final String name;

    private final boolean isColor;

    static {
        for (ChatColor colour : values()) {
            BY_CHAR.put(colour.code, colour);
        }
    }

    ChatColor(char code, String name) {
        this(code, name, false);
    }

    ChatColor(char code, String name, boolean noColor) {
        this.code = code;
        this.name = name;
        this.toString = new String(new char[]{COLOR_CHAR, code});
        this.isColor = !noColor;
    }

    public boolean isColor() {
        return this.isColor;
    }

    @Override
    public String toString() {
        return toString;
    }

    /**
     * Strips the given message of all color codes
     *
     * @param input String to strip of color
     * @return A copy of the input string, without any coloring
     */
    public static String stripColor(final String input) {
        if (input == null) {
            return null;
        }

        return STRIP_COLOR_PATTERN.matcher(input).replaceAll("");
    }

    /**
     * Gets the ChatColors used at the end of the given input string.
     *
     * @param input Input string to retrieve the colors from.
     * @return Any remaining ChatColors to pass onto the next line.
     */
    @NotNull
    public static String getLastColors(@NotNull String input) {
        Validate.notNull(input, "Cannot get last colors from null text");

        StringBuilder result = new StringBuilder();
        int length = input.length();

        // Search backwards from the end as it is faster
        for (int index = length - 1; index > -1; index--) {
            char section = input.charAt(index);
            if (section == COLOR_CHAR && index < length - 1) {
                char c = input.charAt(index + 1);
                ChatColor color = getByChar(c);

                if (color != null) {
                    result.insert(0, color.toString());

                    // Once we find a color or reset we can stop searching
                    if (color.isColor() || color.equals(RESET)) {
                        break;
                    }
                }
            }
        }

        return result.toString();
    }

    public static ChatColor getLastColor(final String input) {
        int i = input.lastIndexOf(COLOR_CHAR);
        if (i == -1 || i == input.length() - 2) {
            return null;
        }
        return getByChar(input.charAt(i + 1));
    }

    public static String translateAlternateColorCodes(char altColorChar, String textToTranslate) {
        char[] b = textToTranslate.toCharArray();
        for (int i = 0; i < b.length - 1; i++) {
            if (b[i] == altColorChar && ALL_CODES.indexOf(b[i + 1]) > -1) {
                b[i] = ChatColor.COLOR_CHAR;
                b[i + 1] = Character.toLowerCase(b[i + 1]);
            }
        }
        return new String(b);
    }

    /**
     * Get the colour represented by the specified code.
     *
     * @param code the code to search for
     * @return the mapped colour, or null if non exists
     */
    public static ChatColor getByChar(char code) {
        return BY_CHAR.get(code);
    }
}
