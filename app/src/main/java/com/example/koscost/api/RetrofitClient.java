package com.example.koscost.api; // Sesuaikan dengan nama packagemu

import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {

    // Nanti ganti IP ini dengan IP VPS kamu atau URL n8n
    // Penting: Akhiri dengan tanda miring '/'
    // Contoh: "http://192.168.1.5/kos-api/" atau domain kamu
    // IP VPS BARU
    private static final String BASE_URL = "http://43.157.207.164/kos-api/";
    // Catatan: 10.0.2.2 adalah localhost khusus untuk Emulator Android.
    // Kalau pakai HP asli, harus pakai IP Laptop (misal 192.168.x.x) atau IP VPS Public.

    private static Retrofit retrofit = null;

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }
}