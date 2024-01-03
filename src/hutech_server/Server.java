
package hutech_server;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

public class Server extends javax.swing.JFrame {

   private final ArrayList<ClientHandler> clients = new ArrayList<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(4);
    private List<PrintWriter> clientWriters = new ArrayList<>();
    private DefaultTableModel clientTableModel;
    private DefaultTableModel actionTableModel;

    private JPopupMenu popupMenu;

    public Server() {
        initComponents();
         init();

        Thread socketThread = new Thread(() -> {
            try {
                startSocket();
            } catch (IOException ex) {
                Logger.getLogger(Server.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
        socketThread.start();
    }
 public static void setJTableColumnsWidth(JTable table, int tablePreferredWidth,
            double... percentages) {
        // Calculate the total percentage: Ex: The total is 100% but if user enter 90% we must to calculate again!
        double total = 0;
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            total += percentages[i];
        }
        // From their percentages, we set prefered width for them 
        for (int i = 0; i < table.getColumnModel().getColumnCount(); i++) {
            TableColumn column = table.getColumnModel().getColumn(i);
            column.setPreferredWidth((int) (tablePreferredWidth * (percentages[i] / total)));
        }
    }
  public final void init() {
        clientTableModel = new DefaultTableModel();
        clientTable.setModel(clientTableModel);

        clientTableModel.addColumn("IP");
        clientTableModel.addColumn("Port");
        clientTableModel.addColumn("Trạng thái");
        clientTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        actionTableModel = new DefaultTableModel();
        actionTable.setModel(actionTableModel);

        actionTableModel.addColumn("IP");
        actionTableModel.addColumn("Port");
        actionTableModel.addColumn("Thời gian");
        actionTableModel.addColumn("Action");
        actionTableModel.addColumn("Mô tả");

        setJTableColumnsWidth(actionTable, 450, 10, 5, 15, 20, 50);

        actionTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        popupMenu = new JPopupMenu();
        addPopupMenu();
    }
   private ClientHandler getClientByRow(int row) {
        String ip = clientTable.getValueAt(row, 0).toString();
        int port = Integer.parseInt(clientTable.getValueAt(row, 1).toString());
        ClientHandler result = null;

        for (ClientHandler ch : clients) {
            Socket clientSocket = ch.getClient();

            if (clientSocket.getInetAddress().toString().equals(ip)
                    && clientSocket.getPort() == port) {
                result = ch;
                break;
            }
        }
        return result;
    }

    public void removeClientBySocket(Socket socket) {
        int size = clients.size();
        for (int i = 0; i < size; i++) {
            ClientHandler curClient = clients.get(i);

            if (curClient.getClient() == socket) {
                clients.remove(curClient);
                clientTableModel.removeRow(i);
            }
        }
    }

    public void addActionToTable(Action action) {
        this.actionTableModel.addRow(new Object[]{action.getIp(), action.getPort(), action.getDate(), action.getAction(), action.getDesc()});
    }

    private void addPopupMenu() {
        JMenuItem monitorItem = new JMenuItem("Theo dõi");
        JMenuItem disconnectItem = new JMenuItem("Ngắt kết nối");

        popupMenu.add(monitorItem);
        popupMenu.add(disconnectItem);

        monitorItem.addActionListener((e) -> {
            int selectedRow = clientTable.getSelectedRow();
            if (selectedRow != -1) {
                ClientHandler selectedClient = getClientByRow(selectedRow);

                if (selectedClient != null) {
                    ChooseFolder chooseFolderScreen = new ChooseFolder();
                    chooseFolderScreen.addClientHandler(selectedClient);
                    chooseFolderScreen.show();
                }
            }
        });

       disconnectItem.addActionListener((e) -> {
    int selectedRow = clientTable.getSelectedRow();
    if (selectedRow != -1) {
        ClientHandler selectedClient = getClientByRow(selectedRow);
        if (selectedClient != null) {
            try {
                clients.remove(selectedClient);
                clientTableModel.removeRow(selectedRow); // Xóa hàng trong bảng
                selectedClient.getClient().close();
            } catch (IOException ex) {
                System.out.println("Server.java, row 96, Error: " + ex);
            }
        }
    }
});
    }

    public void addClientToTable(ClientHandler client) {
       // Kiểm tra xem client đã tồn tại trong danh sách clients hay chưa
    boolean clientExists = false;
    for (int i = 0; i < clientTableModel.getRowCount(); i++) {
        String ip = clientTableModel.getValueAt(i, 0).toString();
        int port = Integer.parseInt(clientTableModel.getValueAt(i, 1).toString());
        Socket clientSocket = client.getClient();
        if (clientSocket.getInetAddress().toString().equals(ip)
                && clientSocket.getPort() == port) {
            clientExists = true;
            break;
        }
    }
    
    // Nếu client chưa tồn tại, thêm nó vào bảng
    if (!clientExists) {
        clientTableModel.addRow(new Object[]{
            client.getClient().getInetAddress().toString(),
            client.getClient().getPort(),
            "Kết nối"
        });
    }
    }

    public void renderDirectoryLabel(String directory) {
        var text = "";
        if (!directory.equals("")) {
            text = directory;
        } else {
            text = "Vui lòng chọn thư mục của client để quan sát";
        }

        pathLabel.setText(text);
    }

    public final void startSocket() throws IOException {
        ServerSocket server = null;

        try {
            InetAddress localHost = InetAddress.getLocalHost();
            server = new ServerSocket(3000, 50, localHost);

            ipInput.setText(localHost.getHostAddress());
               
            portInput.setText(3000 + "");

            while (true) {
                Socket socket = server.accept();
                ClientHandler clientThread = new ClientHandler(socket);
                clientThread.setServerView(this);

                clients.add(clientThread);
                addClientToTable(clientThread);

                pool.execute(clientThread);

            }
        } catch (IOException ex) {
            System.out.println(ex);
        } finally {
            if (server != null) {
                server.close();
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        pathLabel = new javax.swing.JLabel();
        portInput = new javax.swing.JTextField();
        ipInput = new javax.swing.JTextField();
        jScrollPane3 = new javax.swing.JScrollPane();
        clientTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        actionTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);

        jPanel1.setBackground(new java.awt.Color(192, 255, 251));

        jLabel1.setFont(new java.awt.Font("Segoe UI", 1, 24)); // NOI18N
        jLabel1.setText("Server");

        jLabel2.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel2.setText("IP:");

        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel3.setText("Port:");

        jLabel4.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel4.setText("Danh sách máy khách");

        jLabel6.setFont(new java.awt.Font("Segoe UI", 1, 12)); // NOI18N
        jLabel6.setText("Folder giám sát:");

        pathLabel.setText("Chọn client để xem folder");

        portInput.setText("3000");

        ipInput.setText("127.0.0.1");

        clientTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        clientTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                clientTableMouseClicked(evt);
            }
        });
        jScrollPane3.setViewportView(clientTable);

        actionTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {
                {},
                {},
                {},
                {}
            },
            new String [] {

            }
        ));
        actionTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                actionTableMouseClicked(evt);
            }
        });
        jScrollPane2.setViewportView(actionTable);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 296, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 697, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(436, 436, 436)
                        .addComponent(jLabel1))
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(20, 20, 20)
                                .addComponent(jLabel2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(ipInput, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(11, 11, 11)
                                .addComponent(jLabel3)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(portInput, javax.swing.GroupLayout.PREFERRED_SIZE, 136, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGap(21, 21, 21)
                                .addComponent(jLabel4)))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel6)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(pathLabel)))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel1Layout.createSequentialGroup()
                .addContainerGap(22, Short.MAX_VALUE)
                .addComponent(jLabel1)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addGap(45, 45, 45)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(portInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(ipInput, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel2))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel4)
                        .addGap(4, 4, 4))
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(pathLabel)
                            .addComponent(jLabel6))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)))
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.DEFAULT_SIZE, 370, Short.MAX_VALUE)
                    .addComponent(jScrollPane3, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)))
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void clientTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_clientTableMouseClicked
        // TODO add your handling code here:
          int selectedRow = clientTable.getSelectedRow();
        if (selectedRow != -1) {
            ClientHandler selectedClient = getClientByRow(selectedRow);

            if (selectedClient != null) {
                renderDirectoryLabel(selectedClient.getMonitoredDirectory());
            }
        }

        if (SwingUtilities.isRightMouseButton(evt)) {
            if (clientTable.getSelectedRows().length > 0) {
                popupMenu.show(clientTable, evt.getX(), evt.getY());
            }
        }
    }//GEN-LAST:event_clientTableMouseClicked

    private void actionTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_actionTableMouseClicked
        // TODO add your handling code here:
          int selectedRow = actionTable.getSelectedRow();
        if (selectedRow != -1) {
            ClientHandler selectedClient = getClientByRow(selectedRow);

            if (selectedClient != null) {
                renderDirectoryLabel(selectedClient.getMonitoredDirectory());
            }
        }
    }//GEN-LAST:event_actionTableMouseClicked
  
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
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
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
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Server.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new Server().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable actionTable;
    private javax.swing.JTable clientTable;
    private javax.swing.JTextField ipInput;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JLabel pathLabel;
    private javax.swing.JTextField portInput;
    // End of variables declaration//GEN-END:variables
}
