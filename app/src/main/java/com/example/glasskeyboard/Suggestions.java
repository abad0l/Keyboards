package com.example.glasskeyboard;

import android.content.Context;
import android.content.SharedPreferences;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Tiny prefix-based suggestion engine with on-device learning. */
public final class Suggestions {

    private final List<String> en = new ArrayList<>();
    private final List<String> ar = new ArrayList<>();
    private final Map<String, Integer> learned = new HashMap<>();
    private final SharedPreferences prefs;

    public Suggestions(Context ctx) {
        prefs = ctx.getSharedPreferences("glass_words", Context.MODE_PRIVATE);
        load(ctx, "en.txt", en);
        load(ctx, "ar.txt", ar);
        for (Map.Entry<String, ?> e : prefs.getAll().entrySet()) {
            if (e.getValue() instanceof Integer) learned.put(e.getKey(), (Integer) e.getValue());
        }
    }

    private static void load(Context ctx, String name, List<String> out) {
        try (BufferedReader r = new BufferedReader(new InputStreamReader(ctx.getAssets().open(name), "UTF-8"))) {
            String line;
            LinkedHashMap<String, Boolean> seen = new LinkedHashMap<>();
            while ((line = r.readLine()) != null) {
                for (String w : line.trim().split("\\s+")) {
                    if (w.length() > 0) seen.put(w, true);
                }
            }
            out.addAll(seen.keySet());
        } catch (Exception ignored) { }
    }

    /** Best completions for the given prefix (never includes the prefix itself as first choice). */
    public List<String> complete(String prefix, boolean arabic, int max) {
        List<String> res = new ArrayList<>();
        if (prefix == null || prefix.isEmpty()) return res;
        String p = prefix.toLowerCase();
        // learned words first, strongest first
        List<Map.Entry<String, Integer>> hits = new ArrayList<>();
        for (Map.Entry<String, Integer> e : learned.entrySet()) {
            String k = e.getKey();
            if (isArabic(k) != arabic) continue;
            if (k.toLowerCase().startsWith(p) && !k.equalsIgnoreCase(prefix)) hits.add(e);
        }
        java.util.Collections.sort(hits, (a, b) -> b.getValue() - a.getValue());
        for (Map.Entry<String, Integer> e : hits) {
            if (res.size() >= max) break;
            res.add(e.getKey());
        }
        List<String> dict = arabic ? ar : en;
        for (String w : dict) {
            if (res.size() >= max) break;
            if (w.toLowerCase().startsWith(p) && !w.equalsIgnoreCase(prefix) && !res.contains(w)) res.add(w);
        }
        // match capitalisation of what the user typed
        if (!arabic && prefix.length() > 0 && Character.isUpperCase(prefix.charAt(0))) {
            for (int i = 0; i < res.size(); i++) {
                String w = res.get(i);
                res.set(i, Character.toUpperCase(w.charAt(0)) + w.substring(1));
            }
        }
        return res;
    }

    /** Words offered when nothing has been typed yet. */
    public List<String> starters(boolean arabic) {
        List<String> l = new ArrayList<>();
        if (arabic) { l.add("ازيك"); l.add("شكرا"); l.add("تمام"); }
        else { l.add("I"); l.add("The"); l.add("Thanks"); }
        return l;
    }

    public void learn(String word) {
        if (word == null || word.length() < 2) return;
        Integer n = learned.get(word);
        int v = n == null ? 1 : n + 1;
        learned.put(word, v);
        prefs.edit().putInt(word, v).apply();
    }

    public static boolean isArabic(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 0x0600 && c <= 0x06FF) return true;
        }
        return false;
    }
}
