import java.io.*;
import java.util.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.Queue;
import java.net.*;


class Client_Handler implements Runnable {
    Socket socket;
    BufferedReader read;
    PrintWriter write;
    String username;

    Client_Handler(Socket socket) throws IOException {
        this.socket = socket;
        read = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        write = new PrintWriter(socket.getOutputStream(), true);
    }

    @Override
    public void run() {
        try {

            while (true) {
                username = read.readLine();
                if (username == null) {

                    return;
                }

                if (Main.usernameExists(username)) {
                    write.println("false");
                } else {
                    Main.addUsername(username);
                    write.println("true");
                    Main.broadcast(username + " joined!",true);
                    break;
                }
            }

            Main.update(this);
            Main.addClient(this);
            Main.clientMap.put(username, this);
            Main.broadcast("#user#a" + username,false);
            Main.sendHistory(this);


            String msg;
            while ((msg = read.readLine()) != null) {
                if(msg.charAt(0) == '@') {
                    String pattern = msg.substring(1).trim();
                    List<String> matched = Main.searchMessages(Main.messages,pattern);
                    for(String s : matched){
                        write.println("@"+s);
                    }
                    continue;
                }if(msg.charAt(0)=='$'){

                    int end = msg.indexOf(':');
                    String user =  msg.substring(1,end);
                    System.out.println(msg);
                    Client_Handler c = Main.clientMap.get(user);
                    c.send(msg);


                    continue;
                }
                String finalMsg = username + ": " + msg + "\n";
                Main.broadcast(finalMsg,true);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {
                socket.close();
            } catch (IOException ignored) {}
            Main.broadcast(username + " left!",true);
            Main.broadcast("#user#r" + username,false);
            Main.clientMap.remove(username);
            Main.removeClient(this);



        }
    }

    public void send(String msg) {
        write.println(msg);
    }
}

public class Main {
    static final Object LOCK = new Object();

    static ArrayList<Client_Handler> clients = new ArrayList<>();
    static ArrayList<String> User_names = new ArrayList<>();
    static Queue<String> messages = new LinkedList<>();
    static HashMap<String, Client_Handler> clientMap = new HashMap<>();


    public static List<String> searchMessages(Queue<String> messageQueue, String pattern) {
        synchronized (LOCK) {


            List<String> messages = new ArrayList<>(messageQueue);


            StringBuilder sb = new StringBuilder();
            List<Integer> posToMsgId = new ArrayList<>();
            for (int i = 0; i < messages.size(); i++) {
                String m = messages.get(i);
                for (char c : m.toCharArray()) posToMsgId.add(i);
                sb.append(m);
                posToMsgId.add(i);
                sb.append('|');
            }
            String text = sb.toString().toLowerCase();
            pattern = pattern.toLowerCase();


            int[] sa = buildSuffixArray(text);


            Set<Integer> foundMsgIds = new HashSet<>();
            int patternLen = pattern.length();
            for (int idx : sa) {
                if (idx + patternLen <= text.length() &&
                        text.substring(idx, idx + patternLen).equals(pattern)) {
                    foundMsgIds.add(posToMsgId.get(idx));
                }
            }


            List<String> result = new ArrayList<>();
            for (int id : foundMsgIds) {
                result.add(messages.get(id));
            }

            return result;

        }

    }


    private static int[] buildSuffixArray(String s) {
        int n = s.length();
        String[] suffixes = new String[n];
        Integer[] indices = new Integer[n];

        for (int i = 0; i < n; i++) {
            suffixes[i] = s.substring(i);
            indices[i] = i;
        }

        Arrays.sort(indices, (a, b) -> suffixes[a].compareTo(suffixes[b]));

        int[] sa = new int[n];
        for (int i = 0; i < n; i++) sa[i] = indices[i];

        return sa;
    }


    static void update(Client_Handler client) {
        synchronized (LOCK) {
            for(Client_Handler c : clients) {
                client.send("#user#a" + c.username);
            }
        }
    }
    static void addClient(Client_Handler ch) {
        synchronized (LOCK) {
            clients.add(ch);
        }
    }

    static boolean usernameExists(String name) {
        synchronized (LOCK) {
            return User_names.contains(name);
        }
    }

    static void addUsername(String name) {
        synchronized (LOCK) {
            User_names.add(name);
        }
    }

    static void sendHistory(Client_Handler ch) {
        synchronized (LOCK) {
            for (String msg : messages) {
                ch.send(msg);
            }
        }
    }

    static void removeClient(Client_Handler ch) {
        synchronized (LOCK) {
            clients.remove(ch);
            if (ch.username != null) {
                User_names.remove(ch.username);
            }
        }
    }

    static void broadcast(String msg,boolean check) {
        synchronized (LOCK) {
            if(check)
            messages.add(msg);
            for (Client_Handler c : clients) {
                c.send(msg);
            }
        }
    }

    static void start_Server() {
        try (ServerSocket serverSocket = new ServerSocket(5000)) {
            System.out.println("Server started on port 5000...");
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket);
                new Thread(new Client_Handler(clientSocket)).start();
            }
        } catch (Exception e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        start_Server();
    }
}
