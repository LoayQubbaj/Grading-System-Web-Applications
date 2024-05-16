package client;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Scanner;

public class Client
{

    public static void main(String[] args)
    {
        Scanner scanner = new Scanner( System.in );
        PrintWriter out;
        BufferedReader in;
        try
        {
            Socket socket = new Socket( "localhost" , 1234 );
            // write to server
            out = new PrintWriter( socket.getOutputStream() , true );

            // read from server
            in = new BufferedReader( new InputStreamReader( socket.getInputStream() ) );

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println(line);
                // Check if the end has been signaled
                if (line.contains("\0")) {
                    break;
                }
                // sending the user input
                if(line.contains("Answer:")) {
                    String userInput = scanner.nextLine();
                    out.println(userInput);
                    out.flush();
                }
            }
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally
        {
            scanner.close();
        }
    }
}