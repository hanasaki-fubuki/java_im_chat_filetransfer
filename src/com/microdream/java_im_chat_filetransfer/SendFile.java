package com.microdream.java_im_chat_filetransfer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.swing.*;
import java.awt.*;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.security.KeyStore;
import java.util.List;

public class SendFile extends JFrame {
	private JTextField textFieldFilePath;
	private SSLSocket sslSocket;
	private long size;
	private JProgressBar progressBar;
	private JLabel lblSpeed;

	public SendFile(String drcUser, String filePath, int port, FileInputStream fis, long fileLength) {

		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(100, 100, 431, 163);
		JPanel panel = new JPanel();
		getContentPane().add(panel, BorderLayout.CENTER);
		panel.setLayout(null);
		panel.setToolTipText("给" + drcUser + "的发送进度");
		
		textFieldFilePath = new JTextField();
		textFieldFilePath.setText("<dynamic>");
		textFieldFilePath.setToolTipText("");
		textFieldFilePath.setBounds(10, 22, 385, 26);
		panel.add(textFieldFilePath);
		textFieldFilePath.setColumns(10);
		textFieldFilePath.setText(filePath);
		textFieldFilePath.setEditable(false);
		
		progressBar = new JProgressBar();
		progressBar.setToolTipText("");
		progressBar.setBounds(50, 80, 304, 20);
		panel.add(progressBar);
		
		lblSpeed = new JLabel("", JLabel.CENTER);
		lblSpeed.setBounds(50, 58, 132, 16);
		panel.add(lblSpeed);
		lblSpeed.setVisible(false);

		try {
			sslSocket = createSSLSocket(port);
			OutputStream os = sslSocket.getOutputStream();
			UpdateProgressBar updateProgressBar = new UpdateProgressBar(os, fis);
			updateProgressBar.execute();
			size = fileLength;
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public SSLSocket createSSLSocket(int port) throws Exception {
		String passphrase = "123456";
		char[] password = passphrase.toCharArray();
		String trustStoreFile = "mykeys.keystore";
		KeyStore ts = KeyStore.getInstance("PKCS12");
		ts.load(new FileInputStream(trustStoreFile), password);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(ts);
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, tmf.getTrustManagers(), null); 
		SSLSocketFactory factory = sslContext.getSocketFactory();
		sslSocket = (SSLSocket) factory.createSocket("localhost", port);
		return (SSLSocket) sslSocket;
	}

	class UpdateProgressBar extends SwingWorker<String, String> {
		private OutputStream os;
		private FileInputStream fis;

		public UpdateProgressBar(OutputStream os, FileInputStream fis) {
			this.os = os;
			this.fis = fis;
		}

		// 重载函数在后台传输文件
		@Override
		protected String doInBackground() throws Exception {
			int n = 0;
			long sum = 0;
			System.out.println("文件总长度：" + size);
			byte[] buffer = new byte[1024];
			// 把数据读到字节数组中
			while ((n = fis.read(buffer)) != -1) {
				os.write(buffer, 0, n);
				os.flush();
				sum = sum + n;
				publish("已发送:" + sum / 1024 + "KB/" + size / 1024 + "KB" + '\n');
			}
			if (n == -1) {
				os.close();
				sslSocket.close();
				fis.close();
				System.out.println("关闭文件传输连接");
				setVisible(false);
			}
			return null;
		}

		// 重载进程传输文件
		@Override
		protected void process(List<String> chunks) {
			for (String string : chunks) {
				System.out.println(string);
				string = string.replace(":", " ");
				string = string.replace("KB", " ");
				String[] submit = new String[10];
				submit = string.split(" ");
				float persent = Float.parseFloat(submit[1]) / (float) (size / 1024);
				progressBar.setVisible(true);
				progressBar.setValue((int) (persent * 100));
				lblSpeed.setText("传输进度:" + (int) (persent * 100) + "%");
				lblSpeed.setVisible(true);
			}
		}
	}
}
