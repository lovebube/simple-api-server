package com.nexon.apiserver.dao;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017-02-04.
 */
public class Dao {
    private final SimpleSqliteTemplate jdbcTemplate;

    public Dao() {
        this.jdbcTemplate = new SimpleSqliteTemplate();
        dropUsersTable();
        dropChatroomTable();
        dropChatroomSnapShotTable();
        dropChatTable();
        createUsersTable();
        createChatroomTable();
        createChatroomSnapShotTable();
        createChatTable();
    }


    private void dropChatTable() {
        jdbcTemplate.executeUpdate(jdbcTemplate.preparedStatement("DROP TABLE messages;"));
    }

    private void dropChatroomSnapShotTable() {
        jdbcTemplate.executeUpdate(jdbcTemplate.preparedStatement("DROP TABLE chatroomssnapshot;"));
    }

    private void dropChatroomTable() {
        jdbcTemplate.executeUpdate(jdbcTemplate.preparedStatement("DROP TABLE chatrooms;"));
    }

    public void dropUsersTable() {
        jdbcTemplate.executeUpdate(jdbcTemplate.preparedStatement("DROP TABLE users"));
    }
        
    private void createChatTable() {
        String query = "CREATE TABLE messages (messageid INTEGER PRIMARY KEY, " +
                "chatroomid INTEGER," +
                "senderid INTEGER," +
                "receiverid INTEGER," +
                "messagebody VARCHAR(100) not NULL);";
        jdbcTemplate.executeUpdate(jdbcTemplate.preparedStatement(query));
    }

    private void createChatroomSnapShotTable() {
        jdbcTemplate.executeUpdate(jdbcTemplate.preparedStatement("CREATE TABLE chatroomssnapshot " +
                "(userid INTEGER," +
                "chatroomid INTEGER);"));
    }

    public void createUsersTable() {
        jdbcTemplate.executeUpdate(jdbcTemplate.preparedStatement("CREATE TABLE users " +
                "(userid INTEGER PRIMARY KEY," +
                "nickname VARACHAR(20) not NULL);"));
    }

    public void createChatroomTable() {
        jdbcTemplate.executeUpdate(jdbcTemplate.preparedStatement("CREATE TABLE chatrooms " +
                "(chatroomid INTEGER PRIMARY KEY AUTOINCREMENT," +
                "userid INTEGER," +
                "chatroomname VARCHAR(100) not NULL);"));
    }

