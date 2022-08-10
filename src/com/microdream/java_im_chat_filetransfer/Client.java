/*****************************************************************************************************
 * �������Ȩ��Mengyun199���У�All Rights Reserved (C) 2022-
 *****************************************************************************************************
 * ������hanasaki-workstation
 * ��¼�û���Mengyun Jia
 * �������ƣ�hanasaki-workstation
 * ��ϵ�����䣺jiamengyun1024@outlook.com
 *****************************************************************************************************
 * ������ݣ�2022
 * �����ˣ�Mengyun Jia
 *****************************************************************************************************/

package com.microdream.java_im_chat_filetransfer;

import javax.net.ssl.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.net.*;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Client extends JFrame {

	DatagramSocket p2pSocket;
	private SSLServerSocket serverSocket;
	private final int port = 9999;
	private SSLSocket socket;
	private ObjectInputStream ois;// �ͻ���������ois��������Ϣ����װ�ɶ���������
	private ObjectOutputStream oos;// �ͻ���ͨ��oos�����˷�����Ϣ��socket�������װ�ɶ��������
	private String localUserName = "";
	private String localPasswd;

	// �������û��б�ListModel��,����ά���������û��б�����ʾ������
	private final DefaultListModel<String> onlineUserDlm = new DefaultListModel<String>();
	// ���ڿ���ʱ����Ϣ��ʾ��ʽ
	// private final SimpleDateFormat dateFormat = new
	// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");

	private final JPanel contentPane;
	private final JTextField textFieldUserName;
	private final JPasswordField passwordFieldPwd;
	private final JTextField textFieldMsgToSend;
	private final JTextPane textPaneMsgRecord;
	private final JList<String> listOnlineUsers;
	private final JButton btnLogon;
	private final JButton btnSendMsg;
	private final JButton btnSendFile;
	private JCheckBox chckbxPrivateChat;
	private String fileSelectPath;
	private String fileSavePath;
	private String fileName;
	private long fileLength;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					Client frame = new Client();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public Client() {
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				// ������������û��˳���Ϣ
				if (localUserName != "") {
					UserLogoffMessage userLogoffMessage = new UserLogoffMessage(localUserName);
					try {
						synchronized (oos) {
							oos.writeObject(userLogoffMessage);
							oos.flush();
						}
						System.exit(0);
					}
					catch (IOException ex) {
						ex.printStackTrace();
					}
				} else {
					System.exit(0);
				}
			}
		});

		setTitle("\u5BA2\u6237\u7AEF");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 612, 397);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JPanel panelNorth = new JPanel();
		panelNorth.setBorder(new EmptyBorder(0, 0, 5, 0));
		contentPane.add(panelNorth, BorderLayout.NORTH);
		panelNorth.setLayout(new BoxLayout(panelNorth, BoxLayout.X_AXIS));

		JLabel lblUserName = new JLabel("\u7528\u6237\u540D\uFF1A");
		panelNorth.add(lblUserName);

		textFieldUserName = new JTextField();
		panelNorth.add(textFieldUserName);
		textFieldUserName.setColumns(10);

		Component horizontalStrut = Box.createHorizontalStrut(20);
		panelNorth.add(horizontalStrut);

		JLabel lblPwd = new JLabel("\u53E3\u4EE4\uFF1A");
		panelNorth.add(lblPwd);

		passwordFieldPwd = new JPasswordField();
		passwordFieldPwd.setColumns(10);
		panelNorth.add(passwordFieldPwd);

		Component horizontalStrut5 = Box.createHorizontalStrut(20);
		panelNorth.add(horizontalStrut5);
		btnLogon = new JButton("\u767B\u5F55"); // ����¼����ť
		btnLogon.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (btnLogon.getText().equals("��¼")) {
					localUserName = textFieldUserName.getText().trim();// ȡ�û�����ɾ������ո�
					localPasswd = passwordFieldPwd.getText();// ��ȡ�û�����
					if (localUserName.length() > 0) {
						// ��������˽���Socket���ӣ�����׳��쳣���򵯳��Ի���֪ͨ�û������˳�
						try {
							// if (socket == null) { // socket��Ϊ�գ��������ڡ��û�ע�ᡱʱ�Ѿ������������Socket������
							// socket = new Socket("localhost", port);// �ͻ������������������
							// ��socket����������������ֱ��װ�ɶ����������Ͷ��������
							socket = createSSLSocket();
							oos = new ObjectOutputStream(socket.getOutputStream());// socket�������װ�ɶ��������
							ois = new ObjectInputStream(socket.getInputStream());// socket��������װ�ɶ���������
							// }
						} catch (UnknownHostException ex) {
							JOptionPane.showMessageDialog(null, "�Ҳ�������������");
							return;
						} catch (IOException ex) {
							JOptionPane.showMessageDialog(null, "������I/O���󣬷�����δ������");
							return;
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						// ������������û�������Ϣ�����Լ����û����Ϳ���͸�������
						// ����̶�Ϊ"nopassword"����Ҫ�ĳɴӽ����JPassword����л�ȡ����
						UserLogonMessage userLogonMessage = new UserLogonMessage(localUserName, localPasswd);

						try {
							oos.writeObject(userLogonMessage);
							oos.flush();
							// �ڡ���Ϣ��¼���ı������ú�ɫ��ӡ����ڵ�¼...������Ϣ
							addMsgRecord("���ڵ�¼...\r\n", Color.red, 12, false, false);
							// ���շ�������������Ӧ�����Ϣ
							ResultMessage msg = (ResultMessage) ois.readObject();
							if (msg.getResult() == ResultMessage.LOGON_SUCCESS) {

								// �ڡ���Ϣ��¼���ı������ú�ɫ��ӡ�XXʱ���¼�ɹ�������Ϣ
								String msgRecord = dateFormat.format(new Date()) + " ��¼�ɹ�\r\n";
								addMsgRecord(msgRecord, Color.red, 12, false, false);

								// ��������������̨�����̡߳�,�����������������������Ϣ
								new Thread(new ListeningHandler()).start();
								// ������¼����ť��Ϊ���˳�����ť
								btnLogon.setText("�˳�");
								// �������ļ���ť��Ϊ����״̬
								btnSendFile.setEnabled(true);
								// ��������Ϣ��ť��Ϊ����״̬
								btnSendMsg.setEnabled(true);
							} else if (msg.getResult() == ResultMessage.LOGON_FAILURE) {
								// �ڡ���Ϣ��¼���ı������ú�ɫ��ӡ�XXʱ���¼ʧ�ܡ�����Ϣ
								String msgRecord = dateFormat.format(new Date()) + " ��¼ʧ�ܣ�" + msg.getMsg() + "\r\n";
								addMsgRecord(msgRecord, Color.red, 12, false, false);
							}
						} catch (IOException ex) {
							ex.printStackTrace();
						} catch (ClassNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				} else if (btnLogon.getText().equals("�˳�")) {
					if (JOptionPane.showConfirmDialog(null, "�Ƿ��˳�?", "�˳�ȷ��",
							JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
						// ������������û��˳���Ϣ
						UserLogoffMessage userLogoffMessage = new UserLogoffMessage(localUserName);
						try {
							oos.writeObject(userLogoffMessage);
							oos.flush();
							// �ڡ���Ϣ��¼���ı������ú�ɫ��ӡ������˳�...������Ϣ
							addMsgRecord("�����˳�...\r\n", Color.red, 12, false, false);
						} catch (IOException ex) {
							ex.printStackTrace();
						}
					}
				}

			}
		});
		panelNorth.add(btnLogon);

		Component horizontalStrut1 = Box.createHorizontalStrut(20);
		panelNorth.add(horizontalStrut1);

		// ע�ᰴť
		JButton btnRegister = new JButton("\u6CE8\u518C");
		btnRegister.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				SubFrameRegister frame = new SubFrameRegister();
				frame.setVisible(true);
			}
		});
		panelNorth.add(btnRegister);

		JSplitPane splitPaneCenter = new JSplitPane();
		splitPaneCenter.setResizeWeight(1.0);
		contentPane.add(splitPaneCenter, BorderLayout.CENTER);

		JScrollPane scrollPaneMsgRecord = new JScrollPane();
		scrollPaneMsgRecord.setViewportBorder(new TitledBorder(UIManager.getBorder("TitledBorder.border"),
				"\u6D88\u606F\u8BB0\u5F55", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		splitPaneCenter.setLeftComponent(scrollPaneMsgRecord);

		textPaneMsgRecord = new JTextPane();
		textPaneMsgRecord.setEditable(false);
		scrollPaneMsgRecord.setViewportView(textPaneMsgRecord);

		JScrollPane scrollPaneOnlineUsers = new JScrollPane();
		scrollPaneOnlineUsers.setViewportBorder(
				new TitledBorder(null, "\u5728\u7EBF\u7528\u6237", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		splitPaneCenter.setRightComponent(scrollPaneOnlineUsers);

		listOnlineUsers = new JList<String>(onlineUserDlm);
		scrollPaneOnlineUsers.setViewportView(listOnlineUsers);

		JPanel panelSouth = new JPanel();
		panelSouth.setBorder(new EmptyBorder(5, 0, 0, 0));
		contentPane.add(panelSouth, BorderLayout.SOUTH);
		panelSouth.setLayout(new BoxLayout(panelSouth, BoxLayout.X_AXIS));

		textFieldMsgToSend = new JTextField();
		panelSouth.add(textFieldMsgToSend);
		textFieldMsgToSend.setColumns(10);

		Component horizontalStrut4 = Box.createHorizontalStrut(20);
		panelSouth.add(horizontalStrut4);

		chckbxPrivateChat = new JCheckBox("\u79C1\u804A");
		panelSouth.add(chckbxPrivateChat);

		Component horizontalStrut_2 = Box.createHorizontalStrut(20);
		panelSouth.add(horizontalStrut_2);

		// ��������Ϣ����ť
		btnSendMsg = new JButton("biu ~~~");
		btnSendMsg.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String msgContent = textFieldMsgToSend.getText();
				if (msgContent.length() > 0) {
					//�����ж��Ƿ�Ϊp2p˽����Ϣ
					String pattern = "<p2p>.*";
					boolean isMatch = Pattern.matches(pattern, msgContent);
					if (chckbxPrivateChat.isSelected() && isMatch == false) {// ˽��
						// ����Ϣ�ı����е�������Ϊ˽����Ϣ���͸�������
						String dstUser = listOnlineUsers.getSelectedValue();
						if (dstUser != null) {
							PrivateChatMessage privateChatMessage = new PrivateChatMessage(localUserName, msgContent,
									dstUser);
							try {
								oos.writeObject(privateChatMessage);
								oos.flush();
							} catch (IOException ex) {
								if (ex.getMessage().contains("Socket closed")) {
									JOptionPane.showMessageDialog(Client.this, "�������Ѿ��ر�");
									return;
								} else {
									ex.printStackTrace();
								}
							}
							// �ڡ���Ϣ��¼���ı���������ɫ��ʾ���͵���Ϣ������ʱ��
							String msgRecord = dateFormat.format(new Date()) + "��" + dstUser + "˵:" + msgContent + "\r\n";
							addMsgRecord(msgRecord, Color.blue, 12, false, false);
						}
					} else if (chckbxPrivateChat.isSelected() && isMatch == true) {
						String dstUser = listOnlineUsers.getSelectedValue();
						Matcher msg_ext = Pattern.compile("<p2p>.*").matcher(msgContent);
						P2PChatMessage msg = new P2PChatMessage("", 0, localUserName, dstUser);
						try {
							synchronized (oos) {
								oos.writeObject(msg);
								oos.flush();
							}
						} catch (IOException e1) {
							e1.printStackTrace();
						}
						String  msgRecord = dateFormat.format(new Date()) + " ��"+ dstUser +"˵(P2P):" + msgContent + "\r\n";
						addMsgRecord(msgRecord, Color.blue, 12, false, false);
					} else {
						// ����Ϣ�ı����е�������Ϊ������Ϣ���͸�������
						PublicChatMessage publicChatMessage = new PublicChatMessage(localUserName, msgContent);
						try {
							oos.writeObject(publicChatMessage);
							oos.flush();
						} catch (IOException ex) {
							if (ex.getMessage().contains("Socket closed")) {
								JOptionPane.showMessageDialog(Client.this, "�������Ѿ��ر�");
								return;
							} else {
								ex.printStackTrace();
							}
						}
						// �ڡ���Ϣ��¼���ı���������ɫ��ʾ���͵���Ϣ������ʱ��
						String msgRecord = dateFormat.format(new Date()) + "����˵:" + msgContent + "\r\n";
						addMsgRecord(msgRecord, Color.blue, 12, false, false);

					}
				}
			}
		});
		panelSouth.add(btnSendMsg);

		Component horizontalStrut_3 = Box.createHorizontalStrut(20);
		panelSouth.add(horizontalStrut_3);

		// �����ļ���ť
		btnSendFile = new JButton("\u53D1\u9001\u6587\u4EF6");
		btnSendFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser("D:");
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int number = listOnlineUsers.getSelectedIndex(); // ��ȡ���ѡ����û������û��б��л�ȡ
				if (number == -1) { // δѡ���û�
					JOptionPane.showMessageDialog(null, "��ѡ��һ���û������ļ�");
				} else {
					// ��ʾ�û�ѡ��򿪰�ť
					if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						fileSelectPath = fileChooser.getSelectedFile().getPath();// ��ȡ�ļ�·��
						fileName = fileChooser.getSelectedFile().getName();// ��ȡ�ļ���
						fileLength = new File(fileSelectPath).length();// ��ȡ�ļ�����
					}
					String drcUser = onlineUserDlm.getElementAt(number);
					FileMessage fileMessage = new FileMessage(localUserName, drcUser, fileName, fileLength); // ����������������������ͻ��˷��͵���Ϣ

					if (fileName != null) {
						// �����������һ������˵��������Ҫ��˭���ļ�
						try {
							synchronized (oos) {
								oos.writeObject(fileMessage);// �������ļ�����Ϣ���͸�������
								oos.flush();
							}
						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						// ������Ͱ�ť֮��ȴ�����ť������
						btnSendFile.setEnabled(false);
					}
				}

			}
		});
		panelSouth.add(btnSendFile);

		// �������ļ���ť��Ϊ������״̬
		btnSendFile.setEnabled(false);
		// ��������Ϣ��ť��Ϊ������״̬
		btnSendMsg.setEnabled(false);
	}

	// ����Ϣ��¼�ı��������һ����Ϣ��¼
	private void addMsgRecord(final String msgRecord, Color msgColor, int fontSize, boolean isItalic,
			boolean isUnderline) {
		final SimpleAttributeSet attrset = new SimpleAttributeSet();
		StyleConstants.setForeground(attrset, msgColor);
		StyleConstants.setFontSize(attrset, fontSize);
		StyleConstants.setUnderline(attrset, isUnderline);
		StyleConstants.setItalic(attrset, isItalic);
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				Document docs = textPaneMsgRecord.getDocument();
				try {
					docs.insertString(docs.getLength(), msgRecord, attrset);
				} catch (BadLocationException e) {
					e.printStackTrace();
				}
			}
		});
	}

	//����P2P
	class p2pHandler implements Runnable{

		@Override
		public void run() {
		while(true) {
			byte[] data =new byte[1024];
			DatagramPacket packet=new DatagramPacket(data, data.length);
			try {
			       p2pSocket.receive(packet);
				} catch (IOException e) {
					e.printStackTrace();
				}

			String info =new String(data, 0, packet.getLength());
			if( info.length() > 0) {
					final String msgRecord = dateFormat.format(new Date()) + info + "\r\n";
					addMsgRecord(msgRecord, Color.black, 12, false, false);
				}
			}
		}
	}

	// ��̨�����߳�
	class ListeningHandler implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					Message msg = null;
					msg = (Message) ois.readObject();
					if (msg instanceof UserLogonMessage) {// �ؼ��� ����+instanceof�� �ж���Ϣ���� true��false
						// �����û�������Ϣ
						processUserLogonMessage((UserLogonMessage) msg);
					} else if (msg instanceof UserLogoffMessage) {
						// �����û��˳���Ϣ
						processUserLogoffMessage((UserLogoffMessage) msg);
					} else if (msg instanceof PublicChatMessage) {
						// ��������Ϣ
						processPublicChatMessage((PublicChatMessage) msg);
					} else if (msg instanceof ResultMessage) {
						// �����������Ӧ�����Ϣ
						processResultMessage((ResultMessage) msg);
					} else if (msg instanceof OnlineUsersMessage) {
						// ���������û����б���Ϣ
						processOnlineUsersMessage((OnlineUsersMessage) msg);
					} else if (msg instanceof PrivateChatMessage) {
						// ����˽����Ϣ
						processPrivateChatMessage((PrivateChatMessage) msg);
					} else if (msg instanceof FileMessage) {
						// �����ļ�������Ϣ
						processFileMessage((FileMessage) msg);
					} else if (msg instanceof SendFileMessage) {
						// �����ļ�������Ϣ
						processFileSend((SendFileMessage) msg);
					} else if (msg instanceof P2PChatMessage) {
						//����p2p��Ϣ
						processP2PChatMessage((P2PChatMessage) msg);
					} else if (msg instanceof PortInfo) {
						//�˿���Ϣ����
						processPortInfo((PortInfo) msg);
					}
				}
			} catch (IOException e) {
				if (e.toString().endsWith("Connection reset")) {
					System.out.println("���������˳�");
				} else {
					e.printStackTrace();
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				if (socket != null) {
					try {
						socket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		private void processPortInfo(PortInfo msg) {
			try {
				p2pSocket = new DatagramSocket(msg.getPort());
				System.out.println(msg.getPort());
				new Thread(new p2pHandler()).start();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		private void processP2PChatMessage(P2PChatMessage msg) throws Exception{
			int p2pport = msg.getPort();
			String ip = msg.getAddress();
			InetAddress address = null;
			try {
				address = InetAddress.getByName(ip);
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			String info = " "+ localUserName + "˵(P2P)��" + textFieldMsgToSend.getText();
			byte[] data=info.getBytes();
			DatagramPacket packet = new DatagramPacket(data, data.length, address, p2pport);
			DatagramSocket Socket = null;
			try {
				Socket = new DatagramSocket();
			} catch (SocketException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			try {
				synchronized (Socket) {
					Socket.send(packet);
					//System.out.println("����"+packet.getPort());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Socket.close();
		}

		// �����ļ�������Ϣ
		private void processFileMessage(FileMessage msg) {
			// ��ȡҪ���͵��ļ�����Ϣ���շ���
			String fileName = msg.getFilename();
			long fileLength = msg.getFilelength();
			String srcUser = msg.getSrcUser();
			String dstUser = msg.getDstUser();
			int filePort;
			// ���յ���Ϣ��ʱ�䡢������Ϣ���û�������Ϣ������ӵ�����Ϣ��¼���ı�����
			final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "�������ļ�:" + fileName + "\r\n"
					+ "  �ļ���С:" + fileLength + "bit" + "\r\n";
			addMsgRecord(msgRecord, Color.black, 12, false, false);
			int m = JOptionPane.showConfirmDialog(null, "�Ƿ����" + fileName + "�ļ�", "��ʾ", JOptionPane.YES_NO_OPTION);
			// ����Է�ͬ������ļ�
			if (m == JOptionPane.YES_OPTION) {
				System.out.println("���շ�ͬ������ļ�");
				// ��ָ��λ�ô���һ���ļ������շ��ͷ�������
				JFileChooser chooser = new JFileChooser("D:");
				// �����ǽ����ļ���ֻ����ѡ��Ŀ¼�������Ŀ¼�н����ļ�
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				try {
					System.out.println("��ʼ����");
					if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						// ��������ý����ļ�·��
						String selectPath = chooser.getSelectedFile().getPath();
						File file = new File(selectPath + "/" + fileName);
						file.createNewFile();
						fileSavePath = file.getPath();
						FileOutputStream fos = new FileOutputStream(file.getPath());
						serverSocket = createSSLServerSocket();// ���� serverSocket���Է����ļ�
						filePort = serverSocket.getLocalPort();
						// ���Է�����ͬ������ļ��ġ��ļ�������Ӧ��Ϣ����������ServerSocket��һ�������˿ڣ���ͨ�����ļ�������Ӧ��Ϣ����ServerSocket�ĵ�ַ�Ͷ˿ڷ���Alice���ȴ�Alice���������ӡ�
						SendFileMessage fileDecisionMessage = new SendFileMessage(dstUser, srcUser, true, filePort);
						synchronized (oos) {
							oos.writeObject(fileDecisionMessage);
							oos.flush();
						}
						// �ȴ����ͷ������ļ�
						SSLSocket sslSocket = (SSLSocket) serverSocket.accept();
						// �����߳����������ļ�����
						RecieveFileHandler receiveHandler = new RecieveFileHandler(sslSocket, fos, fileLength);
						new Thread(receiveHandler).start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("�ܾ������ļ�");
				SendFileMessage fileDecisionMessage = new SendFileMessage(dstUser, srcUser, false, 0);
				try {
					synchronized (oos) {
						oos.writeObject(fileDecisionMessage);
						oos.flush();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

		// �����ļ�������Ϣ
		private void processFileSend(SendFileMessage msg) {
			if (((SendFileMessage) msg).isAccept()) {
				try {
					btnSendFile.setEnabled(true);
					String DrcUser = msg.getSrcUser();// ��ȡ���ͷ��û�
					FileInputStream fis = new FileInputStream(fileSelectPath);// ��ȡ�ļ�·��
					int port = ((SendFileMessage) msg).getPort();// ��ȡ���Ӷ˿�
					System.out.println("filePort:" + port);
					SendFile frame = new SendFile(DrcUser, fileSelectPath, port, fis, fileLength);// ���͵�����
					frame.show();
					fileName = null;
					fileSelectPath = null;
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {// �ܾ������ļ�
				JOptionPane.showMessageDialog(null, "�Է��ܾ����ļ�����");
				btnSendFile.setEnabled(true);
				fileName = null;
				fileSelectPath = null;
			}
		}

		// ��������ļ�
		class RecieveFileHandler implements Runnable {

			private InputStream inputStream;
			private long fileSize;
			private FileOutputStream fos;

			public RecieveFileHandler(SSLSocket sslSocket, FileOutputStream fos, long fileSize) {
				try {
					this.inputStream = sslSocket.getInputStream();
				} catch (IOException e) {
					e.printStackTrace();
				}
				this.fos = fos;
				this.fileSize = fileSize;
			}

			// �����ļ�
			@Override
			public void run() {
				System.out.println("�ļ��ܳ�:" + fileSize);
				int sum = 0;
				int n = 0;
				byte[] buffer = new byte[1024];
				try {
					while ((n = inputStream.read(buffer)) != -1) {// �ж��ļ��Ƿ��Ѷ�����ͷ
						sum = sum + n;// �����ļ��Ľ���
						fos.write(buffer, 0, n);
						fos.flush();
					}
					JOptionPane.showMessageDialog(null, "�ļ��������");

				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					try {
						inputStream.close();
						fos.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}

		}

		// ����˽����Ϣ
		private void processPrivateChatMessage(PrivateChatMessage msg) {
			// TODO Auto-generated method stub
			String srcUser = msg.getSrcUser();
			String msgContent = msg.getMsgContent();
			if (onlineUserDlm.contains(srcUser)) {
				// �ú�ɫ���ֽ��յ���Ϣ��ʱ�䡢������Ϣ���û�������Ϣ������ӵ�����Ϣ��¼���ı�����
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "˵: " + msgContent + "\r\n";
				addMsgRecord(msgRecord, Color.black, 12, false, false);
			}
		}

		// ���������û��б���Ϣ��Ϊ�����ߵ��û����������û��б�
		private void processOnlineUsersMessage(OnlineUsersMessage msg) {
			for (String user : msg.getOnlineUsers()) {
				onlineUserDlm.addElement(user);
			}
		}

		// �����û�������Ϣ
		private void processUserLogonMessage(UserLogonMessage msg) {
			String srcUser = msg.getSrcUser();
			if (!onlineUserDlm.contains(srcUser)) {
				// ����ɫ���ֽ��û������û�����ʱ����ӵ�����Ϣ��¼���ı�����
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "������!\r\n";
				addMsgRecord(msgRecord, Color.green, 12, false, false);
				// �ڡ������û����б������������ߵ��û���
				onlineUserDlm.addElement(srcUser);
			}
		}

		// �����û�������Ϣ
		private void processUserLogoffMessage(UserLogoffMessage msg) {
			String srcUser = msg.getSrcUser();
			if (onlineUserDlm.contains(srcUser)) {
				// ����ɫ���ֽ��û������û��˳�ʱ����ӵ�����Ϣ��¼���ı�����
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "������!\r\n";
				addMsgRecord(msgRecord, Color.green, 12, false, false);
				// �ڡ������û����б���ɾ���˳����û���
				onlineUserDlm.removeElement(srcUser);
			}
		}

		// �����������Ӧ�����Ϣ
		private void processResultMessage(ResultMessage msg) {
			if (msg.getResult() == ResultMessage.LOGOFF_SUCCESS) {
				// �ڡ���Ϣ��¼���ı������ú�ɫ��ӡ�XXʱ���˳��ɹ�������Ϣ
				String msgRecord = dateFormat.format(new Date()) + " �˳��ɹ�\r\n";
				addMsgRecord(msgRecord, Color.red, 12, false, false);
				System.exit(0);
			} else if (msg.getResult() == ResultMessage.LOGON_SUCCESS) {
				String msgRecord = dateFormat.format(new Date()) + " ��¼�ɹ�\r\n";
				addMsgRecord(msgRecord, Color.red, 12, false, false);
				System.exit(0);
			}

			if (msg.getResult() == ResultMessage.REGISTER_SUCCESS) {
				// �ڡ���Ϣ��¼���ı������ú�ɫ��ӡ�XXʱ��ע��ɹ�������Ϣ
				String msgRecord = dateFormat.format(new Date()) + " ע��ɹ�\r\n";
				addMsgRecord(msgRecord, Color.red, 12, false, false);
				System.exit(0);
			} else if (msg.getResult() == ResultMessage.REGISTER_FAILURE) {
				String msgRecord = dateFormat.format(new Date()) + " ע��ʧ�� \r\n";
				addMsgRecord(msgRecord, Color.red, 12, false, false);
				System.exit(0);
			}


		}

		// ���������ת�����Ĺ�����Ϣ
		private void processPublicChatMessage(PublicChatMessage msg) {
			String srcUser = msg.getSrcUser();
			String msgContent = msg.getMsgContent();
			if (onlineUserDlm.contains(srcUser)) {
				// �ú�ɫ���ֽ��յ���Ϣ��ʱ�䡢������Ϣ���û�������Ϣ������ӵ�����Ϣ��¼���ı�����
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "�Դ��˵: " + msgContent + "\r\n";
				addMsgRecord(msgRecord, Color.black, 12, false, false);
			}
		}
	}

	// �ͻ��˽�����������
	public SSLSocket createSSLSocket() throws Exception {
		String passphrase = "123456";
		char[] password = passphrase.toCharArray();
		String trustStoreFile = "mykeys.keystore";
		KeyStore ts = KeyStore.getInstance("PKCS12");
		ts.load(new FileInputStream(trustStoreFile), password);
		TrustManagerFactory tmf = TrustManagerFactory.getInstance("SunX509");
		tmf.init(ts);//
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(null, tmf.getTrustManagers(), null);
		SSLSocketFactory factory = sslContext.getSocketFactory();
		socket = (SSLSocket) factory.createSocket("localhost", port);
		return (SSLSocket) socket;
	}

	// ������������������
	public SSLServerSocket createSSLServerSocket() throws Exception {
		String keyStoreFile = "mykeys.keystore";
		String passphrase = "123456";
		KeyStore ks = KeyStore.getInstance("PKCS12");
		char[] password = passphrase.toCharArray();
		ks.load(new FileInputStream(keyStoreFile), password);
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, password);// ������Ҫ����
		SSLContext sslContext = SSLContext.getInstance("SSL");// ʹ��SSLЭ��
		sslContext.init(kmf.getKeyManagers(), null, null);// ��ͻ��˷��Ϳɿ�֤��
		SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
		serverSocket = (SSLServerSocket) factory.createServerSocket(0);
		return (SSLServerSocket) serverSocket;
	}
}
