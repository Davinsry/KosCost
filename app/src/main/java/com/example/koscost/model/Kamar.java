package com.example.koscost.model;

import com.google.gson.annotations.SerializedName;

public class Kamar {

    @SerializedName("id_kamar")
    private String idKamar;

    @SerializedName("no_kamar")
    private String noKamar;

    @SerializedName("status")
    private String status; // "0" = Kosong, "1" = Terisi

    @SerializedName("fasilitas")
    private String fasilitas;

    @SerializedName("harga_harian")
    private double hargaHarian;

    @SerializedName("harga_mingguan")
    private double hargaMingguan;

    @SerializedName("harga_bulanan")
    private double hargaBulanan;

    // --- TAMBAHAN BARU (PENTING UNTUK OFFLINE) ---
    @SerializedName("nama_penghuni")
    private String namaPenghuni;

    @SerializedName("id_sewa")
    private String idSewa;

    // --- Constructor ---
    public Kamar() {}

    // --- Getter ---
    public String getIdKamar() { return idKamar; }
    public String getNoKamar() { return noKamar; }
    public String getStatus() { return status; }
    public String getFasilitas() { return fasilitas; }
    public double getHargaHarian() { return hargaHarian; }
    public double getHargaMingguan() { return hargaMingguan; }
    public double getHargaBulanan() { return hargaBulanan; }

    // Getter Baru
    public String getNamaPenghuni() { return namaPenghuni; }
    public String getIdSewa() { return idSewa; }

    // --- Setter ---
    public void setIdKamar(String idKamar) { this.idKamar = idKamar; }
    public void setNoKamar(String noKamar) { this.noKamar = noKamar; }
    public void setStatus(String status) { this.status = status; }
    public void setFasilitas(String fasilitas) { this.fasilitas = fasilitas; }
    public void setHargaHarian(double hargaHarian) { this.hargaHarian = hargaHarian; }
    public void setHargaMingguan(double hargaMingguan) { this.hargaMingguan = hargaMingguan; }
    public void setHargaBulanan(double hargaBulanan) { this.hargaBulanan = hargaBulanan; }

    // Setter Baru
    public void setNamaPenghuni(String namaPenghuni) { this.namaPenghuni = namaPenghuni; }
    public void setIdSewa(String idSewa) { this.idSewa = idSewa; }
}