package com.todoListApp;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.UUID;
public class App
{
    private static final Map<String,String> sessions=new ConcurrentHashMap<>();
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
                }).start();    
            }
        }
    }
    public static void handleClient(Socket socket) throws IOException,SQLException
    {
        try(BufferedReader in=new BufferedReader(new InputStreamReader(socket.getInputStream())))
        {
            String type=in.readLine();
            if(type==null)
                return;
            int lenOfData=0;
            String readLen;
            String cookies=null;
            while((readLen=in.readLine()) !=null && !readLen.isEmpty())
            {
                if(readLen.startsWith("Content-Length"))
                {
                    String[] tempReadLen=readLen.split(":");
                    lenOfData=Integer.parseInt(tempReadLen[1].trim());
                }
                else if(readLen.startsWith("Cookie:"))
                    cookies=readLen.substring("Cookie:".length()).trim();
            }
            String body="";
            if(lenOfData!=0)
            {
                char[] tempBody=new char[lenOfData];
                in.read(tempBody);
                body=new String(tempBody);
            }
            if(type.startsWith("GET /signup.html"))
                getFile("signup.html",socket,"text/html");
            else if(type.startsWith("POST /signup"))
                postSignUpInfo(socket, body);
            else if(type.startsWith("GET /userForm"))
                getFile("UserForm.html",socket,"text/html");
            else if(type.startsWith("POST /userFormDetails"))
                postUserFormDetails(socket,body,cookies);
            else if(type.startsWith("GET /loginPage"))
                getFile("login.html",socket,"text/html");
            else if(type.startsWith("POST /userDetails"))
                handleLogin(socket,body);
            else if(type.startsWith("GET /userDetails"))
                getUserDetails(cookies,socket);
            else if (type.startsWith("GET /styles.css"))
                getFile("styles.css", socket, "text/css");
            else if (type.startsWith("GET /scripts.js"))
                getFile("scripts.js", socket, "application/javascript");

        }
    }
    public static void getFile(String filename,Socket socket,String contentType) throws IOException
    {
        Path path = Paths.get(filename);
        byte[] data = Files.readAllBytes(path);
        String res="HTTP/1.1 200 OK\r\n"+"Content-Type: "+contentType+"\r\n"+"Content-Length: "+data.length+"\r\n\r\n";
        socket.getOutputStream().write(res.getBytes());
        socket.getOutputStream().write(data);
        socket.getOutputStream().flush();
    }
    public static void postSignUpInfo(Socket socket,String body) throws IOException,SQLException
    {
        String[] splittedStr=body.split("&");
        String eMail=java.net.URLDecoder.decode(splittedStr[0].split("=")[1],"UTF-8");
        String passWord=java.net.URLDecoder.decode(splittedStr[1].split("=")[1],"UTF-8");
        String comparePassWord=java.net.URLDecoder.decode(splittedStr[2].split("=")[1],"UTF-8");
        if(!(passWord.equals(comparePassWord)))
        {
            String msg="Password mismatch!";
            String response="HTTP/1.1 400 Bad Request\r\n"+"Content-Type: text/plain\r\n"+"Content-Length: "+msg.length()+"\r\n\r\n"+msg;
            socket.getOutputStream().write(response.getBytes());
            socket.getOutputStream().flush();
            return;
        }
        else{
        storeValues(eMail, passWord);
        String sessionId=UUID.randomUUID().toString();
        sessions.put(sessionId,eMail);
        String response="HTTP/1.1 302 Found\r\n"+"Location: /userForm\r\n"+"Set-Cookie: sessionId="+sessionId+";HttpOnly\r\n"+"Content-Length: 0\r\n\r\n";
        socket.getOutputStream().write(response.getBytes());
        socket.getOutputStream().flush();}
    }
    public static void postUserFormDetails(Socket socket,String body,String cookies) throws IOException,SQLException
    {
        String Id=getCookiesValue(cookies,"sessionId");
        if(Id!=null)
        {
            String eMail=sessions.get(Id);
            String[] split=body.split("&");
            String name=java.net.URLDecoder.decode(split[0].split("=")[1],"UTF-8");
            String dob=java.net.URLDecoder.decode(split[1].split("=")[1],"UTF-8");
            String phoneNum=java.net.URLDecoder.decode(split[2].split("=")[1],"UTF-8");
            String primarySkills=java.net.URLDecoder.decode(split[3].split("=")[1],"UTF-8");
            String College=java.net.URLDecoder.decode(split[4].split("=")[1],"UTF-8");
            storeUserDetails(eMail,name, dob, phoneNum, primarySkills, College);
            String res="HTTP/1.1 302 Found \r\n"+"Location: /userDetails\r\n"+"Content-Length: 0\r\n\r\n";
            socket.getOutputStream().write(res.getBytes());
            socket.getOutputStream().flush();
        }
        else
        {
            String mail="Signup first";
            String res="HTTP/1.1 400 Bad Request\r\n"+"Content-Type: text/plain\r\n"+"Content-Length: "+mail.length()+"\r\n\r\n"+mail;
            socket.getOutputStream().write(res.getBytes());
            socket.getOutputStream().flush();
        }

    }
    public static void handleLogin(Socket socket,String body) throws IOException,SQLException
    {
        String[] splittedstr=body.split("&");
        String eMail=java.net.URLDecoder.decode(splittedstr[0].split("=")[1],"UTF-8");
        String passWord=java.net.URLDecoder.decode(splittedstr[1].split("=")[1],"UTF-8");
        if(!validMail(eMail,passWord))
        {
            String mail="Enter a valid details(email/password)";
            String res="HTTP/1.1 400 Bad Request\r\n"+"Content-Type: text/plain\r\n"+"Content-Length: "+mail.length()+"\r\n\r\n"+mail;
            socket.getOutputStream().write(res.getBytes());
            socket.getOutputStream().flush();
        }
        else{
        String sessionId=UUID.randomUUID().toString();
        sessions.put(sessionId,eMail);
        String res="HTTP/1.1 302 Found\r\n"+"Location: /userDetails\r\n"+"Set-Cookie: sessionId="+sessionId+";HttpOnly\r\n"+"Content-Length: 0\r\n\r\n";
        socket.getOutputStream().write(res.getBytes());
        socket.getOutputStream().flush();}
        
    }
    public static String  getCookiesValue(String cookie,String name)
    {
        if(cookie==null)
            return null;
        String[] iniSplit=cookie.split(";");
        for(int i=0;i<iniSplit.length;i++)
        {
            String[] temp=iniSplit[i].trim().split("=",2);
            if(temp[0].equals(name))
                return temp[1];
        }
        return null;

    }
    public static void getUserDetails(String cookies,Socket socket) throws IOException,SQLException
    {
        String Id=getCookiesValue(cookies,"sessionId");
        if(Id!=null){
        String eMail=sessions.get(Id);
        String sql="select * from userdetails where eMail=?";
        try(Connection conn=connectDB();PreparedStatement pSt=conn.prepareStatement(sql))
        {
            pSt.setString(1,eMail);
            ResultSet rs=pSt.executeQuery();
            String responseHtml;
            if(rs.next())
            {
                String name=rs.getString("Name");
                String Dob=rs.getString("Dob");
                String phoneNum=rs.getString("phoneNum");
                String Skills=rs.getString("Skills");
                String College=rs.getString("College");
                String fileDetails=Files.readString(Paths.get("userInfoTemplate.html"),StandardCharsets.UTF_8);
                responseHtml = fileDetails.replace("{Name}",name).replace("{Dob}",Dob).replace("{phoneNum}",phoneNum).replace("{Skills}",Skills).replace("{College}",College);
                byte[] resByteFile=responseHtml.getBytes(StandardCharsets.UTF_8);
                String res="HTTP/1.1 200 OK\r\n"+"Content-Type: text/html\r\n"+"Content-Length: "+resByteFile.length+"\r\n\r\n";
                socket.getOutputStream().write(res.getBytes());
                socket.getOutputStream().write(resByteFile);
                socket.getOutputStream().flush();
            }   
        }}
        else{
            String mail="SignUp first";
            String res="HTTP/1.1 400 Bad Request\r\n"+"Content-Type: text/plain\r\n"+"Content-Length: "+mail.length()+"\r\n\r\n"+mail;
            socket.getOutputStream().write(res.getBytes());
            socket.getOutputStream().flush();
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
        String sql="insert into loginPage(emailId,password) values(?,?)";
        try(Connection conn=connectDB();PreparedStatement pSt=conn.prepareStatement(sql))
        {
            pSt.setString(1,eMAil);
            pSt.setString(2, passWord);
            pSt.executeUpdate();
        }
    }
    public static void storeUserDetails(String eMail,String Name,String Dob,String phoneNum,String Skills,String College) throws SQLException,IOException
    {
        String sql="insert into userDetails(eMail,Name,Dob,phoneNum,Skills,College) values(?,?,?,?,?,?)";
        try(Connection conn=connectDB();PreparedStatement pSt=conn.prepareStatement(sql))
        {
            pSt.setString(1,eMail);
            pSt.setString(2,Name);
            pSt.setString(3,Dob);
            pSt.setString(4,phoneNum);
            pSt.setString(5,Skills);
            pSt.setString(6,College);
            pSt.executeUpdate();
        }
    }
    public static boolean validMail(String mail,String psd) throws IOException,SQLException
    {
        String sql="select * from loginPage where emailId=?";
        try(Connection conn=connectDB();PreparedStatement pSt=conn.prepareStatement(sql))
        {
            pSt.setString(1, mail);
            ResultSet rs=pSt.executeQuery();
            String s="0";
            if(rs.next())
            {
                    s=rs.getString("emailId");
                    if(!s.equals(mail))
                        return false;
                    String enterKey = rs.getString("password");
                    if(!enterKey.equals(psd))
                        return false;
            }
            else 
                return false;
            return true;
        }
    }
}