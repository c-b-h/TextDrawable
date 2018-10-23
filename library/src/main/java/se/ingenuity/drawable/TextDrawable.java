/*
 * Copyright (c) 2018 Ingenuity
 * Copyright (c) 2012 Wireless Designs, LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining
 * a copy of this software and associated documentation files (the
 * "Software"), to deal in the Software without restriction, including
 * without limitation the rights to use, copy, modify, merge, publish,
 * distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so, subject to
 * the following conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE
 * LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION
 * WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package se.ingenuity.drawable;

import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.util.SparseIntArray;
import android.util.TypedValue;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Objects;

import androidx.annotation.AttrRes;
import androidx.annotation.IntDef;
import androidx.annotation.IntRange;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StyleRes;

/**
 * A Drawable object that draws text.
 * A TextDrawable accepts most of the same parameters that can be applied to
 * {@link android.widget.TextView} for displaying and formatting text.
 * <p>
 * Optionally, a {@link Path} may be supplied on which to draw the text.
 * <p>
 * A TextDrawable has an intrinsic size equal to that required to draw all
 * the text it has been supplied, when possible.  In cases where a {@link Path}
 * has been supplied, the caller must explicitly call
 * {@link #setBounds(android.graphics.Rect) setBounds()} to provide the Drawable
 * size based on the Path constraints.
 */
@SuppressWarnings("WeakerAccess")
public class TextDrawable extends Drawable {
    // Maps styleable attributes that exist both in TextView style and TextAppearance.
    private static final SparseIntArray sAppearanceValues = new SparseIntArray();

    static {
//        sAppearanceValues.put(R.styleable.TextDrawable_android_textColorHighlight,
//                R.styleable.TextDrawable_TextAppearance_android_textColorHighlight);
        sAppearanceValues.put(R.styleable.TextDrawable_android_textColor,
                R.styleable.TextDrawable_TextAppearance_android_textColor);
        sAppearanceValues.put(R.styleable.TextDrawable_android_textColorHint,
                R.styleable.TextDrawable_TextAppearance_android_textColorHint);
        sAppearanceValues.put(R.styleable.TextDrawable_android_textColorLink,
                R.styleable.TextDrawable_TextAppearance_android_textColorLink);
        sAppearanceValues.put(R.styleable.TextDrawable_android_textSize,
                R.styleable.TextDrawable_TextAppearance_android_textSize);
        sAppearanceValues.put(R.styleable.TextDrawable_android_typeface,
                R.styleable.TextDrawable_TextAppearance_android_typeface);
        sAppearanceValues.put(R.styleable.TextDrawable_android_fontFamily,
                R.styleable.TextDrawable_TextAppearance_android_fontFamily);
        sAppearanceValues.put(R.styleable.TextDrawable_android_textStyle,
                R.styleable.TextDrawable_TextAppearance_android_textStyle);
        sAppearanceValues.put(R.styleable.TextDrawable_android_textFontWeight,
                R.styleable.TextDrawable_TextAppearance_android_textFontWeight);
        sAppearanceValues.put(R.styleable.TextDrawable_android_textAllCaps,
                R.styleable.TextDrawable_TextAppearance_android_textAllCaps);
        sAppearanceValues.put(R.styleable.TextDrawable_android_shadowColor,
                R.styleable.TextDrawable_TextAppearance_android_shadowColor);
        sAppearanceValues.put(R.styleable.TextDrawable_android_shadowDx,
                R.styleable.TextDrawable_TextAppearance_android_shadowDx);
        sAppearanceValues.put(R.styleable.TextDrawable_android_shadowDy,
                R.styleable.TextDrawable_TextAppearance_android_shadowDy);
        sAppearanceValues.put(R.styleable.TextDrawable_android_shadowRadius,
                R.styleable.TextDrawable_TextAppearance_android_shadowRadius);
        sAppearanceValues.put(R.styleable.TextDrawable_android_elegantTextHeight,
                R.styleable.TextDrawable_TextAppearance_android_elegantTextHeight);
        sAppearanceValues.put(R.styleable.TextDrawable_android_fallbackLineSpacing,
                R.styleable.TextDrawable_TextAppearance_android_fallbackLineSpacing);
        sAppearanceValues.put(R.styleable.TextDrawable_android_letterSpacing,
                R.styleable.TextDrawable_TextAppearance_android_letterSpacing);
        sAppearanceValues.put(R.styleable.TextDrawable_android_fontFeatureSettings,
                R.styleable.TextDrawable_TextAppearance_android_fontFeatureSettings);
    }

