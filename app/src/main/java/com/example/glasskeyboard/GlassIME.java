package com.example.glasskeyboard;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.inputmethodservice.InputMethodService;
import android.media.AudioManager;
import android.os.Build;
import android.os.SystemClock;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.text.InputType;
import android.text.TextUtils;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.view.inputmethod.InputMethodSubtype;
import android.widget.Toast;

import java.util.List;
import java.util.Map;

public class GlassIME extends InputMethodService implements GlassKeyboardView.Listener {

    private GlassKeyboardView view;
    private Suggestions dict;
    private SharedPreferences prefs;

    private boolean arabic, numberRow, haptic, sound, suggestionsOn = true, autoCap = true, doubleSpace = true;
    private int page, shift;
    private long lastShiftTap, lastSpace;

    // per-field state
    private boolean noSuggest, numericField;
    private int enterAction;               // 0 = plain newline
    private final String[] sugText = new String[3];

    @Override
    public void onCreate() {
        super.onCreate();
        prefs = getSharedPreferences("glass", MODE_PRIVATE);
        dict = new Suggestions(this);
        arabic = prefs.getBoolean("lastAr", false);
    }

    @Override
    public View onCreateInputView() {
        view = new GlassKeyboardView(this);
        view.setListener(this);
        applyPrefs();
        rebuild();
        return view;
    }

