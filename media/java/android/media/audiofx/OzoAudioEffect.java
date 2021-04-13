/*
Copyright (C) 2019 Nokia Corporation.
This material, including documentation and any related
computer programs, is protected by copyright controlled by
Nokia Corporation. All rights are reserved. Copying,
including reproducing, storing, adapting or translating, any
or all of this material requires the prior written consent of
Nokia Corporation. This material also contains confidential
information which may not be disclosed to others without the
prior written consent of Nokia Corporation.
*/

package android.media.audiofx;

import android.util.Log;
import java.util.UUID;
import java.io.UnsupportedEncodingException;

/**
 * @hide
 */
public class OzoAudioEffect extends AudioEffect {

    private final static String TAG = "OzoAudioEffect";

    /**
     * Effect type
     */
    private final static UUID OZO_EFFECT_TYPE = UUID.fromString("56d6b082-1a83-455a-84a8-9db3a35cf532");

    /**
     * Effect UUID.
     */
    private final static UUID OZO_EFFECT_UUID = UUID.fromString("7e384a3b-7850-4a64-a097-884250d8a737");

    private final int OZO_PARAM_DEVICE_UUID = 111111;
    private final int OZO_CAPTURE_WNR_LEVEL = 111119;
    private final int OZO_CAPTURE_AUDIO_LEVEL = 111121;
    private final int OZO_PARAM_MICBLOCKING_MODE = 111122;
    private final int OZO_CAPTURE_MICBLOCKING_LEVEL = 111123;

    private final int OZO_PARAM_GENERIC = 111124;

    public final static class OzoParameters
    {
        private OzoParameters() {}

        // Feature on/off
        public static final String ENABLED = "on";
        public static final String DISABLED = "off";

        // Custom processing feature tag
        public final static String FEAT_CUSTOM = "custom";

        // Windscreen feature tag
        public final static String FEAT_WINDSCREEN = "wnr";

        // Noise suppression feature tag
        public final static String FEAT_NOISESUPPRESSION = "ns";

        // Zoom feature tag
        public final static String FEAT_ZOOM = "zoom";

        // Focus feature tags
        public final static String FEAT_FOCUS = "focus";
        public final static String FEAT_FOCUSAZIMUTH = "focus-azimuth";
        public final static String FEAT_FOCUSELEVATION = "focus-elevation";
        public final static String FEAT_FOCUSWIDTH = "focus-width";
        public final static String FEAT_FOCUSHEIGHT = "focus-height";
    }

