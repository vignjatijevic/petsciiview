package org.garageapps.android.petsciiview;

/**
 * PETSCIIFormatter
 * <p>
 * Constants for {@link PETSCIIView#printFormattedText}
 */
public class PETSCIIFormatter {

    // supported formatters

    public static final String CLR = "CLR"; // clear screen
    public static final String HOM = "HOM"; // move cursor to home (upper left corner)
    public static final String CUP = "CUP"; // move cursor up
    public static final String CDN = "CDN"; // move cursor down
    public static final String CLT = "CLT"; // move cursor left
    public static final String CRT = "CRT"; // move cursor right
    public static final String COL = "COL"; // set color
    public static final String RON = "RON"; // set reverse on
    public static final String ROF = "ROF"; // set reverse off

    // parsing error messages

    public static final String UNKNOWN_FORMATTER_ERROR = "UNKNOWN FORMATTER ERROR";
    public static final String NUMBER_FORMAT_ERROR = "NUMBER FORMAT ERROR";
    public static final String PARSING_ERROR = "PARSING ERROR";
}