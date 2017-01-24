import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;


public class Client{
  private Socket socket;
  private BufferedReader in;
  public PrintStream out;

  public Client(int port, String ip){

    try{
      this.socket = new Socket(ip , port);
      this.out = new PrintStream(socket.getOutputStream());
      this.in = new BufferedReader(new InputStreamReader(System.in));
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
    // Start thread to listen for incoming messages.
    try{
      (new Thread(new MessageListener(socket, this))).start();
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }

  private void clean(){
    try{
      out.close();
      in.close();
      socket.close();

    }catch(Exception e){
      e.printStackTrace();
    }
  }

  public void start_file_transfer_sender(String message){

    int index = message.indexOf(":");
    String reciever = message.substring(index + 1, message.indexOf(";"));
    String filename = message.substring(message.indexOf(";") + 1, message.indexOf(" ", message.indexOf(";")));
    File file = null;

    try{
       file = new File(filename);
    }catch(Exception e){
      e.printStackTrace();
    }

    try{
      int port = 5001;
      Boolean found_port = false;
      ServerSocket server_socket = null;

          server_socket = new ServerSocket(port);
          found_port = true;

      out.println("-m PORT:" + port + ";" + file.length() + ";" + filename + " -to " + reciever);
      Socket socket = server_socket.accept();
      DataOutputStream dos = new DataOutputStream(socket.getOutputStream());
		  FileInputStream fis = new FileInputStream(file);
		  byte[] buffer = new byte[(int)file.length()];
  		while (fis.read(buffer) > 0) {
  			dos.write(buffer);
  		}
      socket.close();
      fis.close();
      dos.close();
      server_socket.close();
    }catch(Exception e){
      e.printStackTrace();
    }
    System.out.println("transfer sender done");
  }

  private Boolean file_exists(String message){
    String flag = "-send-file ";
    int end_index = message.indexOf(" ", message.indexOf(flag) + flag.length());
    end_index = end_index == -1 ? message.length() : end_index;
    String filename = message.substring(message.indexOf("-send-file ") + 11, end_index);
    File file = new File(filename);
    return (file.exists() && !file.isDirectory());
  }

  public void start_file_transfer_reciever(String message){
    int port = Integer.parseInt(message.substring(5,9));
    int size = Integer.parseInt(message.substring(10, message.indexOf(";", 10)));
    String filename = message.substring(message.indexOf(";", 10) + 1, message.indexOf(" "));
    try{
      Socket socket = new Socket("130.237.227.11", port);
      byte [] buffer  = new byte [size];
      DataInputStream dis = new DataInputStream(socket.getInputStream());
      FileOutputStream fos = new FileOutputStream(filename);
      while(dis.read(buffer) > 0){
        fos.write(buffer);
      }
      socket.close();
      dis.close();
      fos.close();
    }catch(Exception e){
      e.printStackTrace();
    }

    System.out.println("transfer reciever done");
  }

  public void run() throws Exception{
    String message;
    try{
      System.out.print("> ");
      while((message = in.readLine()) != null){
        if(message.equals("QUIT")){
          clean();
          break;
        }
        if(message.matches(".*-send-file [a-zA-Z0-9]+.*")){
          if(!file_exists(message)){
            System.out.println("File does not exist!");
            continue;
          }
        }
        out.println(message);
        System.out.print("> ");
      }

    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }

  public static void main(String[] args) throws Exception{
    if(args.length < 2){
      System.out.println("Please specifiy port and ip-adress! <port> <ip-address>");
      System.exit(-1);
    }
    int port = Integer.parseInt(args[0]);
    String ip = args[1];

    Client client = new Client(port, ip);
    client.run();
  }
}

class MessageListener implements Runnable {
  private Socket socket;
  BufferedReader in;
  Client client;
  public MessageListener(Socket socket, Client client) throws Exception{
    this.socket = socket;
    this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    this.client = client;
  }

  public void run() {
    try{
      String message;
      while((message = in.readLine()) != null){
        if(message.matches("APPROVED-TRANSFER.*")){
          System.out.println("transfer approved");
          client.start_file_transfer_sender(message);
          continue;
        }else if(message.matches("REJECTED-TRANSFER.*")){
          System.out.println("transfer rejected");
          continue;
        }else if(message.matches("PORT:[0-9]{4};[0-9]+.+")){
          client.start_file_transfer_reciever(message);
        }else{
          System.out.println(message);
          System.out.print("> ");
        }
      }
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }
}
