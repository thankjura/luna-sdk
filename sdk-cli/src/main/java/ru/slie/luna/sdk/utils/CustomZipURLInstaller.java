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

public class CustomZipURLInstaller extends ZipURLInstaller {
    private final Logger log = LoggerFactory.getLogger("installer");
    private final I18nResolver i18n = new I18nResolver();

    public CustomZipURLInstaller(URL remoteLocation, String downloadDir, String extractDir) {
        super(remoteLocation, downloadDir, extractDir);
    }

    private void unpack() throws Exception {
        String source = this.getFileHandler().append(this.getDownloadDir(), this.getSourceFileName());
        this.getLogger().info("Installing container [" + source + "] in [" + this.getExtractDir() + "]", this.getClass().getName());
        if (source.endsWith(".7z")) {
            SevenZFile sevenZFile = new SevenZFile(new File(source));

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
        ArchiveInputStream dearchivedInputStream = asf.createArchiveInputStream(sourceInputStream);

        ArchiveEntry archiveEntry;
        try {
            while((archiveEntry = dearchivedInputStream.getNextEntry()) != null) {
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
                        this.getFileHandler().copy(dearchivedInputStream, destinationFileOutputStream);
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
            if (dearchivedInputStream != null) {
                try {
                    dearchivedInputStream.close();
                } catch (Throwable var10) {
                    var13.addSuppressed(var10);
                }
            }

            throw var13;
        }

        if (dearchivedInputStream != null) {
            dearchivedInputStream.close();
        }

    }

    @Override
    public void install() {
        if (!this.isAlreadyExtracted()) {
            log.info(i18n.getString("cargo.installer.container_not_installed", this.getSourceFileName()));
            String targetFile = this.getFileHandler().append(this.getDownloadDir(), this.getSourceFileName());
            if (this.getFileHandler().isDirectory(targetFile)) {
                throw new ContainerException("Target file [" + targetFile + "] already exists as a directory, either delete it or change the ZipURLInstaller target folder or file name");
            }

            if (!this.getFileHandler().exists(targetFile)) {
                this.getLogger().debug("Container [" + this.getSourceFileName() + "] is not yet downloaded.", this.getClass().getName());
                this.download();
            }

            try {
                this.getLogger().debug("Container [" + this.getSourceFileName() + "] is downloaded, now unpacking.", this.getClass().getName());
                this.unpack();
            } catch (Exception var5) {
                this.getLogger().debug("Container [" + this.getSourceFileName() + "] is broken.", this.getClass().getName());
                this.getFileHandler().delete(targetFile);
                this.download();

                try {
                    this.getLogger().debug("As the container was broken, also deleting [" + this.getExtractDir() + "] before extraction.", this.getClass().getName());
                    this.getFileHandler().delete(this.getExtractDir());
                    this.unpack();
                } catch (Exception ee) {
                    throw new ContainerException("Failed to unpack [" + this.getSourceFileName() + "]", ee);
                }
            }

            this.getLogger().debug("Container [" + this.getSourceFileName() + "] is unpacked, now registering.", this.getClass().getName());
            this.registerInstallation();
        } else {
            log.info(i18n.getString("cargo.installer.container_already_installed", this.getSourceFileName()));
            log.info(i18n.getString("cargo.installer.container_using", this.getExtractDir()));
        }
    }
}
