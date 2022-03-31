// Reference: https://cs.lmu.edu/~ray/notes/javanetexamples/
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.*;


public class Server {

    // All client names, so we can check for duplicates upon registration.
    private static Set<String> names = new HashSet<>();
    // Coordinator to designate
    private static Set<String> coordinators = new HashSet<>();

    // The set of all the print writers for all the clients, used for broadcast.
    private static Set<PrintWriter> writers = new HashSet<>();

    // Socket set
    private static HashMap<String, Socket> sockets = new HashMap<String, Socket>();

    // Port and Server Socket
    public static void main(String[] args) throws Exception {
        System.out.println(new Date(System.currentTimeMillis()) + " The chat server is running...");
        ExecutorService pool = Executors.newFixedThreadPool(500);
        try (ServerSocket listener = new ServerSocket(39001)) {
            while (true) {
                pool.execute(new Handler(listener.accept()));
            }
        }
    }
    // We need to have a coordinator as the member is connected.
    public static void findcoordinator() {
        // Server needs to have at least one member to be qualified as a coordinator
        if (!names.isEmpty() && coordinators.isEmpty()) {
            for (String i : names) {
                    coordinators.add(i);
                    for (PrintWriter writer : writers) {
                        // message to show member is coordinator
                        writer.println("MESSAGE " + i + " is now the coordinator");
                    }
                    System.out.println(i + " is now the coordinator");
                    break;
                }
            }
        }

    // Client's handler
    private static class Handler implements Runnable {
        // list of members
        private String name;

        private Socket socket;
        // scanner to allow receiving message between members
        private Scanner in;
        // PrintWriter to allow sending message between members
        private PrintWriter out;


        public Handler(Socket socket) {

            this.socket = socket;
        }

        public void run() {
            try {
                in = new Scanner(socket.getInputStream());
                out = new PrintWriter(socket.getOutputStream(), true);

                // Keep requesting an id until we get a unique one.
                while (true) {
                    out.println("SUBMIT NAME");
                    name = in.nextLine();
                    if (name == null) {
                        return;
                    }
                    synchronized (names) {//synchronize name because we cannot allow 2 users with same name in the same time to add it in names
                        if (!name.equals("") && !names.contains(name.toLowerCase().trim())) {
                            names.add(name.toLowerCase().trim());
                            sockets.put(name.toLowerCase().trim(), socket);
                            break;
                        } else if (names.contains(name.toLowerCase().trim())) {
                            out.println("ERROR User Already Exist");
                        }
                    }
                }

                writers.add(out);
                // Now that a successful name has been chosen, add the socket's print writer
                // to the set of all writers so this client can receive broadcast messages.
                // But BEFORE THAT, let everyone else know that the new person has joined!
                out.println("NAME ACCEPTED " + name);
                for (PrintWriter writer : writers) {
                    writer.println("MESSAGE " + name + " has joined.");
                    writer.println("MESSAGE " + "Welcome " + name + "!" + " if you want to leave type /quit ");
                }

                if (names.size() == 1) {
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + names.stream().findFirst().get() + " is the first one to join.");
                        // adding name to coordinator's list
                        for (String i : names) {
                            coordinators.add(i);
                        }
                    }
                    System.out.println(names.stream().findFirst().get() + " is the first one to join.");
                }

                // The first member connected is the coordinator, and can check members connected by typing command /members.
                if  (names.size() == 1) {
                    coordinators.add(names.stream().findFirst().get());
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + names.stream().findFirst().get() + " became the coordinator. If you want to see who is connected type /members ");
                    }

                }
                findcoordinator();
                // Accept messages from this client and broadcast them.
                while (true) {
                    // The server will attempt to nominate a coordinator at all times
                    findcoordinator();
                    // Accept messages from this client and broadcast them.
                    while (true) {
                        String input = in.nextLine();
                        if (input.toLowerCase().startsWith("/quit")) {
                            return;
                        }
                        for (PrintWriter writer : writers) {
                            writer.println("MESSAGE " + name + ": " + input);
                        }
                        // list of members connected.
                        if (input.toLowerCase().startsWith("/members") && coordinators.contains(name)) {
                            for (PrintWriter writer : writers) {
                                writer.println("MESSAGE " + "System: " + names);
                            }
                        } else if (input.toLowerCase().startsWith("/members") && !coordinators.contains(name)) {
                                for (PrintWriter writer : writers) {
                                    writer.println("MESSAGE " + "System: " + "Only coordinators can do that");
                                }
                            }
                        }
                    }
            } catch (Exception e)  {
                // System.out.println(e);
            }  finally {
                if (out != null) {
                    writers.remove(out);
                }
                if (name != null) {
                    System.out.println(name + " is leaving.");
                    names.remove(name);
                    for (PrintWriter writer : writers) {
                        writer.println("MESSAGE " + name + " has left. :(");
                    }
                    coordinators.remove(name);


                }
                try { socket.close(); } catch (IOException e) {}

            }

        }
    }
}
