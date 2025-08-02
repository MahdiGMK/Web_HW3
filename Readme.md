<div dir="rtl">

# تمرین سوم درس طراحی وب

مهدی بهرامیان
401171593

## مراحل پیاده سازی برنامه مطرح شده

برای پیاده سازی این بکند کار را با راهنمای "شروع سریع" اسپرینگ شروع میکنیم.
پس از آن دیتابیس را تنظیم میکنیم :

</div>

pom.xml -> dependencies :

```xml
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-jdbc</artifactId>
</dependency>
<dependency>
	<groupId>org.postgresql</groupId>
	<artifactId>postgresql</artifactId>
	<scope>runtime</scope>
</dependency>
<dependency>
	<groupId>org.projectlombok</groupId>
	<artifactId>lombok</artifactId>
	<optional>true</optional>
</dependency>
<dependency>
	<groupId>org.springframework.boot</groupId>
	<artifactId>spring-boot-starter-test</artifactId>
	<scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>
```

<div dir="rtl">
    به این شکل jpa و lombok و jdbc و postgresql را نصب میکنیم.
</div>

src/main/resources/application.properties

```properties
spring.application.name=pain-ting
spring.datasource.url=jdbc:postgresql://localhost:5432/painting
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.datasource.driver-class-name=org.postgresql.Driver

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true
spring.jpa.properties.hibernate.dialect=org.hibernate.dialect.PostgreSQLDialect
```

<div dir="rtl">

و به این شکل پایگاه داده مورد استفاده توسط jpa را به postgresql درحال اجرا روی کامپیوتر خودمان تنظیم میکنیم.

حال با توجه به این که برای کل این پروژه صرفا به یک جدول برای نگهداری کاربران و نقاشی های آنها داریم،
کافیست Entity و Repository و Service را برای کاربر بسازیم که کد مربوطه را در زیر مشاهده میکنید :

</div>

```java
@Data
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String username;
    @Column(nullable = false)
    private String password;
    @Column()
    private String painting;
}
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String name);
}

@Service
public class UserService {

    private final UserRepository userRepository;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Optional<User> findByUsername(String name){
         return userRepository.findByUsername(name);
    }
    public User updateImageJson(Long userId, String painting_json) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPainting(painting_json);
        return userRepository.save(user);
    }
}
```

<div dir="rtl">
    و در نهایت کافیست در یک Controller
    ۴ تابع Rest درست کنیم که اعمال
    login, register, getPainting, setPainting
    را با توجه به کاربران انجام دهیم :

</div>

Controller.java

```java
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
```

<div dir="rtl">
    این کنترلر مسئول انجام کلیه اعمال است و از یک تابع کمکی genHeaders برای تولید هدر های مربوط به CORS و
    1 تابع Autowired برای تنظیم اولیه و ۴ تابع Rest تشکیل شده است :

- POST /api/register :
  مکانیزم کلی این تابع به این شکل است که وجود کاربر با نام مشابه را چک میکند و اگر نبود در این صورت یک کاربر جدید با این نام و رمز عبور
  میسازد و نقاشی این کاربر را نیز به یک نقاشی پیش فرض تعیین میکند. در نهایت برای کاربر یک Session میسازد که ۱۰ ثانیه اعتبار دارد
  این زمان کم به علت کمک در هنگام توسعه تخصیص داده شده است.
- POST /api/login :
  این تابع نیز مشابه تابع ثبت نام است با این تفاوت که در این تابع فقط بخش احراز هویت و تخصیص Session انجام میشود.
- POST /api/painting :
  این تابع از روی بدنه درخواست، یک رشته میگیرد و آنرا درون فیلد painting جدول قرار میدهد.
  توجه کنید که این تابع کاربر را از روی شماره "نشست" شناسایی میکند.
- GET /api/painting :
  این تابع نیز مشابه تابع قبل است با این تفاوت که فیلد painting جدول را خوانده و به کاربر میفرستد.

  و در نهایت، برای اینکه از این بکند بتوانیم استفاده کنیم، کافیست قطعه کد زیر را به Topbar در فرانت اضافه کنیم که قابلیت های جدید
  مانند ورود و ثبت نام و ارسال و دریافت نقشه را پیاده کنیم :

</div>

```tsx
//...
<button
  onClick={async () => {
    const obj = { name: nm, shapes: shapes };
    const json = JSON.stringify(obj);
    await fetch(`http://localhost:8080/api/painting`, {
      method: "POST",
      credentials: "include",
      body: json,
    });
  }}
>
  ToCloud
</button>
<button
  onClick={async () => {
    const response = await fetch(`http://localhost:8080/api/painting`, {
      method: "GET",
      credentials: "include",
    });
    const obj = await response.json();
    setNm(obj.name);
    setShapes(obj.shapes);
  }}
>
  FromCloud
</button>
<form
  style={{
    display: "inline-flex",
    gap: "8px",
    marginLeft: "16px",
    alignItems: "center",
  }}
  onSubmit={async (e) => {
    e.preventDefault();
    const user = usernameRef.current.value;
    const pass = passwordRef.current.value;
    fetch(
      `http://localhost:8080/api/login?username=${user}&password=${pass}`,
      {
        method: "POST",
        credentials: "include",
      },
    ).then((response) => {
      console.log("OK? " + response.ok);
    });
  }}
>
  <input
    ref={usernameRef}
    type="text"
    placeholder="Username"
    style={{
      padding: "4px 8px",
      borderRadius: "4px",
      border: "1px solid #ccc",
    }}
  />
  <input
    ref={passwordRef}
    type="password"
    placeholder="Password"
    style={{
      padding: "4px 8px",
      borderRadius: "4px",
      border: "1px solid #ccc",
    }}
  />
  <button type="submit">Login</button>
  <button
    onClick={async (e) => {
      e.preventDefault();
      const user = usernameRef.current.value;
      const pass = passwordRef.current.value;
      fetch(
        `http://localhost:8080/api/register?username=${user}&password=${pass}`,
        {
          method: "POST",
          credentials: "include",
        },
      ).then((response) => {
        console.log("OK? " + response.ok);
      });
    }}
  >
    Register
  </button>
</form>
// ...
```

<div dir="rtl">
  این قطعه کد عمکرد ویژه ای ندارد و صرفا با استفاده از دستور fetch توابع بکند متناظر با عملکرد خود را صدا میزند.
</div>
