package com.koneksi;

import com.aplikasi.tiketDispenser;
import gnu.io.*;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.io.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.view.JasperViewer;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfInt;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;

public class koneksiArduino implements SerialPortEventListener {
    
    tiketDispenser window = null;
    private Enumeration port = null;
    private final HashMap portMap = new HashMap();
    private CommPortIdentifier portIdentifier = null;
    private SerialPort serialPort = null;
    private InputStream input = null;
    private OutputStream output = null;
    private boolean serialConnected = false;
    final static int TIMEOUT = 2000;
    String dataIn = "";
    String statusPort = "";
    String masuk = "";
    String nokar = "";
    int id = 0;
    String jamsuk = "";
    char dataSerial = ' '; // menampung data dari serial port
    
    public koneksiArduino(tiketDispenser window) {
        this.window = window;
    }
    
    public void cekSerialPort(){
        port = CommPortIdentifier.getPortIdentifiers();
        while (port.hasMoreElements()) {
            CommPortIdentifier curPort = (CommPortIdentifier) port.nextElement();
            if (curPort.getPortType() == CommPortIdentifier.PORT_SERIAL) {
                window.jComboBox1.addItem(curPort.getName());
                portMap.put(curPort.getName(), curPort);
            }
        }
    }
    
    final public boolean getConnection() {
        return serialConnected;
    }
    
    public void setConnected(boolean serialConnected) {
        this.serialConnected = serialConnected;
    }
    
    public void connect() {
        String selectedPort = (String) window.jComboBox1.getSelectedItem();
        portIdentifier = (CommPortIdentifier) portMap.get(selectedPort);
        CommPort commPort = null;
        try {
            commPort = portIdentifier.open(null, TIMEOUT);
            serialPort = (SerialPort) commPort;
            setConnected (true);
            window.jButton1.setText("Disconnect");
        } catch (PortInUseException e) {
            statusPort = selectedPort + " is Busy. (" +e.toString()+ ")";
            JOptionPane.showMessageDialog(null, statusPort);
        } catch (Exception e) {
            statusPort = selectedPort + "Failed to opened. (" +e.toString()+ ")";
            JOptionPane.showMessageDialog(null, statusPort);
        }
    }
    
    public void disconnect() {
        try {
            serialPort.removeEventListener();
            serialPort.close();
            input.close();
            setConnected(false);
            statusPort = "Port Disconnection Sucessfuly";
            JOptionPane.showMessageDialog(null, statusPort);
            window.jButton1.setText("Connect");
        } catch (IOException | HeadlessException e) {
            statusPort = "Failed to close " +serialPort.getName() +"("+e.toString()+")";
            JOptionPane.showMessageDialog(null, statusPort);
        }
    }
    
    public boolean initIOStream() {
        boolean successful = false;
            try {
                input  = serialPort.getInputStream();
                output = serialPort.getOutputStream();
                
                successful = true;
            } catch (IOException e) {
                statusPort = "IO failed to open. ("+e.toString()+")";
                JOptionPane.showMessageDialog(null, statusPort);
                return successful;
        }
        return successful;
    }
    
    public void sendChar(Byte a) {
        //untuk kirim data ke arduino
    }
    
    public void initListener() {
        try {
            serialPort.addEventListener(this);
            serialPort.notifyOnDataAvailable(true);
            
        } catch (TooManyListenersException e) {
            JOptionPane.showMessageDialog(null, e.toString());
        }        
    }
    
