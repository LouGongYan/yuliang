import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.zip.CRC32;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Copies the aapt2 APK and adds classes.dex.
 *
 * Android 11+ requires resources.arsc to be stored uncompressed and 4-byte
 * aligned. Some ZIP libraries quietly recompress it, so this tiny writer sets
 * STORED explicitly for resources.arsc and classes.dex.
 *
 * Usage: java RepackApk <unsigned.apk> <with-dex.apk> <classes.dex>
 */
public final class RepackApk {

    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("usage: RepackApk <unsigned.apk> <with-dex.apk> <classes.dex>");
            System.exit(2);
        }
        Path input = Paths.get(args[0]);
        Path output = Paths.get(args[1]);
        Path dex = Paths.get(args[2]);

        try (ZipInputStream in = new ZipInputStream(Files.newInputStream(input));
             ZipOutputStream out = new ZipOutputStream(Files.newOutputStream(output))) {
            ZipEntry entry;
            while ((entry = in.getNextEntry()) != null) {
                if ("classes.dex".equals(entry.getName())) continue;
                byte[] data = readAll(in);
                ZipEntry copy = new ZipEntry(entry.getName());
                if (entry.getTime() != -1) copy.setTime(entry.getTime());
                if ("resources.arsc".equals(entry.getName())) {
                    store(copy, data);
                } else {
                    copy.setMethod(ZipOutputStream.DEFLATED);
                }
                out.putNextEntry(copy);
                out.write(data);
                out.closeEntry();
            }

            byte[] dexBytes = Files.readAllBytes(dex);
            ZipEntry dexEntry = new ZipEntry("classes.dex");
            store(dexEntry, dexBytes);
            out.putNextEntry(dexEntry);
            out.write(dexBytes);
            out.closeEntry();
        }
        System.out.println("repacked " + output.toAbsolutePath() + " (" + Files.size(output) + " bytes)");
    }

    private static void store(ZipEntry entry, byte[] data) {
        entry.setMethod(ZipOutputStream.STORED);
        entry.setSize(data.length);
        entry.setCompressedSize(data.length);
        CRC32 crc = new CRC32();
        crc.update(data);
        entry.setCrc(crc.getValue());
    }

    private static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream(8192);
        byte[] buffer = new byte[8192];
        int n;
        while ((n = in.read(buffer)) > 0) out.write(buffer, 0, n);
        return out.toByteArray();
    }
}
