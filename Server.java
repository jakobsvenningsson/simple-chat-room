import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

class Message
{
    public String content;
    public String from;
 };

public class Server{
  private ServerSocket server_socket;
  private TreeMap<String, ClientThread> clients;
  public Server(){
    clients = new TreeMap<String, ClientThread>();
    try{
      server_socket = new ServerSocket(5200);
    }catch(Exception e){
      e.printStackTrace();
      System.exit(-1);
    }
  }
  private void run() throws Exception{
    MessageConsumer m_consumer = new MessageConsumer(clients);
    (new Thread(m_consumer)).start();

    while(true){
      Socket socket = server_socket.accept();
      (new Thread(new ClientThread(socket, clients, m_consumer))).start();
    }
  }

  public static void main(String[] args) throws Exception{
    Server server = new Server();
    server.run();
  }
}

class MessageConsumer implements Runnable{
  private TreeMap<String, ClientThread> clients;
  private LinkedList<Message> messageQue;

  public MessageConsumer(TreeMap<String, ClientThread> clients){
    this.clients = clients;
    this.messageQue = new LinkedList<Message>();
  }

  private void print_users(String from){
    clients.get(from).out.println("Connected users:");
    for(String user : clients.keySet()){
      clients.get(from).out.println(user);
    }
  }
  public synchronized void enqueue_message(Message message){
    messageQue.add(message);
    notifyAll();
  }
  private synchronized void consume(){
    while(true){
      while(messageQue.isEmpty()) {
        try{
          wait();
        }catch(Exception e){
          e.printStackTrace();
        }
      }
      Message message = messageQue.pop();
      if(message.content.equals("users")){
        print_users(message.from);
        continue;
      }
      // Check if client has specified a user
      else if(message.content.matches(".+-to [a-zA-Z0-9]+")){
        int index = message.content.indexOf("-to");
        System.out.println(message.content.substring(index+3, message.content.length()));
        String reciever = message.content.substring(index+3, message.content.length()).trim();
        if(clients.containsKey(reciever)){
          clients.get(reciever).out.println(message.content.substring(0,index) + " from " + message.from);
        }else{
          clients.get(message.from).out.println("User not found!");
        }
      // No user specified -> send to all clients.
      }else{
        for(String key : clients.keySet()){
          clients.get(key).out.println(message.content + " from " + message.from);
        }
      }
    }
  }
  public void run(){
    System.out.println("MessageListener is running...");
    consume();
  }
}

class ClientThread implements Runnable{

  private Socket socket;
  public PrintStream out;
  private BufferedReader in;
  private TreeMap<String, ClientThread> clients;
  private String name;
  private MessageConsumer m_consumer;
  private Boolean recieve_file = false;

  public ClientThread(Socket socket,TreeMap<String, ClientThread> clients, MessageConsumer consumer){
    try{
      this.clients = clients;
      this.socket = socket;
      this.out = new PrintStream(socket.getOutputStream());
      this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.m_consumer = consumer;
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
      clients.put(this.name, this);
    }catch(Exception e){
      System.out.println(e.getMessage());
    }
  }
  public void transfer_file_sender(Message message){
    if(!message.content.matches(".+-to [a-zA-Z0-9]+")){
      clients.get(message.from).out.println("Specify a reciever!");
      return;
    }
    int index = message.content.indexOf("-send-file");
    String filename = message.content.substring(index+11, message.content.indexOf(" ", index + 11)).trim();
    index = message.content.indexOf("-to");
    String reciever = message.content.substring(index + 4, message.content.length()).trim();
    if(!clients.containsKey(reciever)){
      clients.get(message.from).out.println("User not found!");
      return;
    }
    clients.get(reciever).out.println("START-TRANSFER-RECIEVER");
    clients.get(reciever).out.println(message.from + " wants to send you the file: " + filename + " ,accept?(y/n)");

    System.out.println("2");

    String response = null;
    try{
      response = in.readLine();
      System.out.println("3");

    }catch(Exception e){
      e.printStackTrace();
    }
    if(response.equals("ACCEPTED-TRANSFER")){
      System.out.println("accepted");
    }else{
      System.out.println("rejected");
    }
  }

  public void transfer_file_reciever(Message m){
    System.out.println("Transfer reciever started");
    String response = null;
    Boolean valid_response = false;
    while(!valid_response){
      try{
        response = in.readLine();
        if(response.equals("y")){
          valid_response = true;
          System.out.println("1");
          clients.get(m.from).out.println("ACCEPTED-TRANSFER");

        }else if(response.equals("n")){
          clients.get(m.from).out.println("REJECTED-TRANSFER");
          return;
        }else{
          this.out.println("(y/n)?");
        }
      }catch(Exception e){
        e.printStackTrace();
      }
    }



  }

  private void listen(){
    try{
      String message;
      while((message = in.readLine()) != null){
        if(message.equals("QUIT")){
          clients.remove(this.name);
          socket.close();
          break;
        }
        Message m = new Message();
        m.content = message.toString();
        m.from = name;
        if(m.content.equals("START-TRANSFER-RECIEVER")){
          transfer_file_reciever(m);
        }
        if(m.content.matches(".*-send-file [a-zA-Z0-9/]+.*")){
          transfer_file_sender(m);
          continue;
        }
        this.m_consumer.enqueue_message(m);
      }
    }catch(Exception e){
      clients.remove(this.name);
      System.out.println(e.getMessage());
    }
    System.out.println(name + " has disconnected.");
  }

  public void run(){
    System.out.println(socket.getRemoteSocketAddress() + " has connected.");
    get_client_username();
    listen();
  }
}