    public void autoKodeKarcis() {
        try {
            Connection c = koneksiDB.getConnection();
            Statement st = c.createStatement();
            String sql = "SELECT * FROM parkir_keluar ORDER BY id DESC";
            ResultSet rs = st.executeQuery(sql);
            if (rs.next()) {
                String kode_id = rs.getString("id");
                String an = "" + (Integer.parseInt(kode_id) + 1);
                String kd_id = "" + (Integer.parseInt(kode_id));
                String nol = "";
                if (an.length() == kd_id.length()) {
                    nol = "0";
                }
                java.util.Date now = new java.util.Date();
                SimpleDateFormat bulanTahun = new SimpleDateFormat("MMyy");
                String tanggal = bulanTahun.format(now);
                SimpleDateFormat hari = new SimpleDateFormat("dd", Locale.getDefault());
                String tanggal2 = hari.format(now);
                String nol_jam = "", nol_menit ="", nol_detik ="";
                java.util.Date dateTime= new java.util.Date();
                int nilai_jam = dateTime.getHours();
                int nilai_menit = dateTime.getMinutes();
                int nilai_detik = dateTime.getSeconds();
                if (nilai_jam<=9)nol_jam = "0";
                if (nilai_menit<=9)nol_menit = "0";
                if(nilai_detik<=9)nol_detik = "0";
                String jam = nol_jam + Integer.toString(nilai_jam);
                String menit = nol_menit + Integer.toString(nilai_menit);
                String detik = nol_detik + Integer.toString(nilai_detik);
                nokar = jam+tanggal+nol+an+tanggal2+menit;
                System.out.println(nokar);
                }
        } catch (SQLException e) {
            
        }
    }
        void showTime() {    
        new javax.swing.Timer (0, (ActionEvent e) -> {
            Date d = new Date ();
            SimpleDateFormat s = new SimpleDateFormat ("yyyy-MM-dd HH:mm:ss");
            jamsuk = String.valueOf(s.format(d));
        } //menentukan jam masuk berdasarkan waktu real
        ).start();
    }
    
    public void autoFoto() {
        showTime();
        try {
            System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
            VideoCapture camera = new VideoCapture(0);
            int min = 10000;
            Random randomNum = new Random();
            int a = min + randomNum.nextInt();
            Connection kon = koneksiDB.getConnection();
            PreparedStatement ps = kon.prepareStatement("insert into parkir_keluar(id, nomor_karcis, "
                    + "jam_masuk, fotomasuk) values(?,?,?,?)");
            if(!camera.isOpened()){
                System.out.println("Error");
            }
            else {
                Mat frame = new Mat();
//                MatOfInt size = new MatOfInt(Highgui.IMWRITE_PNG_COMPRESSION, 9);
                while(true){
                    if (camera.read(frame)){
                        Highgui.imwrite("C:\\Users\\Singgih\\Documents\\NetBeansProjects\\"
                                + "Ticket Dispenser\\Foto\\camera"+a+".png", frame);
                        InputStream is = new FileInputStream("C:\\Users\\Singgih\\Documents\\"
                                + "NetBeansProjects\\Ticket Dispenser\\Foto\\camera"+a+".png");
                        ps.setInt(1, id);
                        ps.setString(2, nokar);
                        ps.setString(3, jamsuk);
                        ps.setBlob(4, is);
                        ps.executeUpdate();
                        break;
                    }
                }
            }
            camera.release();
        }
        catch(SQLException e) {
            JOptionPane.showMessageDialog(null,"Error "+e.getMessage());
        } catch (FileNotFoundException ex) {
            Logger.getLogger(koneksiArduino.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
        public void autoPrint() {
            try {
                String jRxml = "C:\\Users\\Singgih\\Documents\\NetBeansProjects\\Ticket Dispenser\\src\\com\\print\\tiketKarcis.jrxml";
                Connection kon = koneksiDB.getConnection();
                HashMap param = new HashMap();
                param.put("nokar", nokar);
                JasperReport jSpr = JasperCompileManager.compileReport(jRxml);
                JasperPrint jPrint = JasperFillManager.fillReport(jSpr, param, kon);
                JasperViewer.viewReport(jPrint, false);
//                System.out.println("berhasil");
//                            JasperPrintManager.printReport(jPrint,false);
            } catch (JRException e) {
                System.out.println("Error");
            } catch (SQLException f) {
                System.out.println("Error");
            }
        }
    
    @Override
    public void serialEvent(SerialPortEvent evt) {
        if (evt.getEventType() == SerialPortEvent.DATA_AVAILABLE) {
            try {
                if (dataSerial != '\n') {
                    dataSerial = (char) input.read();
                    dataIn = dataIn + String.valueOf(dataSerial);
//                    System.out.print(dataIn);
//                    System.out.print("\t");
//                    System.out.print(dataSerial);
                    if (dataIn.equals("1")) {
//                        System.out.println("berhasil");
                        autoKodeKarcis();
                        autoFoto();
                        autoPrint();
//                            test();
                    }
//                    System.out.print("\n");
                } else {
                    dataSerial = ' ';
                    dataIn = "";
                }
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null, e.toString());
            }
        }
    }
}