    @IntDef(value = {
            XMLTypefaceAttr.DEFAULT_TYPEFACE,
            XMLTypefaceAttr.SANS,
            XMLTypefaceAttr.SERIF,
            XMLTypefaceAttr.MONOSPACE})
    @Retention(RetentionPolicy.SOURCE)
    private @interface XMLTypefaceAttr {
        int DEFAULT_TYPEFACE = -1;
        int SANS = 1;
        int SERIF = 2;
        int MONOSPACE = 3;
    }

    @IntDef(value = {Typeface.NORMAL, Typeface.BOLD, Typeface.ITALIC, Typeface.BOLD_ITALIC})
    @Retention(RetentionPolicy.SOURCE)
    private @interface Style {
    }

    private static final int MAX_WEIGHT = 1000;

    /* Paint to hold most drawing primitives for the text */
    @NonNull
    private final TextPaint mTextPaint;
    /* Container for the bounds to be reported to widgets */
    @NonNull
    private final Rect mTextBounds;

    private Resources mResources;

    private Resources.Theme mTheme;

    /* Layout is used to measure and draw the text */
    private StaticLayout mTextLayout;
    /* Alignment of the text inside its bounds */
    private Layout.Alignment mTextAlignment = Layout.Alignment.ALIGN_NORMAL;
    /* Optional path on which to draw the text */
    private Path mTextPath;
    /* Stateful text color list */
    private ColorStateList mTextColors;
    /* Text string to draw */
    private CharSequence mText = "";
    /* All caps */
    private boolean mAllCaps = false;

    public TextDrawable() {
        //Definition of this drawables size
        mTextBounds = new Rect();
        //Paint to use for the text
        mTextPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
//        mTextPaint.setDither(true);
    }

    public TextDrawable(@NonNull Context context,
                        @Nullable AttributeSet attrs,
                        @AttrRes int defStyleAttr,
                        @StyleRes int defStyleRes) {
        this();

        init(context.getResources(), context.getTheme(), attrs, defStyleAttr, defStyleRes);
    }

    private void init(@NonNull Resources resources,
                      @Nullable Resources.Theme theme,
                      @Nullable AttributeSet attrs,
                      @AttrRes int defStyleAttr,
                      @StyleRes int defStyleRes) {
        mResources = resources;
        mTheme = theme;

        mTextPaint.density = mResources.getDisplayMetrics().density;

        final TextAppearanceAttributes attributes = new TextAppearanceAttributes();
        attributes.mTextColor = ColorStateList.valueOf(0xFF000000);
        attributes.mTextSize = 15;

        /*
         * Look the appearance up without checking first if it exists because
         * almost every TextView has one and it greatly simplifies the logic
         * to be able to parse the appearance first and then let specific tags
         * for this View override it.
         */
        TypedArray a = obtainAttributes(mResources, mTheme, attrs,
                R.styleable.TextViewAppearance, defStyleAttr, defStyleRes);
        TypedArray appearance = null;
        final int ap = a.getResourceId(
                R.styleable.TextViewAppearance_android_textAppearance, -1);
        a.recycle();
        if (ap != -1) {
            appearance = obtainAttributes(mResources,
                    mTheme,
                    null,
                    R.styleable.TextDrawable_TextAppearance,
                    0,
                    ap);
        }
        if (appearance != null) {
            readTextAppearance(appearance, attributes, false /* styleArray */);
            attributes.mFontFamilyExplicit = false;
            appearance.recycle();
        }

        a = obtainAttributes(mResources, mTheme,
                attrs, R.styleable.TextDrawable, defStyleAttr, defStyleRes);

        setText(a.getText(R.styleable.TextDrawable_android_text));

        readTextAppearance(a, attributes, true /* styleArray */);

        a.recycle();

        applyTextAppearance(attributes);
    }

