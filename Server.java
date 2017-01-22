import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;



public class Server{
  private ServerSocket server_socket;
  private TreeMap<String, PrintStream> clients;
  public Server(){
    clients = new TreeMap<String, PrintStream>();
    try{
      server_socket = new ServerSocket(5200);
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }

  private void run() throws Exception{
    while(true){
      Socket socket = server_socket.accept();
      (new Thread(new ClientThread(socket, clients))).start();
    }
  }

  public static void main(String[] args) throws Exception{
    Server server = new Server();
    server.run();
  }
}

class ClientThread implements Runnable{

  private Socket socket;
  private PrintStream out;
  private BufferedReader in;
  private TreeMap<String, PrintStream> clients;
  private String name;

  public ClientThread(Socket socket,TreeMap<String, PrintStream> clients){
    try{
      this.clients = clients;
      this.socket = socket;
      this.out = new PrintStream(socket.getOutputStream());
      this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
    }catch(Exception e){
      System.out.println(e.getMessage());
    }
  }
  private void get_client_username(){
    out.println("Enter your name!");
    Boolean accepted_name = false;
    try{
      while(!accepted_name && (this.name = in.readLine()) != null){
        accepted_name = this.name.length() > 0 ? true : false;
      }
      clients.put(this.name, out);
    }catch(Exception e){
      System.out.println(e.getMessage());
    }
  }

  private void listen(){
    try{
      String message;
      while((message = in.readLine()) != null){
        if(message.equals("users")){
          out.println("Connected users:");
          for(String user : clients.keySet()){
            out.println(user);
          }
        }
        // Check if client has specified a user
        if(message.matches(".+-to.+")){
          int index = message.indexOf("-to");
          System.out.println(message.substring(index+3, message.length()));
          String reciever = message.substring(index+3, message.length()).trim();
          if(clients.containsKey(reciever)){
            clients.get(reciever).println(message.substring(0,index) + " from " + this.name);
          }else{
            out.println("User not found!");
          }
        // No user specified -> send to all clients.
        }else{
          for(String key : clients.keySet()){
            clients.get(key).println(message + " from " + this.name);
          }
        }
      }
    }catch(Exception e){
      System.out.println(e.getMessage());
    }
    clients.remove(this.name);

  }

  public void run(){
    System.out.println(socket.getRemoteSocketAddress() + " has connected.");
    get_client_username();
    listen();
  }
}
