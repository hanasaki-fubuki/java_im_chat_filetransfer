/*****************************************************************************************************
 * 本代码版权归Mengyun199所有，All Rights Reserved (C) 2022-
 *****************************************************************************************************
 * 所属域：hanasaki-workstation
 * 登录用户：Mengyun Jia
 * 机器名称：hanasaki-workstation
 * 联系人邮箱：jiamengyun1024@outlook.com
 *****************************************************************************************************
 * 创建年份：2022
 * 创建人：Mengyun Jia
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
	private ObjectInputStream ois;// 客户端输入流ois，接收消息，封装成对象输入流
	private ObjectOutputStream oos;// 客户端通过oos向服务端发送消息，socket输出流封装成对象输出流
	private String localUserName = "";
	private String localPasswd;

	// “在线用户列表ListModel”,用于维护“在线用户列表”中显示的内容
	private final DefaultListModel<String> onlineUserDlm = new DefaultListModel<String>();
	// 用于控制时间信息显示格式
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
				// 向服务器发送用户退出消息
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
		btnLogon = new JButton("\u767B\u5F55"); // “登录”按钮
		btnLogon.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (btnLogon.getText().equals("登录")) {
					localUserName = textFieldUserName.getText().trim();// 取用户名，删除多余空格
					localPasswd = passwordFieldPwd.getText();// 获取用户口令
					if (localUserName.length() > 0) {
						// 与服务器端建立Socket连接，如果抛出异常，则弹出对话框通知用户，并退出
						try {
							// if (socket == null) { // socket不为空，可能是在“用户注册”时已经与服务器连接Socket连接了
							// socket = new Socket("localhost", port);// 客户端与服务器建立连接
							// 将socket的输入流和输出流分别封装成对象输入流和对象输出流
							socket = createSSLSocket();
							oos = new ObjectOutputStream(socket.getOutputStream());// socket输出流封装成对象输出流
							ois = new ObjectInputStream(socket.getInputStream());// socket输入流封装成对象输入流
							// }
						} catch (UnknownHostException ex) {
							JOptionPane.showMessageDialog(null, "找不到服务器主机");
							return;
						} catch (IOException ex) {
							JOptionPane.showMessageDialog(null, "服务器I/O错误，服务器未启动？");
							return;
						} catch (Exception e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						// 向服务器发送用户上线信息，将自己的用户名和口令发送给服务器
						// 口令固定为"nopassword"，需要改成从界面的JPassword组件中获取口令
						UserLogonMessage userLogonMessage = new UserLogonMessage(localUserName, localPasswd);

						try {
							oos.writeObject(userLogonMessage);
							oos.flush();
							// 在“消息记录”文本框中用红色添加“正在登录...”的信息
							addMsgRecord("正在登录...\r\n", Color.red, 12, false, false);
							// 接收服务器发来的响应结果消息
							ResultMessage msg = (ResultMessage) ois.readObject();
							if (msg.getResult() == ResultMessage.LOGON_SUCCESS) {

								// 在“消息记录”文本框中用红色添加“XX时间登录成功”的信息
								String msgRecord = dateFormat.format(new Date()) + " 登录成功\r\n";
								addMsgRecord(msgRecord, Color.red, 12, false, false);

								// 创建并启动“后台监听线程”,监听并处理服务器传来的信息
								new Thread(new ListeningHandler()).start();
								// 将“登录”按钮设为“退出”按钮
								btnLogon.setText("退出");
								// 将发送文件按钮设为可用状态
								btnSendFile.setEnabled(true);
								// 将发送消息按钮设为可用状态
								btnSendMsg.setEnabled(true);
							} else if (msg.getResult() == ResultMessage.LOGON_FAILURE) {
								// 在“消息记录”文本框中用红色添加“XX时间登录失败”的信息
								String msgRecord = dateFormat.format(new Date()) + " 登录失败：" + msg.getMsg() + "\r\n";
								addMsgRecord(msgRecord, Color.red, 12, false, false);
							}
						} catch (IOException ex) {
							ex.printStackTrace();
						} catch (ClassNotFoundException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
					}
				} else if (btnLogon.getText().equals("退出")) {
					if (JOptionPane.showConfirmDialog(null, "是否退出?", "退出确认",
							JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
						// 向服务器发送用户退出消息
						UserLogoffMessage userLogoffMessage = new UserLogoffMessage(localUserName);
						try {
							oos.writeObject(userLogoffMessage);
							oos.flush();
							// 在“消息记录”文本框中用红色添加“正在退出...”的信息
							addMsgRecord("正在退出...\r\n", Color.red, 12, false, false);
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

		// 注册按钮
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

		// “发送消息”按钮
		btnSendMsg = new JButton("biu ~~~");
		btnSendMsg.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				String msgContent = textFieldMsgToSend.getText();
				if (msgContent.length() > 0) {
					//正则判断是否为p2p私聊消息
					String pattern = "<p2p>.*";
					boolean isMatch = Pattern.matches(pattern, msgContent);
					if (chckbxPrivateChat.isSelected() && isMatch == false) {// 私聊
						// 将消息文本框中的内容作为私聊消息发送给服务器
						String dstUser = listOnlineUsers.getSelectedValue();
						if (dstUser != null) {
							PrivateChatMessage privateChatMessage = new PrivateChatMessage(localUserName, msgContent,
									dstUser);
							try {
								oos.writeObject(privateChatMessage);
								oos.flush();
							} catch (IOException ex) {
								if (ex.getMessage().contains("Socket closed")) {
									JOptionPane.showMessageDialog(Client.this, "服务器已经关闭");
									return;
								} else {
									ex.printStackTrace();
								}
							}
							// 在“消息记录”文本框中用蓝色显示发送的消息及发送时间
							String msgRecord = dateFormat.format(new Date()) + "向" + dstUser + "说:" + msgContent + "\r\n";
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
						String  msgRecord = dateFormat.format(new Date()) + " 向"+ dstUser +"说(P2P):" + msgContent + "\r\n";
						addMsgRecord(msgRecord, Color.blue, 12, false, false);
					} else {
						// 将消息文本框中的内容作为公聊消息发送给服务器
						PublicChatMessage publicChatMessage = new PublicChatMessage(localUserName, msgContent);
						try {
							oos.writeObject(publicChatMessage);
							oos.flush();
						} catch (IOException ex) {
							if (ex.getMessage().contains("Socket closed")) {
								JOptionPane.showMessageDialog(Client.this, "服务器已经关闭");
								return;
							} else {
								ex.printStackTrace();
							}
						}
						// 在“消息记录”文本框中用蓝色显示发送的消息及发送时间
						String msgRecord = dateFormat.format(new Date()) + "向大家说:" + msgContent + "\r\n";
						addMsgRecord(msgRecord, Color.blue, 12, false, false);

					}
				}
			}
		});
		panelSouth.add(btnSendMsg);

		Component horizontalStrut_3 = Box.createHorizontalStrut(20);
		panelSouth.add(horizontalStrut_3);

		// 发送文件按钮
		btnSendFile = new JButton("\u53D1\u9001\u6587\u4EF6");
		btnSendFile.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				JFileChooser fileChooser = new JFileChooser("D:");
				fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
				int number = listOnlineUsers.getSelectedIndex(); // 获取鼠标选择的用户，从用户列表中获取
				if (number == -1) { // 未选择用户
					JOptionPane.showMessageDialog(null, "请选择一个用户发送文件");
				} else {
					// 表示用户选择打开按钮
					if (fileChooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						fileSelectPath = fileChooser.getSelectedFile().getPath();// 获取文件路径
						fileName = fileChooser.getSelectedFile().getName();// 获取文件名
						fileLength = new File(fileSelectPath).length();// 获取文件长度
					}
					String drcUser = onlineUserDlm.getElementAt(number);
					FileMessage fileMessage = new FileMessage(localUserName, drcUser, fileName, fileLength); // 给服务器发送请求给其他客户端发送的信息

					if (fileName != null) {
						// 向服务器发送一个对象，说明它即将要给谁发文件
						try {
							synchronized (oos) {
								oos.writeObject(fileMessage);// 将发送文件的信息发送给服务器
								oos.flush();
							}
						} catch (IOException e1) {
							e1.printStackTrace();
						} catch (Exception e1) {
							e1.printStackTrace();
						}
						// 点击发送按钮之后等待，按钮不可用
						btnSendFile.setEnabled(false);
					}
				}

			}
		});
		panelSouth.add(btnSendFile);

		// 将发送文件按钮设为不可用状态
		btnSendFile.setEnabled(false);
		// 将发送消息按钮设为不可用状态
		btnSendMsg.setEnabled(false);
	}

	// 向消息记录文本框中添加一条消息记录
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

	//监听P2P
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

	// 后台监听线程
	class ListeningHandler implements Runnable {
		@Override
		public void run() {
			try {
				while (true) {
					Message msg = null;
					msg = (Message) ois.readObject();
					if (msg instanceof UserLogonMessage) {// 关键字 对象+instanceof， 判断消息类型 true或false
						// 处理用户上线消息
						processUserLogonMessage((UserLogonMessage) msg);
					} else if (msg instanceof UserLogoffMessage) {
						// 处理用户退出消息
						processUserLogoffMessage((UserLogoffMessage) msg);
					} else if (msg instanceof PublicChatMessage) {
						// 处理公聊消息
						processPublicChatMessage((PublicChatMessage) msg);
					} else if (msg instanceof ResultMessage) {
						// 处理服务器响应结果消息
						processResultMessage((ResultMessage) msg);
					} else if (msg instanceof OnlineUsersMessage) {
						// 处理在线用户名列表消息
						processOnlineUsersMessage((OnlineUsersMessage) msg);
					} else if (msg instanceof PrivateChatMessage) {
						// 处理私聊消息
						processPrivateChatMessage((PrivateChatMessage) msg);
					} else if (msg instanceof FileMessage) {
						// 处理文件请求消息
						processFileMessage((FileMessage) msg);
					} else if (msg instanceof SendFileMessage) {
						// 处理文件发送消息
						processFileSend((SendFileMessage) msg);
					} else if (msg instanceof P2PChatMessage) {
						//处理p2p消息
						processP2PChatMessage((P2PChatMessage) msg);
					} else if (msg instanceof PortInfo) {
						//端口信息处理
						processPortInfo((PortInfo) msg);
					}
				}
			} catch (IOException e) {
				if (e.toString().endsWith("Connection reset")) {
					System.out.println("服务器端退出");
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
			String info = " "+ localUserName + "说(P2P)：" + textFieldMsgToSend.getText();
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
					//System.out.println("发送"+packet.getPort());
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			Socket.close();
		}

		// 处理文件请求消息
		private void processFileMessage(FileMessage msg) {
			// 获取要发送的文件的信息及收发者
			String fileName = msg.getFilename();
			long fileLength = msg.getFilelength();
			String srcUser = msg.getSrcUser();
			String dstUser = msg.getDstUser();
			int filePort;
			// 将收到消息的时间、发送消息的用户名和消息内容添加到“消息记录”文本框中
			final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "请求发送文件:" + fileName + "\r\n"
					+ "  文件大小:" + fileLength + "bit" + "\r\n";
			addMsgRecord(msgRecord, Color.black, 12, false, false);
			int m = JOptionPane.showConfirmDialog(null, "是否接收" + fileName + "文件", "提示", JOptionPane.YES_NO_OPTION);
			// 如果对方同意接收文件
			if (m == JOptionPane.YES_OPTION) {
				System.out.println("接收方同意接收文件");
				// 在指定位置创建一个文件来接收发送方的数据
				JFileChooser chooser = new JFileChooser("D:");
				// 由于是接收文件，只可以选择目录，在这个目录中接收文件
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				try {
					System.out.println("开始接收");
					if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
						// 创建并获得接收文件路径
						String selectPath = chooser.getSelectedFile().getPath();
						File file = new File(selectPath + "/" + fileName);
						file.createNewFile();
						fileSavePath = file.getPath();
						FileOutputStream fos = new FileOutputStream(file.getPath());
						serverSocket = createSSLServerSocket();// 创建 serverSocket给对方发文件
						filePort = serverSocket.getLocalPort();
						// 给对方发送同意接收文件的“文件发送响应消息”，并在用ServerSocket打开一个本机端口，并通过“文件发送响应消息”把ServerSocket的地址和端口发给Alice，等待Alice来建立连接。
						SendFileMessage fileDecisionMessage = new SendFileMessage(dstUser, srcUser, true, filePort);
						synchronized (oos) {
							oos.writeObject(fileDecisionMessage);
							oos.flush();
						}
						// 等待发送方发送文件
						SSLSocket sslSocket = (SSLSocket) serverSocket.accept();
						// 开个线程来做接受文件任务
						RecieveFileHandler receiveHandler = new RecieveFileHandler(sslSocket, fos, fileLength);
						new Thread(receiveHandler).start();
					}
				} catch (IOException e) {
					e.printStackTrace();
				} catch (Exception e) {
					e.printStackTrace();
				}
			} else {
				System.out.println("拒绝接收文件");
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

		// 处理文件发送消息
		private void processFileSend(SendFileMessage msg) {
			if (((SendFileMessage) msg).isAccept()) {
				try {
					btnSendFile.setEnabled(true);
					String DrcUser = msg.getSrcUser();// 获取发送方用户
					FileInputStream fis = new FileInputStream(fileSelectPath);// 获取文件路径
					int port = ((SendFileMessage) msg).getPort();// 获取连接端口
					System.out.println("filePort:" + port);
					SendFile frame = new SendFile(DrcUser, fileSelectPath, port, fis, fileLength);// 发送的内容
					frame.show();
					fileName = null;
					fileSelectPath = null;
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {// 拒绝传输文件
				JOptionPane.showMessageDialog(null, "对方拒绝了文件传输");
				btnSendFile.setEnabled(true);
				fileName = null;
				fileSelectPath = null;
			}
		}

		// 处理接收文件
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

			// 接收文件
			@Override
			public void run() {
				System.out.println("文件总长:" + fileSize);
				int sum = 0;
				int n = 0;
				byte[] buffer = new byte[1024];
				try {
					while ((n = inputStream.read(buffer)) != -1) {// 判断文件是否已读到尽头
						sum = sum + n;// 接收文件的进度
						fos.write(buffer, 0, n);
						fos.flush();
					}
					JOptionPane.showMessageDialog(null, "文件传输结束");

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

		// 处理私聊消息
		private void processPrivateChatMessage(PrivateChatMessage msg) {
			// TODO Auto-generated method stub
			String srcUser = msg.getSrcUser();
			String msgContent = msg.getMsgContent();
			if (onlineUserDlm.contains(srcUser)) {
				// 用黑色文字将收到消息的时间、发送消息的用户名和消息内容添加到“消息记录”文本框中
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "说: " + msgContent + "\r\n";
				addMsgRecord(msgRecord, Color.black, 12, false, false);
			}
		}

		// 处理在线用户列表消息（为新上线的用户生成在线用户列表）
		private void processOnlineUsersMessage(OnlineUsersMessage msg) {
			for (String user : msg.getOnlineUsers()) {
				onlineUserDlm.addElement(user);
			}
		}

		// 处理用户上线消息
		private void processUserLogonMessage(UserLogonMessage msg) {
			String srcUser = msg.getSrcUser();
			if (!onlineUserDlm.contains(srcUser)) {
				// 用绿色文字将用户名和用户上线时间添加到“消息记录”文本框中
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "上线了!\r\n";
				addMsgRecord(msgRecord, Color.green, 12, false, false);
				// 在“在线用户”列表中增加新上线的用户名
				onlineUserDlm.addElement(srcUser);
			}
		}

		// 处理用户下线消息
		private void processUserLogoffMessage(UserLogoffMessage msg) {
			String srcUser = msg.getSrcUser();
			if (onlineUserDlm.contains(srcUser)) {
				// 用绿色文字将用户名和用户退出时间添加到“消息记录”文本框中
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "下线了!\r\n";
				addMsgRecord(msgRecord, Color.green, 12, false, false);
				// 在“在线用户”列表中删除退出的用户名
				onlineUserDlm.removeElement(srcUser);
			}
		}

		// 处理服务器响应结果消息
		private void processResultMessage(ResultMessage msg) {
			if (msg.getResult() == ResultMessage.LOGOFF_SUCCESS) {
				// 在“消息记录”文本框中用红色添加“XX时间退出成功”的信息
				String msgRecord = dateFormat.format(new Date()) + " 退出成功\r\n";
				addMsgRecord(msgRecord, Color.red, 12, false, false);
				System.exit(0);
			} else if (msg.getResult() == ResultMessage.LOGON_SUCCESS) {
				String msgRecord = dateFormat.format(new Date()) + " 登录成功\r\n";
				addMsgRecord(msgRecord, Color.red, 12, false, false);
				System.exit(0);
			}

			if (msg.getResult() == ResultMessage.REGISTER_SUCCESS) {
				// 在“消息记录”文本框中用红色添加“XX时间注册成功”的信息
				String msgRecord = dateFormat.format(new Date()) + " 注册成功\r\n";
				addMsgRecord(msgRecord, Color.red, 12, false, false);
				System.exit(0);
			} else if (msg.getResult() == ResultMessage.REGISTER_FAILURE) {
				String msgRecord = dateFormat.format(new Date()) + " 注册失败 \r\n";
				addMsgRecord(msgRecord, Color.red, 12, false, false);
				System.exit(0);
			}


		}

		// 处理服务器转发来的公聊消息
		private void processPublicChatMessage(PublicChatMessage msg) {
			String srcUser = msg.getSrcUser();
			String msgContent = msg.getMsgContent();
			if (onlineUserDlm.contains(srcUser)) {
				// 用黑色文字将收到消息的时间、发送消息的用户名和消息内容添加到“消息记录”文本框中
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "对大家说: " + msgContent + "\r\n";
				addMsgRecord(msgRecord, Color.black, 12, false, false);
			}
		}
	}

	// 客户端建立信任连接
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

	// 服务器建立信任连接
	public SSLServerSocket createSSLServerSocket() throws Exception {
		String keyStoreFile = "mykeys.keystore";
		String passphrase = "123456";
		KeyStore ks = KeyStore.getInstance("PKCS12");
		char[] password = passphrase.toCharArray();
		ks.load(new FileInputStream(keyStoreFile), password);
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, password);// 区别：需要口令
		SSLContext sslContext = SSLContext.getInstance("SSL");// 使用SSL协议
		sslContext.init(kmf.getKeyManagers(), null, null);// 向客户端发送可靠证书
		SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
		serverSocket = (SSLServerSocket) factory.createServerSocket(0);
		return (SSLServerSocket) serverSocket;
	}
}
