package com.nexon.apiserver.test;

import com.nexon.apiserver.Response;
import com.nexon.apiserver.dao.Chatroom;
import com.nexon.apiserver.dao.Message;
import com.nexon.apiserver.dao.User;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Random;

import static org.junit.Assert.assertEquals;

/**
 * Created by chan8 on 2017-02-09.
 */
public class UserTest {

    private static String HOST = "http://localhost:";
    private static String BASE_URL = "/api/v1/";
    private static int PORT = 0;
    private JSONParser jsonParser;
    private RandomStringGenerator randomStringGenerator;
    private Random random;
    private String DEST = null;

    @Before
    public void startServer() {
//        Server server = new Server();
//        server.initialize();
//        server.start();

        this.jsonParser = new JSONParser();
        this.randomStringGenerator = new RandomStringGenerator();
        this.random = new Random();
        randomStringGenerator.initialize();
        initialize();
    }

    private void initialize() {
        JSONParser jsonParser = new JSONParser();
        JSONObject jsonObject = null;
        try {
            jsonObject = (JSONObject) jsonParser.parse(new FileReader("./config.json"));
        } catch (IOException e) {
            System.out.println("Error occured when parse from config.json");
        } catch (ParseException e) {
            System.out.println("Error occured when parse from config.json. Check Json syntax");
        }
        this.BASE_URL = (String) jsonObject.get("baseurl");
        this.PORT = Integer.parseInt(String.valueOf(jsonObject.get("port")));
        this.HOST = (String) jsonObject.get("host");

        this.DEST = HOST + ":" + PORT + BASE_URL;
    }


    @Test   // TCU-0111
    public void testPostUserLessTwentyLetter() throws IOException, ParseException {
        testPostUser(randomStringGenerator.nextRandomString(random.nextInt(19)));
    }

    @Test   // TCU-0112
    public void testPostUserTwentyLetters() throws IOException, ParseException {
        testPostUser(randomStringGenerator.nextRandomString(20));
    }

    @Test   // TCU-0113
    public void testPostUserOverTwentyLetter() throws IOException, ParseException {
        testPostUser(randomStringGenerator.nextRandomString(25));
    }

    @Test   // TCU-0114
    public void testPostUserWithSpecialLetter() throws IOException, ParseException {
        Response response = testPostUser(randomStringGenerator.nextRandomString(10) + "!@#$");
        assertEquals(400, response.getStatusCode());
    }

    @Test   // TCU-0121
    public void testPostUserExist() throws IOException, ParseException {
        String name = randomStringGenerator.nextRandomString(20);
        testPostUserExist(name);
    }

    @Test   // TCU-0211
    public void testPutUser() throws IOException, ParseException {
        String newName = randomStringGenerator.nextRandomString(20);
        String origin = randomStringGenerator.nextRandomString(20);
        Response response = testPutUser(origin, newName);
        assertEquals(response.getUser().getNickname(), newName);
    }

    @Test   // TCU-0212
    public void testPutUserExist() throws IOException, ParseException {
        String name = randomStringGenerator.nextRandomString(20);
        Response response = testPutUser(name, name);
        assertEquals(409, response.getStatusCode());
    }

    @Test   // TCU-0311
    public void testGetUser() throws IOException, ParseException {
        Response response = postUsers(randomStringGenerator.nextRandomString(20));
        int userid = response.getUser().getUserid();
        int matcheruserid = getUsers(userid).getUser().getUserid();
        assertEquals(userid, matcheruserid);
    }

    @Test   // TCU-0312
    public void getNonExistUser() throws IOException, ParseException {
        Response response = getUsers(randomStringGenerator.nextRandomInt());
        assertEquals(404, response.getStatusCode());
    }

