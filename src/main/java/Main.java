import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
public class Main {
    private static final Gson gson = new Gson();
    public static void main(String[] args) throws Exception {
        String command = args[0];
        if (command.equals("decode")) {
            String bencodedValue = args[1];
            String decoded;
            switch (bencodedValue.charAt(0)) {
                case 'i' -> {
                    Bencode bencode = new Bencode(true);
                    decoded = "" + bencode.decode(bencodedValue.getBytes(), Type.NUMBER);
                }
                case 'l' -> {
                    Bencode bencode = new Bencode(false);
                    decoded = gson.toJson(bencode.decode(bencodedValue.getBytes(), Type.LIST));
                }
                case 'd' -> {
                    Bencode bencode = new Bencode(false);
                    decoded = gson.toJson(bencode.decode(bencodedValue.getBytes(), Type.DICTIONARY));
                }
                default -> {
                    try {
                        decoded = gson.toJson(decodeBencode(bencodedValue));
                    } catch (RuntimeException e) {
                        System.out.println(e.getMessage());
                        return;
                    }
                }
            }
            System.out.println(decoded);
        } else if (command.equals("info")) {
            String filePath = args[1];
            Torrent torrent = new Torrent(Files.readAllBytes(Path.of(filePath)));
            System.out.println("Tracker URL: " + torrent.announce);
            System.out.println("Length: " + torrent.length);
            System.out.println("Info Hash: " + bytesToHex(torrent.infoHash));
            System.out.println("Piece Length: " + torrent.pieceLength);
            for (byte[] pieceHash : torrent.pieceHashes) {
                System.out.println(bytesToHex(pieceHash));
            }
        }
                                else {
            System.out.println("Unknown command: " +
                    command);
        }
    }
    private static String decodeBencode(String bencodedString) {
        if (Character.isDigit(bencodedString.charAt(0))) {
            int firstColonIndex = 0;
            for (int i = 0; i < bencodedString.length(); i++) {
                if (bencodedString.charAt(i) == ':') {
                    firstColonIndex = i;
                    break;
                }
            }
            int length =
                    Integer.parseInt(bencodedString.substring(0, firstColonIndex));
            return bencodedString.substring(firstColonIndex + 1,
                    firstColonIndex + 1 + length);
        } else {
            throw new RuntimeException("Only strings are supported at the moment");
        }
    }
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
class Torrent {
    public String announce;
        public long length;
        public byte[] infoHash;
        public long pieceLength;
        public List<byte[]> pieceHashes = new ArrayList<>();
    public Torrent(byte[] bytes) throws Exception {
                Bencode bencode1 = new Bencode(false);
                Bencode bencode2 = new Bencode(true);
                Map<String, Object> root = bencode1.decode(bytes, Type.DICTIONARY);
                Map<String, Object> info = (Map<String, Object>)root.get("info");
                announce = (String)root.get("announce");
                length = (long)info.get("length");
                MessageDigest digest = MessageDigest.getInstance("SHA-1");
                infoHash = digest.digest(bencode2.encode(
                        (Map<String, Object>)bencode2.decode(bytes, Type.DICTIONARY)
                                .get("info")));
                pieceLength = (long)info.get("piece length");
                byte[] pieceHashes = ((ByteBuffer)((Map<String, Object>)bencode2
                        .decode(bytes, Type.DICTIONARY)
                        .get("info"))
                        .get("pieces"))
                        .array();
                for (int i = 0; i < pieceHashes.length; i += 20) {
                    byte[] pieceHash = Arrays.copyOfRange(pieceHashes, i, i + 20);
                    this.pieceHashes.add(pieceHash);
                }
            }
        }