package ru.slie.luna.sdk.utils;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.sevenz.SevenZArchiveEntry;
import org.apache.commons.compress.archivers.sevenz.SevenZFile;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.codehaus.cargo.container.ContainerException;
import org.codehaus.cargo.container.installer.ZipURLInstaller;
import org.codehaus.cargo.util.DefaultFileHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;

public class CustomZipURLInstaller extends ZipURLInstaller {
    private final Logger log = LoggerFactory.getLogger("installer");
    private final I18nResolver i18n = new I18nResolver();
    private final String extractDir;

    public CustomZipURLInstaller(URL remoteLocation, Path targetDir) {
        super(remoteLocation, targetDir.resolve("downloads").toString(), targetDir.toString());
        this.extractDir = targetDir.toString();
    }

    @Override
    public String getExtractDir() {
        if (extractDir == null) {
            return super.getExtractDir();
        }

        return extractDir;
    }

    @Override
    public String getHome() {
        if (!this.isAlreadyExtracted()) {
            throw new ContainerException("Failed to get container installation home as the container has not yet been installed. Please call install() first.");
        } else {
            String name = this.getSourceFileName();
            int dotPos = name.lastIndexOf(".");
            if (dotPos > -1) {
                name = name.substring(0, dotPos);
            }

            if (name.endsWith(".tar")) {
                name = name.substring(0, name.length() - 4);
            }

            return Path.of(extractDir).resolve(name).toString();
        }
    }

    private void unpack() throws Exception {
        String source = this.getFileHandler().append(this.getDownloadDir(), this.getSourceFileName());
        log.info(i18n.getString("cargo.installer.container_installing", source, this.getExtractDir()));
        if (source.endsWith(".7z")) {
            SevenZFile.Builder builder = SevenZFile.builder();
            builder.setFile(new File(source));
            SevenZFile sevenZFile = builder.get();

            SevenZArchiveEntry sevenZEntry;
            while((sevenZEntry = sevenZFile.getNextEntry()) != null) {
                String destinationEntry = this.getFileHandler().append(this.getExtractDir(), DefaultFileHandler.sanitizeFilename(sevenZEntry.getName(), this.getLogger()));
                if (sevenZEntry.isDirectory()) {
                    this.getFileHandler().mkdirs(destinationEntry);
                } else {
                    String parent = this.getFileHandler().getParent(destinationEntry);
                    if (!this.getFileHandler().isDirectory(parent)) {
                        this.getFileHandler().mkdirs(parent);
                    }

                    OutputStream destinationFileOutputStream = this.getFileHandler().getOutputStream(destinationEntry);

                    try {
                        byte[] sevenZContent = new byte[262144];

                        int length;
                        while((length = sevenZFile.read(sevenZContent)) != -1) {
                            destinationFileOutputStream.write(sevenZContent, 0, length);
                        }
                    } catch (Throwable var15) {
                        if (destinationFileOutputStream != null) {
                            try {
                                destinationFileOutputStream.close();
                            } catch (Throwable var11) {
                                var15.addSuppressed(var11);
                            }
                        }

                        throw var15;
                    }

                    if (destinationFileOutputStream != null) {
                        destinationFileOutputStream.close();
                    }
                }
            }
        } else {
            BufferedInputStream sourceInputStream = new BufferedInputStream(this.getFileHandler().getInputStream(source));

            try {
                boolean compressedStream;
                try {
                    CompressorStreamFactory.detect(sourceInputStream);
                    compressedStream = true;
                } catch (CompressorException var13) {
                    compressedStream = false;
                }

                if (compressedStream) {
                    CompressorStreamFactory csf = new CompressorStreamFactory();
                    BufferedInputStream decompressedInputStream = new BufferedInputStream(csf.createCompressorInputStream(sourceInputStream));

                    try {
                        this.unpackStream(decompressedInputStream);
                    } catch (Throwable var12) {
                        try {
                            decompressedInputStream.close();
                        } catch (Throwable var10) {
                            var12.addSuppressed(var10);
                        }

                        throw var12;
                    }

                    decompressedInputStream.close();
                } else {
                    this.unpackStream(sourceInputStream);
                }
            } catch (Throwable var14) {
                try {
                    sourceInputStream.close();
                } catch (Throwable var9) {
                    var14.addSuppressed(var9);
                }

                throw var14;
            }

            sourceInputStream.close();
        }

    }

