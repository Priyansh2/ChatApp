import java.io.* ;
import java.net.* ;
import java.util.* ;
import java.awt.* ;
import java.nio.file.*;

public class ChatServer {
    public static Vector<Socket> ClientSockets; //store client sockets for message broadcasting
    public static Vector<String> LoginNames; // store usernames (assumed unique for every user)
    public static Vector<Chatroom> Chatrooms; // store chatrooms. Each chatroom has two attributes (name of chatroom , chatroom members stored in vector) and several methods like leave, add, listusers, notify, join
    public static Map<String,Chatroom> ConnectedChatroom; // one to many mapping: username -> chatroom in which he/she belongs. Multiple user can have atmost 1 chatroom. If there is no chatroom associated with user then the value is NULL
    public static int max_no_clients; //max clients which can be conneced to server (server capacity)
    public static Vector<Integer> Ports; // clients ports used in case of message broadcasting
    public static DatagramSocket SocUDP; //server udp_socket name
    ChatServer(int max_no_clients_) { //class constructor
        try {
            System.out.println("Server running on localhost Port-6666(TCP), 6661(UDP)");
            ServerSocket Soc = new ServerSocket(6666) ;
            DatagramSocket SocUDP = new DatagramSocket(6661);
            ClientSockets = new Vector<Socket>() ;
            LoginNames = new Vector<String>() ;
            Chatrooms = new Vector<Chatroom>() ;
            ConnectedChatroom = new HashMap<String,Chatroom>();
            max_no_clients=max_no_clients_; Ports = new Vector<Integer>();
            while(true) //server is listening and will never stop unless an error occur
            {
                Socket CSoc = Soc.accept(); // accept the client incoming connection request.
                AcceptClient client_ = new AcceptClient(CSoc,SocUDP) ; // create the 'client_' object of class AcceptClient extending thread class. This for concurrency using threading (each incoming client request is handled independently by a thread)
            }
        }
        catch(Exception e)
        {
            e.printStackTrace(System.out);
            System.exit(0);
        }
    }
    public static void main(String args[]) throws Exception {
        if(args.length==0){
            System.out.println("Maximum number of Users for the Server not given.");
            System.exit(0);
        }
        ChatServer server = new ChatServer(Integer.parseInt(args[0])) ;
    }
}

class AcceptClient extends Thread {
    Socket ClientSocket;
    DataInputStream din ;
    DataOutputStream dout ;
    String LoginName;
    DatagramSocket SocUDP;

