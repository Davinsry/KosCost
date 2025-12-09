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
    private static final int DATABASE_VERSION = 3; // NAIKKAN VERSI KE 3 (PENTING!)

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Tabel Cache Kamar (Dashboard Offline)
        String sqlKamar = "CREATE TABLE tb_kamar_lokal (id_kamar INTEGER PRIMARY KEY, no_kamar TEXT, status TEXT, fasilitas TEXT, harga_bulanan DOUBLE)";
        db.execSQL(sqlKamar);

        // 2. TABEL BARU: Antrean Tambah Kamar (Pending)
        String sqlPendingKamar = "CREATE TABLE tb_pending_kamar (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, no_kamar TEXT, fasilitas TEXT, harian DOUBLE, mingguan DOUBLE, bulanan DOUBLE)";
        db.execSQL(sqlPendingKamar);

        // 3. TABEL BARU: Antrean Sewa/Check-In (Pending)
        String sqlPendingSewa = "CREATE TABLE tb_pending_sewa (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, no_kamar TEXT, nama TEXT, wa TEXT, kerja TEXT, durasi TEXT, total DOUBLE, status TEXT)";
        db.execSQL(sqlPendingSewa);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Hapus tabel lama jika update versi
        db.execSQL("DROP TABLE IF EXISTS tb_kamar_lokal");
        db.execSQL("DROP TABLE IF EXISTS tb_pending_kamar");
        db.execSQL("DROP TABLE IF EXISTS tb_pending_sewa");
        onCreate(db);
    }

    // --- FITUR 1: SIMPAN CACHE DARI SERVER ---
    public void simpanKamarOffline(List<Kamar> listKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM tb_kamar_lokal"); // Bersihkan data lama

        db.beginTransaction();
        try {
            for (Kamar k : listKamar) {
                ContentValues values = new ContentValues();
                // Pastikan tipe data id_kamar sesuai (String/Int), disini kita generate ID lokal atau simpan apa adanya
                // Jika dari server id_kamar string, simpan di kolom lain atau abaikan di local PK
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

    public List<Kamar> getKamarOffline() {
        List<Kamar> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM tb_kamar_lokal ORDER BY no_kamar ASC", null);

        if (c.moveToFirst()) {
            do {
                Kamar k = new Kamar();
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

    // --- FITUR 2: SIMPAN DATA PENDING (SAAT OFFLINE) ---

    // Tambah Antrean Kamar
    public void addPendingKamar(String email, String no, String fas, double h, double m, double b) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("no_kamar", no);
        values.put("fasilitas", fas);
        values.put("harian", h);
        values.put("mingguan", m);
        values.put("bulanan", b);
        db.insert("tb_pending_kamar", null, values);
    }

    // Ambil Antrean Kamar
    public Cursor getPendingKamar() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_kamar", null);
    }

    // Hapus Antrean Kamar (Setelah sukses kirim)
    public void clearPendingKamar() {
        this.getWritableDatabase().execSQL("DELETE FROM tb_pending_kamar");
    }

    // Tambah Antrean Sewa
    public void addPendingSewa(String email, String no, String nama, String wa, String kerja, String durasi, double total, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("email", email);
        values.put("no_kamar", no);
        values.put("nama", nama);
        values.put("wa", wa);
        values.put("kerja", kerja);
        values.put("durasi", durasi);
        values.put("total", total);
        values.put("status", status);
        db.insert("tb_pending_sewa", null, values);
    }

    public Cursor getPendingSewa() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_sewa", null);
    }

    public void clearPendingSewa() {
        this.getWritableDatabase().execSQL("DELETE FROM tb_pending_sewa");
    }
}