    private void unpackStream(InputStream sourceInputStream) throws Exception {
        ArchiveStreamFactory asf = new ArchiveStreamFactory();
        ArchiveInputStream<?> unpackedInputStream = asf.createArchiveInputStream(sourceInputStream);

        ArchiveEntry archiveEntry;
        try {
            while((archiveEntry = unpackedInputStream.getNextEntry()) != null) {
                String destinationEntry = this.getFileHandler().append(this.getExtractDir(), DefaultFileHandler.sanitizeFilename(archiveEntry.getName(), this.getLogger()));
                if (archiveEntry.isDirectory()) {
                    this.getFileHandler().mkdirs(destinationEntry);
                } else {
                    String parent = this.getFileHandler().getParent(destinationEntry);
                    if (!this.getFileHandler().isDirectory(parent)) {
                        this.getFileHandler().mkdirs(parent);
                    }

                    OutputStream destinationFileOutputStream = this.getFileHandler().getOutputStream(destinationEntry);

                    try {
                        this.getFileHandler().copy(unpackedInputStream, destinationFileOutputStream);
                    } catch (Throwable var12) {
                        if (destinationFileOutputStream != null) {
                            try {
                                destinationFileOutputStream.close();
                            } catch (Throwable var11) {
                                var12.addSuppressed(var11);
                            }
                        }

                        throw var12;
                    }

                    if (destinationFileOutputStream != null) {
                        destinationFileOutputStream.close();
                    }
                }
            }
        } catch (Throwable var13) {
            if (unpackedInputStream != null) {
                try {
                    unpackedInputStream.close();
                } catch (Throwable var10) {
                    var13.addSuppressed(var10);
                }
            }

            throw var13;
        }

        unpackedInputStream.close();
    }

    @Override
    public void install() {
        if (!this.isAlreadyExtracted()) {
            log.info(i18n.getString("cargo.installer.container_not_installed", this.getSourceFileName()));
            String targetFile = this.getFileHandler().append(this.getDownloadDir(), this.getSourceFileName());
            if (this.getFileHandler().isDirectory(targetFile)) {
                throw new ContainerException(i18n.getString("cargo.installer.file_already_exists_as_directory", targetFile));
            }

            if (!this.getFileHandler().exists(targetFile)) {
                log.debug(i18n.getString("cargo.installer.container_is_not_yet_downloaded", this.getSourceFileName()));
                this.download();
            }

            try {
                log.info(i18n.getString("cargo.installer.container_is_downloaded_unpacking", this.getSourceFileName()));
                this.unpack();
            } catch (Exception var5) {
                log.warn(i18n.getString("cargo.installer.container_is_broken", this.getSourceFileName()));
                this.getFileHandler().delete(targetFile);
                this.download();

                try {
                    log.info(i18n.getString("cargo.installer.container_as_broken_also_deleting", this.getExtractDir()));
                    this.getFileHandler().delete(this.getExtractDir());
                    this.unpack();
                } catch (Exception ee) {
                    throw new ContainerException("Failed to unpack [" + this.getSourceFileName() + "]", ee);
                }
            }
            log.info(i18n.getString("cargo.installer.container_is_unpacking", this.getSourceFileName()));
            this.registerInstallation();
        } else {
            log.info(i18n.getString("cargo.installer.container_already_installed", this.getSourceFileName()));
            log.info(i18n.getString("cargo.installer.container_using", this.getExtractDir()));
        }
    }
}
