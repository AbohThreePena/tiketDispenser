package com.koneksi;

import java.sql.DriverManager;
import java.sql.Connection;
import java.sql.SQLException;

public class koneksiDB {
    private static Connection koneksi;
    public Connection getKoneksi() {
        return koneksi;
    } 
    public static Connection getConnection() throws SQLException{
            if(koneksi==null){
               try {
                   String url = "jdbc:mysql://localhost:3306/axia_parkir";
                   String usr = "root";
                   String pwd = "";
                   DriverManager.registerDriver(new com.mysql.jdbc.Driver());
                   koneksi = DriverManager.getConnection(url, usr, pwd);
               } catch(SQLException ex) {
                   ex.getStackTrace();
               }
        }
            return koneksi;
    }  
}
