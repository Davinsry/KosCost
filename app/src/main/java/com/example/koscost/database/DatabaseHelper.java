package com.example.koscost.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.koscost.model.Kamar;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "KosCost.db";
    private static final int DATABASE_VERSION = 2; // Naikkan versi biar tabel baru dibuat

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabel Offline Kamar
        String sqlKamar = "CREATE TABLE tb_kamar_lokal (id_kamar INTEGER PRIMARY KEY, no_kamar TEXT, status TEXT, fasilitas TEXT, harga_bulanan DOUBLE)";
        db.execSQL(sqlKamar);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS tb_kamar_lokal");
        onCreate(db);
    }

    // --- FITUR OFFLINE ---

    // 1. Simpan data dari Server ke Lokal
    public void simpanKamarOffline(List<Kamar> listKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM tb_kamar_lokal"); // Bersihkan data lama

        db.beginTransaction();
        try {
            for (Kamar k : listKamar) {
                ContentValues values = new ContentValues();
                // Pastikan id_kamar dari server dikonversi ke int jika perlu, atau simpan string
                // Di sini kita pakai ID unik untuk lokal
                values.put("no_kamar", k.getNoKamar());
                values.put("status", k.getStatus());
                values.put("fasilitas", k.getFasilitas());
                values.put("harga_bulanan", k.getHargaBulanan());
                db.insert("tb_kamar_lokal", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // 2. Ambil data Lokal saat Offline
    public List<Kamar> getKamarOffline() {
        List<Kamar> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM tb_kamar_lokal ORDER BY no_kamar ASC", null);

        if (c.moveToFirst()) {
            do {
                Kamar k = new Kamar();
                // Di model Kamar, pastikan tipe datanya cocok
                k.setNoKamar(c.getString(c.getColumnIndexOrThrow("no_kamar")));
                k.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
                k.setFasilitas(c.getString(c.getColumnIndexOrThrow("fasilitas")));
                k.setHargaBulanan(c.getDouble(c.getColumnIndexOrThrow("harga_bulanan")));
                list.add(k);
            } while (c.moveToNext());
        }
        c.close();
        return list;
    }
}