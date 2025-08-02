package com.example.pain_ting;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.HashMap;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class Controller {
    private final UserService userService;
    private final HashMap<String, Long> activeSessions = new HashMap<>();
    private final UserRepository userRepository;

    private HttpHeaders genHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Access-Control-Allow-Origin", "http://localhost:5173");
        headers.add("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE");
        headers.add("Access-Control-Allow-Headers", "Set-Cookie, Content-Type, Authorization");
        headers.add("Access-Control-Allow-Credentials", "true");
        return headers;
    }

    @Autowired
    public Controller(UserService userService, UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }
    @GetMapping("/painting")
    public ResponseEntity<String> getUserPainting(@CookieValue("sessionId") String sessionId) {
        System.out.println("Sid " + sessionId);
        if(!activeSessions.containsKey(sessionId))
            return new ResponseEntity<>(genHeaders(), HttpStatus.UNAUTHORIZED);
        var userid =  activeSessions.get(sessionId);
        var user = userRepository.findById(userid).orElse(null);
        return new ResponseEntity<>(user.getPainting(), genHeaders(), HttpStatus.OK);
    }

    @PostMapping("/painting")
    public ResponseEntity<String> setUserPainting(@CookieValue("sessionId") String sessionId , @RequestBody String painting) {
        System.out.println("Sid " + sessionId);
        if(!activeSessions.containsKey(sessionId))
            return new ResponseEntity<>(genHeaders(), HttpStatus.UNAUTHORIZED);
        System.out.println("hello " + painting);
        var userid =  activeSessions.get(sessionId);
        var user = userRepository.findById(userid).orElse(null);
        user.setPainting(painting);
        userRepository.save(user);
        return new ResponseEntity<>(genHeaders(), HttpStatus.OK);
    }

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestParam String username, @RequestParam String password) {
        // TODO
        var user = userRepository.findByUsername(username);
        if(user.isEmpty()) {
            return new ResponseEntity<>(genHeaders(), HttpStatus.NOT_FOUND);
        }
        if(!user.get().getPassword().equals(password)) {
            return new ResponseEntity<>(genHeaders(), HttpStatus.NOT_FOUND);
        }
        var uuid = UUID.randomUUID().toString();
        var headers = genHeaders();
        activeSessions.put(uuid, user.get().getId());
        ResponseCookie cookie = ResponseCookie.from("sessionId", uuid)
                .httpOnly(true)
                .secure(true) // Use in HTTPS
                .path("/")
                .maxAge(10)
                .sameSite("Lax")
                .build();
        headers.add("Set-Cookie", cookie.toString());
        System.out.println(headers);
        return new ResponseEntity<>("{\"resp\":\"Success\"}" , headers, HttpStatus.OK);
    }
    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestParam String username, @RequestParam String password) {
        var user = userRepository.findByUsername(username);
        if(!user.isEmpty()) {
            return new ResponseEntity<>("{\"resp\":\"Occupied\"}" , genHeaders(), HttpStatus.NOT_ACCEPTABLE);
        }
        var newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setPainting("{\"name\":\"Untitled\",\"shapes\":[]}");
        userRepository.save(newUser);
        user = userRepository.findByUsername(username);

        var uuid = UUID.randomUUID().toString();
        var headers = genHeaders();
        activeSessions.put(uuid, user.get().getId());
        ResponseCookie cookie = ResponseCookie.from("sessionId", uuid)
                .httpOnly(true)
                .secure(true) // Use in HTTPS
                .path("/")
                .maxAge(10)
                .sameSite("Lax")
                .build();
        headers.add("Set-Cookie", cookie.toString());
        return new ResponseEntity<>("{\"resp\":\"Success\"}", headers, HttpStatus.OK);
    }
}
