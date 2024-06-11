import com.google.gson.Gson;
import com.dampcake.bencode.Bencode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Map;
public class Main {
    private static final Gson gson = new Gson();
    public static void main(String[] args) throws Exception {
        String command = args[0];
        if ("decode".equals(command)) {
            String bencodedValue = args[1];
            Object decoded;
            try {
                decoded = Bencode.decode(bencodedValue);
            } catch (RuntimeException e) {
                System.out.println(e.getMessage());
                return;
            }
            System.out.println(gson.toJson(decoded));
        } else if ("info".equals(command)) {
            Path filePath = Path.of(args[1]);
            if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                throw new RuntimeException("not a file.");
            }
            var fileBytes = Files.readAllBytes(filePath);
            Object decoded = Bencode.decode(fileBytes);
            switch (decoded) {
                case Map<?, ?> m -> {
                    assert m.containsKey("announce") && m.containsKey("info");
                    var trackerUrl = (String) m.get("announce");
                    var infoDict = (Map<?, ?>) m.get("info");
                    var length = (Long) infoDict.get("length");
                    var infoHash = calculateHash(infoDict);
                    System.out.printf("Tracker URL: %s\n", trackerUrl);
                    System.out.printf("Length: %d\n", length);
                    System.out.printf("Info Hash: %s\n", toHexString(infoHash));
                    System.out.printf("Piece Length: %d\n", (long)infoDict.get("piece length"));
                    printPieceHashes(infoDict);
                }
                default -> throw new IllegalStateException("Unexpected value: " + decoded);
            }
            System.out.println(gson.toJson(decoded));
        } else {
            System.out.println("Unknown command: " + command);
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