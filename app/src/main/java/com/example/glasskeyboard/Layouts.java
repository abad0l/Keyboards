package com.example.glasskeyboard;

import java.util.ArrayList;
import java.util.List;

/** Keyboard layouts (English / Arabic / symbols). */
public final class Layouts {

    public static final int CODE_SHIFT = -1, CODE_DEL = -2, CODE_SYM = -3, CODE_EMOJI = -4,
            CODE_ENTER = -5, CODE_SYM2 = -6, CODE_ABC = -7, CODE_SPACE = 32;

    public static final int T_CHAR = 0, T_FUNC = 1, T_SPACE = 2, T_ENTER = 3;

    public static final class Key {
        public final String label;
        public final int code;
        public final float weight;
        public final int type;
        public final String alts;     // long-press alternatives, one char (or string) per element, space separated
        // filled by the view during layout
        public float x, y, w, h;

        Key(String label, int code, float weight, int type, String alts) {
            this.label = label; this.code = code; this.weight = weight; this.type = type; this.alts = alts;
        }
    }

    private static Key c(String s) { return new Key(s, s.codePointAt(0), 1f, T_CHAR, null); }
    private static Key c(String s, String alts) { return new Key(s, s.codePointAt(0), 1f, T_CHAR, alts); }
    private static Key f(String label, int code, float w) { return new Key(label, code, w, T_FUNC, null); }

    private static List<Key> row(String chars, String[] alts) {
        List<Key> r = new ArrayList<>();
        int i = 0;
        for (String s : chars.split(" ")) {
            r.add(alts != null && i < alts.length && alts[i] != null ? c(s, alts[i]) : c(s));
            i++;
        }
        return r;
    }

    /** page: 0 = letters, 1 = symbols, 2 = more symbols. */
    public static List<List<Key>> get(boolean arabic, int page, boolean numberRow) {
        List<List<Key>> rows = new ArrayList<>();
        if (page == 0) {
            if (numberRow) {
                rows.add(row(arabic ? "١ ٢ ٣ ٤ ٥ ٦ ٧ ٨ ٩ ٠" : "1 2 3 4 5 6 7 8 9 0", null));
            }
            if (arabic) {
                String[] a1 = {"1 ١ ّ", "2 ٢ َ", "3 ٣ ً", "4 ٤ ُ", "5 ٥ ٌ", "6 ٦ ِ", "7 ٧ ٍ", "8 ٨ ْ", "9 ٩ ـ", "0 ٠", null};
                if (numberRow) { a1 = new String[]{"ّ", "َ", "ً", "ُ", "ٌ", "ِ", "ٍ", "ْ", "ـ", null, null}; }
                rows.add(row("ض ص ث ق ف غ ع ه خ ح ج", a1));
                rows.add(row("ش س ي ب ل ا ت ن م ك ط",
                        new String[]{null, null, "ئ", null, null, "أ إ آ ء", null, null, null, null, "ظ"}));
                List<Key> r3 = new ArrayList<>();
                r3.add(new Key("لا", 0x0644, 1.35f, T_CHAR, "لأ لإ لآ"));
                r3.addAll(row("ذ ء ؤ ر ى ة و ز د", new String[]{null, null, null, null, "ي", "ه", null, null, null}));
                r3.add(f("⌫", CODE_DEL, 1.35f));
                rows.add(r3);
            } else {
                if (!numberRow) {
                    rows.add(row("q w e r t y u i o p", new String[]{"1", "2", "3 è é ê ë", "4 ŕ", "5 ţ", "6 ý ÿ", "7 û ü ù ú", "8 î ï í ì", "9 ö ô ò ó õ", "0"}));
                } else {
                    rows.add(row("q w e r t y u i o p", new String[]{null, null, "è é ê ë", null, null, "ý ÿ", "û ü ù ú", "î ï í ì", "ö ô ò ó õ", null}));
                }
                rows.add(row("a s d f g h j k l", new String[]{"à á â ä ã å", "ß §", null, null, null, null, null, null, "ł"}));
                List<Key> r3 = new ArrayList<>();
                r3.add(f("⇧", CODE_SHIFT, 1.45f));
                r3.addAll(row("z x c v b n m", new String[]{null, null, "ç ć č", null, null, "ñ ń", null}));
                r3.add(f("⌫", CODE_DEL, 1.45f));
                rows.add(r3);
            }
        } else if (page == 1) {
            rows.add(row(arabic ? "١ ٢ ٣ ٤ ٥ ٦ ٧ ٨ ٩ ٠" : "1 2 3 4 5 6 7 8 9 0", null));
            rows.add(row(arabic ? "- / : ؛ ( ) $ & @ \"" : "- / : ; ( ) $ & @ \"", null));
            List<Key> r3 = new ArrayList<>();
            r3.add(f("#+=", CODE_SYM2, 1.45f));
            r3.addAll(row(arabic ? ". ، ؟ ! ' %" : ". , ? ! ' %", null));
            r3.add(f("⌫", CODE_DEL, 1.45f));
            rows.add(r3);
        } else {
            rows.add(row("[ ] { } # % ^ * + =", null));
            rows.add(row("_ \\ | ~ < > € £ ¥ •", null));
            List<Key> r3 = new ArrayList<>();
            r3.add(f("123", CODE_SYM, 1.45f));
            r3.addAll(row(". , ? ! ' %", null));
            r3.add(f("⌫", CODE_DEL, 1.45f));
            rows.add(r3);
        }
        // bottom row
        List<Key> b = new ArrayList<>();
        b.add(f(page == 0 ? "123" : "ABC", page == 0 ? CODE_SYM : CODE_ABC, 1.35f));
        b.add(f("☺", CODE_EMOJI, 1.1f));
        if (arabic) {
            b.add(c("،", "؛ ؟ ! . : ,"));
        } else {
            b.add(c(",", "; : ! ? ' \""));
        }
        b.add(new Key("space", CODE_SPACE, 4.2f, T_SPACE, null));
        b.add(c(".", ", ? ! : ; …"));
        b.add(new Key("return", CODE_ENTER, 1.9f, T_ENTER, null));
        rows.add(b);
        return rows;
    }

