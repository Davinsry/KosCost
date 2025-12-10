package com.example.koscost.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import com.example.koscost.model.Kamar;
import com.example.koscost.model.Transaksi; // Import Model Transaksi
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
        // 1. Cache Kamar (Menyimpan data dari server)
        db.execSQL("CREATE TABLE tb_kamar_lokal (id_kamar INTEGER PRIMARY KEY, no_kamar TEXT, status TEXT, fasilitas TEXT, harga_bulanan DOUBLE, nama_penghuni TEXT, id_sewa TEXT)");

        // 2. Pending Kamar (Tambah Kamar Offline)
        db.execSQL("CREATE TABLE tb_pending_kamar (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, no_kamar TEXT, fasilitas TEXT, harian DOUBLE, mingguan DOUBLE, bulanan DOUBLE)");

        // 3. Pending Sewa (Check-In Offline)
        db.execSQL("CREATE TABLE tb_pending_sewa (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, no_kamar TEXT, nama TEXT, wa TEXT, kerja TEXT, durasi TEXT, total DOUBLE, status TEXT, tgl_in TEXT, tgl_out TEXT)");

        // 4. Pending Update Sewa (Edit Penghuni Offline)
        db.execSQL("CREATE TABLE tb_pending_update_sewa (id INTEGER PRIMARY KEY AUTOINCREMENT, id_sewa TEXT, nama TEXT, wa TEXT, kerja TEXT, tgl_in TEXT, tgl_out TEXT, durasi TEXT, total DOUBLE, bayar DOUBLE, status TEXT)");

        // 5. Pending Checkout (Checkout Offline)
        db.execSQL("CREATE TABLE tb_pending_checkout (id INTEGER PRIMARY KEY AUTOINCREMENT, no_kamar TEXT)");

        // 6. Pending Edit Kamar (Edit Data Kamar Offline)
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

    // =========================================================================
    // FITUR 1: SIMPAN CACHE DARI SERVER (Dipanggil saat Online)
    // =========================================================================
    public void simpanKamarOffline(List<Kamar> listKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM tb_kamar_lokal"); // Bersihkan data lama

        db.beginTransaction();
        try {
            for (Kamar k : listKamar) {
                ContentValues values = new ContentValues();
                // Simpan ID Kamar (Penting untuk Edit)
                values.put("id_kamar", k.getIdKamar() != null ? Integer.parseInt(k.getIdKamar()) : 0);
                values.put("no_kamar", k.getNoKamar());
                values.put("status", k.getStatus());
                values.put("fasilitas", k.getFasilitas());
                values.put("harga_bulanan", k.getHargaBulanan());

                // Simpan Detail Penghuni (Jika ada)
                values.put("nama_penghuni", (k.getNamaPenghuni() != null) ? k.getNamaPenghuni() : "");
                values.put("id_sewa", (k.getIdSewa() != null) ? k.getIdSewa() : "");

                db.insert("tb_kamar_lokal", null, values);
            }
            db.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // FITUR 2: AMBIL DATA DASHBOARD (GABUNGAN CACHE + PENDING)
    // =========================================================================
    public List<Kamar> getKamarOffline() {
        List<Kamar> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // A. Ambil dari Cache (Data Lama)
        Cursor c = db.rawQuery("SELECT * FROM tb_kamar_lokal ORDER BY no_kamar ASC", null);
        if (c.moveToFirst()) {
            do {
                Kamar k = new Kamar();
                k.setIdKamar(String.valueOf(c.getInt(c.getColumnIndexOrThrow("id_kamar"))));
                k.setNoKamar(c.getString(c.getColumnIndexOrThrow("no_kamar")));
                k.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
                k.setFasilitas(c.getString(c.getColumnIndexOrThrow("fasilitas")));
                k.setHargaBulanan(c.getDouble(c.getColumnIndexOrThrow("harga_bulanan")));

                // Set Nama Penghuni jika ada
                String nama = c.getString(c.getColumnIndexOrThrow("nama_penghuni"));
                if (nama != null && !nama.isEmpty()) k.setNamaPenghuni(nama);

                // Set ID Sewa
                String idSewa = c.getString(c.getColumnIndexOrThrow("id_sewa"));
                if (idSewa != null) k.setIdSewa(idSewa);

                list.add(k);
            } while (c.moveToNext());
        }
        c.close();

        // B. Gabungkan dengan Pending Kamar (Data Baru Offline)
        Cursor cPending = db.rawQuery("SELECT * FROM tb_pending_kamar", null);
        if (cPending.moveToFirst()) {
            do {
                Kamar k = new Kamar();
                k.setIdKamar("PENDING"); // ID Dummy
                k.setNoKamar(cPending.getString(cPending.getColumnIndexOrThrow("no_kamar")));
                k.setFasilitas(cPending.getString(cPending.getColumnIndexOrThrow("fasilitas")));
                k.setHargaBulanan(cPending.getDouble(cPending.getColumnIndexOrThrow("bulanan")));
                k.setHargaHarian(cPending.getDouble(cPending.getColumnIndexOrThrow("harian")));
                k.setHargaMingguan(cPending.getDouble(cPending.getColumnIndexOrThrow("mingguan")));

                k.setStatus("0"); // Default Kosong
                list.add(k);
            } while (cPending.moveToNext());
        }
        cPending.close();

        // C. Update Status Kamar jika ada Pending Sewa (Visualisasi Terisi)
        List<String> kamarTerisiPending = new ArrayList<>();
        Cursor cSewa = db.rawQuery("SELECT no_kamar FROM tb_pending_sewa", null);
        if (cSewa.moveToFirst()) {
            do {
                kamarTerisiPending.add(cSewa.getString(0));
            } while (cSewa.moveToNext());
        }
        cSewa.close();

        // Loop list utama, ubah status jadi terisi jika ada di antrean sewa
        for (Kamar k : list) {
            if (kamarTerisiPending.contains(k.getNoKamar())) {
                k.setStatus("1");
            }
        }

        return list;
    }

    // =========================================================================
    // FITUR 3: AMBIL DETAIL UNTUK DetailKamarActivity (OFFLINE MODE)
    // =========================================================================
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
                        // Data detail lainnya mungkin tidak tersimpan di list cache, beri default
                        json.put("no_wa", "-");
                        json.put("pekerjaan", "-");
                        json.put("durasi_sewa", "-");
                        json.put("tgl_checkin", "-");
                        json.put("tgl_checkout", "-");
                        json.put("total_tarif", 0);
                        json.put("sudah_dibayar", 0);
                    } catch (Exception e) { e.printStackTrace(); }
                }
            }
            cLokal.close();
        }
        return json;
    }

    // =========================================================================
    // FITUR 4: AMBIL TRANSAKSI PENDING (UNTUK LAPORAN ACTIVITY)
    // =========================================================================
    public List<Transaksi> getPendingTransaksi() {
        List<Transaksi> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 1. Ambil dari Pending Check-In
        Cursor cSewa = db.rawQuery("SELECT * FROM tb_pending_sewa", null);
        if (cSewa.moveToFirst()) {
            do {
                Transaksi t = new Transaksi();
                t.setIdSewa("PENDING");
                t.setNoKamar(cSewa.getString(cSewa.getColumnIndexOrThrow("no_kamar")));
                t.setNamaPenghuni(cSewa.getString(cSewa.getColumnIndexOrThrow("nama")));
                t.setTotalTarif(cSewa.getDouble(cSewa.getColumnIndexOrThrow("total")));
                t.setStatusLunas(cSewa.getString(cSewa.getColumnIndexOrThrow("status")));
                t.setTglCheckin(cSewa.getString(cSewa.getColumnIndexOrThrow("tgl_in")));
                list.add(t);
            } while (cSewa.moveToNext());
        }
        cSewa.close();

        // 2. Ambil dari Pending Update (Edit Pembayaran)
        Cursor cUpd = db.rawQuery("SELECT * FROM tb_pending_update_sewa", null);
        if (cUpd.moveToFirst()) {
            do {
                Transaksi t = new Transaksi();
                t.setIdSewa("PENDING_UPDATE");
                t.setNoKamar("Edit");
                t.setNamaPenghuni(cUpd.getString(cUpd.getColumnIndexOrThrow("nama")));
                t.setTotalTarif(cUpd.getDouble(cUpd.getColumnIndexOrThrow("bayar")));
                t.setStatusLunas(cUpd.getString(cUpd.getColumnIndexOrThrow("status")));
                t.setTglCheckin(cUpd.getString(cUpd.getColumnIndexOrThrow("tgl_in")));
                list.add(t);
            } while (cUpd.moveToNext());
        }
        cUpd.close();

        return list;
    }

    // =========================================================================
    // HELPER METHODS: ADD PENDING DATA
    // =========================================================================

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

    public void addPendingUpdateSewa(String idSewa, String nama, String wa, String kerja, String tglIn, String tglOut, String durasi, double total, double bayar, String status, String noKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_sewa", idSewa); values.put("nama", nama); values.put("wa", wa);
        values.put("kerja", kerja); values.put("tgl_in", tglIn); values.put("tgl_out", tglOut);
        values.put("durasi", durasi); values.put("total", total); values.put("bayar", bayar);
        values.put("status", status);
        db.insert("tb_pending_update_sewa", null, values);

        // Update Cache Lokal juga agar UI berubah
        ContentValues updateCache = new ContentValues();
        updateCache.put("nama_penghuni", nama);
        db.update("tb_kamar_lokal", updateCache, "no_kamar = ?", new String[]{noKamar});
    }

    public void addPendingCheckout(String noKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("no_kamar", noKamar);
        db.insert("tb_pending_checkout", null, values);

        // Kosongkan Cache Lokal
        ContentValues updateCache = new ContentValues();
        updateCache.put("status", "0");
        updateCache.put("nama_penghuni", "");
        updateCache.put("id_sewa", "");
        db.update("tb_kamar_lokal", updateCache, "no_kamar = ?", new String[]{noKamar});
    }

    public void addPendingEditKamar(String idKamar, String noKamar, String fas, double h, double m, double b) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("id_kamar", idKamar); values.put("no_kamar", noKamar); values.put("fasilitas", fas);
        values.put("harian", h); values.put("mingguan", m); values.put("bulanan", b);
        db.insert("tb_pending_edit_kamar", null, values);

        // Update Cache Lokal
        ContentValues updateCache = new ContentValues();
        updateCache.put("no_kamar", noKamar);
        updateCache.put("fasilitas", fas);
        updateCache.put("harga_bulanan", b);
        db.update("tb_kamar_lokal", updateCache, "id_kamar = ?", new String[]{idKamar});
    }

    // =========================================================================
    // HELPER METHODS: GET & DELETE PENDING (FOR SYNC)
    // =========================================================================

    public Cursor getPendingKamar() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_kamar", null); }
    public void deletePendingKamar(int id) { this.getWritableDatabase().delete("tb_pending_kamar", "id = ?", new String[]{String.valueOf(id)}); }

    public Cursor getPendingSewa() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_sewa", null); }
    public void deletePendingSewa(int id) { this.getWritableDatabase().delete("tb_pending_sewa", "id = ?", new String[]{String.valueOf(id)}); }

    public Cursor getPendingUpdateSewa() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_update_sewa", null); }
    public void deletePendingUpdateSewa(int id) { this.getWritableDatabase().delete("tb_pending_update_sewa", "id = ?", new String[]{String.valueOf(id)}); }

    public Cursor getPendingCheckout() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_checkout", null); }
    public void deletePendingCheckout(int id) { this.getWritableDatabase().delete("tb_pending_checkout", "id = ?", new String[]{String.valueOf(id)}); }

    public Cursor getPendingEditKamar() { return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_edit_kamar", null); }
    public void deletePendingEditKamar(int id) { this.getWritableDatabase().delete("tb_pending_edit_kamar", "id = ?", new String[]{String.valueOf(id)}); }
}