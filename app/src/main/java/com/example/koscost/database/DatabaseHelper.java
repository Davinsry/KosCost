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
    // NAIK KE VERSI 4 agar method onUpgrade terpanggil dan tabel dibuat ulang dengan kolom baru
    private static final int DATABASE_VERSION = 4;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Tabel Cache Kamar (UPDATE: Tambah nama_penghuni & id_sewa)
        // Ini menyimpan data terakhir dari server saat online
        String sqlKamar = "CREATE TABLE tb_kamar_lokal (id_kamar INTEGER PRIMARY KEY, no_kamar TEXT, status TEXT, fasilitas TEXT, harga_bulanan DOUBLE, nama_penghuni TEXT, id_sewa TEXT)";
        db.execSQL(sqlKamar);

        // 2. Pending Kamar (Data kamar baru yang dibuat saat offline)
        String sqlPendingKamar = "CREATE TABLE tb_pending_kamar (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, no_kamar TEXT, fasilitas TEXT, harian DOUBLE, mingguan DOUBLE, bulanan DOUBLE)";
        db.execSQL(sqlPendingKamar);

        // 3. Pending Sewa (UPDATE: Tambah tgl_in dan tgl_out)
        // Data transaksi sewa yang dibuat saat offline
        String sqlPendingSewa = "CREATE TABLE tb_pending_sewa (id INTEGER PRIMARY KEY AUTOINCREMENT, email TEXT, no_kamar TEXT, nama TEXT, wa TEXT, kerja TEXT, durasi TEXT, total DOUBLE, status TEXT, tgl_in TEXT, tgl_out TEXT)";
        db.execSQL(sqlPendingSewa);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Hapus tabel lama jika struktur berubah (Update Versi)
        db.execSQL("DROP TABLE IF EXISTS tb_kamar_lokal");
        db.execSQL("DROP TABLE IF EXISTS tb_pending_kamar");
        db.execSQL("DROP TABLE IF EXISTS tb_pending_sewa");
        onCreate(db);
    }

    // =========================================================================
    // FITUR 1: SIMPAN CACHE DARI SERVER (Dipanggil di MainActivity saat Online)
    // =========================================================================
    public void simpanKamarOffline(List<Kamar> listKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM tb_kamar_lokal"); // Bersihkan data lama

        db.beginTransaction();
        try {
            for (Kamar k : listKamar) {
                ContentValues values = new ContentValues();
                values.put("id_kamar", k.getIdKamar()); // Simpan ID aslinya
                values.put("no_kamar", k.getNoKamar());
                values.put("status", k.getStatus());
                values.put("fasilitas", k.getFasilitas());
                values.put("harga_bulanan", k.getHargaBulanan());

                // PENTING: Simpan Nama & ID Sewa (Pastikan Model Kamar sudah ada getternya, atau pasang string kosong jika null)
                // Nanti kita update Model Kamar setelah ini
                values.put("nama_penghuni", (k.getNamaPenghuni() != null) ? k.getNamaPenghuni() : "");
                values.put("id_sewa", (k.getIdSewa() != null) ? k.getIdSewa() : "");

                db.insert("tb_kamar_lokal", null, values);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    // =========================================================================
    // FITUR 2: AMBIL DATA OFFLINE (GABUNGAN CACHE + PENDING LOKAL)
    // =========================================================================
    // --- UPDATE: AMBIL GABUNGAN CACHE + PENDING ---
    public List<Kamar> getKamarOffline() {
        List<Kamar> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        // 1. AMBIL DARI CACHE (Data yang didapat saat terakhir Online)
        Cursor c = db.rawQuery("SELECT * FROM tb_kamar_lokal ORDER BY no_kamar ASC", null);

        // Debugging: Cek apakah ada data di cache?
        if (c.getCount() > 0) {
            if (c.moveToFirst()) {
                do {
                    Kamar k = new Kamar();
                    k.setNoKamar(c.getString(c.getColumnIndexOrThrow("no_kamar")));
                    k.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
                    k.setFasilitas(c.getString(c.getColumnIndexOrThrow("fasilitas")));
                    k.setHargaBulanan(c.getDouble(c.getColumnIndexOrThrow("harga_bulanan")));
                    k.setIdKamar(String.valueOf(c.getInt(c.getColumnIndexOrThrow("id_kamar"))));

                    // Ambil nama penghuni jika ada
                    // k.setNamaPenghuni(c.getString(c.getColumnIndexOrThrow("nama_penghuni")));

                    list.add(k);
                } while (c.moveToNext());
            }
        }
        c.close();

        // 2. GABUNGKAN DENGAN PENDING KAMAR (Sama seperti sebelumnya)
        // ... (Kode gabung pending kamar tetap sama) ...
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

        // 3. UPDATE STATUS (Sama seperti sebelumnya)
        // ... (Kode cek pending sewa tetap sama) ...
        List<String> kamarTerisiPending = new ArrayList<>();
        Cursor cSewa = db.rawQuery("SELECT no_kamar FROM tb_pending_sewa", null);
        if (cSewa.moveToFirst()) {
            do {
                kamarTerisiPending.add(cSewa.getString(0));
            } while (cSewa.moveToNext());
        }
        cSewa.close();

        for (Kamar k : list) {
            if (kamarTerisiPending.contains(k.getNoKamar())) {
                k.setStatus("1");
            }
        }

        return list;
    }

        // B. Ambil dari Pending Kamar (Yang baru ditambah user tapi belum sync)
        Cursor cPending = db.rawQuery("SELECT * FROM tb_pending_kamar", null);
        if (cPending.moveToFirst()) {
            do {
                Kamar k = new Kamar();
                k.setIdKamar("PENDING"); // Penanda
                k.setNoKamar(cPending.getString(cPending.getColumnIndexOrThrow("no_kamar")));
                k.setFasilitas(cPending.getString(cPending.getColumnIndexOrThrow("fasilitas")));
                k.setHargaBulanan(cPending.getDouble(cPending.getColumnIndexOrThrow("bulanan")));
                k.setStatus("0"); // Default Kosong
                list.add(k);
            } while (cPending.moveToNext());
        }
        cPending.close();

        // C. Update Status Kamar jika ada di Pending Sewa (Biar jadi merah/terisi)
        List<String> kamarTerisiPending = new ArrayList<>();
        Cursor cSewa = db.rawQuery("SELECT no_kamar FROM tb_pending_sewa", null);
        if (cSewa.moveToFirst()) {
            do {
                kamarTerisiPending.add(cSewa.getString(0));
            } while (cSewa.moveToNext());
        }
        cSewa.close();

        for (Kamar k : list) {
            if (kamarTerisiPending.contains(k.getNoKamar())) {
                k.setStatus("1"); // Ubah jadi Terisi secara visual
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

                // Kalau ada datanya, kita buat JSON pura-pura biar DetailActivity gak error
                if (nama != null && !nama.isEmpty()) {
                    try {
                        json = new JSONObject();
                        json.put("id_sewa", idSewa);
                        json.put("nama_penghuni", nama);
                        json.put("no_wa", "-"); // Data detail lain mungkin tidak tersimpan di cache list
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

    // =========================================================================
    // HELPER: PENDING DATA (Untuk Sync & Input)
    // =========================================================================

    // Input Data Sewa Pending (UPDATE: Tambah parameter tanggal)
    public void addPendingSewa(String email, String no, String nama, String wa, String kerja, String durasi, double total, String status, String tglIn, String tglOut) {
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
        values.put("tgl_in", tglIn);   // Tambahan
        values.put("tgl_out", tglOut); // Tambahan
        db.insert("tb_pending_sewa", null, values);
    }

    // Input Data Kamar Pending
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

    public Cursor getPendingKamar() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_kamar", null);
    }

    public Cursor getPendingSewa() {
        return this.getReadableDatabase().rawQuery("SELECT * FROM tb_pending_sewa", null);
    }

    public void clearPendingKamar() {
        this.getWritableDatabase().execSQL("DELETE FROM tb_pending_kamar");
    }

    public void clearPendingSewa() {
        this.getWritableDatabase().execSQL("DELETE FROM tb_pending_sewa");
    }
}