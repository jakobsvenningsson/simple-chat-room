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
  private TreeMap<String, PrintStream> clients;
  public Server(){
    clients = new TreeMap<String, PrintStream>();
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
  private TreeMap<String, PrintStream> clients;
  private LinkedList<Message> messageQue;

  public MessageConsumer(TreeMap<String, PrintStream> clients){
    this.clients = clients;
    this.messageQue = new LinkedList<Message>();
  }

  private void print_users(String from){
    clients.get(from).println("Connected users:");
    for(String user : clients.keySet()){
      clients.get(from).println(user);
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
      }
      // Check if client has specified a user
      if(message.content.matches(".+-to.+")){
        int index = message.content.indexOf("-to");
        System.out.println(message.content.substring(index+3, message.content.length()));
        String reciever = message.content.substring(index+3, message.content.length()).trim();
        if(clients.containsKey(reciever)){
          clients.get(reciever).println(message.content.substring(0,index) + " from " + message.from);
        }else{
          clients.get(message.from).println("User not found!");
        }
      // No user specified -> send to all clients.
      }else{
        for(String key : clients.keySet()){
          clients.get(key).println(message.content + " from " + message.from);
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
  private PrintStream out;
  private BufferedReader in;
  private TreeMap<String, PrintStream> clients;
  private String name;
  private MessageConsumer m_consumer;

  public ClientThread(Socket socket,TreeMap<String, PrintStream> clients, MessageConsumer consumer){
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
      clients.put(this.name, out);
    }catch(Exception e){
      System.out.println(e.getMessage());
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
        this.m_consumer.enqueue_message(m);
      }
    }catch(Exception e){
      System.out.println("hej");
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