    /**
     * Creates an OzoAudioEffect and attaches it to the AudioRecord on the audio session specified.
     *
     * @param audioSession system wide unique audio session identifier. The OzoAudioEffect
     * will be applied to the AudioRecord with the same audio session.
     * @return OzoAudioEffect created or null if the device does not implement the effect.
     *
     * @hide
     */
    public static OzoAudioEffect create(int audioSession) {
        OzoAudioEffect ozo = null;
        try {
            ozo = new OzoAudioEffect(audioSession);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "not implemented on this device" + ozo);
        } catch (UnsupportedOperationException e) {
            Log.w(TAG, "not enough resources");
        } catch (RuntimeException e) {
            Log.w(TAG, "not enough memory");
        }
        return ozo;
    }

    /**
     * Assign device ID to effect.
     *
     * @param id Device ID
     * @return {@link #SUCCESS} in case of success, {@link #ERROR_BAD_VALUE},
     *         {@link #ERROR_NO_MEMORY}, {@link #ERROR_INVALID_OPERATION} or
     *         {@link #ERROR_DEAD_OBJECT} in case of failure
     *
     * @hide
     */
    public int setDevice(String id) {
        try {
            byte[] uuid = id.getBytes("UTF-8");
            return this.setParameter(OZO_PARAM_DEVICE_UUID, uuid);
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Unable to set device ID: " + e);
            return ERROR_BAD_VALUE;
        }
    }

    /**
     * Query effect device ID.
     *
     * @return Device ID
     * @hide
     */
    public String getDevice() {
        byte[] value = new byte[36];
        this.getParameter(OZO_PARAM_DEVICE_UUID, value);
        return new String(value);
    }

    /**
     * Enable wind noise reduction.
     *
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int enableWnr() {
        return setOzoParameter(OzoParameters.FEAT_WINDSCREEN, OzoParameters.ENABLED);
    }

    /**
     * Disable wind noise reduction.
     *
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int disableWnr() {
        return setOzoParameter(OzoParameters.FEAT_WINDSCREEN, OzoParameters.DISABLED);
    }

    /**
     * Query wind noise level.
     *
     * @return Wind noise level on success, {@link #ERROR_BAD_VALUE} on failure
     *
     * @hide
     */
    public int getWnrLevel() {
        byte[] value = new byte[1];
        if (this.getParameter(OZO_CAPTURE_WNR_LEVEL, value) >= 0)
            return (int) value[0];

        return ERROR_BAD_VALUE;
    }

    /**
     * Query audio levels. The level range is 0...32768.
     *
     * @return Left and right channel levels on success, level values -1 indicate failure.
     *
     * @hide
     */
    public int[] getAudioLevel() {
        byte[] value = new byte[8];
        if (this.getParameter(OZO_CAPTURE_AUDIO_LEVEL, value) >= 0)
            return new int[]{byteArrayToInt(value, 0), byteArrayToInt(value, 4)};

        return new int[]{-1, -1};
    }

    /**
     * Enable noise suppression.
     *
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int enableNs() {
        return setOzoParameter(OzoParameters.FEAT_NOISESUPPRESSION, OzoParameters.ENABLED);
    }

    /**
     * Disable noise suppression.
     *
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int disableNs() {
        return setOzoParameter(OzoParameters.FEAT_NOISESUPPRESSION, OzoParameters.DISABLED);
    }

    /**
     * Enable audio focus.
     *
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int enableFocus() {
        return setOzoParameter(OzoParameters.FEAT_FOCUS, OzoParameters.ENABLED);
    }

    /**
     * Disable audio focus.
     *
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int disableFocus() {
        return setOzoParameter(OzoParameters.FEAT_FOCUS, OzoParameters.DISABLED);
    }

    /**
     * Set audio focus gain.
     *
     * @param gain Focus gain (between 0 and 5)
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int setFocusGain(double gain) {
        return setOzoParameter(OzoParameters.FEAT_ZOOM, Double.toString(gain));
    }

    /**
     * Set audio focus azimuth.
     *
     * @param gain Focus azimuth (between -180 and 180)
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int setFocusAzimuth(double azimuth) {
        return setOzoParameter(OzoParameters.FEAT_FOCUSAZIMUTH, Double.toString(azimuth));
    }

    /**
     * Set audio focus elevation.
     *
     * @param gain Focus elevation
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int setFocusElevation(double elevation) {
        return setOzoParameter(OzoParameters.FEAT_FOCUSELEVATION, Double.toString(elevation));
    }

    /**
     * Set audio focus sector width.
     *
     * @param gain Focus sector width
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int setFocusWidth(double width) {
        return setOzoParameter(OzoParameters.FEAT_FOCUSWIDTH, Double.toString(width));
    }

    /**
     * Set audio focus sector height.
     *
     * @param height Focus sector height
     * @see android.media.audiofx.OzoAudioEffect.setOzoParameter for return values.
     *
     * @hide
     */
    public int setFocusHeight(double height) {
        return setOzoParameter(OzoParameters.FEAT_FOCUSHEIGHT, Double.toString(height));
    }

    /**
     * Set parameter to Ozo processing.
     *
     * @param key Parameter ID
     * @param value Parameter value
     * @return {@link #SUCCESS} in case of success, {@link #ERROR_BAD_VALUE},
     *         {@link #ERROR_NO_MEMORY}, {@link #ERROR_INVALID_OPERATION} or
     *         {@link #ERROR_DEAD_OBJECT} in case of failure
     *
     * @hide
     */
    public int setOzoParameter(String key, String value) {
        try {
            String value2 = key + "=" + value;
            return setParameter(OZO_PARAM_GENERIC, value2.getBytes("UTF-8"));
        } catch (UnsupportedEncodingException e) {
            Log.w(TAG, "Unable to set Ozo parameter: " + key +  " " + value + " " + e);
            return ERROR_BAD_VALUE;
        }
    }

    /**
     * Class constructor.
     * <p> The constructor is not guarantied to succeed and throws the following exceptions:
     * <ul>
     *  <li>IllegalArgumentException is thrown if the device does not implement OZO Audio effect.</li>
     *  <li>UnsupportedOperationException is thrown is the resources allocated to audio
     *  pre-procesing are currently exceeded.</li>
     *  <li>RuntimeException is thrown if a memory allocation error occurs.</li>
     * </ul>
     *
     * @param audioSession system wide unique audio session identifier. The OzoAudioEffect
     * will be applied to the AudioRecord with the same audio session.
     *
     * @throws java.lang.IllegalArgumentException
     * @throws java.lang.UnsupportedOperationException
     * @throws java.lang.RuntimeException
     *
     * @hide
     */
    private OzoAudioEffect(int audioSession)
            throws IllegalArgumentException, UnsupportedOperationException, RuntimeException {
        super(OZO_EFFECT_TYPE, OZO_EFFECT_UUID, 0, audioSession);
    }
}
