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

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;

public class Server extends JFrame {

	private SSLServerSocket serverSocket;
	private Hashtable<String,String> user_ip = new Hashtable<String,String>();
	private  Hashtable<String,Integer> user_port = new Hashtable<String,Integer>();
	private final static int PORT = 9999;
	private UserDatabase userDatabase = new UserDatabase();
	// 保存在线用户的用户名与Socket信息
	private final UserManager userManager = new UserManager();
	// “在线用户列表ListModel”,用于维护“在线用户列表”中显示的内容
	final DefaultTableModel onlineUsersDtm = new DefaultTableModel();
	// 用于控制时间信息显示格式
	// private final SimpleDateFormat dateFormat = new
	// SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm:ss");
	private final JPanel contentPane;
	private final JTable tableOnlineUsers;
	private final JTextPane textPaneMsgRecord;

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				try {
					Server frame = new Server();
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
	public Server() {
		userDatabase = new UserDatabase();

		setTitle("\u670D\u52A1\u5668");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 561, 403);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		contentPane.setLayout(new BorderLayout(0, 0));
		setContentPane(contentPane);

		JSplitPane splitPaneNorth = new JSplitPane();
		splitPaneNorth.setResizeWeight(0.5);
		contentPane.add(splitPaneNorth, BorderLayout.CENTER);

		JScrollPane scrollPaneMsgRecord = new JScrollPane();
		scrollPaneMsgRecord.setPreferredSize(new Dimension(100, 300));
		scrollPaneMsgRecord.setViewportBorder(
				new TitledBorder(null, "\u6D88\u606F\u8BB0\u5F55", TitledBorder.LEADING, TitledBorder.TOP, null, null));
		splitPaneNorth.setLeftComponent(scrollPaneMsgRecord);

		textPaneMsgRecord = new JTextPane();
		textPaneMsgRecord.setEditable(false);
		textPaneMsgRecord.setPreferredSize(new Dimension(100, 100));
		scrollPaneMsgRecord.setViewportView(textPaneMsgRecord);

		JScrollPane scrollPaneOnlineUsers = new JScrollPane();
		scrollPaneOnlineUsers.setPreferredSize(new Dimension(100, 300));
		splitPaneNorth.setRightComponent(scrollPaneOnlineUsers);

		onlineUsersDtm.addColumn("用户名");
		onlineUsersDtm.addColumn("IP");
		onlineUsersDtm.addColumn("端口");
		onlineUsersDtm.addColumn("登录时间");
		tableOnlineUsers = new JTable(onlineUsersDtm);
		tableOnlineUsers.setEnabled(false);
		tableOnlineUsers.setPreferredSize(new Dimension(100, 270));
		tableOnlineUsers.setFillsViewportHeight(true); // 让JTable充满它的容器
		scrollPaneOnlineUsers.setViewportView(tableOnlineUsers);

		JPanel panelSouth = new JPanel();
		contentPane.add(panelSouth, BorderLayout.SOUTH);
		panelSouth.setLayout(new FlowLayout(FlowLayout.CENTER, 5, 5));

		final JButton btnStart = new JButton("\u542F\u52A8");
		// "启动"按钮
		btnStart.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				try {
					// 创建ServerSocket打开端口9999监听客户端连接
					//serverSocket = new ServerSocket(PORT);
					serverSocket = createSSLServerSocket();
					// 在“消息记录”文本框中用红色显示“服务器启动成功X”和启动时间信息
					String msgRecord = dateFormat.format(new Date()) + " 服务器启动成功" + "\r\n";
					addMsgRecord(msgRecord, Color.red, 12, false, false);
					// 创建并启动“接受用户连接线程”，接受并处理客户端连接请求
					new Thread() {
						@Override
						public void run() {
							while (true) {
								try {
									// 调用serverSocket.accept()方法接受用户连接请求
									Socket socket = serverSocket.accept();
									// 为新来的用户创建并启动“用户服务线程”
									// 并把serverSocket.accept()方法返回的socket对象交给“用户服务线程”来处理
									UserHandler userHandler = new UserHandler(socket);
									new Thread(userHandler).start();
								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						};
					}.start();

					btnStart.setEnabled(false);
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (Exception e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
		});
		panelSouth.add(btnStart);
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

	class UserHandler implements Runnable {
		private final Socket currentUserSocket;
		private ObjectInputStream ois;
		private ObjectOutputStream oos;

		public UserHandler(Socket currentUserSocket) {
			this.currentUserSocket = currentUserSocket;
			try {
				ois = new ObjectInputStream(currentUserSocket.getInputStream());
				oos = new ObjectOutputStream(currentUserSocket.getOutputStream());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void run() {
			try {
				while (true) {
					Message msg = (Message) ois.readObject();
					if (msg instanceof UserLogonMessage) {
						// 处理用户上线消息
						processUserLogonMessage((UserLogonMessage) msg);
					} else if (msg instanceof RegisterMessage) {
						// 处理用户注册信息
						processRegisterMessage((RegisterMessage) msg);
					} else if (msg instanceof UserLogoffMessage) {
						// 处理用户下线消息
						processUserLogoffMessage((UserLogoffMessage) msg);
					} else if (msg instanceof PublicChatMessage) {
						// 处理公聊消息
						processPublicChatMessage((PublicChatMessage) msg);
					} else if (msg instanceof PrivateChatMessage) {
						// 处理私聊消息
						processPrivateChatMessage((PrivateChatMessage) msg);
					} else if (msg instanceof FileMessage) {
						// 处理文件发送请求消息
						processFileMessage((FileMessage) msg);
					} else if (msg instanceof SendFileMessage) {
						// 处理文件发送消息
						processSendFileMessage((SendFileMessage) msg);
					} else if (msg instanceof P2PChatMessage) {
						// 处理文件发送请求消息
						processP2PChatMessage((P2PChatMessage) msg);
					}
				}
			} catch (IOException e) {
				if (e.toString().endsWith("Connection reset")) {
					System.out.println("客户端可能已经掉线");
					userManager.removeUser(getName());
				} else {
					e.printStackTrace();
				}
			} catch (ClassNotFoundException e) {
				e.printStackTrace();
			} finally {
				if (currentUserSocket != null) {
					try {
						currentUserSocket.close();
					} catch (IOException e) {
						e.printStackTrace();
					}
				}
			}
		}

		// 注册
		private void processRegisterMessage(RegisterMessage msg) {
			// TODO Auto-generated method stub
			String srcUser = msg.getSrcUser();
			char[] tmppassWd = msg.getPasswd(); // 取口令
			String passwdString = String.valueOf(tmppassWd);
			String ageString = msg.getAge();

			try {
				if (userDatabase.insertUser(srcUser, passwdString, ageString)) {// 如果插入成功，就向用户返回注册成功信息
					userDatabase.showAllUsers();
					ResultMessage resultMessage = new ResultMessage(null, ResultMessage.REGISTER_SUCCESS, "注册成功");
					synchronized (oos) {
						oos.writeObject(resultMessage);
						oos.flush();
					}
					String ip = currentUserSocket.getInetAddress().getHostAddress();
					final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "(" + ip + ")"
							+ "注册成功!\r\n";
					addMsgRecord(msgRecord, Color.black, 12, false, false);
				} else {
					ResultMessage resultMessage = new ResultMessage(null, ResultMessage.REGISTER_FAILURE, "注册失败");
					synchronized (oos) {
						oos.writeObject(resultMessage);
						oos.flush();
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		// 处理私聊消息
		private void processPrivateChatMessage(PrivateChatMessage msg) {
			// TODO Auto-generated method stub
			String srcUser = msg.getSrcUser();
			String msgContent = msg.getMsgContent();
			String dstUser = msg.getDstUser();

			if (userManager.hasUser(srcUser) && userManager.hasUser(dstUser) && msgContent.length() > 0) {
				// 用黑色文字将收到消息的时间、发送消息的用户名和消息内容添加到“消息记录”文本框中
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "向: " + dstUser + "说"
						+ msgContent + "\r\n";
				addMsgRecord(msgRecord, Color.black, 12, false, false);
				// 将私聊消息由服务器转发给特定用户
				ObjectOutputStream dstOos = userManager.getUserOos(dstUser);
				try {
					synchronized (dstOos) {
						dstOos.writeObject(msg);
						dstOos.flush();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		// 处理P2P消息
		private void processP2PChatMessage(P2PChatMessage msg) {
			String dstUser = msg.getDstUser();
			String srcUser = msg.getSrcUser();
			String ip = user_ip.get(dstUser);
			int p2pport = user_port.get(dstUser);
			String msgContent = "";

			//发往发送方监听端口和ip地址;
			P2PChatMessage srcMsg = new P2PChatMessage(ip, p2pport, dstUser, srcUser);
			try {
				ObjectOutputStream o = userManager.getUserOos(srcUser);
				synchronized (o) {
					o.writeObject(srcMsg);
					o.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}

			String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "向" + dstUser + "发送P2P消息\r\n";
			addMsgRecord(msgRecord, Color.black, 12, false, false);
		}

		// 向其它用户转发消息
		private void transferMsgToOtherUsers(Message msg) {
			String[] users = userManager.getAllUsers();
			for (String user : users) {
				if (userManager.getUserSocket(user) != currentUserSocket) {
					try {
						ObjectOutputStream o = userManager.getUserOos(user);
						synchronized (o) {
							o.writeObject(msg);
							o.flush();
						}
					} catch (IOException e) {
						if (e.toString().endsWith("Connection reset")) {
							System.out.println("客户端" + user + "可能已经掉线");
							userManager.removeUser(getName());
						} else {
							e.printStackTrace();
						}
					}
				}
			}
		}

		// 处理用户上线消息
		private void processUserLogonMessage(UserLogonMessage msg) {
			String srcUser = msg.getSrcUser();

			try {
				if (userManager.hasUser(srcUser)) {
					// 这种情况意味着用户重复登录，向客户端发送登录失败消息
					System.err.println("用户重复登录");
					ResultMessage resultMessage = new ResultMessage(null, ResultMessage.LOGON_FAILURE, "用户重复登录");
					synchronized (oos) {
						oos.writeObject(resultMessage);
						oos.flush();
					}
					return;
				}

				// 检查用户名和口令是否正确，如果不正确，向客户端发送登录失败消息
				String password = msg.getPassword();
				boolean checkResult = userDatabase.checkUserPassword(srcUser, password);
				if (checkResult) {
					ResultMessage resultMessage = new ResultMessage(null, ResultMessage.LOGON_SUCCESS, "用户登录成功 ");
					synchronized (oos) {
						oos.writeObject(resultMessage);
						oos.flush();
					}
					// 向新登录的用户转发当前在线用户列表
					String[] users = userManager.getAllUsers();
					OnlineUsersMessage onlineUsersMessage = new OnlineUsersMessage(srcUser);
					for (String user : users) {
						onlineUsersMessage.addUser(user);
					}
					synchronized (oos) {
						oos.writeObject(onlineUsersMessage);
						oos.flush();
					}
					// 向所有其它在线用户转发用户登录消息
					transferMsgToOtherUsers(msg);
					PortInfo newmsg = new PortInfo(currentUserSocket.getPort(),"&&server&&",srcUser);
					try {
						synchronized (oos) {
							oos.writeObject(newmsg);
							oos.flush();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// 将用户信息加入到“在线用户”列表中
					onlineUsersDtm.addRow(new Object[] { srcUser, currentUserSocket.getInetAddress().getHostAddress(),
							currentUserSocket.getPort(), dateFormat.format(new Date()) });
					userManager.addUser(srcUser, currentUserSocket, oos, ois);
					// 用绿色文字将用户名和用户登录时间添加到“消息记录”文本框中
					String ip = currentUserSocket.getInetAddress().getHostAddress();
					user_ip.put(srcUser, ip);
					user_port.put(srcUser,currentUserSocket.getPort());
					final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "(" + ip + ")"
							+ "登录了!\r\n";
					addMsgRecord(msgRecord, Color.green, 12, false, false);
					return;
				} else {
					ResultMessage resultMessage = new ResultMessage(null, ResultMessage.LOGON_FAILURE, "用户登录失败 ");
					synchronized (oos) {
						oos.writeObject(resultMessage);
						oos.flush();
					}
					return;
				}

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// 处理用户退出消息
		private void processUserLogoffMessage(UserLogoffMessage msg) {
			String srcUser = msg.getSrcUser();
			// 用绿色文字将用户名和用户退出时间添加到“消息记录”文本框中
			String ip = userManager.getUserSocket(srcUser).getInetAddress().getHostAddress();
			final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "(" + ip + ")" + "退出了!\r\n";
			addMsgRecord(msgRecord, Color.green, 12, false, false);
			// 在“在线用户列表”中删除退出用户
			userManager.removeUser(srcUser);
			user_ip.remove(srcUser);
			user_port.remove(srcUser);
			for (int i = 0; i < onlineUsersDtm.getRowCount(); i++) {
				if (onlineUsersDtm.getValueAt(i, 0).equals(srcUser)) {
					onlineUsersDtm.removeRow(i);
					break;
				}
			}
			// 将用户退出消息转发给所有其它在线用户
			transferMsgToOtherUsers(msg);
			// 向客户端发送退出成功消息
			ResultMessage resultMessage = new ResultMessage(null, ResultMessage.LOGOFF_SUCCESS, "用户退出成功");
			try {
				synchronized (oos) {
					oos.writeObject(resultMessage);
					oos.flush();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		// 处理用户发来的公聊消息
		private void processPublicChatMessage(PublicChatMessage msg) {
			String srcUser = msg.getSrcUser();
			String msgContent = msg.getMsgContent();
			if (userManager.hasUser(srcUser) && msgContent.length() > 0) {
				// 用黑色文字将收到消息的时间、发送消息的用户名和消息内容添加到“消息记录”文本框中
				final String msgRecord = dateFormat.format(new Date()) + " " + srcUser + "向大家说: " + msgContent + "\r\n";
				addMsgRecord(msgRecord, Color.black, 12, false, false);
				// 将公聊消息转发给所有其它在线用户
				transferMsgToOtherUsers(msg);
			}
		}
	}

	public SSLServerSocket createSSLServerSocket() throws Exception {
		String keyStoreFile = "mykeys.keystore";
		String passphrase = "123456";
		KeyStore ks = KeyStore.getInstance("PKCS12");
		char[] password = passphrase.toCharArray();
		ks.load(new FileInputStream(keyStoreFile), password);
		KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509");
		kmf.init(ks, password);// 区别：需要口令
		SSLContext sslContext = SSLContext.getInstance("SSL");
		sslContext.init(kmf.getKeyManagers(), null, null);// 创建一个默认的TrustManager，发送数字证书证明自己的身份可靠

		SSLServerSocketFactory factory = sslContext.getServerSocketFactory();
		serverSocket = (SSLServerSocket) factory.createServerSocket(PORT);
		return (SSLServerSocket) serverSocket;
	}

	// 处理文件发送请求消息
	private void processFileMessage(FileMessage msg) {
		// 接收文件的用户
		String dstUser = msg.getDstUser();
		try {
			ObjectOutputStream objectOutputStream = userManager.getUserOos(dstUser);
			synchronized (objectOutputStream) {
				objectOutputStream.writeObject(msg);
				objectOutputStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void processSendFileMessage(SendFileMessage msg) {
		String dstUser = msg.getDstUser();
		try {
			ObjectOutputStream objectOutputStream = userManager.getUserOos(dstUser);
			synchronized (objectOutputStream) {
				objectOutputStream.writeObject(msg);
				objectOutputStream.flush();
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
}

// 管理在线用户信息
class UserManager {
	private final Hashtable<String, User> onLineUsers;// 用户名到user对象的映射·

	public UserManager() {
		onLineUsers = new Hashtable<String, User>();
	}

	// 判断某用户是否在线
	public boolean hasUser(String userName) {
		return onLineUsers.containsKey(userName);
	}

	// 判断在线用户列表是否空
	public boolean isEmpty() {
		return onLineUsers.isEmpty();
	}

	// 获取在线用户的Socket的的输出流封装成的对象输出流
	public ObjectOutputStream getUserOos(String userName) {
		if (hasUser(userName)) {
			return onLineUsers.get(userName).getOos();
		}
		return null;
	}

	// 获取在线用户的Socket的的输入流封装成的对象输入流
	public ObjectInputStream getUserOis(String userName) {
		if (hasUser(userName)) {
			return onLineUsers.get(userName).getOis();
		}
		return null;
	}

	// 获取在线用户的Socket
	public Socket getUserSocket(String userName) {
		if (hasUser(userName)) {
			return onLineUsers.get(userName).getSocket();
		}
		return null;
	}

	// 添加在线用户
	public boolean addUser(String userName, Socket userSocket, ObjectOutputStream oos, ObjectInputStream ios) {
		if ((userName != null) && (userSocket != null) && (oos != null) && (ios != null)) {
			onLineUsers.put(userName, new User(userSocket, oos, ios));
			return true;
		}
		return false;
	}

	// 删除在线用户
	public boolean removeUser(String userName) {
		if (hasUser(userName)) {
			onLineUsers.remove(userName);
			return true;
		}
		return false;
	}

	// 获取所有在线用户名
	public String[] getAllUsers() {
		String[] users = new String[onLineUsers.size()];
		int i = 0;
		for (String userName : onLineUsers.keySet()) {
			users[i++] = userName;
		}
		return users;
	}

	// 获取在线用户个数
	public int getOnlineUserCount() {
		return onLineUsers.size();
	}
}

class User {
	private final Socket socket;
	private ObjectOutputStream oos;
	private ObjectInputStream ois;
	private final Date logonTime;

	public User(Socket socket) {
		this.socket = socket;
		try {
			oos = new ObjectOutputStream(socket.getOutputStream());
			ois = new ObjectInputStream(socket.getInputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
		logonTime = new Date();
	}

	public User(Socket socket, ObjectOutputStream oos, ObjectInputStream ois) {
		this.socket = socket;
		this.oos = oos;
		this.ois = ois;
		logonTime = new Date();
	}

	public User(Socket socket, ObjectOutputStream oos, ObjectInputStream ois, Date logonTime) {
		this.socket = socket;
		this.oos = oos;
		this.ois = ois;
		this.logonTime = logonTime;
	}

	public Socket getSocket() {
		return socket;
	}

	public ObjectOutputStream getOos() {
		return oos;
	}

	public ObjectInputStream getOis() {
		return ois;
	}

	public Date getLogonTime() {
		return logonTime;
	}

}
