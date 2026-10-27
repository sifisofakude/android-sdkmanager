package io.github.sifisofakude.sdkmanager;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

import java.security.MessageDigest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import kotlinx.io.Sink;
import kotlinx.io.Source;
import kotlinx.io.SinksJvmKt;
import kotlinx.io.SourcesJvmKt;

import io.github.sifisofakude.filesystem.FileOperation;
import io.github.sifisofakude.net.serverresults.ServerResults;


public class AndroidSdkManager {

    private final String url =
        "https://dl.google.com/android/repository";

    private static final String REPOSITORY_FILE =
        "repository2-3.xml";

    private static final int BUFFER_SIZE = 64 * 1024;


    public boolean downloadRepoConfig(String outputFile) {
        FileOperation file = new FileOperation(outputFile);

        try {
            if (!file.exists()) {
                file.createNewFile();
            }

            Sink sink = file.openSink(false);

            if (sink == null) {
                return false;
            }

            try (OutputStream out = SinksJvmKt.asOutputStream(sink)) {
                return downloadRepoConfig(out);
            }
        } catch (Exception e) {
            return false;
        }
    }


    public boolean downloadRepoConfig(OutputStream out) {
        ServerResults sr = ServerResults.getServerResults(
            url + "/" + REPOSITORY_FILE,
            (String) null,
            "get"
        );

        return sr.isSuccess() && sr.downloadFile(out);
    }


    public InputStream downloadRepoConfig() {
        ServerResults sr = ServerResults.getServerResults(
            url + "/" + REPOSITORY_FILE,
            (String) null,
            "get"
        );

        if (!sr.isSuccess()) {
            return null;
        }

        return sr.getResponse().body().byteStream();
    }


    public Map<String, String> parsePlatformsRepoFile(
        String platform,
        String filePath
    ) throws Exception {

        Source source = new FileOperation(filePath).openSource();

        if (source == null) {
            throw new IOException(
                "File '" + filePath + "' does not exist"
            );
        }

        try (InputStream in = SourcesJvmKt.asInputStream(source)) {
            return parsePlatformsRepoFile(platform, in);
        }
    }


