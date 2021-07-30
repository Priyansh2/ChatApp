import java.awt.* ;
import java.io.* ;
import java.net.* ;
import java.nio.file.*;
import java.util.* ;


public class User {
    public static String ip="127.0.0.1";
    public static int port = 6661;
    public static DatagramSocket clientSocUDP;
    public static void main(String args[])   {
        try
        {
            if(args.length==0)
            {
                System.out.println("No Username given\n");
                System.exit(0);
            }
            Socket clientSoc;
            DataInputStream din;
            DataOutputStream dout;
            String LoginName=args[0];
            clientSocUDP = new DatagramSocket();
            clientSoc = new Socket(ip,6666) ;
            System.out.println("Connected to Server at localhost Port-6666(TCP)");
            din = new DataInputStream(clientSoc.getInputStream());
            dout = new DataOutputStream(clientSoc.getOutputStream());
            String a="HELLO SERVER!!";
            byte[] file_contents = new byte[1000];
            file_contents = a.getBytes();
            DatagramPacket initial = new DatagramPacket(file_contents,file_contents.length,InetAddress.getByName(ip),port); /*For sending a packet via UDP, we should know 4 things, the message to send, its length, ipaddress of destination, port at which destination is listening.*/
            //System.out.println(initial);
            clientSocUDP.send(initial); //send for testing
            dout.writeUTF(LoginName);

            //Recieve messages
            new Thread(new RecievedMessagesHandler (din,LoginName)).start();

            //Send messages
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
            String inputLine=null;
            while(true)
            {
                try
                {
                    inputLine=bufferedReader.readLine();
                    dout.writeUTF(inputLine);
                    if(inputLine.equals("LOGOUT"))
                    {
                        clientSoc.close();
                        din.close();
                        dout.close();
                        System.out.println("Logged Out");
                        System.exit(0);
                    }
                    StringTokenizer tokenedcommand = new StringTokenizer(inputLine);
                    //check file transfer
                    String comm,fl,typ;
                    comm = tokenedcommand.nextToken();
                    if(comm.equals("reply"))
                    {
                        boolean isFile=false;
                        if(tokenedcommand.hasMoreTokens())
                        {
                            fl=tokenedcommand.nextToken();
                            if(tokenedcommand.hasMoreTokens())
                            {
                                typ=tokenedcommand.nextToken();
                                //file transfer
                                if(typ.equals("tcp"))
                                {
                                    isFile=true;
                                    File file = new File(fl);
                                    FileInputStream fpin = new FileInputStream(file);
                                    BufferedInputStream bpin = new BufferedInputStream(fpin);
                                    long fileLength =  file.length(), current=0, start = System.nanoTime();
                                    dout.writeUTF("LENGTH "+fileLength); //sending filelength to the server
                                    int size = 1000; //sending file content in chunks of size 1000 bytes
                                    while(current!=fileLength)
                                    {
                                        if(fileLength - current >= size) current+=size;
                                        else {
                                            size = (int)(fileLength-current);
                                            current=fileLength;
                                        }
                                        file_contents = new byte[size];
                                        bpin.read(file_contents,0,size);
                                        dout.write(file_contents);
                                        System.out.println("Sending file..."+(current*100/fileLength)+"% complete");
                                    }
                                    fpin.close();
                                    bpin.close();
                                    System.out.println("TCP: Sent file");
                                }
                                else if(typ.equals("udp"))
                                {
                                    int size=1024;
                                    isFile=true;
                                    File file = new File(fl);
                                    FileInputStream fpin = new FileInputStream(file);
                                    BufferedInputStream bpin = new BufferedInputStream(fpin);
                                    long fileLength = file.length(), current =0, start =System.nanoTime();
                                    dout.writeUTF("LENGTH "+fileLength);
                                    while(current!=fileLength)
                                    {
                                        if(fileLength - current >= size) current+=size;
                                        else {
                                            size = (int)(fileLength-current);
                                            current=fileLength;
                                        }
                                        file_contents = new byte[size];
                                        bpin.read(file_contents,0,size);
                                        DatagramPacket sendPacket = new DatagramPacket(file_contents,size,InetAddress.getByName(ip),port);
                                        clientSocUDP.send(sendPacket);
                                        System.out.println("Sending file..."+(current*100/fileLength)+"% complete");
                                    }
                                    fpin.close();
                                    bpin.close();
                                    System.out.println("UDP: Sent file");
                                }
                            }
                        }
                    }
                } catch(Exception e){
                    System.out.println(e);
                    break;
                }
            }
        }
        catch(Exception e) {
            System.out.println(e);
            System.exit(0);
        }
    }
}

