package com.example.koscost.activities;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.pdf.PdfDocument;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.Button;
import androidx.cardview.widget.CardView;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

// Import Session Manager
import com.example.koscost.utils.SessionManager;
import com.example.koscost.R;

public class CetakKuitansiActivity extends AppCompatActivity {

    // Variabel untuk Tampilan Data
    private TextView tvNama, tvNoKamar, tvPeriode, tvJumlah, tvMetode, tvStatus, tvTglCetak, tvNamaKostTTD;

    // Variabel untuk PDF
    private CardView cvKuitansi;
    private Button btnCetakPdf;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cetak_kuitansi);

        // 1. Binding ID (Sambungkan dengan XML)
        tvNama = findViewById(R.id.tv_nama);
        tvNoKamar = findViewById(R.id.tv_no_kamar);
        tvPeriode = findViewById(R.id.tv_periode);
        tvJumlah = findViewById(R.id.tv_jumlah_uang);
        tvMetode = findViewById(R.id.tv_metode);
        tvStatus = findViewById(R.id.tv_status);
        tvTglCetak = findViewById(R.id.tv_tanggal_cetak);
        tvNamaKostTTD = findViewById(R.id.tv_nama_kost_ttd); // ID Baru untuk Nama Kos di bawah
        cvKuitansi = findViewById(R.id.cv_kuitansi);
        btnCetakPdf = findViewById(R.id.btn_cetak_pdf);

        // 2. Tangkap Data "Lemparan" dari InputSewaActivity
        String nama = getIntent().getStringExtra("NAMA");
        String kamar = getIntent().getStringExtra("KAMAR");
        String periode = getIntent().getStringExtra("PERIODE"); // Tangkap Periode
        double harga = getIntent().getDoubleExtra("HARGA", 0);
        String status = getIntent().getStringExtra("STATUS");
        String metode = getIntent().getStringExtra("METODE"); // Tangkap Metode

        if (nama != null) {
            // Tampilkan Data ke Layar
            tvNama.setText(nama);
            tvNoKamar.setText(kamar);
            tvJumlah.setText(formatRupiah(harga));
            tvStatus.setText(status);
            tvMetode.setText(metode != null ? metode : "Transfer");
            tvPeriode.setText(periode != null ? periode : "-");

            // --- LOGIKA BARU: TANGGAL & NAMA KOS ---

            // A. Ambil Nama Kos dari Session
            SessionManager sessionManager = new SessionManager(this);
            String namaKos = sessionManager.getNamaKos();
            tvNamaKostTTD.setText(namaKos);

            // B. Set Tanggal Cetak (Format: 07 Dec 2025)
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.US);
            String tglSekarang = sdf.format(new Date());
            tvTglCetak.setText(tglSekarang);

        } else {
            Toast.makeText(this, "Tidak ada data kuitansi baru", Toast.LENGTH_SHORT).show();
        }

        // Tombol PDF
        btnCetakPdf.setOnClickListener(v -> cetakPdf());
    }

    // Helper Format Rupiah
    private String formatRupiah(double number) {
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
        return formatRupiah.format(number);
    }

    // Fungsi Cetak PDF
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
        File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), "Kuitansi_" + System.currentTimeMillis() + ".pdf");

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF Tersimpan di Download!", Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this, "Gagal simpan PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }

        pdfDocument.close();
    }
}