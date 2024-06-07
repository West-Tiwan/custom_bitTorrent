import com.dampcake.bencode.Bencode; // - available if you need it!
import com.dampcake.bencode.Type;
import com.google.gson.Gson;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class Main {
    private static final Logger LOGGER = LoggerFactory.getLogger(Main.class);
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
        } else {
            System.out.println("Unknown command: " + command);
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