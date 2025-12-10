package com.example.koscost.activities;

import android.app.DatePickerDialog;
import android.content.Intent;
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
import com.example.koscost.database.DatabaseHelper; // Import ini penting
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
    Button btnUpdate, btnCheckout, btnCetak;
    ImageButton btnBack;
    String noKamar, idSewaSaatIni;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_kamar);

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
        btnCetak = findViewById(R.id.btn_cetak_ulang);
        btnBack = findViewById(R.id.btn_back);

        btnBack.setOnClickListener(v -> finish());
        etTotal.addTextChangedListener(new CurrencyTextWatcher(etTotal));
        etBayar.addTextChangedListener(new CurrencyTextWatcher(etBayar));
        setupDatePicker(etTglIn);
        setupDatePicker(etTglOut);

        noKamar = getIntent().getStringExtra("NO_KAMAR");
        if (noKamar != null) {
            tvJudul.setText("Kamar " + noKamar);
            loadDataPenghuni();
        } else { finish(); }

        btnUpdate.setOnClickListener(v -> updateDataPenghuni());
        btnCheckout.setOnClickListener(v -> dialogCheckout());

        btnCetak.setOnClickListener(v -> {
            String nama = etNama.getText().toString();
            String tglIn = etTglIn.getText().toString();
            String tglOut = etTglOut.getText().toString();
            double harga = CurrencyTextWatcher.parseCurrency(etTotal.getText().toString());
            String status = "Lunas";
            String periode = tglIn + " s.d " + tglOut;

            Intent intent = new Intent(DetailKamarActivity.this, CetakKuitansiActivity.class);
            intent.putExtra("NAMA", nama);
            intent.putExtra("KAMAR", noKamar);
            intent.putExtra("PERIODE", periode);
            intent.putExtra("HARGA", harga);
            intent.putExtra("STATUS", status);
            intent.putExtra("METODE", "Tunai/Transfer");
            startActivity(intent);
        });
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
                        JSONObject json = new JSONObject(response.body().string());
                        tampilkanData(json);
                    } else { ambilDataOffline(); }
                } catch (Exception e) { ambilDataOffline(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) { ambilDataOffline(); }
        });
    }

    private void ambilDataOffline() {
        DatabaseHelper db = new DatabaseHelper(this);
        JSONObject jsonOffline = db.getDetailPendingSewa(noKamar);

        if (jsonOffline != null) {
            Toast.makeText(this, "Mode Offline", Toast.LENGTH_SHORT).show();
            tampilkanData(jsonOffline);
            // Tombol tetap aktif agar bisa edit offline
            btnUpdate.setEnabled(true);
            btnCheckout.setEnabled(true);
        } else {
            Toast.makeText(this, "Data tidak ditemukan (Offline)", Toast.LENGTH_SHORT).show();
        }
    }

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
                        if (response.isSuccessful()) {
                            Toast.makeText(DetailKamarActivity.this, "Data Berhasil Diupdate!", Toast.LENGTH_SHORT).show();
                        } else {
                            simpanUpdateOffline(idSewaSaatIni, nama, wa, kerja, tglIn, tglOut, durasi, total, bayar, status, noKamar);
                        }
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        simpanUpdateOffline(idSewaSaatIni, nama, wa, kerja, tglIn, tglOut, durasi, total, bayar, status, noKamar);
                    }
                });
    }

    private void simpanUpdateOffline(String idSewa, String nama, String wa, String kerja, String in, String out, String durasi, double total, double bayar, String status, String noKamar) {
        DatabaseHelper db = new DatabaseHelper(this);
        db.addPendingUpdateSewa(idSewa, nama, wa, kerja, in, out, durasi, total, bayar, status, noKamar);
        Toast.makeText(this, "Offline: Perubahan Disimpan di HP", Toast.LENGTH_SHORT).show();
    }

    private void dialogCheckout() {
        new AlertDialog.Builder(this)
                .setTitle("Konfirmasi Check-Out")
                .setMessage("Yakin ingin check-out?")
                .setPositiveButton("Ya", (dialog, which) -> {
                    ApiService api = RetrofitClient.getClient().create(ApiService.class);
                    api.prosesCheckout(noKamar).enqueue(new Callback<ResponseBody>() {
                        @Override
                        public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(DetailKamarActivity.this, "Berhasil Check-Out", Toast.LENGTH_SHORT).show();
                                finish();
                            } else { checkoutOffline(); }
                        }
                        @Override
                        public void onFailure(Call<ResponseBody> call, Throwable t) { checkoutOffline(); }
                    });
                })
                .setNegativeButton("Batal", null)
                .show();
    }

    private void checkoutOffline() {
        DatabaseHelper db = new DatabaseHelper(this);
        db.addPendingCheckout(noKamar);
        Toast.makeText(this, "Offline: Checkout Disimpan. Kamar jadi Kosong.", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String hitungDurasi(String in, String out) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            long diff = sdf.parse(out).getTime() - sdf.parse(in).getTime();
            long days = java.util.concurrent.TimeUnit.DAYS.convert(diff, java.util.concurrent.TimeUnit.MILLISECONDS);
            if (days < 7) return "Harian"; else if(days <= 13) return "Mingguan"; else return "Bulanan";
        } catch (Exception e) { return "Bulanan"; }
    }
}