    @Override
    public void inflate(@NonNull Resources resources,
                        @NonNull XmlPullParser parser,
                        @NonNull AttributeSet attrs,
                        @Nullable Resources.Theme theme) throws IOException, XmlPullParserException {
        super.inflate(resources, parser, attrs, theme);
        mResources = resources;
        mTheme = theme;

        init(resources, theme, attrs, 0, 0);
    }

    /**
     * Read the Text Appearance attributes from a given TypedArray and set its values to the given
     * set. If the TypedArray contains a value that was already set in the given attributes, that
     * will be overriden.
     *
     * @param appearance The TypedArray to read properties from
     * @param attributes the TextAppearanceAttributes to fill in
     * @param styleArray Whether the given TypedArray is a style or a TextAppearance. This defines
     *                   what attribute indexes will be used to read the properties.
     */
    private void readTextAppearance(@NonNull TypedArray appearance,
                                    @NonNull TextAppearanceAttributes attributes,
                                    boolean styleArray) {
        final int n = appearance.getIndexCount();
        for (int i = 0; i < n; i++) {
            final int attr = appearance.getIndex(i);
            int index = attr;
            // Translate style array index ids to TextAppearance ids.
            if (styleArray) {
                index = sAppearanceValues.get(attr, -1);
                if (index == -1) {
                    // This value is not part of a Text Appearance and should be ignored.
                    continue;
                }
            }
            /*if (index == R.styleable.TextDrawable_TextAppearance_android_textColorHighlight) {
                attributes.mTextColorHighlight =
                        appearance.getColor(attr, attributes.mTextColorHighlight);

            } else */
            if (index == R.styleable.TextDrawable_TextAppearance_android_textColor) {
                attributes.mTextColor = appearance.getColorStateList(attr);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_textColorHint) {
                attributes.mTextColorHint = appearance.getColorStateList(attr);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_textColorLink) {
                attributes.mTextColorLink = appearance.getColorStateList(attr);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_textSize) {
                attributes.mTextSize =
                        appearance.getDimensionPixelSize(attr, attributes.mTextSize);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_typeface) {
                attributes.mTypefaceIndex = appearance.getInt(attr, attributes.mTypefaceIndex);
                if (attributes.mTypefaceIndex != -1 && !attributes.mFontFamilyExplicit) {
                    attributes.mFontFamily = null;
                }

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_fontFamily) {
//                if (!context.isRestricted() && context.canLoadUnsafeResources()) {
                try {
                    attributes.mFontTypeface = appearance.getFont(attr);
                } catch (UnsupportedOperationException | Resources.NotFoundException e) {
                    // Expected if it is not a font resource.
                }
//                }
                if (attributes.mFontTypeface == null) {
                    attributes.mFontFamily = appearance.getString(attr);
                }
                attributes.mFontFamilyExplicit = true;

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_textStyle) {
                attributes.mStyleIndex = appearance.getInt(attr, attributes.mStyleIndex);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_textFontWeight) {
                attributes.mFontWeight = appearance.getInt(attr, attributes.mFontWeight);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_textAllCaps) {
                attributes.mAllCaps = appearance.getBoolean(attr, attributes.mAllCaps);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_shadowColor) {
                attributes.mShadowColor = appearance.getInt(attr, attributes.mShadowColor);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_shadowDx) {
                attributes.mShadowDx = appearance.getFloat(attr, attributes.mShadowDx);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_shadowDy) {
                attributes.mShadowDy = appearance.getFloat(attr, attributes.mShadowDy);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_shadowRadius) {
                attributes.mShadowRadius = appearance.getFloat(attr, attributes.mShadowRadius);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_elegantTextHeight) {
                attributes.mHasElegant = true;
                attributes.mElegant = appearance.getBoolean(attr, attributes.mElegant);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_fallbackLineSpacing) {
                attributes.mHasFallbackLineSpacing = true;
                attributes.mFallbackLineSpacing = appearance.getBoolean(attr,
                        attributes.mFallbackLineSpacing);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_letterSpacing) {
                attributes.mHasLetterSpacing = true;
                attributes.mLetterSpacing =
                        appearance.getFloat(attr, attributes.mLetterSpacing);

            } else if (index == R.styleable.TextDrawable_TextAppearance_android_fontFeatureSettings) {
                attributes.mFontFeatureSettings = appearance.getString(attr);
            }
        }
    }

