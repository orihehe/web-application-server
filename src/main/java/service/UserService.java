package service;

import model.User;

import java.util.HashMap;
import java.util.Map;

public class UserService {

    private Map<String, User> users = new HashMap<>();

    public void register(Map<String, String> userInfo) {
        String id = userInfo.get("userId");
        User user = new User(id,
                userInfo.get("password"),
                userInfo.get("name"),
                userInfo.get("email"));

        System.out.println(user.toString());
        users.put(id, user);
    }
}
