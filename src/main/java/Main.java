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
            var infoHash = calculateHash(info);
            System.out.printf("Info Hash: %s\n", toHexString(infoHash));
            System.out.printf("Piece Length: %d\n", (long)info.get("piece length"));
            printPieceHashes(info);
        }
    }

    private static String toHexString(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private static byte[] calculateHash(Map<?, ?> infoDict) {
        var stringEncoded = Bencode.encodeToByteBuff(infoDict);
        try {
            var sha1Digest = MessageDigest.getInstance("SHA-1");
            sha1Digest.update(stringEncoded);
            return sha1Digest.digest();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
    }
}

    private static void printPieceHashes(Map<?,?> infoDict) {
        var data = (String)infoDict.get("pieces");
        var bytes = data.getBytes(StandardCharsets.ISO_8859_1);
        System.out.print("Piece Hashes:");
        for(int i=0;i<bytes.length; ++i){
            if(i%20 == 0){
                System.out.println();
            }
            System.out.printf("%02x", bytes[i]);
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