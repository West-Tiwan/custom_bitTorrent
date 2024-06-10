import com.dampcake.bencode.Bencode; // - available if you need it!
import com.dampcake.bencode.Type;
import com.google.gson.Gson;

import java.math.BigInteger;
import java.util.Map;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.*;
public class Main {
    private static final Gson gson = new Gson();
    public static void main(String[] args) throws Exception {
        String command = args[0];
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            String decoded;
            try {
                decoded = decodeBencode(bencodedValue);
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(decoded);
        } else if ("info".equals(command)) {
            String torrentPath = args[1];
            Torrent torrent = new Torrent(Files.readAllBytes(Path.of(torrentPath)));
            System.out.println("Tracker URL: "+torrent.announce);
            System.out.println("Length: "+torrent.length);
        } else {
            System.out.println("Unknown command: " + command);
        }
    }

    static class Torrent {
        public String announce;
        public long length;
        public Torrent(byte[] bytes) throws NoSuchAlgorithmException {
            Bencode bencode = new Bencode(false);
            Bencode bencode2 = new Bencode(true);
            Map<String ,Object> root = bencode.decode(bytes,Type.DICTIONARY);
            Map<String ,Object> info = (Map<String,Object>) root.get("info");
            announce = (String) root.get("announce");
            length = (Long) info.get("length");
            MessageDigest digest2 = MessageDigest.getInstance("SHA-1");
            byte[] infoHash = digest2.digest(bencode2.encode((Map<String, Object>)bencode2.decode(bytes, Type.DICTIONARY).get("info")));
            BigInteger no = new BigInteger(1,infoHash);
            String hashText = no.toString(16);
            System.out.println("Info Hash: "+hashText);
            System.out.println("Piece Length: " + info.get("piece length"));
            int i = 0;
            System.out.println("Piece Hashes: ");
            while (i < ((byte[])info.get("pieces")).length) {
                byte[] splitted =Arrays.copyOfRange((byte[])info.get("pieces"), i, i + 20);
                BigInteger no1 = new BigInteger(1,splitted);
                String pieceHash = no1.toString(16);
                System.out.println(pieceHash);
                i += 20;
                if (i < ((byte[])info.get("pieces")).length)
                    System.out.println();
            }
        }
    }

    static String decodeBencode(String bencodedString) {
        Bencode bencode = new Bencode();
        char firstChar = bencodedString.charAt(0);
        Object decoded;
        if (Character.isDigit(firstChar)) {
            // bencoded string
            int firstColonIndex = 0;
            for (int i = 0; i < bencodedString.length(); i++) {
                if (bencodedString.charAt(i) == ':') {
                    firstColonIndex = i;
                    break;
                }
            }
            int length =
                    Integer.parseInt(bencodedString.substring(0, firstColonIndex));
            decoded = bencodedString.substring(firstColonIndex + 1,
                    firstColonIndex + 1 + length);
        } else if (firstChar == 'i') {
            // bencoded number
            decoded = bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8),
                    Type.NUMBER);
        } else if (firstChar == 'l') {
            // bencoded list
            decoded = bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8),
                    Type.LIST);
        } else if (firstChar == 'd') {
            // bencoded dictionary
            decoded = bencode.decode(bencodedString.getBytes(StandardCharsets.UTF_8),
                    Type.DICTIONARY);
        } else {
            throw new RuntimeException("Not supported");
        }
        return gson.toJson(decoded);
    }
}