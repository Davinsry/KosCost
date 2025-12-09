package com.example.koscost.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.utils.CurrencyTextWatcher;

import org.json.JSONObject;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.Calendar;
import java.util.Locale;

public class DetailKamarActivity extends AppCompatActivity {

    TextView tvJudul;
    EditText etNama, etWa, etKerja, etTglIn, etTglOut, etTotal, etBayar;
    Button btnUpdate, btnCheckout;
    ImageButton btnBack; // Tombol Back
    String noKamar, idSewaSaatIni;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_kamar);

        // Binding ID (Harus sama persis dengan XML)
        tvJudul = findViewById(R.id.tv_judul_kamar);
        etNama = findViewById(R.id.et_detail_nama);
        etWa = findViewById(R.id.et_detail_wa);
        etKerja = findViewById(R.id.et_detail_kerja);
        etTglIn = findViewById(R.id.et_detail_tgl_in);
        etTglOut = findViewById(R.id.et_detail_tgl_out);
        etTotal = findViewById(R.id.et_detail_total);
        etBayar = findViewById(R.id.et_detail_bayar);

        btnUpdate = findViewById(R.id.btn_update_data);
        btnCheckout = findViewById(R.id.btn_checkout);
        btnBack = findViewById(R.id.btn_back);

        // Logic Tombol Back
        btnBack.setOnClickListener(v -> finish());

        // Format Uang
        etTotal.addTextChangedListener(new CurrencyTextWatcher(etTotal));
        etBayar.addTextChangedListener(new CurrencyTextWatcher(etBayar));

        // Date Picker
        setupDatePicker(etTglIn);
        setupDatePicker(etTglOut);

        noKamar = getIntent().getStringExtra("NO_KAMAR");
        if (noKamar != null) {
            tvJudul.setText("Kamar " + noKamar);
            loadDataPenghuni();
        } else {
            Toast.makeText(this, "Error: Nomor Kamar Kosong", Toast.LENGTH_SHORT).show();
            finish();
        }

        btnUpdate.setOnClickListener(v -> updateDataPenghuni());
        btnCheckout.setOnClickListener(v -> dialogCheckout());
    }

    private void setupDatePicker(EditText editText) {
        editText.setFocusable(false);
        editText.setClickable(true);
        editText.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                String tgl = String.format(Locale.US, "%d-%02d-%02d", year, month + 1, dayOfMonth);
                editText.setText(tgl);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });
    }

    private void loadDataPenghuni() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.getDetailSewa(noKamar).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                try {
                    if (response.isSuccessful()) {
                        // 1. Sukses Online -> Tampilkan Data Server
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);
                        tampilkanData(json);
                    } else {
                        // 2. Gagal Server -> Coba Ambil Offline
                        ambilDataOffline();
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                // 3. Gagal Koneksi (Offline) -> Coba Ambil Offline
                ambilDataOffline();
            }
        });
    }

    // Method baru untuk ambil data offline
    private void ambilDataOffline() {
        com.example.koscost.database.DatabaseHelper db = new com.example.koscost.database.DatabaseHelper(this);
        JSONObject jsonOffline = db.getDetailPendingSewa(noKamar);

        if (jsonOffline != null) {
            Toast.makeText(this, "Mode Offline: Menampilkan data lokal", Toast.LENGTH_SHORT).show();
            tampilkanData(jsonOffline);

            // Matikan tombol edit/checkout saat offline agar tidak konflik sync
            btnUpdate.setEnabled(false);
            btnCheckout.setEnabled(false);
            btnUpdate.setText("Menu Edit (Hanya Online)");
        } else {
            Toast.makeText(this, "Data tidak ditemukan (Offline)", Toast.LENGTH_SHORT).show();
        }
    }

    // Pindahkan logika set text ke sini biar rapi
    private void tampilkanData(JSONObject json) {
        idSewaSaatIni = json.optString("id_sewa");
        etNama.setText(json.optString("nama_penghuni"));
        etWa.setText(json.optString("no_wa"));
        etKerja.setText(json.optString("pekerjaan"));
        etTglIn.setText(json.optString("tgl_checkin"));
        etTglOut.setText(json.optString("tgl_checkout"));

        double total = json.optDouble("total_tarif", 0);
        double bayar = json.optDouble("sudah_dibayar", 0);
        etTotal.setText(String.format(Locale.US, "%.0f", total));
        etBayar.setText(String.format(Locale.US, "%.0f", bayar));
    }

    private void updateDataPenghuni() {
        String nama = etNama.getText().toString();
        String wa = etWa.getText().toString();
        String kerja = etKerja.getText().toString();
        String tglIn = etTglIn.getText().toString();
        String tglOut = etTglOut.getText().toString();
        double total = CurrencyTextWatcher.parseCurrency(etTotal.getText().toString());
        double bayar = CurrencyTextWatcher.parseCurrency(etBayar.getText().toString());
        String status = (bayar >= total) ? "Lunas" : "Belum Lunas";
        String durasi = hitungDurasi(tglIn, tglOut);

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.updateSewa(idSewaSaatIni, nama, wa, kerja, tglIn, tglOut, durasi, total, bayar, status)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Toast.makeText(DetailKamarActivity.this, "Data Berhasil Diupdate!", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {}
                });
    }

    private void dialogCheckout() {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Check-Out")
                .setMessage("Data akan dihapus dari kamar ini. Lanjutkan?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    ApiService api = RetrofitClient.getClient().create(ApiService.class);
                    api.prosesCheckout(noKamar).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            Toast.makeText(DetailKamarActivity.this, "Berhasil Check-Out", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private String hitungDurasi(String in, String out) {
        // Logic hitung hari sederhana
        try {
            // ... (Kode sama seperti sebelumnya)
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            long diff = sdf.parse(out).getTime() - sdf.parse(in).getTime();
            long days = java.util.concurrent.TimeUnit.DAYS.convert(diff, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (days < 7) return "Harian"; else if(days <= 13) return "Mingguan"; else return "Bulanan";
        } catch (Exception e) { return "Bulanan"; }
    }
}