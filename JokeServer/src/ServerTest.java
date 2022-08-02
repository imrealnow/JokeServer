import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.Test;

public class ServerTest {

    @Test
    public void serverCanStart() throws Exception {
        try {
            JokeServer server = new JokeServer();
            server.start("localhost", 4444);
            return;
        } catch (Exception e) {
            fail();
        }
    }

    @Test
    public void clientCanConnect() throws Exception {
        try {
            Runnable serverThread = () -> {
                JokeServer server = new JokeServer();
                try {
                    server.start("localhost", 4444);
                    server.listenForClients();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Server failed to start");
                }
            };
            Thread thread = new Thread(serverThread);
            thread.start();
            JokeClient client = new JokeClient();
            client.startConnection("localhost", 4444);
            String response = client.readMessage();
            assertEquals("[Client: 0] has connected to Joke Server[0.0.0.0:4444]\nDo you want to hear a joke? (Y/N)",
                    response);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void multipleClientsCanConnect() throws Exception {
        try {
            Runnable serverThread = () -> {
                JokeServer server = new JokeServer();
                try {
                    server.start("localhost", 4444);
                    server.listenForClients();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Server failed to start");
                }
            };
            Thread thread = new Thread(serverThread);
            thread.start();
            JokeClient client1 = new JokeClient();
            JokeClient client2 = new JokeClient();
            JokeClient client3 = new JokeClient();
            client1.startConnection("localhost", 4444);
            client2.startConnection("localhost", 4444);
            client3.startConnection("localhost", 4444);
            String response1 = client1.readMessage();
            assertEquals("[Client: 0] has connected to Joke Server[0.0.0.0:4444]\nDo you want to hear a joke? (Y/N)",
                    response1);
            String response2 = client2.readMessage();
            assertEquals("[Client: 1] has connected to Joke Server[0.0.0.0:4444]\nDo you want to hear a joke? (Y/N)",
                    response2);
            String response3 = client3.readMessage();
            assertEquals("[Client: 2] has connected to Joke Server[0.0.0.0:4444]\nDo you want to hear a joke? (Y/N)",
                    response3);
        } catch (Exception e) {
            fail(e);
        }
    }

    @Test
    public void serverSendsJoke() throws Exception {
        try {
            Runnable serverThread = () -> {
                JokeServer server = new JokeServer();
                try {
                    server.start("localhost", 4444);
                    server.listenForClients();
                } catch (Exception e) {
                    e.printStackTrace();
                    fail("Server failed to start");
                }
            };
            Thread thread = new Thread(serverThread);
            thread.start();
            JokeClient client = new JokeClient();
            client.startConnection("localhost", 4444);
            client.readMessage();
            String response = client.sendMessage("Y");
            assertTrue(isValidJoke(response), response);
        } catch (Exception e) {
            fail(e);
        }
    }

    private boolean isValidJoke(String joke) {
        for (String setJoke : JokeServer.JOKE_STRINGS) {
            if (joke.contains(setJoke)) {
                return true;
            }
        }
        return false;
    }
}
