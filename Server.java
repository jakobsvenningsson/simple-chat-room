import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;

class Message{
    public String content = "";
    public String from = "";
    public String to = "";
    public Boolean server_message = false;
    public Boolean file_transfer_request = false;
    public String filename = "";
    public Boolean error = false;
    public Message(){}
    public Message(String content, String to){
      this.content = content;
      this.to = to;
    }
 };

 class Pair{
     public String reciever;
     public String filename;
     public Pair(String reciever, String filename){
       this.reciever = reciever;
       this.filename = filename;
     }
  };

public class Server{
  private ServerSocket server_socket;
  private TreeMap<String, ClientThread> clients;
  public Server(){

    clients = new TreeMap<String, ClientThread>();
    try{
      System.out.println(Inet4Address.getLocalHost().getHostAddress());

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
      else if(message.server_message){
        clients.get(message.to).out.println(message.content);
      }
      else if(message.file_transfer_request){
        clients.get(message.to).out.println(message.content);
      }
      // Check if client has specified a user
      else if(message.to.length()>0){
        if(clients.containsKey(message.to)){
          clients.get(message.to).out.println(message.content + " from " + message.from);
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
  private Boolean transfer_file = false;
  public LinkedList<Pair> file_requests;

  public synchronized void add_file_request(String from, String filename){
    file_requests.add(new Pair(from, filename));
  }

private synchronized void remove_file_request(String reciever, String filename){
    for(int i = 0; i < file_requests.size(); i++){
      if(file_requests.get(i).reciever.equals(reciever) && file_requests.get(i).filename.equals(filename)){
        file_requests.remove(i);
      }
    }
  }
  public ClientThread(Socket socket,TreeMap<String, ClientThread> clients, MessageConsumer consumer){
    try{
      this.clients = clients;
      this.socket = socket;
      this.out = new PrintStream(socket.getOutputStream());
      this.in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
      this.m_consumer = consumer;
      this.file_requests = new LinkedList<Pair>();

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

  private void transfer_file_sender(Message message){
    // Check if a reciever has been specified.
    if(message.to.length() == 0){
      m_consumer.enqueue_message(new Message("Please specify a reciever!", name));
      return;
    }
    if(!clients.containsKey(message.to)){
      m_consumer.enqueue_message(new Message("User not found!", name));
      return;
    }
    message.content = message.from + " wants to send the file " + message.filename + " to you, accept?(y/n)";
    clients.get(message.to).add_file_request(message.from, message.filename);
    m_consumer.enqueue_message(message);
  }

  private int find_index(String s){
    for(int i = 0; i < file_requests.size(); ++i){
      if(file_requests.get(i).reciever.equals(s)){
        return i;
      }
    }
    return -1;
  }

  private int end_index(String s, String flag){
    int end_index = s.indexOf(" ", s.indexOf(flag) + flag.length());
    end_index = end_index == -1 ? s.length() : end_index;
    return end_index;
  }


  private Message translate_message(String message){
    Message m = new Message();
    m.from = this.name;
    if(message.matches(".*-send-file [a-zA-Z0-9]+.*")){
      m.file_transfer_request = true;
      m.filename = message.substring(message.indexOf("-send-file ") + 11, end_index(message, "-send-file "));
    }
    if(message.matches(".*-to [a-zA-Z0-9]+.*")){
      m.to = message.substring(message.indexOf("-to ") + 4, end_index(message, "-to "));
    }
    if(message.matches(".*-m [a-zA-Z0-9]+.*")){
      m.content = message.substring(message.indexOf("-m ") + 3, end_index(message, "-m "));
    }
    return m;

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
        Message m = translate_message(message);
        if(m.file_transfer_request){
          transfer_file_sender(m);
          continue;
        }
        if(file_requests.size() > 0 && m.to.length() > 0){
          System.out.println(file_requests.size());
          System.out.println("1");
          int index;
          if((index = find_index(m.to)) != -1){
            Pair reciever = file_requests.get(index);
            if(reciever.reciever.equals(m.to)){
              System.out.println("6");
              if(m.content.charAt(0) == 'y'){
                System.out.println("3");
                  m.content = "APPROVED-TRANSFER:" + name + ";" + reciever.filename
                  +  " -to " + m.to;
                  remove_file_request(m.to, reciever.filename);
                  m_consumer.enqueue_message(m);
                  continue;
              }else if(m.content.charAt(0)== 'n'){
                m.content = "REJECTED-TRANSFER" + " -to " + m.to;
                m_consumer.enqueue_message(m);
                remove_file_request(m.to, reciever.filename);
                continue;
              }
            }
          }
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
    System.out.println(socket.getInetAddress().getHostAddress() + " has connected.");

    get_client_username();
    listen();
  }
}
