package com.example.koscost.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.koscost.model.Kamar;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "KosCost.db";
    private static final int DATABASE_VERSION = 5;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Cache & Pending Input
        db.execSQL("CREATE TABLE tb_kamar_lokal (id_kamar INTEGER PRIMARY KEY, no_kamar TEXT, status TEXT, fasilitas TEXT, harga_bulanan DOUBLE, nama_penghuni TEXT, id_sewa TEXT)");
        db.execSQL("CREATE TABLE tb_pending_kamar (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, no_kamar TEXT, fasilitas TEXT, harian DOUBLE, mingguan DOUBLE, bulanan DOUBLE)");
        db.execSQL("CREATE TABLE tb_pending_sewa (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, no_kamar TEXT, nama TEXT, wa TEXT, kerja TEXT, durasi TEXT, total DOUBLE, status TEXT, tgl_in TEXT, tgl_out TEXT)");

        // 2. TABEL BARU: Pending Update & Checkout
        db.execSQL("CREATE TABLE tb_pending_update_sewa (id INTEGER PRIMARY KEY AUTOINCREMENT, id_sewa TEXT, nama TEXT, wa TEXT, kerja TEXT, tgl_in TEXT, tgl_out TEXT, durasi TEXT, total DOUBLE, bayar DOUBLE, status TEXT)");
        db.execSQL("CREATE TABLE tb_pending_checkout (id INTEGER PRIMARY KEY AUTOINCREMENT, no_kamar TEXT)");
        db.execSQL("CREATE TABLE tb_pending_edit_kamar (id INTEGER PRIMARY KEY AUTOINCREMENT, id_kamar TEXT, no_kamar TEXT, fasilitas TEXT, harian DOUBLE, mingguan DOUBLE, bulanan DOUBLE)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS tb_kamar_lokal");
        db.execSQL("DROP TABLE IF EXISTS tb_pending_kamar");
        db.execSQL("DROP TABLE IF EXISTS tb_pending_sewa");
        db.execSQL("DROP TABLE IF EXISTS tb_pending_update_sewa");
        db.execSQL("DROP TABLE IF EXISTS tb_pending_checkout");
        db.execSQL("DROP TABLE IF EXISTS tb_pending_edit_kamar");
        onCreate(db);
    }

    // --- HELPER UNTUK OFFLINE ACTION ---

    public void addPendingUpdateSewa(String idSewa, String nama, String wa, String kerja, String tglIn, String tglOut, String durasi, double total, double bayar, String status, String noKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_sewa", idSewa);
        values.put("nama", nama);
        values.put("wa", wa);
        values.put("kerja", kerja);
        values.put("tgl_in", tglIn);
        values.put("tgl_out", tglOut);
        values.put("durasi", durasi);
        values.put("total", total);
        values.put("bayar", bayar);
        values.put("status", status);
        db.insert("tb_pending_update_sewa", null, values);

        // Update Cache Lokal
        ContentValues updateCache = new ContentValues();
        updateCache.put("nama_penghuni", nama);
        db.update("tb_kamar_lokal", updateCache, "no_kamar = ?", new String[]{noKamar});
    }

    public void addPendingCheckout(String noKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("no_kamar", noKamar);
        db.insert("tb_pending_checkout", null, values);

        // Update Cache Lokal (Kosongkan)
        ContentValues updateCache = new ContentValues();
        updateCache.put("status", "0");
        updateCache.put("nama_penghuni", "");
        updateCache.put("id_sewa", "");
        db.update("tb_kamar_lokal", updateCache, "no_kamar = ?", new String[]{noKamar});
    }

    public void addPendingEditKamar(String idKamar, String noKamar, String fas, double h, double m, double b) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_kamar", idKamar);
        values.put("no_kamar", noKamar);
        values.put("fasilitas", fas);
        values.put("harian", h);
        values.put("mingguan", m);
        values.put("bulanan", b);
        db.insert("tb_pending_edit_kamar", null, values);

        // Update Cache Lokal
        ContentValues updateCache = new ContentValues();
        updateCache.put("no_kamar", noKamar);
        updateCache.put("fasilitas", fas);
        updateCache.put("harga_bulanan", b);
        db.update("tb_kamar_lokal", updateCache, "id_kamar = ?", new String[]{idKamar});
    }

    // --- GETTERS & DELETE FOR SYNC ---
    public Cursor getPendingUpdateSewa() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_update_sewa", null); }
    public void deletePendingUpdateSewa(int id) { this.getWritableDatabase().delete("tb_pending_update_sewa", "id = ?", new String[]{String.valueOf(id)}); }

    public Cursor getPendingCheckout() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_checkout", null); }
    public void deletePendingCheckout(int id) { this.getWritableDatabase().delete("tb_pending_checkout", "id = ?", new String[]{String.valueOf(id)}); }

    public Cursor getPendingEditKamar() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_edit_kamar", null); }
    public void deletePendingEditKamar(int id) { this.getWritableDatabase().delete("tb_pending_edit_kamar", "id = ?", new String[]{String.valueOf(id)}); }

    // --- CORE FEATURES ---

    public void simpanKamarOffline(List<Kamar> listKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM tb_kamar_lokal");
        db.beginTransaction();
        try {
            for (Kamar k : listKamar) {
                ContentValues values = new ContentValues();
                values.put("id_kamar", k.getIdKamar());
                values.put("no_kamar", k.getNoKamar());
                values.put("status", k.getStatus());
                values.put("fasilitas", k.getFasilitas());
                values.put("harga_bulanan", k.getHargaBulanan());
                values.put("nama_penghuni", (k.getNamaPenghuni() != null) ? k.getNamaPenghuni() : "");
                values.put("id_sewa", (k.getIdSewa() != null) ? k.getIdSewa() : "");
                db.insert("tb_kamar_lokal", null, values);
            }
            db.setTransactionSuccessful();
        } finally { db.endTransaction(); }
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
                k.setIdKamar(String.valueOf(c.getInt(c.getColumnIndexOrThrow("id_kamar"))));
                String nama = c.getString(c.getColumnIndexOrThrow("nama_penghuni"));
                if(nama!=null) k.setNamaPenghuni(nama);
                list.add(k);
            } while (c.moveToNext());
        }
        c.close();

        // Gabung Pending Kamar
        Cursor cPending = db.rawQuery("SELECT * FROM tb_pending_kamar", null);
        if (cPending.moveToFirst()) {
            do {
                Kamar k = new Kamar();
                k.setNoKamar(cPending.getString(cPending.getColumnIndexOrThrow("no_kamar")));
                k.setFasilitas(cPending.getString(cPending.getColumnIndexOrThrow("fasilitas")));
                k.setHargaBulanan(cPending.getDouble(cPending.getColumnIndexOrThrow("bulanan")));
                k.setStatus("0");
                k.setIdKamar("PENDING");
                list.add(k);
            } while (cPending.moveToNext());
        }
        cPending.close();

        // Update Status Pending Sewa
        List<String> kamarTerisiPending = new ArrayList<>();
        Cursor cSewa = db.rawQuery("SELECT no_kamar FROM tb_pending_sewa", null);
        if (cSewa.moveToFirst()) {
            do { kamarTerisiPending.add(cSewa.getString(0)); } while (cSewa.moveToNext());
        }
        cSewa.close();
        for (Kamar k : list) {
            if (kamarTerisiPending.contains(k.getNoKamar())) { k.setStatus("1"); }
        }
        return list;
    }

    public JSONObject getDetailPendingSewa(String noKamar) {
        SQLiteDatabase db = this.getReadableDatabase();
        JSONObject json = null;

        // 1. Cek di Pending Sewa (Prioritas Utama: Transaksi Offline barusan)
        Cursor c = db.rawQuery("SELECT * FROM tb_pending_sewa WHERE no_kamar = ?", new String[]{noKamar});
        if (c.moveToLast()) {
            try {
                json = new JSONObject();
                json.put("id_sewa", "PENDING_LOKAL");
                json.put("nama_penghuni", c.getString(c.getColumnIndexOrThrow("nama")));
                json.put("no_wa", c.getString(c.getColumnIndexOrThrow("wa")));
                json.put("pekerjaan", c.getString(c.getColumnIndexOrThrow("kerja")));
                json.put("durasi_sewa", c.getString(c.getColumnIndexOrThrow("durasi")));
                json.put("total_tarif", c.getDouble(c.getColumnIndexOrThrow("total")));
                json.put("sudah_dibayar", c.getDouble(c.getColumnIndexOrThrow("total")));
                json.put("tgl_checkin", c.getString(c.getColumnIndexOrThrow("tgl_in")));
                json.put("tgl_checkout", c.getString(c.getColumnIndexOrThrow("tgl_out")));
                json.put("sisa_bayar", 0);
            } catch (Exception e) { e.printStackTrace(); }
        }
        c.close();

        // 2. Jika tidak ada di Pending, Cek di Cache Kamar (Data terakhir dari server)
        if (json == null) {
            Cursor cLokal = db.rawQuery("SELECT * FROM tb_kamar_lokal WHERE no_kamar = ?", new String[]{noKamar});
            if (cLokal.moveToFirst()) {
                String nama = cLokal.getString(cLokal.getColumnIndexOrThrow("nama_penghuni"));
                String idSewa = cLokal.getString(cLokal.getColumnIndexOrThrow("id_sewa"));

                if (nama != null && !nama.isEmpty()) {
                    try {
                        json = new JSONObject();
                        json.put("id_sewa", idSewa);
                        json.put("nama_penghuni", nama);
                        json.put("no_wa", "-");
                        json.put("pekerjaan", "-");
                        json.put("durasi_sewa", "-");
                        json.put("tgl_checkin", "-");
                        json.put("tgl_checkout", "-");
                        json.put("total_tarif", 0);
                        json.put("sudah_dibayar", 0);
                        json.put("sisa_bayar", 0);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            cLokal.close();
        }
        return json;
    }

    public void addPendingKamar(String email, String no, String fas, double h, double m, double b) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("email", email); values.put("no_kamar", no); values.put("fasilitas", fas);
        values.put("harian", h); values.put("mingguan", m); values.put("bulanan", b);
        db.insert("tb_pending_kamar", null, values);
    }

    public void addPendingSewa(String email, String no, String nama, String wa, String kerja, String durasi, double total, String status, String tglIn, String tglOut) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("email", email); values.put("no_kamar", no); values.put("nama", nama); values.put("wa", wa);
        values.put("kerja", kerja); values.put("durasi", durasi); values.put("total", total); values.put("status", status);
        values.put("tgl_in", tglIn); values.put("tgl_out", tglOut);
        db.insert("tb_pending_sewa", null, values);
    }

    public Cursor getPendingKamar() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_kamar", null); }
    public void deletePendingKamar(int id) { this.getWritableDatabase().delete("tb_pending_kamar", "id = ?", new String[]{String.valueOf(id)}); }

    public Cursor getPendingSewa() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_sewa", null); }
    public void deletePendingSewa(int id) { this.getWritableDatabase().delete("tb_pending_sewa", "id = ?", new String[]{String.valueOf(id)}); }
}