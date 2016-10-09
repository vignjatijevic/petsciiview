package org.garageapps.android.petsciiview;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * PETSCIIView
 *
 * @author Vladimir Ignjatijevic
 */
public class PETSCIIView extends View implements View.OnTouchListener {

    private static final String ASSETS_FONT_PATH = "fonts/C64_Pro_Mono-STYLE.ttf";

    /**
     * PETSCIIListener
     */
    public interface PETSCIIListener {

        /**
         * On click
         *
         * @param action
         *         action event
         * @param x
         *         x position
         * @param y
         *         y position
         */
        void onClick(int action, int x, int y);
    }

    /**
     * Attribute defaults
     */
    private int screenWidth = 40;
    private int screenHeight = 25;
    private int fontSize = 16;
    private int borderSizeLeft = 4 * fontSize;
    private int borderSizeTop = 4 * fontSize + fontSize / 4;
    private int borderSizeRight = 4 * fontSize;
    private int borderSizeBottom = 4 * fontSize + fontSize / 4;
    private int borderColor = 14;
    private int backgroundColor = 6;
    private int cursorColor = 14;

    /**
     * Listener
     */
    private PETSCIIListener listener;

    /**
     * View members
     */
    private Paint borderPaint;
    private Paint backgroundPaint;
    private Paint[] colorsPaint;
    private char[] lineBuffer;
    private char[] screenRam;
    private boolean screenRamEnabled;
    private int[] colorRam;
    private boolean colorRamEnabled;
    private Typeface textTypeface;
    private Rect textBounds;
    private int textHeight;

    /**
     * Constructor
     *
     * @param context
     *         - activity context
     */
    public PETSCIIView(Context context) {
        this(context, null);
    }

