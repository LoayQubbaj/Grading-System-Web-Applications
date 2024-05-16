package springmvcapp;

import app.services.DataRetrievingService;
import app.services.LoginService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.SessionAttributes;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Controller
@SessionAttributes ({ "studentID" , "name" , "email" , "courses" })
public class LoginController
{
    @RequestMapping (value = "/login.springmvc", method = RequestMethod.GET)
    public String showLoginPage()
    {
        return "login";
    }
    
    @RequestMapping (value = "/login.springmvc", method = RequestMethod.POST)
    public String handleLoginRequest(ModelMap model , @RequestParam String email , @RequestParam String password)
    {
        //used to hash the password before sending it to the database.
        String student_password = hashPassword(password);
        boolean is_valid_user = LoginService.validateUser( email , student_password );
        
        if(is_valid_user)
        {
            model.put( "email" , email );
            //model.addAttribute("email", email);
            model.put( "name" , DataRetrievingService.getStudentName( email ) );
            model.put( "courses" , DataRetrievingService.getCourses( email ) );
            model.put( "studentID" , DataRetrievingService.getStudentID( email ) );
            model.put( "password" , password );
            
            return "welcome";
        }
        else
        {
            model.put( "errorMessage" , "Invalid Credentials !!" );
            
            return "login";
        }
    }
    //The hashing method used to secure the passwords
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