    public Map<String, String> parsePlatformsRepoFile(
        String platform,
        InputStream inputStream
    ) throws Exception {

        if (inputStream == null) {
            throw new IOException("Repository input stream is null");
        }

        Map<String, String> result = new HashMap<>();

        DocumentBuilderFactory factory =
            DocumentBuilderFactory.newInstance();

        // Restrict XML parsing to prevent external entity access.
        factory.setFeature(
            XMLConstants.FEATURE_SECURE_PROCESSING,
            true
        );

        factory.setFeature(
            "http://apache.org/xml/features/disallow-doctype-decl",
            true
        );

        factory.setFeature(
            "http://xml.org/sax/features/external-general-entities",
            false
        );

        factory.setFeature(
            "http://xml.org/sax/features/external-parameter-entities",
            false
        );

        factory.setXIncludeAware(false);
        factory.setExpandEntityReferences(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(inputStream);

        NodeList packages =
            document.getElementsByTagName("remotePackage");

        for (int i = 0; i < packages.getLength(); i++) {
            Node node = packages.item(i);

            if (node.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element element = (Element) node;

            if (!platform.equals(element.getAttribute("path"))) {
                continue;
            }

            NodeList completeElements =
                element.getElementsByTagName("complete");

            if (completeElements.getLength() == 0) {
                continue;
            }

            Element complete = (Element) completeElements.item(0);

            NodeList children = complete.getChildNodes();

            for (int j = 0; j < children.getLength(); j++) {
                Node child = children.item(j);

                if (child.getNodeType() != Node.ELEMENT_NODE) {
                    continue;
                }

                Element childElement = (Element) child;

                String name = child.getNodeName();
                String value = child.getTextContent().trim();

                if ("checksum".equals(name)) {
                    String type = childElement.getAttribute("type");
                    value = type + ";" + value;
                }

                result.put(name, value);
            }

            // The package path is unique in repository metadata.
            break;
        }

        return result;
    }


    public boolean downloadPlatform(
        String platform,
        String outputDir
    ) throws Exception {

        Map<String, String> platformInfo;

        try (InputStream repository = downloadRepoConfig()) {
            if (repository == null) {
                return false;
            }

            platformInfo = parsePlatformsRepoFile(
                platform,
                repository
            );
        }

        String packageUrl = platformInfo.get("url");

        if (packageUrl == null || packageUrl.isEmpty()) {
            return false;
        }

        String checksum = platformInfo.get("checksum");

        return downloadPackage(packageUrl, checksum, outputDir);
    }


    public boolean downloadPackage(
        String packageUrl,
        String expectedChecksum,
        String outputDir
    ) throws Exception {

        if (packageUrl == null || packageUrl.isEmpty()) {
            return false;
        }

        String downloadUrl = packageUrl.startsWith("https://")
            ? packageUrl
            : url + "/" + packageUrl.replaceFirst("^/+", "");

        ServerResults sr = ServerResults.getServerResults(
            downloadUrl,
            (String) null,
            "get"
        );

        if (!sr.isSuccess()) {
            return false;
        }

        File destination = new File(outputDir)
            .getCanonicalFile();

        if (!destination.exists() && !destination.mkdirs()) {
            return false;
        }

        if (!destination.isDirectory()) {
            return false;
        }

        File temporaryArchive = File.createTempFile(
            "sdk-package-",
            ".zip"
        );

        try {
            try (
                InputStream in = sr.getResponse()
                    .body()
                    .byteStream();

                OutputStream out = new java.io.FileOutputStream(
                    temporaryArchive
                )
            ) {
                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }

            if (expectedChecksum != null
                    && !expectedChecksum.isEmpty()) {

                String[] parts = expectedChecksum.split(";", 2);

                if (parts.length != 2) {
                    return false;
                }

                String algorithm = parts[0].trim();
                String expected = parts[1].trim();

                if (!verifyChecksum(
                        temporaryArchive,
                        algorithm,
                        expected)) {
                    return false;
                }
            }

            return extractArchive(temporaryArchive, destination);

        } finally {
            Files.deleteIfExists(temporaryArchive.toPath());
        }
    }


    private boolean verifyChecksum(
        File file,
        String algorithm,
        String expected
    ) throws Exception {

        String javaAlgorithm;

        switch (algorithm.toLowerCase(Locale.ROOT)) {
            case "sha1":
                javaAlgorithm = "SHA-1";
                break;

            case "sha256":
                javaAlgorithm = "SHA-256";
                break;

            case "sha512":
                javaAlgorithm = "SHA-512";
                break;

            default:
                throw new IllegalArgumentException(
                    "Unsupported checksum algorithm: " + algorithm
                );
        }

        MessageDigest digest = MessageDigest.getInstance(
            javaAlgorithm
        );

        try (InputStream in = new java.io.FileInputStream(file)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int bytesRead;

            while ((bytesRead = in.read(buffer)) != -1) {
                digest.update(buffer, 0, bytesRead);
            }
        }

        StringBuilder actual = new StringBuilder();

        for (byte b : digest.digest()) {
            actual.append(
                String.format(Locale.ROOT, "%02x", b & 0xff)
            );
        }

        return actual.toString().equalsIgnoreCase(expected);
    }


    private boolean extractArchive(
        File archive,
        File destination
    ) throws IOException {

        Path root = destination.toPath()
            .toRealPath();

        byte[] buffer = new byte[BUFFER_SIZE];

        try (
            InputStream fileIn =
                new java.io.FileInputStream(archive);

            ZipInputStream zip = new ZipInputStream(fileIn)
        ) {

            ZipEntry entry;

            while ((entry = zip.getNextEntry()) != null) {
                try {
                    Path target = root.resolve(
                        entry.getName()
                    ).normalize();

                    // Prevent Zip Slip.
                    if (!target.startsWith(root)) {
                        return false;
                    }

                    if (entry.isDirectory()) {
                        Files.createDirectories(target);
                        continue;
                    }

                    Path parent = target.getParent();

                    if (parent != null) {
                        Files.createDirectories(parent);
                    }

                    // Avoid following a pre-existing symbolic link.
                    Path current = target;

                    while (current != null && current.startsWith(root)) {
                        if (Files.isSymbolicLink(current)) {
                            return false;
                        }

                        if (current.equals(root)) {
                            break;
                        }

                        current = current.getParent();
                    }

                    try (OutputStream out =
                            Files.newOutputStream(
                                target,
                                java.nio.file.StandardOpenOption.CREATE,
                                java.nio.file.StandardOpenOption.TRUNCATE_EXISTING,
                                java.nio.file.StandardOpenOption.WRITE
                            )) {

                        int bytesRead;

                        while ((bytesRead = zip.read(buffer)) != -1) {
                            out.write(buffer, 0, bytesRead);
                        }
                    }

                } finally {
                    zip.closeEntry();
                }
            }
        }

        return true;
    }


    private void printUsage() {
        System.out.println(
            "AndroidSdkManager\n" +
            "\n" +
            "Usage:\n" +
            "  sdkmanager --repo-config [--output <file>]\n" +
            "  sdkmanager --platform <package-path> [--output <dir>]\n" +
            "  sdkmanager --help\n" +
            "\n" +
            "Options:\n" +
            "  --repo-config       Download repository2-3.xml\n" +
            "  --platform <path>   Download an SDK package\n" +
            "  --output <path>     Output file or installation directory\n" +
            "  --help              Show this help message\n" +
            "\n" +
            "Examples:\n" +
            "  sdkmanager --repo-config --output ./repository.xml\n" +
            "  sdkmanager --platform \"platform;android-36\" " +
            "--output ./platforms/android-36"
        );
    }


    public static void main(String[] args) {
        AndroidSdkManager manager = new AndroidSdkManager();

        if (args.length == 0) {
            manager.printUsage();
            return;
        }

        String command = null;
        String output = null;
        List<String> positional = new ArrayList<>();

        for (int i = 0; i < args.length; i++) {
            String arg = args[i];

            switch (arg) {
                case "--help":
                case "-h":
                    manager.printUsage();
                    return;

                case "--repo-config":
                    if (command != null) {
                        System.err.println(
                            "Only one command can be specified."
                        );
                        return;
                    }

                    command = "repo-config";
                    break;

                case "--platform":
                    if (command != null) {
                        System.err.println(
                            "Only one command can be specified."
                        );
                        return;
                    }

                    if (i + 1 >= args.length) {
                        System.err.println(
                            "--platform requires a package path."
                        );
                        return;
                    }

                    command = "platform";
                    positional.add(args[++i]);
                    break;

                case "--output":
                case "-o":
                    if (i + 1 >= args.length) {
                        System.err.println(
                            arg + " requires a path."
                        );
                        return;
                    }

                    output = args[++i];
                    break;

                default:
                    System.err.println(
                        "Unknown option: " + arg
                    );
                    manager.printUsage();
                    return;
            }
        }

        if (command == null) {
            manager.printUsage();
            return;
        }

        try {
            boolean success;

            switch (command) {
                case "repo-config":
                    if (output == null) {
                        output = REPOSITORY_FILE;
                    }

                    success = manager.downloadRepoConfig(output);

                    if (success) {
                        System.out.println(
                            "Repository configuration saved to: " +
                            output
                        );
                    }
                    break;

                case "platform":
                    if (output == null) {
                        output = "platforms";
                    }

                    success = manager.downloadPlatform(
                        positional.get(0),
                        output
                    );

                    if (success) {
                        System.out.println(
                            "Package installed to: " + output
                        );
                    }
                    break;

                default:
                    success = false;
            }

            if (!success) {
                System.err.println(
                    "Operation failed."
                );

                System.exit(1);
            }

        } catch (Exception e) {
            System.err.println(
                "Error: " + e.getMessage()
            );

            System.exit(1);
        }
    }
}