    /**
     * Constructor
     *
     * @param context
     *         - activity context
     * @param attrs
     *         - view attributes
     */
    public PETSCIIView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    /**
     * Constructor
     *
     * @param context
     *         - activity context
     * @param attrs
     *         - view attributes
     * @param defStyleAttr
     *         - view defStyle attribute
     */
    public PETSCIIView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        // obtain attributes
        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.PETSCIIView, 0, 0);

        // read attributes
        screenWidth = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrScreenWidth, screenWidth);
        screenHeight = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrScreenHeight, screenHeight);
        fontSize = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrFontSize, fontSize);
        borderSizeLeft = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrBorderSizeLeft, 4 * fontSize);
        borderSizeTop = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrBorderSizeTop, 4 * fontSize + fontSize / 4);
        borderSizeRight = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrBorderSizeRight, 4 * fontSize);
        borderSizeBottom = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrBorderSizeBottom, 4 * fontSize + fontSize / 4);
        borderColor = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrBorderColor, borderColor);
        backgroundColor = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrBackgroundColor, backgroundColor);
        cursorColor = typedArray.getInteger(R.styleable.PETSCIIView_pet_attrCursorColor, cursorColor);
        boolean testPicture = typedArray.getBoolean(R.styleable.PETSCIIView_pet_attrTestPicture, false);

        // recycle attributes
        typedArray.recycle();

        // initialize view
        initView();

        // reset buffers to default values
        fillWithChar(' ');
        fillWithColor(14);

        // print test picture if needed
        if (testPicture) {
            printTestPicture();
        }

        // attach on touch listener
        setOnTouchListener(this);

        // refresh view
        invalidate();
    }

    /**
     * Initialize view <p><i>Must be called on every screen or font size update</i></p>
     */
    private void initView() {

        // allocate objects
        borderPaint = new Paint();
        backgroundPaint = new Paint();
        colorsPaint = new Paint[PETSCIIColors.C64.length];
        lineBuffer = new char[screenWidth];
        screenRam = new char[screenWidth * screenHeight];
        colorRam = new int[screenWidth * screenHeight];
        textBounds = new Rect();

        // allocate colors
        for (int i = 0; i < colorsPaint.length; i++) {
            colorsPaint[i] = new Paint();
            colorsPaint[i].setTextSize(fontSize);
            colorsPaint[i].setTypeface(getTextTypeface());
            colorsPaint[i].setColor(Color.parseColor(PETSCIIColors.C64[i]));
        }

        // get text height from the highest available character in charset (reversed space)
        colorsPaint[0].getTextBounds(String.valueOf(PETSCIIChars.UPPERCASE[160]), 0, 1, textBounds);
        textHeight = textBounds.height();

        // set colors
        setBorderColor(borderColor);
        setBkgColor(backgroundColor);
        setCursorColor(cursorColor);

        // enable both buffers
        setScreenRamEnabled(true);
        setColorRamEnabled(true);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        // set view dimensions depending on screen, font and border sizes
        setMeasuredDimension(getViewWidth(), getViewHeight());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        // save canvas
        canvas.save();

        // render border
        canvas.drawRect(0, 0, getViewWidth(), getViewHeight(), borderPaint);

        if (screenRamEnabled) {

            // render background
            canvas.drawRect(borderSizeLeft, borderSizeTop, getViewWidth() - borderSizeRight, getViewHeight() - borderSizeBottom,
                    backgroundPaint);

            // render screen
            for (int y = 0; y < screenHeight; y++) {

                int currentIndex = 0;
                int currentColor = 0;
                int lastColor = colorRam[y * screenWidth];
                int copyStart = 0;
                int copyLength = 0;

                // make a copy of the current line
                System.arraycopy(screenRam, y * screenWidth, lineBuffer, 0, screenWidth);

                while (currentIndex != screenWidth) {

                    copyLength++;

                    // get current color from the color ram (or cursor color if color ram is disabled)
                    currentColor = (!colorRamEnabled) ? cursorColor : colorRam[currentIndex + y * screenWidth];

                    // draw characters only if color has been changed
                    if (currentColor != lastColor) {
                        canvas.drawText(lineBuffer, copyStart, copyLength, borderSizeLeft + copyStart * fontSize,
                                textHeight - textBounds.bottom + borderSizeTop + y * fontSize, colorsPaint[lastColor]);
                        lastColor = currentColor;
                        copyStart = currentIndex;
                        copyLength = 1;
                    }

                    currentIndex++;
                }

                // draw rest of the characters
                canvas.drawText(lineBuffer, copyStart, copyLength, borderSizeLeft + copyStart * fontSize,
                        textHeight - textBounds.bottom + borderSizeTop + y * fontSize, colorsPaint[lastColor]);
            }
        }

        // restore canvas
        canvas.restore();
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {

        if (listener != null) {

            int x = -1;

            // check if x is in horizontal screen bounds
            if (event.getX() > borderSizeLeft && event.getX() < getViewWidth() - borderSizeRight) {
                x = (int) ((event.getX() - borderSizeLeft) / fontSize);
            }

            int y = -1;

            // check if y is in vertical screen bounds
            if (event.getY() > borderSizeTop && event.getY() < getViewHeight() - borderSizeBottom) {
                y = (int) ((event.getY() - borderSizeTop) / fontSize);
            }

            // notify listener
            listener.onClick(event.getAction(), x, y);

            return true;
        }

        return false;
    }

    /****************************************************************************************************
     * HELPER METHODS
     ***************************************************************************************************/

    /**
     * Create (if needed) and return text typeface
     */
    private Typeface getTextTypeface() {

        if (textTypeface == null) {
            textTypeface = Typeface.createFromAsset(getContext().getAssets(), ASSETS_FONT_PATH);
        }

        return textTypeface;
    }

    /**
     * Return view width
     */
    private int getViewWidth() {
        return borderSizeLeft + screenWidth * fontSize + borderSizeRight;
    }

    /**
     * Return view height
     */
    private int getViewHeight() {
        return borderSizeTop + screenHeight * fontSize + borderSizeBottom;
    }

    /**
     * Return same char but reversed
     */
    private char getReversedChar(char chr) {

        for (int i = 0; i < 256; i++) {
            if (chr == PETSCIIChars.UPPERCASE[i]) {
                return PETSCIIChars.UPPERCASE[(i + 128) % 256];
            } else if (chr == PETSCIIChars.LOWERCASE[i]) {
                return PETSCIIChars.LOWERCASE[(i + 128) % 256];
            }
        }

        return chr;
    }

    /**
     * Check if a offset is in range <p><i>Must be called before writing to the screen or to the color RAM</i></p>
     */
    private boolean validOffset(int offset) {
        return (offset >= 0 && offset < screenWidth * screenHeight);
    }

    /**
     * Check if a color is in range <p><i>Must be called before writing to the color RAM</i></p>
     */
    private boolean validColor(int color) {
        return (color >= 0 && color < PETSCIIColors.C64.length);
    }

    /**
     * Print error message at top of the screen
     *
     * @param errorMessage
     *         error message
     * @param text
     *         text which caused the error
     * @param position
     *         position where the error has occurred
     */
    private void printErrorMessage(String errorMessage, String text, int position) {
        printFormattedText("{RON}" + errorMessage + " AT POSITION " + position + "{ROF}", 0, 0, 1);
        printText(text, 0, 1, 1);
    }

    /**
     * Print test picture
     */
    private void printTestPicture() {

        int offset;

        printText("**** PETSCII View " + BuildConfig.VERSION_NAME + " ****", 6, 1, cursorColor);

        printText("CHARACTER MAP UPPERCASE:", 0, 4, cursorColor);

        offset = 6 * screenWidth;
        for (int i = 0; i < PETSCIIChars.UPPERCASE.length; i++) {
            putChar(PETSCIIChars.UPPERCASE[i], offset);
            putColor(cursorColor, offset);
            offset++;
        }

        printText("CHARACTER MAP LOWERCASE:", 0, 15, cursorColor);

        offset = 17 * screenWidth;
        for (int i = 0; i < PETSCIIChars.LOWERCASE.length; i++) {
            putChar(PETSCIIChars.LOWERCASE[i], offset);
            putColor(cursorColor, offset);
            offset++;
        }
    }

    /****************************************************************************************************
     * PETSCII VIEW API
     ***************************************************************************************************/

    // getters

    /**
     * Return screen width
     */
    public int getScreenWidth() {
        return screenWidth;
    }

    /**
     * Return screen height
     */
    public int getScreenHeight() {
        return screenHeight;
    }

    /**
     * Return font size
     */
    public int getFontSize() {
        return fontSize;
    }

    /**
     * Return border size
     */
    public Rect getBorderSize() {
        return new Rect(borderSizeLeft, borderSizeTop, borderSizeRight, borderSizeBottom);
    }

    /**
     * Return border color
     */
    public int getBorderColor() {
        return borderColor;
    }

    /**
     * Return background color
     */
    public int getBkgColor() {
        return backgroundColor;
    }

    /**
     * Return cursor color
     */
    public int getCursorColor() {
        return cursorColor;
    }

    // setters

    /**
     * Set screen width (resets and measures view)
     */
    public void setScreenWidth(int width) {
        screenWidth = width;
        initView();
        requestLayout();
    }

    /**
     * Set screen height (resets and measures view)
     */
    public void setScreenHeight(int height) {
        screenHeight = height;
        initView();
        requestLayout();
    }

    /**
     * Set font size (resets and remeasures view)
     */
    public void setFontSize(int size) {
        fontSize = size;
        initView();
        requestLayout();
    }

    /**
     * Set default border size depending on the font size (measures view)
     */
    public void setDefaultBorderSize() {
        borderSizeLeft = 4 * fontSize;
        borderSizeTop = 4 * fontSize + fontSize / 4;
        borderSizeRight = 4 * fontSize;
        borderSizeBottom = 4 * fontSize + fontSize / 4;
        requestLayout();
    }

    /**
     * Set border size (measures view)
     */
    public void setBorderSize(Rect size) {
        borderSizeLeft = size.left;
        borderSizeTop = size.top;
        borderSizeRight = size.right;
        borderSizeBottom = size.bottom;
        requestLayout();
    }

    /**
     * Set border color
     */
    public void setBorderColor(int color) {
        if (validColor(color)) {
            borderColor = color;
            borderPaint.setColor(Color.parseColor(PETSCIIColors.C64[color]));
        }
    }

    /**
     * Set background color
     */
    public void setBkgColor(int color) {
        if (validColor(color)) {
            backgroundColor = color;
            backgroundPaint.setColor(Color.parseColor(PETSCIIColors.C64[color]));
        }
    }

    /**
     * Set cursor color
     */
    public void setCursorColor(int color) {
        if (validColor(color)) {
            cursorColor = color;
        }
    }

    /**
     * Enable or disable screen RAM
     */
    public void setScreenRamEnabled(boolean enabled) {
        screenRamEnabled = enabled;
    }

    /**
     * Enable or disable color RAM <p><i>If disabled, screen will be rendered with current cursor color</i></p>
     */
    public void setColorRamEnabled(boolean enabled) {
        colorRamEnabled = enabled;
    }

    /**
     * Set callback listener
     */
    public void setListener(PETSCIIListener listener) {
        this.listener = listener;
    }

    // screen manipulation

    /**
     * Put color to color RAM at given position
     *
     * @param color
     *         color
     * @param x
     *         x position
     * @param y
     *         y position
     */
    public void putColor(int color, int x, int y) {
        putColor(color, x + y * screenWidth);
    }

    /**
     * Put color to color RAM at given offset
     *
     * @param color
     *         color
     * @param offset
     *         offset position
     */
    public void putColor(int color, int offset) {
        if (validOffset(offset)) {
            if (validColor(color)) {
                colorRam[offset] = color;
            }
        }
    }

    /**
     * Fill color RAM with given color
     *
     * @param color
     *         fill color
     */
    public void fillWithColor(int color) {
        for (int i = 0; i < screenWidth * screenHeight; i++) {
            putColor(color, i);
        }
    }

    /**
     * Fill portion of color RAM with given color
     *
     * @param color
     *         fill color
     * @param fromX
     *         from x position
     * @param fromY
     *         from y position
     * @param toX
     *         to x position
     * @param toY
     *         to y position
     */
    public void fillWithColor(int color, int fromX, int fromY, int toX, int toY) {
        for (int y = fromY; y <= toY; y++) {
            for (int x = fromX; x <= toX; x++) {
                putColor(color, x, y);
            }
        }
    }

    /**
     * Put char to screen RAM at given position
     *
     * @param chr
     *         char
     * @param x
     *         x position
     * @param y
     *         y position
     */
    public void putChar(char chr, int x, int y) {
        putChar(chr, x + y * screenWidth);
    }

    /**
     * Put char to screen RAM at given offset
     *
     * @param chr
     *         char
     * @param offset
     *         offset position
     */
    public void putChar(char chr, int offset) {
        if (validOffset(offset)) {
            screenRam[offset] = chr;
        }
    }

    /**
     * Fill screen RAM with given char
     *
     * @param chr
     *         char
     */
    public void fillWithChar(char chr) {
        for (int i = 0; i < screenWidth * screenHeight; i++) {
            putChar(chr, i);
        }
    }

    /**
     * Fill portion of screen RAM with given char
     *
     * @param chr
     *         char
     * @param fromX
     *         from x position
     * @param fromY
     *         from y position
     * @param toX
     *         to x position
     * @param toY
     *         to y position
     */
    public void fillWithChar(char chr, int fromX, int fromY, int toX, int toY) {
        for (int y = fromY; y <= toY; y++) {
            for (int x = fromX; x <= toX; x++) {
                putChar(chr, x, y);
            }
        }
    }

    /**
     * Print text to screen at given position with given color
     *
     * @param text
     *         text
     * @param x
     *         x position
     * @param y
     *         y position
     * @param color
     *         text color
     */
    public void printText(String text, int x, int y, int color) {
        printText(text, x + y * screenWidth, color);
    }

    /**
     * Print text to screen at given offset with given color
     *
     * @param text
     *         text
     * @param offset
     *         offset position
     * @param color
     *         text color
     */
    public void printText(String text, int offset, int color) {

        // check if string is empty
        if (TextUtils.isEmpty(text)) {
            return;
        }

        // remember offset
        int lineStartOffset = offset;

        for (int i = 0; i < text.length(); i++) {

            // get char
            char chr = text.charAt(i);

            // line break
            if (chr == 10) {
                // update offsets
                offset = lineStartOffset + screenWidth;
                lineStartOffset = offset;
            }

            // char
            else {
                // put data to RAM
                putChar(chr, offset);
                putColor(color, offset);
                offset++;
            }
        }
    }

    /**
     * Print formatted text on screen at given position with given color
     *
     * @param text
     *         text
     * @param x
     *         x position
     * @param y
     *         y position
     * @param color
     *         text color
     */
    public void printFormattedText(String text, int x, int y, int color) {
        printFormattedText(text, x + y * screenWidth, color);
    }

    /**
     * Print formatted text on screen at given offset with given color
     *
     * @param text
     *         text
     * @param offset
     *         offset position
     * @param color
     *         text color
     */
    public void printFormattedText(String text, int offset, int color) {

        // check if string is empty
        if (TextUtils.isEmpty(text)) {
            return;
        }

        int textIndex = 0;
        int lineStartOffset = offset;
        boolean reverseEnabled = false;

        try {
            while (textIndex != text.length()) {

                // get char
                char chr = text.charAt(textIndex);

                // formatter
                if (chr == '{') {

                    // clear screen
                    if (text.startsWith(PETSCIIFormatter.CLR, textIndex + 1)) {
                        fillWithChar(' ');
                        offset = 0;
                        textIndex += 5;
                    }

                    // home
                    else if (text.startsWith(PETSCIIFormatter.HOM, textIndex + 1)) {
                        offset = 0;
                        textIndex += 5;
                    }

                    // cursor up
                    else if (text.startsWith(PETSCIIFormatter.CUP, textIndex + 1)) {
                        String textNum = text.substring(textIndex + 5, textIndex + 5 + 2);
                        offset -= Integer.valueOf(textNum) * screenWidth;
                        textIndex += 8;
                    }

                    // cursor down
                    else if (text.startsWith(PETSCIIFormatter.CDN, textIndex + 1)) {
                        String textNum = text.substring(textIndex + 5, textIndex + 5 + 2);
                        offset += Integer.valueOf(textNum) * screenWidth;
                        textIndex += 8;
                    }

                    // cursor left
                    else if (text.startsWith(PETSCIIFormatter.CLT, textIndex + 1)) {
                        String textNum = text.substring(textIndex + 5, textIndex + 5 + 2);
                        offset += Integer.valueOf(textNum);
                        textIndex += 8;
                    }

                    // cursor right
                    else if (text.startsWith(PETSCIIFormatter.CRT, textIndex + 1)) {
                        String textNum = text.substring(textIndex + 5, textIndex + 5 + 2);
                        offset -= Integer.valueOf(textNum);
                        textIndex += 8;
                    }

                    // cursor color
                    else if (text.startsWith(PETSCIIFormatter.COL, textIndex + 1)) {
                        String textNum = text.substring(textIndex + 5, textIndex + 5 + 2);
                        color = Integer.valueOf(textNum);
                        textIndex += 8;
                    }

                    // reverse on
                    else if (text.startsWith(PETSCIIFormatter.RON, textIndex + 1)) {
                        reverseEnabled = true;
                        textIndex += 5;
                    }

                    // reverse off
                    else if (text.startsWith(PETSCIIFormatter.ROF, textIndex + 1)) {
                        reverseEnabled = false;
                        textIndex += 5;
                    }

                    // unknown formatter
                    else {
                        printErrorMessage(PETSCIIFormatter.UNKNOWN_FORMATTER_ERROR, text, textIndex + 1);
                        break;
                    }
                }

                // line break
                else if (chr == 10) {
                    // update offsets
                    offset = lineStartOffset + screenWidth;
                    lineStartOffset = offset;
                    textIndex++;
                }

                // char
                else {
                    // put data to RAM
                    putChar(reverseEnabled ? getReversedChar(chr) : chr, offset);
                    putColor(color, offset);
                    textIndex++;
                    offset++;
                }
            }
        } catch (NumberFormatException e) {
            printErrorMessage(PETSCIIFormatter.NUMBER_FORMAT_ERROR, text, textIndex + 5);
        } catch (Exception e) {
            printErrorMessage(PETSCIIFormatter.PARSING_ERROR, text, textIndex);
        }
    }
}
