package com.example.koscost.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class SessionManager {
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;
    private static final String PREF_NAME = "KosCostSession";
    private static final String KEY_IS_LOGGED_IN = "isLoggedIn";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAMA_KOS = "nama_kos"; // <--- TAMBAH INI

    public SessionManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    // Update fungsi Login: Terima namaKos juga
    public void createLoginSession(String email, String namaKos) {
        editor.putBoolean(KEY_IS_LOGGED_IN, true);
        editor.putString(KEY_EMAIL, email);
        editor.putString(KEY_NAMA_KOS, namaKos); // Simpan Nama Kos
        editor.commit();
    }

    public boolean isLoggedIn() {
        return sharedPreferences.getBoolean(KEY_IS_LOGGED_IN, false);
    }

    public String getEmail() {
        return sharedPreferences.getString(KEY_EMAIL, null);
    }

    // Fungsi Ambil Nama Kos (Untuk Kuitansi)
    public String getNamaKos() {
        return sharedPreferences.getString(KEY_NAMA_KOS, "Nama Kos Belum Diatur");
    }

    public void logoutUser() {
        editor.clear();
        editor.commit();
    }
}