    @Override
    public boolean onEvaluateFullscreenMode() { return false; }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        applyPrefs();
    }

    private void applyPrefs() {
        haptic = prefs.getBoolean("haptic", true);
        sound = prefs.getBoolean("sound", false);
        suggestionsOn = prefs.getBoolean("suggest", true);
        autoCap = prefs.getBoolean("autocap", true);
        doubleSpace = prefs.getBoolean("dblspace", true);
        numberRow = prefs.getBoolean("numrow", false);
        if (view == null) return;
        view.setKbTheme(KbTheme.get(prefs.getInt("theme", 0)));
        view.setPopupEnabled(prefs.getBoolean("popup", true));
        view.setAnimated(prefs.getBoolean("anim", true));
        float scale = prefs.getInt("height", 100) / 100f;
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) scale *= 0.75f;
        view.setHeightScale(scale);
    }

    @Override
    public void onStartInputView(EditorInfo info, boolean restarting) {
        super.onStartInputView(info, restarting);
        applyPrefs();
        int cls = info.inputType & InputType.TYPE_MASK_CLASS;
        int var = info.inputType & InputType.TYPE_MASK_VARIATION;
        numericField = cls == InputType.TYPE_CLASS_NUMBER || cls == InputType.TYPE_CLASS_PHONE
                || cls == InputType.TYPE_CLASS_DATETIME;
        noSuggest = (info.inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0 || numericField;
        if (cls == InputType.TYPE_CLASS_TEXT) {
            if (var == InputType.TYPE_TEXT_VARIATION_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
                    || var == InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS
                    || var == InputType.TYPE_TEXT_VARIATION_URI) noSuggest = true;
        }
        // enter key behaviour
        int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean noAction = (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        String label = arabic ? "إدخال" : "return";
        boolean accent = true;
        enterAction = 0;
        if (!noAction) {
            switch (action) {
                case EditorInfo.IME_ACTION_GO: enterAction = action; label = arabic ? "اذهب" : "go"; break;
                case EditorInfo.IME_ACTION_SEARCH: enterAction = action; label = arabic ? "بحث" : "search"; break;
                case EditorInfo.IME_ACTION_SEND: enterAction = action; label = arabic ? "إرسال" : "send"; break;
                case EditorInfo.IME_ACTION_NEXT: enterAction = action; label = arabic ? "التالي" : "next"; break;
                case EditorInfo.IME_ACTION_DONE: enterAction = action; label = arabic ? "تم" : "done"; break;
                case EditorInfo.IME_ACTION_PREVIOUS: enterAction = action; label = arabic ? "السابق" : "prev"; break;
                default: accent = false;
            }
        } else accent = false;
        if (view != null) {
            view.setEnterLabel(label, accent);
            view.setEmojiMode(false);
        }
        page = numericField ? 1 : 0;
        shift = 0;
        rebuild();
        updateAutoShift();
        refreshSuggestions();
    }

    @Override
    public void onUpdateSelection(int oldSs, int oldSe, int newSs, int newSe, int cs, int ce) {
        super.onUpdateSelection(oldSs, oldSe, newSs, newSe, cs, ce);
        updateAutoShift();
        refreshSuggestions();
    }

    private void rebuild() {
        if (view == null) return;
        view.setLayout(Layouts.get(arabic, page, numberRow), arabic, page);
        view.setShift(shift);
    }

    // ------------------------------------------------------------------ key handling
    @Override
    public void onKey(int code, String text) {
        switch (code) {
            case Layouts.CODE_DEL: handleDelete(); return;
            case Layouts.CODE_SHIFT: handleShift(); return;
            case Layouts.CODE_SYM: page = 1; rebuild(); return;
            case Layouts.CODE_SYM2: page = 2; rebuild(); return;
            case Layouts.CODE_ABC:
                if (view != null) view.setEmojiMode(false);
                page = 0; rebuild(); updateAutoShift(); return;
            case Layouts.CODE_EMOJI:
                if (view != null) view.setEmojiMode(true);
                return;
            case Layouts.CODE_ENTER: handleEnter(); return;
        }
        if (text != null) typeText(text);
    }

    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (text.equals(" ")) { handleSpace(ic); return; }
        boolean wordBreak = text.length() == 1 && !Character.isLetterOrDigit(text.charAt(0))
                && Character.getType(text.charAt(0)) != Character.NON_SPACING_MARK && text.charAt(0) != '\'';
        if (wordBreak) learnCurrentWord(ic);
        // keep punctuation tight: "word ." -> "word."
        if (wordBreak && ".,!?;:،؛؟".indexOf(text.charAt(0)) >= 0) {
            CharSequence b = ic.getTextBeforeCursor(2, 0);
            if (b != null && b.length() == 2 && b.charAt(1) == ' ' && Character.isLetterOrDigit(b.charAt(0))
                    && lastSpace > 0) {
                ic.deleteSurroundingText(1, 0);
            }
        }
        ic.commitText(text, 1);
        lastSpace = 0;
        if (shift == 1 && page == 0) { shift = 0; if (view != null) view.setShift(0); }
        updateAutoShift();
        refreshSuggestions();
    }

    private void handleSpace(InputConnection ic) {
        long now = SystemClock.uptimeMillis();
        CharSequence b = ic.getTextBeforeCursor(2, 0);
        if (doubleSpace && lastSpace > 0 && now - lastSpace < 450 && b != null && b.length() == 2
                && b.charAt(1) == ' ' && Character.isLetterOrDigit(b.charAt(0))) {
            ic.deleteSurroundingText(1, 0);
            ic.commitText(". ", 1);
            lastSpace = 0;
        } else {
            learnCurrentWord(ic);
            ic.commitText(" ", 1);
            lastSpace = now;
        }
        updateAutoShift();
        refreshSuggestions();
    }

    private void handleDelete() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        CharSequence sel = ic.getSelectedText(0);
        if (!TextUtils.isEmpty(sel)) {
            ic.commitText("", 1);
        } else {
            CharSequence b = ic.getTextBeforeCursor(2, 0);
            if (b == null || b.length() == 0) {
                sendDownUpKeyEvents(KeyEvent.KEYCODE_DEL);
            } else {
                int n = (b.length() == 2 && Character.isHighSurrogate(b.charAt(0))
                        && Character.isLowSurrogate(b.charAt(1))) ? 2 : 1;
                ic.deleteSurroundingText(n, 0);
            }
        }
        lastSpace = 0;
        updateAutoShift();
        refreshSuggestions();
    }

    private void handleEnter() {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        learnCurrentWord(ic);
        if (enterAction != 0) ic.performEditorAction(enterAction);
        else sendDownUpKeyEvents(KeyEvent.KEYCODE_ENTER);
        lastSpace = 0;
        updateAutoShift();
        refreshSuggestions();
    }

    private void handleShift() {
        long now = SystemClock.uptimeMillis();
        if (shift == 2) shift = 0;
        else if (shift == 1 && now - lastShiftTap < 350) shift = 2;
        else shift = shift == 0 ? 1 : 0;
        lastShiftTap = now;
        if (view != null) view.setShift(shift);
    }

    private void updateAutoShift() {
        if (!autoCap || arabic || page != 0 || shift == 2 || view == null) return;
        InputConnection ic = getCurrentInputConnection();
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ic == null || ei == null || ei.inputType == 0) return;
        int caps = ic.getCursorCapsMode(ei.inputType);
        int want = caps != 0 ? 1 : 0;
        if (want != shift) { shift = want; view.setShift(shift); }
    }

    // ------------------------------------------------------------------ suggestions
    private static boolean isWordChar(char ch) {
        return Character.isLetter(ch) || ch == '\''
                || Character.getType(ch) == Character.NON_SPACING_MARK;
    }

    private String currentWord(InputConnection ic) {
        CharSequence b = ic.getTextBeforeCursor(40, 0);
        if (b == null) return "";
        int i = b.length();
        while (i > 0 && isWordChar(b.charAt(i - 1))) i--;
        return b.subSequence(i, b.length()).toString();
    }

    private void learnCurrentWord(InputConnection ic) {
        if (noSuggest || !suggestionsOn) return;
        String w = currentWord(ic);
        if (w.length() >= 3) dict.learn(w);
    }

    private void refreshSuggestions() {
        if (view == null) return;
        sugText[0] = sugText[1] = sugText[2] = null;
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || !suggestionsOn || noSuggest) { view.setSuggestions(null, null, null); return; }
        String word = currentWord(ic);
        if (word.isEmpty()) {
            CharSequence b = ic.getTextBeforeCursor(1, 0);
            if (b == null || b.length() == 0 || b.charAt(0) == ' ' || b.charAt(0) == '\n') {
                List<String> s = dict.starters(arabic);
                for (int i = 0; i < 3 && i < s.size(); i++) sugText[i] = s.get(i);
            }
            view.setSuggestions(sugText[0], sugText[1], sugText[2]);
            return;
        }
        List<String> c = dict.complete(word, Suggestions.isArabic(word), 2);
        if (c.isEmpty()) {
            sugText[1] = word;
            view.setSuggestions(null, word, null);
            return;
        }
        sugText[0] = word;
        sugText[1] = c.get(0);
        if (c.size() > 1) sugText[2] = c.get(1);
        view.setSuggestions("\"" + word + "\"", sugText[1], sugText[2]);
    }

    @Override
    public void onSuggestion(int slot) {
        InputConnection ic = getCurrentInputConnection();
        if (ic == null || slot < 0 || slot > 2 || sugText[slot] == null) return;
        String word = currentWord(ic);
        if (!word.isEmpty()) ic.deleteSurroundingText(word.length(), 0);
        ic.commitText(sugText[slot] + " ", 1);
        dict.learn(sugText[slot]);
        lastSpace = SystemClock.uptimeMillis();
        updateAutoShift();
        refreshSuggestions();
    }

    // ------------------------------------------------------------------ misc keyboard actions
    @Override
    public void onDeleteRepeat() { handleDelete(); feedbackQuiet(); }

    @Override
    public void onCursorMove(int delta) {
        sendDownUpKeyEvents(delta > 0 ? KeyEvent.KEYCODE_DPAD_RIGHT : KeyEvent.KEYCODE_DPAD_LEFT);
        feedbackQuiet();
        refreshSuggestions();
    }

    @Override
    public void onGlobe(boolean longPress) {
        if (longPress) {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
            return;
        }
        arabic = !arabic;
        prefs.edit().putBoolean("lastAr", arabic).apply();
        page = numericField ? 1 : 0;
        shift = 0;
        EditorInfo ei = getCurrentInputEditorInfo();
        if (ei != null) onStartInputViewKeepState(ei);
        rebuild();
        updateAutoShift();
        refreshSuggestions();
    }

    /** Re-evaluates the enter label (it is localised) without resetting the layout. */
    private void onStartInputViewKeepState(EditorInfo info) {
        int action = info.imeOptions & EditorInfo.IME_MASK_ACTION;
        boolean noAction = (info.imeOptions & EditorInfo.IME_FLAG_NO_ENTER_ACTION) != 0;
        String label = arabic ? "إدخال" : "return";
        boolean accent = false;
        if (!noAction) {
            switch (action) {
                case EditorInfo.IME_ACTION_GO: label = arabic ? "اذهب" : "go"; accent = true; break;
                case EditorInfo.IME_ACTION_SEARCH: label = arabic ? "بحث" : "search"; accent = true; break;
                case EditorInfo.IME_ACTION_SEND: label = arabic ? "إرسال" : "send"; accent = true; break;
                case EditorInfo.IME_ACTION_NEXT: label = arabic ? "التالي" : "next"; accent = true; break;
                case EditorInfo.IME_ACTION_DONE: label = arabic ? "تم" : "done"; accent = true; break;
                case EditorInfo.IME_ACTION_PREVIOUS: label = arabic ? "السابق" : "prev"; accent = true; break;
            }
        }
        if (view != null) view.setEnterLabel(label, accent);
    }

    @Override
    public void onMic() {
        try {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            Map<InputMethodInfo, List<InputMethodSubtype>> map = imm.getShortcutInputMethodsAndSubtypes();
            for (Map.Entry<InputMethodInfo, List<InputMethodSubtype>> e : map.entrySet()) {
                for (InputMethodSubtype st : e.getValue()) {
                    if ("voice".equals(st.getMode())) {
                        if (Build.VERSION.SDK_INT >= 28) {
                            switchInputMethod(e.getKey().getId(), st);
                        } else {
                            imm.setInputMethodAndSubtype(getWindow().getWindow().getAttributes().token,
                                    e.getKey().getId(), st);
                        }
                        return;
                    }
                }
            }
        } catch (Throwable ignored) { }
        Toast.makeText(this, arabic ? "مفيش كيبورد صوتي متاح على الموبايل" : "No voice input available", Toast.LENGTH_SHORT).show();
    }

    // ------------------------------------------------------------------ feedback
    @Override
    public void onFeedback() {
        if (haptic) buzz(9);
        if (sound) {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) am.playSoundEffect(AudioManager.FX_KEYPRESS_STANDARD, 0.5f);
        }
    }

    private void feedbackQuiet() { if (haptic) buzz(5); }

    @SuppressWarnings("deprecation")
    private void buzz(int ms) {
        try {
            Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
            if (v == null || !v.hasVibrator()) return;
            v.vibrate(VibrationEffect.createOneShot(ms, VibrationEffect.DEFAULT_AMPLITUDE));
        } catch (Throwable ignored) { }
    }
}
