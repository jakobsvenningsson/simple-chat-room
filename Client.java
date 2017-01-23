import java.io.*;
import java.net.*;
import java.lang.*;
import java.util.*;


public class Client{
  private Socket socket;
  private BufferedReader in;
  public PrintStream out;
  private Boolean transfer_file = false;
  private String transfer_sender = null;

  public Client(){
    try{
      this.socket = new Socket("localhost", 5200);
      this.out = new PrintStream(socket.getOutputStream());
      this.in = new BufferedReader(new InputStreamReader(System.in));
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
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

  private void start_file_transfer_sender(String message){

  }

  public void start_file_transfer_reciever(String sender){
    System.out.println("reciever ready");
    transfer_file = true;
    transfer_sender = sender;
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
        if(message.matches(".*-send-file [a-zA-Z0-9/]+.*")){
          start_file_transfer_sender(message);
        }
        if(transfer_file) message = message + " -to " + transfer_sender;
        out.println(message);
        System.out.print("> ");
      }
      System.out.println("outside for");

    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }

  public static void main(String[] args) throws Exception{
    Client client = new Client();
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
        if(message.matches("START-TRANSFER-RECIEVER")){
          //String sender = message.substring(message.indexOf(":")+1, message.length());
          //client.start_file_transfer_reciever(sender);
          client.out.println("START-TRANSFER-RECIEVER");
          continue;
        }
        if(message.matches("ACCEPTED-TRANSFER")){
          System.out.println("A");
          client.out.println("ACCEPTED-TRANSFER");
          continue;
        }
        if(message.matches("REJECTED-TRANSFER")){
          client.out.println("REJECTED-TRANSFER");
          continue;
        }

        System.out.println(message);
      }
    }catch(Exception e){
      System.out.println(e.getMessage());
      System.exit(-1);
    }
  }
}