    public User addUser(String nickname) {
        if (getUser(nickname).getUserid() != 0) {
            System.out.println("Nickname exists!");
            return null;
        }

        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("INSERT INTO users " +
                "(nickname) values (?);");
        try {
            preparedStatement.setString(1, nickname);
            jdbcTemplate.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        preparedStatement = jdbcTemplate.preparedStatement("SELECT userid FROM users WHERE nickname=?;");
        try {
            preparedStatement.setString(1, nickname);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        User user = (User) jdbcTemplate.executeQuery(preparedStatement, SimpleSqliteTemplate.USER);
        user.setNickname(nickname);
        return user;
    }

    public void joinChatroom(int userid, int chatroomid) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("INSERT INTO chatroomssnapshot " +
                "(userid, chatroomid) values(?, ?);");
        try {
            preparedStatement.setInt(1, userid);
            preparedStatement.setInt(2, chatroomid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        jdbcTemplate.executeUpdate(preparedStatement);

    }

    public User getUser(String nickname) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("SELECT userid FROM users WHERE nickname=?;");
        try {
            preparedStatement.setString(1, nickname);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        User user = (User) jdbcTemplate.executeQuery(preparedStatement, SimpleSqliteTemplate.USER);
        user.setNickname(nickname);
        return user;
    }
    
    public Message postMessage(int senderid, int receiverid, int chatroomid, String messageBody) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("INSERT INTO messages " +
                "(senderid, receiverid, chatroomid, messagebody) values(?, ?, ?, ?)");
        try {
            preparedStatement.setInt(1, senderid);
            preparedStatement.setInt(2, receiverid);
            preparedStatement.setInt(3, chatroomid);
            preparedStatement.setString(4, messageBody);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        int messageid = jdbcTemplate.executeUpdate(preparedStatement);
        
        System.out.println(messageid);
        return null;
    }
    
    public Message getMessage(int messageid) {
        Message message = new Message();
        String query = "SELECT senderid, receiverid, chatroomid, messagebody " +
                "FROM messages " +
                "WHERE messageid=?;";
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement(query);
        try {
            preparedStatement.setInt(1, messageid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        jdbcTemplate.executeQuery(preparedStatement, SimpleSqliteTemplate.CHAT);

        return message;
    }

    public User getUser(int userid) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("SELECT nickname FROM users WHERE userid=?;");
        try {
            preparedStatement.setInt(1, userid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        User user = (User) jdbcTemplate.executeQuery(preparedStatement, SimpleSqliteTemplate.USER);
        if (user.getNickname() != null)
            user.setUserid(userid);
        return user;
    }

    public User updateUser(int userid, String nickname) {
        if (getUser(userid).getUserid() != 0) {
            PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("UPDATE users SET nickname=? WHERE userid=?;");
            try {
                preparedStatement.setString(1, nickname);
                preparedStatement.setInt(2, userid);
                jdbcTemplate.executeUpdate(preparedStatement);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        } else {
            return null;
        }

        User user = getUser(nickname);
        return user;
    }

    public void deleteUser(int userid) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("DELETE FROM users WHERE userid=?;");
        try {
            preparedStatement.setInt(1, userid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        jdbcTemplate.executeUpdate(preparedStatement);
    }

    public Chatroom addChatRoom(String chatroomname, int userid) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("INSERT INTO chatrooms (chatroomname, userid) values (?, ?);");
        try {
            preparedStatement.setString(1, chatroomname);
            preparedStatement.setInt(2, userid);
            jdbcTemplate.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return getChatRoom(chatroomname);
    }

    public Chatroom getChatRoom(String chatroomname) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("SELECT chatroomid, userid FROM chatrooms WHERE chatroomname=?;");
        try {
            preparedStatement.setString(1, chatroomname);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Chatroom chatroom = (Chatroom) jdbcTemplate.executeQuery(preparedStatement, SimpleSqliteTemplate.CHATROOM);
        chatroom.setChatroomname(chatroomname);
        return chatroom;
    }

    public Chatroom getChatRoom(int chatroomid) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("SELECT chatroomname, userid FROM chatrooms WHERE chatroomid=?;");
        try {
            preparedStatement.setInt(1, chatroomid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Chatroom chatroom = (Chatroom) jdbcTemplate.executeQuery(preparedStatement, SimpleSqliteTemplate.CHATROOM);
        chatroom.setChatroomid(chatroomid);
        return chatroom;
    }

    public List<Chatroom> getChatRoomByUserid(int userid) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("SELECT chatrooms.chatroomid, chatrooms.chatroomname " +
                "FROM chatrooms INNER JOIN chatroomssnapshot " +
                "ON chatroomssnapshot.chatroomid = chatrooms.chatroomid " +
                "WHERE chatroomssnapshot.userid=?;");
        try {
            preparedStatement.setInt(1, userid);
        } catch (SQLException e) {
            e.printStackTrace();
        }

        List<Chatroom> chatroomList = (List<Chatroom>) jdbcTemplate.executeQuery(preparedStatement, SimpleSqliteTemplate.CHATROOMS);
        return chatroomList;
    }

    public Chatroom updateChatroom(String chatroomname, int userid) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("UPDATE chatrooms SET chatroomname=? WHERE userid=?;");
        try {
            preparedStatement.setString(1, chatroomname);
            preparedStatement.setInt(2, userid);
            jdbcTemplate.executeUpdate(preparedStatement);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        Chatroom tempRoom = getChatRoom(chatroomname);
        
        return new Chatroom(tempRoom.getChatroomname(), tempRoom.getChatroomid(), tempRoom.getUserid());
    }

    public void quitChatroom(int chatroomid, int userid) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("DELETE FROM chatroomssnapshot WHERE userid=? AND chatroomid=?;");
        try {
            preparedStatement.setInt(1, userid);
            preparedStatement.setInt(2, chatroomid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        jdbcTemplate.executeUpdate(preparedStatement);
    }

    public ArrayList<User> getChatroomJoiner(int chatroomid) {
        PreparedStatement preparedStatement = jdbcTemplate.preparedStatement("SELECT userid FROM chatroomssnapshot WHERE chatroomid =?;");
        try {
            preparedStatement.setInt(1, chatroomid);
        } catch (SQLException e) {
            e.printStackTrace();
        }
        ArrayList<User> users = (ArrayList<User>) jdbcTemplate.executeQuery(preparedStatement, SimpleSqliteTemplate.CHATROOMUSER);
        
        for (User us : users) {
            us.setNickname(getUser(us.getUserid()).getNickname());       
        }
        
        return users;
    }
}
