
package hutech_client;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JOptionPane;


public class Client extends javax.swing.JFrame {
    
    private Client _this;// tham chiếu đến đối tượng client
    // các chuỗi xác định lệnh giữa client và server
    public static String GET_ALL_DISKS_AND_FOLDER = "GET_ALL_DISKS_AND_FOLDER";
    public static String DISCONNECT = "DISCONNECT";
    public static String SEND_MONITORED_FOLDER = "SEND_MONITORED_FOLDER";
    public static String CREATE_FILE = "CREATE_FILE";
    public static String DELETE_FILE = "DELETE_FILE";
    public static String MODIFY_FILE = "MODIFY_FILE";
    public static String CREATE_FOLDER = "CREATE_FOLDER";
    public static String DELETE_FOLDER = "DELETE_FOLDER";
    public static String MODIFY_FOLDER = "MODIFY_FOLDER";
    
    private Socket client; // kết nối và truyền dữ liệu giữa client và server
    private DataOutputStream dout = null; // gửi dữ liệu đến server
    private DataInputStream din = null;// nhận dữ liệu từ server
    private boolean isConnecting = false; // xác nhận trạng thái của client
    private boolean inactiveConnection = false;// xác nhận trạng thái của client
   
    public Client() throws UnknownHostException {
        try {
              initComponents();
        _this = this;
        init();
        // xử lý sự kiện đóng cửa sổ đột ngột
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                super.windowClosing(e);
                if (isConnecting) {
                    try {
                        dout.writeUTF(Client.DISCONNECT);
                        dout.flush();
                        client.close();
                    } catch (IOException ex) {
                        System.out.println("Client.java, row: 276, ERROR: " + ex);
                    }
                }
            }

        });
        
	  
        } catch (Exception e) {
        }
        
    }
    // khởi tạo giá trị mặc định cho địa chỉ ip , lấy địa chỉ ip của localhost làm địa chỉ mặc định
 public final void init() throws UnknownHostException {
        InetAddress localHost = InetAddress.getLocalHost();
        hostInput.setText(localHost.getHostAddress());
        
	  
    }
public final void startClientSocket(String host, int port) throws IOException {
        try {
            // kết nối với server qua host và port
            client = new Socket(host, port);
            client.setTcpNoDelay(true);
            isConnecting = true;
            inactiveConnection = false;
            // thiết lập các luồng dữ liệu để gửi và nhận 
            dout = new DataOutputStream(client.getOutputStream());
            din = new DataInputStream(client.getInputStream());

            String sendMsg = "", receiveMsg;
            statusLabel.setText("Kết nối thành công");
            // nhận request từ server và xử lý
            while (true) {
                receiveMsg = din.readUTF();
                // client nhận request lấy tất cả ổ đĩa và thư mục từ server
                if (receiveMsg.equals(Client.GET_ALL_DISKS_AND_FOLDER)) {
                    File[] disks;
                    dout.writeUTF(receiveMsg);

                    // returns pathnames for disks
                    disks = File.listRoots();
                    int numberOfDisk = disks.length - 1;

                    // for each pathname in pathname array
                    dout.writeUTF(numberOfDisk + "");
                    dout.flush();

                    for (File disk : disks) {
                        if (!disk.getPath().equals("C:\\")) {
                            sendDirectory(disk, dout);
                        }
                    }
                    // client nhận request theo dõi thư mục được chỉ định từ server
                } else if (receiveMsg.equals(Client.SEND_MONITORED_FOLDER)) {
                    String folderPath = din.readUTF();

                    WatchService watcher = FileSystems.getDefault().newWatchService();
                    Path monitoredPath = Path.of(folderPath);

                    WatchKey key = monitoredPath.register(watcher,
                            StandardWatchEventKinds.ENTRY_CREATE,
                            StandardWatchEventKinds.ENTRY_DELETE,
                            StandardWatchEventKinds.ENTRY_MODIFY);

                    while (true) {

                        for (WatchEvent<?> event : key.pollEvents()) {
                            WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                            Path fileName = pathEvent.context();

                            WatchEvent.Kind<?> kind = event.kind();

                            boolean isDirectory = Files.isDirectory(Path.of(folderPath).resolve(fileName));
                            String type = "";

                            if (kind == StandardWatchEventKinds.ENTRY_CREATE) {
                                if (isDirectory) {
                                    type = CREATE_FOLDER;
                                    System.out.println("A new folder is created : " + fileName);
                                } else {
                                    type = CREATE_FILE;
                                    System.out.println("A new file is created : " + fileName);
                                }

                            } else if (kind == StandardWatchEventKinds.ENTRY_DELETE) {
                                // When deleting a file or folder, we cann't check if it is a folder or file because
                                // it is deleted so i use a trick that file'name often has dot (.). Ex: data.txt or data.docx
                                // I know that trick isn't perfect but I don't know whatever way to do

                                if (isDirectory || !fileName.toString().contains(".")) {
                                    type = DELETE_FOLDER;
                                    System.out.println("A new folder is deleted : " + fileName);
                                } else {
                                    type = DELETE_FILE;
                                    System.out.println("A new file is deleted : " + fileName);
                                }
                            } else if (kind == StandardWatchEventKinds.ENTRY_MODIFY) {
                                if (isDirectory) {
                                    type = MODIFY_FOLDER;
                                    System.out.println("A new folder is modified : " + fileName);
                                } else {
                                    type = MODIFY_FILE;
                                    System.out.println("A new file is modified : " + fileName);
                                }
                            }

                            sendMsg = "MONITOR - " + type + " - " + fileName.toString();
                        }

                        if (!sendMsg.equals("")) {
                            dout.writeUTF(sendMsg);
                            dout.flush();
                            sendMsg = "";
                        }

                        boolean valid = key.reset();
                        if (!valid) {
                            break;
                        }
                    }
                }
            }

        } catch (IOException ex) {
            System.out.println(ex);
            statusLabel.setText("Kết nối thất bại!");
            if (!inactiveConnection) {
                JOptionPane.showMessageDialog(_this,
                        "Vui lòng kiểm tra IP và Port server đúng không?\nHoặc có thể server chưa hoạt động\nHoặc bạn bị server ngắt kết nối",
                        "Lỗi kết nối",
                        JOptionPane.WARNING_MESSAGE);
            }

        } finally {
            if (dout != null && din != null && client != null) {
                dout.close();
                din.close();
                client.close();
                isConnecting = false;
            }
        }
    }
