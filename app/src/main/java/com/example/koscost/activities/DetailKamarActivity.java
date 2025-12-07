package com.example.koscost.activities;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import com.example.koscost.R;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.utils.CurrencyTextWatcher; // Pastikan ini ada

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
    String noKamar, idSewaSaatIni;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_kamar);

        // Binding ID harus SAMA PERSIS dengan XML
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

        // Pasang Format Uang (Agar user mudah baca angka)
        etTotal.addTextChangedListener(new CurrencyTextWatcher(etTotal));
        etBayar.addTextChangedListener(new CurrencyTextWatcher(etBayar));

        // Setup Kalendar
        setupDatePicker(etTglIn);
        setupDatePicker(etTglOut);

        noKamar = getIntent().getStringExtra("NO_KAMAR");
        tvJudul.setText("Detail Kamar " + noKamar);

        // Load data saat pertama buka
        loadDataPenghuni();

        // Tombol Simpan Perubahan
        btnUpdate.setOnClickListener(v -> updateDataPenghuni());

        // Tombol Checkout
        btnCheckout.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Konfirmasi Check-Out")
                    .setMessage("Yakin ingin check-out? Status kamar akan menjadi KOSONG.")
                    .setPositiveButton("Ya", (dialog, which) -> prosesCheckout())
                    .setNegativeButton("Batal", null)
                    .show();
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
                        String res = response.body().string();
                        JSONObject json = new JSONObject(res);

                        idSewaSaatIni = json.optString("id_sewa"); // PENTING: ID ini dipakai buat update

                        etNama.setText(json.optString("nama_penghuni"));
                        etWa.setText(json.optString("no_wa"));
                        etKerja.setText(json.optString("pekerjaan"));
                        etTglIn.setText(json.optString("tgl_checkin"));
                        etTglOut.setText(json.optString("tgl_checkout"));

                        // Tampilkan harga (Format uang otomatis jalan karena TextWatcher)
                        double total = json.optDouble("total_tarif", 0);
                        double bayar = json.optDouble("sudah_dibayar", 0);

                        etTotal.setText(String.format(Locale.US, "%.0f", total));
                        etBayar.setText(String.format(Locale.US, "%.0f", bayar));
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DetailKamarActivity.this, "Gagal koneksi", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDataPenghuni() {
        String nama = etNama.getText().toString();
        String wa = etWa.getText().toString();
        String kerja = etKerja.getText().toString();
        String tglIn = etTglIn.getText().toString();
        String tglOut = etTglOut.getText().toString();

        // Ambil nilai uang asli (buang titiknya)
        double total = CurrencyTextWatcher.parseCurrency(etTotal.getText().toString());
        double bayar = CurrencyTextWatcher.parseCurrency(etBayar.getText().toString());

        String status = (bayar >= total) ? "Lunas" : "Belum Lunas";

        // Hitung ulang durasi otomatis
        String durasiBaru = hitungDurasiOtomatis(tglIn, tglOut);

        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.updateSewa(idSewaSaatIni, nama, wa, kerja, tglIn, tglOut, durasiBaru, total, bayar, status)
                .enqueue(new Callback<ResponseBody>() {
                    @Override
                    public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                        Toast.makeText(DetailKamarActivity.this, "Data Berhasil Diupdate!", Toast.LENGTH_SHORT).show();
                    }
                    @Override
                    public void onFailure(Call<ResponseBody> call, Throwable t) {
                        Toast.makeText(DetailKamarActivity.this, "Gagal Update", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String hitungDurasiOtomatis(String tglMasuk, String tglKeluar) {
        try {
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US);
            java.util.Date dateIn = sdf.parse(tglMasuk);
            java.util.Date dateOut = sdf.parse(tglKeluar);
            long diff = dateOut.getTime() - dateIn.getTime();
            long days = java.util.concurrent.TimeUnit.DAYS.convert(diff, java.util.concurrent.TimeUnit.MILLISECONDS);

            if (days < 7) return "Harian (" + days + " Hari)";
            else if (days <= 13) return "Mingguan";
            else return "Bulanan";
        } catch (Exception e) { return "Bulanan"; }
    }

    private void prosesCheckout() {
        ApiService api = RetrofitClient.getClient().create(ApiService.class);
        api.prosesCheckout(noKamar).enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                Toast.makeText(DetailKamarActivity.this, "Check-Out Berhasil!", Toast.LENGTH_SHORT).show();
                finish(); // Balik ke Dashboard
            }
            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Toast.makeText(DetailKamarActivity.this, "Gagal Checkout", Toast.LENGTH_SHORT).show();
            }
        });
    }
}