    // ------------------------------------------------------------------ emoji
    public static final String[] EMOJI_TABS = {"😀", "❤️", "👋", "🐱", "🍔", "⚽", "🚗", "💡", "🔣"};

    public static final String[][] EMOJI = {
        {"😀","😃","😄","😁","😆","😅","😂","🤣","😊","😇","🙂","🙃","😉","😌","😍","🥰","😘","😗","😙","😚","😋","😛","😜","🤪","😝","🤑","🤗","🤭","🤫","🤔","🤐","😐","😑","😶","😏","😒","🙄","😬","😮‍💨","🤥","😔","😪","🤤","😴","😷","🤒","🤕","🤢","🤮","🤧","🥵","🥶","😵","🤯","🤠","🥳","😎","🤓","🧐","😕","😟","🙁","☹️","😮","😯","😲","😳","🥺","😦","😧","😨","😰","😥","😢","😭","😱","😖","😣","😞","😓","😩","😫","🥱","😤","😡","😠","🤬","😈","👿","💀","💩","🤡","👻","👽","🤖"},
        {"❤️","🧡","💛","💚","💙","💜","🖤","🤍","🤎","💔","❣️","💕","💞","💓","💗","💖","💘","💝","💟","♥️","😻","💑","💏","🌹","🥀","🌸","💐","🎁","💋","✨","💫","⭐","🌟","🔥","💥","💯","🎉","🎊","🥂","🍾"},
        {"👍","👎","👌","✌️","🤞","🤟","🤘","🤙","👈","👉","👆","👇","☝️","✋","🤚","🖐️","🖖","👋","🤝","🙏","💪","👏","🙌","👐","🤲","✍️","💅","🤳","🙋","🙆","🙅","🤷","🤦","🧏","👀","👁️","👅","👄","🧠","🦾"},
        {"🐱","🐶","🐭","🐹","🐰","🦊","🐻","🐼","🐨","🐯","🦁","🐮","🐷","🐸","🐵","🙈","🙉","🙊","🐔","🐧","🐦","🐤","🦆","🦅","🦉","🦇","🐺","🐗","🐴","🦄","🐝","🐛","🦋","🐌","🐞","🐢","🐍","🦎","🐙","🦑","🦀","🐠","🐬","🐳","🦈","🐘","🦒","🐫","🌵","🌲","🌴","🌱","🍀","🌻","🌞","🌙","⭐","☁️","⚡","❄️","🌈"},
        {"🍏","🍎","🍐","🍊","🍋","🍌","🍉","🍇","🍓","🍒","🍑","🥭","🍍","🥥","🥝","🍅","🥑","🍆","🥕","🌽","🌶️","🥔","🍞","🧀","🍳","🥞","🥓","🍔","🍟","🍕","🌭","🌮","🌯","🥙","🍝","🍜","🍲","🍣","🍤","🍦","🍩","🍪","🎂","🍰","🍫","🍬","🍭","☕","🍵","🥤","🍺","🍷"},
        {"⚽","🏀","🏈","⚾","🎾","🏐","🏉","🎱","🏓","🏸","🥊","🥋","⛳","🏹","🎣","🎮","🎲","🧩","🎯","🎳","🎤","🎧","🎸","🎹","🥁","🎬","🏆","🥇","🥈","🥉","🏅","🚴","🏊","🏃","🧘","⛷️","🏄"},
        {"🚗","🚕","🚙","🚌","🏎️","🚓","🚑","🚒","🚚","🚜","🏍️","🚲","🛵","🚂","✈️","🚀","🛸","🚁","⛵","🚤","🚢","⚓","🏠","🏢","🏥","🏫","🕌","⛪","🗼","🗽","🌍","🌎","🌏","🗺️","⛰️","🏖️","🏝️","🌋"},
        {"💡","📱","💻","⌨️","🖥️","🖨️","📷","📹","📺","📻","⏰","⌛","🔋","🔌","💰","💳","💎","🔧","🔨","⚙️","🧲","🔑","🔒","🔓","📌","📎","✂️","📝","📚","📖","📅","📊","📈","📉","✉️","📦","🎁","🧹","🧼","🛒"},
        {"✅","❌","⭕","❗","❓","‼️","⚠️","🚫","🔞","♻️","➕","➖","➗","✖️","➡️","⬅️","⬆️","⬇️","↗️","↘️","🔄","🔁","🔔","🔕","🔊","🔇","📣","💬","💭","🗯️","©️","®️","™️","🆗","🆕","🆒","🔝","🔜","🕌","☪️"},
    };
}
