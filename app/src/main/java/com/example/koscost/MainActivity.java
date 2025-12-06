package com.example.koscost; // Sesuaikan package

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.koscost.adapter.KamarAdapter;
import com.example.koscost.api.ApiService;
import com.example.koscost.api.RetrofitClient;
import com.example.koscost.model.Kamar;

import java.util.List;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView rvKamar;
    private KamarAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Pastikan di XML-mu ada RecyclerView id: rv_kamar

        rvKamar = findViewById(R.id.rv_kamar);
        rvKamar.setLayoutManager(new GridLayoutManager(this, 2)); // Grid 2 Kolom

        loadDataKamar();
    }

    // Jangan lupa di activity_main.xml tambahkan <androidx.recyclerview.widget.RecyclerView android:id="@+id/rv_kamar" ... />

    private void loadDataKamar() {
        ApiService apiService = RetrofitClient.getClient().create(ApiService.class);
        Call<List<Kamar>> call = apiService.getDaftarKamar();

        call.enqueue(new Callback<List<Kamar>>() {
            @Override
            public void onResponse(Call<List<Kamar>> call, Response<List<Kamar>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Kamar> listKamar = response.body();
                    adapter = new KamarAdapter(MainActivity.this, listKamar);
                    rvKamar.setAdapter(adapter);
                } else {
                    Toast.makeText(MainActivity.this, "Gagal memuat data", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Kamar>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}