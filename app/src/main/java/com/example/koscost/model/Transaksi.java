package com.example.koscost.model;

import com.google.gson.annotations.SerializedName;

public class Transaksi {
    @SerializedName("nama_penghuni")
    private String nama;

    @SerializedName("no_kamar")
    private String kamar;

    @SerializedName("total_tarif")
    private double harga;

    @SerializedName("tgl_checkin")
    private String tanggal;

    // Getter
    public String getNama() { return nama; }
    public String getKamar() { return kamar; }
    public double getHarga() { return harga; }
    public String getTanggal() { return tanggal; }
}