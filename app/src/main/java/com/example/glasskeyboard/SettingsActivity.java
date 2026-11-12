package com.example.glasskeyboard;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.provider.Settings;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private SharedPreferences prefs;
    private float d;
    private LinearLayout root;
    private TextView step1State, step2State;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        prefs = getSharedPreferences("glass", MODE_PRIVATE);
        d = getResources().getDisplayMetrics().density;
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        GradientDrawable bg = new GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#4A2377"), Color.parseColor("#1E1730"), Color.parseColor("#14111B")});
        sv.setBackground(bg);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(18), dp(40), dp(18), dp(30));
        sv.addView(root, new ViewGroup.LayoutParams(-1, -2));
        setContentView(sv);

        TextView title = text("Glass Keyboard", 28, true);
        root.addView(title);
        root.addView(sub("كيبورد زجاجي بخلفية متحركة، بيدعم العربي والإنجليزي والإيموجي"));

        // ---- setup card
        LinearLayout setup = card();
        setup.addView(text("التفعيل", 17, true));
        step1State = sub("");
        setup.addView(step1State);
        setup.addView(button("١) فعّل الكيبورد", v -> startActivity(new Intent(Settings.ACTION_INPUT_METHOD_SETTINGS))));
        step2State = sub("");
        setup.addView(step2State);
        setup.addView(button("٢) اختار Glass Keyboard", v -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) imm.showInputMethodPicker();
        }));
        root.addView(setup);

        // ---- test field
        LinearLayout test = card();
        test.addView(text("جرّب هنا", 17, true));
        EditText et = new EditText(this);
        et.setHint("اكتب أي حاجة...");
        et.setHintTextColor(Color.parseColor("#88FFFFFF"));
        et.setTextColor(Color.WHITE);
        et.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        et.setMinLines(2);
        et.setGravity(Gravity.TOP | Gravity.START);
        et.setBackground(glass(14, 0x22FFFFFF));
        et.setPadding(dp(14), dp(12), dp(14), dp(12));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(10);
        test.addView(et, lp);
        root.addView(test);

        // ---- themes
        LinearLayout themes = card();
        themes.addView(text("الثيم", 17, true));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        hs.addView(row);
        for (int i = 0; i < KbTheme.ALL.length; i++) row.addView(swatch(i, row));
        LinearLayout.LayoutParams hl = new LinearLayout.LayoutParams(-1, -2);
        hl.topMargin = dp(12);
        themes.addView(hs, hl);
        root.addView(themes);

        // ---- options
        LinearLayout opt = card();
        opt.addView(text("الإعدادات", 17, true));
        opt.addView(toggle("اهتزاز عند الضغط", "haptic", true));
        opt.addView(toggle("صوت الضغط", "sound", false));
        opt.addView(toggle("معاينة الحرف المضغوط", "popup", true));
        opt.addView(toggle("اقتراحات الكلمات", "suggest", true));
        opt.addView(toggle("صف الأرقام فوق الحروف", "numrow", false));
        opt.addView(toggle("خلفية متحركة", "anim", true));
        opt.addView(toggle("حرف كبير تلقائي في أول الجملة", "autocap", true));
        opt.addView(toggle("مسافتين = نقطة", "dblspace", true));
        opt.addView(sub("ارتفاع الكيبورد"));
        SeekBar sb = new SeekBar(this);
        sb.setMax(40);                       // 80%..120%
        sb.setProgress(prefs.getInt("height", 100) - 80);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) {
                prefs.edit().putInt("height", 80 + p).apply();
            }
            @Override public void onStartTrackingTouch(SeekBar s) { }
            @Override public void onStopTrackingTouch(SeekBar s) { }
        });
        opt.addView(sb);
        root.addView(opt);

        LinearLayout tips = card();
        tips.addView(text("نصايح", 17, true));
        tips.addView(sub("• اضغط مطوّلاً على أي حرف عشان تطلع الحروف المشابهة والأرقام والتشكيل.\n"
                + "• اسحب صباعك على مفتاح المسافة عشان تحرّك المؤشر.\n"
                + "• اضغط على 🌐 لتغيير اللغة، ومطوّلاً لاختيار كيبورد تاني.\n"
                + "• اضغط مرتين على ⇧ لتثبيت الحروف الكبيرة.\n"
                + "• مسافتين ورا بعض بتحط نقطة."));
        root.addView(tips);
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshState();
    }

    private void refreshState() {
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        boolean enabled = false;
        if (imm != null) {
            for (InputMethodInfo i : imm.getEnabledInputMethodList()) {
                if (i.getPackageName().equals(getPackageName())) enabled = true;
            }
        }
        String cur = Settings.Secure.getString(getContentResolver(), Settings.Secure.DEFAULT_INPUT_METHOD);
        boolean selected = cur != null && cur.startsWith(getPackageName() + "/");
        step1State.setText(enabled ? "✓ الكيبورد مفعّل" : "الكيبورد لسه مش مفعّل");
        step2State.setText(selected ? "✓ هو الكيبورد الحالي" : "لسه مش الكيبورد الحالي");
    }

    // ------------------------------------------------------------------ small UI helpers
    private int dp(float v) { return (int) (v * d + 0.5f); }

    private GradientDrawable glass(float radiusDp, int color) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(dp(radiusDp));
        g.setStroke(dp(1), 0x30FFFFFF);
        return g;
    }

    private TextView text(String s, float sp, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.create("sans-serif", bold ? Typeface.BOLD : Typeface.NORMAL));
        return t;
    }

    private TextView sub(String s) {
        TextView t = text(s, 14, false);
        t.setTextColor(0xCCFFFFFF);
        t.setLineSpacing(0, 1.2f);
        t.setPadding(0, dp(6), 0, dp(4));
        return t;
    }

    private LinearLayout card() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setBackground(glass(22, 0x1AFFFFFF));
        c.setPadding(dp(18), dp(16), dp(18), dp(16));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(16);
        c.setLayoutParams(lp);
        return c;
    }

    private Button button(String label, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setTextColor(Color.WHITE);
        b.setBackground(glass(16, 0x55B65CFF));
        b.setOnClickListener(l);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, dp(48));
        lp.topMargin = dp(6);
        lp.bottomMargin = dp(8);
        b.setLayoutParams(lp);
        return b;
    }

    private View toggle(String label, final String key, boolean def) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextColor(Color.WHITE);
        s.setTextSize(15);
        s.setChecked(prefs.getBoolean(key, def));
        s.setPadding(0, dp(10), 0, dp(10));
        s.setOnCheckedChangeListener((CompoundButton v, boolean on) -> prefs.edit().putBoolean(key, on).apply());
        return s;
    }

    private View swatch(final int idx, final LinearLayout parent) {
        KbTheme t = KbTheme.get(idx);
        TextView v = new TextView(this);
        v.setText(t.name);
        v.setGravity(Gravity.CENTER);
        v.setTextSize(12);
        v.setTextColor(t.light ? Color.parseColor("#1D1B2A") : Color.WHITE);
        v.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(78), dp(78));
        lp.setMarginEnd(dp(10));
        v.setLayoutParams(lp);
        styleSwatch(v, t, prefs.getInt("theme", 0) == idx);
        v.setOnClickListener(x -> {
            prefs.edit().putInt("theme", idx).apply();
            for (int i = 0; i < parent.getChildCount(); i++) {
                styleSwatch((TextView) parent.getChildAt(i), KbTheme.get(i), i == idx);
            }
        });
        return v;
    }

    private void styleSwatch(TextView v, KbTheme t, boolean selected) {
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{t.blobA, t.bgTop, t.bgBottom});
        g.setCornerRadius(dp(18));
        g.setStroke(dp(selected ? 3 : 1), selected ? Color.WHITE : 0x40FFFFFF);
        v.setBackground(g);
        v.setText(selected ? "✓ " + t.name : t.name);
    }
}
