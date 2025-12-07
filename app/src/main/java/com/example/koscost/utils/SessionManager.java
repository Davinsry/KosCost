package com.example.koscost.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "KosCostSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMAIL = "email";

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Fungsi untuk membuat sesi login
    public void createLoginSession(String email) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_EMAIL, email);
        editor.commit(); // Simpan perubahan
    }

    // Cek apakah user sudah login?
    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    // Ambil email yang sedang login
    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    // Fungsi Logout (Hapus Data)
    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}