    @Test   // TCU-0411
    public void testDeleteUser() throws IOException, ParseException {
        Response response = postUsers(randomStringGenerator.nextRandomString(20));
        User getUser = getUsers(response.getUser().getUserid()).getUser();
        response = deleteUser(getUser.getUserid());

        // Delete exist user : expect status code 200
        assertEquals(200, response.getStatusCode());

        // Delete non exist user : expect status code 400
        response = deleteUser(random.nextInt(Integer.MAX_VALUE));
        assertEquals(400, response.getStatusCode());
    }

    @Test   // TCU-0511
    public void getOwnChatrooms() throws IOException, ParseException {
        Response response = postUsers(randomStringGenerator.nextRandomString(15));
        Chatroom chatroom1 = postChatRoom(randomStringGenerator.nextRandomString(30), response.getUser().getUserid()).getChatroom();
        Chatroom chatroom2 = postChatRoom(randomStringGenerator.nextRandomString(30), response.getUser().getUserid()).getChatroom();

        response = getOwnChatrooms(response.getUser().getUserid());
        int id1 = response.getChatroomArrayList().get(0).getChatroomid();
        int id2 = response.getChatroomArrayList().get(1).getChatroomid();
        assertEquals(chatroom1.getChatroomid(), id1);
        assertEquals(chatroom2.getChatroomid(), id2);
    }