class RecievedMessagesHandler implements Runnable {
    private DataInputStream server;
    private String LoginName;
    public RecievedMessagesHandler(DataInputStream server,String LoginName) {
        this.server = server;
        this.LoginName = LoginName;
    }
    @Override
    public void run() {
        String inputLine=null;
        while(true)
        {
            try {
                inputLine=server.readUTF();
                StringTokenizer st = new StringTokenizer(inputLine);
                String message_type = st.nextToken();
                if(message_type.equals("FILE"))
                {
                    //File recieve
                    String fileName=st.nextToken();
                    String typ=st.nextToken();
                    st.nextToken();
                    int fileLength = Integer.parseInt(st.nextToken());
                    byte[] file_contents = new byte[1000];
                    Path base_dir = Paths.get(System.getProperty("user.dir"));
                    Path dir = Paths.get(base_dir.toString(), "temp",LoginName);
                    File dir_ = new File(dir.toString());
                    if(!dir_.exists()) dir_.mkdirs();
                    dir = Paths.get(dir_.toString(),fileName);
                    FileOutputStream fpout = new FileOutputStream(dir.toString());
                    //System.out.println(dir.toString());
                    //System.out.println(fileLength);
                    BufferedOutputStream bpout = new BufferedOutputStream(fpout);
                    DatagramPacket receivePacket;
                    if(typ.equals("TCP"))
                    {
                        int bytesRead=0,size=1000,current=0,total = fileLength;
                        if(size>fileLength)size=fileLength;
                        while((bytesRead=server.read(file_contents,0,size))!=-1 && fileLength>0)
                        {

                            bpout.write(file_contents,0,size);
                            if(total - current >= size) current+=size;
                            else current=total;
                            System.out.println("Recieving file..."+(current*100/total)+"% complete");
                            fileLength-=size;
                            if(size>fileLength) size=fileLength;
                            //file_contents = new byte[1000];
                        }
                        bpout.flush();
                        fpout.close();
                        bpout.close();
                        System.out.println("TCP: Recieved file");
                    }
                    else
                    {
                        int size=1024,current=0,total = fileLength;
                        file_contents = new byte[size];
                        if(size>fileLength) size=fileLength;
                        System.out.println("DEBUG: UDP FILELENGTH ==> "+fileLength);
                        while(fileLength>0)
                        {
                            receivePacket  = new DatagramPacket(file_contents, size);
                            System.out.println("DEBUG: UDP start"); //start
                            User.clientSocUDP.receive(receivePacket);
                            System.out.println("DEBUG: UDP received"); //recieved chunk
                            bpout.write(file_contents,0,size);
                            System.out.println("DEBUG: UDP write"); //write chunk
                            if(total - current >= size) current+=size;
                            else current=total;
                            System.out.println("Recieving file..."+(current*100/total)+"% complete");

                            fileLength-=size;
                            if(size>fileLength) size=fileLength;
                        }
                        bpout.flush();
                        fpout.close();
                        bpout.close();

                        System.out.println("UDP: Recieved file");
                    }
                }
                else
                    System.out.println(inputLine);
            }
            catch(Exception e){
                e.printStackTrace(System.out);
                break;
            }
        }
    }
}
