package correcter;

import java.io.*;
import java.util.Random;
import java.util.Scanner;

public class Main {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        System.out.print("Write a mode: ");
        String mode = getUserInput(scanner);
        switch (mode) {
            case "encode":
                encode();
                break;
            case "send":
                send();
                break;
            case "decode":
                decode();
                break;
        }
    }

    private static void encode() {
        File textFile = new File("send.txt");
        File encodedFile = new File("encoded.txt");
        try (InputStream inputStreamReader = new FileInputStream(textFile);
             OutputStream outputStream = new FileOutputStream(encodedFile)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (inputStreamReader.available() > 0) {
                byteArrayOutputStream.write(inputStreamReader.read());
            }
            System.out.println(textFile.getPath() + ":");
            textView(byteArrayOutputStream.toString());
            hexView(byteArrayOutputStream.toByteArray());
            binView(byteArrayOutputStream.toByteArray(), "bin view: ");
            System.out.println();
            System.out.println(encodedFile.getPath() + ":");
            byte[] enc = encodeHamming(byteArrayOutputStream.toByteArray());
            expandView(enc);
            binView(enc, "parity: ");
            hexView(enc);
            outputStream.write(enc);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void send() {
        File encodedFile = new File("encoded.txt");
        File receivedFile = new File("received.txt");
        try (InputStream inputStreamReader = new FileInputStream(encodedFile);
             OutputStream outputStream = new FileOutputStream(receivedFile)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (inputStreamReader.available() > 0) {
                byteArrayOutputStream.write(inputStreamReader.read());
            }
            System.out.println(encodedFile.getPath() + ":");
            hexView(byteArrayOutputStream.toByteArray());
            binView(byteArrayOutputStream.toByteArray(), "bin view: ");
            System.out.println();
            System.out.println(receivedFile.getPath() + ":");

            byte[] received = sendWithErrors(byteArrayOutputStream.toByteArray());
            binView(received, "bin view: ");
            hexView(received);
            outputStream.write(received);

        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void decode() {
        File receivedFile = new File("received.txt");
        File textFile = new File("decoded.txt");
        try (InputStream inputStreamReader = new FileInputStream(receivedFile);
             OutputStream outputStream = new FileOutputStream(textFile)) {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            while (inputStreamReader.available() > 0) {
                byteArrayOutputStream.write(inputStreamReader.read());
            }
            System.out.println(receivedFile.getPath() + ":");
            hexView(byteArrayOutputStream.toByteArray());
            binView(byteArrayOutputStream.toByteArray(), "bin view: ");
            System.out.println();

            System.out.println(textFile.getPath() + ":");
            byte[] corrected = correctHamming(byteArrayOutputStream.toByteArray());
            binView(corrected, "correct: ");
            byte[] decoded = decodeHamming(corrected);
            binView(decoded, "decoded: ");
            hexView(decoded);
            textView(new String(decoded));
            outputStream.write(decoded);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static byte[] decodeHamming(byte[] bytes) {
        byte[] decoded = new byte[bytes.length / 2];
        for (int i = 0; i < bytes.length; i++) {
            int half = (bytes[i] & 0x20) >> 2 | ((0x0F & bytes[i])) >> 1;
            decoded[(i >> 1)] += half << (((i + 1) & 0x01) * 4);
        }
        return decoded;
    }

    private static byte[] correctHamming(byte[] bytes) {
        for (int i = 0; i < bytes.length; i++) {
            byte b = bytes[i];
            int p1 = bit(7, b);
            int p2 = bit(6, b);
            int p4 = bit(4, b);
            int parity1 = bit(5, b) ^ bit(3, b) ^ bit(1, b);
            int parity2 = bit(5, b) ^ bit(2, b) ^ bit(1, b);
            int parity4 = bit(3, b) ^ bit(2, b) ^ bit(1, b);
            int faultyBit = (8 - ((p4 ^ parity4) * 4 + (p2 ^ parity2) * 2 + (p1 ^ parity1))) % 8;
            bytes[i] = flipBit(faultyBit, bytes[i]);
        }
        return bytes;
    }

    private static byte[] encodeHamming(byte[] bytes) {
        // {0x00, 0xD2, 0x54, 0x86, 0x98, 0x4A, 0xCC, 0x1E, 0xE0, 0x32, 0xB4, 0x66, 0x78, 0xAA, 0x2C, 0xFE}
        final byte[] hammingCodeTable = {
                (byte) 0x00, (byte) 0xD2, (byte) 0x54, (byte) 0x86,
                (byte) 0x98, (byte) 0x4A, (byte) 0xCC, (byte) 0x1E,
                (byte) 0xE0, (byte) 0x32, (byte) 0xB4, (byte) 0x66,
                (byte) 0x78, (byte) 0xAA, (byte) 0x2C, (byte) 0xFE
        };
        byte[] enc = new byte[bytes.length * 2];
        for (int i = 0; i < enc.length; i++) {
            int half = 0x0F & bytes[i / 2] >> 4 * ((i + 1) % 2);
            enc[i] = hammingCodeTable[half];
        }
        return enc;
    }

    private static byte bit(int bitPos, byte aByte) {
        return (byte) ((aByte & (1 << bitPos)) >>> bitPos);
    }

    private static byte flipBit(int bitPos, byte aByte) {
        return (byte) (aByte ^ (1 << bitPos));
    }

    private static void binView(byte[] bytes, String title) {
        System.out.print(title);
        System.out.println(toBinary(bytes));
    }

    private static void expandView(byte[] bytes) {
        System.out.print("expand: ");
        System.out.println(toBinary(bytes).replaceAll("[10]{2} |[10]{2}$", ".. "));
    }

    private static String toBinary(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * Byte.SIZE);
        for (int i = 0; i < Byte.SIZE * bytes.length; i++) {
            if (i > 0 && i % 8 == 0) sb.append(" ");
            sb.append((bytes[i / Byte.SIZE] << i % Byte.SIZE & 0x80) == 0 ? '0' : '1');
        }
        return sb.toString();
    }

    private static void hexView(byte[] bytes) {
        System.out.print("hex view: ");
        for (byte b : bytes) {
            System.out.printf("%02X ", b);
        }
        System.out.println();
    }

    private static void textView(String str) {
        System.out.println("text view: " + str);
    }

    private static byte[] sendWithErrors(byte[] bytes) {
        Random rnd = new Random();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = oneBitError(bytes[i], rnd);
        }
        return bytes;
    }

    private static byte oneBitError(byte b, Random rnd) {
        return (byte) (b ^ (1 << rnd.nextInt(8)));
    }

    private static String getUserInput(Scanner sc) {
        return sc.nextLine();
    }

}
