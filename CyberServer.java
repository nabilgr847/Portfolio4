import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import java.io.*;
import java.net.InetSocketAddress;
import java.sql.*;
import java.util.*;
import java.net.URLDecoder;

public class CyberServer {
    private static Connection conn;

    public static void main(String[] args) throws Exception {
        // ডাটাবেজ কানেকশন
        conn = DriverManager.getConnection("jdbc:sqlite:vault.db");
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE IF NOT EXISTS users (name TEXT, email TEXT UNIQUE, password TEXT)");

        // রেন্ডার পোর্টের জন্য ডায়নামিক সেটআপ
        int port = Integer.parseInt(System.getenv().getOrDefault("PORT", "8080"));
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        
        server.createContext("/", new AuthHandler());
        server.setExecutor(null);
        
        System.out.println("-------------------------------------------");
        System.out.println("🚀 CYBER VAULT SERVER IS LIVE!");
        System.out.println("📡 Listening on Port: " + port);
        System.out.println("-------------------------------------------");
        server.start();
    }

    static class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS Headers (GitHub থেকে ডাটা আসার পারমিশন)
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            // ইনকামিং ডাটা পড়া
            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String query = br.readLine();
            
            // 🔥 রেন্ডার লগে ডাটা দেখানোর ম্যাজিক লাইন
            System.out.println("\n[!] NEW REQUEST RECEIVED AT: " + new java.util.Date());
            System.out.println("📩 Raw Query Data: " + query);
            
            Map<String, String> params = parseQuery(query);
            String action = params.getOrDefault("action", "none");
            String responseMsg = "INVALID";

            try {
                if ("login".equals(action)) {
                    String email = params.get("email");
                    String pass = params.get("password");
                    
                    System.out.println("🔑 ACTION: LOGIN | USER: " + email);
                    
                    String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, email);
                    pstmt.setString(2, pass);
                    ResultSet rs = pstmt.executeQuery();
                    
                    if (rs.next()) {
                        responseMsg = "SUCCESS";
                        System.out.println("✅ STATUS: Login Successful");
                    } else {
                        responseMsg = "FAILED";
                        System.out.println("❌ STATUS: Login Failed (Wrong Credentials)");
                    }
                } 
                else if ("request".equals(action)) {
                    String name = params.get("name");
                    String email = params.get("email");
                    String pass = params.get("password"); 
                    
                    System.out.println("📝 ACTION: REGISTRATION");
                    System.out.println("👤 NAME: " + name);
                    System.out.println("📧 EMAIL: " + email);
                    System.out.println("🔐 KEY/PASS: " + pass);

                    try {
                        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, name);
                        pstmt.setString(2, email);
                        pstmt.setString(3, pass);
                        pstmt.executeUpdate();
                        responseMsg = "REQUEST_RECEIVED";
                        System.out.println("✅ STATUS: Account Created Successfully!");
                    } catch (Exception e) { 
                        responseMsg = "ALREADY_EXISTS"; 
                        System.out.println("❌ STATUS: Registration Failed (User already exists)");
                    }
                }
                else if ("forgot".equals(action)) {
                    String email = params.get("email");
                    System.out.println("❓ ACTION: FORGOT PASSWORD | EMAIL: " + email);
                    
                    String sql = "SELECT password FROM users WHERE email = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, email);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        String foundPass = rs.getString("password");
                        responseMsg = "FOUND_KEY: " + foundPass;
                        System.out.println("✅ STATUS: Key Found -> " + foundPass);
                    } else {
                        responseMsg = "NOT_FOUND";
                        System.out.println("❌ STATUS: Email not found in Database");
                    }
                }
            } catch (Exception e) { 
                responseMsg = "SERVER_ERROR"; 
                System.out.println("⚠️ ERROR: " + e.getMessage());
            }

            System.out.println("-------------------------------------------\n");

            byte[] responseBytes = responseMsg.getBytes("utf-8");
            exchange.sendResponseHeaders(200, responseBytes.length);
            OutputStream os = exchange.getResponseBody();
            os.write(responseBytes);
            os.close();
        }

        private Map<String, String> parseQuery(String query) {
            Map<String, String> result = new HashMap<>();
            if (query != null) {
                for (String param : query.split("&")) {
                    String[] pair = param.split("=");
                    if (pair.length > 1) {
                        try {
                            String key = URLDecoder.decode(pair[0], "UTF-8");
                            String value = URLDecoder.decode(pair[1], "UTF-8");
                            result.put(key, value);
                        } catch (Exception e) { }
                    }
                }
            }
            return result;
        }
    }
}
