package com.todoListApp;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
public class App{
    public static void main(String[] args) throws  IOException
    {
        int portNum=3000;
        try (ServerSocket serversocket = new ServerSocket(portNum)) {
            while(true)
            {
                Socket clientCame=serversocket.accept();
                new Thread(() -> {
                    try {
                        handleClient(clientCame);
                    }
                    catch (SQLException | IOException e) {
                        e.printStackTrace();
                    }
                });    
            }
        }
    }
    public static void handleClient(Socket socket) throws IOException,SQLException
    {
        try(BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream())))
        {
            String type=in.readLine();
            if(type.startsWith("POST"))
            {
                int lenOfData=0;
                String readLen;
                while((readLen=in.readLine()) !=null && !readLen.isEmpty())
                {
                    if(readLen.startsWith("Content-length"))
                    {
                        String[] tempReadLen=readLen.split(":");
                        lenOfData=Integer.parseInt(tempReadLen[1].trim());
                    }
                }
                char[] tempBody=new char[lenOfData];
                in.read(tempBody);
                String body=new String(tempBody);
                String[] splittedStr=body.split("&");
                String eMail=java.net.URLDecoder.decode(splittedStr[0].split("=")[1],"UTF-8");
                String passWord=java.net.URLDecoder.decode(splittedStr[1].split("=")[1],"UTF-8");
                String comparePassWord=java.net.URLDecoder.decode(splittedStr[2].split("=")[1],"UTF-8");
                if(!(passWord.equals(comparePassWord)))
                {
                    String msg="Password mismatch!";
                    String response="HTTP/1.1 400 Bad Request\r\n"+"Content-Type: text/plain\r\n"+"Content-Length:"+msg.length()+"\r\n\r\n"+msg;
                    socket.getOutputStream().write(response.getBytes());
                    socket.getOutputStream().flush();
                    return;
                }
                storeValues(eMail, passWord);
                String msg="login successfully";
                String response="HTTP/1.1 200 OK\r\n"+"Content-Type: text/plain\r\n"+"Content-Length:"+msg.length()+"\r\n\r\n"+msg;
                socket.getOutputStream().write(response.getBytes());
                socket.getOutputStream().flush();
            }
        }
    }
    public static Connection connectDB() throws IOException,SQLException
    {
        String url="jdbc:mysql://localhost:3306/formdb";
        String username="root";
        String password="Kakashi@27";
        return DriverManager.getConnection(url, username, password);
    }
    public static void storeValues(String eMAil,String passWord) throws SQLException, IOException
    {
        String sql="Insert values into loginPage(emailId,password) values(?,?)";
        try(Connection conn=connectDB();PreparedStatement pSt=conn.prepareStatement(sql))
        {
            pSt.setString(1,eMAil);
            pSt.setString(2, passWord);
            pSt.executeUpdate();
        }
    }
}
