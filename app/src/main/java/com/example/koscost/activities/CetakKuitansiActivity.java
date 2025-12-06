package com.example.koscost.activities;

import android.database.Cursor;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.util.Locale;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import androidx.cardview.widget.CardView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

// Import Database Helper
import com.example.koscost.database.DatabaseHelper;
import com.example.koscost.R;

public class CetakKuitansiActivity extends AppCompatActivity {

    // 1. DEKLARASI SEMUA VARIABEL DI SINI
    private DatabaseHelper dbHelper;
    private int idSewa;

    // Variabel untuk Tampilan Data
    private TextView tvNama, tvNoKamar, tvPeriode, tvJumlah, tvMetode, tvStatus, tvTglCetak;

    // Variabel untuk PDF
    private CardView cvKuitansi;
    private Button btnCetakPdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cetak_kuitansi);

        // 2. INISIALISASI DATABASE & ID
        dbHelper = new DatabaseHelper(this);
        idSewa = getIntent().getIntExtra("ID_SEWA", 0);

        // 3. BINDING (Menghubungkan variabel dengan ID di XML)
        // Bagian Data Teks
        tvNama = findViewById(R.id.tv_nama);
        tvNoKamar = findViewById(R.id.tv_no_kamar);
        tvPeriode = findViewById(R.id.tv_periode);
        tvJumlah = findViewById(R.id.tv_jumlah_uang);
        tvMetode = findViewById(R.id.tv_metode);
        tvStatus = findViewById(R.id.tv_status);
        tvTglCetak = findViewById(R.id.tv_tanggal_cetak);

        // Bagian Container PDF & Tombol
        cvKuitansi = findViewById(R.id.cv_kuitansi);
        btnCetakPdf = findViewById(R.id.btn_cetak_pdf);

        // 4. LOGIKA UTAMA
        // Tampilkan data jika ID ditemukan
        if (idSewa > 0) {
            tampilkanDataKuitansi(idSewa);
        } else {
            Toast.makeText(this, "Data Transaksi Tidak Ditemukan!", Toast.LENGTH_SHORT).show();
        }

        // Aksi ketika tombol Cetak ditekan
        btnCetakPdf.setOnClickListener(v -> {
            cetakPdf();
        });
    }

    // --- METHOD UNTUK MENAMPILKAN DATA ---
    private void tampilkanDataKuitansi(int id) {
        Cursor cursor = dbHelper.getDataKuitansi(id);

        if (cursor != null && cursor.moveToFirst()) {
            // Pastikan nama kolom di bawah ini SAMA PERSIS dengan di DatabaseHelper
            String nama = cursor.getString(cursor.getColumnIndexOrThrow("nama_penghuni"));
            String noKamar = cursor.getString(cursor.getColumnIndexOrThrow("no_kamar"));
            String tglIn = cursor.getString(cursor.getColumnIndexOrThrow("tgl_checkin"));
            String tglOut = cursor.getString(cursor.getColumnIndexOrThrow("tgl_checkout"));
            double totalBayar = cursor.getDouble(cursor.getColumnIndexOrThrow("total_tarif"));
            String metode = cursor.getString(cursor.getColumnIndexOrThrow("metode_bayar"));
            String status = cursor.getString(cursor.getColumnIndexOrThrow("status_lunas"));

            tvNama.setText(nama);
            tvNoKamar.setText(noKamar);
            tvPeriode.setText(tglIn + " s.d " + tglOut);
            tvMetode.setText(metode);
            tvStatus.setText(status);
            tvJumlah.setText(formatRupiah(totalBayar));

            // Tips: Gunakan tanggal hari ini yang dinamis jika mau
            tvTglCetak.setText("Dicetak pada: " + java.text.DateFormat.getDateInstance().format(new java.util.Date()));

            cursor.close();
        } else {
            Toast.makeText(this, "Gagal memuat data", Toast.LENGTH_SHORT).show();
        }
    }

    // --- METHOD UNTUK FORMAT RUPIAH ---
    private String formatRupiah(double number) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        return formatRupiah.format(number);
    }

    // --- METHOD UNTUK MEMBUAT PDF ---
    private void cetakPdf() {
        // A. Ukur dimensi CardView
        DisplayMetrics displayMetrics = new DisplayMetrics();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            getDisplay().getRealMetrics(displayMetrics);
        } else {
            getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        }

        cvKuitansi.measure(
                View.MeasureSpec.makeMeasureSpec(displayMetrics.widthPixels, View.MeasureSpec.EXACTLY),
                View.MeasureSpec.makeMeasureSpec(displayMetrics.heightPixels, View.MeasureSpec.UNSPECIFIED));
        cvKuitansi.layout(0, 0, cvKuitansi.getMeasuredWidth(), cvKuitansi.getMeasuredHeight());

        // B. Ubah View menjadi Bitmap
        int viewWidth = cvKuitansi.getMeasuredWidth();
        int viewHeight = cvKuitansi.getMeasuredHeight();

        // Cek agar tidak error jika ukuran 0
        if(viewWidth == 0 || viewHeight == 0) {
            Toast.makeText(this, "Gagal mengukur Layout, coba lagi.", Toast.LENGTH_SHORT).show();
            return;
        }

        Bitmap bitmap = Bitmap.createBitmap(viewWidth, viewHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        cvKuitansi.draw(canvas);

        // C. Buat Dokumen PDF
        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(viewWidth, viewHeight, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);

        Canvas pdfCanvas = page.getCanvas();
        pdfCanvas.drawBitmap(bitmap, 0, 0, null);
        pdfDocument.finishPage(page);

        // D. Simpan File
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kuitansi_Kost_" + System.currentTimeMillis() + ".pdf");

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Berhasil Disimpan di Folder Download!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal simpan PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }
}