package service;

import db.DataBase;
import model.User;

import java.util.Map;

public class UserService {

    public void register(Map<String, String> userInfo) {
        String id = userInfo.get("userId");
        User user = new User(id,
                userInfo.get("password"),
                userInfo.get("name"),
                userInfo.get("email"));

        DataBase.addUser(user);
    }

    public boolean isLoginSuccessful(Map<String, String> loginInfo) {
        String id = loginInfo.get("userId");
        String password = loginInfo.get("password");

        User user = DataBase.findUserById(id);
        return user != null && user.getPassword().equals(password);
    }

    public boolean isLogined(Map<String, String> cookies) {
        return "true".equals(cookies.get("logined"));
    }
}
