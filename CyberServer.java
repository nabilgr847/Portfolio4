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
        conn = DriverManager.getConnection("jdbc:sqlite:vault.db");
        Statement st = conn.createStatement();
        st.execute("CREATE TABLE IF NOT EXISTS users (name TEXT, email TEXT UNIQUE, password TEXT)");

        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new AuthHandler());
        server.setExecutor(null);
        System.out.println("🚀 Cyber Vault Server Ready at Port 8080");
        server.start();
    }

    static class AuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().add("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().add("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().add("Access-Control-Allow-Headers", "Content-Type");

            if (exchange.getRequestMethod().equalsIgnoreCase("OPTIONS")) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String query = br.readLine();
            Map<String, String> params = parseQuery(query);

            String action = params.get("action");
            String responseMsg = "INVALID";

            try {
                if ("login".equals(action)) {
                    String email = params.get("email");
                    String pass = params.get("password");
                    String sql = "SELECT * FROM users WHERE email = ? AND password = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, email);
                    pstmt.setString(2, pass);
                    ResultSet rs = pstmt.executeQuery();
                    responseMsg = rs.next() ? "SUCCESS" : "FAILED";
                } 
                else if ("request".equals(action)) {
                    String name = params.get("name");
                    String email = params.get("email");
                    String pass = params.get("password"); 
                    try {
                        String sql = "INSERT INTO users (name, email, password) VALUES (?, ?, ?)";
                        PreparedStatement pstmt = conn.prepareStatement(sql);
                        pstmt.setString(1, name);
                        pstmt.setString(2, email);
                        pstmt.setString(3, pass);
                        pstmt.executeUpdate();
                        responseMsg = "REQUEST_RECEIVED";
                    } catch (Exception e) { responseMsg = "ALREADY_EXISTS"; }
                }
                else if ("forgot".equals(action)) {
                    String email = params.get("email");
                    String sql = "SELECT password FROM users WHERE email = ?";
                    PreparedStatement pstmt = conn.prepareStatement(sql);
                    pstmt.setString(1, email);
                    ResultSet rs = pstmt.executeQuery();
                    if (rs.next()) {
                        responseMsg = "FOUND_KEY: " + rs.getString("password");
                    } else {
                        responseMsg = "NOT_FOUND";
                    }
                }
            } catch (Exception e) { responseMsg = "SERVER_ERROR"; }

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
