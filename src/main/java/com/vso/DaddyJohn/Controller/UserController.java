    package com.vso.DaddyJohn.Controller;

    import com.vso.DaddyJohn.Entity.Users;
    import com.vso.DaddyJohn.Service.UserService;
    import org.springframework.web.bind.annotation.*;

    import java.util.List;

    @RestController
    @RequestMapping("/User")
    @CrossOrigin(origins = "*", maxAge = 3600)
    public class UserController {
        private final UserService userService;

        public UserController(UserService userService){
            this.userService = userService;
        }

        @PostMapping("/signUp")
        public Users addUser(@RequestBody Users users){
            return userService.addUser(users);
        }

        @GetMapping("/getUsers")
        public List<Users> getUsers(){
            return userService.getAllUser();
        }


    }
