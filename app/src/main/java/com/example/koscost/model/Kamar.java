package com.example.koscost.model; // Pastikan nama package benar

import com.google.gson.annotations.SerializedName;

public class Kamar {

    // @SerializedName("nama_kolom_di_json")
    // Ini fungsinya mencocokkan nama variabel di PHP dengan di Java

    @SerializedName("id_kamar")
    private String idKamar;

    @SerializedName("no_kamar")
    private String noKamar;

    @SerializedName("status")
    private String status; // "0" = Kosong, "1" = Terisi

    @SerializedName("fasilitas")
    private String fasilitas;

    // Variabel Harga (Sesuai request kamu yang variatif)
    @SerializedName("harga_harian")
    private double hargaHarian;

    @SerializedName("harga_mingguan")
    private double hargaMingguan;

    @SerializedName("harga_bulanan")
    private double hargaBulanan;

    // --- Constructor (Kosongkan saja tidak apa-apa) ---
    public Kamar() {}

    // --- Getter (Wajib ada biar bisa dibaca) ---
    public String getIdKamar() { return idKamar; }
    public String getNoKamar() { return noKamar; }
    public String getStatus() { return status; }
    public String getFasilitas() { return fasilitas; }
    public double getHargaHarian() { return hargaHarian; }
    public double getHargaMingguan() { return hargaMingguan; }
    public double getHargaBulanan() { return hargaBulanan; }

    // --- Setter (Opsional, buat jaga-jaga) ---
    public void setNoKamar(String noKamar) { this.noKamar = noKamar; }
    public void setStatus(String status) { this.status = status; }
}