// gửi request thông tin thư mục đến cho server
 private void sendDirectory(File directory, DataOutputStream dout) throws IOException {
    if (directory.isDirectory()) {
        // Send folder name to server
        int length = directory.getPath().length();
        String folderName = (length == 3) ? directory.getPath() : directory.getName();
        dout.writeUTF(folderName);
        // Get all subfolders and files in the current folder to send to the server
        File[] files = directory.listFiles();
        int numberOfDirectories = 0;

        // Count the number of subfolders
        if (files != null) {
            for (File file : files) {
                // Check if it is a folder
                if (file.isDirectory()) {
                    if (!file.getPath().contains("RECYCLE.BIN") && !file.getPath().contains("System Volume Information")) {
                        numberOfDirectories++;
                    }
                }
            }
        }

        dout.writeInt(numberOfDirectories);
        dout.flush();

        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    if (!file.getPath().contains("RECYCLE.BIN") && !file.getPath().contains("System Volume Information")) {
                        sendDirectory(file, dout);
                    }
                }
            }
        }
    }
}
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        hostInput = new javax.swing.JTextField();
        jLabel7 = new javax.swing.JLabel();
        statusLabel = new javax.swing.JLabel();
        portInput = new javax.swing.JTextField();
        jLabel6 = new javax.swing.JLabel();
        disconnectBtn = new javax.swing.JButton();
        connectBtn1 = new javax.swing.JButton();
        btnThoat = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(32767, 32767));
        setPreferredSize(new java.awt.Dimension(450, 300));

        jPanel1.setBackground(new java.awt.Color(153, 255, 153));
        jPanel1.setFont(new java.awt.Font("Segoe UI Semibold", 1, 12)); // NOI18N

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Client");

        jLabel5.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel5.setText("Kết nối đến Server để giám sát");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel4.setText("Nhập IP Server:");

        hostInput.setText("192.168.137.1");
        hostInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                hostInputActionPerformed(evt);
            }
        });

        jLabel7.setFont(new java.awt.Font("Segoe UI", 1, 14)); // NOI18N
        jLabel7.setText("Trạng thái");

        statusLabel.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        statusLabel.setForeground(new java.awt.Color(255, 0, 0));
        statusLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        statusLabel.setText("Chưa kết nối");

        portInput.setText("3000");
        portInput.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                portInputActionPerformed(evt);
            }
        });

        jLabel6.setFont(new java.awt.Font("Segoe UI", 0, 14)); // NOI18N
        jLabel6.setText("Nhập Port:");

        disconnectBtn.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        disconnectBtn.setText("Ngắt kết nối");
        disconnectBtn.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                disconnectBtnActionPerformed(evt);
            }
        });

        connectBtn1.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        connectBtn1.setText("Kết nối");
        connectBtn1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                connectBtn1ActionPerformed(evt);
            }
        });

        btnThoat.setText("Thoát");
        btnThoat.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                btnThoatActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel4)
                    .addComponent(jLabel6)
                    .addComponent(disconnectBtn))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(148, 148, 148)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(portInput, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(statusLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 159, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(98, 98, 98)
                        .addComponent(connectBtn1)
                        .addGap(111, 111, 111)
                        .addComponent(btnThoat))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(hostInput, javax.swing.GroupLayout.PREFERRED_SIZE, 118, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(jLabel7)
                        .addGap(47, 47, 47))))
            .addComponent(jLabel5)
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel1)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(22, 22, 22)
                        .addComponent(jLabel7)
                        .addGap(30, 30, 30)
                        .addComponent(statusLabel))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(jLabel5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4)
                            .addComponent(hostInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGap(29, 29, 29)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel6)
                            .addComponent(portInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addGap(99, 99, 99)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(btnThoat)
                    .addComponent(connectBtn1)
                    .addComponent(disconnectBtn))
                .addContainerGap(44, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void hostInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_hostInputActionPerformed
        
    }//GEN-LAST:event_hostInputActionPerformed

    private void portInputActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_portInputActionPerformed
    
    }//GEN-LAST:event_portInputActionPerformed

    private void disconnectBtnActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_disconnectBtnActionPerformed
 
      if (isConnecting) {
            try {
                dout.writeUTF(Client.DISCONNECT);
                dout.flush();
                Thread.sleep(500);
                client.close();
            } catch (IOException e) {
                System.out.println("Client.java, row: 276, ERROR: " + e);
            } catch (InterruptedException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            isConnecting = false;
            inactiveConnection = true;

        } else {
            JOptionPane.showMessageDialog(this,
                    "Bạn chưa kết nối tới server nào!",
                    "Chưa kết nối!",
                    JOptionPane.INFORMATION_MESSAGE);
        }
    }//GEN-LAST:event_disconnectBtnActionPerformed
    private void disconnect()
    {
         if (isConnecting) {
            try {
                dout.writeUTF(Client.DISCONNECT);
                dout.flush();
                Thread.sleep(500);
                client.close();
            } catch (IOException e) {
                System.out.println("Client.java, row: 276, ERROR: " + e);
            } catch (InterruptedException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
            isConnecting = false;
            inactiveConnection = true;
        } 
    }
    private void connectBtn1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_connectBtn1ActionPerformed
         if (!isConnecting) {
            String host = hostInput.getText();
            int port = Integer.parseInt(portInput.getText());

            if (host.length() > 0 && port > 0) {
                Thread socketThread = new Thread(() -> {
                    try {
                        startClientSocket(host, port);
                    } catch (IOException ex) {
                        System.out.println(ex);
                    }
                });
                socketThread.start();

            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Bạn đang kết nối tới server",
                    "Đang kết nối!",
                    JOptionPane.INFORMATION_MESSAGE);
        }
      
    }//GEN-LAST:event_connectBtn1ActionPerformed

    private void btnThoatActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_btnThoatActionPerformed
     int result = JOptionPane.showInternalOptionDialog(
    null,
    "Bạn Có Muốn Thoát Chương Trình Này !",
    "DISCONNECT",
    JOptionPane.YES_NO_OPTION,
    JOptionPane.QUESTION_MESSAGE,
    null,
    null,
    null
    );

    if (result == JOptionPane.YES_OPTION) {
    // Thực hiện ngắt kết nối và thoát khỏi ứng dụng
    disconnect();
    System.exit(0);
        }
    }//GEN-LAST:event_btnThoatActionPerformed


   
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Client.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>
        
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(() -> {
            try {
                new Client().setVisible(true);
            } catch (IOException ex) {
                Logger.getLogger(Client.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton btnThoat;
    private javax.swing.JButton connectBtn1;
    private javax.swing.JButton disconnectBtn;
    private javax.swing.JTextField hostInput;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JTextField portInput;
    private javax.swing.JLabel statusLabel;
    // End of variables declaration//GEN-END:variables
}
