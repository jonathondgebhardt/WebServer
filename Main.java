package Task1;

public class Main
{

    public static void main(String[] args)
    {

        // Use IP 127.0.0.1, port 1234
        Client c = new Client("127.0.0.1", 1234);
        WebServer w = new WebServer(1234);

    }
}


