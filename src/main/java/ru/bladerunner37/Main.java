package ru.bladerunner37;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.zeroturnaround.zip.ZipUtil;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;

import static java.nio.file.FileVisitResult.CONTINUE;
import static java.nio.file.FileVisitResult.TERMINATE;

public class Main {

    private static final HttpClient CLIENT = HttpClientBuilder.create().build();
    private static final String DOWNLOAD_URL = "http://search.maven.org/remotecontent?filepath=%s";
    private static final String INPUT = "jars.txt";
    private static final String DEST = "toNexus.zip";

    private File inputFile;
    private File temp;

    public static void main(String[] args) {
        Main main = new Main();

        try {
            main.prepareFiles(INPUT);

            for (ArtiInfo artiInfo : main.artifacts()) {
                main.downloadPomAndJar(artiInfo);
            }

            print("Artifacts downloaded, packing to zip...");
            ZipUtil.pack(main.temp, new File(DEST));
            println("  SUCCESS");
        } catch (Throwable t) {
            println(t.getMessage());
            t.printStackTrace();
        } finally {
            String err = "Fail to delete temp file: ";
            deleteFileOrFolder(main.inputFile.toPath(), err.concat(main.inputFile.getName()));
            deleteFileOrFolder(main.temp.toPath(), err.concat(main.temp.getName()));
        }
    }

    private void prepareFiles(String inputFileName) {
        File dest = new File(DEST);

        if (dest.exists()) {
            deleteFileOrFolder(dest.toPath(), "Fail to delete existing \"toNexus.zip\" file");
        }

        this.inputFile = new File(inputFileName);

        if (!this.inputFile.exists()) {
            println("Input file not found");
            System.exit(0);
        }

        this.temp = new File("temp");
        this.temp.mkdirs();
    }

    private void downloadPomAndJar(ArtiInfo artiInfo) throws Exception {
        try {
            print("Artifact downloading: " + artiInfo.getRawString() + "...");
            File dir = createArtiDir(artiInfo);

            String pomPath = prepareFilepath(artiInfo).concat(".pom");
            String jarPath = prepareFilepath(artiInfo).concat(".jar");

            HttpGet pomRequest = new HttpGet(String.format(DOWNLOAD_URL, pomPath));
            HttpResponse pomResponse = CLIENT.execute(pomRequest);

            File pom = new File(dir, "pom.xml");
            copyFileFromResponse(pomResponse, pom);

            HttpGet jarRequest = new HttpGet(String.format(DOWNLOAD_URL, jarPath));
            HttpResponse jarResponse = CLIENT.execute(jarRequest);

            File jar = new File(dir, artiInfo.getName() + "-" + artiInfo.getVersion() + ".jar");
            copyFileFromResponse(jarResponse, jar);
            
            println("  SUCCESS");
        } catch (Throwable t) {
            println("  FAIL");
            throw new Exception("Cause:", t);
        }
    }

    private void copyFileFromResponse(HttpResponse response, File dest) throws IOException {
        InputStream is = response.getEntity().getContent();

        if (!dest.exists()) {
            dest.createNewFile();
        }

        OutputStream os = new FileOutputStream(dest);
        byte[] buf = new byte[1024];
        int length;

        while ((length = is.read(buf)) > 0) {
            os.write(buf, 0, length);
        }

        is.close();
        os.close();
    }

    private String prepareFilepath(ArtiInfo artiInfo) {
        StringBuilder filepath = new StringBuilder();

        artiInfo.getDomain().forEach(part -> {
            filepath.append(part);
            filepath.append("/");
        });

        filepath.append(artiInfo.getName());
        filepath.append("/");
        filepath.append(artiInfo.getVersion());
        filepath.append("/");
        filepath.append(artiInfo.getName());
        filepath.append("-");
        filepath.append(artiInfo.getVersion());

        return filepath.toString();
    }

    private File createArtiDir(ArtiInfo artiInfo) {

        File file = new File(this.temp, artiInfo.name);
        file.mkdirs();

        return file;
    }

    private List<ArtiInfo> artifacts() throws Exception {

        List<ArtiInfo> result = new ArrayList<>();

        try {
            Scanner scanner = new Scanner(new FileInputStream(this.inputFile));

            while (scanner.hasNextLine()) {
                String raw = scanner.nextLine();
                ArtiInfo artiInfo = exctractArtiInfo(raw);
                artiInfo.setRawString(raw);
                result.add(artiInfo);
            }
        } catch (Throwable t) {
            throw new Exception("Fail to parse input file:");
        }

        return result;
    }

    private ArtiInfo exctractArtiInfo(String line) {
        ArtiInfo artiInfo = new ArtiInfo();

        String[] arr1 = line.split(":");
        String[] domainParts = arr1[0].split("\\.");

        for (String domainPart : domainParts) {
            artiInfo.getDomain().add(domainPart);
        }

        artiInfo.setName(arr1[1]);
        artiInfo.setVersion(arr1[2]);

        return artiInfo;
    }
    
    private static void println(String s) {
        System.out.println(s);
    }

    private static void print(String s) {
        System.out.print(s);
    }

    private static void deleteFileOrFolder(final Path path, String errorText) {
        try {
            Files.walkFileTree(path, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult visitFile(final Path file, final BasicFileAttributes attrs)
                        throws IOException {
                    Files.delete(file);
                    return CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(final Path file, final IOException e) {
                    return handleException(e);
                }

                private FileVisitResult handleException(final IOException e) {
                    e.printStackTrace();
                    return TERMINATE;
                }

                @Override
                public FileVisitResult postVisitDirectory(final Path dir, final IOException e)
                        throws IOException {
                    if (e != null) return handleException(e);
                    Files.delete(dir);
                    return CONTINUE;
                }
            });
        } catch (IOException e) {
            println(errorText);
            e.printStackTrace();
        }
    }

    ;

    private class ArtiInfo {
        private List<String> domain = new ArrayList<>();
        private String name;
        private String version;
        private String rawString;

        List<String> getDomain() {
            return domain;
        }

        public void setDomain(List<String> domain) {
            this.domain = domain;
        }

        String getName() {
            return name;
        }

        void setName(String name) {
            this.name = name;
        }

        String getVersion() {
            return version;
        }

        void setVersion(String version) {
            this.version = version;
        }

        String getRawString() {
            return rawString;
        }

        void setRawString(String rawString) {
            this.rawString = rawString;
        }
    }
}