    AcceptClient (Socket CSoc, DatagramSocket SocUDP_) throws Exception { //constructor
        ClientSocket = CSoc ;
        SocUDP=SocUDP_;
        din = new DataInputStream(ClientSocket.getInputStream()) ;
        dout = new DataOutputStream(ClientSocket.getOutputStream()) ;
        byte[] initial = new byte[1000];
        DatagramPacket recieve_initial = new DatagramPacket(initial, initial.length); //udp packet of size initial.length i.e, 1000
        SocUDP.receive(recieve_initial); //recieve test message content ("HELLO SERVER!")
        LoginName = din.readUTF() ; //get  username//loginname (assumed unique)
        if(ChatServer.LoginNames.size()==ChatServer.max_no_clients)
        {
            System.out.println("Cannot login user: Server's maximum limit reached");
            dout.writeUTF("Cannot connect: Reached Server's maximum capacity");
            ClientSocket.close();
            din.close();
            dout.close();
            return;
        }
        System.out.println("User "+LoginName+" logged in");
        ChatServer.Ports.add(recieve_initial.getPort()); //add user socket's port
        ChatServer.LoginNames.add(LoginName) ; // add user to username list
        ChatServer.ClientSockets.add(ClientSocket) ;
        ChatServer.ConnectedChatroom.put(LoginName,null);
        start() ; //start the thread
    }
    public void run() {
        while(true)
        {
            try
            {
                String commandfromClient = new String() ;
                commandfromClient = din.readUTF() ; //read client command
                StringTokenizer tokenedcommand = new StringTokenizer(commandfromClient); //parsing client command
                String command=tokenedcommand.nextToken();
                if(command.equals("LOGOUT"))
                {
                    //i think this case shouldn't be implemented at server side.
                    Chatroom C=ChatServer.ConnectedChatroom.get(LoginName);
                    if(C!=null)
                    {
                        String outp=C.Leave(LoginName);
                        if(outp.equals("DEL")) ChatServer.Chatrooms.remove(C);
                        else dout.writeUTF(outp);

                        ClientSocket.close();
                        din.close();
                        dout.close();
                        if(ChatServer.Chatrooms.contains(C))
                            C.Notify(LoginName+" left the chatroom",LoginName);
                        C=null;
                    }
                    ChatServer.LoginNames.remove(LoginName);
                    ChatServer.ClientSockets.remove(ClientSocket);
                }
                if(command.equals("create"))
                {
                    Chatroom C = ChatServer.ConnectedChatroom.get(LoginName);
                    if(C!=null) dout.writeUTF("You are already part of chatroom "+C.name);

                    else
                    {
                        tokenedcommand.nextToken();
                        String chatroomName = tokenedcommand.nextToken();
                        Chatroom chatR = new Chatroom(chatroomName, LoginName);
                        ChatServer.Chatrooms.add(chatR);
                        dout.writeUTF("Chatroom "+chatroomName+" created\nYou are in chatroom "+chatroomName);
                    }
                }
                else if(command.equals("list"))
                {
                    String nxtcomm=tokenedcommand.nextToken();
                    if(nxtcomm.equals("chatrooms"))
                    {
                        String outp="";
                        int sz = ChatServer.Chatrooms.size();
                        if(sz==0) dout.writeUTF("No Chatrooms exist");

                        else
                        {
                            for(int i=0;i<sz;i++) outp+=ChatServer.Chatrooms.elementAt(i).name+"\n";
                            dout.writeUTF(outp);
                        }
                    }
                    else if(nxtcomm.equals("users"))
                    {
                        //user can use this command inside a chatroom so check if he/she belongs to any chatroom
                        Chatroom C =ChatServer.ConnectedChatroom.get(LoginName);
                        if(C==null) dout.writeUTF("You are not part of any chatroom");
                        else
                        {
                            Vector<String> C_users=C.ListUsers();
                            String outp="";
                            for(int i=0;i<C_users.size();i++) outp+=C_users.elementAt(i)+"\n";
                            dout.writeUTF(outp);
                        }
                    }
                    else {
                        dout.writeUTF("CommandNotFoundError: list command has few or invalid arguements");
                    }
                }
                else if(command.equals("join"))
                {
                    String chatroomName=tokenedcommand.nextToken();
                    Chatroom C =ChatServer.ConnectedChatroom.get(LoginName);
                    if(C!=null) {
                        dout.writeUTF("You are already part of chatroom "+C.name);
                    }
                    else
                    {

                        boolean found= false;
                        for(int i=0;i<ChatServer.Chatrooms.size();i++){
                            C = ChatServer.Chatrooms.elementAt(i);
                            if(C.name.equals(chatroomName)){
                                String outp=C.Join(LoginName);
                                dout.writeUTF(outp);
                                C.Notify(LoginName+" joined the chatroom",LoginName);
                                found = true;
                                break;
                            }
                        }
                        if(!found) dout.writeUTF(chatroomName+" doesn't exist");

                    }
                }
                else if(command.equals("leave"))
                {
                    Chatroom C =ChatServer.ConnectedChatroom.get(LoginName);
                    if(C==null) dout.writeUTF("You are not part of any chatroom");
                    else
                    {
                        String C_name=C.name;
                        String outp = C.Leave(LoginName);
                        C.Notify(LoginName+" left the chatroom",LoginName);
                        if(outp.equals("DEL"))
                        {
                            ChatServer.Chatrooms.remove(C);
                            C=null;
                            dout.writeUTF("You left Chatroom "+C_name+'\n'+C_name+" deleted");
                        }
                        else dout.writeUTF(outp);

                    }
                }
                else if(command.equals("add"))
                {
                    //user can use this commands inside a chatroom so check if user belongs to any chatroom
                    String user = tokenedcommand.nextToken();
                    Chatroom C = ChatServer.ConnectedChatroom.get(LoginName);
                    if(C==null) dout.writeUTF("You are not a part of any chatroom");
                    else
                    {
                        String outp = C.Add(user);
                        //NOTE: user can add other_user if 'other_user' username exists! (its username is in database) and 'other_user' do not present in chatroom of 'user' neither it present is any other chatroom. Add() method will take care of error handling with appropriate response messages.
                        if(!outp.contains("Connot")){
                            String C_name = C.name;
                            C.Notify(LoginName+" added "+user+" to chatroom "+C_name,LoginName);
                        }
                        dout.writeUTF(outp);
                    }
                }
                else if(command.equals("reply"))
                {
                    StringTokenizer cmd = new StringTokenizer(commandfromClient);
                    cmd.nextToken();
                    String fl,tp;
                    boolean isFile=false;
                    if(cmd.hasMoreTokens())
                    {
                        fl=cmd.nextToken();
                        File f = new File(fl);
                        String fileName = f.getName();
                        //System.out.println(fileName);
                        if(cmd.hasMoreTokens())
                        {
                            tp=cmd.nextToken();
                            if(tp.equals("tcp"))
                            {
                                isFile=true;
                                //File transfer
                                Chatroom C = ChatServer.ConnectedChatroom.get(LoginName);
                                if(C==null) dout.writeUTF("You are not part of any chatroom");
                                else
                                {
                                    String st_ = din.readUTF(); //get filelength from client
                                    StringTokenizer stt = new StringTokenizer(st_);
                                    stt.nextToken();
                                    int fileLength = Integer.parseInt(stt.nextToken());
                                    C.Notify("FILE "+fileName+" TCP LENGTH "+fileLength,LoginName);
                                    byte[] file_contents = new byte[1000];
                                    int bytesRead=0,size=1000;
                                    if(size>fileLength)size=fileLength;
                                    while((bytesRead=din.read(file_contents,0,size))!=-1 && fileLength>0)
                                    {
                                        for(int i=0;i<C.Members.size();i++)
                                        {
                                            String member = C.Members.elementAt(i);
                                            if(!member.equals(LoginName))
                                            {
                                                Socket sendSoc = ChatServer.ClientSockets.elementAt(ChatServer.LoginNames.indexOf(member));
                                                DataOutputStream senddout = new DataOutputStream(sendSoc.getOutputStream());
                                                senddout.write(file_contents,0,size);
                                            }
                                        }
                                        fileLength-=size;
                                        if(size>fileLength) size=fileLength;
                                    }
                                    System.out.println("Broadcasting file...");
                                }
                            }
                            else if(tp.equals("udp"))
                            {
                                isFile=true;
                                //File transfer
                                Chatroom C = ChatServer.ConnectedChatroom.get(LoginName);
                                if(C==null) dout.writeUTF("You are not part of any chatroom");
                                else
                                {
                                    String st_ = din.readUTF();
                                    StringTokenizer stt = new StringTokenizer(st_);
                                    stt.nextToken();
                                    int fileLength = Integer.parseInt(stt.nextToken());
                                    C.Notify("FILE "+fileName+" UDP LENGTH "+fileLength,LoginName);
                                    int size = 1024;
                                    byte[] file_contents = new byte[size];
                                    if(size>fileLength)size=fileLength;
                                    DatagramPacket packetUDP;
                                    while(fileLength>0)
                                    {
                                        packetUDP = new DatagramPacket(file_contents,size);
                                        SocUDP.receive(packetUDP);
                                        for(int i=0;i<C.Members.size();i++)
                                        {
                                            String member = C.Members.elementAt(i);
                                            if(!member.equals(LoginName))
                                            {
                                                String port = ChatServer.Ports.elementAt(ChatServer.LoginNames.indexOf(member)).toString();
                                                packetUDP = new DatagramPacket(file_contents,size,InetAddress.getByName("127.0.0.1"),Integer.parseInt(port));
                                                SocUDP.send(packetUDP);
                                            }
                                        }
                                        fileLength-=size;
                                        if(size>fileLength) size=fileLength;
                                    }
                                }
                            }
                        }
                    }
                    if(isFile==false)
                    {
                        //if arguements of reply commands is None that means client wants to send chat message
                        String msgfromClient=LoginName+":";
                        Chatroom C = ChatServer.ConnectedChatroom.get(LoginName);
                        while(tokenedcommand.hasMoreTokens()) msgfromClient+=" "+ tokenedcommand.nextToken();
                        if(C==null) dout.writeUTF("You are not part of any chatroom");
                        else C.Notify(msgfromClient,LoginName);
                    }
                }
                else
                {
                    dout.writeUTF("CommandNotFoundError: Command do not exists!!");
                }
            }
            catch(Exception e) {
                e.printStackTrace(System.out) ; break;
            }
        }
    }
}

