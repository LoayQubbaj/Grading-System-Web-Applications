package server;

import database.Database;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.io.InputStreamReader;
import java.util.Scanner;

public class Server {
    public static void main(String[] args) {
        ServerSocket server = null;
        try {
            server = new ServerSocket(1234);
            server.setReuseAddress(true);

            while (true) {
                // socket object to receive incoming client
                // requests
                System.out.println("Waiting for client request ...\n");
                Socket client = server.accept();
                System.out.println("New client connected " + client.getInetAddress().getHostAddress() + "\n");
                ClientHandler clientSock = new ClientHandler(client);
                new Thread(clientSock).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (server != null) {
                try {
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static class ClientHandler implements Runnable {
        private final Socket clientSocket;
        private String user_email;
        private String user_password;
        private String user_name;
        private ArrayList<String> courses;
        PrintWriter out = null;
        BufferedReader in = null;
        String roleChoice;

        // Constructor
        public ClientHandler(Socket socket) {
            this.clientSocket = socket;
        }

        public void run() {
            try {
                out = new PrintWriter(clientSocket.getOutputStream(), true);
                in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

                while (true) {
                    // Ask the user for their role (admin, instructor, student)
                    out.println("Choose your role:");
                    out.println("1. Admin");
                    out.println("2. Instructor");
                    out.println("3. Student");
                    // Read user's role choice
                    out.println("Answer:");
                    roleChoice = in.readLine();
                    System.out.println("Received role choice from client: " + roleChoice);
                    out.println("User chose: " + roleChoice);

                    switch (roleChoice) {
                        case "1":
                            adminSignIn();
                            serveAdmin();
                            break;
                        case "2":
                            instructorSignIn();
                            serveInstructor();
                            break;
                        case "3":
                            studentSignIn();
                            serveStudent();
                            break;
                        default:
                            out.println("Invalid role choice");
                    }


                    out.println("Do you want to select another role? (yes/no)");
                    out.println("Answer:");
                    String continueChoice = in.readLine().toLowerCase();
                    if (!continueChoice.equals("yes")) {
                        break;
                    }
                }

                out.println("\0");  // Signal the end of communication
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (out != null) {
                        out.close();
                    }
                    if (in != null) {
                        in.close();
                        clientSocket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }


        private void adminSignIn() {
            boolean is_Admin = false;
            while (!is_Admin) {
                checkEmailAndPass();
                is_Admin = Database.isAdmin(user_email, user_password);
                if (!is_Admin) {
                    out.println("Invalid email or password");

                } else {
                    user_name = Database.getAdminName(user_email);
                    out.println("Successfully signed in as admin \nWelcome " + user_name);
                    System.out.println("An admin has successfully signed in \nEmail : " + user_email + "\nName : " + user_name + "\n");
                }
            }

        }

        private void serveAdmin() throws IOException {
            Scanner scanner = new Scanner(clientSocket.getInputStream());

            while (true) {
                out.println("Select an option :");
                out.println("1. Add new instructor.");
                out.println("2. Add new student.");
                out.println("3. Exit");
                out.println("Answer:");

                String answer = scanner.nextLine();

                if (answer.equals("1")) {
                    // Add new instructor
                    out.println("Enter instructor name:");
                    out.println("Answer:");
                    String instructorName = scanner.nextLine();
                    out.println("Enter instructor email:");
                    out.println("Answer:");
                    String instructorEmail = scanner.nextLine();
                    out.println("Enter instructor password:");
                    out.println("Answer:");
                    String instructorPassword = scanner.nextLine();

                    // Hash the password
                    String hashedInstructorPassword = hashPassword(instructorPassword);

                    if (Database.addNewInstructor(instructorName, instructorEmail, hashedInstructorPassword)) {
                        out.println("Instructor added successfully!");
                    } else {
                        out.println("Failed to add instructor.");
                    }

                } else if (answer.equals("2")) {
                    // Add new student
                    out.println("Enter student name:");
                    out.println("Answer:");
                    String studentName = scanner.nextLine();
                    out.println("Enter student email:");
                    out.println("Answer:");
                    String studentEmail = scanner.nextLine();
                    out.println("Enter student password:");
                    out.println("Answer:");
                    String studentPassword = scanner.nextLine();

                    // Hash the password
                    String hashedStudentPassword = hashPassword(studentPassword);

                    if (Database.addNewStudent(studentName, studentEmail, hashedStudentPassword)) {
                        out.println("Student added successfully!");
                    } else {
                        out.println("Failed to add student.");
                    }

                } else if (answer.equals("3")) {
                    out.println("Exiting admin service.");
                    break;
                } else {
                    out.println("Invalid option, please select 1, 2, or 3");
                }
            }


        }

        private void instructorSignIn() {
            boolean is_Instructor = false;
            while (!is_Instructor) {
                checkEmailAndPass();
                is_Instructor = Database.isInstructor(user_email, user_password);
                if (!is_Instructor) {
                    out.println("Invalid email or password");

                } else {
                    user_name = Database.getInstructorName(user_email);
                    out.println("Successfully signed in as instructor \nWelcome " + user_name);
                    System.out.println("An instructor has successfully signed in \nEmail : " + user_email + "\nName : " + user_name + "\n");
                }
            }
        }


        private void serveInstructor() {
            try {
                while (true) {
                    out.println("Select an option:");
                    out.println("1. Add marks for a student.");
                    out.println("2. Exit");
                    out.println("Answer:");
                    String answer = in.readLine();

                    if (answer.equals("1")) {
                        addMarksForStudent();
                    } else if (answer.equals("2")) {
                        out.println("Exiting instructor service.");
                        break;
                    } else {
                        out.println("Invalid option, please select 1 or 2");
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private void addMarksForStudent() throws IOException {
            try {
                out.println("Enter student ID:");
                out.println("Answer:");
                String studentIDStr = in.readLine();

                // Check if the entered student ID is valid
                try {
                    int studentID = Integer.parseInt(studentIDStr);
                    if (!Database.isStudentExists(studentID)) {
                        out.println("Invalid student ID.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    out.println("Invalid student ID. Please enter a numeric value.");
                    return;
                }

                out.println("Enter course ID:");
                out.println("Answer:");
                String courseIDStr = in.readLine();

                try {
                    int courseID = Integer.parseInt(courseIDStr);
                    if (!Database.isCourseExists(courseID)) {
                        out.println("Invalid course ID.");
                        return;
                    }
                } catch (NumberFormatException e) {
                    out.println("Invalid course ID. Please enter a numeric value.");
                    return;
                }

                out.println("Enter mark:");
                out.println("Answer:");
                String markStr = in.readLine();

                try {
                    int mark = Integer.parseInt(markStr);


                    if (mark < 0 || mark > 100) {
                        out.println("Invalid mark. Marks should be in the range 0-100.");
                        return;
                    }

                    // Add the mark to the database
                    if (Database.addMark(Integer.parseInt(studentIDStr), Integer.parseInt(courseIDStr), mark)) {
                        out.println("Mark added successfully!");
                    } else {
                        out.println("Failed to add mark.");
                    }
                } catch (NumberFormatException e) {
                    out.println("Invalid mark. Please enter a numeric value.");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }


        private void studentSignIn() {
            boolean is_Student=false;
            while (!is_Student) {
                checkEmailAndPass();
                is_Student = Database.isStudent(user_email, user_password);
                if (!is_Student) {
                    out.println("Invalid email or password");
                    return;
                } else {
                    user_name = Database.getStudentName(user_email);
                    out.println("Successfully signed in as admin \nWelcome " + user_name);
                    System.out.println("An admin has successfully signed in \nEmail : " + user_email + "\nName : " + user_name + "\n");
                }
            }

        }
        private void serveStudent()
        {
            try
            {
                do
                {
                    displayCourses();
                    out.println( "Choose a course or 'exit' to quit :" );
                    out.println("Answer:");
                    String course = in.readLine().toLowerCase();
                    if(course.equals( "exit" ))
                    {
                        break;
                    }
                    else if(!courses.contains( course ))
                    {
                        out.println( "Invalid course" );
                    }
                    else
                    {
                        printCourseInfo( course );
                    }
                }
                while(true);
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        private void displayCourses()
        {
            out.println( "Your courses are :" );
            courses = Database.getCourses( user_email );
            for(String c : Database.getCoursesAndMarks( user_email ))
            {
                out.println( c );
            }

            System.out.println( "Displaying courses for :\nStudent Email : " + user_email + "\n" + "Student Name " +
                    ":" + " " + user_name + "\n" );
        }
        private void printCourseInfo(String course)
        {

            StringBuilder sb = new StringBuilder();
            sb.append( "Your mark in " ).append( course ).append( " is " ).append( Database.getMark( user_email ,
                    course ) ).append( "\n" );
            sb.append( "Class Average is " ).append( Database.getAverageMark( course ) ).append( "\n" );
            sb.append( "Class Median is " ).append( Database.getMedianMark( course ) ).append( "\n" );
            sb.append( "Class Highest is " ).append( Database.getHighestMark( course ) ).append( "\n" );
            sb.append( "Class Lowest is " ).append( Database.getLowestMark( course ) ).append( "\n" );
            out.println( sb );
            System.out.println( "Printing course info for :\nCourse : " + course + "\nStudent Email : " + user_email + "\n" + "Student Name : " + user_name + "\n" );
        }
        public void checkEmailAndPass() {
            try {
                String line;
                do {
                    out.println("Enter your email:");
                    out.println("Answer:");
                    line = in.readLine();

                    if (line == null) {
                        out.println("Input is not valid");
                    } else {
                        user_email = line;
                    }
                } while (line == null || line.equals(""));

                do {
                    out.println("Enter your password:");
                    out.println("Answer:");
                    line = in.readLine();

                    if (line == null) {
                        out.println("Input is not valid");
                    } else {
                        // Hash the entered password using SHA-256
                        user_password = hashPassword(line);
                    }
                } while (line == null || line.equals(""));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        private String hashPassword(String password) {
            try {
                MessageDigest digest = MessageDigest.getInstance("SHA-256");
                byte[] hash = digest.digest(password.getBytes());

                // Convert the byte array to a hexadecimal string
                StringBuilder hexString = new StringBuilder();
                for (byte b : hash) {
                    String hex = Integer.toHexString(0xFF & b);
                    if (hex.length() == 1) {
                        hexString.append('0');
                    }
                    hexString.append(hex);
                }
                return hexString.toString();
            } catch (NoSuchAlgorithmException e) {
                e.printStackTrace();
                return null;
            }
        }

    }
}