    private void applyTextAppearance(TextAppearanceAttributes attributes) {
        if (attributes.mTextColor != null) {
            setTextColor(attributes.mTextColor);
        }

        if (attributes.mTextColorHint != null) {
//            setHintTextColor(attributes.mTextColorHint);
        }

        if (attributes.mTextColorLink != null) {
//            setLinkTextColor(attributes.mTextColorLink);
        }

//        if (attributes.mTextColorHighlight != 0) {
//            setHighlightColor(attributes.mTextColorHighlight);
//        }

        if (attributes.mTextSize != 0) {
            setRawTextSize(attributes.mTextSize);
        }

        if (attributes.mTypefaceIndex != -1 && !attributes.mFontFamilyExplicit) {
            attributes.mFontFamily = null;
        }
        setTypefaceFromAttrs(attributes.mFontTypeface, attributes.mFontFamily,
                attributes.mTypefaceIndex, attributes.mStyleIndex, attributes.mFontWeight);

        if (attributes.mShadowColor != 0) {
            setShadowLayer(attributes.mShadowRadius,
                    attributes.mShadowDx,
                    attributes.mShadowDy,
                    attributes.mShadowColor);
        }

        mAllCaps = attributes.mAllCaps;

        if (attributes.mHasElegant) {
            setElegantTextHeight(attributes.mElegant);
        }

        if (attributes.mHasFallbackLineSpacing) {
//            setFallbackLineSpacing(attributes.mFallbackLineSpacing);
        }

        if (attributes.mHasLetterSpacing) {
            setLetterSpacing(attributes.mLetterSpacing);
        }

        if (attributes.mFontFeatureSettings != null) {
            setFontFeatureSettings(attributes.mFontFeatureSettings);
        }
    }

    /**
     * Return the text currently being displayed
     */
    @NonNull
    public CharSequence getText() {
        return mText;
    }

    /**
     * Set the text that will be displayed
     *
     * @param text Text to display
     */
    public void setText(@Nullable CharSequence text) {
        if (text == null) text = "";

        if (mAllCaps) {
            mText = text.toString().toUpperCase(mResources.getConfiguration().locale);
        } else {
            mText = text;
        }

        measureContent();
    }

    /**
     * Return the current text size, in pixels
     */
    public float getTextSize() {
        return mTextPaint.getTextSize();
    }