class Chatroom {
    Vector<String> Members = new Vector<String>();
    String name;
    Chatroom (String name,String member) {
        this.name = name;
        this.Members.add(member);
        ChatServer.ConnectedChatroom.put(member,this);
    }
    public String Join (String member) {
        this.Members.add(member);
        ChatServer.ConnectedChatroom.put(member,this);
        return ("Joined Chatroom "+this.name);
    }
    public String Leave (String member) {
        this.Members.remove(member);
        ChatServer.ConnectedChatroom.put(member,null);
        if(this.Members.isEmpty()) return ("DEL");
        else return("You left chatroom "+this.name);
    }
    public Vector<String> ListUsers() {
        return this.Members;
    }
    public String Add(String memberAdd) {
        if(this.Members.contains(memberAdd)) return(memberAdd+" is already a part of "+this.name);
        if(!ChatServer.LoginNames.contains(memberAdd)) return("The username "+memberAdd+" doesn't exist");
        if (ChatServer.ConnectedChatroom.get(memberAdd)!=null) {
            String name_ = ChatServer.ConnectedChatroom.get(memberAdd).name;
            return("Cannot add "+memberAdd+" to chatroom "+this.name+"\n"+memberAdd+" already a part of chatroom "+name_);
        }//shortcut of below
        /*for(int c=0; c<ChatServer.Chatrooms.size();c++)
        {
            Chatroom C = ChatServer.Chatrooms.elementAt(c);
            if(C.Members.contains(memberAdd)) return("Cannot add "+memberAdd+" to chatroom "+this.name+"\n"+memberAdd+" already a part of chatroom "+C.name);
        }*/

        this.Members.add(memberAdd);
        ChatServer.ConnectedChatroom.put(memberAdd,this);
        return(memberAdd+" added to chatroom "+this.name);
    }
    public void Notify(String msg,String no_notif) {
        for(int i=0;i<this.Members.size();i++)
        {
            String member =this.Members.elementAt(i);
            if(!member.equals(no_notif))
            {
                try {

                    Socket sendSoc = ChatServer.ClientSockets.elementAt(ChatServer.LoginNames.indexOf(member)); //get socket of member to whom message is broadcasted
                    DataOutputStream senddout = new DataOutputStream(sendSoc.getOutputStream());
                    senddout.writeUTF(msg);
                }
                catch(Exception e){
                    System.out.println("Unable to notify users of chatroom " + this.name + " from message sent by "+no_notif);
                }
            }
        }
    }
}
