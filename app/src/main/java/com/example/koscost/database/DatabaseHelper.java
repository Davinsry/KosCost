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

    // Nama Database
    private static final String DATABASE_NAME = "KostApp.db";
    private static final int DATABASE_VERSION = 1;

    // --- TABEL 1: MASTER KAMAR ---
    private static final String TABLE_KAMAR = "tb_kamar";
    private static final String COL_ID_KAMAR = "id_kamar";
    private static final String COL_NO_KAMAR = "no_kamar";
    private static final String COL_FASILITAS = "fasilitas";
    private static final String COL_HARGA_DASAR = "harga_dasar"; // Harga default
    private static final String COL_STATUS_KAMAR = "status_kamar"; // 0: Kosong, 1: Isi

    // --- TABEL 2: TRANSAKSI / PENGHUNI ---
    // Tabel ini menyimpan semua data untuk Kuitansi & Manajemen Penghuni
    private static final String TABLE_SEWA = "tb_sewa";
    private static final String COL_ID_SEWA = "id_sewa";
    private static final String COL_FK_ID_KAMAR = "id_kamar_fk"; // Foreign Key ke Tabel Kamar

    // Data Diri
    private static final String COL_NAMA_PENGHUNI = "nama_penghuni";
    private static final String COL_NO_WA = "no_wa";
    private static final String COL_NO_KTP = "no_ktp";
    private static final String COL_PEKERJAAN = "pekerjaan";

    // Data Sewa (Checkin/Checkout)
    private static final String COL_TGL_CHECKIN = "tgl_checkin";   // Format: YYYY-MM-DD
    private static final String COL_TGL_CHECKOUT = "tgl_checkout"; // Format: YYYY-MM-DD
    private static final String COL_TIPE_SEWA = "tipe_sewa";       // Harian/Mingguan/Bulanan

    // Data Keuangan (Invoice)
    private static final String COL_TOTAL_TARIF = "total_tarif";     // Total yang harus dibayar
    private static final String COL_SUDAH_DIBAYAR = "sudah_dibayar"; // Uang yang masuk (DP/Full)
    private static final String COL_METODE_BAYAR = "metode_bayar";   // Transfer/Tunai
    private static final String COL_STATUS_LUNAS = "status_lunas";   // Lunas/Belum

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // 1. Buat Tabel Kamar
        String createTableKamar = "CREATE TABLE " + TABLE_KAMAR + " (" +
                COL_ID_KAMAR + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_NO_KAMAR + " TEXT, " +
                COL_FASILITAS + " TEXT, " +
                COL_HARGA_DASAR + " REAL, " +
                COL_STATUS_KAMAR + " INTEGER DEFAULT 0)"; // Default 0 (Kosong)
        db.execSQL(createTableKamar);

        db.execSQL("CREATE TABLE tb_kamar_lokal (id_kamar INTEGER PRIMARY KEY, no_kamar TEXT, status TEXT, fasilitas TEXT, harga_bulanan DOUBLE)");

        // 2. Buat Tabel Sewa (Menyimpan data penghuni & pembayaran)
        String createTableSewa = "CREATE TABLE " + TABLE_SEWA + " (" +
                COL_ID_SEWA + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_FK_ID_KAMAR + " INTEGER, " +
                COL_NAMA_PENGHUNI + " TEXT, " +
                COL_NO_WA + " TEXT, " +
                COL_NO_KTP + " TEXT, " +
                COL_PEKERJAAN + " TEXT, " +
                COL_TGL_CHECKIN + " TEXT, " +
                COL_TGL_CHECKOUT + " TEXT, " +
                COL_TIPE_SEWA + " TEXT, " +
                COL_TOTAL_TARIF + " REAL, " +
                COL_SUDAH_DIBAYAR + " REAL, " +
                COL_METODE_BAYAR + " TEXT, " +
                COL_STATUS_LUNAS + " TEXT)";
        db.execSQL(createTableSewa);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_KAMAR);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEWA);
        onCreate(db);
    }

    // --- CONTOH METHOD CRUD DASAR ---

    // 1. Tambah Kamar Baru (Master Data)
    public boolean addKamar(String noKamar, String fasilitas, double harga) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_NO_KAMAR, noKamar);
        values.put(COL_FASILITAS, fasilitas);
        values.put(COL_HARGA_DASAR, harga);
        long result = db.insert(TABLE_KAMAR, null, values);
        return result != -1;
    }

    // 2. Proses Check-In (Tambah data sewa + Update status kamar jadi Terisi)
    public boolean prosesCheckIn(int idKamar, String nama, String wa, String ktp, String kerja,
                                 String tglIn, String tglOut, String tipe,
                                 double total, double bayar, String metode, String statusLunas) {
        SQLiteDatabase db = this.getWritableDatabase();

        // A. Simpan Data Sewa
        ContentValues values = new ContentValues();
        values.put(COL_FK_ID_KAMAR, idKamar);
        values.put(COL_NAMA_PENGHUNI, nama);
        values.put(COL_NO_WA, wa);
        values.put(COL_NO_KTP, ktp);
        values.put(COL_PEKERJAAN, kerja);
        values.put(COL_TGL_CHECKIN, tglIn);
        values.put(COL_TGL_CHECKOUT, tglOut);
        values.put(COL_TIPE_SEWA, tipe);
        values.put(COL_TOTAL_TARIF, total);
        values.put(COL_SUDAH_DIBAYAR, bayar);
        values.put(COL_METODE_BAYAR, metode);
        values.put(COL_STATUS_LUNAS, statusLunas);

        long result = db.insert(TABLE_SEWA, null, values);

        // B. Update Status Kamar jadi 'Terisi' (1)
        if (result != -1) {
            ContentValues kamarValues = new ContentValues();
            kamarValues.put(COL_STATUS_KAMAR, 1); // Set jadi Terisi
            db.update(TABLE_KAMAR, kamarValues, COL_ID_KAMAR + "=?", new String[]{String.valueOf(idKamar)});
            return true;
        }
        return false;
    }

    // 3. Ambil Data untuk Kuitansi (Berdasarkan ID Sewa)
    // Nanti dipanggil saat mau cetak kuitansi
    public Cursor getDataKuitansi(int idSewa) {
        SQLiteDatabase db = this.getReadableDatabase();
        // Query Join agar kita dapat Nomor Kamar dari tabel Kamar, dan Nama dari tabel Sewa
        String query = "SELECT s.*, k." + COL_NO_KAMAR +
                " FROM " + TABLE_SEWA + " s " +
                " JOIN " + TABLE_KAMAR + " k ON s." + COL_FK_ID_KAMAR + " = k." + COL_ID_KAMAR +
                " WHERE s." + COL_ID_SEWA + " = " + idSewa;
        return db.rawQuery(query, null);
    }
    public int getLastIdSewa() {
        SQLiteDatabase db = this.getReadableDatabase();
        String query = "SELECT " + COL_ID_SEWA + " FROM " + TABLE_SEWA + " ORDER BY " + COL_ID_SEWA + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        int lastId = 0;
        if (cursor.moveToFirst()) {
            lastId = cursor.getInt(0);
        }
        cursor.close();
        return lastId;
    }

    // --- METHOD BARU: SIMPAN CACHE ---
    public void simpanKamarOffline(List<Kamar> listKamar) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM tb_kamar_lokal"); // Hapus data lama (Refresh)

        db.beginTransaction();
        try {
            for (Kamar k : listKamar) {
                ContentValues values = new ContentValues();
                values.put("id_kamar", k.getIdKamar());
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

    // --- METHOD BARU: AMBIL CACHE ---
    public List<Kamar> getKamarOffline() {
        List<Kamar> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM tb_kamar_lokal ORDER BY no_kamar ASC", null);

        if (c.moveToFirst()) {
            do {
                Kamar k = new Kamar();
                k.setIdKamar(c.getString(c.getColumnIndexOrThrow("id_kamar")));
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


