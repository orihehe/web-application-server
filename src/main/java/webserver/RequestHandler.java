package webserver;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import service.UserService;
import util.HttpRequestUtils;
import util.IOUtils;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class RequestHandler extends Thread {
    private static final Logger log = LoggerFactory.getLogger(RequestHandler.class);

    private Socket connection;
    private UserService userService;

    public RequestHandler(Socket connectionSocket) {
        this.connection = connectionSocket;
        this.userService = new UserService();
    }

    public void run() {
        log.debug("New Client Connect! Connected IP : {}, Port : {}", connection.getInetAddress(),
                connection.getPort());

        try (InputStream in = connection.getInputStream(); OutputStream out = connection.getOutputStream()) {
            // TODO 사용자 요청에 대한 처리는 이 곳에 구현하면 된다.

            BufferedReader br = new BufferedReader(new InputStreamReader(in, "UTF-8"));
            String firstLine = br.readLine();
            Map<String, String> header = HttpRequestUtils.parseHeader(br);
            String body = IOUtils.readData(br, Integer.parseInt(Optional.ofNullable(header.get("Content-Length")).orElse("0")));

            String urlPath = firstLine.split(" ")[1];

            DataOutputStream dos = new DataOutputStream(out);

            byte[] responseBody = new byte[0];
            if ("/".equals(urlPath)) {
                responseBody = "Hello World".getBytes();
                response200Header(dos, responseBody.length);
            } else if ("/user/create".equals(urlPath)) {
                userService.register(HttpRequestUtils.parseQueryString(body));
                responseBody = Files.readAllBytes(new File("./webapp/index.html").toPath());
                response302Header(dos, "/index.html");
            } else if ("/user/login".equals(urlPath)) {
                if (userService.isLoginSuccessful(HttpRequestUtils.parseQueryString(body))) {
//                    responseBody = Files.readAllBytes(new File("./webapp/index.html").toPath());
                    response302Header(dos, "/index.html");
                    setCookieAtHeader(dos, new HashMap() {{
                        put("logined", "true");
                    }});
                } else {
//                    responseBody = Files.readAllBytes(new File("./webapp/user/login_failed.html").toPath());
                    response302Header(dos,"/user/login_failed.html");
                    setCookieAtHeader(dos, new HashMap() {{
                        put("logined", "false");
                    }});
                }
            } else if ("/user/list.html".equals(urlPath)) {
                if (!userService.isLogined(HttpRequestUtils.parseCookies(header.get("Cookie")))) {
                    response302Header(dos, "/user/login.html");
                } else {
                    responseBody = Files.readAllBytes(new File("./webapp/user/list.html").toPath());
                    response200Header(dos, responseBody.length);
                }
            } else {
                responseBody = Files.readAllBytes(new File("./webapp" + urlPath).toPath());
                response200Header(dos, responseBody.length);
            }


            setContentTypeAtHeader(dos, urlPath);
//            response200Header(dos, responseBody.length);
            responseBody(dos, responseBody);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

    private void response200Header(DataOutputStream dos, int lengthOfBodyContent) {
        try {
            dos.writeBytes("HTTP/1.1 200 OK \r\n");
            dos.writeBytes("Content-Length: " + lengthOfBodyContent + "\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void response302Header(DataOutputStream dos, String location) {
        try {
            dos.writeBytes("HTTP/1.1 302 Found \r\n");
            dos.writeBytes("Location: " + location + "\r\n");
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void setContentTypeAtHeader(DataOutputStream dos, String urlPath) {
        try {
            if (StringUtils.endsWith(urlPath, ".css")) {
                dos.writeBytes("Content-Type: text/css\r\n");
            } else {
                dos.writeBytes("Content-Type: text/html;charset=utf-8\r\n");
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void setCookieAtHeader(DataOutputStream dos, Map<String, String> cookies) {
        try {
            for (Map.Entry<String, String> entry : cookies.entrySet()) {
                dos.writeBytes(String.format("Set-Cookie: %s=%s\r\n", entry.getKey(), entry.getValue()));
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }

    private void responseBody(DataOutputStream dos, byte[] body) {
        try {
            dos.writeBytes("\r\n");
            dos.write(body, 0, body.length);
            dos.flush();
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
