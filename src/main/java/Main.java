import com.dampcake.bencode.Bencode;
import com.dampcake.bencode.BencodeInputStream;
import com.dampcake.bencode.Type;
import com.google.gson.Gson;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.net.InetAddress;
import java.io.IOException;
import Bencode.Bdecoder;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Map;
import org.apache.commons.codec.binary.Hex;
import org.apache.http.HttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
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
        } else if (command.equals("peers")) {
            String filePath = args[1];
            Torrent torrent = new Torrent(Files.readAllBytes(Path.of(filePath)));

        } else {System.out.println("Unknown command: " +command);
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
            infoHash = digest.digest(bencode2.encode((Map<String, Object>)bencode2.decode(bytes, Type.DICTIONARY).get("info")));
            pieceLength = (long)info.get("piece length");
            byte[] pieceHashes = ((ByteBuffer)((Map<String, Object>)bencode2.decode(bytes, Type.DICTIONARY).get("info")).get("pieces")).array();
            for (int i = 0; i < pieceHashes.length; i += 20) {
                byte[] pieceHash = Arrays.copyOfRange(pieceHashes, i, i + 20);
                this.pieceHashes.add(pieceHash);
            }
    }
}

class Peer {
    private InetAddress ip;
    private int port;
    public Peer(InetAddress ip, int port) {
        this.ip = ip;
        this.port = port;
    }
    public InetAddress getIp() { return ip; }
    public void setIp(InetAddress ip) { this.ip = ip; }
    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }
    @Override
    public String toString() {
        return ip + ":" + port;
    }
}

class Tracker {
    private TrackerRequest request;
    private TrackerResponse response;
    public Tracker(TrackerRequest request) {
        this.request = request;
        response = this.sendRequest();
    }
    public TrackerResponse getResponse() { return response; }
    private TrackerResponse sendRequest() {
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpGet httpGet = new HttpGet(request.getUri());
        CloseableHttpResponse httpResponse = null;
        try {
            httpResponse = httpClient.execute(httpGet);
            return new TrackerResponse(httpResponse.getEntity().getContent());
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                httpResponse.close();
                httpClient.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}

class TrackerRequest {
    private String announce;
    private byte[] infoHash;
    private byte[] peerId;
    private int port;
    private int uploaded;
    private int downloaded;
    private long left;
    private int compact;
    public TrackerRequest(Torrent torrent) {
        announce = torrent.announce;
        infoHash = torrent.infoHash;
        peerId = "00112233445566778899".getBytes();
        port = 6881;
        uploaded = 0;
        downloaded = 0;
        left = torrent.length;
        compact = 1;
    }
    public URI getUri() {
        StringBuilder uri = new StringBuilder(announce + '/');
        uri.append("?info_hash=").append(urlEncode(infoHash));
        uri.append("&peer_id=").append(urlEncode(peerId));
        uri.append("&port=").append(port);
        uri.append("&uploaded=").append(uploaded);
        uri.append("&downloaded=").append(downloaded);
        uri.append("&left=").append(left);
        uri.append("&compact=").append(compact);
        try {
            return new URI(uri.toString());
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return null;
        }
    }
    private String urlEncode(byte[] bytes) {
        StringBuilder encoded = new StringBuilder();
        encoded.append("%");
        for (int i = 0; i < bytes.length; i++) {
            encoded.append(URLEncoder.encode(String.format("%02X", bytes[i]),
                    StandardCharsets.UTF_8));
            if (i < bytes.length - 1) {
                encoded.append("%");
            }
        }
        return encoded.toString();
    }
}

class TrackerResponse {
    private int interval;
    private Peer[] peers;
    public TrackerResponse(InputStream in) {
        BencodeInputStream decoder = new BencodeInputStream(in);
        Map<String, Object> decodedBody = (Map<String, Object>)decoder.getDecoded();
        peers = parsePeers((byte[])decodedBody.get("peers"));
        interval = ((Long)decodedBody.get("interval")).intValue();
    }
    public int getInterval() { return interval; }
    public Peer[] getPeers() { return peers; }
    private Peer[] parsePeers(byte[] bytes) {
        int PEER_SIZE = 6;
        int totalPeers = bytes.length / PEER_SIZE;
        Peer[] peers = new Peer[totalPeers];
        for (int i = 0; i < totalPeers; i++) {
            int offset = i * PEER_SIZE;
            byte[] ipBytes = Arrays.copyOfRange(bytes, offset, offset + 4);
            byte[] portBytes =
                    Arrays.copyOfRange(bytes, offset + 4, offset + PEER_SIZE);
            InetAddress ip = null;
            try {
                ip = InetAddress.getByAddress(ipBytes);
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
            int port = 0;
            for (byte portByte : portBytes) {
                port = (port << 8) | (portByte & 0xFF);
            }
            peers[i] = new Peer(ip, port);
        }
        return peers;
    }
}