    /**
     * Set the text size.  The value will be interpreted in "sp" units
     *
     * @param size Text size value, in sp
     */
    public void setTextSize(float size) {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, size);
    }

    /**
     * Set the text size, using the supplied complex units
     *
     * @param unit Units for the text size, such as dp or sp
     * @param size Text size value
     */
    public void setTextSize(int unit, float size) {
        float dimension = TypedValue.applyDimension(unit, size, mResources.getDisplayMetrics());
        setRawTextSize(dimension);
    }

    /*
     * Set the text size, in raw pixels
     */
    private void setRawTextSize(float size) {
        if (size != mTextPaint.getTextSize()) {
            mTextPaint.setTextSize(size);

            measureContent();
        }
    }

    /**
     * Gets the extent by which text should be stretched horizontally.
     * This will usually be 1.0.
     *
     * @return The horizontal scale factor.
     */
    public float getTextScaleX() {
        return mTextPaint.getTextScaleX();
    }

    /**
     * Sets the horizontal scale factor for text. The default value
     * is 1.0. Values greater than 1.0 stretch the text wider.
     * Values less than 1.0 make the text narrower. By default, this value is 1.0.
     *
     * @param size The horizontal scale factor.
     * @attr ref R.styleable#TextView_android_textScaleX
     */
    public void setTextScaleX(float size) {
        if (size != mTextPaint.getTextScaleX()) {
            mTextPaint.setTextScaleX(size);
            measureContent();
        }
    }

    /**
     * Return the paint's letter-spacing for text. The default value
     * is 0.
     *
     * @return the paint's letter-spacing for drawing text.
     */
    public float getLetterSpacing() {
        return mTextPaint.getLetterSpacing();
    }

    /**
     * Set the paint's letter-spacing for text. The default value
     * is 0.  The value is in 'EM' units.  Typical values for slight
     * expansion will be around 0.05.  Negative values tighten text.
     *
     * @param letterSpacing set the paint's letter-spacing for drawing text.
     */
    public void setLetterSpacing(float letterSpacing) {
        if (mTextPaint.getLetterSpacing() != letterSpacing) {
            mTextPaint.setLetterSpacing(letterSpacing);
        }
    }


    /**
     * Returns the font feature settings. The format is the same as the CSS
     * font-feature-settings attribute:
     * <a href="https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop">
     * https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop</a>
     *
     * @return the currently set font feature settings.  Default is null.
     * @see #setFontFeatureSettings(String)
     * @see Paint#setFontFeatureSettings(String) Paint.setFontFeatureSettings(String)
     */
    @Nullable
    public String getFontFeatureSettings() {
        return mTextPaint.getFontFeatureSettings();
    }

    /**
     * Sets font feature settings. The format is the same as the CSS
     * font-feature-settings attribute:
     * <a href="https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop">
     * https://www.w3.org/TR/css-fonts-3/#font-feature-settings-prop</a>
     *
     * @param fontFeatureSettings font feature settings represented as CSS compatible string
     * @attr ref android.R.styleable#TextView_fontFeatureSettings
     * @see #getFontFeatureSettings()
     * @see Paint#getFontFeatureSettings() Paint.getFontFeatureSettings()
     */
    public void setFontFeatureSettings(@Nullable String fontFeatureSettings) {
        if (!Objects.equals(fontFeatureSettings, mTextPaint.getFontFeatureSettings())) {
            mTextPaint.setFontFeatureSettings(fontFeatureSettings);
        }
    }

    /**
     * Get the value of the TextView's elegant height metrics flag. This setting selects font
     * variants that have not been compacted to fit Latin-based vertical
     * metrics, and also increases top and bottom bounds to provide more space.
     *
     * @return {@code true} if the elegant height metrics flag is set.
     * @see #setElegantTextHeight(boolean)
     * @see Paint#setElegantTextHeight(boolean)
     */
    public boolean isElegantTextHeight() {
        return mTextPaint.isElegantTextHeight();
    }

    /**
     * Set the TextView's elegant height metrics flag. This setting selects font
     * variants that have not been compacted to fit Latin-based vertical
     * metrics, and also increases top and bottom bounds to provide more space.
     *
     * @param elegant set the paint's elegant metrics flag.
     * @attr ref android.R.styleable#TextView_elegantTextHeight
     * @see #isElegantTextHeight()
     * @see Paint#isElegantTextHeight()
     */
    public void setElegantTextHeight(boolean elegant) {
        if (elegant != mTextPaint.isElegantTextHeight()) {
            mTextPaint.setElegantTextHeight(elegant);
        }
    }

    /**
     * Return the current text alignment setting
     */
    public Layout.Alignment getTextAlign() {
        return mTextAlignment;
    }

    /**
     * Set the text alignment.  The alignment itself is based on the text layout direction.
     * For LTR text NORMAL is left aligned and OPPOSITE is right aligned.
     * For RTL text, those alignments are reversed.
     *
     * @param align Text alignment value.  Should be set to one of:
     *              <p>
     *              {@link Layout.Alignment#ALIGN_NORMAL},
     *              {@link Layout.Alignment#ALIGN_NORMAL},
     *              {@link Layout.Alignment#ALIGN_OPPOSITE}.
     */
    public void setTextAlign(Layout.Alignment align) {
        if (mTextAlignment != align) {
            mTextAlignment = align;
            measureContent();
        }
    }


    /**
     * Return the current typeface and style that the Paint
     * using for display.
     */
    public Typeface getTypeface() {
        return mTextPaint.getTypeface();
    }

    /**
     * Sets the typeface and style in which the text should be displayed.
     * Note that not all Typeface families actually have bold and italic
     * variants, so you may need to use
     * {@link #setTypeface(Typeface, int)} to get the appearance
     * that you actually want.
     */
    public void setTypeface(Typeface tf) {
        if (mTextPaint.getTypeface() != tf) {
            mTextPaint.setTypeface(tf);

            measureContent();
        }
    }

    /**
     * Sets the typeface and style in which the text should be displayed,
     * and turns on the fake bold and italic bits in the Paint if the
     * Typeface that you provided does not have all the bits in the
     * style that you specified.
     *
     * @attr ref R.styleable#TextView_android_typeface
     * @attr ref R.styleable#TextView_android_textStyle
     */
    public void setTypeface(@Nullable Typeface tf, @Style int style) {
        if (style > 0) {
            if (tf == null) {
                tf = Typeface.defaultFromStyle(style);
            } else {
                tf = Typeface.create(tf, style);
            }

            setTypeface(tf);
            // now compute what (if any) algorithmic styling is needed
            int typefaceStyle = tf != null ? tf.getStyle() : 0;
            int need = style & ~typefaceStyle;
            mTextPaint.setFakeBoldText((need & Typeface.BOLD) != 0);
            mTextPaint.setTextSkewX((need & Typeface.ITALIC) != 0 ? -0.25f : 0);
        } else {
            mTextPaint.setFakeBoldText(false);
            mTextPaint.setTextSkewX(0);
            setTypeface(tf);
        }
    }

    /**
     * Gives the text a shadow of the specified blur radius and color, the specified
     * distance from its drawn position.
     * <p>
     * The text shadow produced does not interact with the properties on view
     * that are responsible for real time shadows,
     *
     * @attr ref R.styleable#TextView_android_shadowColor
     * @attr ref R.styleable#TextView_android_shadowDx
     * @attr ref R.styleable#TextView_android_shadowDy
     * @attr ref R.styleable#TextView_android_shadowRadius
     * @see Paint#setShadowLayer(float, float, float, int)
     */
    public void setShadowLayer(float radius, float dx, float dy, int color) {
        mTextPaint.setShadowLayer(radius, dx, dy, color);
        measureContent();
    }

    public void setTextAppearance(@StyleRes int resId) {
        final TypedArray ta = obtainAttributes(mResources,
                null, null, R.styleable.TextDrawable_TextAppearance, 0, resId);
        final TextAppearanceAttributes attributes = new TextAppearanceAttributes();
        readTextAppearance(ta, attributes, false /* styleArray */);
        ta.recycle();
        applyTextAppearance(attributes);
    }

    /**
     * Set a single text color for all states
     *
     * @param color Color value such as {@link Color#WHITE} or {@link Color#argb(int, int, int, int)}
     */
    public void setTextColor(int color) {
        setTextColor(ColorStateList.valueOf(color));
    }

    /**
     * Set the text color as a state list
     *
     * @param colorStateList ColorStateList of text colors, such as inflated from an R.color resource
     */
    public void setTextColor(ColorStateList colorStateList) {
        mTextColors = colorStateList;
        updateTextColors(getState());
    }

    /**
     * Optional Path object on which to draw the text.  If this is set,
     * TextDrawable cannot properly measure the bounds this drawable will need.
     * You must call {@link #setBounds(int, int, int, int) setBounds()} before
     * applying this TextDrawable to any View.
     * <p>
     * Calling this method with <code>null</code> will remove any Path currently attached.
     */
    public void setTextPath(Path path) {
        if (mTextPath != path) {
            mTextPath = path;
            measureContent();
        }
    }

    /**
     * Internal method to take measurements of the current contents and apply
     * the correct bounds when possible.
     */
    private void measureContent() {
        //If drawing to a path, we cannot measure intrinsic bounds
        //We must rely on setBounds being called externally
        if (mTextPath != null) {
            //Clear any previous measurement
            mTextLayout = null;
            mTextBounds.setEmpty();
        } else {
            //Measure text bounds
            double desired = Math.ceil(Layout.getDesiredWidth(mText, mTextPaint));
            mTextLayout = new StaticLayout(mText, mTextPaint, (int) desired,
                    mTextAlignment, 1.0f, 0.0f, false);
            mTextBounds.set(0, 0, mTextLayout.getWidth(), mTextLayout.getHeight());
        }

        //We may need to be redrawn
        invalidateSelf();
    }

    /**
     * Internal method to apply the correct text color based on the drawable's state
     */
    private boolean updateTextColors(int[] stateSet) {
        int newColor = mTextColors.getColorForState(stateSet, Color.WHITE);
        if (mTextPaint.getColor() != newColor) {
            mTextPaint.setColor(newColor);
            return true;
        }

        return false;
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        //Update the internal bounds in response to any external requests
        mTextBounds.set(bounds);
    }

    @Override
    public boolean isStateful() {
        /*
         * The drawable's ability to represent state is based on
         * the text color list set
         */
        return mTextColors.isStateful();
    }

    @Override
    protected boolean onStateChange(int[] state) {
        //Upon state changes, grab the correct text color
        return updateTextColors(state);
    }

    @Override
    public int getIntrinsicHeight() {
        //Return the vertical bounds measured, or -1 if none
        if (mTextBounds.isEmpty()) {
            return -1;
        } else {
            return (mTextBounds.bottom - mTextBounds.top);
        }
    }

    @Override
    public int getIntrinsicWidth() {
        //Return the horizontal bounds measured, or -1 if none
        if (mTextBounds.isEmpty()) {
            return -1;
        } else {
            return (mTextBounds.right - mTextBounds.left);
        }
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        final Rect bounds = getBounds();
        final int count = canvas.save();
        canvas.translate(bounds.left, bounds.top);
        if (mTextPath == null) {
            //Allow the layout to draw the text
            mTextLayout.draw(canvas);
        } else {
            //Draw directly on the canvas using the supplied path
            canvas.drawTextOnPath(mText.toString(), mTextPath, 0, 0, mTextPaint);
        }
        canvas.restoreToCount(count);
    }

    @Override
    public void setAlpha(int alpha) {
        if (mTextPaint.getAlpha() != alpha) {
            mTextPaint.setAlpha(alpha);
        }
    }

    @Override
    public int getOpacity() {
        return mTextPaint.getAlpha();
    }

    @Override
    public void setColorFilter(ColorFilter cf) {
        if (mTextPaint.getColorFilter() != cf) {
            mTextPaint.setColorFilter(cf);
        }
    }


    /**
     * Sets the Typeface taking into account the given attributes.
     *
     * @param typeface      a typeface
     * @param familyName    family name string, e.g. "serif"
     * @param typefaceIndex an index of the typeface enum, e.g. SANS, SERIF.
     * @param style         a typeface style
     * @param weight        a weight value for the Typeface or -1 if not specified.
     */
    private void setTypefaceFromAttrs(@Nullable Typeface typeface, @Nullable String familyName,
                                      @XMLTypefaceAttr int typefaceIndex, @Style int style,
                                      @IntRange(from = -1, to = MAX_WEIGHT) int weight) {
        if (typeface == null && familyName != null) {
            // Lookup normal Typeface from system font map.
            final Typeface normalTypeface = Typeface.create(familyName, Typeface.NORMAL);
            resolveStyleAndSetTypeface(normalTypeface, style, weight);
        } else if (typeface != null) {
            resolveStyleAndSetTypeface(typeface, style, weight);
        } else {  // both typeface and familyName is null.
            switch (typefaceIndex) {
                case XMLTypefaceAttr.SANS:
                    resolveStyleAndSetTypeface(Typeface.SANS_SERIF, style, weight);
                    break;
                case XMLTypefaceAttr.SERIF:
                    resolveStyleAndSetTypeface(Typeface.SERIF, style, weight);
                    break;
                case XMLTypefaceAttr.MONOSPACE:
                    resolveStyleAndSetTypeface(Typeface.MONOSPACE, style, weight);
                    break;
                case XMLTypefaceAttr.DEFAULT_TYPEFACE:
                default:
                    resolveStyleAndSetTypeface(null, style, weight);
                    break;
            }
        }
    }

    private void resolveStyleAndSetTypeface(@Nullable Typeface typeface,
                                            @Style int style,
                                            @IntRange(from = -1, to = MAX_WEIGHT) int weight) {
        if (weight >= 0) {
            weight = Math.min(MAX_WEIGHT, weight);
            final boolean italic = (style & Typeface.ITALIC) != 0;
            setTypeface(Typeface.create(typeface, weight, italic));
        } else {
            setTypeface(typeface, style);
        }
    }

    @NonNull
    private static TypedArray obtainAttributes(@NonNull Resources res,
                                               @Nullable Resources.Theme theme,
                                               @Nullable AttributeSet set,
                                               @NonNull int[] attrs,
                                               @AttrRes int defStyleAttr,
                                               @StyleRes int defStyleRes) {
        if (theme == null) {
            return res.obtainAttributes(set, attrs);
        }
        return theme.obtainStyledAttributes(set, attrs, defStyleAttr, defStyleRes);
    }

    /**
     * Set of attributes that can be defined in a Text Appearance. This is used to simplify the code
     * that reads these attributes in the constructor and in {@link TextDrawable#setTextAppearance}.
     */
    static class TextAppearanceAttributes {
        //        int mTextColorHighlight = 0;
        ColorStateList mTextColor = null;
        ColorStateList mTextColorHint = null;
        ColorStateList mTextColorLink = null;
        int mTextSize = 0;
        String mFontFamily = null;
        Typeface mFontTypeface = null;
        boolean mFontFamilyExplicit = false;
        @XMLTypefaceAttr
        int mTypefaceIndex = XMLTypefaceAttr.DEFAULT_TYPEFACE;
        @Style
        int mStyleIndex = Typeface.NORMAL;
        int mFontWeight = -1;
        boolean mAllCaps = false;
        int mShadowColor = 0;
        float mShadowDx = 0, mShadowDy = 0, mShadowRadius = 0;
        boolean mHasElegant = false;
        boolean mElegant = false;
        boolean mHasFallbackLineSpacing = false;
        boolean mFallbackLineSpacing = false;
        boolean mHasLetterSpacing = false;
        float mLetterSpacing = 0;
        String mFontFeatureSettings = null;

        @NonNull
        @Override
        public String toString() {
            return "TextAppearanceAttributes {\n"
//                    + "    mTextColorHighlight:" + mTextColorHighlight + "\n"
                    + "    mTextColor:" + mTextColor + "\n"
                    + "    mTextColorHint:" + mTextColorHint + "\n"
                    + "    mTextColorLink:" + mTextColorLink + "\n"
                    + "    mTextSize:" + mTextSize + "\n"
                    + "    mFontFamily:" + mFontFamily + "\n"
                    + "    mFontTypeface:" + mFontTypeface + "\n"
                    + "    mFontFamilyExplicit:" + mFontFamilyExplicit + "\n"
                    + "    mTypefaceIndex:" + mTypefaceIndex + "\n"
                    + "    mStyleIndex:" + mStyleIndex + "\n"
                    + "    mFontWeight:" + mFontWeight + "\n"
                    + "    mAllCaps:" + mAllCaps + "\n"
                    + "    mShadowColor:" + mShadowColor + "\n"
                    + "    mShadowDx:" + mShadowDx + "\n"
                    + "    mShadowDy:" + mShadowDy + "\n"
                    + "    mShadowRadius:" + mShadowRadius + "\n"
                    + "    mHasElegant:" + mHasElegant + "\n"
                    + "    mElegant:" + mElegant + "\n"
                    + "    mHasFallbackLineSpacing:" + mHasFallbackLineSpacing + "\n"
                    + "    mFallbackLineSpacing:" + mFallbackLineSpacing + "\n"
                    + "    mHasLetterSpacing:" + mHasLetterSpacing + "\n"
                    + "    mLetterSpacing:" + mLetterSpacing + "\n"
                    + "    mFontFeatureSettings:" + mFontFeatureSettings + "\n"
                    + "}";
        }
    }
}
