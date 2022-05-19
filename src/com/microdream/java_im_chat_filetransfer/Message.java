package com.microdream.java_im_chat_filetransfer;

import java.io.Serializable;
import java.util.ArrayList;



// 所有消息的基类
public class Message implements Serializable {
	/**
	 * 
	 */

	private String srcUser;
	public Message(String srcUser) {
		this.srcUser = srcUser;
	}

	public String getSrcUser() {
		return srcUser;
	}
	
	public void setSrcUser(String srcUser) {
		this.srcUser = srcUser;
	}
}

//注册信息
class RegisterMessage extends Message{
/**
	 * 
	 */

	//	private String passwdString;
	private char[] passwd ;
	private String userAgeString;
	
	public RegisterMessage(String srcUser, char[] passwd, String userAgeString) {
		super(srcUser);
		this.passwd = passwd;
		this.userAgeString = userAgeString;
	}
	
	public char[] getPasswd() {
		return passwd;
	}
	
	public String getAge() {
		return userAgeString;
	}
	
}


// 聊天消息（公聊、私聊、群聊等）的基类
abstract class ChatMessage extends Message{
	/**
	 * 
	 */
	
	private String msgContent;

	public ChatMessage(String srcUser, String msgContent) {
		super(srcUser);
		this.msgContent = msgContent;
	}

	public String getMsgContent() {
		return msgContent;
	}

	public void setMsgContent(String msgContent) {
		this.msgContent = msgContent;
	}
}

// 公聊消息
class PublicChatMessage extends ChatMessage {
	/**
	 * 
	 */

	public PublicChatMessage(String srcUser, String msgContent) {
		super(srcUser, msgContent);
	}
}

//  p2p私聊消息
class P2PChatMessage extends Message {
	private int port;
	private String address;
	private String dstUser;

	public P2PChatMessage(String address,int port,String srcUser, String dstUser) {
		super(srcUser);
		this.dstUser = dstUser;
		this.address = address;
		this.port = port;
		// TODO Auto-generated constructor stub
	}

	public String getAddress() {
		return this.address;
	}

	public int getPort() {
		return this.port;
	}

	public String getDstUser() {
		return dstUser;
	}
}
class PortInfo extends Message {
	private int port;
	private String dstUser;

	public PortInfo(int port, String srcUser, String dstUser) {
		super(srcUser);
		this.dstUser = dstUser;
		this.port = port;
		// TODO Auto-generated constructor stub
	}

	public int getPort() {
		return this.port;
	}
}

//  私聊消息
class  PrivateChatMessage extends ChatMessage {

	private String dstUser;

	public PrivateChatMessage(String srcUser, String msgContent, String dstUser) {
		super(srcUser, msgContent);
		this.dstUser = dstUser;
	}

	public String getDstUser() {
		return dstUser;
	}	
}

// 用户上线（登录）消息
class UserLogonMessage extends Message {

	private String password;
	public UserLogonMessage(String srcUser, String password) {
		super(srcUser);
		this.password = password;
	}
	
	public String getPassword() {
		return password;
	}
}

// 用户下线消息
class UserLogoffMessage extends Message {
	
	public UserLogoffMessage(String srcUser) {
		super(srcUser);
	}
}

// 在线用户名列表消息
class OnlineUsersMessage extends Message {
	
	private ArrayList<String> onlineUsers = new ArrayList<String>();
	
	public OnlineUsersMessage(String srcUser) {
		super(srcUser);
	}

	public void addUser(String user) {
		onlineUsers.add(user);
	}
	
	public ArrayList<String> getOnlineUsers() {
		return onlineUsers;
	}
}

//响应结果消息
class ResultMessage extends Message {

	public static final int LOGON_SUCCESS = 0;
	public static final int LOGON_FAILURE = 1;
	public static final int LOGOFF_SUCCESS = 2;
	public static final int REGISTER_SUCCESS = 3;
	public static final int REGISTER_FAILURE = 4;
	//在此补充其它响应结果............
	
	private String msg;
	private int result = -1;
	
	public ResultMessage(String srcUser, int result, String msg) {
		super(srcUser); 	// src == null 表示消息是由服务器发送的
		this.result = result;
		this.msg = msg;
	}

	public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public int getResult() {
		return result;
	}

	public void setResult(int result) {
		this.result = result;
	}
	
}

//文件发送请求消息
class FileMessage extends Message{
	
	private String filename;
	private long filelength;
	private String dstUser;
	
	public FileMessage(String srcUser, String dstUser, String filename) {
		super(srcUser);
		this.filename = filename;
		this.dstUser = dstUser;
	}
	
	public FileMessage(String srcUser,String dstUser,String filename, long filelength) {
		super(srcUser);
		this.dstUser = dstUser;
		this.filename = filename;
		this.filelength = filelength;
	}

	public String getFilename() {
		return filename;
	}

	public void setFilename(String filename) {
		this.filename = filename;
	}

	public long getFilelength() {
		return filelength;
	}

	public void setFilelength(long filelength) {
		this.filelength = filelength;
	}

	public String getDstUser() {
		return dstUser;
	}
	
}

//文件发送消息

class SendFileMessage extends Message{

	private boolean accept;
	private int port;
	private String dstUser;
	
	public String getDstUser() {
		return dstUser;
	}
	public SendFileMessage(String srcUser,String dstUser) {
		super(srcUser);
		this.dstUser=dstUser;
	}
	public SendFileMessage(String srcUser,String dstUser,  boolean accept,int port) {
		super(srcUser);
		this.accept = accept;
		this.dstUser = dstUser;
		this.port = port;
	}
	public boolean isAccept() {
		return accept;
	}
	public void setAccept(boolean accept) {
		this.accept = accept;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public void getDstUser(String dstUser) {
		this.dstUser = dstUser;
	}
	
}