    private Response getOwnChatrooms(int userid) throws IOException, ParseException {
        String str = DEST + "users/" + userid + "/chatrooms";

        URL url = new URL(str);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Accept", "application/json");

        Response response = new Response();

        if (urlConnection.getResponseCode() != 200) {
            response.setStatusCode(urlConnection.getResponseCode());
            return response;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        String output;
        StringBuilder sb = new StringBuilder();

        while ((output = br.readLine()) != null) {
            sb.append(output);
        }

        response = new Response();
        response.setStatusCode(urlConnection.getResponseCode());

        urlConnection.disconnect();
        JSONObject obj = (JSONObject) jsonParser.parse(sb.toString());
        JSONArray jsonArray = (JSONArray) obj.get("chatrooms");
        ArrayList<Chatroom> chatroomList = makeArrayListFromJsonArray(jsonArray);
        response.setChatroomArrayList(chatroomList);
        return response;
    }

    private ArrayList<Chatroom> makeArrayListFromJsonArray(JSONArray jsonArray) {
        ArrayList<Chatroom> chatroomArrayList = new ArrayList<Chatroom>();

        for (int i = 0; i < jsonArray.size(); ++i) {
            JSONObject obj = (JSONObject) jsonArray.get(i);
            Chatroom chatroom = new Chatroom();
            chatroom.setChatroomname((String) obj.get("chatroomname"));
            chatroom.setChatroomid(Integer.parseInt(String.valueOf(obj.get("chatroomid"))));

            chatroomArrayList.add(chatroom);
        }

        return chatroomArrayList;
    }

    private Response postChatRoom(String chatroomname, int userid) throws IOException, ParseException {
        String str = DEST + "chatrooms/";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("chatroomname", chatroomname);
        jsonObject.put("userid", userid);

        URL url = new URL(str);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");

        OutputStream out = urlConnection.getOutputStream();
        out.write(jsonObject.toJSONString().getBytes());
        out.flush();

        int statusCode = urlConnection.getResponseCode();

        if (statusCode != 200) {
            return new Response(statusCode);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        String output;
        StringBuilder sb = new StringBuilder();

        while ((output = br.readLine()) != null) {
            sb.append(output);
        }

        Response response = new Response();
        response.setStatusCode(urlConnection.getResponseCode());

        jsonObject = (JSONObject) jsonParser.parse(sb.toString());
        response.getChatroom().setChatroomid(Integer.parseInt(String.valueOf(jsonObject.get("chatroomid")), 10));
        response.getChatroom().setUserid(Integer.parseInt(String.valueOf(jsonObject.get("userid")), 10));
        response.getChatroom().setChatroomname((String) jsonObject.get("chatroomname"));

        return response;
    }


    private void testPostUserExist(String name) throws IOException, ParseException {
        Response response = postUsers(name);

        response = postUsers(name);
        assertEquals(response.getStatusCode(), 409);
    }

    private Response deleteUser(int userid) throws IOException, ParseException {
        String str = DEST + "users/" + userid;

        URL url = new URL(str);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("DELETE");
        urlConnection.setRequestProperty("Accept", "application/json");

        Response response = new Response();
        response.setStatusCode(urlConnection.getResponseCode());

        return response;
    }

    public Response testPostUser(String nickname) throws IOException, ParseException {
        Response response = postUsers(nickname);
        User getUser = getUsers(response.getUser().getUserid()).getUser();
        assertEquals(response.getUser().getNickname(), getUser.getNickname());
        return response;
    }

    public Response testPutUser(String originName, String newName) throws IOException, ParseException {
        Response response = postUsers(originName);
        User getUser = getUsers(response.getUser().getUserid()).getUser();

        response = putUser(getUser.getUserid(), newName);

        return response;
    }

    private Response putUser(int userid, String nickname) throws IOException, ParseException {
        String str = DEST + "users/" + userid;

        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nickname", nickname);

        URL url = new URL(str);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("PUT");
        urlConnection.setRequestProperty("Content-Type", "application/json");

        OutputStream out = urlConnection.getOutputStream();
        out.write(jsonObject.toJSONString().getBytes());
        out.flush();

        int statusCode = urlConnection.getResponseCode();
        if (statusCode != 200) {
            return new Response(statusCode);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        String output;
        StringBuilder sb = new StringBuilder();

        while ((output = br.readLine()) != null) {
            sb.append(output);
        }

        Response response = new Response();
        response.setStatusCode(urlConnection.getResponseCode());

        jsonObject = (JSONObject) jsonParser.parse(sb.toString());
        response.getUser().setNickname((String) jsonObject.get("nickname"));
        response.getUser().setUserid(Integer.parseInt(String.valueOf(jsonObject.get("userid")), 10));

        return response;
    }

    public Response postUsers(String nickname) throws IOException, ParseException {
        String str = DEST + "users/";
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("nickname", nickname);
        URL url = new URL(str);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setDoOutput(true);
        urlConnection.setRequestMethod("POST");
        urlConnection.setRequestProperty("Content-Type", "application/json");

        OutputStream out = urlConnection.getOutputStream();
        out.write(jsonObject.toJSONString().getBytes());
        out.flush();

        int statusCode = urlConnection.getResponseCode();
        if (statusCode != 200) {
            return new Response(statusCode);
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        String output;
        StringBuilder sb = new StringBuilder();

        while ((output = br.readLine()) != null) {
            sb.append(output);
        }

        Response response = new Response();
        response.setStatusCode(urlConnection.getResponseCode());

        jsonObject = (JSONObject) jsonParser.parse(sb.toString());
        response.getUser().setNickname((String) jsonObject.get("nickname"));
        response.getUser().setUserid(Integer.parseInt(String.valueOf(jsonObject.get("userid")), 10));

        return response;
    }

    public Response getUsers(int userid) throws IOException, ParseException {
        String str = DEST + "users/" + userid;

        URL url = new URL(str);
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        urlConnection.setRequestMethod("GET");
        urlConnection.setRequestProperty("Accept", "application/json");

        Response response = new Response();

        if (urlConnection.getResponseCode() != 200) {
            response.setStatusCode(urlConnection.getResponseCode());
            return response;
        }

        BufferedReader br = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));

        String output;
        StringBuilder sb = new StringBuilder();
        while ((output = br.readLine()) != null) {
            sb.append(output);
        }

        urlConnection.disconnect();

        JSONObject jsonObject = (JSONObject) jsonParser.parse(sb.toString());
        User user = new User((String) jsonObject.get("nickname"), Integer.parseInt(String.valueOf(jsonObject.get("userid")), 10));
        response.setUser(user);
        return response;
    }
    
}